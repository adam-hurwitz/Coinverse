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
class FeedViewIntent : FeedViewIntentType()

@ExperimentalCoroutinesApi
sealed class FeedViewIntentType(
        val initState: MutableStateFlow<Boolean> = MutableStateFlow(true),
        val loadFromNetwork: MutableStateFlow<Boolean?> = MutableStateFlow(null),
        val swipeToRefresh: MutableStateFlow<Event<SwipeToRefresh?>> = MutableStateFlow(Event(null)),
        val selectContent: MutableStateFlow<Event<SelectContent?>> = MutableStateFlow(Event(null)),
        val swipeContent: MutableStateFlow<SwipeContent?> = MutableStateFlow(null),
        val labelContent: MutableStateFlow<LabelContent?> = MutableStateFlow(null),
        val shareContent: MutableStateFlow<Content?> = MutableStateFlow(null),
        val openContentSource: MutableStateFlow<String?> = MutableStateFlow(null),
        val updateAds: MutableStateFlow<Event<Boolean?>> = MutableStateFlow(Event(null))
) {
    data class FeedLoad(
            val feedType: FeedType,
            val timeframe: Timeframe,
            val isRealtime: Boolean
    ) : FeedViewIntentType()

    data class SwipeToRefresh(
            val feedType: FeedType,
            val timeframe: Timeframe,
            val isRealtime: Boolean
    ) : FeedViewIntentType()

    data class SelectContent(val content: Content, val position: Int) : FeedViewIntentType()

    class SwipeContent(
            val feedType: FeedType,
            val actionType: UserActionType,
            val position: Int,
            val isSwiped: Boolean = false
    ) : FeedViewIntentType()

    data class LabelContent(
            val feedType: FeedType,
            val actionType: UserActionType,
            val user: FirebaseUser?,
            val position: Int,
            val content: Content?,
            val isMainFeedEmptied: Boolean
    ) : FeedViewIntentType()
}