package app.carpecoin.contentFeed

import androidx.lifecycle.MutableLiveData
import androidx.paging.ItemKeyedDataSource
import app.carpecoin.Enums.Timeframe
import app.carpecoin.contentFeed.models.Content
import app.carpecoin.firebase.FirestoreCollections
import app.carpecoin.utils.Constants
import app.carpecoin.utils.DateAndTime
import com.google.firebase.firestore.Query
import java.util.*

class ContentFeedDataSource(var timeframe: MutableLiveData<Timeframe>) : ItemKeyedDataSource<Date, Content>() {

    override fun loadBefore(params: LoadParams<Date>, callback: LoadCallback<Content>) {}
    override fun getKey(item: Content) = item.timestamp
    override fun loadInitial(params: LoadInitialParams<Date>, callback: LoadInitialCallback<Content>) {

        FirestoreCollections.contentCollection
                .collection(FirestoreCollections.ALL_COLLECTION)
                .orderBy(Constants.TIMESTAMP, Query.Direction.DESCENDING)
                .whereGreaterThanOrEqualTo(Constants.TIMESTAMP,
                        DateAndTime.getTimeframe(timeframe.value))
                .limit(params.requestedLoadSize.toLong())
                .get().addOnCompleteListener {
                    val items = arrayListOf<Content?>()
                    for (document in it.result.documents) {
                        val content = document.toObject(Content::class.java)
                        items.add(content)
                    }
                    callback.onResult(items.sortedByDescending { it?.qualityScore })
                }
    }

    override fun loadAfter(params: LoadParams<Date>, callback: LoadCallback<Content>) {
        FirestoreCollections.contentCollection
                .collection(FirestoreCollections.ALL_COLLECTION)
                .orderBy(Constants.TIMESTAMP, Query.Direction.DESCENDING)
                .startAt(params.key)
                .whereGreaterThanOrEqualTo(Constants.TIMESTAMP,
                        DateAndTime.getTimeframe(timeframe.value))
                .limit(params.requestedLoadSize.toLong())
                .get().addOnCompleteListener {
                    val items = arrayListOf<Content?>()
                    for (document in it.result.documents) {
                        val content = document.toObject(Content::class.java)
                        items.add(content)
                    }
                    val sortedByQualityScore =
                            ArrayList(items.sortedByDescending { it?.qualityScore })
                    callback.onResult(sortedByQualityScore)
                    sortedByQualityScore.clear()
                }
    }
}