package app.coinverse.content

import android.app.Application
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar.GONE
import android.widget.ProgressBar.VISIBLE
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.paging.PagedList
import app.coinverse.R.string.*
import app.coinverse.content.models.*
import app.coinverse.content.models.ContentViewEffect.*
import app.coinverse.content.models.ContentViewEvent.ContentSwiped
import app.coinverse.firebase.CLEAR_FEED_COUNT
import app.coinverse.utils.*
import app.coinverse.utils.DateAndTime.getTimeframe
import app.coinverse.utils.Enums.ContentType.*
import app.coinverse.utils.Enums.FeedType
import app.coinverse.utils.Enums.FeedType.*
import app.coinverse.utils.Enums.UserActionType
import app.coinverse.utils.livedata.Event
import app.coinverse.utils.models.Lce
import app.coinverse.utils.models.ToolbarState
import com.crashlytics.android.Crashlytics
import com.google.firebase.Timestamp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseUser

class ContentViewModel(application: Application) : AndroidViewModel(application) {
    val viewState: LiveData<ContentViewState> get() = _viewState
    val viewEffect: LiveData<Event<ContentViewEffect>> get() = _viewEffect
    private val LOG_TAG = ContentViewModel::class.java.simpleName
    //TODO: Add isRealtime Boolean for paid feature.
    private val app = application
    private val contentRepository = ContentRepository(application)
    private var analytics = FirebaseAnalytics.getInstance(application)
    private val _viewState = MutableLiveData<ContentViewState>()
    private val _viewEffect = MutableLiveData<Event<ContentViewEffect>>()
    private val contentLoadingSet = hashSetOf<String>()

    fun processEvent(event: ContentViewEvent) {
        when (event) {
            is ContentViewEvent.ScreenLoad -> {
                _viewState.value = ContentViewState(
                        event.feedType,
                        event.timeframe,
                        setToolbar(event.feedType),
                        getContentList(event, event.feedType, event.isRealtime, getTimeframe(event.timeframe)),
                        MutableLiveData<Event<ContentResult.ContentToPlay>>(),
                        MutableLiveData<Event<ContentResult.ContentLabeled>>())
                _viewEffect.value = Event(UpdateAds())
            }
            is ContentViewEvent.SwipeToRefresh -> _viewState.value = _viewState.value?.copy(
                    contentList = getContentList(event, event.feedType, event.isRealtime, getTimeframe(event.timeframe)))
            is ContentViewEvent.ContentSelected -> {
                val contentSelected = ContentViewEvent.ContentSelected(event.position, event.content)
                when (contentSelected.content.contentType) {
                    ARTICLE -> _viewState.value = _viewState.value?.copy(
                            contentToPlay =
                            Transformations.switchMap(contentRepository.getAudiocast(contentSelected)) { contentToPlay ->
                                when (contentToPlay) {
                                    is Lce.Loading ->
                                        MutableLiveData<Event<ContentResult.ContentToPlay>>().apply {
                                            setContentLoadingStatus(contentSelected.content.id, View.VISIBLE)
                                            _viewEffect.value = Event(NotifyItemChanged(contentSelected.position))
                                        }
                                    is Lce.Content ->
                                        MutableLiveData<Event<ContentResult.ContentToPlay>>().apply {
                                            setContentLoadingStatus(contentSelected.content.id, View.GONE)
                                            _viewEffect.value = Event(NotifyItemChanged(contentSelected.position))
                                            this.value = Event(contentToPlay.packet)
                                        }
                                    is Lce.Error ->
                                        MutableLiveData<Event<ContentResult.ContentToPlay>>().apply {
                                            setContentLoadingStatus(contentSelected.content.id, View.GONE)
                                            _viewEffect.value = Event(NotifyItemChanged(contentSelected.position))
                                            if (!contentToPlay.packet.response.equals(TTS_CHAR_LIMIT_ERROR))
                                                _viewEffect.value = Event(SnackBar(TTS_CHAR_LIMIT_ERROR_MESSAGE))
                                            else _viewEffect.value = Event(SnackBar(CONTENT_PLAY_ERROR))
                                        }
                                }
                            })
                    YOUTUBE -> {
                        setContentLoadingStatus(contentSelected.content.id, View.GONE)
                        _viewEffect.value = Event(NotifyItemChanged(contentSelected.position))
                        _viewState.value = _viewState.value?.copy(contentToPlay =
                        MutableLiveData<Event<ContentResult.ContentToPlay>>().apply {
                            this.value = Event(ContentResult.ContentToPlay(
                                    contentSelected.position, contentSelected.content, "", ""))
                        })
                    }
                    NONE -> throw IllegalArgumentException("contentType expected, contentType is 'NONE'")
                }
            }
            is ContentViewEvent.ContentSwipeDrawed -> _viewEffect.value = Event(EnableSwipeToRefresh(false))
            is ContentSwiped -> _viewEffect.value =
                    Event(ContentViewEffect.ContentSwiped(event.feedType, event.actionType,
                            event.position))
            is ContentViewEvent.ContentLabeled ->
                if (event.user != null)
                    _viewState.value = _viewState.value?.copy(
                            contentLabeled = Transformations.switchMap(contentRepository.editContentLabels(
                                    event.feedType, event.actionType, event.content, event.user,
                                    event.position)) { lce ->
                                when (lce) {
                                    is Lce.Loading -> MutableLiveData()
                                    is Lce.Content -> {
                                        if (event.feedType == MAIN) {
                                            contentRepository.labelContentFirebaseAnalytics(event.content!!)
                                            updateActionAnalytics(event.actionType, event.content, event.user)
                                            if (event.isMainFeedEmptied)
                                                analytics.logEvent(CLEAR_FEED_EVENT, Bundle().apply {
                                                    contentRepository.updateUserActionCounter(event.user.uid, CLEAR_FEED_COUNT)
                                                    putString(TIMESTAMP_PARAM, Timestamp.now().toString())
                                                })
                                        }
                                        MutableLiveData<Event<ContentResult.ContentLabeled>>().apply {
                                            _viewEffect.value = Event(NotifyItemChanged(event.position))
                                            this.value = Event(ContentResult.ContentLabeled(
                                                    event.position, ""))
                                        }
                                    }
                                    is Lce.Error -> {
                                        _viewEffect.value = Event(SnackBar(CONTENT_LABEL_ERROR))
                                        Crashlytics.log(Log.ERROR, LOG_TAG, lce.packet.errorMessage)
                                        MutableLiveData()
                                    }
                                }
                            }
                    )
                else {
                    _viewEffect.value = Event(NotifyItemChanged(event.position))
                    _viewEffect.value = Event(SignIn(true))
                }
            is ContentViewEvent.ContentShared ->
                _viewEffect.value = Event(ShareContentIntent(contentRepository.getContent(event.content.id)))
            is ContentViewEvent.ContentSourceOpened -> _viewEffect.value = Event(OpenContentSourceIntent(event.url))
            is ContentViewEvent.UpdateAds -> _viewEffect.value = Event(UpdateAds())
        }
    }

