package app.coinverse.content.models

import app.coinverse.content.models.ContentViewEventType.*
import app.coinverse.utils.FeedType
import app.coinverse.utils.Timeframe
import app.coinverse.utils.UserActionType
import com.google.firebase.auth.FirebaseUser

interface ContentViewEvents {
    fun feedLoad(event: FeedLoad)
    fun feedLoadComplete(event: FeedLoadComplete)
    fun audioPlayerLoad(event: AudioPlayerLoad)
    fun swipeToRefresh(event: SwipeToRefresh)
    fun contentSelected(event: ContentSelected)
    fun contentSwipeDrawed(event: ContentSwipeDrawed)
    fun contentSwiped(event: ContentSwiped)
    fun contentLabeled(event: ContentViewEventType.ContentLabeled)
    fun contentShared(event: ContentShared)
    fun contentSourceOpened(event: ContentSourceOpened)
    fun updateAds(event: UpdateAds)
}

sealed class ContentViewEventType {
    data class FeedLoad(val feedType: FeedType, val timeframe: Timeframe, val isRealtime: Boolean) : ContentViewEventType()
    data class FeedLoadComplete(val hasContent: Boolean) : ContentViewEventType()
    data class AudioPlayerLoad(val contentId: String, val filePath: String, val previewImageUrl: String) : ContentViewEventType()
    data class SwipeToRefresh(val feedType: FeedType, val timeframe: Timeframe,
                              val isRealtime: Boolean) : ContentViewEventType()

    data class ContentSelected(val position: Int, val content: Content) : ContentViewEventType()
    data class ContentSwipeDrawed(val isDrawed: Boolean) : ContentViewEventType()
    data class ContentSwiped(val feedType: FeedType, val actionType: UserActionType, val position: Int) : ContentViewEventType()

    data class ContentLabeled(val feedType: FeedType, val actionType: UserActionType,
                              val user: FirebaseUser?, val position: Int, val content: Content?,
                              val isMainFeedEmptied: Boolean) : ContentViewEventType()

    data class ContentShared(val content: Content) : ContentViewEventType()
    data class ContentSourceOpened(val url: String) : ContentViewEventType()
    class UpdateAds : ContentViewEventType()
}