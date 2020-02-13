package app.coinverse.contentviewmodel.testCases

import app.coinverse.contentviewmodel.FeedLoadTest
import app.coinverse.contentviewmodel.mockDbContentListForAll
import app.coinverse.contentviewmodel.mockDbContentListForDay
import app.coinverse.utils.FeedType
import app.coinverse.utils.FeedType.MAIN
import app.coinverse.utils.Status
import app.coinverse.utils.Status.LOADING
import app.coinverse.utils.Timeframe
import app.coinverse.utils.Timeframe.DAY
import java.util.stream.Stream

fun feedLoadTestCases() = Stream.of(
        // MAIN
        // DAY
        FeedLoadTest(
                isRealtime = false,
                feedType = MAIN,
                timeframe = DAY,
                status = LOADING,
                mockFeedList = mockDbContentListForDay),
        FeedLoadTest(
                isRealtime = false,
                feedType = MAIN,
                timeframe = DAY,
                status = Status.SUCCESS,
                mockFeedList = mockDbContentListForDay),
        FeedLoadTest(
                isRealtime = false,
                feedType = MAIN,
                timeframe = DAY,
                status = Status.SUCCESS,
                mockFeedList = listOf()),
        FeedLoadTest(
                isRealtime = false,
                feedType = MAIN,
                timeframe = DAY,
                status = Status.ERROR,
                mockFeedList = mockDbContentListForDay),

        // ALL
        FeedLoadTest(
                isRealtime = false,
                feedType = MAIN,
                timeframe = Timeframe.ALL,
                status = LOADING,
                mockFeedList = mockDbContentListForAll),
        FeedLoadTest(
                isRealtime = false,
                feedType = MAIN,
                timeframe = Timeframe.ALL,
                status = Status.SUCCESS,
                mockFeedList = mockDbContentListForAll),
        FeedLoadTest(
                isRealtime = false,
                feedType = MAIN,
                timeframe = Timeframe.ALL,
                status = Status.ERROR,
                mockFeedList = mockDbContentListForAll),

        // SAVED
        // DAY
        FeedLoadTest(
                isRealtime = false,
                feedType = FeedType.SAVED,
                timeframe = DAY,
                status = LOADING,
                mockFeedList = mockDbContentListForDay),
        FeedLoadTest(
                isRealtime = false,
                feedType = FeedType.SAVED,
                timeframe = DAY,
                status = Status.SUCCESS,
                mockFeedList = mockDbContentListForDay),
        FeedLoadTest(
                isRealtime = false,
                feedType = FeedType.SAVED,
                timeframe = DAY,
                status = Status.SUCCESS,
                mockFeedList = listOf()),
        FeedLoadTest(
                isRealtime = false,
                feedType = FeedType.SAVED,
                timeframe = DAY,
                status = Status.ERROR,
                mockFeedList = mockDbContentListForDay),

        // DISMISSED
        // DAY
        FeedLoadTest(
                isRealtime = false,
                feedType = FeedType.DISMISSED,
                timeframe = DAY,
                status = LOADING,
                mockFeedList = mockDbContentListForDay),
        FeedLoadTest(
                isRealtime = false,
                feedType = FeedType.DISMISSED,
                timeframe = DAY,
                status = Status.SUCCESS,
                mockFeedList = mockDbContentListForDay),
        FeedLoadTest(
                isRealtime = false,
                feedType = FeedType.DISMISSED,
                timeframe = DAY,
                status = Status.SUCCESS,
                mockFeedList = listOf()),
        FeedLoadTest(
                isRealtime = false,
                feedType = FeedType.DISMISSED,
                timeframe = DAY,
                status = Status.ERROR,
                mockFeedList = mockDbContentListForDay))