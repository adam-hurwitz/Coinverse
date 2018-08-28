package app.carpecoin.contentFeed

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import app.carpecoin.Enums
import app.carpecoin.Enums.Timeframe
import app.carpecoin.contentFeed.models.Content
import app.carpecoin.utils.Constants.PAGE_SIZE
import app.carpecoin.utils.Constants.PREFETCH_DISTANCE


class ContentFeedViewModel : ViewModel() {

    //TODO: Add isRealtimeDataEnabled Boolean for paid feature.
    var timeframe = MutableLiveData<Timeframe>()
    var contentFeedDataSourceFactory = ContentFeedDataSourceFactory(timeframe)
    val contentList: LiveData<PagedList<Content>> = LivePagedListBuilder(
            contentFeedDataSourceFactory,
            PagedList.Config.Builder()
                    .setEnablePlaceholders(true)
                    .setPrefetchDistance(PREFETCH_DISTANCE)
                    .setPageSize(PAGE_SIZE)
                    .build())
            .build()

    var contentSelected = MutableLiveData<Content>()

    init {
        timeframe.value = Enums.Timeframe.WEEK
    }

    fun contentClicked(content: Content) {
        contentSelected.value = content
    }

}