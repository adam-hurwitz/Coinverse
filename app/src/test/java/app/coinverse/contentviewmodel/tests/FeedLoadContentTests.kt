package app.coinverse.contentviewmodel.tests

import android.widget.ProgressBar.GONE
import android.widget.ProgressBar.VISIBLE
import app.coinverse.R.string.*
import app.coinverse.content.ContentRepository
import app.coinverse.content.ContentRepository.getMainFeedList
import app.coinverse.content.ContentRepository.queryLabeledContentList
import app.coinverse.content.ContentRepository.queryMainContentList
import app.coinverse.content.ContentViewModel
import app.coinverse.content.models.ContentEffectType.*
import app.coinverse.content.models.ContentViewEvents
import app.coinverse.content.models.ContentViewEvents.*
import app.coinverse.contentviewmodel.*
import app.coinverse.utils.*
import app.coinverse.utils.FeedType.*
import app.coinverse.utils.LCE_STATE.*
import app.coinverse.utils.models.ToolbarState
import com.crashlytics.android.Crashlytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import io.mockk.*
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@ExtendWith(ContentTestExtension::class)
class FeedLoadContentTests(val testDispatcher: TestCoroutineDispatcher,
                           val contentViewModel: ContentViewModel) {

    private fun FeedLoad() = feedLoadTestCases()

    @BeforeAll
    fun beforeAll() {
        // Android libraries
        mockkStatic(FirebaseRemoteConfig::class)
        mockkStatic(Crashlytics::class)
    }

    @ParameterizedTest
    @MethodSource("FeedLoad")
    fun `Feed Load`(test: FeedLoadContentTest) = testDispatcher.runBlockingTest {
        mockComponents(test)
        FeedLoad(test.feedType, test.timeframe, false).also { event ->
            contentViewModel.processEvent(event)
            assertThatToolbarState(test)
            assertContentList(test, event)
        }
        verifyTests(test)
    }

    @ParameterizedTest
    @MethodSource("FeedLoad")
    fun `Swipe-to-Refresh`(test: FeedLoadContentTest) = testDispatcher.runBlockingTest {
        mockComponents(test)
        FeedLoad(test.feedType, test.timeframe, false).also { event ->
            contentViewModel.processEvent(event)
            assertContentList(test, event)
        }
        SwipeToRefresh(test.feedType, test.timeframe, false).also { event ->
            contentViewModel.processEvent(event)
            assertContentList(test, event)
            contentViewModel.feedViewState().contentList.getOrAwaitValue().also { pagedList ->
                assertThat(pagedList).isEqualTo(test.mockFeedList)
                if (test.feedType == MAIN) assertSwipeToRefresh(test)
            }
        }
        verifyTests(test)
    }

    private fun mockComponents(test: FeedLoadContentTest) {

        // Android libraries
        every { FirebaseRemoteConfig.getInstance() } returns mockk(relaxed = true)
        every { Crashlytics.log(any(), any(), any()) } returns Unit

        // Coinverse

        // ContentRepository
        coEvery {
            getMainFeedList(test.isRealtime, any())
        } returns mockGetMainFeedList(test.mockFeedList, test.lceState)
        every { queryMainContentList(any()) } returns mockQueryMainContentListLiveData(test.mockFeedList)
        every {
            queryLabeledContentList(test.feedType)
        } returns mockQueryMainContentListFlow(test.mockFeedList)

        // FirebaseRemoteConfig - Constant values
        mockkStatic(CONSTANTS_CLASS_COMPILED_JAVA)
        every { CONTENT_REQUEST_NETWORK_ERROR } returns MOCK_CONTENT_REQUEST_NETWORK_ERROR
        every {
            CONTENT_REQUEST_SWIPE_TO_REFRESH_ERROR
        } returns MOCK_CONTENT_REQUEST_SWIPE_TO_REFRESH_ERROR
    }

    private fun assertThatToolbarState(test: FeedLoadContentTest) {
        assertThat(contentViewModel.feedViewState().toolbar).isEqualTo(ToolbarState(
                when (test.feedType) {
                    MAIN -> GONE
                    SAVED, DISMISSED -> VISIBLE
                },
                when (test.feedType) {
                    SAVED -> saved
                    DISMISSED -> dismissed
                    MAIN -> app_name
                },
                when (test.feedType) {
                    SAVED, MAIN -> false
                    DISMISSED -> true
                }
        ))
    }

    private fun assertContentList(test: FeedLoadContentTest, event: ContentViewEvents) {
        contentViewModel.feedViewState().contentList.getOrAwaitValue().also { pagedList ->
            assertThat(pagedList).isEqualTo(test.mockFeedList)
            assertThat(contentViewModel.feedViewState().timeframe).isEqualTo(test.timeframe)
            contentViewModel.viewEffects().updateAds.observe().also { effect ->
                assertThat(effect.javaClass).isEqualTo(UpdateAdsEffect::class.java)
            }
            if (test.feedType == MAIN && test.lceState == ERROR) {
                contentViewModel.viewEffects().snackBar.observe().also { effect ->
                    assertThat(effect).isEqualTo(SnackBarEffect(
                            if (event is FeedLoad) MOCK_CONTENT_REQUEST_NETWORK_ERROR
                            else MOCK_CONTENT_REQUEST_SWIPE_TO_REFRESH_ERROR))
                }
            }
            // ScreenEmptyEffect
            contentViewModel.processEvent(FeedLoadComplete(hasContent = pagedList.isNotEmpty()))
            contentViewModel.viewEffects().screenEmpty.observe().also { effect ->
                assertThat(effect).isEqualTo(ScreenEmptyEffect(pagedList.isEmpty()))
            }
        }
    }

    private fun assertSwipeToRefresh(test: FeedLoadContentTest) {
        when (test.lceState) {
            LOADING -> contentViewModel.viewEffects()
                    .swipeToRefresh.observe().also { effect ->
                assertThat(effect).isEqualTo(SwipeToRefreshEffect(true))
            }
            CONTENT -> contentViewModel.viewEffects()
                    .swipeToRefresh.observe().also { effect ->
                assertThat(effect).isEqualTo(SwipeToRefreshEffect(false))
            }
            ERROR -> contentViewModel.viewEffects()
                    .swipeToRefresh.observe().also { effect ->
                assertThat(effect).isEqualTo(SwipeToRefreshEffect(false))
            }
        }
    }

    private fun verifyTests(test: FeedLoadContentTest) {
        coVerify {
            when (test.feedType) {
                MAIN -> {
                    getMainFeedList(test.isRealtime, any())
                    if (test.lceState == LOADING || test.lceState == ERROR) queryMainContentList(any())
                }
                SAVED, DISMISSED -> queryLabeledContentList(test.feedType)
            }
        }
        confirmVerified(ContentRepository)
    }
}