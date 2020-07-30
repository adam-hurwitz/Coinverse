package app.coinverse.feedViewModel.tests

import android.content.SharedPreferences
import android.widget.ProgressBar.GONE
import android.widget.ProgressBar.VISIBLE
import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import app.coinverse.R
import app.coinverse.analytics.Analytics
import app.coinverse.feed.FeedViewModel
import app.coinverse.feed.data.FeedRepository
import app.coinverse.feed.state.FeedView
import app.coinverse.feed.state.FeedViewIntent
import app.coinverse.feed.state.FeedViewIntentType.SelectContent
import app.coinverse.feed.state.FeedViewIntentType.SwipeToRefresh
import app.coinverse.feed.state.FeedViewState
import app.coinverse.feed.state.FeedViewState.Feed
import app.coinverse.feed.state.FeedViewState.OpenContent
import app.coinverse.feed.state.FeedViewState.UpdateAds
import app.coinverse.feedViewModel.FeedViewTest
import app.coinverse.feedViewModel.mockArticleContent
import app.coinverse.feedViewModel.mockFeedPosition
import app.coinverse.feedViewModel.mockGetAudiocast
import app.coinverse.feedViewModel.mockGetMainFeedList
import app.coinverse.feedViewModel.mockGetMainFeedRoom
import app.coinverse.feedViewModel.testCases.feedViewTestCases
import app.coinverse.utils.CONSTANTS_CLASS_COMPILED_JAVA
import app.coinverse.utils.CONTENT_REQUEST_NETWORK_ERROR
import app.coinverse.utils.CONTENT_REQUEST_SWIPE_TO_REFRESH_ERROR
import app.coinverse.utils.Event
import app.coinverse.utils.FeedType
import app.coinverse.utils.FeedType.DISMISSED
import app.coinverse.utils.FeedType.MAIN
import app.coinverse.utils.FeedType.SAVED
import app.coinverse.utils.MOCK_CONTENT_REQUEST_SWIPE_TO_REFRESH_ERROR
import app.coinverse.utils.MOCK_GET_MAIN_FEED_ERROR
import app.coinverse.utils.MOCK_TTS_CHAR_LIMIT_ERROR_MESSAGE
import app.coinverse.utils.PLAYER_OPEN_STATUS_KEY
import app.coinverse.utils.TTS_CHAR_LIMIT_ERROR_MESSAGE
import app.coinverse.utils.ToolbarState
import app.coinverse.utils.asPagedList
import com.crashlytics.android.Crashlytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@ExperimentalCoroutinesApi
class FeedViewTests {
    // Todo: Move to extension
    private val testDispatcher = TestCoroutineDispatcher()
    private val testCoroutineScope = TestCoroutineScope()
    private fun FeedLoad() = feedViewTestCases()
    private lateinit var test: FeedViewTest

    val intent = FeedViewIntent()
    val sharedPreferences = mockkClass(SharedPreferences::class)
    val repository = mockkClass(FeedRepository::class)
    val analytics = mockkClass(Analytics::class)

    @ParameterizedTest
    @MethodSource("FeedLoad")
    fun `Feed Load`(feedView: FeedViewTest) = testDispatcher.runBlockingTest {
        test = feedView
        val viewModel = FeedViewModel(
                coroutineScopeProvider = testCoroutineScope,
                feedType = test.feedType,
                timeframe = test.timeframe,
                isRealtime = test.isRealtime,
                sharedPreferences = sharedPreferences,
                repository = repository,
                analytics = analytics
        )

        // Todo: Move to extension
        // Set Coroutine Dispatcher.
        Dispatchers.setMain(testDispatcher)
        // Set LiveData Executor.
        ArchTaskExecutor.getInstance().setDelegate(object : TaskExecutor() {
            override fun executeOnDiskIO(runnable: Runnable) = runnable.run()
            override fun postToMainThread(runnable: Runnable) = runnable.run()
            override fun isMainThread(): Boolean = true
        })

        // Todo: Move to lifecycle method
        mockkStatic(FirebaseRemoteConfig::class)
        mockkStatic(Crashlytics::class)

        mockComponents(test)
        viewModel.bindIntents(object : FeedView {
            override fun initState() = intent.initState
            override fun loadFromNetwork() = intent.loadFromNetwork.filterNotNull()
            override fun swipeToRefresh() = intent.swipeToRefresh.filterNotNull()
            override fun selectContent() = intent.selectContent.filterNotNull()
            override fun swipeContent() = intent.swipeContent.filterNotNull()
            override fun labelContent() = intent.labelContent.filterNotNull()
            override fun shareContent() = intent.shareContent.filterNotNull()
            override fun openContentSource() = intent.openContentSource.filterNotNull()
            override fun updateAds() = intent.updateAds.filterNotNull()
            override fun render(state: FeedViewState) {
                when (state) {
                    is Feed -> testRenderFeed(state)
                    is FeedViewState.SwipeToRefresh -> assertThat(state).isEqualTo(test.swipeToRefresh)
                    is OpenContent -> testRenderOpenContent(state)
                    is UpdateAds -> assertThat(state.javaClass).hasSameClassAs(UpdateAds::class.java)
                }
            }

        })
        intent.loadFromNetwork.value = true

        // Todo: Move to extension
        unmockkAll()
        // Reset Coroutine Dispatcher and Scope.
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
        testCoroutineScope.cleanupTestCoroutines()
        // Clear LiveData Executor
        ArchTaskExecutor.getInstance().setDelegate(null)
    }

