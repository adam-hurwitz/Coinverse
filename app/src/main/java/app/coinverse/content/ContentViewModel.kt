package app.coinverse.content

import android.util.Log
import android.view.View
import android.widget.ProgressBar.GONE
import android.widget.ProgressBar.VISIBLE
import androidx.lifecycle.*
import app.coinverse.R.string.*
import app.coinverse.analytics.Analytics.labelContentFirebaseAnalytics
import app.coinverse.analytics.Analytics.updateActionAnalytics
import app.coinverse.analytics.Analytics.updateFeedEmptiedActionsAndAnalytics
import app.coinverse.content.ContentRepository.bitmapToByteArray
import app.coinverse.content.ContentRepository.editContentLabels
import app.coinverse.content.ContentRepository.getAudiocast
import app.coinverse.content.ContentRepository.getContent
import app.coinverse.content.ContentRepository.getMainFeedList
import app.coinverse.content.ContentRepository.queryLabeledContentList
import app.coinverse.content.ContentRepository.queryMainContentList
import app.coinverse.content.models.*
import app.coinverse.content.models.ContentEffectType.*
import app.coinverse.content.models.ContentViewEvents.*
import app.coinverse.content.models.ContentViewEvents.ContentLabeled
import app.coinverse.utils.*
import app.coinverse.utils.ContentType.*
import app.coinverse.utils.DateAndTime.getTimeframe
import app.coinverse.utils.FeedType.*
import app.coinverse.utils.livedata.Event
import app.coinverse.utils.models.Lce
import app.coinverse.utils.models.Lce.Error
import app.coinverse.utils.models.Lce.Loading
import app.coinverse.utils.models.ToolbarState
import com.crashlytics.android.Crashlytics
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.collect

class ContentViewModel : ViewModel() {
    //TODO: Add isRealtime Boolean for paid feature.
    var contentPlaying = Content()
    val feedViewState: LiveData<FeedViewState> get() = _feedViewState
    val playerViewState: LiveData<PlayerViewState> get() = _playerViewState
    val viewEffect: LiveData<ContentEffects> get() = _viewEffect
    private val LOG_TAG = ContentViewModel::class.java.simpleName
    private val _feedViewState = MutableLiveData<FeedViewState>()
    private val _playerViewState = MutableLiveData<PlayerViewState>()
    private val _viewEffect = MutableLiveData<ContentEffects>()
    private val contentLoadingSet = hashSetOf<String>()

