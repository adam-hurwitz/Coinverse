package app.carpecoin

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser


class HomeViewModel : ViewModel() {
    var isRealtime = MutableLiveData<Boolean>()
    var endSwipeToRefresh = MutableLiveData<Boolean>()
    var user = MutableLiveData<FirebaseUser>()

    init {
        //TODO: Set ability to toggle based on user configuration.
        //TODO: Return info to ContentFragment observerSignIn().
        //TODO: Toggle with button if subscribed user.
        isRealtime.value = true
        user.value = getCurrentUser()
    }

    fun getCurrentUser(): FirebaseUser? {
        return FirebaseAuth.getInstance().currentUser
    }

    fun disableSwipeToRefresh() {
        endSwipeToRefresh.value = true
    }

}