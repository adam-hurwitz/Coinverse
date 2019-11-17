package app.coinverse.contentviewmodel.testCases

import app.coinverse.contentviewmodel.FeedLoadContentTest
import app.coinverse.contentviewmodel.mockDbContentListForAll
import app.coinverse.contentviewmodel.mockDbContentListForDay
import app.coinverse.utils.FeedType
import app.coinverse.utils.LCE_STATE
import app.coinverse.utils.Timeframe
import java.util.stream.Stream

fun feedLoadTestCases() = Stream.of(
        // MAIN
        // DAY
        FeedLoadContentTest(
                isRealtime = false,
                feedType = FeedType.MAIN,
                timeframe = Timeframe.DAY,
                lceState = LCE_STATE.LOADING,
                mockFeedList = mockDbContentListForDay),
        FeedLoadContentTest(
                isRealtime = false,
                feedType = FeedType.MAIN,
                timeframe = Timeframe.DAY,
                lceState = LCE_STATE.CONTENT,
                mockFeedList = mockDbContentListForDay),
        FeedLoadContentTest(
                isRealtime = false,
                feedType = FeedType.MAIN,
                timeframe = Timeframe.DAY,
                lceState = LCE_STATE.CONTENT,
                mockFeedList = listOf()),
        // TODO - Add FeedLoad Error, SwipeToRefreshError
        FeedLoadContentTest(
                isRealtime = false,
                feedType = FeedType.MAIN,
                timeframe = Timeframe.DAY,
                lceState = LCE_STATE.ERROR,
                mockFeedList = mockDbContentListForDay),

        // ALL
        FeedLoadContentTest(
                isRealtime = false,
                feedType = FeedType.MAIN,
                timeframe = Timeframe.ALL,
                lceState = LCE_STATE.LOADING,
                mockFeedList = mockDbContentListForAll),
        FeedLoadContentTest(
                isRealtime = false,
                feedType = FeedType.MAIN,
                timeframe = Timeframe.ALL,
                lceState = LCE_STATE.CONTENT,
                mockFeedList = mockDbContentListForAll),
        FeedLoadContentTest(
                isRealtime = false,
                feedType = FeedType.MAIN,
                timeframe = Timeframe.ALL,
                lceState = LCE_STATE.ERROR,
                mockFeedList = mockDbContentListForAll),

        // SAVED
        // DAY
        FeedLoadContentTest(
                isRealtime = false,
                feedType = FeedType.SAVED,
                timeframe = Timeframe.DAY,
                lceState = LCE_STATE.LOADING,
                mockFeedList = mockDbContentListForDay),
        FeedLoadContentTest(
                isRealtime = false,
                feedType = FeedType.SAVED,
                timeframe = Timeframe.DAY,
                lceState = LCE_STATE.CONTENT,
                mockFeedList = mockDbContentListForDay),
        FeedLoadContentTest(
                isRealtime = false,
                feedType = FeedType.SAVED,
                timeframe = Timeframe.DAY,
                lceState = LCE_STATE.CONTENT,
                mockFeedList = listOf()),
        FeedLoadContentTest(
                isRealtime = false,
                feedType = FeedType.SAVED,
                timeframe = Timeframe.DAY,
                lceState = LCE_STATE.ERROR,
                mockFeedList = mockDbContentListForDay),

        // DISMISSED
        // DAY
        FeedLoadContentTest(
                isRealtime = false,
                feedType = FeedType.DISMISSED,
                timeframe = Timeframe.DAY,
                lceState = LCE_STATE.LOADING,
                mockFeedList = mockDbContentListForDay),
        FeedLoadContentTest(
                isRealtime = false,
                feedType = FeedType.DISMISSED,
                timeframe = Timeframe.DAY,
                lceState = LCE_STATE.CONTENT,
                mockFeedList = mockDbContentListForDay),
        FeedLoadContentTest(
                isRealtime = false,
                feedType = FeedType.DISMISSED,
                timeframe = Timeframe.DAY,
                lceState = LCE_STATE.CONTENT,
                mockFeedList = listOf()),
        FeedLoadContentTest(
                isRealtime = false,
                feedType = FeedType.DISMISSED,
                timeframe = Timeframe.DAY,
                lceState = LCE_STATE.ERROR,
                mockFeedList = mockDbContentListForDay))