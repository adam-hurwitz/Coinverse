package app.coinverse.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import app.coinverse.firebase.FirestoreCollections.usersCollection
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser


class HomeViewModel(application: Application) : AndroidViewModel(application) {
    var homeRepository: HomeRepository

    var isRealtime = MutableLiveData<Boolean>()
    var isSwipeToRefreshEnabled = MutableLiveData<Boolean>()
    var isRefreshing = MutableLiveData<Boolean>()
    var bottomSheetState = MutableLiveData<Int>()
    var user = MutableLiveData<FirebaseUser>()
    var messageCenterLiveData: LiveData<ArrayList<MessageCenterUpdate>>
    var messageCenterUnreadCountLiveData: LiveData<Double>

    init {
        //TODO: Set ability to toggle based on user configuration.
        //TODO: Return info to ContentFragment observerSignIn().
        //TODO: Toggle with button if subscribed user.
        homeRepository = HomeRepository(application)
        isRealtime.value = false
        user.value = getCurrentUser()
        val messageCenterUpdates = homeRepository.messageCenterUpdatesLiveData
        this.messageCenterLiveData = Transformations.map(messageCenterUpdates) { result -> result }

        val messageCenterUnreadCount = homeRepository.messageCenterUnreadCountLiveData
        this.messageCenterUnreadCountLiveData = Transformations.map(messageCenterUnreadCount) { result -> result }
    }

    fun getCurrentUser(): FirebaseUser? {
        return FirebaseAuth.getInstance().currentUser
    }

    fun syncMessageCenterUpdates() {
        homeRepository.syncMessageCenterUpdates(getCurrentUser()?.uid)
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