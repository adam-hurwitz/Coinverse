package app.coinverse.content

import android.app.Application
import android.util.Log
import android.view.View
import android.widget.ProgressBar.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import app.coinverse.BuildConfig.DEBUG
import app.coinverse.Enums
import app.coinverse.Enums.ContentType.*
import app.coinverse.Enums.FeedType
import app.coinverse.Enums.FeedType.DISMISSED
import app.coinverse.Enums.FeedType.SAVED
import app.coinverse.Enums.Timeframe
import app.coinverse.Enums.UserActionType
import app.coinverse.content.models.Content
import app.coinverse.content.models.ContentSelected
import app.coinverse.utils.*
import app.coinverse.utils.DateAndTime.getTimeframe
import app.coinverse.utils.livedata.Event
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.functions.FirebaseFunctionsException

class ContentViewModel(application: Application) : AndroidViewModel(application) {
    val LOG_TAG = ContentViewModel::class.java.name

    var contentRepository: ContentRepository
    var feedType = NONE.name
    //TODO: Add isRealtime Boolean for paid feature.
    var timeframe = MutableLiveData<Timeframe>()
    //TODO: Needs further testing.
    //var isNewContentAddedLiveData: LiveData<Boolean>

    var contentLoadingStatusMap = hashMapOf<String, Int>()

    private val _contentSelected = MutableLiveData<Event<ContentSelected>>()
    val contentSelected: LiveData<Event<ContentSelected>>
        get() = _contentSelected

    val pagedListConfiguration = PagedList.Config.Builder()
            .setEnablePlaceholders(true)
            .setPrefetchDistance(PREFETCH_DISTANCE)
            .setPageSize(PAGE_SIZE)
            .build()

    init {
        contentRepository = ContentRepository(application)
        timeframe.value = Enums.Timeframe.WEEK
        //TODO: Needs further testing.
        /*val isNewContentAddedLiveData = contentRepository.isNewContentAddedLiveData
        this.isNewContentAddedLiveData = Transformations.map(isNewContentAddedLiveData) { result -> result }*/
    }

    fun initializeMainContent(isRealtime: Boolean) {
        contentRepository.initializeMainRoomContent(isRealtime, timeframe.value!!)
    }

    fun initializeCategorizedContent(feedType: String, userId: String) {
        contentRepository.initializeCategorizedRoomContent(feedType, userId)
    }

    fun getMainContentList() =
            LivePagedListBuilder(
                    contentRepository.getMainContent(getTimeframe(timeframe.value)),
                    pagedListConfiguration).build()

    fun getCategorizedContentList(feedType: FeedType) =
            LivePagedListBuilder(contentRepository.getCategorizedContent(feedType),
                    pagedListConfiguration).build()

    fun getToolbarVisibility() =
            when (feedType) {
                SAVED.name, DISMISSED.name -> View.VISIBLE
                else -> View.GONE
            }

    fun onContentClicked(contentSelected: ContentSelected) {
        val content = contentSelected.content
        when (content.contentType) {
            ARTICLE -> {
                contentRepository.getAudiocast(DEBUG, content).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val response = task.result
                        if (response?.get(ERROR_PATH_PARAM).isNullOrEmpty()) {
                            content.audioUrl = response?.get(FILE_PATH_PARAM)!!
                            Log.v(LOG_TAG, "getAudioCast Success: " + response.get(FILE_PATH_PARAM))
                        } else {
                            content.audioUrl = response?.get(ERROR_PATH_PARAM)!!
                            Log.v(LOG_TAG, "getAudioCast Error: " + response.get(ERROR_PATH_PARAM))
                        }
                        _contentSelected.value = Event(contentSelected)  // Trigger the event by setting a new Event as a new value
                    } else {
                        val e = task.exception
                        if (e is FirebaseFunctionsException) {
                            val code = e.code
                            val details = e.details
                            Log.e(LOG_TAG, "$GET_AUDIOCAST_FUNCTION Exception: " +
                                    "${code.name} Details: ${details.toString()}")
                        }
                    }
                }
            }
            YOUTUBE -> _contentSelected.value = Event(contentSelected)  // Trigger the event by setting a new Event as a new value
            NONE -> throw IllegalArgumentException("contentType expected, contentType is 'NONE'")
        }
    }

    fun organizeContent(feedType: String, actionType: UserActionType, user: FirebaseUser,
                        content: Content?, mainFeedEmptied: Boolean) {
        contentRepository.organizeContent(feedType, actionType, content, user, mainFeedEmptied)
    }

    fun updateActions(actionType: UserActionType, content: Content, user: FirebaseUser) {
        contentRepository.updateActions(actionType, content, user)
    }

    fun getLoadingState(contentId: String) = when (contentLoadingStatusMap.get(contentId)) {
        GONE, INVISIBLE, null -> GONE
        else -> VISIBLE
    }
}