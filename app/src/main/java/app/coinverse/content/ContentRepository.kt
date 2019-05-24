package app.coinverse.content

import android.app.Application
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.paging.DataSource
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import app.coinverse.BuildConfig.DEBUG
import app.coinverse.analytics.models.ContentAction
import app.coinverse.analytics.models.UserAction
import app.coinverse.content.models.Content
import app.coinverse.content.models.ContentResult
import app.coinverse.content.models.ContentViewEvent
import app.coinverse.content.room.CoinverseDatabase
import app.coinverse.firebase.*
import app.coinverse.utils.*
import app.coinverse.utils.Enums.FeedType
import app.coinverse.utils.Enums.FeedType.*
import app.coinverse.utils.Enums.UserActionType
import app.coinverse.utils.Enums.UserActionType.*
import app.coinverse.utils.livedata.Event
import app.coinverse.utils.models.Lce
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
import com.google.firebase.functions.FirebaseFunctionsException

object ContentRepository {
    private val LOG_TAG = ContentRepository::class.java.simpleName

    private lateinit var analytics: FirebaseAnalytics
    private lateinit var database: CoinverseDatabase

    operator fun invoke(application: Application) {
        analytics = getInstance(application)
        database = CoinverseDatabase.getAppDatabase(application)
    }

    //TODO: Cloud Function to auto save labeledSet to Firestore.
    fun getMainFeedList(isRealtime: Boolean, timeframe: Timestamp) =
            MutableLiveData<Lce<ContentResult.PagedListResult>>().also { lce ->
                lce.value = Lce.Loading()
                val labeledSet = HashSet<String>()
                var errorMessage = ""
                //TODO: Retrieve labeledSet from Firestore.
                if (getInstance().currentUser != null) {
                    usersDocument.collection(getInstance().currentUser!!.uid).also { user ->
                        // Get save_collection.
                        user.document(COLLECTIONS_DOCUMENT)
                                .collection(SAVE_COLLECTION).orderBy(TIMESTAMP, DESCENDING)
                                .whereGreaterThanOrEqualTo(TIMESTAMP, timeframe)
                                .addSnapshotListener(EventListener { value, error ->
                                    error?.run {
                                        errorMessage = "Error retrieving user save_collection: ${error.localizedMessage}"
                                        return@EventListener
                                    }
                                    value!!.documentChanges.all { document ->
                                        document.document.toObject(Content::class.java).let { savedContent ->
                                            labeledSet.add(savedContent.id)
                                            Thread(Runnable {
                                                run { database.contentDao().updateContentItem(savedContent) }
                                            }).start()
                                        }
                                        true
                                    }
                                })
                        // Get dismiss_collection.
                        user.document(COLLECTIONS_DOCUMENT)
                                .collection(DISMISS_COLLECTION)
                                .orderBy(TIMESTAMP, DESCENDING)
                                .whereGreaterThanOrEqualTo(TIMESTAMP, timeframe)
                                .addSnapshotListener(EventListener { value, error ->
                                    error?.run {
                                        errorMessage = "Error retrieving user dismiss_collection: ${error.localizedMessage}"
                                        return@EventListener
                                    }
                                    value!!.documentChanges.all { document ->
                                        document.document.toObject(Content::class.java).let { dismissedContent ->
                                            labeledSet.add(dismissedContent.id)
                                            Thread(Runnable {
                                                run { database.contentDao().updateContentItem(dismissedContent) }
                                            }).start()
                                        }
                                        true
                                    }
                                })
                        if (errorMessage.isNotEmpty())
                            lce.value = Lce.Error(
                                    ContentResult.PagedListResult(null, errorMessage))
                    }
                    // Logged in and realtime enabled.
                    if (isRealtime) //TODO: Retrieve labeledSet from Firestore.
                        contentEnCollection.orderBy(TIMESTAMP, DESCENDING)
                                .whereGreaterThanOrEqualTo(TIMESTAMP, timeframe)
                                .addSnapshotListener(EventListener { value, error ->
                                    error?.run {
                                        lce.value = Lce.Error(
                                                ContentResult.PagedListResult(
                                                        null,
                                                        "Error retrieving logged in, realtime content_en_collection: ${error.localizedMessage}"))
                                        return@EventListener
                                    }
                                    arrayListOf<Content?>().also { contentList ->
                                        value!!.documentChanges.all { document ->
                                            document.document.toObject(Content::class.java).also { content ->
                                                if (!labeledSet.contains(content.id))
                                                    contentList.add(content)
                                            }
                                            true
                                        }
                                        Thread(Runnable {
                                            run { database.contentDao().insertContentList(contentList) }
                                        }).start()
                                        lce.value = Lce.Content(ContentResult.PagedListResult(
                                                getRoomMainList(timeframe),
                                                ""))
                                    }
                                })
                    // Logged in, non-realtime.
                    else contentEnCollection.orderBy(TIMESTAMP, DESCENDING)
                            .whereGreaterThanOrEqualTo(TIMESTAMP, timeframe)
                            .get().addOnCompleteListener {
                                arrayListOf<Content?>().also { contentList ->
                                    it.result!!.documentChanges.all { document ->
                                        document.document.toObject(Content::class.java).also { content ->
                                            if (!labeledSet.contains(content.id))
                                                contentList.add(content)
                                        }
                                        true
                                    }
                                    Thread(Runnable {
                                        run { database.contentDao().insertContentList(contentList) }
                                    }).start()
                                }
                                lce.value = Lce.Content(ContentResult.PagedListResult(
                                        getRoomMainList(timeframe),
                                        ""))
                            }.addOnFailureListener {
                                lce.value = Lce.Error(ContentResult.PagedListResult(
                                        null,
                                        "Error retrieving logged in, non-realtime content_en_collection: ${it.localizedMessage}"))
                            }
                    // Logged out, non-realtime.
                } else contentEnCollection.orderBy(TIMESTAMP, DESCENDING)
                        .whereGreaterThanOrEqualTo(TIMESTAMP, timeframe)
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
                            lce.value = Lce.Content(ContentResult.PagedListResult(
                                    getRoomMainList(timeframe),
                                    ""))
                        }.addOnFailureListener {
                            lce.value = Lce.Error(ContentResult.PagedListResult(
                                    null,
                                    "Error retrieving logged out, non-realtime content_en_collection: ${it.localizedMessage}"))
                        }
            }

    fun getContent(contentId: String) = MutableLiveData<Event<Content>>().apply {
        contentEnCollection.document(contentId).get().addOnCompleteListener { result ->
            this.value = Event((result.result?.toObject(Content::class.java)!!))
        }
    }

    fun getRoomMainList(timestamp: Timestamp) =
            liveDataBuilder(database.contentDao().getMainContentList(timestamp, MAIN))

    fun getRoomCategoryList(feedType: FeedType) =
            liveDataBuilder(database.contentDao().getCategorizedContentList(feedType))

    fun liveDataBuilder(dataSource: DataSource.Factory<Int, Content>) =
            LivePagedListBuilder(dataSource,
                    PagedList.Config.Builder().setEnablePlaceholders(true)
                            .setPrefetchDistance(PREFETCH_DISTANCE)
                            .setPageSize(PAGE_SIZE)
                            .build())
                    .build()

    fun getAudiocast(contentSelected: ContentViewEvent.ContentSelected) =
            MutableLiveData<Lce<ContentResult.ContentToPlay>>().apply {
                this.value = Lce.Loading()
                val content = contentSelected.content
                FirebaseFunctions.getInstance().getHttpsCallable(GET_AUDIOCAST_FUNCTION).call(
                        hashMapOf(
                                DEBUG_ENABLED_PARAM to DEBUG,
                                CONTENT_ID_PARAM to content.id,
                                CONTENT_TITLE_PARAM to content.title,
                                CONTENT_PREVIEW_IMAGE_PARAM to content.previewImage))
                        .continueWith { task -> (task.result?.data as HashMap<String, String>) }
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful)
                                task.result.also { response ->
                                    if (response?.get(ERROR_PATH_PARAM).isNullOrEmpty())
                                        this.value = Lce.Content(
                                                ContentResult.ContentToPlay(
                                                        contentSelected.position,
                                                        contentSelected.content,
                                                        response?.get(FILE_PATH_PARAM),
                                                        ""))
                                    else this.value =
                                            Lce.Error(ContentResult.ContentToPlay(
                                                    contentSelected.position,
                                                    contentSelected.content,
                                                    "",
                                                    response?.get(ERROR_PATH_PARAM)!!))
                                } else {
                                val e = task.exception
                                this.value = Lce.Error(ContentResult.ContentToPlay(
                                        contentSelected.position,
                                        contentSelected.content,
                                        "",
                                        if (e is FirebaseFunctionsException)
                                            "$GET_AUDIOCAST_FUNCTION exception: " +
                                                    "${e.code.name} details: ${e.details.toString()}"
                                        else "$GET_AUDIOCAST_FUNCTION exception: ${e?.localizedMessage}"
                                ))
                            }
                        }
            }

    fun editContentLabels(feedType: FeedType, actionType: UserActionType, content: Content?,
                          user: FirebaseUser, position: Int) =
            usersDocument.collection(user.uid).let { userReference ->
                content?.feedType =
                        if (actionType == SAVE) SAVED
                        else if (actionType == DISMISS) DISMISSED
                        else MAIN
                if (actionType == SAVE || actionType == DISMISS) {
                    if (feedType == SAVED || feedType == DISMISSED)
                        Transformations.switchMap(removeContentLabel(
                                userReference,
                                if (actionType == SAVE && feedType == DISMISSED) DISMISS_COLLECTION
                                else if (actionType == DISMISS && feedType == SAVED) SAVE_COLLECTION
                                else "",
                                content,
                                position)) { lce ->
                            when (lce) {
                                is Lce.Loading -> MutableLiveData()
                                is Lce.Content -> addContentLabelSwitchMap(actionType, userReference,
                                        content!!, position)
                                is Lce.Error -> MutableLiveData<Lce<ContentResult.ContentLabeled>>().apply {
                                    this.value = lce
                                }
                            }
                        }
                    else addContentLabelSwitchMap(actionType, userReference, content!!, position)
                } else MutableLiveData()
            }

    fun addContentLabelSwitchMap(actionType: UserActionType, userReference: CollectionReference,
                                 content: Content, position: Int) =
            Transformations.switchMap(addContentLabel(actionType, userReference,
                    content, position)) { lce ->
                MutableLiveData<Lce<ContentResult.ContentLabeled>>().apply {
                    this.value = lce
                }
            }

    fun addContentLabel(actionType: UserActionType, userCollection: CollectionReference,
                        content: Content?, position: Int) =
            MutableLiveData<Lce<ContentResult.ContentLabeled>>().apply {
                this.value = Lce.Loading()
                val collection =
                        if (actionType == SAVE) SAVE_COLLECTION
                        else if (actionType == DISMISS) DISMISS_COLLECTION
                        else ""
                userCollection.document(COLLECTIONS_DOCUMENT).collection(collection).document(content!!.id)
                        .set(content).addOnSuccessListener {
                            Thread(Runnable { run { database.contentDao().updateContentItem(content) } }).start()
                            this.value = Lce.Content(ContentResult.ContentLabeled(position, ""))
                        }.addOnFailureListener {
                            this.value = Lce.Error(ContentResult.ContentLabeled(
                                    position,
                                    "'${content.title}' failed to be added to collection $collection"))
                        }
            }

    fun removeContentLabel(userReference: CollectionReference, collection: String, content: Content?,
                           position: Int) =
            MutableLiveData<Lce<ContentResult.ContentLabeled>>().apply {
                this.value = Lce.Loading()
                userReference.document(COLLECTIONS_DOCUMENT).collection(collection).document(content!!.id)
                        .delete().addOnSuccessListener {
                            this.value = Lce.Content(ContentResult.ContentLabeled(position, ""))
                        }.addOnFailureListener {
                            this.value = Lce.Error(ContentResult.ContentLabeled(
                                    position,
                                    "Content failed to be deleted from ${collection}: ${it.localizedMessage}"))
                        }
            }

    fun updateContentAudioUrl(contentId: String, url: Uri) =
            contentEnCollection.document(contentId)
                    .update(AUDIO_URL, Regex(AUDIO_URL_TOKEN_REGEX).replace(url.toString(), ""))

    //TODO: Move to Cloud Function.
    fun updateActionAnalytics(actionType: UserActionType, content: Content, user: FirebaseUser) {
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

    //TODO: Move to Cloud Function.
    fun updateContentActionCounter(contentId: String, counterType: String) {
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

    //TODO: Move to Cloud Function.
    fun updateUserActions(userId: String, actionCollection: String, content: Content, countType: String) {
        usersDocument.collection(userId).document(ACTIONS_DOCUMENT).collection(actionCollection)
                .document(content.id).set(ContentAction(now(), content.id, content.title, content.creator,
                        content.qualityScore)).addOnSuccessListener {
                    updateUserActionCounter(userId, countType)
                }.addOnFailureListener {
                    Log.e(LOG_TAG, "User content action update FAIL.")
                }
    }

    //TODO: Move to Cloud Function.
    fun updateUserActionCounter(userId: String, counterType: String) {
        usersDocument.collection(userId).document(ACTIONS_DOCUMENT).apply {
            FirebaseFirestore.getInstance().runTransaction(Transaction.Function<String> { counterTransaction ->
                counterTransaction.update(this, counterType,
                        counterTransaction.get(this).getDouble(counterType)!! + 1.0)
                return@Function "User counter update SUCCESS."
            }).addOnSuccessListener { status -> Log.v(LOG_TAG, status) }
                    .addOnFailureListener { e -> Log.e(LOG_TAG, "user counter update FAIL.", e) }
        }
    }

    //TODO: Move to Cloud Function.
    fun updateQualityScore(score: Double, contentId: String) {
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
            }).addOnSuccessListener({ Log.d(LOG_TAG, "Transaction success!") })
                    .addOnFailureListener({ e ->
                        Log.e(LOG_TAG, "Transaction failure updateQualityScore.", e)
                    })
        }
    }

    fun labelContentFirebaseAnalytics(content: Content) {
        Bundle().apply {
            this.putString(ITEM_ID, content.id)
            this.putString(USER_ID_PARAM, getInstance().currentUser?.uid)
            this.putString(CREATOR_PARAM, content.creator)
            analytics.logEvent(
                    if (content.feedType == SAVED) ORGANIZE_EVENT else DISMISS_EVENT, this)
        }
    }
}