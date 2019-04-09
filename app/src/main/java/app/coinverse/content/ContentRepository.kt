package app.coinverse.content

import android.app.Application
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import app.coinverse.BuildConfig.DEBUG
import app.coinverse.Enums.FeedType
import app.coinverse.Enums.FeedType.*
import app.coinverse.Enums.Status
import app.coinverse.Enums.Status.ERROR
import app.coinverse.Enums.Status.SUCCESS
import app.coinverse.Enums.Timeframe
import app.coinverse.Enums.UserActionType
import app.coinverse.Enums.UserActionType.*
import app.coinverse.content.models.Content
import app.coinverse.content.models.ContentSwipedStatus
import app.coinverse.content.models.UserAction
import app.coinverse.content.room.CoinverseDatabase
import app.coinverse.firebase.*
import app.coinverse.user.models.ContentAction
import app.coinverse.utils.*
import app.coinverse.utils.DateAndTime.getTimeframe
import app.coinverse.utils.livedata.Event
import com.google.firebase.Timestamp
import com.google.firebase.Timestamp.now
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.FirebaseAnalytics.Param.ITEM_ID
import com.google.firebase.analytics.FirebaseAnalytics.getInstance
import com.google.firebase.auth.FirebaseAuth.getInstance
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import com.google.firebase.firestore.Query.Direction.DESCENDING
import com.google.firebase.functions.FirebaseFunctions

class ContentRepository(application: Application) {
    private val LOG_TAG = ContentRepository::class.java.simpleName

    private var organizedSet = HashSet<String>()
    private var analytics: FirebaseAnalytics
    private var firestore: FirebaseFirestore
    private var database: CoinverseDatabase
    private var functions: FirebaseFunctions

    init {
        analytics = getInstance(application)
        firestore = FirebaseFirestore.getInstance()
        database = CoinverseDatabase.getAppDatabase(application)
        functions = FirebaseFunctions.getInstance()
    }

