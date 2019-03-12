package app.coinverse.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import app.coinverse.Enums.PaymentStatus
import app.coinverse.Enums.PaymentStatus.FREE
import app.coinverse.content.models.ContentSelected
import app.coinverse.utils.livedata.Event
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    val homeRepository: HomeRepository
    val showLocationPermission = MutableLiveData<Event<Boolean>>()
    val isRealtime = MutableLiveData<Boolean>()
    val accountType = MutableLiveData<PaymentStatus>()
    val isSwipeToRefreshEnabled = MutableLiveData<Boolean>()
    val isRefreshing = MutableLiveData<Boolean>()
    val bottomSheetState = MutableLiveData<Int>()
    val user = MutableLiveData<FirebaseUser>()
    // Saved Content
    val _savedContentSelected = MutableLiveData<Event<ContentSelected>>()
    val savedContentSelected: LiveData<Event<ContentSelected>>
        get() = _savedContentSelected

    init {
        //TODO: Set ability to toggle based on user configuration.
        //TODO: Return info to ContentFragment observerSignIn().
        //TODO: Toggle with button if subscribed user.
        homeRepository = HomeRepository(application)
        isRealtime.value = false
        accountType.value = FREE
        user.value = getCurrentUser()
    }

    fun getCurrentUser() = FirebaseAuth.getInstance().currentUser

    fun enableSwipeToRefresh(isEnabled: Boolean) {
        isSwipeToRefreshEnabled.value = isEnabled
    }

    fun setSwipeToRefreshState(isRefreshing: Boolean) {
        this.isRefreshing.value = isRefreshing
    }

}