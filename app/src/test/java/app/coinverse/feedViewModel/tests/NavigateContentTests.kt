package app.coinverse.feedViewModel.tests

import androidx.lifecycle.SavedStateHandle
import app.coinverse.analytics.Analytics
import app.coinverse.feedViewModel.NavigateContentTest
import app.coinverse.feedViewModel.mockGetContent
import app.coinverse.feedViewModel.mockGetMainFeedList
import app.coinverse.feedViewModel.mockQueryMainContentListFlow
import app.coinverse.feedViewModel.testCases.navigateContentTestCases
import app.coinverse.feed.FeedRepository
import app.coinverse.feed.models.FeedViewEffectType.OpenContentSourceIntentEffect
import app.coinverse.feed.models.FeedViewEffectType.UpdateAdsEffect
import app.coinverse.feed.models.FeedViewEventType.*
import app.coinverse.feed.viewmodel.FeedViewModel
import app.coinverse.utils.ContentTestExtension
import app.coinverse.utils.FeedType.*
import app.coinverse.utils.Status.SUCCESS
import app.coinverse.utils.getOrAwaitValue
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockkClass
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@ExtendWith(ContentTestExtension::class)
class NavigateContentTests(val testDispatcher: TestCoroutineDispatcher) {

    private fun NavigateContent() = navigateContentTestCases()
    private val repository = mockkClass(FeedRepository::class)
    private val analytics = mockkClass(Analytics::class)
    private lateinit var feedViewModel: FeedViewModel

    @ParameterizedTest
    @MethodSource("NavigateContent")
    fun `Navigate Content`(test: NavigateContentTest) = testDispatcher.runBlockingTest {
        mockComponents(test)
        feedViewModel = FeedViewModel(
                savedStateHandle = SavedStateHandle(),
                repository = repository,
                analytics = analytics,
                feedType = test.feedType,
                timeframe = test.timeframe,
                isRealtime = test.isRealtime)
        assertContentList(test)
        ContentShared(test.mockContent).also { event ->
            feedViewModel.contentShared(event)
            assertThat(feedViewModel.effects.shareContentIntent.getOrAwaitValue().contentRequest.getOrAwaitValue())
                    .isEqualTo(test.mockContent)
        }
        ContentSourceOpened(test.mockContent.url).also { event ->
            feedViewModel.contentSourceOpened(event)
            assertThat(feedViewModel.effects.openContentSourceIntent.getOrAwaitValue())
                    .isEqualTo(OpenContentSourceIntentEffect(test.mockContent.url))
        }
        // Occurs on Fragment 'onViewStateRestored'
        UpdateAds().also { event ->
            feedViewModel.updateAds(event)
            assertThat(feedViewModel.effects.updateAds.getOrAwaitValue().javaClass).isEqualTo(UpdateAdsEffect::class.java)
        }
        verifyTests(test)
    }

    private fun mockComponents(test: NavigateContentTest) {
        // Coinverse - ContentRepository
        coEvery {
            repository.getMainFeedNetwork(test.isRealtime, any())
        } returns mockGetMainFeedList(test.mockFeedList, SUCCESS)
        every {
            repository.getLabeledFeedRoom(test.feedType)
        } returns mockQueryMainContentListFlow(test.mockFeedList)
        every { repository.getContent(test.mockContent.id) } returns mockGetContent(test)
    }

    private fun assertContentList(test: NavigateContentTest) {
        feedViewModel.state.feedList.getOrAwaitValue().also { pagedList ->
            assertThat(pagedList).isEqualTo(test.mockFeedList)
        }
    }

    private fun verifyTests(test: NavigateContentTest) {
        coVerify {
            when (test.feedType) {
                MAIN -> repository.getMainFeedNetwork(test.isRealtime, any())
                SAVED, DISMISSED -> repository.getLabeledFeedRoom(test.feedType)
            }
        }
    }
}