    //TODO: Return LiveData of Status.
    //TODO: Error message.
    fun initMainContent(isRealtime: Boolean, timeframe: Timeframe) =
            MutableLiveData<Status>().also { status ->
                if (getInstance().currentUser != null) {
                    usersDocument.collection(getInstance().currentUser!!.uid).also { user ->
                        organizedSet.clear()
                        // Get Saved collection.
                        user.document(COLLECTIONS_DOCUMENT)
                                .collection(SAVE_COLLECTION).orderBy(TIMESTAMP, DESCENDING)
                                .whereGreaterThanOrEqualTo(TIMESTAMP, getTimeframe(timeframe))
                                .addSnapshotListener(EventListener { value, error ->
                                    error?.run {
                                        status.value = ERROR
                                        Log.e(LOG_TAG, "Get saved collection error. ${error.localizedMessage}")
                                        return@EventListener
                                    }
                                    value!!.documentChanges.all { document ->
                                        document.document.toObject(Content::class.java).let { savedContent ->
                                            organizedSet.add(savedContent.id)
                                            Thread(Runnable {
                                                run { database.contentDao().updateContentItem(savedContent) }
                                            }).start()
                                        }
                                        true
                                    }
                                })
                        // Get Dismissed collection.
                        user.document(COLLECTIONS_DOCUMENT)
                                .collection(DISMISS_COLLECTION)
                                .orderBy(TIMESTAMP, DESCENDING)
                                .whereGreaterThanOrEqualTo(TIMESTAMP, getTimeframe(timeframe))
                                .addSnapshotListener(EventListener { value, error ->
                                    error?.run {
                                        status.value = ERROR
                                        Log.e(LOG_TAG, "Get dismissed collection error. ${error.localizedMessage}")
                                        return@EventListener
                                    }
                                    value!!.documentChanges.all { document ->
                                        document.document.toObject(Content::class.java).let { dismissedContent ->
                                            organizedSet.add(dismissedContent.id)
                                            Thread(Runnable {
                                                run { database.contentDao().updateContentItem(dismissedContent) }
                                            }).start()
                                        }
                                        true
                                    }
                                })
                        //Logged in and realtime enabled.
                        if (isRealtime) {
                            contentEnCollection.orderBy(TIMESTAMP, DESCENDING)
                                    .whereGreaterThanOrEqualTo(TIMESTAMP, getTimeframe(timeframe))
                                    .addSnapshotListener(EventListener { value, error ->
                                        error?.run {
                                            status.value = ERROR
                                            Log.e(LOG_TAG, "Main feed filter error. Logged In and Realtime. ${error.localizedMessage}")
                                            return@EventListener
                                        }
                                        arrayListOf<Content?>().also { contentList ->
                                            value!!.documentChanges.all { document ->
                                                document.document.toObject(Content::class.java).also { content ->
                                                    if (!organizedSet.contains(content.id))
                                                        contentList.add(content)
                                                }
                                                true
                                            }
                                            Thread(Runnable {
                                                run { database.contentDao().insertContentList(contentList) }
                                            }).start()
                                        }
                                    })
                        } else { // Logged in, not realtime.
                            contentEnCollection.orderBy(TIMESTAMP, DESCENDING)
                                    .whereGreaterThanOrEqualTo(TIMESTAMP, getTimeframe(timeframe))
                                    .get().addOnCompleteListener {
                                        arrayListOf<Content?>().also { contentList ->
                                            it.result!!.documentChanges.all { document ->
                                                document.document.toObject(Content::class.java).also { content ->
                                                    if (!organizedSet.contains(content.id))
                                                        contentList.add(content)
                                                }
                                                true
                                            }
                                            Thread(Runnable {
                                                run { database.contentDao().insertContentList(contentList) }
                                            }).start()
                                        }
                                        status.value = SUCCESS
                                    }.addOnFailureListener {
                                        status.value = ERROR
                                        Log.e(LOG_TAG, "Main feed filter error. Logged In. ${it.localizedMessage}")
                                    }
                        }
                    }
                } else { // Logged out, not realtime.
                    contentEnCollection.orderBy(TIMESTAMP, DESCENDING)
                            .whereGreaterThanOrEqualTo(TIMESTAMP, getTimeframe(timeframe))
                            .get()
                            .addOnCompleteListener {
                                arrayListOf<Content?>().also { contentList ->
                                    it.result!!.documents.all { document ->
                                        contentList.add(document.toObject(Content::class.java))
                                        true
                                    }
                                    Thread(Runnable {
                                        run { database.contentDao().insertContentList(contentList) }
                                    }).start()
                                }
                                status.value = SUCCESS
                            }.addOnFailureListener {
                                status.value = ERROR
                                Log.e(LOG_TAG, "Main feed filter error. Logged out. ${it.localizedMessage}")
                            }
                }
            }

    fun getContent(contentId: String) = MutableLiveData<Event<Content>>().apply {
        contentEnCollection.document(contentId).get().addOnCompleteListener { result ->
            this.value = Event((result.result?.toObject(Content::class.java)!!))
        }
    }

    fun getMainRoomContent(timeframe: Timestamp): DataSource.Factory<Int, Content> =
            database.contentDao().getMainContentList(timeframe, MAIN)

    fun getCategorizedRoomContent(feedType: FeedType): DataSource.Factory<Int, Content> =
            database.contentDao().getCategorizedContentList(feedType)

    //TODO: Return LiveData of Status.
    //TODO: Custom response object 1) deleteStatus 2) setContent status 3) mainFeedEmptiedStatus.
    fun organizeContent(feedType: FeedType, actionType: UserActionType, content: Content?,
                        user: FirebaseUser, mainFeedEmptied: Boolean) =
            MutableLiveData<ContentSwipedStatus>().apply {
                usersDocument.collection(user.uid).also { userReference ->
                    content?.feedType =
                            if (actionType == SAVE) SAVED
                            else if (actionType == DISMISS) DISMISSED
                            else MAIN
                    if (actionType == SAVE || actionType == DISMISS) {
                        if (feedType == SAVED || feedType == DISMISSED)
                            deleteContent(
                                    userReference,
                                    if (actionType == SAVE && feedType == DISMISSED) DISMISS_COLLECTION
                                    else if (actionType == DISMISS && feedType == SAVED) SAVE_COLLECTION
                                    else "",
                                    content)
                        setContent(
                                feedType,
                                userReference,
                                if (actionType == SAVE) SAVE_COLLECTION
                                else if (actionType == DISMISS) DISMISS_COLLECTION
                                else "",
                                content,
                                mainFeedEmptied)
                    }
                    if (mainFeedEmptied) {
                        analytics.logEvent(CLEAR_FEED_EVENT, Bundle().apply {
                            putString(TIMESTAMP_PARAM, now().toString())
                        })
                        updateUserActionCounter(user.uid, CLEAR_FEED_COUNT)
                    }
                }
            }

