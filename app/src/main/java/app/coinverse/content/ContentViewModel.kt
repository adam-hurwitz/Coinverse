package app.coinverse.content

import android.app.Application
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar.GONE
import android.widget.ProgressBar.VISIBLE
import androidx.lifecycle.*
import androidx.lifecycle.Transformations.switchMap
import androidx.paging.PagedList
import app.coinverse.R.string.*
import app.coinverse.analytics.updateActionAnalytics
import app.coinverse.content.models.*
import app.coinverse.content.models.ContentViewEffect.*
import app.coinverse.content.models.ContentViewEvent.ContentSwiped
import app.coinverse.firebase.CLEAR_FEED_COUNT
import app.coinverse.utils.*
import app.coinverse.utils.DateAndTime.getTimeframe
import app.coinverse.utils.Enums.ContentType.*
import app.coinverse.utils.Enums.FeedType
import app.coinverse.utils.Enums.FeedType.*
import app.coinverse.utils.livedata.Event
import app.coinverse.utils.models.Lce
import app.coinverse.utils.models.ToolbarState
import com.crashlytics.android.Crashlytics
import com.google.firebase.Timestamp
import com.google.firebase.analytics.FirebaseAnalytics

class ContentViewModel(application: Application) : AndroidViewModel(application) {
    var contentPlaying = Content()
    val feedViewState: LiveData<FeedViewState> get() = _feedViewState
    val playerViewState: LiveData<PlayerViewState> get() = _playerViewState
    val viewEffect: LiveData<Event<ContentViewEffect>> get() = _viewEffect
    private val LOG_TAG = ContentViewModel::class.java.simpleName
    //TODO: Add isRealtime Boolean for paid feature.
    private val app = application
    private val repository = ContentRepository.also { it.invoke(application) }
    private var analytics = FirebaseAnalytics.getInstance(application)
    private val _feedViewState = MutableLiveData<FeedViewState>()
    private val _playerViewState = MutableLiveData<PlayerViewState>()
    private val _viewEffect = MutableLiveData<Event<ContentViewEffect>>()
    private val contentLoadingSet = hashSetOf<String>()

