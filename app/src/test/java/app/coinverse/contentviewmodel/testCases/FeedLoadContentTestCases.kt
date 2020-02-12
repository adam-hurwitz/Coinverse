package app.coinverse.contentviewmodel.testCases

import app.coinverse.contentviewmodel.FeedLoadTest
import app.coinverse.contentviewmodel.mockDbContentListForAll
import app.coinverse.contentviewmodel.mockDbContentListForDay
import app.coinverse.utils.FeedType
import app.coinverse.utils.LCE_STATE
import app.coinverse.utils.Timeframe
import java.util.stream.Stream

fun feedLoadTestCases() = Stream.of(
        // MAIN
        // DAY
        FeedLoadTest(
                isRealtime = false,
                feedType = FeedType.MAIN,
                timeframe = Timeframe.DAY,
                lceState = LCE_STATE.LOADING,
                mockFeedList = mockDbContentListForDay),
        FeedLoadTest(
                isRealtime = false,
                feedType = FeedType.MAIN,
                timeframe = Timeframe.DAY,
                lceState = LCE_STATE.CONTENT,
                mockFeedList = mockDbContentListForDay),
        FeedLoadTest(
                isRealtime = false,
                feedType = FeedType.MAIN,
                timeframe = Timeframe.DAY,
                lceState = LCE_STATE.CONTENT,
                mockFeedList = listOf()),
        FeedLoadTest(
                isRealtime = false,
                feedType = FeedType.MAIN,
                timeframe = Timeframe.DAY,
                lceState = LCE_STATE.ERROR,
                mockFeedList = mockDbContentListForDay),

        // ALL
        FeedLoadTest(
                isRealtime = false,
                feedType = FeedType.MAIN,
                timeframe = Timeframe.ALL,
                lceState = LCE_STATE.LOADING,
                mockFeedList = mockDbContentListForAll),
        FeedLoadTest(
                isRealtime = false,
                feedType = FeedType.MAIN,
                timeframe = Timeframe.ALL,
                lceState = LCE_STATE.CONTENT,
                mockFeedList = mockDbContentListForAll),
        FeedLoadTest(
                isRealtime = false,
                feedType = FeedType.MAIN,
                timeframe = Timeframe.ALL,
                lceState = LCE_STATE.ERROR,
                mockFeedList = mockDbContentListForAll),

        // SAVED
        // DAY
        FeedLoadTest(
                isRealtime = false,
                feedType = FeedType.SAVED,
                timeframe = Timeframe.DAY,
                lceState = LCE_STATE.LOADING,
                mockFeedList = mockDbContentListForDay),
        FeedLoadTest(
                isRealtime = false,
                feedType = FeedType.SAVED,
                timeframe = Timeframe.DAY,
                lceState = LCE_STATE.CONTENT,
                mockFeedList = mockDbContentListForDay),
        FeedLoadTest(
                isRealtime = false,
                feedType = FeedType.SAVED,
                timeframe = Timeframe.DAY,
                lceState = LCE_STATE.CONTENT,
                mockFeedList = listOf()),
        FeedLoadTest(
                isRealtime = false,
                feedType = FeedType.SAVED,
                timeframe = Timeframe.DAY,
                lceState = LCE_STATE.ERROR,
                mockFeedList = mockDbContentListForDay),

        // DISMISSED
        // DAY
        FeedLoadTest(
                isRealtime = false,
                feedType = FeedType.DISMISSED,
                timeframe = Timeframe.DAY,
                lceState = LCE_STATE.LOADING,
                mockFeedList = mockDbContentListForDay),
        FeedLoadTest(
                isRealtime = false,
                feedType = FeedType.DISMISSED,
                timeframe = Timeframe.DAY,
                lceState = LCE_STATE.CONTENT,
                mockFeedList = mockDbContentListForDay),
        FeedLoadTest(
                isRealtime = false,
                feedType = FeedType.DISMISSED,
                timeframe = Timeframe.DAY,
                lceState = LCE_STATE.CONTENT,
                mockFeedList = listOf()),
        FeedLoadTest(
                isRealtime = false,
                feedType = FeedType.DISMISSED,
                timeframe = Timeframe.DAY,
                lceState = LCE_STATE.ERROR,
                mockFeedList = mockDbContentListForDay))