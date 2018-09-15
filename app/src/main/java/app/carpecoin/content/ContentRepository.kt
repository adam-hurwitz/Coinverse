package app.carpecoin.content

import android.util.Log
import androidx.paging.DataSource
import app.carpecoin.Enums
import app.carpecoin.Enums.FeedType
import app.carpecoin.Enums.FeedType.*
import app.carpecoin.content.models.Content
import app.carpecoin.content.room.ContentDatabase
import app.carpecoin.firebase.FirestoreCollections
import app.carpecoin.firebase.FirestoreCollections.ARCHIVED_COLLECTION
import app.carpecoin.firebase.FirestoreCollections.SAVED_COLLECTION
import app.carpecoin.utils.Constants
import app.carpecoin.utils.DateAndTime.getTimeframe
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.util.*
import kotlin.collections.HashSet

private val LOG_TAG = ContentRepository.javaClass.simpleName

object ContentRepository {
    private lateinit var savedListenerRegistration: ListenerRegistration
    private lateinit var archivedListenerRegistration: ListenerRegistration
    private lateinit var contentListenerRegistration: ListenerRegistration

    private var organizedSet = HashSet<String>()

    //TODO: Filter on server.
    fun initializeMainRoomContent(contentDatabase: ContentDatabase, isRealtime: Boolean,
                                  timeframe: Enums.Timeframe) {

        val contentDao = contentDatabase.contentDao()
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val userReference = FirestoreCollections.usersCollection.document(user.uid)
            organizedSet.clear()
            savedListenerRegistration = userReference
                    .collection(FirestoreCollections.SAVED_COLLECTION)
                    .orderBy(Constants.TIMESTAMP, Query.Direction.DESCENDING)
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
            archivedListenerRegistration = userReference
                    .collection(FirestoreCollections.ARCHIVED_COLLECTION)
                    .orderBy(Constants.TIMESTAMP, Query.Direction.DESCENDING)
                    .addSnapshotListener(EventListener { value, error ->
                        error?.run {
                            Log.e(LOG_TAG, "Content EventListener Failed.", error)
                            return@EventListener
                        }
                        for (document in value!!.documentChanges) {
                            val archivedContent = document.document.toObject(Content::class.java)
                            organizedSet.add(archivedContent.id)
                            Thread(Runnable { run { contentDao.updateContent(archivedContent) } }).start()
                        }
                    })
            //Logged in and realtime enabled.
            if (isRealtime) {
                contentListenerRegistration = FirestoreCollections.contentCollection
                        .orderBy(Constants.TIMESTAMP, Query.Direction.DESCENDING)
                        .whereGreaterThanOrEqualTo(Constants.TIMESTAMP, getTimeframe(timeframe))
                        .addSnapshotListener(EventListener { value, error ->
                            error?.run {
                                Log.e(LOG_TAG, "Content EventListener Failed.", error)
                                return@EventListener
                            }
                            val contentList = arrayListOf<Content?>()
                            for (document in value!!.documentChanges) {
                                val content = document.document.toObject(Content::class.java)
                                if (!organizedSet.contains(content.id)) {
                                    contentList.add(content)
                                }
                            }
                            Thread(Runnable { run { contentDao.insertContent(contentList) } }).start()
                        })
            } else { // Logged in but not realtime.
                FirestoreCollections.contentCollection
                        .orderBy(Constants.TIMESTAMP, Query.Direction.DESCENDING)
                        .whereGreaterThanOrEqualTo(Constants.TIMESTAMP, getTimeframe(timeframe))
                        .get()
                        .addOnCompleteListener {
                            val contentList = arrayListOf<Content?>()
                            for (document in it.result.documentChanges) {
                                val content = document.document.toObject(Content::class.java)
                                if (!organizedSet.contains(content.id)) {
                                    contentList.add(content)
                                }
                            }
                            Thread(Runnable { run { contentDao.insertContent(contentList) } }).start()
                        }
            }

        } else { // Looged out and thus not realtime.
            FirestoreCollections.contentCollection
                    .orderBy(Constants.TIMESTAMP, Query.Direction.DESCENDING)
                    .whereGreaterThanOrEqualTo(Constants.TIMESTAMP, getTimeframe(timeframe))
                    .get()
                    .addOnCompleteListener {
                        val contentList = arrayListOf<Content?>()
                        for (document in it.result.documents) {
                            val content = document.toObject(Content::class.java)
                            contentList.add(content)
                        }
                        Thread(Runnable { run { contentDao.insertContent(contentList) } }).start()
                    }
        }
    }

    fun initializeCategorizedContent(contentDatabase: ContentDatabase, feedType: String, userId: String) {
        var collectionType = ""
        var newFeedType = FeedType.NONE
        if (feedType == SAVED.name) {
            collectionType = SAVED_COLLECTION
            newFeedType = SAVED
        } else if (feedType == ARCHIVED.name) {
            collectionType = ARCHIVED_COLLECTION
            newFeedType = ARCHIVED
        }
        FirestoreCollections.contentCollection
                .document(userId)
                .collection(collectionType)
                .orderBy(Constants.TIMESTAMP, Query.Direction.DESCENDING)
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
                    Thread(Runnable { run { contentDatabase.contentDao().insertContent(contentList) } }).start()
                })
    }

    fun getMainContent(contentDatabase: ContentDatabase, timeframe: Date): DataSource.Factory<Int, Content> {
        return contentDatabase.contentDao().getMainContent(timeframe, MAIN)
    }

    fun getCategorizedContent(contentDatabase: ContentDatabase, feedType: FeedType): DataSource.Factory<Int, Content> {
        return contentDatabase.contentDao().getCategorizedContent(feedType)
    }

    fun setContent(contentViewmodel: ContentViewModel, userReference: DocumentReference,
                   collection: String, content: Content?, mainFeedEmptied: Boolean) {
        userReference
                .collection(collection)
                .document(content!!.contentTitle)
                .set(content)
                .addOnSuccessListener {
                    Log.v(LOG_TAG, String.format("Content added to collection:%s", it))
                    contentViewmodel.categorizeContentComplete(content, mainFeedEmptied)
                }.addOnFailureListener {
                    Log.v(LOG_TAG, String.format("Content failed to be added to collection:%s", it))
                }
    }

    fun deleteContent(userReference: DocumentReference, collection: String, content: Content?) {
        userReference
                .collection(collection)
                .document(content!!.contentTitle)
                .delete()
                .addOnSuccessListener {
                    Log.v(LOG_TAG, String.format("Content deleted from to collection:%s", it))
                }.addOnFailureListener {
                    Log.v(LOG_TAG, String.format("Content failed to be deleted from collection:%s", it))
                }
    }

}