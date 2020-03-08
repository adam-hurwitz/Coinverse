package app.coinverse.analytics

import android.app.Activity
import android.os.Bundle
import android.util.Log
import app.coinverse.analytics.models.ContentAction
import app.coinverse.analytics.models.UserAction
import app.coinverse.feed.models.Content
import app.coinverse.feed.room.CoinverseDatabase
import app.coinverse.firebase.*
import app.coinverse.utils.*
import com.google.firebase.Timestamp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Transaction
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Analytics @Inject constructor(private val firebaseAnalytics: FirebaseAnalytics,
                                    val database: CoinverseDatabase) {
    private val LOG_TAG = Analytics::class.java.simpleName

    fun setCurrentScreen(activity: Activity, viewName: String) {
        firebaseAnalytics.setCurrentScreen(activity, viewName, null)
    }

    /**
     * Track user action in user profile and Google Analytics.
     *
     * @param content Content audiocast, video, or text media
     * @param watchPercent Double the amount of time in seconds the user has listened/watched content
     */
    suspend fun updateActionsAndAnalytics(content: Content, watchPercent: Double) {
        val bundle = Bundle()
        database.feedDao().updateContent(content)
        if (watchPercent >= FINISH_THRESHOLD) {
            FirebaseAuth.getInstance().currentUser.also { user ->
                bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, content.title)
                if (user != null && !user.isAnonymous) {
                    updateActionAnalytics(UserActionType.FINISH, content, user)
                    bundle.putString(USER_ID_PARAM, user.uid)
                }
            }
            bundle.putString(CREATOR_PARAM, content.creator)
            firebaseAnalytics.logEvent(FINISH_CONTENT_EVENT, bundle)
        } else if (watchPercent >= CONSUME_THRESHOLD) {
            FirebaseAuth.getInstance().currentUser.also { user ->
                bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, content.title)
                if (user != null && !user.isAnonymous) {
                    updateActionAnalytics(UserActionType.CONSUME, content, user)
                    bundle.putString(USER_ID_PARAM, user.uid)
                }
                bundle.putString(CREATOR_PARAM, content.creator)
                firebaseAnalytics.logEvent(CONSUME_CONTENT_EVENT, bundle)
            }
        }
    }

    /**
     * Track user starting content in user profile and Google Analytics.
     *
     * @param content Content audiocast, video, or text media
     */
    fun updateStartActionsAndAnalytics(content: Content) {
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, content.title)
        bundle.putString(CREATOR_PARAM, content.creator)
        FirebaseAuth.getInstance().currentUser.let { user ->
            if (user != null && !user.isAnonymous) {
                updateActionAnalytics(UserActionType.START, content, user)
                bundle.putString(USER_ID_PARAM, user.uid)
            }
        }
        firebaseAnalytics.logEvent(START_CONTENT_EVENT, bundle)
    }

    fun getWatchPercent(currentPosition: Double, seekToPositionMillis: Double, duration: Double) =
            (currentPosition - seekToPositionMillis) / duration

    /**
     * Track user action in user profile and Google Analytics.
     *
     * @param actionType UserActionType action taken on content
     * @param content Content audiocast, video, or text media
     * @param user FirebaseUser user account
     */
    fun updateActionAnalytics(actionType: UserActionType, content: Content, user: FirebaseUser) {
        var actionCollection = ""
        var score = INVALID_SCORE
        var countType = ""
        when (actionType) {
            UserActionType.START -> {
                actionCollection = START_ACTION_COLLECTION
                score = START_SCORE
                countType = START_COUNT
            }
            UserActionType.CONSUME -> {
                actionCollection = CONSUME_ACTION_COLLECTION
                score = CONSUME_SCORE
                countType = CONSUME_COUNT
            }
            UserActionType.FINISH -> {
                actionCollection = FINISH_ACTION_COLLECTION
                score = FINISH_SCORE
                countType = FINISH_COUNT
            }
            UserActionType.SAVE -> {
                actionCollection = SAVE_ACTION_COLLECTION
                score = SAVE_SCORE
                countType = ORGANIZE_COUNT
            }
            UserActionType.DISMISS -> {
                actionCollection = DISMISS_ACTION_COLLECTION
                score = DISMISS_SCORE
                countType = DISMISS_COUNT
            }
        }
        contentEnCollection.document(content.id).collection(actionCollection)
                .document(user.email!!).set(UserAction(Timestamp.now(), user.email!!))
                .addOnSuccessListener { status ->
                    updateContentActionCounter(content.id, countType)
                    updateUserActions(user.uid, actionCollection, content, countType)
                    updateQualityScore(score, content.id)
                }.addOnFailureListener { e ->
                    Log.e(LOG_TAG,
                            "Transaction failure update action $actionCollection ${countType}.", e)
                }
    }

    /**
     * Track user clearing the main feed of content in user profile and Google Analytics.
     *
     * @param userId String of active user
     */
    fun updateFeedEmptiedActionsAndAnalytics(userId: String) {
        updateUserActionCounter(userId, CLEAR_FEED_COUNT)
        firebaseAnalytics.logEvent(CLEAR_FEED_EVENT, Bundle().apply {
            putString(TIMESTAMP_PARAM, Timestamp.now().toString())
        })
    }

    /**
     * Track user labeling content in Google Analytics.
     *
     * @param content Content audiocast, video, or text media
     */
    fun labelContentFirebaseAnalytics(content: Content) {
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, content.id)
        bundle.putString(USER_ID_PARAM, FirebaseAuth.getInstance().currentUser?.uid)
        bundle.putString(CREATOR_PARAM, content.creator)
        firebaseAnalytics.logEvent(if (content.feedType == FeedType.SAVED) ORGANIZE_EVENT else DISMISS_EVENT, bundle)
    }

    //TODO: Move to Cloud Function
    private fun updateContentActionCounter(contentId: String, counterType: String) {
        contentEnCollection.document(contentId).apply {
            FirebaseFirestore.getInstance().runTransaction(Transaction.Function<String> { counterTransaction ->
                val contentSnapshot = counterTransaction.get(this)
                val newCounter = contentSnapshot.getDouble(counterType)!! + 1.0
                counterTransaction.update(this, counterType, newCounter)
                return@Function "Content counter update SUCCESS."
            }).addOnSuccessListener { status -> Log.v(LOG_TAG, status) }
                    .addOnFailureListener { e -> Log.e(LOG_TAG, "Content counter update FAIL.", e) }
        }
    }

    //TODO: Move to Cloud Function
    private fun updateUserActions(userId: String, actionCollection: String, content: Content, countType: String) {
        usersDocument.collection(userId).document(ACTIONS_DOCUMENT).collection(actionCollection)
                .document(content.id).set(ContentAction(Timestamp.now(), content.id, content.title, content.creator,
                        content.qualityScore)).addOnSuccessListener {
                    updateUserActionCounter(userId, countType)
                }.addOnFailureListener {
                    Log.e(LOG_TAG, "User content action update FAIL.")
                }
    }

    //TODO: Move to Cloud Function
    private fun updateUserActionCounter(userId: String, counterType: String) {
        usersDocument.collection(userId).document(ACTIONS_DOCUMENT).apply {
            FirebaseFirestore.getInstance().runTransaction(Transaction.Function<String> { counterTransaction ->
                counterTransaction.update(this, counterType,
                        counterTransaction.get(this).getDouble(counterType)!! + 1.0)
                return@Function "User counter update SUCCESS."
            }).addOnSuccessListener { status -> Log.v(LOG_TAG, status) }
                    .addOnFailureListener { e -> Log.e(LOG_TAG, "user counter update FAIL.", e) }
        }
    }

    //TODO: Move to Cloud Function
    private fun updateQualityScore(score: Double, contentId: String) {
        Log.d(LOG_TAG, "Transaction success: " + score)
        contentEnCollection.document(contentId).also {
            FirebaseFirestore.getInstance().runTransaction(object : Transaction.Function<Void> {
                @Throws(FirebaseFirestoreException::class)
                override fun apply(transaction: Transaction): Void? {
                    val snapshot = transaction.get(it)
                    val newQualityScore = snapshot.getDouble(QUALITY_SCORE)!! + score
                    transaction.update(it, QUALITY_SCORE, newQualityScore)
                    /* Success */ return null
                }
            }).addOnSuccessListener { Log.d(LOG_TAG, "Transaction success!") }
                    .addOnFailureListener { e -> Log.e(LOG_TAG, "Transaction failure updateQualityScore.", e) }
        }
    }
}