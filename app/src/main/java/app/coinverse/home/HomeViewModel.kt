package app.coinverse.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import app.coinverse.feed.models.ContentToPlay
import app.coinverse.utils.PaymentStatus
import app.coinverse.utils.PaymentStatus.FREE
import app.coinverse.utils.Timeframe
import app.coinverse.utils.buildTypeTimescale
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

/**
 * TODO: Refactor with Unidirectional Data Flow. See [app.coinverse.feed.viewmodels.FeedViewModel].
 * See more: https://medium.com/hackernoon/android-unidirectional-flow-with-livedata-bf24119e747
 **/
class HomeViewModel : ViewModel() {
    val user: LiveData<FirebaseUser> get() = _user
    val showLocationPermission: LiveData<Boolean> get() = _showLocationPermission
    val isRealtime: LiveData<Boolean> get() = _isRealtime
    val accountType: LiveData<PaymentStatus> get() = _accountType
    val timeframe: LiveData<Timeframe> get() = _timeframe
    val isSwipeToRefreshEnabled: LiveData<Boolean> get() = _isSwipeToRefreshEnabled
    val isRefreshing: LiveData<Boolean> get() = _isRefreshing
    val bottomSheetState: LiveData<Int> get() = _bottomSheetState
    val savedContentToPlay: LiveData<ContentToPlay?> get() = _savedContentToPlay

    private val _user = MutableLiveData<FirebaseUser>()
    private val _showLocationPermission = MutableLiveData<Boolean>()
    private val _isRealtime = MutableLiveData<Boolean>()
    private val _accountType = MutableLiveData<PaymentStatus>()
    private val _timeframe = MutableLiveData<Timeframe>()
    private val _isSwipeToRefreshEnabled = MutableLiveData<Boolean>()
    private val _isRefreshing = MutableLiveData<Boolean>()
    private val _bottomSheetState = MutableLiveData<Int>()
    private val _savedContentToPlay = MutableLiveData<ContentToPlay?>()

    init {
        //TODO: Toggle with button if paid user.
        _isRealtime.value = false
        _accountType.value = FREE
        _timeframe.value = buildTypeTimescale
        _user.value = getCurrentUser()
    }

    fun setUser(user: FirebaseUser?) {
        _user.value = user
    }

    fun getCurrentUser() = FirebaseAuth.getInstance().currentUser

    fun setShowLocationPermission(toShow: Boolean) {
        _showLocationPermission.value = toShow
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
        _savedContentToPlay.value = contentToPlay
    }

}