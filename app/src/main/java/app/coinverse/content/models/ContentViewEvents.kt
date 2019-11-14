package app.coinverse.content.models

import app.coinverse.utils.FeedType
import app.coinverse.utils.Timeframe
import app.coinverse.utils.UserActionType
import com.google.firebase.auth.FirebaseUser

sealed class ContentViewEvents {
    data class FeedLoad(val feedType: FeedType, val timeframe: Timeframe, val isRealtime: Boolean)
        : ContentViewEvents()

    data class FeedLoadComplete(val hasContent: Boolean) : ContentViewEvents()
    data class AudioPlayerLoad(val contentId: String, val filePath: String, val previewImageUrl: String)
        : ContentViewEvents()

    data class SwipeToRefresh(val feedType: FeedType, val timeframe: Timeframe,
                              val isRealtime: Boolean) : ContentViewEvents()

    data class ContentSelected(val position: Int, val content: Content) : ContentViewEvents()
    data class ContentSwipeDrawed(val isDrawed: Boolean) : ContentViewEvents()
    data class ContentSwiped(val feedType: FeedType, val actionType: UserActionType, val position: Int)
        : ContentViewEvents()

    data class ContentLabeled(val feedType: FeedType, val actionType: UserActionType,
                              val user: FirebaseUser?, val position: Int, val content: Content?,
                              val isMainFeedEmptied: Boolean) : ContentViewEvents()

    data class ContentShared(val content: Content) : ContentViewEvents()
    data class ContentSourceOpened(val url: String) : ContentViewEvents()
    class UpdateAds : ContentViewEvents()
}