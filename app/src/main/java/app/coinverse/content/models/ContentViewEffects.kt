package app.coinverse.content.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import app.coinverse.content.models.ContentEffectType.*
import app.coinverse.utils.FeedType
import app.coinverse.utils.UserActionType
import app.coinverse.utils.livedata.Event

data class ContentEffects(
        val signIn: LiveData<Event<SignInEffect>> = MutableLiveData(),
        val notifyItemChanged: LiveData<Event<NotifyItemChangedEffect>> = MutableLiveData(),
        val enableSwipeToRefresh: LiveData<Event<EnableSwipeToRefreshEffect>> = MutableLiveData(),
        val swipeToRefresh: LiveData<Event<SwipeToRefreshEffect>> = MutableLiveData(),
        val contentSwiped: LiveData<Event<ContentSwipedEffect>> = MutableLiveData(),
        val snackBar: LiveData<Event<SnackBarEffect>> = MutableLiveData(),
        val shareContentIntent: LiveData<Event<ShareContentIntentEffect>> = MutableLiveData(),
        val openContentSourceIntent: LiveData<Event<OpenContentSourceIntentEffect>> =
                MutableLiveData(),
        val screenEmpty: LiveData<Event<ScreenEmptyEffect>> = MutableLiveData(),
        val updateAds: LiveData<Event<UpdateAdsEffect>> = MutableLiveData())

sealed class ContentEffectType {

    data class SignInEffect(val toSignIn: Boolean) : ContentEffectType()
    data class NotifyItemChangedEffect(val position: Int) : ContentEffectType()
    data class EnableSwipeToRefreshEffect(val isEnabled: Boolean) : ContentEffectType()
    data class SwipeToRefreshEffect(val isEnabled: Boolean) : ContentEffectType()

    data class ContentSwipedEffect(val feedType: FeedType, val actionType: UserActionType,
                                   val position: Int) : ContentEffectType()

    data class SnackBarEffect(val text: String) : ContentEffectType()
    data class ShareContentIntentEffect(
            val contentRequest: LiveData<Event<Content>> = MutableLiveData()) : ContentEffectType()

    data class OpenContentSourceIntentEffect(val url: String) : ContentEffectType()
    data class ScreenEmptyEffect(val isEmpty: Boolean) : ContentEffectType()
    class UpdateAdsEffect : ContentEffectType()
}

fun MutableLiveData<ContentEffects>.send(effect: ContentEffectType) {
    this.value = when (effect) {
        is SignInEffect ->
            this.value?.copy(signIn = liveData { emit(Event(effect)) })
        is NotifyItemChangedEffect ->
            this.value?.copy(notifyItemChanged = liveData { emit(Event(effect)) })
        is EnableSwipeToRefreshEffect ->
            this.value?.copy(enableSwipeToRefresh = liveData { emit(Event(effect)) })
        is SwipeToRefreshEffect ->
            this.value?.copy(swipeToRefresh = liveData { emit(Event(effect)) })
        is ContentSwipedEffect ->
            this.value?.copy(contentSwiped = liveData { emit(Event(effect)) })
        is SnackBarEffect ->
            this.value?.copy(snackBar = liveData { emit(Event(effect)) })
        is ShareContentIntentEffect ->
            this.value?.copy(shareContentIntent = liveData { emit(Event(effect)) })
        is OpenContentSourceIntentEffect ->
            this.value?.copy(openContentSourceIntent = liveData { emit(Event(effect)) })
        is ScreenEmptyEffect ->
            this.value?.copy(screenEmpty = liveData { emit(Event(effect)) })
        is UpdateAdsEffect ->
            this.value?.copy(updateAds = liveData { emit(Event(effect)) })
    }
}