    fun processEvent(event: ContentViewEvents) {
        when (event) {
            is FeedLoad -> {
                _feedViewState.value = FeedViewState(
                        feedType = event.feedType,
                        timeframe = event.timeframe,
                        toolbar = setToolbar(event.feedType),
                        contentList = getContentList(event, event.feedType, event.isRealtime,
                                getTimeframe(event.timeframe)))
                _viewEffect.value = ContentEffects(updateAds = liveData {
                    emit(Event(UpdateAdsEffect()))
                })
            }
            is FeedLoadComplete -> _viewEffect.send(ScreenEmptyEffect(!event.hasContent))
            is AudioPlayerLoad -> _playerViewState.value = PlayerViewState(
                    getAudioPlayer(event.contentId, event.filePath, event.previewImageUrl))
            is SwipeToRefresh ->
                _feedViewState.value = _feedViewState.value?.copy(contentList = getContentList(
                        event = event,
                        feedType = event.feedType,
                        isRealtime = event.isRealtime,
                        timeframe = getTimeframe(event.timeframe)))
            is ContentSelected -> {
                val contentSelected = ContentSelected(event.position, event.content)
                when (contentSelected.content.contentType) {
                    ARTICLE -> _feedViewState.value = _feedViewState.value?.copy(contentToPlay =
                    liveData {
                        getAudiocast(contentSelected).collect { lce ->
                            when (lce) {
                                is Loading -> {
                                    setContentLoadingStatus(contentSelected.content.id, VISIBLE)
                                    _viewEffect.send(NotifyItemChangedEffect(contentSelected.position))
                                    emit(Event(null))
                                }
                                is Lce.Content -> {
                                    setContentLoadingStatus(contentSelected.content.id, GONE)
                                    _viewEffect.send(NotifyItemChangedEffect(contentSelected.position))
                                    emit(Event(lce.packet))
                                }
                                is Error -> {
                                    setContentLoadingStatus(contentSelected.content.id, GONE)
                                    _viewEffect.send(NotifyItemChangedEffect(contentSelected.position))
                                    _viewEffect.send(SnackBarEffect(
                                            if (lce.packet.filePath.equals(TTS_CHAR_LIMIT_ERROR))
                                                TTS_CHAR_LIMIT_ERROR_MESSAGE
                                            else CONTENT_PLAY_ERROR))
                                    emit(Event(null))
                                }
                            }
                        }
                    })
                    YOUTUBE -> {
                        setContentLoadingStatus(contentSelected.content.id, View.GONE)
                        _viewEffect.send(NotifyItemChangedEffect(contentSelected.position))
                        _feedViewState.value = _feedViewState.value?.copy(
                                contentToPlay = liveData<Event<ContentToPlay?>> {
                                    emit(Event(ContentToPlay(contentSelected.position,
                                            contentSelected.content, "", "")))
                                })
                    }
                    NONE -> throw IllegalArgumentException("contentType expected, contentType is 'NONE'")
                }
            }
            is ContentSwipeDrawed -> _viewEffect.send(EnableSwipeToRefreshEffect(false))
            is ContentSwiped -> _viewEffect.send(ContentSwipedEffect(
                    event.feedType, event.actionType, event.position))
            is ContentLabeled -> _feedViewState.value = _feedViewState.value?.copy(contentLabeled = liveData {
                if (event.user != null && !event.user.isAnonymous) {
                    editContentLabels(
                            feedType = event.feedType,
                            actionType = event.actionType,
                            content = event.content,
                            user = event.user,
                            position = event.position).collect { lce ->
                        when (lce) {
                            is Lce.Content -> {
                                if (event.feedType == MAIN) {
                                    labelContentFirebaseAnalytics(event.content!!)
                                    //TODO - Move to Cloud Function. Use with WorkManager.
                                    // Return error in ContentLabeled.
                                    updateActionAnalytics(event.actionType, event.content, event.user)
                                    if (event.isMainFeedEmptied)
                                        updateFeedEmptiedActionsAndAnalytics(event.user.uid)
                                }
                                _viewEffect.send(NotifyItemChangedEffect(event.position))
                                emit(Event(app.coinverse.content.models.ContentLabeled(event.position, "")))
                            }
                            is Error -> {
                                _viewEffect.send(SnackBarEffect(CONTENT_LABEL_ERROR))
                                Crashlytics.log(Log.ERROR, LOG_TAG, lce.packet.errorMessage)
                                emit(Event(null))
                            }
                        }
                    }
                } else {
                    _viewEffect.send(NotifyItemChangedEffect(event.position))
                    _viewEffect.send(SignInEffect(true))
                    emit(Event(null))
                }
            })
            is ContentShared -> _viewEffect.send(ShareContentIntentEffect(getContent(event.content.id)))
            is ContentSourceOpened -> _viewEffect.send(OpenContentSourceIntentEffect(event.url))
            is UpdateAds -> _viewEffect.send(UpdateAdsEffect())
        }
    }

    private fun setToolbar(feedType: FeedType) = ToolbarState(
            when (feedType) {
                MAIN -> GONE
                SAVED, DISMISSED -> VISIBLE
            },
            when (feedType) {
                MAIN -> app_name
                SAVED -> saved
                DISMISSED -> dismissed
            },
            when (feedType) {
                SAVED, MAIN -> false
                DISMISSED -> true
            }
    )

    private fun getContentList(event: ContentViewEvents, feedType: FeedType,
                               isRealtime: Boolean, timeframe: Timestamp) = liveData {
        if (feedType == MAIN) getMainFeedList(isRealtime, timeframe).collect { lce ->
            when (lce) {
                is Loading -> {
                    if (event is SwipeToRefresh)
                        _viewEffect.send(SwipeToRefreshEffect(true))
                    emitSource(queryMainContentList(timeframe))
                }
                is Lce.Content -> {
                    if (event is SwipeToRefresh)
                        _viewEffect.send(SwipeToRefreshEffect(false))
                    emitSource(lce.packet.pagedList!!)
                }
                is Error -> {
                    Crashlytics.log(Log.ERROR, LOG_TAG, lce.packet.errorMessage)
                    if (event is SwipeToRefresh)
                        _viewEffect.send(SwipeToRefreshEffect(false))
                    _viewEffect.send(SnackBarEffect(
                            if (event is FeedLoad) CONTENT_REQUEST_NETWORK_ERROR
                            else CONTENT_REQUEST_SWIPE_TO_REFRESH_ERROR))
                    emitSource(queryMainContentList(timeframe))
                }
            }
        } else queryLabeledContentList(feedType).collect { pagedList ->
            _viewEffect.send(ScreenEmptyEffect(pagedList.isEmpty()))
            emit(pagedList)
        }
    }

