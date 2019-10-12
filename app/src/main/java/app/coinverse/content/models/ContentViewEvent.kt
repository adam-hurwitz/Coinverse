package app.coinverse.content.models

import app.coinverse.utils.FeedType
import app.coinverse.utils.Timeframe
import app.coinverse.utils.UserActionType
import com.google.firebase.auth.FirebaseUser

sealed class ContentViewEvent {
    data class FeedLoad(val feedType: FeedType, val timeframe: Timeframe, val isRealtime: Boolean)
        : ContentViewEvent()

    data class FeedLoadComplete(val hasContent: Boolean) : ContentViewEvent()
    data class AudioPlayerLoad(val contentId: String, val filePath: String, val previewImageUrl: String)
        : ContentViewEvent()

    data class SwipeToRefresh(val feedType: FeedType, val timeframe: Timeframe,
                              val isRealtime: Boolean) : ContentViewEvent()

    data class ContentSelected(val position: Int, val content: Content) : ContentViewEvent()
    data class ContentSwipeDrawed(val isDrawed: Boolean) : ContentViewEvent()
    data class ContentSwiped(val feedType: FeedType, val actionType: UserActionType, val position: Int)
        : ContentViewEvent()

    data class ContentLabeled(val feedType: FeedType, val actionType: UserActionType,
                              val user: FirebaseUser?, val position: Int, val content: Content?,
                              val isMainFeedEmptied: Boolean) : ContentViewEvent()

    data class ContentShared(val content: Content) : ContentViewEvent()
    data class ContentSourceOpened(val url: String) : ContentViewEvent()
    class UpdateAds : ContentViewEvent()
}