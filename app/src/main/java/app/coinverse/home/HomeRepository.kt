package app.coinverse.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.MutableLiveData
import app.coinverse.content.room.CoinverseDatabase
import app.coinverse.firebase.FirestoreCollections.messageCenterCollection
import app.coinverse.firebase.FirestoreCollections.usersCollection
import app.coinverse.user.models.User
import app.coinverse.utils.MESSAGE_CENTER_UNREAD_COUNT
import app.coinverse.utils.TIMESTAMP
import app.coinverse.utils.MESSAGE_CENTER
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query.Direction

class HomeRepository(application: Application) {

    private val LOG_TAG = HomeRepository::javaClass.name

    var messageCenterUpdatesLiveData = MutableLiveData<ArrayList<MessageCenterUpdate>>()
    var messageCenterUnreadCountLiveData = MutableLiveData<Double>()

    private var analytics: FirebaseAnalytics
    private var firestore: FirebaseFirestore
    private var database: CoinverseDatabase

    init {
        analytics = FirebaseAnalytics.getInstance(application)
        firestore = FirebaseFirestore.getInstance()
        database = CoinverseDatabase.getAppDatabase(application)
    }

    fun syncMessageCenterUpdates(userId: String?) {
        if (userId != null) {
            val userMessageCenterList = ArrayList<MessageCenterUpdate>()
            val userCollection = usersCollection.document(userId)
            val userMessageCenterCollection = userCollection
                    .collection(MESSAGE_CENTER)
            userMessageCenterCollection.orderBy(TIMESTAMP, Direction.DESCENDING)
                    .addSnapshotListener(EventListener { value, error ->
                        error?.run {
                            Log.e(LOG_TAG, "User Message Center Listener Failed.", error)
                            return@EventListener
                        }
                        value!!.documents.all { document ->
                            val messageCenterUpdate = document.toObject(MessageCenterUpdate::class.java)!!
                            userMessageCenterList.add(messageCenterUpdate)
                            true
                        }
                        messageCenterUpdatesLiveData.value = userMessageCenterList
                    })

            var unreadMessageCenterCount = 0.0
            messageCenterCollection.orderBy(TIMESTAMP, Direction.DESCENDING)
                    .addSnapshotListener(EventListener { value, error ->
                        error?.run {
                            Log.e(LOG_TAG, "Message Center Listener Failed.", error)
                            return@EventListener
                        }
                        value!!.documents.all { document ->
                            val messageCenterUpdate =
                                    document.toObject(MessageCenterUpdate::class.java)!!
                            if (!userMessageCenterList.contains(messageCenterUpdate)) {
                                userMessageCenterList.add(messageCenterUpdate)
                                unreadMessageCenterCount++
                                userMessageCenterCollection.document(messageCenterUpdate.id).set(messageCenterUpdate)
                            }
                            if (unreadMessageCenterCount > 0.0)
                                updateUnreadMessageCenterCount(userCollection, unreadMessageCenterCount)
                            messageCenterUpdatesLiveData.value = userMessageCenterList
                            true
                        }
                    })
            observeMessageCenterCount(userCollection)
        } else {
            messageCenterCollection.orderBy(TIMESTAMP, Direction.DESCENDING)
                    .addSnapshotListener(EventListener { value, error ->
                        error?.run {
                            Log.e(LOG_TAG, "Message Center Listener Failed.", error)
                            return@EventListener
                        }
                        val messagesList = ArrayList<MessageCenterUpdate>()
                        value!!.documents.all { document ->
                            messagesList.add(document.toObject(MessageCenterUpdate::class.java)!!)
                            true
                        }
                        messageCenterUpdatesLiveData.value = messagesList
                    })
        }
    }

    fun updateUnreadMessageCenterCount(userCollection: DocumentReference, count: Double) {
        userCollection.update(MESSAGE_CENTER_UNREAD_COUNT, count)
    }

    fun observeMessageCenterCount(userCollection: DocumentReference) {
        userCollection.addSnapshotListener(EventListener { value, error ->
            error?.run {
                Log.e(LOG_TAG, "User Listener Failed.", error)
                return@EventListener
            }
            messageCenterUnreadCountLiveData.value =
                    value!!.toObject(User::class.java)!!.messageCenterUnreadCount
        })
    }
}