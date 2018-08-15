package app.carpecoin.contentFeed

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import app.carpecoin.Enums
import app.carpecoin.Enums.Timeframe
import app.carpecoin.contentFeed.models.Content


class ContentFeedViewModel: ViewModel(){

    //TODO: Paid feature
    //var isRealtimeDataEnabled = true

    //TODO: Set timeframe from UI.
    var timeframe = MutableLiveData<Timeframe>()

    init {
        timeframe.value = Enums.Timeframe.WEEK
    }

    val contentFeedQuery = ContentFeedRepository.getContentFeedQuery(timeframe.value)

    var contentSelected = MutableLiveData<Content>()

    fun contentClicked(content: Content){
        contentSelected.value = content
        //println(String.format("CONTENT_CLICKED:%s", content.contentTitle))

    }

    //TODO: Keep in case Adapter is refactored to PagedListAdapter
    /*private var toInitializeContentFeedData = MutableLiveData<Boolean>()

    fun initializeData(isRealtimeDataEnabled: Boolean) {
        this.toInitializeContentFeedData.value = true
        ContentFeedRepository.startFirestoreEventListeners(isRealtimeDataEnabled, timeframe.value!!)
    }

    val contentFeedLiveData = Transformations.switchMap(toInitializeContentFeedData) {
        ContentFeedRepository.contentFeedLiveData
    }*/

}