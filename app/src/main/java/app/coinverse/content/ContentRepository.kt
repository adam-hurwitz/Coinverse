package app.coinverse.content

import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.asFlow
import androidx.lifecycle.liveData
import androidx.paging.toLiveData
import app.coinverse.BuildConfig.BUILD_TYPE
import app.coinverse.analytics.models.ContentAction
import app.coinverse.content.models.*
import app.coinverse.content.models.ContentViewEventType.ContentSelected
import app.coinverse.content.room.CoinverseDatabase.database
import app.coinverse.firebase.*
import app.coinverse.utils.*
import app.coinverse.utils.FeedType.*
import app.coinverse.utils.UserActionType.DISMISS
import app.coinverse.utils.UserActionType.SAVE
import app.coinverse.utils.livedata.Event
import app.coinverse.utils.models.Lce
import app.coinverse.utils.models.Lce.Error
import app.coinverse.utils.models.Lce.Loading
import com.google.firebase.Timestamp
import com.google.firebase.Timestamp.now
import com.google.firebase.auth.FirebaseAuth.getInstance
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query.Direction.DESCENDING
import com.google.firebase.firestore.Transaction
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URL

object ContentRepository {
    private val LOG_TAG = ContentRepository::class.java.simpleName

    fun getMainFeedList(isRealtime: Boolean, timeframe: Timestamp) = flow<Lce<PagedListResult>> {
        emit(Loading())
        val labeledSet = HashSet<String>()
        if (getInstance().currentUser != null && !getInstance().currentUser!!.isAnonymous) {
            val user = usersDocument.collection(getInstance().currentUser!!.uid)
            syncLabeledContent(user, timeframe, labeledSet, SAVE_COLLECTION, this)
            syncLabeledContent(user, timeframe, labeledSet, DISMISS_COLLECTION, this)
            if (isRealtime) getLoggedInAndRealtimeContent(timeframe, labeledSet, this)
            else getLoggedInNonRealtimeContent(timeframe, labeledSet, this)
        } else getLoggedOutNonRealtimeContent(timeframe, this)
    }

    fun queryMainContentList(timestamp: Timestamp) =
            database.contentDao().queryMainContentList(timestamp, MAIN).toLiveData(pagedListConfig)


    fun queryLabeledContentList(feedType: FeedType) =
            database.contentDao().queryLabeledContentList(feedType).toLiveData(pagedListConfig).asFlow()

    fun getContent(contentId: String) = liveData {
        emit(Event(contentEnCollection.document(contentId).get().await()?.toObject(Content::class.java)!!))
    }

    fun getAudiocast(contentSelected: ContentSelected) = flow {
        emit(Loading())
        try {
            val content = contentSelected.content
            FirebaseFunctions.getInstance(firebaseApp(true))
                    .getHttpsCallable(GET_AUDIOCAST_FUNCTION).call(
                            hashMapOf(
                                    BUILD_TYPE_PARAM to BUILD_TYPE,
                                    CONTENT_ID_PARAM to content.id,
                                    CONTENT_TITLE_PARAM to content.title,
                                    CONTENT_PREVIEW_IMAGE_PARAM to content.previewImage))
                    .continueWith { task -> (task.result?.data as HashMap<String, String>) }
                    .await().also { response ->
                        if (response?.get(ERROR_PATH_PARAM).isNullOrEmpty())
                            emit(Lce.Content(ContentToPlay(
                                    position = contentSelected.position,
                                    content = contentSelected.content,
                                    filePath = response?.get(FILE_PATH_PARAM),
                                    errorMessage = "")))
                        else emit(Error(ContentToPlay(
                                position = contentSelected.position,
                                content = contentSelected.content,
                                filePath = "",
                                errorMessage = response?.get(ERROR_PATH_PARAM)!!)))
                    }
        } catch (error: FirebaseFunctionsException) {
            emit(Error(ContentToPlay(
                    position = contentSelected.position,
                    content = contentSelected.content,
                    filePath = "",
                    errorMessage = if (error is FirebaseFunctionsException)
                        "$GET_AUDIOCAST_FUNCTION exception: " +
                                "${error.code.name} details: ${error.details.toString()}"
                    else "$GET_AUDIOCAST_FUNCTION exception: ${error?.localizedMessage}"
            )))
        }
    }

