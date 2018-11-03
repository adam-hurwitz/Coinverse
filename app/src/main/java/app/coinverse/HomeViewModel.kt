package app.coinverse

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import app.coinverse.utils.SAVED_BOTTOM_SHEET_PEEK_HEIGHT
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser


class HomeViewModel : ViewModel() {
    var isRealtime = MutableLiveData<Boolean>()
    var isSwipeToRefreshEnabled = MutableLiveData<Boolean>()
    var isRefreshing = MutableLiveData<Boolean>()
    var bottomSheetState = MutableLiveData<Int>()
    var user = MutableLiveData<FirebaseUser>()
    var savedBottomSheetPeekHeight = SAVED_BOTTOM_SHEET_PEEK_HEIGHT

    init {
        //TODO: Set ability to toggle based on user configuration.
        //TODO: Return info to ContentFragment observerSignIn().
        //TODO: Toggle with button if subscribed user.
        isRealtime.value = false
        user.value = getCurrentUser()
    }

    fun getCurrentUser(): FirebaseUser? {
        return FirebaseAuth.getInstance().currentUser
    }

    fun enableSwipeToRefresh(isEnabled: Boolean) {
        isSwipeToRefreshEnabled.value = isEnabled
    }

    fun setSwipeToRefreshState(isRefreshing: Boolean) {
        this.isRefreshing.value = isRefreshing
    }

}