package app.coinverse.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import app.coinverse.Enums.PaymentStatus
import app.coinverse.Enums.PaymentStatus.FREE
import app.coinverse.content.models.ContentSelected
import app.coinverse.utils.livedata.Event
import com.google.firebase.auth.FirebaseAuth.getInstance
import com.google.firebase.auth.FirebaseUser

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    val user: LiveData<FirebaseUser> get() = _user
    val showLocationPermission: LiveData<Event<Boolean>> get() = _showLocationPermission
    val isRealtime: LiveData<Boolean> get() = _isRealtime
    val accountType: LiveData<PaymentStatus> get() = _accountType
    val isSwipeToRefreshEnabled: LiveData<Boolean> get() = _isSwipeToRefreshEnabled
    val isRefreshing: LiveData<Boolean> get() = _isRefreshing
    val bottomSheetState: LiveData<Int> get() = _bottomSheetState
    val savedContentSelected: LiveData<Event<ContentSelected>> get() = _savedContentSelected

    private val homeRepository: HomeRepository
    private val _user = MutableLiveData<FirebaseUser>()
    private val _showLocationPermission = MutableLiveData<Event<Boolean>>()
    private val _isRealtime = MutableLiveData<Boolean>()
    private val _accountType = MutableLiveData<PaymentStatus>()
    private val _isSwipeToRefreshEnabled = MutableLiveData<Boolean>()
    private val _isRefreshing = MutableLiveData<Boolean>()
    private val _bottomSheetState = MutableLiveData<Int>()
    private val _savedContentSelected = MutableLiveData<Event<ContentSelected>>()

    init {
        //TODO: Set ability to toggle based on user configuration.
        //TODO: Return info to ContentFragment observerSignIn().
        //TODO: Toggle with button if subscribed user.
        homeRepository = HomeRepository(application)
        _isRealtime.value = false
        _accountType.value = FREE
        _user.value = getCurrentUser()
    }

    fun setUser(user: FirebaseUser?) {
        _user.value = user
    }

    fun getCurrentUser() = getInstance().currentUser

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

    fun setSavedContentSelected(contentSelected: ContentSelected) {
        _savedContentSelected.value = Event(contentSelected)
    }

}