package app.coinverse.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import app.coinverse.content.models.ContentToPlay
import app.coinverse.utils.DateAndTime
import app.coinverse.utils.PaymentStatus
import app.coinverse.utils.PaymentStatus.FREE
import app.coinverse.utils.Timeframe
import app.coinverse.utils.livedata.Event
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

/**
 * TODO - Refactor with Unidirectional Data Flow.
 *  See [ContentViewModel]
 *  https://medium.com/hackernoon/android-unidirectional-flow-with-livedata-bf24119e747
 **/
class HomeViewModel : ViewModel() {
    val user: LiveData<FirebaseUser> get() = _user
    val showLocationPermission: LiveData<Event<Boolean>> get() = _showLocationPermission
    val isRealtime: LiveData<Boolean> get() = _isRealtime
    val accountType: LiveData<PaymentStatus> get() = _accountType
    val timeframe: LiveData<Timeframe> get() = _timeframe
    val isSwipeToRefreshEnabled: LiveData<Boolean> get() = _isSwipeToRefreshEnabled
    val isRefreshing: LiveData<Boolean> get() = _isRefreshing
    val bottomSheetState: LiveData<Int> get() = _bottomSheetState
    val savedContentToPlay: LiveData<Event<ContentToPlay?>> get() = _savedContentToPlay

    private val _user = MutableLiveData<FirebaseUser>()
    private val _showLocationPermission = MutableLiveData<Event<Boolean>>()
    private val _isRealtime = MutableLiveData<Boolean>()
    private val _accountType = MutableLiveData<PaymentStatus>()
    private val _timeframe = MutableLiveData<Timeframe>()
    private val _isSwipeToRefreshEnabled = MutableLiveData<Boolean>()
    private val _isRefreshing = MutableLiveData<Boolean>()
    private val _bottomSheetState = MutableLiveData<Int>()
    private val _savedContentToPlay = MutableLiveData<Event<ContentToPlay?>>()

    init {
        //TODO: Set ability to toggle based on user configuration.
        //TODO: Return info to ContentFragment observerSignIn().
        //TODO: Toggle with button if subscribed user.
        _isRealtime.value = false
        _accountType.value = FREE
        _timeframe.value = DateAndTime.buildTypeTimescale
        _user.value = getCurrentUser()
    }

    fun setUser(user: FirebaseUser?) {
        _user.value = user
    }

    fun getCurrentUser() = FirebaseAuth.getInstance().currentUser

    fun setShowLocationPermission(toShow: Boolean) {
        _showLocationPermission.value = Event(toShow)
    }

    fun enableSwipeToRefresh(isEnabled: Boolean) {
        _isSwipeToRefreshEnabled.value = isEnabled
    }

    fun setSwipeToRefreshState(isRefreshing: Boolean) {
        _isRefreshing.value = isRefreshing
    }

    fun setBottomSheetState(state: Int) {
        _bottomSheetState.value = state
    }

    fun setSavedContentToPlay(contentToPlay: ContentToPlay?) {
        _savedContentToPlay.value = Event(contentToPlay)
    }

}