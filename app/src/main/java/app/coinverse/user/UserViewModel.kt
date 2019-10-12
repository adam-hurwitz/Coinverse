package app.coinverse.user

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import app.coinverse.utils.Status
import app.coinverse.utils.Status.ERROR
import app.coinverse.utils.Status.SUCCESS
import app.coinverse.utils.livedata.Event
import com.google.firebase.auth.FirebaseUser

private val LOG_TAG = UserViewModel::class.java.simpleName

/**
 * TODO - Refactor with Unidirectional Data Flow.
 *  See [ContentViewModel]
 *  https://medium.com/hackernoon/android-unidirectional-flow-with-livedata-bf24119e747
 **/
class UserViewModel : ViewModel() {
    fun deleteUser(user: FirebaseUser) =
            MutableLiveData<Event<Status>>().apply {
                deleteUserCall(user).addOnCompleteListener { task ->
                    if (task.isSuccessful) this.value = Event(SUCCESS)
                    else {
                        Log.e(LOG_TAG, "Failed to delete user error: ${task.exception?.localizedMessage}")
                        this.value = Event(ERROR)
                    }
                }
            }
}