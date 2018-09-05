package app.carpecoin.contentFeed

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import app.carpecoin.Enums
import app.carpecoin.Enums.Timeframe
import app.carpecoin.contentFeed.models.Content
import app.carpecoin.contentFeed.room.ContentDatabase
import app.carpecoin.utils.Constants.PAGE_SIZE
import app.carpecoin.utils.Constants.PREFETCH_DISTANCE
import app.carpecoin.utils.DateAndTime.getTimeframe


class ContentFeedViewModel : ViewModel() {
    lateinit var contentDatabase: ContentDatabase

    //TODO: Add isRealtimeDataEnabled Boolean for paid feature.
    var timeframe = MutableLiveData<Timeframe>()
    var isRealtimeDataEnabled = true

    fun initializeData(isRealtimeDataEnabled: Boolean) {
        ContentRepository.startFirestoreEventListeners(contentDatabase, isRealtimeDataEnabled, timeframe.value!!)
    }

    fun getContentList(): LiveData<PagedList<Content>> {
        return LivePagedListBuilder(
                ContentRepository.getAllPaged(getTimeframe(timeframe.value)),
                PagedList.Config.Builder()
                        .setEnablePlaceholders(true)
                        .setPrefetchDistance(PREFETCH_DISTANCE)
                        .setPageSize(PAGE_SIZE)
                        .build())
                .build()
    }

    var contentSelected = MutableLiveData<Content>()

    init {
        timeframe.value = Enums.Timeframe.WEEK
    }

    fun contentClicked(content: Content) {
        contentSelected.value = content
    }

}