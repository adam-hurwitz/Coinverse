package app.coinverse.contentviewmodel

import app.coinverse.utils.*
import app.coinverse.utils.FeedType.*
import app.coinverse.utils.LCE_STATE.*
import app.coinverse.utils.Timeframe.ALL
import app.coinverse.utils.Timeframe.DAY
import java.util.stream.Stream

// TODO - Create multiple files.

fun feedLoadTestCases() = Stream.of(
        // MAIN
        // DAY
        FeedLoadContentTest(
                isRealtime = false,
                feedType = MAIN,
                timeframe = DAY,
                lceState = LOADING,
                mockFeedList = mockDbContentListForDay),
        FeedLoadContentTest(
                isRealtime = false,
                feedType = MAIN,
                timeframe = DAY,
                lceState = CONTENT,
                mockFeedList = mockDbContentListForDay),
        FeedLoadContentTest(
                isRealtime = false,
                feedType = MAIN,
                timeframe = DAY,
                lceState = CONTENT,
                mockFeedList = listOf()),
        // TODO - Add FeedLoad Error, SwipeToRefreshError
        FeedLoadContentTest(
                isRealtime = false,
                feedType = MAIN,
                timeframe = DAY,
                lceState = ERROR,
                mockFeedList = mockDbContentListForDay),

        // ALL
        FeedLoadContentTest(
                isRealtime = false,
                feedType = MAIN,
                timeframe = ALL,
                lceState = LOADING,
                mockFeedList = mockDbContentListForAll),
        FeedLoadContentTest(
                isRealtime = false,
                feedType = MAIN,
                timeframe = ALL,
                lceState = CONTENT,
                mockFeedList = mockDbContentListForAll),
        FeedLoadContentTest(
                isRealtime = false,
                feedType = MAIN,
                timeframe = ALL,
                lceState = ERROR,
                mockFeedList = mockDbContentListForAll),

        // SAVED
        // DAY
        FeedLoadContentTest(
                isRealtime = false,
                feedType = SAVED,
                timeframe = DAY,
                lceState = LOADING,
                mockFeedList = mockDbContentListForDay),
        FeedLoadContentTest(
                isRealtime = false,
                feedType = SAVED,
                timeframe = DAY,
                lceState = CONTENT,
                mockFeedList = mockDbContentListForDay),
        FeedLoadContentTest(
                isRealtime = false,
                feedType = SAVED,
                timeframe = DAY,
                lceState = CONTENT,
                mockFeedList = listOf()),
        FeedLoadContentTest(
                isRealtime = false,
                feedType = SAVED,
                timeframe = DAY,
                lceState = ERROR,
                mockFeedList = mockDbContentListForDay),

        // DISMISSED
        // DAY
        FeedLoadContentTest(
                isRealtime = false,
                feedType = DISMISSED,
                timeframe = DAY,
                lceState = LOADING,
                mockFeedList = mockDbContentListForDay),
        FeedLoadContentTest(
                isRealtime = false,
                feedType = DISMISSED,
                timeframe = DAY,
                lceState = CONTENT,
                mockFeedList = mockDbContentListForDay),
        FeedLoadContentTest(
                isRealtime = false,
                feedType = DISMISSED,
                timeframe = DAY,
                lceState = CONTENT,
                mockFeedList = listOf()),
        FeedLoadContentTest(
                isRealtime = false,
                feedType = DISMISSED,
                timeframe = DAY,
                lceState = ERROR,
                mockFeedList = mockDbContentListForDay))