    fun getContentUri(contentId: String, filePath: String) = flow {
        emit(Loading())
        try {
            val uri = FirebaseStorage.getInstance(firebaseApp(true))
                    .reference.child(filePath).downloadUrl.await()
            contentEnCollection.document(contentId) // Update content Audio Uri.
                    .update(AUDIO_URL, Regex(AUDIO_URL_TOKEN_REGEX).replace(uri.toString(), "")).await()
            emit(Lce.Content(ContentPlayer(
                    uri = uri,
                    image = ByteArray(0),
                    errorMessage = "")))
        } catch (error: StorageException) {
            emit(Error(ContentPlayer(
                    Uri.parse(""),
                    ByteArray(0), "getContentUri error - ${error.localizedMessage}")))
        }
    }

    fun bitmapToByteArray(url: String) = flow {
        emit(Loading())
        emit(Lce.Content(ContentBitmap(ByteArrayOutputStream().apply {
            try {
                BitmapFactory.decodeStream(URL(url).openConnection().apply {
                    doInput = true
                    connect()
                }.getInputStream())
            } catch (e: IOException) {
                emit(Error(ContentBitmap(ByteArray(0),
                        "bitmapToByteArray error or null - ${e.localizedMessage}")))
                null
            }?.compress(CompressFormat.JPEG, BITMAP_COMPRESSION_QUALITY, this)
        }.toByteArray(), "")))
    }.flowOn(Dispatchers.IO)

    fun editContentLabels(feedType: FeedType, actionType: UserActionType, content: Content?,
                          user: FirebaseUser, position: Int) = flow {
        val userReference = usersDocument.collection(user.uid)
        content?.feedType =
                if (actionType == SAVE) SAVED
                else if (actionType == DISMISS) DISMISSED
                else MAIN
        if (actionType == SAVE || actionType == DISMISS) {
            if (feedType == SAVED || feedType == DISMISSED) {
                removeContentLabel(
                        userReference = userReference,
                        collection = if (actionType == SAVE && feedType == DISMISSED)
                            DISMISS_COLLECTION else if (actionType == DISMISS && feedType == SAVED)
                            SAVE_COLLECTION else "",
                        content = content,
                        position = position).collect { contentLabeled ->
                    when (contentLabeled) {
                        is Lce.Content -> addContentLabel(
                                actionType = actionType,
                                userCollection = userReference,
                                content = content,
                                position = position).collect { emit(it) }
                        is Error -> emit(contentLabeled)
                    }
                }
            } else addContentLabel(
                    actionType = actionType,
                    userCollection = userReference,
                    content = content,
                    position = position).collect { emit(it) }
        }
    }

    //TODO - Move to Cloud Function
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

    //TODO - Move to Cloud Function
    fun updateUserActions(userId: String, actionCollection: String, content: Content, countType: String) {
        usersDocument.collection(userId).document(ACTIONS_DOCUMENT).collection(actionCollection)
                .document(content.id).set(ContentAction(now(), content.id, content.title, content.creator,
                        content.qualityScore)).addOnSuccessListener {
                    updateUserActionCounter(userId, countType)
                }.addOnFailureListener {
                    Log.e(LOG_TAG, "User content action update FAIL.")
                }
    }

    //TODO - Move to Cloud Function
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

    //TODO - Move to Cloud Function
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

    private suspend fun syncLabeledContent(user: CollectionReference, timeframe: Timestamp,
                                           labeledSet: HashSet<String>, collection: String,
                                           lce: FlowCollector<Lce<PagedListResult>>) {
        val response = user.document(COLLECTIONS_DOCUMENT)
                .collection(collection)
                .orderBy(TIMESTAMP, DESCENDING)
                .whereGreaterThanOrEqualTo(TIMESTAMP, timeframe)
                .awaitRealtime()
        if (response.error == null) {
            val contentList = response.packet?.documentChanges?.map { doc ->
                doc.document.toObject(Content::class.java).also { content ->
                    labeledSet.add(content.id)
                }
            }
            database.contentDao().insertContentList(contentList)
        } else lce.emit(Error(PagedListResult(null,
                "Error retrieving user save_collection: ${response.error?.localizedMessage}")))
    }

