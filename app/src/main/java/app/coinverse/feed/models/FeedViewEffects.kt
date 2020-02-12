package app.coinverse.feed.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import app.coinverse.feed.models.FeedViewEffectType.*
import app.coinverse.utils.FeedType
import app.coinverse.utils.UserActionType

/** View state effects for content feeds */
class _FeedViewEffects(
        val _signIn: MutableLiveData<SignInEffect> = MutableLiveData(),
        val _notifyItemChanged: MutableLiveData<NotifyItemChangedEffect> = MutableLiveData(),
        val _contentLoadingIds: HashSet<String> = hashSetOf(),
        val _enableSwipeToRefresh: MutableLiveData<EnableSwipeToRefreshEffect> = MutableLiveData(),
        val _swipeToRefresh: MutableLiveData<SwipeToRefreshEffect> = MutableLiveData(),
        val _contentSwiped: MutableLiveData<ContentSwipedEffect> = MutableLiveData(),
        val _snackBar: MutableLiveData<SnackBarEffect> = MutableLiveData(),
        val _shareContentIntent: MutableLiveData<ShareContentIntentEffect> = MutableLiveData(),
        val _openContentSourceIntent: MutableLiveData<OpenContentSourceIntentEffect> = MutableLiveData(),
        val _screenEmpty: MutableLiveData<ScreenEmptyEffect> = MutableLiveData(),
        val _updateAds: MutableLiveData<UpdateAdsEffect> = MutableLiveData())

class FeedViewEffects(_effects: _FeedViewEffects) {
    val signIn: LiveData<SignInEffect> = _effects._signIn
    val notifyItemChanged: LiveData<NotifyItemChangedEffect> = _effects._notifyItemChanged
    val contentLoadingIds: HashSet<String> = _effects._contentLoadingIds
    val enableSwipeToRefresh: LiveData<EnableSwipeToRefreshEffect> = _effects._enableSwipeToRefresh
    val swipeToRefresh: LiveData<SwipeToRefreshEffect> = _effects._swipeToRefresh
    val contentSwiped: LiveData<ContentSwipedEffect> = _effects._contentSwiped
    val snackBar: LiveData<SnackBarEffect> = _effects._snackBar
    val shareContentIntent: LiveData<ShareContentIntentEffect> = _effects._shareContentIntent
    val openContentSourceIntent: LiveData<OpenContentSourceIntentEffect> = _effects._openContentSourceIntent
    val screenEmpty: LiveData<ScreenEmptyEffect> = _effects._screenEmpty
    val updateAds: LiveData<UpdateAdsEffect> = _effects._updateAds
}

sealed class FeedViewEffectType {

    data class SignInEffect(val toSignIn: Boolean) : FeedViewEffectType()
    data class NotifyItemChangedEffect(val position: Int) : FeedViewEffectType()
    data class EnableSwipeToRefreshEffect(val isEnabled: Boolean) : FeedViewEffectType()
    data class SwipeToRefreshEffect(val isEnabled: Boolean) : FeedViewEffectType()

    data class ContentSwipedEffect(val feedType: FeedType, val actionType: UserActionType,
                                   val position: Int) : FeedViewEffectType()

    data class SnackBarEffect(val text: String) : FeedViewEffectType()
    data class ShareContentIntentEffect(
            val contentRequest: LiveData<Content> = MutableLiveData()) : FeedViewEffectType()

    data class OpenContentSourceIntentEffect(val url: String) : FeedViewEffectType()
    data class ScreenEmptyEffect(val isEmpty: Boolean) : FeedViewEffectType()
    class UpdateAdsEffect : FeedViewEffectType()
}