    //TODO: Return LiveData of Status.
    //TODO: Error message.
    fun setContent(feedType: FeedType, userCollection: CollectionReference, collection: String,
                   content: Content?, mainFeedEmptied: Boolean) =
            MutableLiveData<ContentSwipedStatus>().apply {
                userCollection.document(COLLECTIONS_DOCUMENT).collection(collection).document(content!!.id)
                        .set(content).addOnSuccessListener {
                            logCategorizeContentAnalyticsEvent(feedType, content, mainFeedEmptied)
                            Thread(Runnable { run { database.contentDao().updateContentItem(content) } }).start()
                            this.value = ContentSwipedStatus(feedType, SUCCESS)
                            Log.v(LOG_TAG, "Content '${content.title}' added to collection $collection")
                        }.addOnFailureListener {
                            this.value = ContentSwipedStatus(feedType, ERROR)
                            Log.e(LOG_TAG, "Content failed to be added to collection ${it}")
                        }
            }

    fun updateContentAudioUrl(contentId: String, url: Uri) =
            contentEnCollection.document(contentId)
                    .update(AUDIO_URL, Regex(AUDIO_URL_TOKEN_REGEX).replace(url.toString(), ""))

    //TODO: Return LiveData of Status.
    //TODO: Error message.
    fun deleteContent(userReference: CollectionReference, collection: String, content: Content?) =
            MutableLiveData<Status>().apply {
                userReference.document(COLLECTIONS_DOCUMENT).collection(collection).document(content!!.id)
                        .delete().addOnSuccessListener {
                            this.value = SUCCESS
                            Log.v(LOG_TAG, String.format("Content deleted from to collection:%s", it))
                        }.addOnFailureListener {
                            this.value = ERROR
                            Log.e(LOG_TAG, String.format("Content failed to be deleted from collection:%s", it))
                        }
            }

    //TODO: Return LiveData of Status.
    //TODO: Error message.
    //TODO: Custom response object 1) updateContentActionCounter 2) updateUserActions 3) updateQualityScore
    fun updateActions(actionType: UserActionType, content: Content, user: FirebaseUser) {
        var actionCollection = ""
        var score = INVALID_SCORE
        var countType = ""
        when (actionType) {
            START -> {
                actionCollection = START_ACTION_COLLECTION
                score = START_SCORE
                countType = START_COUNT
            }
            CONSUME -> {
                actionCollection = CONSUME_ACTION_COLLECTION
                score = CONSUME_SCORE
                countType = CONSUME_COUNT
            }
            FINISH -> {
                actionCollection = FINISH_ACTION_COLLECTION
                score = FINISH_SCORE
                countType = FINISH_COUNT
            }
            SAVE -> {
                actionCollection = SAVE_ACTION_COLLECTION
                score = SAVE_SCORE
                countType = ORGANIZE_COUNT
            }
            DISMISS -> {
                actionCollection = DISMISS_ACTION_COLLECTION
                score = DISMISS_SCORE
                countType = DISMISS_COUNT
            }
        }
        contentEnCollection.document(content.id).collection(actionCollection)
                .document(user.email!!).set(UserAction(now(), user.email!!))
                .addOnSuccessListener { status ->
                    updateContentActionCounter(content.id, countType)
                    updateUserActions(user.uid, actionCollection, content, countType)
                    updateQualityScore(score, content.id)
                }.addOnFailureListener { e ->
                    Log.e(LOG_TAG,
                            "Transaction failure update action $actionCollection ${countType}.", e)
                }
    }

