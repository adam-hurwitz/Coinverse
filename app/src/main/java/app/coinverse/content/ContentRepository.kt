package app.coinverse.content

import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import app.coinverse.Enums.FeedType
import app.coinverse.Enums.FeedType.*
import app.coinverse.Enums.Status
import app.coinverse.Enums.Status.ERROR
import app.coinverse.Enums.Status.SUCCESS
import app.coinverse.Enums.Timeframe
import app.coinverse.Enums.UserActionType
import app.coinverse.Enums.UserActionType.*
import app.coinverse.content.models.Content
import app.coinverse.content.models.UserAction
import app.coinverse.content.room.CoinverseDatabase
import app.coinverse.firebase.FirestoreCollections
import app.coinverse.firebase.FirestoreCollections.contentEnCollection
import app.coinverse.firebase.FirestoreCollections.usersCollection
import app.coinverse.user.models.ContentAction
import app.coinverse.utils.*
import app.coinverse.utils.DateAndTime.getTimeframe
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query.Direction.DESCENDING
import com.google.firebase.functions.FirebaseFunctions
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.ReplaySubject

class ContentRepository(application: Application) {
    private val LOG_TAG = ContentRepository::class.java.simpleName
    private lateinit var savedListenerRegistration: ListenerRegistration
    private lateinit var dismissedListenerRegistration: ListenerRegistration
    private lateinit var contentListenerRegistration: ListenerRegistration

    private var organizedSet = HashSet<String>()
    private var analytics: FirebaseAnalytics
    private var firestore: FirebaseFirestore
    private var database: CoinverseDatabase
    private var functions: FirebaseFunctions

    init {
        analytics = FirebaseAnalytics.getInstance(application)
        firestore = FirebaseFirestore.getInstance()
        database = CoinverseDatabase.getAppDatabase(application)
        functions = FirebaseFunctions.getInstance()
    }

