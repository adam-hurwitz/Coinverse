package app.coinverse.content.models

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.paging.PagedList
import app.coinverse.utils.Enums.FeedType
import app.coinverse.utils.Enums.Timeframe
import app.coinverse.utils.Enums.UserActionType
import app.coinverse.utils.livedata.Event
import app.coinverse.utils.models.ToolbarState
import com.google.firebase.auth.FirebaseUser

data class FeedViewState(val feedType: FeedType,
                         val timeframe: Timeframe,
                         val toolbar: ToolbarState,
                         val contentList: LiveData<PagedList<Content>>,
                         val contentToPlay: LiveData<Event<ContentResult.ContentToPlay>>,
                         val contentLabeled: LiveData<Event<ContentResult.ContentLabeled>>,
                         val notificationBitmap: LiveData<Event<Bitmap>>)

data class PlayerViewState(val contentPlayer: LiveData<Event<ContentResult.ContentPlayer>>)

sealed class ContentViewEvent {
    data class FeedLoad(val feedType: FeedType, val timeframe: Timeframe, val isRealtime: Boolean) : ContentViewEvent()
    data class FeedLoadComplete(val hasContent: Boolean) : ContentViewEvent()
    data class PlayerLoad(val contentId: String, val filePath: String, val previewImageUrl: String) : ContentViewEvent()
    data class SwipeToRefresh(val feedType: FeedType, val timeframe: Timeframe, val isRealtime: Boolean) : ContentViewEvent()
    data class ContentSelected(val position: Int, val content: Content) : ContentViewEvent()
    data class ContentSwipeDrawed(val isDrawed: Boolean) : ContentViewEvent()
    data class ContentSwiped(val feedType: FeedType, val actionType: UserActionType, val position: Int) : ContentViewEvent()
    data class ContentLabeled(val feedType: FeedType, val actionType: UserActionType, val user: FirebaseUser?, val position: Int, val content: Content?, val isMainFeedEmptied: Boolean) : ContentViewEvent()
    data class ContentShared(val content: Content) : ContentViewEvent()
    data class ContentSourceOpened(val url: String) : ContentViewEvent()
    class UpdateAds : ContentViewEvent()
}

sealed class ContentViewEffect {
    data class SignIn(val toSignIn: Boolean) : ContentViewEffect()
    data class NotifyItemChanged(val position: Int) : ContentViewEffect()
    data class EnableSwipeToRefresh(val isEnabled: Boolean) : ContentViewEffect()
    data class SwipeToRefresh(val isEnabled: Boolean) : ContentViewEffect()
    data class ContentSwiped(val feedType: FeedType, val actionType: UserActionType, val position: Int) : ContentViewEffect()
    data class SnackBar(val text: String) : ContentViewEffect()
    data class ShareContentIntent(val contentRequest: LiveData<Event<Content>>) : ContentViewEffect()
    data class OpenContentSourceIntent(val url: String) : ContentViewEffect()
    data class ScreenEmpty(val isEmpty: Boolean) : ContentViewEffect()
    class UpdateAds : ContentViewEffect()
}