package app.carpecoin.content

import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import app.carpecoin.Enums
import app.carpecoin.Enums.FeedType
import app.carpecoin.Enums.FeedType.*
import app.carpecoin.Enums.Timeframe
import app.carpecoin.content.models.Content
import app.carpecoin.content.room.ContentDatabase
import app.carpecoin.utils.Constants.PAGE_SIZE
import app.carpecoin.utils.Constants.PREFETCH_DISTANCE
import app.carpecoin.utils.DateAndTime.getTimeframe


class ContentViewModel : ViewModel() {
    lateinit var contentDatabase: ContentDatabase

    var feedType = NONE.name
    //TODO: Add isRealtime Boolean for paid feature.
    var timeframe = MutableLiveData<Timeframe>()
    var categorizeContentComplete = MutableLiveData<Content>()
    val pagedListConfiguration = PagedList.Config.Builder()
            .setEnablePlaceholders(true)
            .setPrefetchDistance(PREFETCH_DISTANCE)
            .setPageSize(PAGE_SIZE)
            .build()

    fun initializeMainContent(isRealtime: Boolean) {
        ContentRepository.initializeMainRoomContent(contentDatabase, isRealtime, timeframe.value!!)
    }

    fun initializeCategorizedContent(feedType: String, userId: String) {
        ContentRepository.initializeCategorizedContent(contentDatabase, feedType, userId)
    }

    fun getMainContentList(): LiveData<PagedList<Content>> {
        return LivePagedListBuilder(
                ContentRepository.getMainContent(contentDatabase, getTimeframe(timeframe.value)),
                pagedListConfiguration).build()
    }

    fun getCategorizedContentList(feedType: FeedType): LiveData<PagedList<Content>> {
        return LivePagedListBuilder(ContentRepository.getCategorizedContent(contentDatabase, feedType),
                pagedListConfiguration).build()
    }

    fun getToolbarVisibility(): Int {
        when (feedType) {
            SAVED.name, ARCHIVED.name -> return View.VISIBLE
            else -> return View.GONE
        }
    }

    var contentSelected = MutableLiveData<Content>()

    init {
        timeframe.value = Enums.Timeframe.WEEK
    }

    fun contentClicked(content: Content) {
        contentSelected.value = content
    }

    fun categorizeContentComplete(content: Content) {
        categorizeContentComplete.value = content
    }

}