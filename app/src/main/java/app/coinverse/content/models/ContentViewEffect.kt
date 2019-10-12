package app.coinverse.content.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import app.coinverse.utils.FeedType
import app.coinverse.utils.UserActionType
import app.coinverse.utils.livedata.Event

data class ContentViewEffects(
        val signIn: LiveData<Event<SignInEffect>> = MutableLiveData(),
        val notifyItemChanged: LiveData<Event<NotifyItemChangedEffect>> = MutableLiveData(),
        val enableSwipeToRefresh: LiveData<Event<EnableSwipeToRefreshEffect>> = MutableLiveData(),
        val swipeToRefresh: LiveData<Event<SwipeToRefreshEffect>> = MutableLiveData(),
        val contentSwiped: LiveData<Event<ContentSwipedEffect>> = MutableLiveData(),
        val snackBar: LiveData<Event<SnackBarEffect>> = MutableLiveData(),
        val shareContentIntent: LiveData<Event<ShareContentIntentEffect>> = MutableLiveData(),
        val openContentSourceIntent: LiveData<Event<OpenContentSourceIntentEffect>> = MutableLiveData(),
        val screenEmpty: LiveData<Event<ScreenEmptyEffect>> = MutableLiveData(),
        val updateAds: LiveData<Event<UpdateAdsEffect>> = MutableLiveData())

data class SignInEffect(val toSignIn: Boolean)

data class NotifyItemChangedEffect(val position: Int)
data class EnableSwipeToRefreshEffect(val isEnabled: Boolean)
data class SwipeToRefreshEffect(val isEnabled: Boolean)
data class ContentSwipedEffect(val feedType: FeedType, val actionType: UserActionType,
                               val position: Int)

data class SnackBarEffect(val text: String)
data class ShareContentIntentEffect(val contentRequest: LiveData<Event<Content>> = MutableLiveData())
data class OpenContentSourceIntentEffect(val url: String)
data class ScreenEmptyEffect(val isEmpty: Boolean)
class UpdateAdsEffect