fun playContentTestCases() = Stream.of(
        // ARTICLE
        // MAIN
        PlayContentTest(
                isRealtime = false,
                feedType = MAIN,
                timeframe = DAY,
                lceState = LOADING,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockArticleContent,
                mockPosition = 0,
                mockFilePath = MOCK_TXT_FILE_PATH,
                mockGetAudiocastError = "",
                mockPreviewImageUrl = "",
                mockPreviewImageByteArray = ByteArray(0)),
        PlayContentTest(
                isRealtime = false,
                feedType = MAIN,
                timeframe = DAY,
                lceState = CONTENT,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockArticleContent,
                mockPosition = 0,
                mockFilePath = MOCK_TXT_FILE_PATH,
                mockGetAudiocastError = "",
                mockPreviewImageUrl = MOCK_PREVIEW_IMAGE,
                mockPreviewImageByteArray = ByteArray(1)),
        PlayContentTest(
                isRealtime = false,
                feedType = MAIN,
                timeframe = DAY,
                lceState = ERROR,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockArticleContent,
                mockPosition = 0,
                mockFilePath = MOCK_TTS_CHAR_LIMIT_ERROR,
                mockGetAudiocastError = MOCK_TTS_CHAR_LIMIT_ERROR_MESSAGE,
                mockPreviewImageUrl = "",
                mockPreviewImageByteArray = ByteArray(0)),
        PlayContentTest(
                isRealtime = false,
                feedType = MAIN,
                timeframe = DAY,
                lceState = ERROR,
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
                feedType = SAVED,
                timeframe = DAY,
                lceState = LOADING,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockArticleContent,
                mockPosition = 0,
                mockFilePath = MOCK_TXT_FILE_PATH,
                mockGetAudiocastError = "",
                mockPreviewImageUrl = MOCK_PREVIEW_IMAGE,
                mockPreviewImageByteArray = ByteArray(1)),
        PlayContentTest(
                isRealtime = false,
                feedType = SAVED,
                timeframe = DAY,
                lceState = CONTENT,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockArticleContent,
                mockPosition = 0, mockFilePath = MOCK_TXT_FILE_PATH,
                mockGetAudiocastError = "",
                mockPreviewImageUrl = MOCK_PREVIEW_IMAGE,
                mockPreviewImageByteArray = ByteArray(1)),
        PlayContentTest(
                isRealtime = false,
                feedType = SAVED,
                timeframe = DAY,
                lceState = ERROR,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockArticleContent,
                mockPosition = 0,
                mockFilePath = MOCK_TTS_CHAR_LIMIT_ERROR,
                mockGetAudiocastError = MOCK_TTS_CHAR_LIMIT_ERROR_MESSAGE,
                mockPreviewImageUrl = "",
                mockPreviewImageByteArray = ByteArray(0)),
        PlayContentTest(
                isRealtime = false,
                feedType = SAVED,
                timeframe = DAY,
                lceState = ERROR,
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
                feedType = DISMISSED,
                timeframe = DAY,
                lceState = LOADING,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockArticleContent,
                mockPosition = 0,
                mockFilePath = MOCK_TXT_FILE_PATH,
                mockGetAudiocastError = "",
                mockPreviewImageUrl = MOCK_PREVIEW_IMAGE,
                mockPreviewImageByteArray = ByteArray(1)),
        PlayContentTest(
                isRealtime = false,
                feedType = DISMISSED,
                timeframe = DAY,
                lceState = CONTENT,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockArticleContent,
                mockPosition = 0,
                mockFilePath = MOCK_TXT_FILE_PATH,
                mockGetAudiocastError = "",
                mockPreviewImageUrl = MOCK_PREVIEW_IMAGE,
                mockPreviewImageByteArray = ByteArray(1)),
        PlayContentTest(
                isRealtime = false,
                feedType = DISMISSED, timeframe = DAY,
                lceState = ERROR,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockArticleContent,
                mockPosition = 0,
                mockFilePath = MOCK_TTS_CHAR_LIMIT_ERROR,
                mockGetAudiocastError = MOCK_TTS_CHAR_LIMIT_ERROR_MESSAGE,
                mockPreviewImageUrl = MOCK_PREVIEW_IMAGE,
                mockPreviewImageByteArray = ByteArray(0)),
        PlayContentTest(
                isRealtime = false,
                feedType = DISMISSED,
                timeframe = DAY,
                lceState = ERROR,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockArticleContent,
                mockPosition = 0, mockFilePath = "",
                mockGetAudiocastError = MOCK_CONTENT_PLAY_ERROR,
                mockPreviewImageUrl = MOCK_PREVIEW_IMAGE,
                mockPreviewImageByteArray = ByteArray(0)),

        // YOUTUBE
        PlayContentTest(
                isRealtime = false,
                feedType = MAIN,
                timeframe = DAY,
                lceState = CONTENT,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockYouTubeContent,
                mockPosition = 0,
                mockFilePath = "",
                mockGetAudiocastError = "",
                mockPreviewImageUrl = MOCK_PREVIEW_IMAGE,
                mockPreviewImageByteArray = ByteArray(0)),
        PlayContentTest(
                isRealtime = false,
                feedType = SAVED,
                timeframe = DAY,
                lceState = CONTENT,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockYouTubeContent,
                mockPosition = 0,
                mockFilePath = "",
                mockGetAudiocastError = "",
                mockPreviewImageUrl = MOCK_PREVIEW_IMAGE,
                mockPreviewImageByteArray = ByteArray(0)),
        PlayContentTest(
                isRealtime = false,
                feedType = DISMISSED,
                timeframe = DAY,
                lceState = CONTENT,
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
                feedType = MAIN,
                timeframe = DAY,
                lceState = CONTENT,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockArticleContent,
                isDrawed = true,
                actionType = UserActionType.SAVE,
                adapterPosition = 1),
        LabelContentTest(
                isUserSignedIn = true,
                isRealtime = false,
                feedType = MAIN,
                timeframe = DAY,
                lceState = ERROR,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockArticleContent,
                isDrawed = true,
                actionType = UserActionType.SAVE,
                adapterPosition = 1),
        // UserActionType - DISMISS
        LabelContentTest(
                isUserSignedIn = true,
                isRealtime = false,
                feedType = MAIN,
                timeframe = DAY,
                lceState = CONTENT,
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
                lceState = ERROR,
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
                feedType = SAVED,
                timeframe = DAY,
                lceState = CONTENT,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockArticleContent,
                isDrawed = true,
                actionType = UserActionType.DISMISS,
                adapterPosition = 1),
        LabelContentTest(
                isUserSignedIn = true,
                isRealtime = false,
                feedType = SAVED,
                timeframe = DAY,
                lceState = ERROR,
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
                feedType = DISMISSED,
                timeframe = DAY,
                lceState = CONTENT,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockArticleContent,
                isDrawed = true,
                actionType = UserActionType.SAVE,
                adapterPosition = 1),
        LabelContentTest(
                isUserSignedIn = true,
                isRealtime = false,
                feedType = DISMISSED,
                timeframe = DAY,
                lceState = ERROR,
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
                feedType = MAIN,
                timeframe = DAY,
                lceState = CONTENT,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockArticleContent,
                isDrawed = true,
                actionType = UserActionType.SAVE,
                adapterPosition = 1),
        LabelContentTest(
                isUserSignedIn = false,
                isRealtime = false,
                feedType = MAIN,
                timeframe = DAY,
                lceState = ERROR,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockArticleContent,
                isDrawed = true,
                actionType = UserActionType.SAVE,
                adapterPosition = 1))

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
                feedType = SAVED,
                timeframe = DAY,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockArticleContent),
        NavigateContentTest(
                isRealtime = false,
                feedType = DISMISSED,
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
                feedType = SAVED,
                timeframe = DAY,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockYouTubeContent),
        NavigateContentTest(
                isRealtime = false,
                feedType = DISMISSED,
                timeframe = DAY,
                mockFeedList = mockDbContentListForDay,
                mockContent = mockYouTubeContent))