package app.coinverse.contentviewmodel.testCases

import app.coinverse.contentviewmodel.LabelContentTest
import app.coinverse.contentviewmodel.mockArticleContent
import app.coinverse.contentviewmodel.mockDbContentListForDay
import app.coinverse.utils.FeedType
import app.coinverse.utils.FeedType.MAIN
import app.coinverse.utils.Status
import app.coinverse.utils.Status.SUCCESS
import app.coinverse.utils.Timeframe.DAY
import app.coinverse.utils.UserActionType
import app.coinverse.utils.UserActionType.SAVE
import java.util.stream.Stream

fun labelContentTestCases() = Stream.of(

        // User: Signed In

        // FeedType: MAIN
        // UserActionType: SAVE
        LabelContentTest(
                isUserSignedIn = true,
                isRealtime = false,
                feedType = MAIN,
                timeframe = DAY,
                status = SUCCESS,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockArticleContent,
                isDrawed = true,
                actionType = SAVE,
                adapterPosition = 1),
        LabelContentTest(
                isUserSignedIn = true,
                isRealtime = false,
                feedType = MAIN,
                timeframe = DAY,
                status = Status.ERROR,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockArticleContent,
                isDrawed = true,
                actionType = SAVE,
                adapterPosition = 1),
        // UserActionType - DISMISS
        LabelContentTest(
                isUserSignedIn = true,
                isRealtime = false,
                feedType = MAIN,
                timeframe = DAY,
                status = SUCCESS,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockArticleContent,
                isDrawed = true,
                actionType = UserActionType.DISMISS,
                adapterPosition = 1),
        LabelContentTest(
                isUserSignedIn = true,
                isRealtime = false,
                feedType = MAIN,
                timeframe = DAY,
                status = Status.ERROR,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockArticleContent,
                isDrawed = true,
                actionType = UserActionType.DISMISS,
                adapterPosition = 1),

        // FeedType: SAVED
        // UserActionType: DISMISS
        LabelContentTest(
                isUserSignedIn = true,
                isRealtime = false,
                feedType = FeedType.SAVED,
                timeframe = DAY,
                status = SUCCESS,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockArticleContent,
                isDrawed = true,
                actionType = UserActionType.DISMISS,
                adapterPosition = 1),
        LabelContentTest(
                isUserSignedIn = true,
                isRealtime = false,
                feedType = FeedType.SAVED,
                timeframe = DAY,
                status = Status.ERROR,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockArticleContent,
                isDrawed = true,
                actionType = UserActionType.DISMISS,
                adapterPosition = 1),

        // FeedType: DISMISSED
        // UserActionType: SAVE
        LabelContentTest(
                isUserSignedIn = true,
                isRealtime = false,
                feedType = FeedType.DISMISSED,
                timeframe = DAY,
                status = SUCCESS,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockArticleContent,
                isDrawed = true,
                actionType = SAVE,
                adapterPosition = 1),
        LabelContentTest(
                isUserSignedIn = true,
                isRealtime = false,
                feedType = FeedType.DISMISSED,
                timeframe = DAY,
                status = Status.ERROR,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockArticleContent,
                isDrawed = true,
                actionType = SAVE,
                adapterPosition = 1),

        // User: Signed out

        // FeedType: MAIN
        // UserActionType: SAVE
        LabelContentTest(
                isUserSignedIn = false,
                isRealtime = false,
                feedType = MAIN,
                timeframe = DAY,
                status = SUCCESS,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockArticleContent,
                isDrawed = true,
                actionType = SAVE,
                adapterPosition = 1),
        LabelContentTest(
                isUserSignedIn = false,
                isRealtime = false,
                feedType = MAIN,
                timeframe = DAY,
                status = Status.ERROR,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockArticleContent,
                isDrawed = true,
                actionType = SAVE,
                adapterPosition = 1))