package app.coinverse.content

import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.paging.DataSource
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import app.coinverse.BuildConfig.BUILD_TYPE
import app.coinverse.analytics.models.ContentAction
import app.coinverse.content.models.*
import app.coinverse.content.models.ContentViewEvent.ContentSelected
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
import com.google.firebase.firestore.*
import com.google.firebase.firestore.Query.Direction.DESCENDING
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URL

object ContentRepository {
    private val LOG_TAG = ContentRepository::class.java.simpleName
    //TODO - Create Cloud Function to update user's mainFeedCollection.
    fun getMainFeedList(isRealtime: Boolean, timeframe: Timestamp) =
            MutableLiveData<Lce<PagedListResult>>().also { lce ->
                lce.value = Loading()
                val labeledSet = HashSet<String>()
                var errorMessage = ""
                //TODO - Retrieve labeledSet from Firestore.
                if (getInstance().currentUser != null && !getInstance().currentUser!!.isAnonymous) {
                    usersDocument.collection(getInstance().currentUser!!.uid).also { user ->
                        // Get save_collection.
                        user.document(COLLECTIONS_DOCUMENT)
                                .collection(SAVE_COLLECTION).orderBy(TIMESTAMP, DESCENDING)
                                .whereGreaterThanOrEqualTo(TIMESTAMP, timeframe)
                                .addSnapshotListener(EventListener { value, error ->
                                    error?.run {
                                        errorMessage = "Error retrieving user save_collection: " +
                                                "${error.localizedMessage}"
                                        return@EventListener
                                    }
                                    value!!.documentChanges.all { document ->
                                        document.document.toObject(Content::class.java).let { savedContent ->
                                            labeledSet.add(savedContent.id)
                                            Thread(Runnable {
                                                run { database.contentDao().updateContent(savedContent) }
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
                                                run { database.contentDao().updateContent(dismissedContent) }
                                            }).start()
                                        }
                                        true
                                    }
                                })
                        if (errorMessage.isNotEmpty())
                            lce.value = Error(PagedListResult(null, errorMessage))
                    }
                    // Logged in and realtime enabled.
                    if (isRealtime) //TODO - Retrieve labeledSet from Firestore.
                        contentEnCollection.orderBy(TIMESTAMP, DESCENDING)
                                .whereGreaterThanOrEqualTo(TIMESTAMP, timeframe)
                                .addSnapshotListener(EventListener { value, error ->
                                    error?.run {
                                        lce.value = Error(PagedListResult(
                                                null,
                                                "Error retrieving logged in," +
                                                        " realtime content_en_collection: " +
                                                        "${error.localizedMessage}"))
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
                                        lce.value = Lce.Content(PagedListResult(
                                                queryMainContentList(timeframe),
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
                                lce.value = Lce.Content(PagedListResult(
                                        queryMainContentList(timeframe),
                                        ""))
                            }.addOnFailureListener {
                                lce.value = Error(PagedListResult(
                                        null, "Error retrieving logged in, " +
                                        "non-realtime content_en_collection: ${it.localizedMessage}"))
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
                            lce.value = Lce.Content(PagedListResult(
                                    queryMainContentList(timeframe),
                                    ""))
                        }.addOnFailureListener {
                            lce.value = Error(PagedListResult(
                                    null, "Error retrieving logged out, " +
                                    "non-realtime content_en_collection: " + "${it.localizedMessage}"))
                        }
            }

    fun getContent(contentId: String) =
            MutableLiveData<Event<Content>>().apply {
                contentEnCollection.document(contentId).get().addOnCompleteListener { result ->
                    value = Event((result.result?.toObject(Content::class.java)!!))
                }
            }

    fun queryMainContentList(timestamp: Timestamp) =
            liveDataBuilder(database.contentDao().queryMainContentList(timestamp, MAIN))

    fun queryLabeledContentList(feedType: FeedType) =
            liveDataBuilder(database.contentDao().queryLabeledContentList(feedType))

    fun liveDataBuilder(dataSource: DataSource.Factory<Int, Content>) =
            LivePagedListBuilder(dataSource,
                    PagedList.Config.Builder().setEnablePlaceholders(true)
                            .setPrefetchDistance(PREFETCH_DISTANCE)
                            .setPageSize(PAGE_SIZE)
                            .build())
                    .build()

    fun getAudiocast(contentSelected: ContentSelected) =
            MutableLiveData<Lce<ContentToPlay>>().apply {
                value = Loading()
                val content = contentSelected.content
                FirebaseFunctions.getInstance(firebaseApp(true)).getHttpsCallable(GET_AUDIOCAST_FUNCTION).call(
                        hashMapOf(
                                BUILD_TYPE_PARAM to BUILD_TYPE,
                                CONTENT_ID_PARAM to content.id,
                                CONTENT_TITLE_PARAM to content.title,
                                CONTENT_PREVIEW_IMAGE_PARAM to content.previewImage))
                        .continueWith { task -> (task.result?.data as HashMap<String, String>) }
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful)
                                task.result.also { response ->
                                    if (response?.get(ERROR_PATH_PARAM).isNullOrEmpty())
                                        value = Lce.Content(
                                                ContentToPlay(
                                                        position = contentSelected.position,
                                                        content = contentSelected.content,
                                                        filePath = response?.get(FILE_PATH_PARAM),
                                                        errorMessage = ""))
                                    else value = Error(ContentToPlay(
                                            position = contentSelected.position,
                                            content = contentSelected.content,
                                            filePath = "",
                                            errorMessage = response?.get(ERROR_PATH_PARAM)!!))
                                } else {
                                val e = task.exception
                                value = Error(ContentToPlay(
                                        position = contentSelected.position,
                                        content = contentSelected.content,
                                        filePath = "",
                                        errorMessage = if (e is FirebaseFunctionsException)
                                            "$GET_AUDIOCAST_FUNCTION exception: " +
                                                    "${e.code.name} details: ${e.details.toString()}"
                                        else "$GET_AUDIOCAST_FUNCTION exception: ${e?.localizedMessage}"
                                ))
                            }
                        }
            }

    fun getContentUri(contentId: String, filePath: String) =
            MutableLiveData<Lce<ContentPlayer>>().apply {
                value = Loading()
                FirebaseStorage.getInstance(firebaseApp(true)).reference.child(filePath).downloadUrl
                        .addOnSuccessListener { uri ->
                            contentEnCollection.document(contentId) // Update content Audio Uri.
                                    .update(AUDIO_URL, Regex(AUDIO_URL_TOKEN_REGEX).replace(
                                            uri.toString(), ""))
                                    .addOnSuccessListener {
                                        value = Lce.Content(ContentPlayer(
                                                uri = uri,
                                                image = ByteArray(0),
                                                errorMessage = ""))
                                    }.addOnFailureListener {
                                        value = Error(ContentPlayer(
                                                uri = Uri.EMPTY,
                                                image = ByteArray(0),
                                                errorMessage = it.localizedMessage))
                                    }
                        }.addOnFailureListener {
                            value = Error(ContentPlayer(
                                    Uri.parse(""),
                                    ByteArray(0),
                                    "getContentUri error - ${it.localizedMessage}"))
                        }
            }

    suspend fun bitmapToByteArray(url: String) = withContext(Dispatchers.IO) {
        MutableLiveData<Lce<ContentBitmap>>().apply {
            postValue(Loading())
            postValue(Lce.Content(ContentBitmap(ByteArrayOutputStream().apply {
                try {
                    BitmapFactory.decodeStream(URL(url).openConnection().apply {
                        doInput = true
                        connect()
                    }.getInputStream())
                } catch (e: IOException) {
                    postValue(Error(ContentBitmap(ByteArray(0),
                            "bitmapToByteArray error or null - ${e.localizedMessage}")))
                    null
                }?.compress(CompressFormat.JPEG, BITMAP_COMPRESSION_QUALITY, this)
            }.toByteArray(), "")))
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
                    if (feedType == SAVED || feedType == DISMISSED) {
                        Transformations.switchMap(removeContentLabel(
                                userReference,
                                if (actionType == SAVE && feedType == DISMISSED) DISMISS_COLLECTION
                                else if (actionType == DISMISS && feedType == SAVED) SAVE_COLLECTION
                                else "",
                                content,
                                position)) { lce ->
                            when (lce) {
                                is Loading -> MutableLiveData()
                                is Lce.Content -> addContentLabelSwitchMap(actionType, userReference,
                                        content!!, position)
                                is Error -> MutableLiveData<Lce<ContentLabeled>>().apply {
                                    value = lce
                                }
                            }
                        }
                    } else addContentLabelSwitchMap(actionType, userReference, content!!, position)
                } else MutableLiveData()
            }

    fun addContentLabelSwitchMap(actionType: UserActionType, userReference: CollectionReference,
                                 content: Content, position: Int) =
            Transformations.switchMap(addContentLabel(actionType, userReference,
                    content, position)) { lce ->
                MutableLiveData<Lce<ContentLabeled>>().apply {
                    value = lce
                }
            }

    fun addContentLabel(actionType: UserActionType, userCollection: CollectionReference,
                        content: Content?, position: Int) =
            MutableLiveData<Lce<ContentLabeled>>().apply {
                value = Loading()
                val collection =
                        if (actionType == SAVE) SAVE_COLLECTION
                        else if (actionType == DISMISS) DISMISS_COLLECTION
                        else ""
                userCollection.document(COLLECTIONS_DOCUMENT).collection(collection).document(content!!.id)
                        .set(content).addOnSuccessListener {
                            Thread(Runnable { run { database.contentDao().updateContent(content) } }).start()
                            value = Lce.Content(ContentLabeled(position, ""))
                        }.addOnFailureListener {
                            value = Error(ContentLabeled(
                                    position,
                                    "'${content.title}' failed to be added to collection $collection"))
                        }
            }

    fun removeContentLabel(userReference: CollectionReference, collection: String, content: Content?,
                           position: Int) =
            MutableLiveData<Lce<ContentLabeled>>().apply {
                value = Loading()
                userReference.document(COLLECTIONS_DOCUMENT).collection(collection).document(content!!.id)
                        .delete().addOnSuccessListener {
                            value = Lce.Content(ContentLabeled(position, ""))
                        }.addOnFailureListener {
                            value = Error(ContentLabeled(
                                    position,
                                    "Content failed to be deleted from ${collection}: ${it.localizedMessage}"))
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
}