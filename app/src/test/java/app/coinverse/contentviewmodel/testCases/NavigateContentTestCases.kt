package app.coinverse.contentviewmodel.testCases

import app.coinverse.contentviewmodel.NavigateContentTest
import app.coinverse.contentviewmodel.mockArticleContent
import app.coinverse.contentviewmodel.mockDbContentListForDay
import app.coinverse.contentviewmodel.mockYouTubeContent
import app.coinverse.utils.FeedType
import app.coinverse.utils.Timeframe
import java.util.stream.Stream

fun navigateContentTestCases() = Stream.of(
        // Article
        NavigateContentTest(
                isRealtime = false,
                feedType = FeedType.MAIN,
                timeframe = Timeframe.DAY,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockArticleContent),
        NavigateContentTest(
                isRealtime = false,
                feedType = FeedType.SAVED,
                timeframe = Timeframe.DAY,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockArticleContent),
        NavigateContentTest(
                isRealtime = false,
                feedType = FeedType.DISMISSED,
                timeframe = Timeframe.DAY,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockArticleContent),

        // YouTube
        NavigateContentTest(
                isRealtime = false,
                feedType = FeedType.MAIN,
                timeframe = Timeframe.DAY,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockYouTubeContent),
        NavigateContentTest(
                isRealtime = false,
                feedType = FeedType.SAVED,
                timeframe = Timeframe.DAY,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockYouTubeContent),
        NavigateContentTest(
                isRealtime = false,
                feedType = FeedType.DISMISSED,
                timeframe = Timeframe.DAY,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockYouTubeContent))