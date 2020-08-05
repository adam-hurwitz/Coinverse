package app.coinverse.feedViewModel

import app.coinverse.feed.state.FeedViewIntentType.FeedLoad
import app.coinverse.feed.state.FeedViewIntentType.SwipeContent
import app.coinverse.feed.state.FeedViewState.*
import app.coinverse.utils.FeedType.DISMISSED
import app.coinverse.utils.FeedType.MAIN
import app.coinverse.utils.FeedType.SAVED
import app.coinverse.utils.MOCK_CONTENT_LABEL_ERROR
import app.coinverse.utils.MOCK_CONTENT_REQUEST_SWIPE_TO_REFRESH_ERROR
import app.coinverse.utils.MOCK_GET_MAIN_FEED_ERROR
import app.coinverse.utils.MOCK_TTS_CHAR_LIMIT_ERROR_MESSAGE
import app.coinverse.utils.MOCK_TXT_FILE_PATH
import app.coinverse.utils.Status.ERROR
import app.coinverse.utils.Status.LOADING
import app.coinverse.utils.Status.SUCCESS
import app.coinverse.utils.Timeframe.DAY
import app.coinverse.utils.UserActionType.DISMISS
import app.coinverse.utils.UserActionType.SAVE
import app.coinverse.utils.mockArticleContent
import app.coinverse.utils.mockContentId
import app.coinverse.utils.mockDbContentListForDay
import app.coinverse.utils.mockFeedPosition
import app.coinverse.utils.mockNetworkContentListForDay
import app.coinverse.utils.mockYouTubeContent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.util.stream.Stream

