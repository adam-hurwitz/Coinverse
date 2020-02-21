package app.coinverse.user

import app.coinverse.firebase.USERS_DOCUMENT
import app.coinverse.utils.DELETE_USER_FUNCTION
import app.coinverse.utils.PATH_FUNCTION_PARAM
import app.coinverse.utils.USER_ID_FUNCTION_PARAM
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.functions.FirebaseFunctions
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TODO: Refactor with Unidirectional Data Flow. See [ContentViewModel].
 * See more: https://medium.com/hackernoon/android-unidirectional-flow-with-livedata-bf24119e747
 **/
@Singleton
class UserRepository @Inject constructor() {
    fun deleteUserCall(user: FirebaseUser) = FirebaseFunctions.getInstance()
            .getHttpsCallable(DELETE_USER_FUNCTION)
            .call(hashMapOf(
                    USER_ID_FUNCTION_PARAM to user.uid,
                    PATH_FUNCTION_PARAM to "$USERS_DOCUMENT${user.uid}"))
            .continueWith { task -> (task.result?.data as HashMap<String, String>) }
}