    fun processEvent(event: ContentViewEvent) {
        when (event) {
            is ContentViewEvent.FeedLoad -> {
                _feedViewState.value = FeedViewState(event.feedType, event.timeframe,
                        setToolbar(event.feedType), getContentList(event, event.feedType,
                        event.isRealtime, getTimeframe(event.timeframe)),
                        MutableLiveData(), MutableLiveData(), MutableLiveData())
                _viewEffect.value = Event(UpdateAds())
            }
            is ContentViewEvent.PlayerLoad ->
                _playerViewState.value = PlayerViewState(getContentPlayer(
                        event.contentId, event.filePath, event.previewImageUrl))
            is ContentViewEvent.SwipeToRefresh -> _feedViewState.value = _feedViewState.value?.copy(
                    contentList = getContentList(event, event.feedType, event.isRealtime,
                            getTimeframe(event.timeframe)))
            is ContentViewEvent.ContentSelected -> {
                val contentSelected = ContentViewEvent.ContentSelected(event.position, event.content)
                when (contentSelected.content.contentType) {
                    ARTICLE -> _feedViewState.value = _feedViewState.value?.copy(contentToPlay =
                    switchMap(repository.getAudiocast(contentSelected)) { contentToPlay ->
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
                                    if (!contentToPlay.packet.filePath.equals(TTS_CHAR_LIMIT_ERROR))
                                        _viewEffect.value = Event(SnackBar(TTS_CHAR_LIMIT_ERROR_MESSAGE))
                                    else _viewEffect.value = Event(SnackBar(CONTENT_PLAY_ERROR))
                                }
                        }
                    })
                    YOUTUBE -> {
                        setContentLoadingStatus(contentSelected.content.id, View.GONE)
                        _viewEffect.value = Event(NotifyItemChanged(contentSelected.position))
                        _feedViewState.value = _feedViewState.value?.copy(contentToPlay =
                        MutableLiveData<Event<ContentResult.ContentToPlay>>().apply {
                            this.value = Event(ContentResult.ContentToPlay(
                                    contentSelected.position, contentSelected.content, "", ""))
                        })
                    }
                    NONE -> throw IllegalArgumentException(
                            "contentType expected, contentType is 'NONE'")
                }
            }
            is ContentViewEvent.ContentSwipeDrawed ->
                _viewEffect.value = Event(EnableSwipeToRefresh(false))
            is ContentSwiped -> _viewEffect.value =
                    Event(ContentViewEffect.ContentSwiped(event.feedType, event.actionType,
                            event.position))
            is ContentViewEvent.ContentLabeled ->
                if (event.user != null && !event.user.isAnonymous)
                    _feedViewState.value = _feedViewState.value?.copy(contentLabeled =
                    switchMap(repository.editContentLabels(
                            event.feedType, event.actionType, event.content, event.user,
                            event.position)) { lce ->
                        when (lce) {
                            is Lce.Loading -> MutableLiveData()
                            is Lce.Content -> {
                                if (event.feedType == MAIN) {
                                    repository.labelContentFirebaseAnalytics(event.content!!)
                                    //TODO: Move to Cloud Function. Use with WorkManager. Return error in ContentLabeled.
                                    updateActionAnalytics(event.actionType, event.content, event.user)
                                    if (event.isMainFeedEmptied)
                                        analytics.logEvent(CLEAR_FEED_EVENT, Bundle().apply {
                                            repository.updateUserActionCounter(event.user.uid, CLEAR_FEED_COUNT)
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
                    })
                else {
                    _viewEffect.value = Event(NotifyItemChanged(event.position))
                    _viewEffect.value = Event(SignIn(true))
                }
            is ContentViewEvent.ContentShared ->
                _viewEffect.value = Event(ShareContentIntent(repository.getContent(event.content.id)))
            is ContentViewEvent.ContentSourceOpened ->
                _viewEffect.value = Event(OpenContentSourceIntent(event.url))
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
                switchMap(repository.getMainFeedList(isRealtime, timeframe)) { lce ->
                    when (lce) {
                        is Lce.Loading -> {
                            if (event is ContentViewEvent.SwipeToRefresh)
                                _viewEffect.value = Event(SwipeToRefresh(true))
                            switchMap(repository.getRoomMainList(timeframe)) { pagedList ->
                                MutableLiveData<PagedList<Content>>().apply { this.value = pagedList }
                            }
                        }
                        is Lce.Content -> {
                            if (event is ContentViewEvent.SwipeToRefresh)
                                _viewEffect.value = Event(SwipeToRefresh(false))
                            switchMap(lce.packet.pagedList!!) { pagedList ->
                                if (pagedList.isNotEmpty())
                                    _viewEffect.value = Event(ScreenEmpty(false))
                                MutableLiveData<PagedList<Content>>().apply { this.value = pagedList }
                            }
                        }
                        is Lce.Error -> {
                            Crashlytics.log(Log.ERROR, LOG_TAG, lce.packet.errorMessage)
                            _viewEffect.value = Event(SnackBar(
                                    if (event is ContentViewEvent.FeedLoad)
                                        CONTENT_REQUEST_NETWORK_ERROR
                                    else {
                                        _viewEffect.value = Event(SwipeToRefresh(false))
                                        CONTENT_REQUEST_SWIPE_TO_REFRESH_ERROR
                                    }
                            ))
                            switchMap(repository.getRoomMainList(timeframe)) { pagedList ->
                                MutableLiveData<PagedList<Content>>().apply { this.value = pagedList }
                            }
                        }
                    }
                }
            else switchMap(repository.getRoomCategoryList(feedType)) { pagedList ->
                MutableLiveData<PagedList<Content>>().apply {
                    _viewEffect.value = Event(ScreenEmpty(pagedList.isEmpty()))
                    this.value = pagedList
                }
            }

    private fun getContentPlayer(contentId: String, filePath: String, imageUrl: String) =
            getContentUri(contentId, filePath).combineLiveData(bitmapToByteArray(imageUrl)) { a, b ->
                Event(ContentResult.ContentPlayer(a.peekEvent().uri, b.peekEvent().image,
                        getLiveDataErrors(a, b)
                ))
            }

    /**
     * Sets the value to the result of a function that is called when both `LiveData`s have data
     * or when they receive updates after that.
     */
    private fun <T, A, B> LiveData<A>.combineLiveData(other: LiveData<B>, onChange: (A, B) -> T) =
            MediatorLiveData<T>().also { result ->
                var source1emitted = false
                var source2emitted = false
                val mergeF = {
                    val source1Value = this.value
                    val source2Value = other.value
                    if (source1emitted && source2emitted)
                        result.value = onChange.invoke(source1Value!!, source2Value!!)
                }
                result.addSource(this) { source1emitted = true; mergeF.invoke() }
                result.addSource(other) { source2emitted = true; mergeF.invoke() }
            }

    private fun getContentUri(contentId: String, filePath: String) =
            switchMap(repository.getContentUri(contentId, filePath)) { lce ->
                when (lce) {
                    is Lce.Loading -> MutableLiveData()
                    is Lce.Content -> MutableLiveData<Event<ContentResult.ContentUri>>().apply {
                        value = Event(ContentResult.ContentUri(lce.packet.uri, ""))
                    }
                    is Lce.Error -> {
                        Crashlytics.log(Log.ERROR, LOG_TAG, lce.packet.errorMessage)
                        MutableLiveData()
                    }
                }
            }

    private fun bitmapToByteArray(url: String) = liveData {
        emitSource(switchMap(repository.bitmapToByteArray(url)) { lce ->
            when (lce) {
                is Lce.Loading -> liveData {}
                is Lce.Content -> liveData {
                    emit(Event(ContentResult.ContentBitmap(lce.packet.image, lce.packet.errorMessage)))
                }
                is Lce.Error -> liveData {
                    Crashlytics.log(Log.WARN, LOG_TAG,
                            "bitmapToByteArray error or null - ${lce.packet.errorMessage}")
                }
            }
        })
    }

    private fun getLiveDataErrors(a: Event<ContentResult.ContentUri>, b: Event<ContentResult.ContentBitmap>) =
            a.peekEvent().errorMessage.apply { if (this.isNotEmpty()) this }.apply {
                b.peekEvent().errorMessage.also {
                    if (it.isNotEmpty()) this.plus(" " + it)
                }
            }

    fun getContentLoadingStatus(contentId: String?) =
            if (contentLoadingSet.contains(contentId)) VISIBLE else GONE

    private fun setContentLoadingStatus(contentId: String, visibility: Int) {
        if (visibility == VISIBLE) contentLoadingSet.add(contentId)
        else contentLoadingSet.remove(contentId)
    }
}