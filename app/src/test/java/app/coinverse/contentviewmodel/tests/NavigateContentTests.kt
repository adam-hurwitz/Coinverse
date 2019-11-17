package app.coinverse.contentviewmodel.tests

import app.coinverse.content.ContentRepository.getContent
import app.coinverse.content.ContentRepository.getMainFeedList
import app.coinverse.content.ContentRepository.queryLabeledContentList
import app.coinverse.content.ContentViewModel
import app.coinverse.content.models.ContentEffectType.OpenContentSourceIntentEffect
import app.coinverse.content.models.ContentEffectType.UpdateAdsEffect
import app.coinverse.content.models.ContentViewEventType.*
import app.coinverse.contentviewmodel.NavigateContentTest
import app.coinverse.contentviewmodel.mockGetContent
import app.coinverse.contentviewmodel.mockGetMainFeedList
import app.coinverse.contentviewmodel.mockQueryMainContentListFlow
import app.coinverse.contentviewmodel.testCases.navigateContentTestCases
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
class NavigateContentTests(val testDispatcher: TestCoroutineDispatcher,
                           val contentViewModel: ContentViewModel) {

    private fun NavigateContent() = navigateContentTestCases()

    @ParameterizedTest
    @MethodSource("NavigateContent")
    fun `Navigate Content`(test: NavigateContentTest) = testDispatcher.runBlockingTest {
        mockComponents(test)
        FeedLoad(test.feedType, test.timeframe, false).also { event ->
            contentViewModel.feedLoad(event)
            assertContentList(test)
        }
        ContentShared(test.mockContent).also { event ->
            contentViewModel.contentShared(event)
            assertThat(contentViewModel.viewEffects().shareContentIntent.observe()
                    .contentRequest.observe())
                    .isEqualTo(test.mockContent)
        }
        ContentSourceOpened(test.mockContent.url).also { event ->
            contentViewModel.contentSourceOpened(event)
            assertThat(contentViewModel.viewEffects().openContentSourceIntent.observe())
                    .isEqualTo(OpenContentSourceIntentEffect(test.mockContent.url))
        }
        // Occurs on Fragment 'onViewStateRestored'
        UpdateAds().also { event ->
            contentViewModel.updateAds(event)
            assertThat(contentViewModel.viewEffects().updateAds.observe().javaClass)
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
        contentViewModel.feedViewState().contentList.getOrAwaitValue().also { pagedList ->
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