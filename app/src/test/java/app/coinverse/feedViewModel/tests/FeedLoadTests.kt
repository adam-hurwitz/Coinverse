package app.coinverse.feedViewModel.tests

import android.widget.ProgressBar.GONE
import android.widget.ProgressBar.VISIBLE
import app.coinverse.R.string.app_name
import app.coinverse.R.string.dismissed
import app.coinverse.R.string.saved
import app.coinverse.analytics.Analytics
import app.coinverse.feed.FeedViewEventType.FeedLoadComplete
import app.coinverse.feed.FeedViewModel
import app.coinverse.feed.data.FeedRepository
import app.coinverse.feed.models.FeedViewEffectType.ScreenEmptyEffect
import app.coinverse.feed.models.FeedViewEffectType.SnackBarEffect
import app.coinverse.feed.models.FeedViewEffectType.SwipeToRefreshEffect
import app.coinverse.feed.models.FeedViewEffectType.UpdateAdsEffect
import app.coinverse.feedViewModel.FeedLoadTest
import app.coinverse.feedViewModel.mockGetMainFeedList
import app.coinverse.feedViewModel.mockQueryMainContentListFlow
import app.coinverse.feedViewModel.mockQueryMainContentListLiveData
import app.coinverse.feedViewModel.testCases.feedLoadTestCases
import app.coinverse.utils.CONSTANTS_CLASS_COMPILED_JAVA
import app.coinverse.utils.CONTENT_REQUEST_NETWORK_ERROR
import app.coinverse.utils.CONTENT_REQUEST_SWIPE_TO_REFRESH_ERROR
import app.coinverse.utils.ContentTestExtension
import app.coinverse.utils.FeedEventType
import app.coinverse.utils.FeedEventType.FEED_LOAD
import app.coinverse.utils.FeedEventType.SWIPE_TO_REFRESH
import app.coinverse.utils.FeedType.DISMISSED
import app.coinverse.utils.FeedType.MAIN
import app.coinverse.utils.FeedType.SAVED
import app.coinverse.utils.MOCK_CONTENT_REQUEST_NETWORK_ERROR
import app.coinverse.utils.MOCK_CONTENT_REQUEST_SWIPE_TO_REFRESH_ERROR
import app.coinverse.utils.Status.ERROR
import app.coinverse.utils.Status.LOADING
import app.coinverse.utils.Status.SUCCESS
import app.coinverse.utils.ToolbarState
import com.crashlytics.android.Crashlytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

// Todo: Refactor with Model-View-Intent.

@ExperimentalCoroutinesApi
@ExtendWith(ContentTestExtension::class)
class FeedLoadTests(
        val testDispatcher: TestCoroutineDispatcher,
        val testScope: TestCoroutineScope
) {

    private fun FeedLoad() = feedLoadTestCases()
    private val repository = mockkClass(FeedRepository::class)
    private val analytics = mockkClass(Analytics::class)
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
        feedViewModel = FeedViewModel(
                coroutineScopeProvider = testScope,
                feedType = test.feedType,
                timeframe = test.timeframe,
                isRealtime = test.isRealtime,
                repository = repository,
                analytics = analytics)
        assertThatToolbarState(test)
        assertContentList(test, FEED_LOAD)
        verifyTests(test)
    }

    @ParameterizedTest
    @MethodSource("FeedLoad")
    fun `Swipe-to-Refresh`(test: FeedLoadTest) = testDispatcher.runBlockingTest {
        mockComponents(test)
        feedViewModel = FeedViewModel(
                coroutineScopeProvider = testScope,
                feedType = test.feedType,
                timeframe = test.timeframe,
                isRealtime = test.isRealtime,
                repository = repository,
                analytics = analytics)
        assertContentList(test, FEED_LOAD)
        // Fixme
        /*if (test.feedType == MAIN)
            SwipeToRefresh(test.feedType, test.timeframe, false).also { event ->
                feedViewModel.swipeToRefresh(event)
                assertContentList(test, FeedEventType.SWIPE_TO_REFRESH)
            }*/
        verifyTests(test)
    }

    private fun mockComponents(test: FeedLoadTest) {

        // Android libraries
        every { FirebaseRemoteConfig.getInstance() } returns mockk(relaxed = true)
        every { Crashlytics.log(any(), any(), any()) } returns Unit

        // Coinverse

        // ContentRepository
        coEvery {
            repository.getMainFeedNetwork(any(), any())
        } returns mockGetMainFeedList(test.mockFeedList, test.status)
        every {
            repository.getMainFeedRoom(any())
        } returns mockQueryMainContentListLiveData(test.mockFeedList)
        every {
            repository.getLabeledFeedRoom(any())
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

    private fun assertContentList(test: FeedLoadTest, eventType: FeedEventType) {
        feedViewModel.state.feedList.getOrAwaitValue().also { pagedList ->
            assertThat(pagedList).isEqualTo(test.mockFeedList)
            if (test.feedType == MAIN && eventType == SWIPE_TO_REFRESH) assertSwipeToRefresh(test)
            // UpdateAdsEffect
            feedViewModel.effect.updateAds.getOrAwaitValue().also { effect ->
                assertThat(effect.javaClass).isEqualTo(UpdateAdsEffect::class.java)
            }
            if (test.feedType == MAIN && test.status == ERROR) {
                // SnackbarEffect
                feedViewModel.effect.snackBar.getOrAwaitValue().also { effect ->
                    assertThat(effect).isEqualTo(SnackBarEffect(
                            if (eventType == FEED_LOAD) MOCK_CONTENT_REQUEST_NETWORK_ERROR
                            else MOCK_CONTENT_REQUEST_SWIPE_TO_REFRESH_ERROR))
                }
            }
            // ScreenEmptyEffect
            feedViewModel.feedLoadComplete(FeedLoadComplete(hasContent = pagedList.isNotEmpty()))
            feedViewModel.effect.screenEmpty.getOrAwaitValue().also { effect ->
                assertThat(effect).isEqualTo(ScreenEmptyEffect(pagedList.isEmpty()))
            }
        }
    }

    private fun assertSwipeToRefresh(test: FeedLoadTest) {
        when (test.status) {
            LOADING -> feedViewModel.effect.swipeToRefresh.getOrAwaitValue().also { effect ->
                assertThat(effect).isEqualTo(SwipeToRefreshEffect(true))
            }
            SUCCESS -> feedViewModel.effect.swipeToRefresh.getOrAwaitValue().also { effect ->
                assertThat(effect).isEqualTo(SwipeToRefreshEffect(false))
            }
            ERROR -> feedViewModel.effect.swipeToRefresh.getOrAwaitValue().also { effect ->
                assertThat(effect).isEqualTo(SwipeToRefreshEffect(false))
            }
        }
    }

    private fun verifyTests(test: FeedLoadTest) {
        coVerify {
            when (test.feedType) {
                MAIN -> {
                    repository.getMainFeedNetwork(any(), any())
                    if (test.status == LOADING || test.status == ERROR)
                        repository.getMainFeedRoom(any())
                }
                SAVED, DISMISSED -> repository.getLabeledFeedRoom(any())
            }
        }
        confirmVerified(repository)
    }
}