    private fun testRenderFeed(feed: Feed) {
        val expect = Feed(
                toolbarState = setToolbarState(test.feedType),
                feed = test.mockFeedList.asPagedList(),
                error = test.error
        )
        assertThat(feed).isEqualToComparingFieldByField(expect)
        if (test.feedType == MAIN)
            intent.swipeToRefresh.value = Event(SwipeToRefresh(
                    feedType = test.feedType,
                    timeframe = test.timeframe,
                    isRealtime = test.isRealtime)
            )
        // Todo: YouTube
        intent.selectContent.value = Event(SelectContent(
                content = mockArticleContent,
                position = mockFeedPosition
        ))
    }

    private fun testRenderOpenContent(state: OpenContent) {
        println("selectContent ${state}")
        assertThat(state).isEqualToComparingOnlyGivenFields(
                test.openContent,
                "isLoading",
                "position",
                "contentId",
                "filePath",
                "error"
        )
        assertThat(state.content).isEqualToComparingOnlyGivenFields(
                test.openContent!!.content,
                "id",
                "contentType"
        )
    }

    private fun mockComponents(test: FeedViewTest) {
        // Android libraries
        every { FirebaseRemoteConfig.getInstance() } returns mockk(relaxed = true)
        every { Crashlytics.log(any(), any(), any()) } returns Unit
        every { sharedPreferences.getBoolean(PLAYER_OPEN_STATUS_KEY, false) } returns false
        // every { SharedPreferences() } returns false
        // FeedRepository
        coEvery {
            repository.getMainFeedNetwork(any(), any())
        } returns mockGetMainFeedList(test.status, test.mockFeedList)
        every {
            repository.getMainFeedRoom(any())
        } returns mockGetMainFeedRoom(test.mockFeedList)
        every {
            repository.getLabelFeedRoom(any())
        } returns mockGetMainFeedRoom(test.mockFeedList)
        every {
            repository.getAudiocast(any())
        } returns mockGetAudiocast(test)
        // FirebaseRemoteConfig - Constant values
        mockkStatic(CONSTANTS_CLASS_COMPILED_JAVA)
        every { CONTENT_REQUEST_NETWORK_ERROR } returns MOCK_GET_MAIN_FEED_ERROR
        every {
            CONTENT_REQUEST_SWIPE_TO_REFRESH_ERROR
        } returns MOCK_CONTENT_REQUEST_SWIPE_TO_REFRESH_ERROR
        every { TTS_CHAR_LIMIT_ERROR_MESSAGE } returns MOCK_TTS_CHAR_LIMIT_ERROR_MESSAGE
    }

    private fun setToolbarState(feedType: FeedType) = ToolbarState(
            visibility = when (feedType) {
                MAIN -> GONE
                SAVED, DISMISSED -> VISIBLE
            },
            titleRes = when (feedType) {
                MAIN -> R.string.app_name
                SAVED -> R.string.saved
                DISMISSED -> R.string.dismissed
            },
            isActionBarEnabled = when (feedType) {
                SAVED, MAIN -> false
                DISMISSED -> true
            })
}


