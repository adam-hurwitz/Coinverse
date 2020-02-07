package app.coinverse.user

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import app.coinverse.utils.Status.ERROR
import app.coinverse.utils.Status.SUCCESS
import app.coinverse.utils.livedata.Event
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.tasks.await

private val LOG_TAG = UserViewModel::class.java.simpleName

/**
 * TODO: Refactor with Unidirectional Data Flow. See [app.coinverse.feed.viewmodels.FeedViewModel].
 * See more: https://medium.com/hackernoon/android-unidirectional-flow-with-livedata-bf24119e747
 **/

class UserViewModel : ViewModel() {
    fun deleteUser(user: FirebaseUser) = liveData {
        try {
            deleteUserCall(user).await()
            emit(Event(SUCCESS))
        } catch (e: FirebaseFunctionsException) {
            emit(Event(ERROR))
            Log.e(LOG_TAG, "Failed to delete user error: ${e?.localizedMessage}")
        }
    }
}
