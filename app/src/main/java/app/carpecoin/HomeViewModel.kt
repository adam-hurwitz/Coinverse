package app.carpecoin

import android.view.View
import androidx.lifecycle.ViewModel
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser


class HomeViewModel : ViewModel() {
    var user = MutableLiveData<FirebaseUser>()

    var profileButtonClick = MutableLiveData<View>()

    init {
        user.value = getCurrentUser()
    }

    fun profileButtonClick(view: View) {
        profileButtonClick.value = view
    }

    fun getCurrentUser(): FirebaseUser? {
        return FirebaseAuth.getInstance().currentUser
    }

}