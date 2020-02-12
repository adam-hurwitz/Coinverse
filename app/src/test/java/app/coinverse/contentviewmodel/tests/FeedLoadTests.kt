package app.coinverse.contentviewmodel.tests

import android.widget.ProgressBar.GONE
import android.widget.ProgressBar.VISIBLE
import androidx.lifecycle.SavedStateHandle
import app.coinverse.R.string.*
import app.coinverse.contentviewmodel.FeedLoadTest
import app.coinverse.contentviewmodel.mockGetMainFeedList
import app.coinverse.contentviewmodel.mockQueryMainContentListFlow
import app.coinverse.contentviewmodel.mockQueryMainContentListLiveData
import app.coinverse.contentviewmodel.testCases.feedLoadTestCases
import app.coinverse.feed.models.FeedViewEffectType.*
import app.coinverse.feed.models.FeedViewEventType.FeedLoadComplete
import app.coinverse.feed.models.FeedViewEventType.SwipeToRefresh
import app.coinverse.feed.network.FeedRepository
import app.coinverse.feed.network.FeedRepository.getLabeledFeedRoom
import app.coinverse.feed.network.FeedRepository.getMainFeedNetwork
import app.coinverse.feed.network.FeedRepository.getMainFeedRoom
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
class FeedLoadTests(val testDispatcher: TestCoroutineDispatcher) {

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
    fun `Feed Load`(test: FeedLoadTest) = testDispatcher.runBlockingTest {
        mockComponents(test)
        feedViewModel = FeedViewModel(SavedStateHandle(), test.feedType, test.timeframe, test.isRealtime)
        assertThatToolbarState(test)
        assertContentList(test, FEED_LOAD)
        verifyTests(test)
    }

    @ParameterizedTest
    @MethodSource("FeedLoad")
    fun `Swipe-to-Refresh`(test: FeedLoadTest) = testDispatcher.runBlockingTest {
        mockComponents(test)
        feedViewModel = FeedViewModel(SavedStateHandle(), test.feedType, test.timeframe, test.isRealtime)
        assertContentList(test, FEED_LOAD)
        SwipeToRefresh(test.feedType, test.timeframe, false).also { event ->
            feedViewModel.swipeToRefresh(event)
            assertContentList(test, SWIPE_TO_REFRESH)
            feedViewModel.state.feedList.getOrAwaitValue().also { pagedList ->
                assertThat(pagedList).isEqualTo(test.mockFeedList)
                if (test.feedType == MAIN) assertSwipeToRefresh(test)
            }
        }
        verifyTests(test)
    }

    private fun mockComponents(test: FeedLoadTest) {

        // Android libraries
        every { FirebaseRemoteConfig.getInstance() } returns mockk(relaxed = true)
        every { Crashlytics.log(any(), any(), any()) } returns Unit

        // Coinverse

        // ContentRepository
        coEvery {
            getMainFeedNetwork(test.isRealtime, any())
        } returns mockGetMainFeedList(test.mockFeedList, test.lceState)
        every { getMainFeedRoom(any()) } returns mockQueryMainContentListLiveData(test.mockFeedList)
        every {
            getLabeledFeedRoom(test.feedType)
        } returns mockQueryMainContentListFlow(test.mockFeedList)

        // FirebaseRemoteConfig - Constant values
        mockkStatic(CONSTANTS_CLASS_COMPILED_JAVA)
        every { CONTENT_REQUEST_NETWORK_ERROR } returns MOCK_CONTENT_REQUEST_NETWORK_ERROR
        every {
            CONTENT_REQUEST_SWIPE_TO_REFRESH_ERROR
        } returns MOCK_CONTENT_REQUEST_SWIPE_TO_REFRESH_ERROR
    }

    private fun assertThatToolbarState(test: FeedLoadTest) {
        assertThat(feedViewModel.state.toolbarState).isEqualTo(ToolbarState(
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

    private fun assertContentList(test: FeedLoadTest, eventType: FEED_EVENT_TYPE) {
        feedViewModel.state.feedList.getOrAwaitValue().also { pagedList ->
            assertThat(pagedList).isEqualTo(test.mockFeedList)
            feedViewModel.effects.updateAds.getOrAwaitValue().also { effect ->
                assertThat(effect.javaClass).isEqualTo(UpdateAdsEffect::class.java)
            }
            if (test.feedType == MAIN && test.lceState == ERROR) {
                feedViewModel.effects.snackBar.getOrAwaitValue().also { effect ->
                    assertThat(effect).isEqualTo(SnackBarEffect(
                            if (eventType == FEED_LOAD) MOCK_CONTENT_REQUEST_NETWORK_ERROR
                            else MOCK_CONTENT_REQUEST_SWIPE_TO_REFRESH_ERROR))
                }
            }
            // ScreenEmptyEffect
            feedViewModel.feedLoadComplete(FeedLoadComplete(hasContent = pagedList.isNotEmpty()))
            feedViewModel.effects.screenEmpty.getOrAwaitValue().also { effect ->
                assertThat(effect).isEqualTo(ScreenEmptyEffect(pagedList.isEmpty()))
            }
        }
    }

    private fun assertSwipeToRefresh(test: FeedLoadTest) {
        when (test.lceState) {
            LOADING -> feedViewModel.effects.swipeToRefresh.getOrAwaitValue().also { effect ->
                assertThat(effect).isEqualTo(SwipeToRefreshEffect(true))
            }
            CONTENT -> feedViewModel.effects.swipeToRefresh.getOrAwaitValue().also { effect ->
                assertThat(effect).isEqualTo(SwipeToRefreshEffect(false))
            }
            ERROR -> feedViewModel.effects.swipeToRefresh.getOrAwaitValue().also { effect ->
                assertThat(effect).isEqualTo(SwipeToRefreshEffect(false))
            }
        }
    }

    private fun verifyTests(test: FeedLoadTest) {
        coVerify {
            when (test.feedType) {
                MAIN -> {
                    getMainFeedNetwork(test.isRealtime, any())
                    if (test.lceState == LOADING || test.lceState == ERROR) getMainFeedRoom(any())
                }
                SAVED, DISMISSED -> getLabeledFeedRoom(test.feedType)
            }
        }
        confirmVerified(FeedRepository)
    }
}