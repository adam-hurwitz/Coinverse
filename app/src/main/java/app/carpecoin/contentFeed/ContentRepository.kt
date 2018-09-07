package app.carpecoin.contentFeed

import android.util.Log
import androidx.paging.DataSource
import app.carpecoin.Enums
import app.carpecoin.contentFeed.models.Content
import app.carpecoin.contentFeed.room.ContentDatabase
import app.carpecoin.firebase.FirestoreCollections
import app.carpecoin.utils.Constants
import app.carpecoin.utils.DateAndTime.getTimeframe
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.util.*
import kotlin.collections.HashSet

private val LOG_TAG = ContentRepository.javaClass.simpleName

object ContentRepository {

    private lateinit var archivedListenerRegistration: ListenerRegistration
    private lateinit var contentListenerRegistration: ListenerRegistration

    private var archivedSet = HashSet<Content>()

    //TODO: Filter on server.
    fun getContent(contentDatabase: ContentDatabase, isRealtime: Boolean,
                   timeframe: Enums.Timeframe) {

        val contentDao = contentDatabase.contentDao()

        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            archivedListenerRegistration = FirestoreCollections.usersCollection
                    .document(user.uid)
                    .collection(FirestoreCollections.ARCHIVED_COLLECTION)
                    .orderBy(Constants.TIMESTAMP, Query.Direction.DESCENDING)
                    .whereGreaterThanOrEqualTo(Constants.TIMESTAMP, getTimeframe(timeframe))
                    .addSnapshotListener(EventListener { value, error ->
                        error?.run {
                            Log.e(LOG_TAG, "Content EventListener Failed.", error)
                            return@EventListener
                        }
                        archivedSet.clear()
                        for (document in value!!.documentChanges) {
                            val archivedContent = document.document.toObject(Content::class.java)
                            archivedSet.add(archivedContent)
                            Thread(Runnable { run { contentDao.delete(archivedContent) } }).start()
                        }
                    })
            //Logged in and realtime enabled.
            if (isRealtime) {
                contentListenerRegistration = FirestoreCollections.contentCollection
                        .collection(FirestoreCollections.ALL_COLLECTION)
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
                                if (!archivedSet.contains(content)) {
                                    contentList.add(content)
                                }
                            }
                            Thread(Runnable { run { contentDao.insertAll(contentList) } }).start()
                        })
            } else { // Logged in but not realtime.
                FirestoreCollections.contentCollection
                        .collection(FirestoreCollections.ALL_COLLECTION)
                        .orderBy(Constants.TIMESTAMP, Query.Direction.DESCENDING)
                        .whereGreaterThanOrEqualTo(Constants.TIMESTAMP, getTimeframe(timeframe))
                        .get()
                        .addOnCompleteListener {
                            val contentList = arrayListOf<Content?>()
                            for (document in it.result.documentChanges) {
                                val content = document.document.toObject(Content::class.java)
                                //TODO: Filter on server.
                                if (!archivedSet.contains(content)) {
                                    contentList.add(content)
                                }
                            }
                            Thread(Runnable { run { contentDao.insertAll(contentList) } }).start()
                        }
            }

        } else { // Looged out and thus not realtime.
            FirestoreCollections.contentCollection
                    .collection(FirestoreCollections.ALL_COLLECTION)
                    .orderBy(Constants.TIMESTAMP, Query.Direction.DESCENDING)
                    .whereGreaterThanOrEqualTo(Constants.TIMESTAMP, getTimeframe(timeframe))
                    .get()
                    .addOnCompleteListener {
                        val contentList = arrayListOf<Content?>()
                        for (document in it.result.documents) {
                            val content = document.toObject(Content::class.java)
                            contentList.add(content)
                        }
                        Thread(Runnable { run { contentDao.insertAll(contentList) } }).start()
                    }
        }
    }

    fun getAllPaged(contentDatabase: ContentDatabase,
                    timeframe: Date): DataSource.Factory<Int, Content> {
        return contentDatabase.contentDao().getAllPaged(timeframe)
    }

}