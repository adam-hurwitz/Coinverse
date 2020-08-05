package app.coinverse.feedViewModel

import android.content.SharedPreferences
import android.widget.ProgressBar.GONE
import android.widget.ProgressBar.VISIBLE
import app.coinverse.R
import app.coinverse.analytics.Analytics
import app.coinverse.feed.FeedViewModel
import app.coinverse.feed.data.FeedRepository
import app.coinverse.feed.state.FeedView
import app.coinverse.feed.state.FeedViewIntent
import app.coinverse.feed.state.FeedViewIntentType.LabelContent
import app.coinverse.feed.state.FeedViewIntentType.OpenContent
import app.coinverse.feed.state.FeedViewIntentType.SwipeContent
import app.coinverse.feed.state.FeedViewIntentType.SwipeToRefresh
import app.coinverse.feed.state.FeedViewState
import app.coinverse.feed.state.FeedViewState.ClearAdjacentAds
import app.coinverse.feed.state.FeedViewState.Feed
import app.coinverse.feed.state.FeedViewState.UpdateAds
import app.coinverse.utils.CONSTANTS_CLASS_COMPILED_JAVA
import app.coinverse.utils.CONTENT_LABEL_ERROR
import app.coinverse.utils.CONTENT_REQUEST_NETWORK_ERROR
import app.coinverse.utils.CONTENT_REQUEST_SWIPE_TO_REFRESH_ERROR
import app.coinverse.utils.ContentType.ARTICLE
import app.coinverse.utils.Event
import app.coinverse.utils.FeedType
import app.coinverse.utils.FeedType.DISMISSED
import app.coinverse.utils.FeedType.MAIN
import app.coinverse.utils.FeedType.SAVED
import app.coinverse.utils.MOCK_CONTENT_LABEL_ERROR
import app.coinverse.utils.MOCK_CONTENT_REQUEST_SWIPE_TO_REFRESH_ERROR
import app.coinverse.utils.MOCK_GET_MAIN_FEED_ERROR
import app.coinverse.utils.MOCK_TTS_CHAR_LIMIT_ERROR_MESSAGE
import app.coinverse.utils.PLAYER_OPEN_STATUS_KEY
import app.coinverse.utils.Status
import app.coinverse.utils.Status.LOADING
import app.coinverse.utils.Status.SUCCESS
import app.coinverse.utils.TTS_CHAR_LIMIT_ERROR_MESSAGE
import app.coinverse.utils.ToolbarState
import app.coinverse.utils.asPagedList
import app.coinverse.utils.mockEditContentLabels
import app.coinverse.utils.mockFeedPosition
import app.coinverse.utils.mockGetAudiocast
import app.coinverse.utils.mockGetMainFeedList
import app.coinverse.utils.mockGetMainFeedRoom
import com.crashlytics.android.Crashlytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@ExperimentalCoroutinesApi
@ExtendWith(FeedViewTestExtension::class)
class FeedViewTest(
        val testCoroutineDispatcher: TestCoroutineDispatcher,
        val testCoroutineScope: TestCoroutineScope
) {
    private fun FeedViewTestCaseStream() = feedViewTestCaseStream()
    private lateinit var test: FeedViewTestCase
    private var firebaseUser: FirebaseUser? = null
    private var isContentSwipe = false

    val intent = FeedViewIntent()
    val sharedPreferences = mockkClass(SharedPreferences::class)
    val repository = mockkClass(FeedRepository::class)
    val firebaseAuth = mockkClass(FirebaseAuth::class)
    val analytics = mockkClass(Analytics::class)

    @ParameterizedTest
    @MethodSource("FeedViewTestCaseStream")
    fun `FeedView`(feedViewTestCase: FeedViewTestCase) = testCoroutineDispatcher.runBlockingTest {
        test = feedViewTestCase
        mockComponents(test)
        if (firebaseAuth.currentUser != null) firebaseUser = firebaseAuth.currentUser
        else firebaseUser = null
        val viewModel = FeedViewModel(
                coroutineScopeProvider = testCoroutineScope,
                feedType = test.feedLoadIntent.feedType,
                timeframe = test.feedLoadIntent.timeframe,
                isRealtime = test.feedLoadIntent.isRealtime,
                sharedPreferences = sharedPreferences,
                repository = repository,
                analytics = analytics
        )
        viewModel.bindIntents(object : FeedView {
            override fun initState() = intent.initState
            override fun loadFromNetwork() = intent.loadFromNetwork.filterNotNull()
            override fun swipeToRefresh() = intent.swipeToRefresh.filterNotNull()
            override fun openContent() = intent.openContent.filterNotNull()
            override fun openContentSource() = intent.openContentSource.filterNotNull()
            override fun swipeContent() = intent.swipeContent.filterNotNull()
            override fun labelContent() = intent.labelContent.filterNotNull()
            override fun shareContent() = intent.shareContent.filterNotNull()
            override fun updateAds() = intent.updateAds.filterNotNull()
            override fun render(state: FeedViewState) {
                when (state) {
                    is Feed -> testRenderFeed(state)
                    is FeedViewState.SwipeToRefresh -> testRenderSwipeToRefresh(state)
                    is FeedViewState.OpenContent -> testRenderOpenContent(state)
                    is FeedViewState.OpenContentSource ->
                        assertThat(state).isEqualTo(test.openContentSourceState)
                    is FeedViewState.SwipeContent -> testRenderSwipeContent(state)
                    is UpdateAds -> assertThat(state.javaClass).hasSameClassAs(UpdateAds::class.java)
                    is ClearAdjacentAds -> assertThat(state).isEqualTo(test.clearAdjacentAdsState)
                    is FeedViewState.ShareContent -> assertThat(state).isEqualTo(test.shareContentState)
                }
            }
        })
        // LoadFromNetwork intent
        intent.loadFromNetwork.value = true
        // Swipe-To-Refresh intent
        if (test.feedLoadIntent.feedType == MAIN)
            intent.swipeToRefresh.value = Event(SwipeToRefresh(
                    feedType = test.feedLoadIntent.feedType,
                    timeframe = test.feedLoadIntent.timeframe,
                    isRealtime = test.feedLoadIntent.isRealtime)
            )
        // OpenContent intent
        if (test.mockFeedList.isNotEmpty())
            intent.openContent.value = Event(OpenContent(
                    content = test.mockContent!!,
                    position = mockFeedPosition
            ))
        // Label content - SwipeContent and LabelContent intents
        if (test.status != LOADING) {
            isContentSwipe = true
            // Partial swipe of content
            intent.swipeContent.value = Event(SwipeContent(
                    feedType = test.swipeContentIntent!!.feedType,
                    actionType = test.swipeContentIntent!!.actionType,
                    position = test.swipeContentIntent!!.position,
                    isSwiped = false
            ))
            // Full swipe of content to apply label
            intent.swipeContent.value = Event(test.swipeContentIntent)
        }
        // OpenContentSource and ShareContent intents
        if (isFeedPopulated()) {
            intent.openContentSource.value = Event(test.mockContent!!.url)
            intent.shareContent.value = Event(test.mockContent)
        }
        // UpdateAds intent
        intent.updateAds.value = Event(true)
        verifyTest(test)
    }

    private fun testRenderFeed(feed: Feed) {
        val expect = Feed(
                toolbarState = setToolbarState(test.feedLoadIntent.feedType),
                feed = test.mockFeedList.asPagedList(),
                error = test.error
        )
        assertThat(feed).isEqualToComparingFieldByField(expect)
    }

    private fun testRenderSwipeToRefresh(state: FeedViewState.SwipeToRefresh) {
        // Normal Swipe-To-Refesh
        if (!isContentSwipe) assertThat(state).isEqualTo(test.swipeToRefreshState)
        // Swipe-To-Refesh disabled when swiping content in the newsfeed.
        else assertThat(state).isEqualTo(FeedViewState.SwipeToRefresh(false))
    }


    private fun testRenderOpenContent(state: FeedViewState.OpenContent) {
        assertThat(state).isEqualToComparingOnlyGivenFields(
                test.openContentState,
                "isLoading",
                "position",
                "contentId",
                "filePath",
                "error"
        )
        assertThat(state.content).isEqualToComparingOnlyGivenFields(
                test.openContentState!!.content,
                "id",
                "contentType"
        )
    }

    private fun testRenderSwipeContent(state: FeedViewState.SwipeContent) {
        assertThat(state).isEqualTo(test.swipeContentState)
        intent.labelContent.value = Event(LabelContent(
                feedType = test.swipeContentIntent!!.feedType,
                actionType = test.swipeContentIntent!!.actionType,
                user = firebaseUser,
                position = test.swipeContentIntent!!.position,
                content = test.mockContent,
                isMainFeedEmptied = test.mockFeedList.isEmpty()
        ))
        isContentSwipe = false
    }

    private fun isFeedPopulated() = test.status == SUCCESS && test.mockFeedList.isNotEmpty()

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

    private fun mockComponents(test: FeedViewTestCase) {
        every { sharedPreferences.getBoolean(PLAYER_OPEN_STATUS_KEY, false) } returns false
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
        // Label Content
        if (test.status != LOADING) {
            every {
                repository.editContentLabels(
                        feedType = test.swipeContentIntent!!.feedType,
                        actionType = test.swipeContentIntent.actionType,
                        content = test.mockContent,
                        user = any(),
                        position = test.swipeContentIntent.position
                )
            } returns mockEditContentLabels(test)
            // Analytics
            every {
                analytics.updateActionAnalytics(
                        actionType = test.swipeContentIntent!!.actionType,
                        content = test.mockContent!!,
                        user = any()
                )
            } returns mockk(relaxed = true)
            every {
                analytics.updateFeedEmptiedActionsAndAnalytics(any())
            } returns mockk(relaxed = true)
            every {
                analytics.labelContentFirebaseAnalytics(test.mockContent!!)
            } returns mockk(relaxed = true)
        }
        // Firebase
        if (test.isLoggedIn)
            every { firebaseAuth.currentUser } returns mockk(relaxed = true)
        else every { firebaseAuth.currentUser } returns null
        mockkStatic(FirebaseRemoteConfig::class)
        every { FirebaseRemoteConfig.getInstance() } returns mockk(relaxed = true)
        mockkStatic(Crashlytics::class)
        every { Crashlytics.log(any(), any(), any()) } returns Unit
        // FirebaseRemoteConfig - Constant values
        mockkStatic(CONSTANTS_CLASS_COMPILED_JAVA)
        every { CONTENT_REQUEST_NETWORK_ERROR } returns MOCK_GET_MAIN_FEED_ERROR
        every {
            CONTENT_REQUEST_SWIPE_TO_REFRESH_ERROR
        } returns MOCK_CONTENT_REQUEST_SWIPE_TO_REFRESH_ERROR
        every { TTS_CHAR_LIMIT_ERROR_MESSAGE } returns MOCK_TTS_CHAR_LIMIT_ERROR_MESSAGE
        every { CONTENT_LABEL_ERROR } returns MOCK_CONTENT_LABEL_ERROR
    }

    private fun verifyTest(test: FeedViewTestCase) {
        coVerify {
            when (test.feedLoadIntent.feedType) {
                MAIN -> {
                    repository.getMainFeedNetwork(any(), any())
                    if (test.status == LOADING || test.status == Status.ERROR)
                        repository.getMainFeedRoom(any())
                }
                SAVED, DISMISSED -> repository.getLabelFeedRoom(any())
            }
            if (test.openContentState != null && test.mockContent?.contentType == ARTICLE) {
                repository.getAudiocast(OpenContent(test.mockContent, mockFeedPosition))
            }
            if (test.swipeContentIntent != null && test.isLoggedIn)
                repository.editContentLabels(
                        feedType = test.swipeContentIntent.feedType,
                        actionType = test.swipeContentIntent.actionType,
                        content = test.mockContent,
                        user = firebaseUser!!,
                        position = test.swipeContentIntent.position)
        }
        confirmVerified(repository)
    }
}