@ExperimentalCoroutinesApi
fun feedViewTestCaseStream() = Stream.of(
        // MAIN
        // DAY
        FeedViewTestCase(
                status = LOADING,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockArticleContent,
                feedLoadIntent = FeedLoad(
                        feedType = MAIN,
                        timeframe = DAY,
                        isRealtime = false
                ),
                swipeToRefreshState = SwipeToRefresh(true),
                openContentState = OpenContent(
                        isLoading = true,
                        position = mockFeedPosition,
                        contentId = mockContentId
                )
        ),
        FeedViewTestCase(
                status = SUCCESS,
                mockFeedList = mockNetworkContentListForDay,
                mockContent = mockArticleContent,
                feedLoadIntent = FeedLoad(
                        feedType = MAIN,
                        timeframe = DAY,
                        isRealtime = false
                ),
                swipeContentIntent = SwipeContent(
                        feedType = MAIN,
                        actionType = SAVE,
                        position = mockFeedPosition,
                        isSwiped = true
                ),
                swipeToRefreshState = SwipeToRefresh(false),
                openContentState = OpenContent(
                        isLoading = false,
                        position = mockFeedPosition,
                        contentId = mockContentId,
                        content = mockArticleContent,
                        filePath = MOCK_TXT_FILE_PATH
                ),
                openContentSourceState = OpenContentSource(mockArticleContent.url),
                swipeContentState = SwipeContent(
                        actionType = SAVE,
                        position = mockFeedPosition
                ),
                shareContentState = ShareContent(mockArticleContent),
                clearAdjacentAdsState = ClearAdjacentAds(mockFeedPosition)
        ),
        FeedViewTestCase(
                status = SUCCESS,
                mockFeedList = mockNetworkContentListForDay,
                mockContent = mockArticleContent,
                feedLoadIntent = FeedLoad(
                        feedType = MAIN,
                        timeframe = DAY,
                        isRealtime = false
                ),
                swipeContentIntent = SwipeContent(
                        feedType = MAIN,
                        actionType = DISMISS,
                        position = mockFeedPosition,
                        isSwiped = true
                ),
                swipeToRefreshState = SwipeToRefresh(false),
                openContentState = OpenContent(
                        isLoading = false,
                        position = mockFeedPosition,
                        contentId = mockContentId,
                        content = mockArticleContent,
                        filePath = MOCK_TXT_FILE_PATH
                ),
                openContentSourceState = OpenContentSource(mockArticleContent.url),
                swipeContentState = SwipeContent(
                        actionType = DISMISS,
                        position = mockFeedPosition
                ),
                shareContentState = ShareContent(mockArticleContent),
                clearAdjacentAdsState = ClearAdjacentAds(mockFeedPosition)
        ),
        FeedViewTestCase(
                status = SUCCESS,
                mockFeedList = mockNetworkContentListForDay,
                mockContent = mockYouTubeContent,
                feedLoadIntent = FeedLoad(
                        feedType = MAIN,
                        timeframe = DAY,
                        isRealtime = false
                ),
                swipeContentIntent = SwipeContent(
                        feedType = MAIN,
                        actionType = SAVE,
                        position = mockFeedPosition,
                        isSwiped = true
                ),
                swipeToRefreshState = SwipeToRefresh(false),
                openContentState = OpenContent(
                        isLoading = false,
                        position = mockFeedPosition,
                        contentId = mockContentId,
                        content = mockYouTubeContent
                ),
                openContentSourceState = OpenContentSource(mockYouTubeContent.url),
                swipeContentState = SwipeContent(
                        actionType = SAVE,
                        position = mockFeedPosition
                ),
                shareContentState = ShareContent(mockYouTubeContent),
                clearAdjacentAdsState = ClearAdjacentAds(mockFeedPosition)
        ),
        FeedViewTestCase(
                status = SUCCESS,
                mockFeedList = mockNetworkContentListForDay,
                isLoggedIn = false,
                mockContent = mockYouTubeContent,
                feedLoadIntent = FeedLoad(
                        feedType = MAIN,
                        timeframe = DAY,
                        isRealtime = false
                ),
                swipeContentIntent = SwipeContent(
                        feedType = MAIN,
                        actionType = SAVE,
                        position = mockFeedPosition,
                        isSwiped = true
                ),
                swipeToRefreshState = SwipeToRefresh(false),
                openContentState = OpenContent(
                        isLoading = false,
                        position = mockFeedPosition,
                        contentId = mockContentId,
                        content = mockYouTubeContent
                ),
                openContentSourceState = OpenContentSource(mockYouTubeContent.url),
                swipeContentState = SwipeContent(
                        actionType = SAVE,
                        position = mockFeedPosition
                ),
                shareContentState = ShareContent(mockYouTubeContent),
                clearAdjacentAdsState = ClearAdjacentAds(mockFeedPosition)
        ),
        FeedViewTestCase(
                status = SUCCESS,
                mockFeedList = listOf(),
                feedLoadIntent = FeedLoad(
                        feedType = MAIN,
                        timeframe = DAY,
                        isRealtime = false
                ),
                swipeContentIntent = SwipeContent(
                        feedType = MAIN,
                        actionType = SAVE,
                        position = mockFeedPosition,
                        isSwiped = true
                ),
                swipeContentState = SwipeContent(
                        actionType = SAVE,
                        position = mockFeedPosition
                ),
                clearAdjacentAdsState = ClearAdjacentAds(mockFeedPosition)
        ),
        FeedViewTestCase(
                status = ERROR,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockArticleContent,
                feedLoadIntent = FeedLoad(
                        feedType = MAIN,
                        timeframe = DAY,
                        isRealtime = false
                ),
                swipeContentIntent = SwipeContent(
                        feedType = MAIN,
                        actionType = SAVE,
                        position = mockFeedPosition,
                        isSwiped = true
                ),
                error = MOCK_GET_MAIN_FEED_ERROR,
                swipeToRefreshState = SwipeToRefresh(
                        isEnabled = false,
                        error = MOCK_CONTENT_REQUEST_SWIPE_TO_REFRESH_ERROR
                ),
                openContentState = OpenContent(
                        isLoading = false,
                        position = mockFeedPosition,
                        contentId = mockContentId,
                        error = MOCK_TTS_CHAR_LIMIT_ERROR_MESSAGE
                ),
                openContentSourceState = OpenContentSource(mockArticleContent.url),
                swipeContentState = SwipeContent(
                        actionType = SAVE,
                        position = mockFeedPosition
                ),
                clearAdjacentAdsState = ClearAdjacentAds(
                        position = app.coinverse.utils.ERROR,
                        error = MOCK_CONTENT_LABEL_ERROR
                )
        ),
        // SAVED
        // DAY
        FeedViewTestCase(
                status = LOADING,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockArticleContent,
                feedLoadIntent = FeedLoad(
                        feedType = SAVED,
                        timeframe = DAY,
                        isRealtime = false
                ),
                openContentState = OpenContent(
                        isLoading = true,
                        position = mockFeedPosition,
                        contentId = mockContentId
                )
        ),
        FeedViewTestCase(
                status = SUCCESS,
                mockFeedList = mockNetworkContentListForDay,
                mockContent = mockArticleContent,
                feedLoadIntent = FeedLoad(
                        feedType = SAVED,
                        timeframe = DAY,
                        isRealtime = false
                ),
                swipeContentIntent = SwipeContent(
                        feedType = SAVED,
                        actionType = SAVE,
                        position = mockFeedPosition,
                        isSwiped = true
                ),
                openContentState = OpenContent(
                        isLoading = false,
                        position = mockFeedPosition,
                        contentId = mockContentId,
                        content = mockArticleContent,
                        filePath = MOCK_TXT_FILE_PATH
                ),
                openContentSourceState = OpenContentSource(mockArticleContent.url),
                swipeContentState = SwipeContent(
                        actionType = SAVE,
                        position = mockFeedPosition
                ),
                shareContentState = ShareContent(mockArticleContent),
                clearAdjacentAdsState = ClearAdjacentAds(mockFeedPosition)
        ),
        FeedViewTestCase(
                status = SUCCESS,
                mockFeedList = mockNetworkContentListForDay,
                mockContent = mockArticleContent,
                feedLoadIntent = FeedLoad(
                        feedType = SAVED,
                        timeframe = DAY,
                        isRealtime = false
                ),
                swipeContentIntent = SwipeContent(
                        feedType = SAVED,
                        actionType = DISMISS,
                        position = mockFeedPosition,
                        isSwiped = true
                ),
                openContentState = OpenContent(
                        isLoading = false,
                        position = mockFeedPosition,
                        contentId = mockContentId,
                        content = mockArticleContent,
                        filePath = MOCK_TXT_FILE_PATH
                ),
                openContentSourceState = OpenContentSource(mockArticleContent.url),
                swipeContentState = SwipeContent(
                        actionType = DISMISS,
                        position = mockFeedPosition
                ),
                shareContentState = ShareContent(mockArticleContent),
                clearAdjacentAdsState = ClearAdjacentAds(mockFeedPosition)
        ),
        FeedViewTestCase(
                status = SUCCESS,
                mockFeedList = mockNetworkContentListForDay,
                mockContent = mockYouTubeContent,
                feedLoadIntent = FeedLoad(
                        feedType = SAVED,
                        timeframe = DAY,
                        isRealtime = false
                ),
                swipeContentIntent = SwipeContent(
                        feedType = SAVED,
                        actionType = SAVE,
                        position = mockFeedPosition,
                        isSwiped = true
                ),
                openContentState = OpenContent(
                        isLoading = false,
                        position = mockFeedPosition,
                        contentId = mockContentId,
                        content = mockYouTubeContent
                ),
                openContentSourceState = OpenContentSource(mockYouTubeContent.url),
                swipeContentState = SwipeContent(
                        actionType = SAVE,
                        position = mockFeedPosition
                ),
                shareContentState = ShareContent(mockYouTubeContent),
                clearAdjacentAdsState = ClearAdjacentAds(mockFeedPosition)
        ),
        FeedViewTestCase(
                status = SUCCESS,
                isLoggedIn = false,
                mockFeedList = mockNetworkContentListForDay,
                mockContent = mockYouTubeContent,
                feedLoadIntent = FeedLoad(
                        feedType = SAVED,
                        timeframe = DAY,
                        isRealtime = false
                ),
                swipeContentIntent = SwipeContent(
                        feedType = SAVED,
                        actionType = SAVE,
                        position = mockFeedPosition,
                        isSwiped = true
                ),
                openContentState = OpenContent(
                        isLoading = false,
                        position = mockFeedPosition,
                        contentId = mockContentId,
                        content = mockYouTubeContent
                ),
                openContentSourceState = OpenContentSource(mockYouTubeContent.url),
                swipeContentState = SwipeContent(
                        actionType = SAVE,
                        position = mockFeedPosition
                ),
                shareContentState = ShareContent(mockYouTubeContent),
                clearAdjacentAdsState = ClearAdjacentAds(mockFeedPosition)
        ),
        FeedViewTestCase(
                status = SUCCESS,
                mockFeedList = listOf(),
                feedLoadIntent = FeedLoad(
                        feedType = SAVED,
                        timeframe = DAY,
                        isRealtime = false
                ),
                swipeContentIntent = SwipeContent(
                        feedType = SAVED,
                        actionType = SAVE,
                        position = mockFeedPosition,
                        isSwiped = true
                ),
                swipeContentState = SwipeContent(
                        actionType = SAVE,
                        position = mockFeedPosition
                ),
                clearAdjacentAdsState = ClearAdjacentAds(mockFeedPosition)
        ),
        FeedViewTestCase(
                status = ERROR,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockArticleContent,
                feedLoadIntent = FeedLoad(
                        feedType = SAVED,
                        timeframe = DAY,
                        isRealtime = false
                ),
                swipeContentIntent = SwipeContent(
                        feedType = MAIN,
                        actionType = SAVE,
                        position = mockFeedPosition,
                        isSwiped = true
                ),
                openContentState = OpenContent(
                        isLoading = false,
                        position = mockFeedPosition,
                        contentId = mockContentId,
                        error = MOCK_TTS_CHAR_LIMIT_ERROR_MESSAGE
                ),
                swipeContentState = SwipeContent(
                        actionType = SAVE,
                        position = mockFeedPosition
                ),
                clearAdjacentAdsState = ClearAdjacentAds(
                        position = app.coinverse.utils.ERROR,
                        error = MOCK_CONTENT_LABEL_ERROR
                )
        ),

        // DISMISSED
        // DAY
        FeedViewTestCase(
                status = LOADING,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockArticleContent,
                feedLoadIntent = FeedLoad(
                        feedType = DISMISSED,
                        timeframe = DAY,
                        isRealtime = false
                ),
                openContentState = OpenContent(
                        isLoading = true,
                        position = mockFeedPosition,
                        contentId = mockContentId
                )
        ),
        FeedViewTestCase(
                status = SUCCESS,
                mockFeedList = mockNetworkContentListForDay,
                mockContent = mockArticleContent,
                feedLoadIntent = FeedLoad(
                        feedType = DISMISSED,
                        timeframe = DAY,
                        isRealtime = false
                ),
                swipeContentIntent = SwipeContent(
                        feedType = DISMISSED,
                        actionType = SAVE,
                        position = mockFeedPosition,
                        isSwiped = true
                ),
                openContentState = OpenContent(
                        isLoading = false,
                        position = mockFeedPosition,
                        contentId = mockContentId,
                        content = mockArticleContent,
                        filePath = MOCK_TXT_FILE_PATH
                ),
                openContentSourceState = OpenContentSource(mockArticleContent.url),
                swipeContentState = SwipeContent(
                        actionType = SAVE,
                        position = mockFeedPosition
                ),
                shareContentState = ShareContent(mockArticleContent),
                clearAdjacentAdsState = ClearAdjacentAds(mockFeedPosition)
        ),
        FeedViewTestCase(
                status = SUCCESS,
                mockFeedList = mockNetworkContentListForDay,
                mockContent = mockArticleContent,
                feedLoadIntent = FeedLoad(
                        feedType = DISMISSED,
                        timeframe = DAY,
                        isRealtime = false
                ),
                swipeContentIntent = SwipeContent(
                        feedType = DISMISSED,
                        actionType = DISMISS,
                        position = mockFeedPosition,
                        isSwiped = true
                ),
                openContentState = OpenContent(
                        isLoading = false,
                        position = mockFeedPosition,
                        contentId = mockContentId,
                        content = mockArticleContent,
                        filePath = MOCK_TXT_FILE_PATH
                ),
                openContentSourceState = OpenContentSource(mockArticleContent.url),
                swipeContentState = SwipeContent(
                        actionType = DISMISS,
                        position = mockFeedPosition
                ),
                shareContentState = ShareContent(mockArticleContent),
                clearAdjacentAdsState = ClearAdjacentAds(mockFeedPosition)
        ),
        FeedViewTestCase(
                status = SUCCESS,
                mockFeedList = mockNetworkContentListForDay,
                mockContent = mockYouTubeContent,
                feedLoadIntent = FeedLoad(
                        feedType = DISMISSED,
                        timeframe = DAY,
                        isRealtime = false
                ),
                swipeContentIntent = SwipeContent(
                        feedType = DISMISSED,
                        actionType = SAVE,
                        position = mockFeedPosition,
                        isSwiped = true
                ),
                openContentState = OpenContent(
                        isLoading = false,
                        position = mockFeedPosition,
                        contentId = mockContentId,
                        content = mockYouTubeContent
                ),
                openContentSourceState = OpenContentSource(mockYouTubeContent.url),
                swipeContentState = SwipeContent(
                        actionType = SAVE,
                        position = mockFeedPosition
                ),
                shareContentState = ShareContent(mockYouTubeContent),
                clearAdjacentAdsState = ClearAdjacentAds(mockFeedPosition)
        ),
        FeedViewTestCase(
                status = SUCCESS,
                isLoggedIn = false,
                mockFeedList = mockNetworkContentListForDay,
                mockContent = mockYouTubeContent,
                feedLoadIntent = FeedLoad(
                        feedType = DISMISSED,
                        timeframe = DAY,
                        isRealtime = false
                ),
                swipeContentIntent = SwipeContent(
                        feedType = DISMISSED,
                        actionType = SAVE,
                        position = mockFeedPosition,
                        isSwiped = true
                ),
                openContentState = OpenContent(
                        isLoading = false,
                        position = mockFeedPosition,
                        contentId = mockContentId,
                        content = mockYouTubeContent
                ),
                openContentSourceState = OpenContentSource(mockYouTubeContent.url),
                swipeContentState = SwipeContent(
                        actionType = SAVE,
                        position = mockFeedPosition
                ),
                shareContentState = ShareContent(mockYouTubeContent),
                clearAdjacentAdsState = ClearAdjacentAds(mockFeedPosition)
        ),
        FeedViewTestCase(
                status = SUCCESS,
                mockFeedList = listOf(),
                feedLoadIntent = FeedLoad(
                        feedType = DISMISSED,
                        timeframe = DAY,
                        isRealtime = false
                ),
                swipeContentIntent = SwipeContent(
                        feedType = DISMISSED,
                        actionType = SAVE,
                        position = mockFeedPosition,
                        isSwiped = true
                ),
                swipeContentState = SwipeContent(
                        actionType = SAVE,
                        position = mockFeedPosition
                ),
                clearAdjacentAdsState = ClearAdjacentAds(mockFeedPosition)
        ),
        FeedViewTestCase(
                status = ERROR,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockArticleContent,
                feedLoadIntent = FeedLoad(
                        feedType = DISMISSED,
                        timeframe = DAY,
                        isRealtime = false
                ),
                swipeContentIntent = SwipeContent(
                        feedType = MAIN,
                        actionType = SAVE,
                        position = mockFeedPosition,
                        isSwiped = true
                ),
                openContentState = OpenContent(
                        isLoading = false,
                        position = mockFeedPosition,
                        contentId = mockContentId,
                        error = MOCK_TTS_CHAR_LIMIT_ERROR_MESSAGE
                ),
                swipeContentState = SwipeContent(
                        actionType = SAVE,
                        position = mockFeedPosition
                ),
                clearAdjacentAdsState = ClearAdjacentAds(
                        position = app.coinverse.utils.ERROR,
                        error = MOCK_CONTENT_LABEL_ERROR
                )
        )
)