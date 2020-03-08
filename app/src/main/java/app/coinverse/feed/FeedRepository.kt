package app.coinverse.feed

import androidx.lifecycle.asFlow
import androidx.lifecycle.liveData
import androidx.paging.PagedList
import androidx.paging.toLiveData
import app.coinverse.BuildConfig
import app.coinverse.feed.models.Content
import app.coinverse.feed.models.ContentToPlay
import app.coinverse.feed.models.FeedViewEventType
import app.coinverse.feed.room.FeedDao
import app.coinverse.firebase.*
import app.coinverse.utils.*
import app.coinverse.utils.FeedType.*
import app.coinverse.utils.Resource.Companion.error
import app.coinverse.utils.Resource.Companion.loading
import app.coinverse.utils.Resource.Companion.success
import app.coinverse.utils.Status.ERROR
import app.coinverse.utils.Status.SUCCESS
import app.coinverse.utils.UserActionType.DISMISS
import app.coinverse.utils.UserActionType.SAVE
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth.getInstance
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query.Direction.DESCENDING
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedRepository @Inject constructor(private val feedDao: FeedDao) {
    //class FeedRepository @Inject constructor(private val database: CoinverseDatabase) {
    private val LOG_TAG = FeedRepository::class.java.simpleName

    fun getMainFeedNetwork(isRealtime: Boolean, timeframe: Timestamp) = flow<Resource<Flow<PagedList<Content>>>> {
        emit(loading(null))
        val labeledSet = HashSet<String>()
        if (getInstance().currentUser != null && !getInstance().currentUser!!.isAnonymous) {
            val user = usersDocument.collection(getInstance().currentUser!!.uid)
            syncLabeledContent(user, timeframe, labeledSet, SAVE_COLLECTION, this)
            syncLabeledContent(user, timeframe, labeledSet, DISMISS_COLLECTION, this)
            if (isRealtime) getLoggedInAndRealtimeContent(timeframe, labeledSet, this)
            else getLoggedInNonRealtimeContent(timeframe, labeledSet, this)
        } else getLoggedOutNonRealtimeContent(timeframe, this)
    }

    fun getMainFeedRoom(timestamp: Timestamp) =
            feedDao.getMainFeedRoom(timestamp, MAIN).toLiveData(pagedListConfig).asFlow()


    fun getLabeledFeedRoom(feedType: FeedType) =
            feedDao.getLabeledFeedRoom(feedType).toLiveData(pagedListConfig).asFlow()

    fun getContent(contentId: String) = liveData {
        emit(contentEnCollection.document(contentId).get().await()?.toObject(Content::class.java)!!)
    }

    fun getAudiocast(contentSelected: FeedViewEventType.ContentSelected) = flow {
        emit(loading(null))
        try {
            val content = contentSelected.content
            FirebaseFunctions.getInstance(firebaseApp(true))
                    .getHttpsCallable(GET_AUDIOCAST_FUNCTION).call(
                            hashMapOf(
                                    BUILD_TYPE_PARAM to BuildConfig.BUILD_TYPE,
                                    CONTENT_ID_PARAM to content.id,
                                    CONTENT_TITLE_PARAM to content.title,
                                    CONTENT_PREVIEW_IMAGE_PARAM to content.previewImage))
                    .continueWith { task -> (task.result?.data as HashMap<String, String>) }
                    .await().also { response ->
                        if (response?.get(ERROR_PATH_PARAM).isNullOrEmpty())
                            emit(success((ContentToPlay(
                                    position = contentSelected.position,
                                    content = contentSelected.content,
                                    filePath = response?.get(FILE_PATH_PARAM)))))
                        else emit(error(response?.get(ERROR_PATH_PARAM)!!, null))
                    }
        } catch (error: FirebaseFunctionsException) {
            val errorMessage = if (error is FirebaseFunctionsException)
                "$GET_AUDIOCAST_FUNCTION exception: " +
                        "${error.code.name} details: ${error.details.toString()}"
            else "$GET_AUDIOCAST_FUNCTION exception: ${error?.localizedMessage}"
            emit(error(errorMessage, null))
        }
    }

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
                        position = position).collect { resource ->
                    when (resource.status) {
                        SUCCESS -> addContentLabel(
                                actionType = actionType,
                                userCollection = userReference,
                                content = content,
                                position = position).collect { emit(it) }
                        ERROR -> emit(resource)
                    }
                }
            } else addContentLabel(
                    actionType = actionType,
                    userCollection = userReference,
                    content = content,
                    position = position).collect { emit(it) }
        }
    }

    private suspend fun syncLabeledContent(user: CollectionReference, timeframe: Timestamp,
                                           labeledSet: HashSet<String>, collection: String,
                                           flow: FlowCollector<Resource<Flow<PagedList<Content>>>>) {
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
            feedDao.insertFeed(contentList)
        } else
            flow.emit(error("Error retrieving user save_collection: " + response.error.localizedMessage, null))
    }

    private suspend fun getLoggedInAndRealtimeContent(timeframe: Timestamp,
                                                      labeledSet: HashSet<String>,
                                                      flow: FlowCollector<Resource<Flow<PagedList<Content>>>>) {
        val response = contentEnCollection.orderBy(TIMESTAMP, DESCENDING)
                .whereGreaterThanOrEqualTo(TIMESTAMP, timeframe)
                .awaitRealtime()
        if (response.error == null) {
            val contentList = response.packet?.documentChanges
                    ?.map { change -> change.document.toObject(Content::class.java) }
                    ?.filter { content -> !labeledSet.contains(content.id) }
            feedDao.insertFeed(contentList)
            flow.emit(success(getMainFeedRoom(timeframe)))
        } else flow.emit(error(CONTENT_LOGGED_IN_REALTIME_ERROR + response.error.localizedMessage, null))
    }

    private suspend fun getLoggedInNonRealtimeContent(timeframe: Timestamp,
                                                      labeledSet: HashSet<String>,
                                                      flow: FlowCollector<Resource<Flow<PagedList<Content>>>>) =
            try {
                val contentList = contentEnCollection.orderBy(TIMESTAMP, DESCENDING)
                        .whereGreaterThanOrEqualTo(TIMESTAMP, timeframe).get().await()
                        .documentChanges
                        .map { change -> change.document.toObject(Content::class.java) }
                        .filter { content -> !labeledSet.contains(content.id) }
                feedDao.insertFeed(contentList)
                flow.emit(success(getMainFeedRoom(timeframe)))
            } catch (error: FirebaseFirestoreException) {
                flow.emit(error("CONTENT_LOGGED_IN_NON_REALTIME_ERROR ${error.localizedMessage}", null))
            }

    private suspend fun getLoggedOutNonRealtimeContent(timeframe: Timestamp,
                                                       flow: FlowCollector<Resource<Flow<PagedList<Content>>>>) =
            try {
                val contentList = contentEnCollection.orderBy(TIMESTAMP, DESCENDING)
                        .whereGreaterThanOrEqualTo(TIMESTAMP, timeframe).get().await()
                        .documentChanges
                        .map { change -> change.document.toObject(Content::class.java) }
                feedDao.insertFeed(contentList)
                flow.emit(success(getMainFeedRoom(timeframe)))
            } catch (error: FirebaseFirestoreException) {
                flow.emit(error(CONTENT_LOGGED_OUT_NON_REALTIME_ERROR + error.localizedMessage, null))
            }

    private fun addContentLabel(actionType: UserActionType, userCollection: CollectionReference,
                                content: Content?, position: Int) = flow {
        emit(loading(null))
        val collection =
                if (actionType == SAVE) SAVE_COLLECTION
                else if (actionType == DISMISS) DISMISS_COLLECTION
                else ""
        try {
            userCollection.document(COLLECTIONS_DOCUMENT).collection(collection)
                    .document(content!!.id)
                    .set(content).await()
            feedDao.updateContent(content)
            emit(success(position))
        } catch (error: FirebaseFirestoreException) {
            emit(error("'${content?.title}' failed to be added to collection $collection", null))
        }
    }

    private fun removeContentLabel(userReference: CollectionReference, collection: String,
                                   content: Content?, position: Int) = flow {
        emit(loading(null))
        try {
            userReference.document(COLLECTIONS_DOCUMENT)
                    .collection(collection)
                    .document(content!!.id)
                    .delete().await()
            emit(success(position))
        } catch (error: FirebaseFirestoreException) {
            emit(error("Content failed to be deleted from ${collection}: ${error.localizedMessage}", null))
        }
    }
}