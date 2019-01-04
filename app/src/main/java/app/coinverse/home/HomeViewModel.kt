package app.coinverse.home

import android.app.Application
import android.os.Parcelable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import app.coinverse.content.models.Content
import app.coinverse.firebase.FirestoreCollections.usersCollection
import app.coinverse.utils.livedata.Event
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser


class HomeViewModel(application: Application) : AndroidViewModel(application) {
    var homeRepository: HomeRepository

    var isRealtime = MutableLiveData<Boolean>()
    var isSwipeToRefreshEnabled = MutableLiveData<Boolean>()
    var isRefreshing = MutableLiveData<Boolean>()
    var bottomSheetState = MutableLiveData<Int>()
    var user = MutableLiveData<FirebaseUser>()

    // Saved Content
    val _savedContentSelected = MutableLiveData<Event<Content>>()
    val savedContentSelected: LiveData<Event<Content>>
        get() = _savedContentSelected
    var savedContentState: Parcelable? = null

    init {
        //TODO: Set ability to toggle based on user configuration.
        //TODO: Return info to ContentFragment observerSignIn().
        //TODO: Toggle with button if subscribed user.
        homeRepository = HomeRepository(application)
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

    fun clearUnreadMessageCenterCount() {
        val user = getCurrentUser()
        if (user != null)
            homeRepository.updateUnreadMessageCenterCount(usersCollection.document(user.uid), 0.0)
    }

}