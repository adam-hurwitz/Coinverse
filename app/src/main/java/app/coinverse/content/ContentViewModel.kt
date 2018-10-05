package app.coinverse.content

import android.app.Application
import android.view.View
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import app.coinverse.Enums
import app.coinverse.Enums.FeedType
import app.coinverse.Enums.FeedType.*
import app.coinverse.Enums.Timeframe
import app.coinverse.Enums.UserActionType
import app.coinverse.content.models.Content
import app.coinverse.utils.Constants.PAGE_SIZE
import app.coinverse.utils.Constants.PREFETCH_DISTANCE
import app.coinverse.utils.DateAndTime.getTimeframe
import app.coinverse.utils.livedata.Event
import com.google.firebase.auth.FirebaseUser


class ContentViewModel(application: Application) : AndroidViewModel(application) {

    var contentRepository: ContentRepository
    var feedType = NONE.name
    //TODO: Add isRealtime Boolean for paid feature.
    var timeframe = MutableLiveData<Timeframe>()

    private val _contentSelected = MutableLiveData<Event<Content>>()
    val contentSelected : LiveData<Event<Content>>
        get() = _contentSelected

    val pagedListConfiguration = PagedList.Config.Builder()
            .setEnablePlaceholders(true)
            .setPrefetchDistance(PREFETCH_DISTANCE)
            .setPageSize(PAGE_SIZE)
            .build()

    init {
        contentRepository = ContentRepository(application)
        timeframe.value = Enums.Timeframe.WEEK
    }

    fun initializeMainContent(isRealtime: Boolean) {
        contentRepository.initializeMainRoomContent(isRealtime, timeframe.value!!)
    }

    fun initializeCategorizedContent(feedType: String, userId: String) {
        contentRepository.initializeCategorizedRoomContent(feedType, userId)
    }

    fun getMainContentList(): LiveData<PagedList<Content>> {
        return LivePagedListBuilder(
                contentRepository.getMainContent(getTimeframe(timeframe.value)),
                pagedListConfiguration).build()
    }

    fun getCategorizedContentList(feedType: FeedType): LiveData<PagedList<Content>> {
        return LivePagedListBuilder(contentRepository.getCategorizedContent(feedType),
                pagedListConfiguration).build()
    }

    fun getToolbarVisibility(): Int {
        when (feedType) {
            SAVED.name, DISMISSED.name -> return View.VISIBLE
            else -> return View.GONE
        }
    }

    fun contentClicked(content: Content) {
        _contentSelected.value = Event(content)  // Trigger the event by setting a new Event as a new value
    }

    fun organizeContent(feedType: String, actionType: UserActionType, user: FirebaseUser,
                        content: Content?, mainFeedEmptied: Boolean) {
        contentRepository.organizeContent(feedType, actionType, content, user, mainFeedEmptied)
    }

    fun updateActions(actionType: UserActionType, content: Content, user: FirebaseUser) {
        contentRepository.updateActionsStatusCheck(actionType, content, user)
    }

}