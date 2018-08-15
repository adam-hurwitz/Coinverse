package app.carpecoin.contentFeed

import androidx.lifecycle.MutableLiveData
import app.carpecoin.utils.Constants.QUALITY_SCORE
import app.carpecoin.utils.auth.Auth
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import app.carpecoin.Enums.Timeframe
import app.carpecoin.Enums.Timeframe.WEEK
import app.carpecoin.contentFeed.models.Content
import app.carpecoin.utils.Constants.CONTENT_ALL_COLLECTION
import app.carpecoin.utils.Constants.CONTENT_BY_WEEK_COLLECTION
import com.google.firebase.firestore.ListenerRegistration

private val LOG_TAG = ContentFeedRepository::class.java.simpleName

object ContentFeedRepository {
    var contentFeedLiveData = MutableLiveData<ArrayList<Content>>()

    private var contentFeedList = ArrayList<Content>()

    private lateinit var listenerRegistration: ListenerRegistration

    fun getContentFeedQuery(timeframe: Timeframe?): Query {
        //TODO: Get Content Collection based on timeframe.
        var contentCollection = ""
        when (timeframe) {
            WEEK -> contentCollection = CONTENT_BY_WEEK_COLLECTION
            else -> contentCollection = CONTENT_ALL_COLLECTION
        }

        //TODO: Explore use of Kotlin dependency injection.
        return FirebaseFirestore.getInstance(FirebaseApp.getInstance(Auth.CONTENT_FIRESTORE_NAME))
                .collection(contentCollection).orderBy(QUALITY_SCORE, Query.Direction.DESCENDING)
    }

    /*fun startFirestoreEventListeners(isLiveDataEnabled: Boolean, timeframe: DateAndTime) {

        if (!isLiveDataEnabled) {
            contentFeedList.clear()
        }

        listenerRegistration = contentFeedCollection
                .orderBy(QUALITY_SCORE, Query.Direction.DESCENDING)
                .addSnapshotListener(EventListener { value, error ->
                    error?.run {
                        Log.e(LOG_TAG, "Content Data EventListener Failed.", error)
                        return@EventListener
                    }

                    if (!isLiveDataEnabled) listenerRegistration.remove()

                    for (priceDataDocument in value!!.getDocumentChanges()) {
                        val content = priceDataDocument.document
                                .toObject(Content::class.java)
                        contentFeedList.add(content)
                    }
                    contentFeedLiveData.value = contentFeedList
                })
    }*/

}