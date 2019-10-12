package app.coinverse.contentviewmodel

import app.coinverse.content.models.Content
import app.coinverse.utils.FeedType
import app.coinverse.utils.LCE_STATE
import app.coinverse.utils.Timeframe
import app.coinverse.utils.UserActionType

data class FeedLoadContentTest(
        val isRealtime: Boolean,
        val feedType: FeedType,
        val timeframe: Timeframe,
        val lceState: LCE_STATE,
        val mockFeedList: List<Content>)

data class PlayContentTest(
        val isRealtime: Boolean,
        val feedType: FeedType,
        val timeframe: Timeframe,
        val lceState: LCE_STATE,
        val mockFeedList: List<Content>,
        val mockContent: Content,
        val mockPosition: Int,
        val mockFilePath: String,
        val mockGetAudiocastError: String,
        val mockPreviewImageUrl: String,
        val mockPreviewImageByteArray: ByteArray)

data class LabelContentTest(
        val isUserSignedIn: Boolean,
        val isRealtime: Boolean,
        val feedType: FeedType,
        val timeframe: Timeframe,
        val lceState: LCE_STATE,
        val mockFeedList: List<Content>,
        val mockContent: Content,
        val isDrawed: Boolean,
        val actionType: UserActionType,
        val adapterPosition: Int)

data class NavigateContentTest(
        val isRealtime: Boolean,
        val feedType: FeedType,
        val timeframe: Timeframe,
        val mockFeedList: List<Content>,
        val mockContent: Content)