    // TODO - Refactor MediatorLiveData to Coroutine Flow - https://kotlinlang.org/docs/reference/coroutines/flow.html
    /**
     * Get audiocast player for PlayerNotificationManager in [AudioService].
     *
     * @param contentId String ID of content
     * @param filePath String location in Google Cloud Storage
     * @param imageUrl String preview image of content
     * @return MediatorLiveData<Event<ContentPlayer>> audio player
     */
    private fun getAudioPlayer(contentId: String, filePath: String, imageUrl: String) =
            getContentUri(contentId, filePath).combinePlayerData(bitmapToByteArray(imageUrl)) { a, b ->
                Event(ContentPlayer(
                        uri = a.peekEvent().uri,
                        image = b.peekEvent().image,
                        errorMessage = getAudioPlayerErrors(a, b)))
            }

    /**
     * Sets the value to the result of a function that is called when both `LiveData`s have data
     * or when they receive updates after that.
     *
     * @receiver LiveData<A> the result of [getContentUri]
     * @param other LiveData<B> the result of [bitmapToByteArray]
     * @param onChange Function2<A, B, T> retrieves value from [getContentUri] and
     * [bitmapToByteArray]
     * @return MediatorLiveData<T> content mp3 file and formatted preview image
     */
    private fun <T, A, B> LiveData<A>.combinePlayerData(other: LiveData<B>, onChange: (A, B) -> T) =
            MediatorLiveData<T>().also { result ->
                var source1emitted = false
                var source2emitted = false
                val mergeF = {
                    val source1Value = this.value
                    val source2Value = other.value
                    if (source1emitted && source2emitted)
                        result.value = onChange.invoke(source1Value!!, source2Value!!)
                }
                result.addSource(this) {
                    source1emitted = true
                    mergeF.invoke()
                }
                result.addSource(other) {
                    source2emitted = true
                    mergeF.invoke()
                }
            }

    /**
     * Retrieves content mp3 file from Google Cloud Storage
     *
     * @param contentId String ID of content
     * @param filePath String Google Cloud Storage location
     * @return LiveData<(Event<ContentUri>?)> content mp3 file
     */
    private fun getContentUri(contentId: String, filePath: String) = liveData {
        ContentRepository.getContentUri(contentId, filePath).collect { lce ->
            when (lce) {
                is Lce.Content -> emit(Event(ContentUri(lce.packet.uri, "")))
                is Error -> {
                    Crashlytics.log(Log.ERROR, LOG_TAG, lce.packet.errorMessage)
                    emit(Event(ContentUri(lce.packet.uri, lce.packet.errorMessage)))
                }
            }
        }
    }

    /**
     * Converts content image Bitmap preview to ByteArray
     *
     * @param url String content image preview url
     * @return LiveData<(Event<ContentBitmap>?)> content preview image as ByteArray
     */
    private fun bitmapToByteArray(url: String) = liveData {
        ContentRepository.bitmapToByteArray(url).collect { lce ->
            when (lce) {
                is Lce.Content -> emit(Event(ContentBitmap(lce.packet.image, lce.packet.errorMessage)))
                is Error -> {
                    Crashlytics.log(Log.WARN, LOG_TAG,
                            "bitmapToByteArray error or null - ${lce.packet.errorMessage}")
                    emit(Event(ContentBitmap(lce.packet.image, lce.packet.errorMessage)))
                }
            }
        }
    }

    /**
     * Collects and combines errors from building the audio player.
     *
     * @param a Event<ContentUri> content mp3 file
     * @param b Event<ContentBitmap> content preview image
     * @return String combined error message
     */
    private fun getAudioPlayerErrors(a: Event<ContentUri>, b: Event<ContentBitmap>) =
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