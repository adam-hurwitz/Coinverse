package app.coinverse.feedViewModel.tests

import app.coinverse.analytics.Analytics
import app.coinverse.feed.FeedViewEventType.OpenContentSource
import app.coinverse.feed.FeedViewEventType.ShareContent
import app.coinverse.feed.FeedViewEventType.UpdateAds
import app.coinverse.feed.FeedViewModel
import app.coinverse.feed.data.FeedRepository
import app.coinverse.feed.models.FeedViewEffectType.OpenSourceIntentEffect
import app.coinverse.feed.models.FeedViewEffectType.UpdateAdsEffect
import app.coinverse.feed.state.FeedViewState
import app.coinverse.feedViewModel.NavigateContentTest
import app.coinverse.feedViewModel.mockGetContent
import app.coinverse.feedViewModel.mockGetMainFeedList
import app.coinverse.feedViewModel.mockQueryMainContentListFlow
import app.coinverse.feedViewModel.testCases.navigateContentTestCases
import app.coinverse.utils.ContentTestExtension
import app.coinverse.utils.FeedType.DISMISSED
import app.coinverse.utils.FeedType.MAIN
import app.coinverse.utils.FeedType.SAVED
import app.coinverse.utils.Status.SUCCESS
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockkClass
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

// Todo: Refactor with Model-View-Intent.

@ExperimentalCoroutinesApi
@ExtendWith(ContentTestExtension::class)
class NavigateContentTests(
        val testDispatcher: TestCoroutineDispatcher,
        val testScope: TestCoroutineScope
) {

    private fun NavigateContent() = navigateContentTestCases()
    private val repository = mockkClass(FeedRepository::class)
    private val analytics = mockkClass(Analytics::class)
    private lateinit var feedViewModel: FeedViewModel

    @ParameterizedTest
    @MethodSource("NavigateContent")
    fun `Navigate Content`(test: NavigateContentTest) = testDispatcher.runBlockingTest {
        mockComponents(test)
        feedViewModel = FeedViewModel(
                coroutineScopeProvider = testScope,
                feedType = test.feedType,
                timeframe = test.timeframe,
                isRealtime = test.isRealtime,
                repository = repository,
                analytics = analytics)
        println("NavigateContent: ${test.mockContent.contentType}")
        assertContentList(test)
        FeedViewState.ShareContent(test.mockContent).also { event ->
            feedViewModel.shareContent(event)
            assertThat(feedViewModel.effect.shareContentIntent.getOrAwaitValue().contentRequest.getOrAwaitValue())
                    .isEqualTo(test.mockContent)
        }
        FeedViewState.OpenContentSource(test.mockContent.url).also { event ->
            feedViewModel.openContentSource(event)
            assertThat(feedViewModel.effect.openSourceIntent.getOrAwaitValue())
                    .isEqualTo(OpenSourceIntentEffect(test.mockContent.url))
        }
        // Occurs on Fragment 'onViewStateRestored'
        FeedViewState.UpdateAds().also { event ->
            feedViewModel.updateAds(event)
            assertThat(feedViewModel.effect.updateAds.getOrAwaitValue().javaClass).isEqualTo(UpdateAdsEffect::class.java)
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
