package app.coinverse.contentviewmodel.tests

import android.widget.ProgressBar.GONE
import android.widget.ProgressBar.VISIBLE
import androidx.lifecycle.SavedStateHandle
import app.coinverse.R.string.*
import app.coinverse.contentviewmodel.FeedLoadContentTest
import app.coinverse.contentviewmodel.mockGetMainFeedList
import app.coinverse.contentviewmodel.mockQueryMainContentListFlow
import app.coinverse.contentviewmodel.mockQueryMainContentListLiveData
import app.coinverse.contentviewmodel.testCases.feedLoadTestCases
import app.coinverse.feed.FeedRepository
import app.coinverse.feed.FeedRepository.getMainFeedList
import app.coinverse.feed.FeedRepository.queryLabeledContentList
import app.coinverse.feed.FeedRepository.queryMainContentList
import app.coinverse.feed.models.FeedViewEffectType.*
import app.coinverse.feed.models.FeedViewEventType.FeedLoadComplete
import app.coinverse.feed.models.FeedViewEventType.SwipeToRefresh
import app.coinverse.feed.viewmodels.FeedViewModel
import app.coinverse.utils.*
import app.coinverse.utils.FEED_EVENT_TYPE.FEED_LOAD
import app.coinverse.utils.FEED_EVENT_TYPE.SWIPE_TO_REFRESH
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
class FeedLoadContentTests(val testDispatcher: TestCoroutineDispatcher) {

    private fun FeedLoad() = feedLoadTestCases()
    private lateinit var feedViewModel: FeedViewModel

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
        feedViewModel = FeedViewModel(SavedStateHandle(), test.feedType, test.timeframe, test.isRealtime)
        assertThatToolbarState(test)
        assertContentList(test, FEED_LOAD)
        verifyTests(test)
    }

    @ParameterizedTest
    @MethodSource("FeedLoad")
    fun `Swipe-to-Refresh`(test: FeedLoadContentTest) = testDispatcher.runBlockingTest {
        mockComponents(test)
        feedViewModel = FeedViewModel(SavedStateHandle(), test.feedType, test.timeframe, test.isRealtime)
        assertContentList(test, FEED_LOAD)
        SwipeToRefresh(test.feedType, test.timeframe, false).also { event ->
            feedViewModel.swipeToRefresh(event)
            assertContentList(test, SWIPE_TO_REFRESH)
            feedViewModel.feedViewState().contentList.getOrAwaitValue().also { pagedList ->
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
        assertThat(feedViewModel.feedViewState().toolbar).isEqualTo(ToolbarState(
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

    private fun assertContentList(test: FeedLoadContentTest, eventType: FEED_EVENT_TYPE) {
        feedViewModel.feedViewState().contentList.getOrAwaitValue().also { pagedList ->
            assertThat(pagedList).isEqualTo(test.mockFeedList)
            assertThat(feedViewModel.feedViewState().timeframe).isEqualTo(test.timeframe)
            feedViewModel.viewEffects().updateAds.observe().also { effect ->
                assertThat(effect.javaClass).isEqualTo(UpdateAdsEffect::class.java)
            }
            if (test.feedType == MAIN && test.lceState == ERROR) {
                feedViewModel.viewEffects().snackBar.observe().also { effect ->
                    assertThat(effect).isEqualTo(SnackBarEffect(
                            if (eventType == FEED_LOAD) MOCK_CONTENT_REQUEST_NETWORK_ERROR
                            else MOCK_CONTENT_REQUEST_SWIPE_TO_REFRESH_ERROR))
                }
            }
            // ScreenEmptyEffect
            feedViewModel.feedLoadComplete(FeedLoadComplete(hasContent = pagedList.isNotEmpty()))
            feedViewModel.viewEffects().screenEmpty.observe().also { effect ->
                assertThat(effect).isEqualTo(ScreenEmptyEffect(pagedList.isEmpty()))
            }
        }
    }

    private fun assertSwipeToRefresh(test: FeedLoadContentTest) {
        when (test.lceState) {
            LOADING -> feedViewModel.viewEffects()
                    .swipeToRefresh.observe().also { effect ->
                assertThat(effect).isEqualTo(SwipeToRefreshEffect(true))
            }
            CONTENT -> feedViewModel.viewEffects()
                    .swipeToRefresh.observe().also { effect ->
                assertThat(effect).isEqualTo(SwipeToRefreshEffect(false))
            }
            ERROR -> feedViewModel.viewEffects()
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
        confirmVerified(FeedRepository)
    }
}