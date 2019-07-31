package app.coinverse.user

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import app.coinverse.utils.Enums
import app.coinverse.utils.Enums.Status.ERROR
import app.coinverse.utils.Enums.Status.SUCCESS
import app.coinverse.utils.livedata.Event
import com.google.firebase.auth.FirebaseUser

private val LOG_TAG = UserViewModel::class.java.simpleName

class UserViewModel : ViewModel() {
    //TODO - Create view state and view events.
    //TODO - Handle LCE.
    fun deleteUser(user: FirebaseUser) =
            MutableLiveData<Event<Enums.Status>>().apply {
                deleteUserCall(user).addOnCompleteListener { task ->
                    if (task.isSuccessful) this.value = Event(SUCCESS)
                    else {
                        Log.e(LOG_TAG, "Failed to delete user error: ${task.exception?.localizedMessage}")
                        this.value = Event(ERROR)
                    }
                }
            }
}