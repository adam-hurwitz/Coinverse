package app.coinverse.contentviewmodel.testCases

import app.coinverse.contentviewmodel.LabelContentTest
import app.coinverse.contentviewmodel.mockArticleContent
import app.coinverse.contentviewmodel.mockDbContentListForDay
import app.coinverse.utils.FeedType
import app.coinverse.utils.LCE_STATE
import app.coinverse.utils.Timeframe
import app.coinverse.utils.UserActionType
import java.util.stream.Stream

fun labelContentTestCases() = Stream.of(

        // User - Signed In

        // FeedType - MAIN
        // UserActionType - SAVE
        LabelContentTest(
                isUserSignedIn = true,
                isRealtime = false,
                feedType = FeedType.MAIN,
                timeframe = Timeframe.DAY,
                lceState = LCE_STATE.CONTENT,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockArticleContent,
                isDrawed = true,
                actionType = UserActionType.SAVE,
                adapterPosition = 1),
        LabelContentTest(
                isUserSignedIn = true,
                isRealtime = false,
                feedType = FeedType.MAIN,
                timeframe = Timeframe.DAY,
                lceState = LCE_STATE.ERROR,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockArticleContent,
                isDrawed = true,
                actionType = UserActionType.SAVE,
                adapterPosition = 1),
        // UserActionType - DISMISS
        LabelContentTest(
                isUserSignedIn = true,
                isRealtime = false,
                feedType = FeedType.MAIN,
                timeframe = Timeframe.DAY,
                lceState = LCE_STATE.CONTENT,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockArticleContent,
                isDrawed = true,
                actionType = UserActionType.DISMISS,
                adapterPosition = 1),
        LabelContentTest(
                isUserSignedIn = true,
                isRealtime = false,
                feedType = FeedType.MAIN,
                timeframe = Timeframe.DAY,
                lceState = LCE_STATE.ERROR,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockArticleContent,
                isDrawed = true,
                actionType = UserActionType.DISMISS,
                adapterPosition = 1),

        // FeedType - SAVED
        // UserActionType - DISMISS
        LabelContentTest(
                isUserSignedIn = true,
                isRealtime = false,
                feedType = FeedType.SAVED,
                timeframe = Timeframe.DAY,
                lceState = LCE_STATE.CONTENT,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockArticleContent,
                isDrawed = true,
                actionType = UserActionType.DISMISS,
                adapterPosition = 1),
        LabelContentTest(
                isUserSignedIn = true,
                isRealtime = false,
                feedType = FeedType.SAVED,
                timeframe = Timeframe.DAY,
                lceState = LCE_STATE.ERROR,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockArticleContent,
                isDrawed = true,
                actionType = UserActionType.DISMISS,
                adapterPosition = 1),

        // FeedType - DISMISSED
        // UserActionType - SAVE
        LabelContentTest(
                isUserSignedIn = true,
                isRealtime = false,
                feedType = FeedType.DISMISSED,
                timeframe = Timeframe.DAY,
                lceState = LCE_STATE.CONTENT,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockArticleContent,
                isDrawed = true,
                actionType = UserActionType.SAVE,
                adapterPosition = 1),
        LabelContentTest(
                isUserSignedIn = true,
                isRealtime = false,
                feedType = FeedType.DISMISSED,
                timeframe = Timeframe.DAY,
                lceState = LCE_STATE.ERROR,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockArticleContent,
                isDrawed = true,
                actionType = UserActionType.SAVE,
                adapterPosition = 1),

        // User - Not signed in.

        // FeedType - MAIN
        // UserActionType - SAVE
        LabelContentTest(
                isUserSignedIn = false,
                isRealtime = false,
                feedType = FeedType.MAIN,
                timeframe = Timeframe.DAY,
                lceState = LCE_STATE.CONTENT,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockArticleContent,
                isDrawed = true,
                actionType = UserActionType.SAVE,
                adapterPosition = 1),
        LabelContentTest(
                isUserSignedIn = false,
                isRealtime = false,
                feedType = FeedType.MAIN,
                timeframe = Timeframe.DAY,
                lceState = LCE_STATE.ERROR,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockArticleContent,
                isDrawed = true,
                actionType = UserActionType.SAVE,
                adapterPosition = 1))