    private suspend fun getLoggedInAndRealtimeContent(timeframe: Timestamp,
                                                      labeledSet: HashSet<String>,
                                                      lce: FlowCollector<Lce<PagedListResult>>) {

        val response = contentEnCollection.orderBy(TIMESTAMP, DESCENDING)
                .whereGreaterThanOrEqualTo(TIMESTAMP, timeframe)
                .awaitRealtime()
        if (response.error == null) {
            val contentList = response.packet?.documentChanges
                    ?.map { change -> change.document.toObject(Content::class.java) }
                    ?.filter { content -> !labeledSet.contains(content.id) }
            database.contentDao().insertContentList(contentList)
            lce.emit(Lce.Content(PagedListResult(queryMainContentList(timeframe), "")))
        } else lce.emit(Error(PagedListResult(null,
                CONTENT_LOGGED_IN_REALTIME_ERROR + "${response.error.localizedMessage}")))
    }

    private suspend fun getLoggedInNonRealtimeContent(timeframe: Timestamp,
                                                      labeledSet: HashSet<String>,
                                                      lce: FlowCollector<Lce<PagedListResult>>) =
            try {
                database.contentDao().insertContentList(
                        contentEnCollection.orderBy(TIMESTAMP, DESCENDING)
                                .whereGreaterThanOrEqualTo(TIMESTAMP, timeframe).get().await()
                                .documentChanges
                                ?.map { change -> change.document.toObject(Content::class.java) }
                                ?.filter { content -> !labeledSet.contains(content.id) })
                lce.emit(Lce.Content(PagedListResult(queryMainContentList(timeframe), "")))
            } catch (error: FirebaseFirestoreException) {
                lce.emit(Error(PagedListResult(
                        null,
                        CONTENT_LOGGED_IN_NON_REALTIME_ERROR + "${error.localizedMessage}")))
            }

    private suspend fun getLoggedOutNonRealtimeContent(timeframe: Timestamp,
                                                       lce: FlowCollector<Lce<PagedListResult>>) =
            try {
                database.contentDao().insertContentList(
                        contentEnCollection.orderBy(TIMESTAMP, DESCENDING)
                                .whereGreaterThanOrEqualTo(TIMESTAMP, timeframe).get().await()
                                .documentChanges
                                ?.map { change -> change.document.toObject(Content::class.java) })
                lce.emit(Lce.Content(PagedListResult(queryMainContentList(timeframe), "")))
            } catch (error: FirebaseFirestoreException) {
                lce.emit(Error(PagedListResult(
                        null,
                        CONTENT_LOGGED_OUT_NON_REALTIME_ERROR + "${error.localizedMessage}")))
            }

    private fun addContentLabel(actionType: UserActionType, userCollection: CollectionReference,
                                content: Content?, position: Int) = flow {
        emit(Loading())
        val collection =
                if (actionType == SAVE) SAVE_COLLECTION
                else if (actionType == DISMISS) DISMISS_COLLECTION
                else ""
        try {
            userCollection.document(COLLECTIONS_DOCUMENT).collection(collection)
                    .document(content!!.id)
                    .set(content).await()
            database.contentDao().updateContent(content)
            emit(Lce.Content(ContentLabeled(position, "")))
        } catch (error: FirebaseFirestoreException) {
            emit(Error(ContentLabeled(
                    position,
                    "'${content?.title}' failed to be added to collection $collection")))
        }
    }

    private fun removeContentLabel(userReference: CollectionReference, collection: String,
                                   content: Content?, position: Int) = flow {
        emit(Loading())
        try {
            userReference.document(COLLECTIONS_DOCUMENT)
                    .collection(collection)
                    .document(content!!.id)
                    .delete().await()
            emit(Lce.Content(ContentLabeled(position, "")))
        } catch (error: FirebaseFirestoreException) {
            emit(Error(ContentLabeled(
                    position,
                    "Content failed to be deleted from ${collection}: ${error.localizedMessage}")))
        }
    }
}