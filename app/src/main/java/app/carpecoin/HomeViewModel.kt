package app.carpecoin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser


class HomeViewModel : ViewModel() {
    var isRealtimeDataEnabled = MutableLiveData<Boolean>()
    var disableSwipeToRefresh = MutableLiveData<Boolean>()
    var user = MutableLiveData<FirebaseUser>()
    var profileButtonClick = MutableLiveData<Boolean>()

    init {
        user.value = getCurrentUser()
    }

    fun profileButtonClick(click: Boolean) {
        profileButtonClick.value = true
    }

    fun getCurrentUser(): FirebaseUser? {
        return FirebaseAuth.getInstance().currentUser
    }

    fun setRefreshStatus(isRealtimeDataEnabled: Boolean) {
        this.isRealtimeDataEnabled.value = isRealtimeDataEnabled
    }

    fun disableSwipeToRefresh() {
        disableSwipeToRefresh.value = true
    }

}