    private fun setToolbar(feedType: FeedType) = ToolbarState(
            when (feedType) {
                SAVED, DISMISSED -> VISIBLE
                MAIN -> GONE
            },
            when (feedType) {
                SAVED -> app.getString(saved)
                DISMISSED -> app.getString(dismissed)
                MAIN -> app.getString(app_name)
            },
            when (feedType) {
                SAVED, MAIN -> false
                DISMISSED -> true
            }
    )

    private fun getContentList(event: ContentViewEvent, feedType: FeedType, isRealtime: Boolean,
                               timeframe: Timestamp) =
            if (feedType == MAIN)
                Transformations.switchMap(
                        contentRepository.getMainFeedList(isRealtime, timeframe)) { lce ->
                    when (lce) {
                        is Lce.Loading -> {
                            if (event is ContentViewEvent.SwipeToRefresh)
                                _viewEffect.value = Event(SwipeToRefresh(true))
                            Transformations.switchMap(contentRepository.getRoomMainList(timeframe)) { pagedList ->
                                MutableLiveData<PagedList<Content>>().apply {
                                    _viewEffect.value = Event(ScreenEmpty(pagedList.isEmpty()))
                                    this.value = pagedList
                                }
                            }
                        }
                        is Lce.Content -> {
                            if (event is ContentViewEvent.SwipeToRefresh)
                                _viewEffect.value = Event(SwipeToRefresh(false))
                            Transformations.switchMap(lce.packet.pagedList!!) { pagedList ->
                                MutableLiveData<PagedList<Content>>().apply {
                                    _viewEffect.value = Event(ScreenEmpty(pagedList.isEmpty()))
                                    this.value = pagedList
                                }
                            }
                        }
                        is Lce.Error -> {
                            Crashlytics.log(Log.ERROR, LOG_TAG, lce.packet.errorMessage)
                            _viewEffect.value = Event(SnackBar(
                                    if (event is ContentViewEvent.ScreenLoad) CONTENT_REQUEST_NETWORK_ERROR
                                    else {
                                        _viewEffect.value = Event(SwipeToRefresh(false))
                                        CONTENT_REQUEST_SWIPE_TO_REFRESH_ERROR
                                    }
                            ))
                            Transformations.switchMap(
                                    contentRepository.getRoomMainList(timeframe)) { pagedList ->
                                _viewEffect.value = Event(ScreenEmpty(pagedList.isEmpty()))
                                MutableLiveData<PagedList<Content>>().apply { this.value = pagedList }
                            }
                        }
                    }
                }
            else Transformations.switchMap(contentRepository.getRoomCategoryList(feedType)) { pagedList ->
                MutableLiveData<PagedList<Content>>().apply {
                    _viewEffect.value = Event(ScreenEmpty(pagedList.isEmpty()))
                    this.value = pagedList
                }
            }

    // TODO: Move to own ViewModel for AudioFragment.
    fun updateContentAudioUrl(contentId: String, url: Uri) =
            contentRepository.updateContentAudioUrl(contentId, url)

    fun updateActionAnalytics(actionType: UserActionType, content: Content, user: FirebaseUser) {
        contentRepository.updateActionAnalytics(actionType, content, user)
    }

    fun getContentLoadingStatus(contentId: String?) =
            if (contentLoadingSet.contains(contentId)) VISIBLE else GONE

    //TODO: Set in ViewState.
    private fun setContentLoadingStatus(contentId: String, visibility: Int) {
        if (visibility == VISIBLE) contentLoadingSet.add(contentId)
        else contentLoadingSet.remove(contentId)
    }
}