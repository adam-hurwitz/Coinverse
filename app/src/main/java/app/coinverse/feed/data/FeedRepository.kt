package app.coinverse.feed.data

import androidx.lifecycle.asFlow
import androidx.paging.PagedList
import androidx.paging.toLiveData
import app.coinverse.BuildConfig
import app.coinverse.feed.Content
import app.coinverse.feed.state.FeedViewIntentType.SelectContent
import app.coinverse.feed.state.FeedViewState.OpenContent
import app.coinverse.firebase.COLLECTIONS_DOCUMENT
import app.coinverse.firebase.DISMISS_COLLECTION
import app.coinverse.firebase.SAVE_COLLECTION
import app.coinverse.firebase.contentEnCollection
import app.coinverse.firebase.firebaseApp
import app.coinverse.firebase.usersDocument
import app.coinverse.utils.BUILD_TYPE_PARAM
import app.coinverse.utils.CONTENT_ID_PARAM
import app.coinverse.utils.CONTENT_LOGGED_IN_REALTIME_ERROR
import app.coinverse.utils.CONTENT_LOGGED_OUT_NON_REALTIME_ERROR
import app.coinverse.utils.CONTENT_PREVIEW_IMAGE_PARAM
import app.coinverse.utils.CONTENT_TITLE_PARAM
import app.coinverse.utils.ERROR_PATH_PARAM
import app.coinverse.utils.FILE_PATH_PARAM
import app.coinverse.utils.FeedType
import app.coinverse.utils.FeedType.DISMISSED
import app.coinverse.utils.FeedType.MAIN
import app.coinverse.utils.FeedType.SAVED
import app.coinverse.utils.GET_AUDIOCAST_FUNCTION
import app.coinverse.utils.Resource
import app.coinverse.utils.Resource.Companion.error
import app.coinverse.utils.Resource.Companion.loading
import app.coinverse.utils.Resource.Companion.success
import app.coinverse.utils.Status.ERROR
import app.coinverse.utils.Status.SUCCESS
import app.coinverse.utils.TIMESTAMP
import app.coinverse.utils.UserActionType
import app.coinverse.utils.UserActionType.DISMISS
import app.coinverse.utils.UserActionType.SAVE
import app.coinverse.utils.awaitRealtime
import app.coinverse.utils.pagedListConfig
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth.getInstance
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query.Direction.DESCENDING
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@ExperimentalCoroutinesApi
@Singleton
class FeedRepository @Inject constructor(private val dao: FeedDao) {
    private val LOG_TAG = FeedRepository::class.java.simpleName

    fun getMainFeedNetwork(isRealtime: Boolean, timeframe: Timestamp) = flow<Resource<Flow<PagedList<Content>>>> {
        emit(loading(null))
        val labelsSet = HashSet<String>()
        if (getInstance().currentUser != null && !getInstance().currentUser!!.isAnonymous) {
            val user = usersDocument.collection(getInstance().currentUser!!.uid)
            syncLabeledContent(user, timeframe, labelsSet, SAVE_COLLECTION, this)
            syncLabeledContent(user, timeframe, labelsSet, DISMISS_COLLECTION, this)
            if (isRealtime) getLoggedInAndRealtimeContent(timeframe, labelsSet, this)
            else getLoggedInNonRealtimeContent(timeframe, labelsSet, this)
        } else getLoggedOutNonRealtimeContent(timeframe, this)
    }

    fun getMainFeedRoom(timestamp: Timestamp) =
            dao.getMainFeedRoom(timestamp, MAIN).toLiveData(pagedListConfig).asFlow()

    fun getLabeledFeedRoom(feedType: FeedType) =
            dao.getLabeledFeedRoom(feedType).toLiveData(pagedListConfig).asFlow()

    fun getAudiocast(selectContent: SelectContent) = flow {
        emit(loading(null))
        try {
            val content = selectContent.content
            FirebaseFunctions.getInstance(firebaseApp(true))
                    .getHttpsCallable(GET_AUDIOCAST_FUNCTION).call(
                            hashMapOf(
                                    BUILD_TYPE_PARAM to BuildConfig.BUILD_TYPE,
                                    CONTENT_ID_PARAM to content.id,
                                    CONTENT_TITLE_PARAM to content.title,
                                    CONTENT_PREVIEW_IMAGE_PARAM to content.previewImage
                            )
                    ).continueWith { task -> (task.result?.data as HashMap<String, String>) }
                    .await().also { response ->
                        if (response?.get(ERROR_PATH_PARAM).isNullOrEmpty())
                            emit(success((OpenContent(
                                    position = selectContent.position,
                                    content = selectContent.content,
                                    filePath = response?.get(FILE_PATH_PARAM)
                            ))))
                        else emit(error(response?.get(ERROR_PATH_PARAM)!!, null))
                    }
        } catch (error: FirebaseFunctionsException) {
            val errorMessage = if (error is FirebaseFunctionsException)
                "$GET_AUDIOCAST_FUNCTION exception: " +
                        "${error.code.name} details: ${error.details.toString()}"
            else "$GET_AUDIOCAST_FUNCTION exception: ${error.localizedMessage}"
            emit(error(errorMessage, null))
        }
    }

