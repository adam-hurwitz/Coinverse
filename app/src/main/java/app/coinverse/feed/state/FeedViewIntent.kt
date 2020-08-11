package app.coinverse.feed.state

import app.coinverse.feed.Content
import app.coinverse.utils.Event
import app.coinverse.utils.FeedType
import app.coinverse.utils.Timeframe
import app.coinverse.utils.UserActionType
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow

@ExperimentalCoroutinesApi
class FeedViewIntent(
        val initState: MutableStateFlow<Event<Boolean>> = MutableStateFlow(Event(true)),
        val loadFromNetwork: MutableStateFlow<Boolean?> = MutableStateFlow(null),
        val swipeToRefresh: MutableStateFlow<Event<SwipeToRefresh?>> = MutableStateFlow(Event(null)),
        val openContent: MutableStateFlow<Event<OpenContent?>> = MutableStateFlow(Event(null)),
        val openContentSource: MutableStateFlow<Event<String?>> = MutableStateFlow(Event(null)),
        val swipeContent: MutableStateFlow<Event<SwipeContent?>> = MutableStateFlow(Event(null)),
        val labelContent: MutableStateFlow<Event<LabelContent?>> = MutableStateFlow(Event(null)),
        val shareContent: MutableStateFlow<Event<Content?>> = MutableStateFlow(Event((null))),
        val updateAds: MutableStateFlow<Event<Boolean?>> = MutableStateFlow(Event(null))
) {
    data class FeedLoad(
            val feedType: FeedType,
            val timeframe: Timeframe,
            val isRealtime: Boolean
    )

    data class SwipeToRefresh(
            val feedType: FeedType,
            val timeframe: Timeframe,
            val isRealtime: Boolean
    )

    data class OpenContent(val content: Content, val position: Int)

    class SwipeContent(
            val feedType: FeedType,
            val actionType: UserActionType,
            val position: Int,
            val isSwiped: Boolean = false
    )

    data class LabelContent(
            val feedType: FeedType,
            val actionType: UserActionType,
            val user: FirebaseUser?,
            val position: Int,
            val content: Content?,
            val isMainFeedEmptied: Boolean
    )
}