package app.coinverse.user

import android.util.Log
import androidx.lifecycle.ViewModel
import app.coinverse.Enums.Status
import app.coinverse.Enums.Status.ERROR
import app.coinverse.Enums.Status.SUCCESS
import com.google.firebase.auth.FirebaseUser
import io.reactivex.Observable
import io.reactivex.subjects.ReplaySubject

class UserViewModel : ViewModel() {
    private val LOG_TAG = UserViewModel::class.java.simpleName

    private var userRepository: UserRepository

    init {
        userRepository = UserRepository()
    }

    fun deleteUser(user: FirebaseUser): Observable<Status> {
        val deleteUserSubscriber = ReplaySubject.create<Status>()
        userRepository.deleteUser(user).addOnCompleteListener { task ->
            if (task.isSuccessful) deleteUserSubscriber.onNext(SUCCESS)
            else {
                Log.e(LOG_TAG, "Failed to delete user error: ${task.exception?.localizedMessage}")
                deleteUserSubscriber.onNext(ERROR)
            }
        }
        return deleteUserSubscriber
    }
}