package app.coinverse.feedViewModel

import app.coinverse.feed.Content
import app.coinverse.feed.state.FeedViewState.OpenContent
import app.coinverse.feed.state.FeedViewState.SwipeToRefresh
import app.coinverse.utils.FeedType
import app.coinverse.utils.Status
import app.coinverse.utils.Timeframe
import app.coinverse.utils.UserActionType

// Expect values
data class FeedViewTest(
        val status: Status,
        val isRealtime: Boolean,
        val feedType: FeedType,
        val timeframe: Timeframe,
        val mockFeedList: List<Content>,
        val error: String? = null,
        val swipeToRefresh: SwipeToRefresh = SwipeToRefresh(),
        val openContent: OpenContent? = null
)

data class PlayContentTest(
        val isRealtime: Boolean,
        val feedType: FeedType,
        val timeframe: Timeframe,
        val status: Status,
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
        val status: Status,
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
