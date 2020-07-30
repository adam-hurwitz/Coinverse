package app.coinverse.feedViewModel.testCases

import app.coinverse.feed.state.FeedViewState.OpenContent
import app.coinverse.feed.state.FeedViewState.SwipeToRefresh
import app.coinverse.feedViewModel.FeedViewTest
import app.coinverse.feedViewModel.mockArticleContent
import app.coinverse.feedViewModel.mockContentId
import app.coinverse.feedViewModel.mockDbContentListForDay
import app.coinverse.feedViewModel.mockFeedPosition
import app.coinverse.feedViewModel.mockNetworkContentListForDay
import app.coinverse.utils.FeedType.DISMISSED
import app.coinverse.utils.FeedType.MAIN
import app.coinverse.utils.FeedType.SAVED
import app.coinverse.utils.MOCK_CONTENT_REQUEST_SWIPE_TO_REFRESH_ERROR
import app.coinverse.utils.MOCK_GET_MAIN_FEED_ERROR
import app.coinverse.utils.MOCK_TTS_CHAR_LIMIT_ERROR_MESSAGE
import app.coinverse.utils.MOCK_TXT_FILE_PATH
import app.coinverse.utils.Status.ERROR
import app.coinverse.utils.Status.LOADING
import app.coinverse.utils.Status.SUCCESS
import app.coinverse.utils.Timeframe.DAY
import java.util.stream.Stream

fun feedViewTestCases() = Stream.of(
        // MAIN
        // DAY
        FeedViewTest(
                status = LOADING,
                isRealtime = false,
                feedType = MAIN,
                timeframe = DAY,
                mockFeedList = mockDbContentListForDay,
                swipeToRefresh = SwipeToRefresh(true),
                // Todo: YT
                openContent = OpenContent(
                        isLoading = true,
                        position = mockFeedPosition,
                        contentId = mockContentId
                )
        ),
        FeedViewTest(
                status = SUCCESS,
                isRealtime = false,
                feedType = MAIN,
                timeframe = DAY,
                mockFeedList = mockNetworkContentListForDay,
                swipeToRefresh = SwipeToRefresh(false),
                openContent = OpenContent(
                        isLoading = false,
                        position = mockFeedPosition,
                        contentId = mockContentId,
                        // Todo: YT
                        content = mockArticleContent,
                        filePath = MOCK_TXT_FILE_PATH
                )
        ),
        FeedViewTest(
                status = SUCCESS,
                isRealtime = false,
                feedType = MAIN,
                timeframe = DAY,
                mockFeedList = listOf(),
                swipeToRefresh = SwipeToRefresh(false),
                openContent = OpenContent(
                        isLoading = false,
                        position = mockFeedPosition,
                        contentId = mockContentId,
                        // Todo: YT
                        content = mockArticleContent,
                        filePath = MOCK_TXT_FILE_PATH
                )
        ),
        FeedViewTest(
                status = ERROR,
                isRealtime = false,
                feedType = MAIN,
                timeframe = DAY,
                mockFeedList = mockDbContentListForDay,
                error = MOCK_GET_MAIN_FEED_ERROR,
                swipeToRefresh = SwipeToRefresh(
                        isEnabled = false,
                        error = MOCK_CONTENT_REQUEST_SWIPE_TO_REFRESH_ERROR
                ),
                openContent = OpenContent(
                        isLoading = false,
                        position = mockFeedPosition,
                        contentId = mockContentId,
                        // Todo: YT
                        error = MOCK_TTS_CHAR_LIMIT_ERROR_MESSAGE
                )
        ),
        // SAVED
        // DAY
        FeedViewTest(
                status = LOADING,
                isRealtime = false,
                feedType = SAVED,
                timeframe = DAY,
                mockFeedList = mockDbContentListForDay
        ),
        FeedViewTest(
                status = SUCCESS,
                isRealtime = false,
                feedType = SAVED,
                timeframe = DAY,
                mockFeedList = mockNetworkContentListForDay
        ),
        FeedViewTest(
                status = SUCCESS,
                isRealtime = false,
                feedType = SAVED,
                timeframe = DAY,
                mockFeedList = listOf()
        ),
        FeedViewTest(
                status = ERROR,
                isRealtime = false,
                feedType = SAVED,
                timeframe = DAY,
                mockFeedList = mockDbContentListForDay
        ),

        // DISMISSED
        // DAY
        FeedViewTest(
                status = LOADING,
                isRealtime = false,
                feedType = DISMISSED,
                timeframe = DAY,
                mockFeedList = mockDbContentListForDay
        ),
        FeedViewTest(
                status = SUCCESS,
                isRealtime = false,
                feedType = DISMISSED,
                timeframe = DAY,
                mockFeedList = mockNetworkContentListForDay
        ),
        FeedViewTest(
                status = SUCCESS,
                isRealtime = false,
                feedType = DISMISSED,
                timeframe = DAY,
                mockFeedList = listOf()
        ),
        FeedViewTest(
                status = ERROR,
                isRealtime = false,
                feedType = DISMISSED,
                timeframe = DAY,
                mockFeedList = mockDbContentListForDay)
)