    //TODO: Return LiveData of Status.
    //TODO: Error message.
    fun updateContentActionCounter(contentId: String, counterType: String) {
        contentEnCollection.document(contentId).apply {
            firestore.runTransaction(Transaction.Function<String> { counterTransaction ->
                val contentSnapshot = counterTransaction.get(this)
                val newCounter = contentSnapshot.getDouble(counterType)!! + 1.0
                counterTransaction.update(this, counterType, newCounter)
                return@Function "Content counter update SUCCESS."
            }).addOnSuccessListener { status -> Log.v(LOG_TAG, status) }
                    .addOnFailureListener { e -> Log.e(LOG_TAG, "Content counter update FAIL.", e) }
        }
    }

    //TODO: Return LiveData of Status.
    //TODO: Error message.
    fun updateUserActions(userId: String, actionCollection: String, content: Content, countType: String) {
        usersDocument.collection(userId).document(ACTIONS_DOCUMENT).collection(actionCollection)
                .document(content.id).set(ContentAction(now(), content.id, content.title, content.creator,
                        content.qualityScore)).addOnSuccessListener {
                    updateUserActionCounter(userId, countType)
                }.addOnFailureListener {
                    Log.e(LOG_TAG, "User content action update FAIL.")
                }
    }

    //TODO: Return LiveData of Status.
    //TODO: Error message.
    fun updateUserActionCounter(userId: String, counterType: String) {
        usersDocument.collection(userId).document(ACTIONS_DOCUMENT).apply {
            firestore.runTransaction(Transaction.Function<String> { counterTransaction ->
                val newCounter = counterTransaction.get(this).getDouble(counterType)!! + 1.0
                counterTransaction.update(this, counterType, newCounter)
                return@Function "User counter update SUCCESS."
            }).addOnSuccessListener { status -> Log.v(LOG_TAG, status) }
                    .addOnFailureListener { e -> Log.e(LOG_TAG, "user counter update FAIL.", e) }
        }
    }

    //TODO: Return LiveData of Status.
    //TODO: Error message.
    fun updateQualityScore(score: Double, contentId: String) {
        Log.d(LOG_TAG, "Transaction success: " + score)
        contentEnCollection.document(contentId).also {
            firestore.runTransaction(object : Transaction.Function<Void> {
                @Throws(FirebaseFirestoreException::class)
                override fun apply(transaction: Transaction): Void? {
                    val snapshot = transaction.get(it)
                    val newQualityScore = snapshot.getDouble(QUALITY_SCORE)!! + score
                    transaction.update(it, QUALITY_SCORE, newQualityScore)
                    // Success
                    return null
                }
            }).addOnSuccessListener({ Log.d(LOG_TAG, "Transaction success!") })
                    .addOnFailureListener({ e ->
                        Log.e(LOG_TAG, "Transaction failure updateQualityScore.", e)
                    })
        }
    }

    fun logCategorizeContentAnalyticsEvent(feedType: FeedType, content: Content, mainFeedEmptied: Boolean) {
        if (feedType == MAIN)
            Bundle().apply {
                this.putString(ITEM_ID, content.id)
                this.putString(USER_ID_PARAM, getInstance().currentUser?.uid)
                this.putString(CREATOR_PARAM, content.creator)
                analytics.logEvent(
                        if (content.feedType == SAVED) ORGANIZE_EVENT else DISMISS_EVENT, this)
            }
    }

    fun getAudiocast(content: Content) =
            functions.getHttpsCallable(GET_AUDIOCAST_FUNCTION).call(
                    hashMapOf(
                            DEBUG_ENABLED_PARAM to DEBUG,
                            CONTENT_ID_PARAM to content.id,
                            CONTENT_TITLE_PARAM to content.title,
                            CONTENT_PREVIEW_IMAGE_PARAM to content.previewImage))
                    .continueWith { task -> (task.result?.data as HashMap<String, String>) }
}