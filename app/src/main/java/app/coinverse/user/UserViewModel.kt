package app.coinverse.user

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import app.coinverse.Enums
import app.coinverse.Enums.Status.ERROR
import app.coinverse.Enums.Status.SUCCESS
import app.coinverse.utils.livedata.Event
import com.google.firebase.auth.FirebaseUser

class UserViewModel : ViewModel() {
    private val LOG_TAG = UserViewModel::class.java.simpleName

    private val userRepository: UserRepository

    init {
        userRepository = UserRepository()
    }

    fun deleteUser(user: FirebaseUser) =
            MutableLiveData<Event<Enums.Status>>().apply {
                userRepository.deleteUser(user).addOnCompleteListener { task ->
                    if (task.isSuccessful) this.value = Event(SUCCESS)
                    else {
                        Log.e(LOG_TAG, "Failed to delete user error: ${task.exception?.localizedMessage}")
                        this.value = Event(ERROR)
                    }
                }
            }
}