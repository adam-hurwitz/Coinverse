package app.coinverse.feedViewModel.testCases

import app.coinverse.feedViewModel.NavigateContentTest
import app.coinverse.feedViewModel.mockArticleContent
import app.coinverse.feedViewModel.mockDbContentListForDay
import app.coinverse.feedViewModel.mockYouTubeContent
import app.coinverse.utils.FeedType
import app.coinverse.utils.FeedType.MAIN
import app.coinverse.utils.Timeframe.DAY
import java.util.stream.Stream

fun navigateContentTestCases() = Stream.of(
        // Article
        NavigateContentTest(
                isRealtime = false,
                feedType = MAIN,
                timeframe = DAY,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockArticleContent),
        NavigateContentTest(
                isRealtime = false,
                feedType = FeedType.SAVED,
                timeframe = DAY,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockArticleContent),
        NavigateContentTest(
                isRealtime = false,
                feedType = FeedType.DISMISSED,
                timeframe = DAY,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockArticleContent),

        // YouTube
        NavigateContentTest(
                isRealtime = false,
                feedType = MAIN,
                timeframe = DAY,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockYouTubeContent),
        NavigateContentTest(
                isRealtime = false,
                feedType = FeedType.SAVED,
                timeframe = DAY,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockYouTubeContent),
        NavigateContentTest(
                isRealtime = false,
                feedType = FeedType.DISMISSED,
                timeframe = DAY,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockYouTubeContent))