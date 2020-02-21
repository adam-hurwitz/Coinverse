package app.coinverse.user.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import app.coinverse.user.UserRepository
import app.coinverse.utils.Status.ERROR
import app.coinverse.utils.Status.SUCCESS
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.tasks.await

private val LOG_TAG = UserViewModel::class.java.simpleName

/**
 * TODO: Refactor with Unidirectional Data Flow. See [app.coinverse.feed.viewmodel.FeedViewModel].
 * See more: https://medium.com/hackernoon/android-unidirectional-flow-with-livedata-bf24119e747
 **/

class UserViewModel(val repository: UserRepository) : ViewModel() {
    fun deleteUser(user: FirebaseUser) = liveData {
        try {
            repository.deleteUserCall(user).await()
            emit(SUCCESS)
        } catch (e: FirebaseFunctionsException) {
            emit(ERROR)
            Log.e(LOG_TAG, "Failed to delete user error: ${e?.localizedMessage}")
        }
    }
}
