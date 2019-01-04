package app.coinverse.content

import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.paging.DataSource
import app.coinverse.Enums
import app.coinverse.Enums.FeedType
import app.coinverse.Enums.FeedType.*
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
import com.google.firebase.functions.FirebaseFunctions

class ContentRepository(application: Application) {

    //TODO: Needs further testing.
    //val isNewContentAddedLiveData = MutableLiveData<Boolean>()

    private val LOG_TAG = ContentRepository::javaClass.name
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

    fun initializeMainRoomContent(isRealtime: Boolean, timeframe: Enums.Timeframe) {
        val contentDao = database.contentDao()
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val userReference = usersCollection.document(user.uid)
            organizedSet.clear()
            // Get Saved collection.
            savedListenerRegistration = userReference
                    .collection(SAVE_COLLECTION)
                    .orderBy(TIMESTAMP, Query.Direction.DESCENDING)
                    .whereGreaterThanOrEqualTo(TIMESTAMP, getTimeframe(timeframe))
                    .addSnapshotListener(EventListener { value, error ->
                        error?.run {
                            Log.e(LOG_TAG, "Content EventListener Failed.", error)
                            return@EventListener
                        }
                        for (document in value!!.documentChanges) {
                            val savedContent = document.document.toObject(Content::class.java)
                            organizedSet.add(savedContent.id)
                            Thread(Runnable { run { contentDao.updateContent(savedContent) } }).start()
                        }
                    })
            // Get Dismissed collection.
            dismissedListenerRegistration = userReference
                    .collection(DISMISS_COLLECTION)
                    .orderBy(TIMESTAMP, Query.Direction.DESCENDING)
                    .whereGreaterThanOrEqualTo(TIMESTAMP, getTimeframe(timeframe))
                    .addSnapshotListener(EventListener { value, error ->
                        error?.run {
                            Log.e(LOG_TAG, "Content EventListener Failed.", error)
                            return@EventListener
                        }
                        for (document in value!!.documentChanges) {
                            val dismissedContent = document.document.toObject(Content::class.java)
                            organizedSet.add(dismissedContent.id)
                            Thread(Runnable { run { contentDao.updateContent(dismissedContent) } }).start()
                        }
                    })
            //Logged in and realtime enabled.
            if (isRealtime) {
                contentListenerRegistration = FirestoreCollections.contentEnCollection
                        .orderBy(TIMESTAMP, Query.Direction.DESCENDING)
                        .whereGreaterThanOrEqualTo(TIMESTAMP, getTimeframe(timeframe))
                        .addSnapshotListener(EventListener { value, error ->
                            error?.run {
                                Log.e(LOG_TAG, "Content EventListener Failed.", error)
                                return@EventListener
                            }
                            val contentList = arrayListOf<Content?>()
                            for (document in value!!.documentChanges) {
                                val content = document.document.toObject(Content::class.java)
                                if (!organizedSet.contains(content.id)) {
                                    //TODO: Needs further testing.
                                    //isNewContentAddedLiveData.value = true
                                    contentList.add(content)
                                }
                            }
                            Thread(Runnable { run { contentDao.insertContent(contentList) } }).start()
                        })
            } else { // Logged in but not realtime.
                FirestoreCollections.contentEnCollection
                        .orderBy(TIMESTAMP, Query.Direction.DESCENDING)
                        .whereGreaterThanOrEqualTo(TIMESTAMP, getTimeframe(timeframe))
                        .get()
                        .addOnCompleteListener {
                            val contentList = arrayListOf<Content?>()
                            for (document in it.result!!.documentChanges) {
                                val content = document.document.toObject(Content::class.java)
                                if (!organizedSet.contains(content.id)) {
                                    //TODO: Needs further testing.
                                    //isNewContentAddedLiveData.value = true
                                    contentList.add(content)
                                }
                            }
                            Thread(Runnable { run { contentDao.insertContent(contentList) } }).start()
                        }
            }

        } else { // Looged out and thus not realtime.
            FirestoreCollections.contentEnCollection
                    .orderBy(TIMESTAMP, Query.Direction.DESCENDING)
                    .whereGreaterThanOrEqualTo(TIMESTAMP, getTimeframe(timeframe))
                    .get()
                    .addOnCompleteListener {
                        val contentList = arrayListOf<Content?>()
                        for (document in it.result!!.documents) {
                            val content = document.toObject(Content::class.java)
                            contentList.add(content)
                        }
                        Thread(Runnable { run { contentDao.insertContent(contentList) } }).start()
                    }
        }
    }

    fun initializeCategorizedRoomContent(feedType: String, userId: String) {
        var collectionType = ""
        var newFeedType = FeedType.NONE
        if (feedType == SAVED.name) {
            collectionType = SAVE_COLLECTION
            newFeedType = SAVED
        } else if (feedType == DISMISSED.name) {
            collectionType = DISMISS_COLLECTION
            newFeedType = DISMISSED
        }
        FirestoreCollections.contentEnCollection
                .document(userId)
                .collection(collectionType)
                .orderBy(TIMESTAMP, Query.Direction.DESCENDING)
                .addSnapshotListener(EventListener { value, error ->
                    error?.run {
                        Log.e(LOG_TAG, "Content EventListener Failed.", error)
                        return@EventListener
                    }
                    val contentList = arrayListOf<Content?>()
                    for (document in value!!.documentChanges) {
                        val content = document.document.toObject(Content::class.java)
                        content.feedType = newFeedType
                        contentList.add(content)
                    }
                    Thread(Runnable { run { database.contentDao().insertContent(contentList) } }).start()
                })
    }

    fun getMainContent(timeframe: Timestamp): DataSource.Factory<Int, Content> =
            database.contentDao().getMainContent(timeframe, MAIN)

    fun getCategorizedContent(feedType: FeedType): DataSource.Factory<Int, Content> =
            database.contentDao().getCategorizedContent(feedType)

    fun organizeContent(feedType: String, actionType: UserActionType, content: Content?,
                        user: FirebaseUser, mainFeedEmptied: Boolean) {
        val userReference = usersCollection.document(user.uid)

        // Add content to new collection.
        if (actionType == SAVE) {
            if (feedType == MAIN.name) {
                updateActions(actionType, content!!, user)
            } else if (feedType == DISMISSED.name) {
                deleteContent(userReference, DISMISS_COLLECTION, content)
            }
            content?.feedType = SAVED
            setContent(feedType, userReference, SAVE_COLLECTION, content, mainFeedEmptied)
        } else if (actionType == DISMISS) {
            if (feedType == MAIN.name) {
                updateActions(actionType, content!!, user)
            } else if (feedType == SAVED.name) {
                deleteContent(userReference, SAVE_COLLECTION, content)
            }
            content?.feedType = DISMISSED
            setContent(feedType, userReference, DISMISS_COLLECTION, content, mainFeedEmptied)
        }

        if (mainFeedEmptied) {
            val bundle = Bundle()
            bundle.putString(TIMESTAMP_PARAM, Timestamp.now().toString())
            analytics.logEvent(CLEAR_FEED_EVENT, bundle)
            updateUserActionCounter(user.uid, CLEAR_FEED_COUNT)
        }
    }

    fun setContent(feedType: String, userReference: DocumentReference, collection: String,
                   content: Content?, mainFeedEmptied: Boolean) {
        userReference.collection(collection).document(content!!.id).set(content)
                .addOnSuccessListener {
                    Log.v(LOG_TAG, String.format("Content added to collection:%s", it))
                    logCategorizeContentAnalyticsEvent(feedType, content, mainFeedEmptied)
                }.addOnFailureListener {
                    Log.v(LOG_TAG, String.format("Content failed to be added to collection:%s", it))
                }
    }

    fun deleteContent(userReference: DocumentReference, collection: String, content: Content?) {
        userReference
                .collection(collection)
                .document(content!!.id)
                .delete()
                .addOnSuccessListener {
                    Log.v(LOG_TAG, String.format("Content deleted from to collection:%s", it))
                }.addOnFailureListener {
                    Log.v(LOG_TAG, String.format("Content failed to be deleted from collection:%s", it))
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