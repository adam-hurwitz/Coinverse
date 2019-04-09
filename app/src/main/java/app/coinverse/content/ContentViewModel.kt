package app.coinverse.content

import android.app.Application
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.ProgressBar.GONE
import android.widget.ProgressBar.VISIBLE
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList.Config.Builder
import app.coinverse.Enums.ContentType.*
import app.coinverse.Enums.FeedType
import app.coinverse.Enums.FeedType.DISMISSED
import app.coinverse.Enums.FeedType.SAVED
import app.coinverse.Enums.Timeframe
import app.coinverse.Enums.Timeframe.DAY
import app.coinverse.Enums.UserActionType
import app.coinverse.content.models.Content
import app.coinverse.content.models.ContentSelected
import app.coinverse.utils.*
import app.coinverse.utils.DateAndTime.getTimeframe
import app.coinverse.utils.livedata.Event
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.functions.FirebaseFunctionsException

class ContentViewModel(application: Application) : AndroidViewModel(application) {
    private val LOG_TAG = ContentViewModel::class.java.simpleName

    //TODO: Add isRealtime Boolean for paid feature.
    var feedType = FeedType.NONE
    val contentLoadingSet = hashSetOf<String>()
    val timeframe: LiveData<Timeframe> get() = _timeframe
    val contentSelected: LiveData<Event<ContentSelected>> get() = _contentSelected

    private val contentRepository: ContentRepository
    private val _timeframe = MutableLiveData<Timeframe>()
    private val pagedListConfiguration =
            Builder().setEnablePlaceholders(true)
                    .setPrefetchDistance(PREFETCH_DISTANCE)
                    .setPageSize(PAGE_SIZE)
                    .build()
    private val _contentSelected = MutableLiveData<Event<ContentSelected>>()

    init {
        contentRepository = ContentRepository(application)
        _timeframe.value = DAY
    }

    fun initMainContent(isRealtime: Boolean) =
            contentRepository.initMainContent(isRealtime, timeframe.value!!)

    fun getContent(contentId: String) = contentRepository.getContent(contentId)

    fun getMainRoomContent() =
            LivePagedListBuilder(
                    contentRepository.getMainRoomContent(getTimeframe(timeframe.value)),
                    pagedListConfiguration).build()

    fun getCategorizedRoomContent(feedType: FeedType) =
            LivePagedListBuilder(
                    contentRepository.getCategorizedRoomContent(feedType),
                    pagedListConfiguration)
                    .build()

    fun getToolbarVisibility() =
            when (feedType) {
                SAVED, DISMISSED -> View.VISIBLE
                else -> View.GONE
            }

    fun onContentClicked(contentSelected: ContentSelected) {
        when (contentSelected.content.contentType) {
            ARTICLE -> {
                contentRepository.getAudiocast(contentSelected.content).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val response = task.result
                        if (response?.get(ERROR_PATH_PARAM).isNullOrEmpty()) {
                            contentSelected.response = response?.get(FILE_PATH_PARAM)!!
                            Log.v(LOG_TAG, "getAudioCast Success: " + response.get(FILE_PATH_PARAM))
                        } else {
                            contentSelected.response = response?.get(ERROR_PATH_PARAM)!!
                            Log.v(LOG_TAG, "getAudioCast Error: " + response.get(ERROR_PATH_PARAM))
                        }
                        _contentSelected.value = Event(contentSelected)
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
            YOUTUBE -> _contentSelected.value = Event(contentSelected)
            NONE -> throw IllegalArgumentException("contentType expected, contentType is 'NONE'")
        }
    }

    fun organizeContent(feedType: FeedType, actionType: UserActionType, user: FirebaseUser,
                        content: Content?, mainFeedEmptied: Boolean) =
            contentRepository.organizeContent(feedType, actionType, content, user, mainFeedEmptied)

    fun updateContentAudioUrl(contentId: String, url: Uri) =
            contentRepository.updateContentAudioUrl(contentId, url)

    fun updateActions(actionType: UserActionType, content: Content, user: FirebaseUser) {
        contentRepository.updateActions(actionType, content, user)
    }

    fun setContentLoadingStatus(contentId: String, visibility: Int) {
        if (visibility == VISIBLE) contentLoadingSet.add(contentId)
        else contentLoadingSet.remove(contentId)
    }

    fun getContentLoadingStatus(contentId: String?) =
            if (contentLoadingSet.contains(contentId)) VISIBLE else GONE
}