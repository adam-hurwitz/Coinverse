package app.coinverse.content

import android.app.Application
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.LiveData
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
import app.coinverse.content.models.UserAction
import app.coinverse.content.room.CoinverseDatabase
import app.coinverse.firebase.*
import app.coinverse.user.models.ContentAction
import app.coinverse.utils.*
import app.coinverse.utils.DateAndTime.getTimeframe
import com.google.firebase.Timestamp
import com.google.firebase.Timestamp.now
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query.Direction.DESCENDING
import com.google.firebase.functions.FirebaseFunctions
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
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
            val userReference = usersDocument.collection(user.uid)
            organizedSet.clear()
            // Get Saved collection.
            savedListenerRegistration = userReference.document(COLLECTIONS_DOCUMENT)
                    .collection(SAVE_COLLECTION).orderBy(TIMESTAMP, DESCENDING)
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
                    .document(COLLECTIONS_DOCUMENT).collection(DISMISS_COLLECTION)
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
                contentListenerRegistration = contentEnCollection.orderBy(TIMESTAMP, DESCENDING)
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
                contentEnCollection.orderBy(TIMESTAMP, DESCENDING)
                        .whereGreaterThanOrEqualTo(TIMESTAMP, getTimeframe(timeframe))
                        .get().addOnCompleteListener {
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
            contentEnCollection.orderBy(TIMESTAMP, DESCENDING)
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

    fun getContent(contentId: String): Observable<Content> {
        val contentSubscriber = ReplaySubject.create<Content>()
        contentEnCollection.document(contentId).get().addOnCompleteListener { result ->
            contentSubscriber.onNext(result.result?.toObject(Content::class.java)!!)
        }
        return contentSubscriber
    }

    fun getMainRoomContent(timeframe: Timestamp): DataSource.Factory<Int, Content> =
            database.contentDao().getMainContentList(timeframe, MAIN)

    fun getCategorizedRoomContent(feedType: FeedType): DataSource.Factory<Int, Content> =
            database.contentDao().getCategorizedContentList(feedType)

    fun organizeContent(feedType: String, actionType: UserActionType, content: Content?,
                        user: FirebaseUser, mainFeedEmptied: Boolean): Observable<Status> {
        val statusSubscriber = ReplaySubject.create<Status>()
        val userReference = usersDocument.collection(user.uid)
        if (actionType == SAVE) {
            content?.feedType = SAVED
            if (feedType == DISMISSED.name) deleteContent(userReference, DISMISS_COLLECTION, content)
            setContent(feedType, userReference, SAVE_COLLECTION, content, mainFeedEmptied)
                    .subscribeOn(mainThread()).observeOn(mainThread()).subscribe { status ->
                        if (status == SUCCESS && feedType == MAIN.name) updateActions(actionType, content!!, user)
                        statusSubscriber.onNext(status)
                    }
        } else if (actionType == DISMISS) {
            content?.feedType = DISMISSED
            if (feedType == SAVED.name) deleteContent(userReference, SAVE_COLLECTION, content)
            setContent(feedType, userReference, DISMISS_COLLECTION, content, mainFeedEmptied)
                    .subscribeOn(mainThread()).observeOn(mainThread()).subscribe { status ->
                        if (status == SUCCESS && feedType == MAIN.name) updateActions(actionType, content!!, user)
                        statusSubscriber.onNext(status)
                    }
        }
        if (mainFeedEmptied) {
            analytics.logEvent(CLEAR_FEED_EVENT, Bundle().apply {
                putString(TIMESTAMP_PARAM, now().toString())
            })
            updateUserActionCounter(user.uid, CLEAR_FEED_COUNT)
        }
        return statusSubscriber
    }

    fun setContent(feedType: String, userCollection: CollectionReference, collection: String,
                   content: Content?, mainFeedEmptied: Boolean): Observable<Status> {
        val statusSubscriber = ReplaySubject.create<Status>()
        userCollection.document(COLLECTIONS_DOCUMENT).collection(collection).document(content!!.id)
                .set(content).addOnSuccessListener {
                    logCategorizeContentAnalyticsEvent(feedType, content, mainFeedEmptied)
                    Thread(Runnable { run { database.contentDao().updateContentItem(content) } }).start()
                    statusSubscriber.onNext(SUCCESS)
                    Log.v(LOG_TAG, "Content '${content.title}' added to collection $collection")
                }.addOnFailureListener {
                    statusSubscriber.onNext(ERROR)
                    Log.e(LOG_TAG, "Content failed to be added to collection ${it}")
                }
        return statusSubscriber
    }

    fun updateContentDb(content: Content) = Thread(Runnable { run {database.contentDao().updateContentItem(content)}}).start()

    fun updateContentAudioUrl(contentId: String, url: Uri) = contentEnCollection.document(contentId)
            .update(AUDIO_URL, Regex(AUDIO_URL_TOKEN_REGEX).replace(url.toString(), ""))

    fun deleteContent(userReference: CollectionReference, collection: String, content: Content?): Observable<Status> {
        val statusSubscriber = ReplaySubject.create<Status>()
        userReference.document(COLLECTIONS_DOCUMENT).collection(collection).document(content!!.id)
                .delete().addOnSuccessListener {
                    statusSubscriber.onNext(SUCCESS)
                    Log.v(LOG_TAG, String.format("Content deleted from to collection:%s", it))
                }.addOnFailureListener {
                    statusSubscriber.onNext(ERROR)
                    Log.e(LOG_TAG, String.format("Content failed to be deleted from collection:%s", it))
                }
        return statusSubscriber
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

    fun updateContentActionCounter(contentId: String, counterType: String) {
        val contentRef = contentEnCollection.document(contentId)
        firestore.runTransaction(Transaction.Function<String> { counterTransaction ->
            val contentSnapshot = counterTransaction.get(contentRef)
            val newCounter = contentSnapshot.getDouble(counterType)!! + 1.0
            counterTransaction.update(contentRef, counterType, newCounter)
            return@Function "Content counter update SUCCESS."
        }).addOnSuccessListener { status -> Log.v(LOG_TAG, status) }
                .addOnFailureListener { e -> Log.e(LOG_TAG, "Content counter update FAIL.", e) }
    }

    fun updateUserActions(userId: String, actionCollection: String, content: Content, countType: String) {
        usersDocument.collection(userId).document(ACTIONS_DOCUMENT).collection(actionCollection)
                .document(content.id).set(ContentAction(now(), content.id, content.title, content.creator,
                        content.qualityScore)).addOnSuccessListener {
                    updateUserActionCounter(userId, countType)
                }.addOnFailureListener {
                    Log.e(LOG_TAG, "User content action update FAIL.")
                }
    }

    fun updateUserActionCounter(userId: String, counterType: String) {
        val userRef = usersDocument.collection(userId).document(ACTIONS_DOCUMENT)
        firestore.runTransaction(Transaction.Function<String> { counterTransaction ->
            val newCounter = counterTransaction.get(userRef).getDouble(counterType)!! + 1.0
            counterTransaction.update(userRef, counterType, newCounter)
            return@Function "User counter update SUCCESS."
        }).addOnSuccessListener { status -> Log.v(LOG_TAG, status) }
                .addOnFailureListener { e -> Log.e(LOG_TAG, "user counter update FAIL.", e) }
    }

    fun updateQualityScore(score: Double, contentId: String) {
        Log.d(LOG_TAG, "Transaction success: " + score)
        val contentDocRef = contentEnCollection.document(contentId)
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
                .addOnFailureListener({ e -> Log.e(LOG_TAG, "Transaction failure updateQualityScore.", e) })
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

    fun getAudiocast(content: Content) = functions.getHttpsCallable(GET_AUDIOCAST_FUNCTION).call(
            hashMapOf(
                    DEBUG_ENABLED_PARAM to DEBUG,
                    CONTENT_ID_PARAM to content.id,
                    CONTENT_TITLE_PARAM to content.title,
                    CONTENT_PREVIEW_IMAGE_PARAM to content.previewImage))
            .continueWith { task -> (task.result?.data as HashMap<String, String>) }
}