package app.coinverse.feed.models

import app.coinverse.feed.models.FeedViewEventType.*
import app.coinverse.utils.FeedType
import app.coinverse.utils.Timeframe
import app.coinverse.utils.UserActionType
import com.google.firebase.auth.FirebaseUser

/**
 * View state events for content feeds
 */
interface FeedViewEvents {
    fun feedLoadComplete(event: FeedLoadComplete)
    fun swipeToRefresh(event: SwipeToRefresh)
    fun contentSelected(event: ContentSelected)
    fun contentSwipeDrawed(event: ContentSwipeDrawed)
    fun contentSwiped(event: ContentSwiped)
    fun contentLabeled(event: FeedViewEventType.ContentLabeled)
    fun contentShared(event: ContentShared)
    fun contentSourceOpened(event: ContentSourceOpened)
    fun updateAds(event: UpdateAds)
}

sealed class FeedViewEventType {
    data class FeedLoad(val feedType: FeedType, val timeframe: Timeframe, val isRealtime: Boolean) : FeedViewEventType()
    data class FeedLoadComplete(val hasContent: Boolean) : FeedViewEventType()
    data class SwipeToRefresh(val feedType: FeedType, val timeframe: Timeframe,
                              val isRealtime: Boolean) : FeedViewEventType()

    data class ContentSelected(val position: Int, val content: Content) : FeedViewEventType()
    data class ContentSwipeDrawed(val isDrawed: Boolean) : FeedViewEventType()
    data class ContentSwiped(val feedType: FeedType, val actionType: UserActionType, val position: Int) : FeedViewEventType()

    data class ContentLabeled(val feedType: FeedType, val actionType: UserActionType,
                              val user: FirebaseUser?, val position: Int, val content: Content?,
                              val isMainFeedEmptied: Boolean) : FeedViewEventType()

    data class ContentShared(val content: Content) : FeedViewEventType()
    data class ContentSourceOpened(val url: String) : FeedViewEventType()
    class UpdateAds : FeedViewEventType()
}