    fun initMainContent(isRealtime: Boolean, timeframe: Timeframe): LiveData<Status> {
        val status = MutableLiveData<Status>()
        val contentDao = database.contentDao()
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val userReference = usersCollection.document(user.uid)
            organizedSet.clear()
            // Get Saved collection.
            savedListenerRegistration = userReference
                    .collection(SAVE_COLLECTION)
                    .orderBy(TIMESTAMP, DESCENDING)
                    .whereGreaterThanOrEqualTo(TIMESTAMP, getTimeframe(timeframe))
                    .addSnapshotListener(EventListener { value, error ->
                        error?.run {
                            status.value = ERROR
                            Log.e(LOG_TAG, "Get saved collection error. ${error.localizedMessage}")
                            return@EventListener
                        }
                        value!!.documentChanges.all { document ->
                            val savedContent = document.document.toObject(Content::class.java)
                            organizedSet.add(savedContent.id)
                            Thread(Runnable { run { contentDao.updateContentItem(savedContent) } }).start()
                            true
                        }
                    })
            // Get Dismissed collection.
            dismissedListenerRegistration = userReference
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
                            val dismissedContent = document.document.toObject(Content::class.java)
                            organizedSet.add(dismissedContent.id)
                            Thread(Runnable { run { contentDao.updateContentItem(dismissedContent) } }).start()
                            true
                        }
                    })
            //Logged in and realtime enabled.
            if (isRealtime) {
                contentListenerRegistration = FirestoreCollections.contentEnCollection
                        .orderBy(TIMESTAMP, DESCENDING)
                        .whereGreaterThanOrEqualTo(TIMESTAMP, getTimeframe(timeframe))
                        .addSnapshotListener(EventListener { value, error ->
                            error?.run {
                                status.value = ERROR
                                Log.e(LOG_TAG, "Main feed filter error. Logged In and Realtime. ${error.localizedMessage}")
                                return@EventListener
                            }
                            val contentList = arrayListOf<Content?>()
                            value!!.documentChanges.all { document ->
                                val content = document.document.toObject(Content::class.java)
                                if (!organizedSet.contains(content.id)) contentList.add(content)
                                true
                            }
                            Thread(Runnable { run { contentDao.insertContentList(contentList) } }).start()
                        })
            } else { // Logged in, not realtime.
                FirestoreCollections.contentEnCollection
                        .orderBy(TIMESTAMP, DESCENDING)
                        .whereGreaterThanOrEqualTo(TIMESTAMP, getTimeframe(timeframe))
                        .get()
                        .addOnCompleteListener {
                            val contentList = arrayListOf<Content?>()
                            it.result!!.documentChanges.all { document ->
                                val content = document.document.toObject(Content::class.java)
                                if (!organizedSet.contains(content.id)) contentList.add(content)
                                true
                            }
                            Thread(Runnable { run { contentDao.insertContentList(contentList) } }).start()
                            status.value = SUCCESS
                        }.addOnFailureListener {
                            status.value = ERROR
                            Log.e(LOG_TAG, "Main feed filter error. Logged In. ${it.localizedMessage}")
                        }
            }

        } else { // Logged out, not realtime.
            FirestoreCollections.contentEnCollection
                    .orderBy(TIMESTAMP, DESCENDING)
                    .whereGreaterThanOrEqualTo(TIMESTAMP, getTimeframe(timeframe))
                    .get()
                    .addOnCompleteListener {
                        val contentList = arrayListOf<Content?>()
                        it.result!!.documents.all { document ->
                            contentList.add(document.toObject(Content::class.java))
                            true
                        }
                        Thread(Runnable { run { contentDao.insertContentList(contentList) } }).start()
                        status.value = SUCCESS
                    }.addOnFailureListener {
                        status.value = ERROR
                        Log.e(LOG_TAG, "Main feed filter error. Logged out. ${it.localizedMessage}")
                    }
        }
        return status
    }

    fun getMainRoomContent(timeframe: Timestamp): DataSource.Factory<Int, Content> =
            database.contentDao().getMainContentList(timeframe, MAIN)

    fun getCategorizedRoomContent(feedType: FeedType): DataSource.Factory<Int, Content> =
            database.contentDao().getCategorizedContentList(feedType)

    fun organizeContent(feedType: String, actionType: UserActionType, content: Content?,
                        user: FirebaseUser, mainFeedEmptied: Boolean): Observable<Status> {
        val statusSubscriber = ReplaySubject.create<Status>()
        val userReference = usersCollection.document(user.uid)
        // Add content to new collection.
        if (actionType == SAVE) {
            if (feedType == MAIN.name) updateActions(actionType, content!!, user)
            else if (feedType == DISMISSED.name) deleteContent(userReference, DISMISS_COLLECTION, content)
            content?.feedType = SAVED
            setContent(feedType, userReference, SAVE_COLLECTION, content, mainFeedEmptied)
                    .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                    .subscribe { status -> statusSubscriber.onNext(status) }
        } else if (actionType == DISMISS) {
            if (feedType == MAIN.name) updateActions(actionType, content!!, user)
            else if (feedType == SAVED.name) deleteContent(userReference, SAVE_COLLECTION, content)
            content?.feedType = DISMISSED
            setContent(feedType, userReference, DISMISS_COLLECTION, content, mainFeedEmptied)
                    .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                    .subscribe { status -> statusSubscriber.onNext(status) }
        }
        if (mainFeedEmptied) {
            analytics.logEvent(CLEAR_FEED_EVENT, Bundle().apply {
                putString(TIMESTAMP_PARAM, Timestamp.now().toString())
            })
            updateUserActionCounter(user.uid, CLEAR_FEED_COUNT)
        }
        return statusSubscriber
    }

    fun setContent(feedType: String, userReference: DocumentReference, collection: String,
                   content: Content?, mainFeedEmptied: Boolean): Observable<Status> {
        val statusSubscriber = ReplaySubject.create<Status>()
        userReference.collection(collection).document(content!!.id).set(content)
                .addOnSuccessListener {
                    logCategorizeContentAnalyticsEvent(feedType, content, mainFeedEmptied)
                    Thread(Runnable { run { database.contentDao().updateContentItem(content) } }).start()
                    statusSubscriber.onNext(SUCCESS)
                    Log.v(LOG_TAG, String.format("Content added to collection:%s", it))
                }.addOnFailureListener {
                    statusSubscriber.onNext(ERROR)
                    Log.e(LOG_TAG, String.format("Content failed to be added to collection:%s", it))
                }
        return statusSubscriber
    }

    fun deleteContent(userReference: DocumentReference, collection: String, content: Content?) {
        userReference
                .collection(collection)
                .document(content!!.id)
                .delete()
                .addOnSuccessListener {
                    //TODO: Add Observerable to animate RecyclerView
                    Log.v(LOG_TAG, String.format("Content deleted from to collection:%s", it))
                }.addOnFailureListener {
                    Log.e(LOG_TAG, String.format("Content failed to be deleted from collection:%s", it))
                }
    }

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
                .document(user.email!!).set(UserAction(Timestamp.now(), user.email!!))
                .addOnSuccessListener { status ->
                    updateContentActionCounter(content.id, countType)
                    updateUserActions(user.uid, actionCollection, content, countType)
                    updateQualityScore(score, content.id)
                }.addOnFailureListener { e -> Log.w(LOG_TAG, "Transaction failure.", e) }
    }

    fun updateContentActionCounter(contentId: String, counterType: String) {
        val contentRef = contentEnCollection.document(contentId)
        firestore.runTransaction(Transaction.Function<String> { counterTransaction ->
            val contentSnapshot = counterTransaction.get(contentRef)
            val newCounter = contentSnapshot.getDouble(counterType)!! + 1.0
            counterTransaction.update(contentRef, counterType, newCounter)
            return@Function "Content counter update SUCCESS."
        }).addOnSuccessListener { status ->
            Log.w(LOG_TAG, status)
        }.addOnFailureListener { e ->
            Log.w(LOG_TAG, "Content counter update FAIL.", e)
        }
    }

    fun updateUserActions(userId: String, actionCollection: String, content: Content, countType: String) {
        usersCollection.document(userId).collection(actionCollection).document(content.id)
                .set(ContentAction(Timestamp.now(), content.id, content.title, content.creator,
                        content.qualityScore)).addOnSuccessListener {
                    updateUserActionCounter(userId, countType)
                }.addOnFailureListener {
                    Log.w(LOG_TAG, "User content action update FAIL.")
                }
    }

    fun updateUserActionCounter(userId: String, counterType: String) {
        val userRef = usersCollection.document(userId)
        firestore.runTransaction(Transaction.Function<String> { counterTransaction ->
            val userSnapshot = counterTransaction.get(userRef)
            val newCounter = userSnapshot.getDouble(counterType)!! + 1.0
            counterTransaction.update(userRef, counterType, newCounter)
            return@Function "User counter update SUCCESS."
        }).addOnSuccessListener { status -> Log.w(LOG_TAG, status) }
                .addOnFailureListener { e -> Log.w(LOG_TAG, "user counter update FAIL.", e) }
    }

    fun updateQualityScore(score: Double, contentId: String) {
        Log.d(LOG_TAG, "Transaction success: " + score)
        val contentDocRef = FirestoreCollections.contentEnCollection.document(contentId)
        firestore.runTransaction(object : Transaction.Function<Void> {
            @Throws(FirebaseFirestoreException::class)
            override fun apply(transaction: Transaction): Void? {
                val snapshot = transaction.get(contentDocRef)
                val newQualityScore = snapshot.getDouble(QUALITY_SCORE)!! + score
                transaction.update(contentDocRef, QUALITY_SCORE, newQualityScore)
                // Success
                return null
            }
        }).addOnSuccessListener({ Log.d(LOG_TAG, "Transaction success!") })
                .addOnFailureListener({ e -> Log.w(LOG_TAG, "Transaction failure.", e) })
    }

    fun logCategorizeContentAnalyticsEvent(feedType: String, content: Content, mainFeedEmptied: Boolean) {
        if (feedType == MAIN.name) {
            var logEvent = ""
            if (content.feedType == SAVED) {
                logEvent = ORGANIZE_EVENT
            } else if (content.feedType == DISMISSED) {
                logEvent = DISMISS_EVENT
            }
            val bundle = Bundle()
            bundle.putString(FirebaseAnalytics.Param.ITEM_ID, content.id)
            bundle.putString(USER_ID_PARAM, FirebaseAuth.getInstance().currentUser?.uid)
            bundle.putString(CREATOR_PARAM, content.creator)
            analytics.logEvent(logEvent, bundle)
        }
    }

    fun getAudiocast(debugEnabled: Boolean, content: Content): Task<HashMap<String, String>> {
        val data = hashMapOf(
                DEBUG_ENABLED_PARAM to debugEnabled,
                CONTENT_ID_PARAM to content.id,
                CONTENT_TITLE_PARAM to content.title,
                CONTENT_PREVIEW_IMAGE_PARAM to content.previewImage)
        return functions
                .getHttpsCallable(GET_AUDIOCAST_FUNCTION)
                .call(data)
                .continueWith { task -> (task.result?.data as HashMap<String, String>) }
    }
}