    fun editContentLabels(
            feedType: FeedType,
            actionType: UserActionType,
            content: Content?,
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

    /**
     * Updates a logged-in user's labeled feeds on the backend to the local data.
     *
     * @param user CollectionReference user's Firestore document
     * @param timeframe Timestamp of feed to query
     * @param labelsSet HashSet<String> of content IDs with a label to filter from the main feed
     * @param collection String type of labeled feed
     * @param flow FlowCollector<Resource<Flow<PagedList<Content>>>> to emit status of request
     */
    private suspend fun syncLabeledContent(
            user: CollectionReference,
            timeframe: Timestamp,
            labelsSet: HashSet<String>,
            collection: String,
            flow: FlowCollector<Resource<Flow<PagedList<Content>>>>
    ) {
        val labelsResponse = user.document(COLLECTIONS_DOCUMENT)
                .collection(collection)
                .orderBy(TIMESTAMP, DESCENDING)
                .awaitRealtime()
        if (labelsResponse.error == null) {
            val labelsList = labelsResponse.packet?.documentChanges?.map { doc ->
                val content = doc.document.toObject(Content::class.java)
                // Only add content to labelsSet if it is newer than the specified timeframe.
                // labelsSet used to filter labeled content out of the main feed.
                if (content.timestamp > timeframe) labelsSet.add(content.id)
                content
            }
            // Add all content with a label to the local storage.
            if (labelsList!!.isNotEmpty()) dao.insertFeed(labelsList)
        } else
            flow.emit(error("Error retrieving user save_collection: "
                    + labelsResponse.error.localizedMessage, null))
    }

    /**
     * Updates the main local feed data from the the backend when the user is logged-in and realtime
     * content is enabled.
     *
     * @param timeframe Timestamp of feed to query
     * @param labeledSet HashSet<String> of new unique content IDs to add to the local feed
     * @param flow FlowCollector<Resource<Flow<PagedList<Content>>>> to emit status of request
     */
    private suspend fun getLoggedInAndRealtimeContent(
            timeframe: Timestamp,
            labeledSet: HashSet<String>,
            flow: FlowCollector<Resource<Flow<PagedList<Content>>>>
    ) {
        val response = contentEnCollection.orderBy(TIMESTAMP, DESCENDING)
                .whereGreaterThanOrEqualTo(TIMESTAMP, timeframe)
                .awaitRealtime()
        if (response.error == null) {
            val contentList = response.packet?.documentChanges
                    ?.map { change -> change.document.toObject(Content::class.java) }
                    ?.filter { content -> !labeledSet.contains(content.id) }
            dao.insertFeed(contentList)
            flow.emit(success(getMainFeedRoom(timeframe)))
        } else flow.emit(error(CONTENT_LOGGED_IN_REALTIME_ERROR + response.error.localizedMessage, null))
    }

    /**
     * Updates the main local feed data from the the backend when the user is logged-in and realtime
     * content is disabled.
     *
     * @param timeframe Timestamp of feed to query
     * @param labeledSet HashSet<String> of new unique content IDs to add to the local feed
     * @param flow FlowCollector<Resource<Flow<PagedList<Content>>>> to emit status of request
     */
    private suspend fun getLoggedInNonRealtimeContent(
            timeframe: Timestamp,
            labeledSet: HashSet<String>,
            flow: FlowCollector<Resource<Flow<PagedList<Content>>>>
    ) =
            try {
                val contentList = contentEnCollection.orderBy(TIMESTAMP, DESCENDING)
                        .whereGreaterThanOrEqualTo(TIMESTAMP, timeframe).get().await()
                        .documentChanges
                        .map { change -> change.document.toObject(Content::class.java) }
                        .filter { content -> !labeledSet.contains(content.id) }
                dao.insertFeed(contentList)
                flow.emit(success(getMainFeedRoom(timeframe)))
            } catch (error: FirebaseFirestoreException) {
                flow.emit(error("CONTENT_LOGGED_IN_NON_REALTIME_ERROR ${error.localizedMessage}", null))
            }

    /**
     * Updates the main local feed data from the the backend when the user is logged-out and
     * realtime content is disabled.
     *
     * @param timeframe Timestamp of feed to query
     * @param flow FlowCollector<Resource<Flow<PagedList<Content>>>> to emit status of request
     */
    private suspend fun getLoggedOutNonRealtimeContent(
            timeframe: Timestamp,
            flow: FlowCollector<Resource<Flow<PagedList<Content>>>>
    ) =
            try {
                val contentList = contentEnCollection.orderBy(TIMESTAMP, DESCENDING)
                        .whereGreaterThanOrEqualTo(TIMESTAMP, timeframe).get().await()
                        .documentChanges
                        .map { change -> change.document.toObject(Content::class.java) }
                dao.insertFeed(contentList)
                flow.emit(success(getMainFeedRoom(timeframe)))
            } catch (error: FirebaseFirestoreException) {
                flow.emit(error(CONTENT_LOGGED_OUT_NON_REALTIME_ERROR + error.localizedMessage, null))
            }

    private fun addContentLabel(
            actionType: UserActionType,
            userCollection: CollectionReference,
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
            dao.updateContent(content)
            emit(success(position))
        } catch (error: FirebaseFirestoreException) {
            emit(error("'${content?.title}' failed to be added to collection $collection", null))
        }
    }

    private fun removeContentLabel(
            userReference: CollectionReference,
            collection: String,
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