package app.coinverse.contentviewmodel.tests

import androidx.lifecycle.SavedStateHandle
import app.coinverse.contentviewmodel.NavigateContentTest
import app.coinverse.contentviewmodel.mockGetContent
import app.coinverse.contentviewmodel.mockGetMainFeedList
import app.coinverse.contentviewmodel.mockQueryMainContentListFlow
import app.coinverse.contentviewmodel.testCases.navigateContentTestCases
import app.coinverse.feed.FeedRepository.getContent
import app.coinverse.feed.FeedRepository.getMainFeedList
import app.coinverse.feed.FeedRepository.queryLabeledContentList
import app.coinverse.feed.models.FeedViewEffectType.OpenContentSourceIntentEffect
import app.coinverse.feed.models.FeedViewEffectType.UpdateAdsEffect
import app.coinverse.feed.models.FeedViewEventType.*
import app.coinverse.feed.viewmodels.FeedViewModel
import app.coinverse.utils.*
import app.coinverse.utils.FeedType.*
import app.coinverse.utils.LCE_STATE.CONTENT
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@ExtendWith(ContentTestExtension::class)
class NavigateContentTests(val testDispatcher: TestCoroutineDispatcher) {

    private fun NavigateContent() = navigateContentTestCases()
    private lateinit var feedViewModel: FeedViewModel

    @ParameterizedTest
    @MethodSource("NavigateContent")
    fun `Navigate Content`(test: NavigateContentTest) = testDispatcher.runBlockingTest {
        mockComponents(test)
        feedViewModel = FeedViewModel(SavedStateHandle(), test.feedType, test.timeframe, test.isRealtime)
        assertContentList(test)
        ContentShared(test.mockContent).also { event ->
            feedViewModel.contentShared(event)
            assertThat(feedViewModel.viewEffects().shareContentIntent.observe()
                    .contentRequest.observe())
                    .isEqualTo(test.mockContent)
        }
        ContentSourceOpened(test.mockContent.url).also { event ->
            feedViewModel.contentSourceOpened(event)
            assertThat(feedViewModel.viewEffects().openContentSourceIntent.observe())
                    .isEqualTo(OpenContentSourceIntentEffect(test.mockContent.url))
        }
        // Occurs on Fragment 'onViewStateRestored'
        UpdateAds().also { event ->
            feedViewModel.updateAds(event)
            assertThat(feedViewModel.viewEffects().updateAds.observe().javaClass)
                    .isEqualTo(UpdateAdsEffect::class.java)
        }
        verifyTests(test)
    }

    private fun mockComponents(test: NavigateContentTest) {
        // Coinverse - ContentRepository
        coEvery { getMainFeedList(test.isRealtime, any()) } returns mockGetMainFeedList(
                test.mockFeedList, CONTENT)
        every {
            queryLabeledContentList(test.feedType)
        } returns mockQueryMainContentListFlow(test.mockFeedList)
        every { getContent(test.mockContent.id) } returns mockGetContent(test)
    }

    private fun assertContentList(test: NavigateContentTest) {
        feedViewModel.feedViewState().contentList.getOrAwaitValue().also { pagedList ->
            assertThat(pagedList).isEqualTo(test.mockFeedList)
        }
    }

    private fun verifyTests(test: NavigateContentTest) {
        coVerify {
            when (test.feedType) {
                MAIN -> getMainFeedList(test.isRealtime, any())
                SAVED, DISMISSED -> queryLabeledContentList(test.feedType)
            }
        }
    }
}