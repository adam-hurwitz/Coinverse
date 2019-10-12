package app.coinverse.contentviewmodel

import app.coinverse.utils.FeedType
import app.coinverse.utils.LCE_STATE
import app.coinverse.utils.Timeframe
import app.coinverse.utils.UserActionType
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

fun playContentTestCases() = Stream.of(
        // ARTICLE
        // MAIN
        PlayContentTest(
                isRealtime = false,
                feedType = FeedType.MAIN,
                timeframe = Timeframe.DAY,
                lceState = LCE_STATE.LOADING,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockArticleContent,
                mockPosition = 0,
                mockFilePath = MOCK_TXT_FILE_PATH,
                mockGetAudiocastError = "",
                mockPreviewImageUrl = "",
                mockPreviewImageByteArray = ByteArray(0)),
        PlayContentTest(
                isRealtime = false,
                feedType = FeedType.MAIN,
                timeframe = Timeframe.DAY,
                lceState = LCE_STATE.CONTENT,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockArticleContent,
                mockPosition = 0,
                mockFilePath = MOCK_TXT_FILE_PATH,
                mockGetAudiocastError = "",
                mockPreviewImageUrl = MOCK_PREVIEW_IMAGE,
                mockPreviewImageByteArray = ByteArray(1)),
        PlayContentTest(
                isRealtime = false,
                feedType = FeedType.MAIN,
                timeframe = Timeframe.DAY,
                lceState = LCE_STATE.ERROR,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockArticleContent,
                mockPosition = 0,
                mockFilePath = MOCK_TTS_CHAR_LIMIT_ERROR,
                mockGetAudiocastError = MOCK_TTS_CHAR_LIMIT_ERROR_MESSAGE,
                mockPreviewImageUrl = "",
                mockPreviewImageByteArray = ByteArray(0)),
        PlayContentTest(
                isRealtime = false,
                feedType = FeedType.MAIN,
                timeframe = Timeframe.DAY,
                lceState = LCE_STATE.ERROR,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockArticleContent,
                mockPosition = 0,
                mockFilePath = "",
                mockGetAudiocastError = MOCK_CONTENT_PLAY_ERROR,
                mockPreviewImageUrl = "",
                mockPreviewImageByteArray = ByteArray(0)),
        // SAVED
        PlayContentTest(
                isRealtime = false,
                feedType = FeedType.SAVED,
                timeframe = Timeframe.DAY,
                lceState = LCE_STATE.LOADING,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockArticleContent,
                mockPosition = 0,
                mockFilePath = MOCK_TXT_FILE_PATH,
                mockGetAudiocastError = "",
                mockPreviewImageUrl = MOCK_PREVIEW_IMAGE,
                mockPreviewImageByteArray = ByteArray(1)),
        PlayContentTest(
                isRealtime = false,
                feedType = FeedType.SAVED,
                timeframe = Timeframe.DAY,
                lceState = LCE_STATE.CONTENT,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockArticleContent,
                mockPosition = 0, mockFilePath = MOCK_TXT_FILE_PATH,
                mockGetAudiocastError = "",
                mockPreviewImageUrl = MOCK_PREVIEW_IMAGE,
                mockPreviewImageByteArray = ByteArray(1)),
        PlayContentTest(
                isRealtime = false,
                feedType = FeedType.SAVED,
                timeframe = Timeframe.DAY,
                lceState = LCE_STATE.ERROR,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockArticleContent,
                mockPosition = 0,
                mockFilePath = MOCK_TTS_CHAR_LIMIT_ERROR,
                mockGetAudiocastError = MOCK_TTS_CHAR_LIMIT_ERROR_MESSAGE,
                mockPreviewImageUrl = "",
                mockPreviewImageByteArray = ByteArray(0)),
        PlayContentTest(
                isRealtime = false,
                feedType = FeedType.SAVED,
                timeframe = Timeframe.DAY,
                lceState = LCE_STATE.ERROR,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockArticleContent,
                mockPosition = 0,
                mockFilePath = "",
                mockGetAudiocastError = MOCK_CONTENT_PLAY_ERROR,
                mockPreviewImageUrl = MOCK_PREVIEW_IMAGE,
                mockPreviewImageByteArray = ByteArray(0)),
        // DISMISSED
        PlayContentTest(
                isRealtime = false,
                feedType = FeedType.DISMISSED,
                timeframe = Timeframe.DAY,
                lceState = LCE_STATE.LOADING,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockArticleContent,
                mockPosition = 0,
                mockFilePath = MOCK_TXT_FILE_PATH,
                mockGetAudiocastError = "",
                mockPreviewImageUrl = MOCK_PREVIEW_IMAGE,
                mockPreviewImageByteArray = ByteArray(1)),
        PlayContentTest(
                isRealtime = false,
                feedType = FeedType.DISMISSED,
                timeframe = Timeframe.DAY,
                lceState = LCE_STATE.CONTENT,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockArticleContent,
                mockPosition = 0,
                mockFilePath = MOCK_TXT_FILE_PATH,
                mockGetAudiocastError = "",
                mockPreviewImageUrl = MOCK_PREVIEW_IMAGE,
                mockPreviewImageByteArray = ByteArray(1)),
        PlayContentTest(
                isRealtime = false,
                feedType = FeedType.DISMISSED, timeframe = Timeframe.DAY,
                lceState = LCE_STATE.ERROR,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockArticleContent,
                mockPosition = 0,
                mockFilePath = MOCK_TTS_CHAR_LIMIT_ERROR,
                mockGetAudiocastError = MOCK_TTS_CHAR_LIMIT_ERROR_MESSAGE,
                mockPreviewImageUrl = MOCK_PREVIEW_IMAGE,
                mockPreviewImageByteArray = ByteArray(0)),
        PlayContentTest(
                isRealtime = false,
                feedType = FeedType.DISMISSED,
                timeframe = Timeframe.DAY,
                lceState = LCE_STATE.ERROR,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockArticleContent,
                mockPosition = 0, mockFilePath = "",
                mockGetAudiocastError = MOCK_CONTENT_PLAY_ERROR,
                mockPreviewImageUrl = MOCK_PREVIEW_IMAGE,
                mockPreviewImageByteArray = ByteArray(0)),

        // YOUTUBE
        PlayContentTest(
                isRealtime = false,
                feedType = FeedType.MAIN,
                timeframe = Timeframe.DAY,
                lceState = LCE_STATE.CONTENT,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockYouTubeContent,
                mockPosition = 0,
                mockFilePath = "",
                mockGetAudiocastError = "",
                mockPreviewImageUrl = MOCK_PREVIEW_IMAGE,
                mockPreviewImageByteArray = ByteArray(0)),
        PlayContentTest(
                isRealtime = false,
                feedType = FeedType.SAVED,
                timeframe = Timeframe.DAY,
                lceState = LCE_STATE.CONTENT,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockYouTubeContent,
                mockPosition = 0,
                mockFilePath = "",
                mockGetAudiocastError = "",
                mockPreviewImageUrl = MOCK_PREVIEW_IMAGE,
                mockPreviewImageByteArray = ByteArray(0)),
        PlayContentTest(
                isRealtime = false,
                feedType = FeedType.DISMISSED,
                timeframe = Timeframe.DAY,
                lceState = LCE_STATE.CONTENT,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockYouTubeContent,
                mockPosition = 0,
                mockFilePath = "",
                mockGetAudiocastError = "",
                mockPreviewImageUrl = MOCK_PREVIEW_IMAGE,
                mockPreviewImageByteArray = ByteArray(0)))

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