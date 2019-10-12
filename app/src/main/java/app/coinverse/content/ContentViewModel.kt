package app.coinverse.content

import android.util.Log
import android.view.View
import android.widget.ProgressBar.GONE
import android.widget.ProgressBar.VISIBLE
import androidx.lifecycle.*
import androidx.lifecycle.Transformations.switchMap
import app.coinverse.R.string.*
import app.coinverse.analytics.Analytics.labelContentFirebaseAnalytics
import app.coinverse.analytics.Analytics.updateActionAnalytics
import app.coinverse.analytics.Analytics.updateFeedEmptiedActionsAndAnalytics
import app.coinverse.content.ContentRepository.editContentLabels
import app.coinverse.content.ContentRepository.getAudiocast
import app.coinverse.content.ContentRepository.getContent
import app.coinverse.content.ContentRepository.getMainFeedList
import app.coinverse.content.ContentRepository.queryLabeledContentList
import app.coinverse.content.ContentRepository.queryMainContentList
import app.coinverse.content.models.*
import app.coinverse.content.models.ContentLabeled
import app.coinverse.content.models.ContentViewEvent.*
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

class ContentViewModel : ViewModel() {
    //TODO: Add isRealtime Boolean for paid feature.
    var contentPlaying = Content()
    val feedViewState: LiveData<FeedViewState> get() = _feedViewState
    val playerViewState: LiveData<PlayerViewState> get() = _playerViewState
    val viewEffect: LiveData<ContentViewEffects> get() = _viewEffect
    private val LOG_TAG = ContentViewModel::class.java.simpleName
    private val _feedViewState = MutableLiveData<FeedViewState>()
    private val _playerViewState = MutableLiveData<PlayerViewState>()
    private val _viewEffect = MutableLiveData<ContentViewEffects>()
    private val contentLoadingSet = hashSetOf<String>()

    fun processEvent(event: ContentViewEvent) {
        when (event) {
            is FeedLoad -> {
                _feedViewState.value = FeedViewState(
                        feedType = event.feedType,
                        timeframe = event.timeframe,
                        toolbar = setToolbar(event.feedType),
                        contentList = getContentList(event, event.feedType, event.isRealtime,
                                getTimeframe(event.timeframe)))
                _viewEffect.value = ContentViewEffects(updateAds = liveData {
                    emit(Event(UpdateAdsEffect()))
                })
            }
            is FeedLoadComplete -> _viewEffect.value = _viewEffect.value?.copy(
                    screenEmpty = liveData { emit(Event(ScreenEmptyEffect(!event.hasContent))) })
            is AudioPlayerLoad -> _playerViewState.value = PlayerViewState(
                    getAudioPlayer(event.contentId, event.filePath, event.previewImageUrl))
            is SwipeToRefresh -> _feedViewState.value = _feedViewState.value?.copy(
                    contentList = getContentList(
                            event = event,
                            feedType = event.feedType,
                            isRealtime = event.isRealtime,
                            timeframe = getTimeframe(event.timeframe)))
            is ContentSelected -> {
                val contentSelected = ContentSelected(event.position, event.content)
                when (contentSelected.content.contentType) {
                    ARTICLE -> _feedViewState.value = _feedViewState.value?.copy(contentToPlay =
                    switchMap(getAudiocast(contentSelected)) { lce ->
                        liveData {
                            when (lce) {
                                is Loading -> {
                                    setContentLoadingStatus(contentSelected.content.id, View.VISIBLE)
                                    _viewEffect.value = _viewEffect.value?.copy(
                                            notifyItemChanged = liveData {
                                                emit(Event(NotifyItemChangedEffect(contentSelected.position)))
                                            })
                                    emit(Event(null))
                                }
                                is Lce.Content -> {
                                    setContentLoadingStatus(contentSelected.content.id, View.GONE)
                                    _viewEffect.value = _viewEffect.value?.copy(
                                            notifyItemChanged = liveData {
                                                emit(Event(NotifyItemChangedEffect(contentSelected.position)))
                                            })
                                    emit(Event(lce.packet))
                                }
                                is Error -> {
                                    setContentLoadingStatus(contentSelected.content.id, View.GONE)
                                    _viewEffect.value = _viewEffect.value?.copy(
                                            notifyItemChanged = liveData {
                                                emit(Event(NotifyItemChangedEffect(contentSelected.position)))
                                            })
                                    if (lce.packet.filePath.equals(TTS_CHAR_LIMIT_ERROR))
                                        _viewEffect.value = _viewEffect.value?.copy(
                                                snackBar = liveData {
                                                    emit(Event(SnackBarEffect(TTS_CHAR_LIMIT_ERROR_MESSAGE)))
                                                })
                                    else _viewEffect.value = _viewEffect.value?.copy(
                                            snackBar = liveData {
                                                emit(Event(SnackBarEffect(CONTENT_PLAY_ERROR)))
                                            })
                                    emit(Event(null))
                                }
                            }
                        }
                    })
                    YOUTUBE -> {
                        setContentLoadingStatus(contentSelected.content.id, View.GONE)
                        _viewEffect.value = _viewEffect.value?.copy(notifyItemChanged = liveData {
                            emit(Event(NotifyItemChangedEffect(contentSelected.position)))
                        })
                        _feedViewState.value = _feedViewState.value?.copy(
                                contentToPlay = liveData<Event<ContentToPlay?>> {
                                    emit(Event(ContentToPlay(contentSelected.position,
                                            contentSelected.content, "", "")))
                                })
                    }
                    NONE -> throw IllegalArgumentException("contentType expected, contentType is 'NONE'")
                }
            }
            is ContentSwipeDrawed -> _viewEffect.value = _viewEffect.value?.copy(
                    enableSwipeToRefresh = liveData {
                        emit(Event(EnableSwipeToRefreshEffect(false)))
                    })
            is ContentSwiped -> _viewEffect.value = _viewEffect.value?.copy(contentSwiped = liveData {
                emit(Event(ContentSwipedEffect(event.feedType, event.actionType, event.position)))
            })
            is ContentViewEvent.ContentLabeled -> _feedViewState.value = _feedViewState.value?.copy(
                    contentLabeled =
                    if (event.user != null && !event.user.isAnonymous) {
                        switchMap(editContentLabels(event.feedType, event.actionType, event.content,
                                event.user, event.position)) { lce ->
                            liveData {
                                when (lce) {
                                    is Lce.Content -> {
                                        if (event.feedType == MAIN) {
                                            labelContentFirebaseAnalytics(event.content!!)
                                            //TODO: Move to Cloud Function. Use with WorkManager.
                                            // Return error in ContentLabeled.
                                            updateActionAnalytics(
                                                    event.actionType, event.content, event.user)
                                            if (event.isMainFeedEmptied)
                                                updateFeedEmptiedActionsAndAnalytics(event.user.uid)
                                        }
                                        _viewEffect.value = _viewEffect.value?.copy(
                                                notifyItemChanged = liveData {
                                                    emit(Event(NotifyItemChangedEffect(event.position)))
                                                })
                                        emit(Event(ContentLabeled(event.position, "")))
                                    }
                                    is Error -> {
                                        _viewEffect.value = _viewEffect.value?.copy(
                                                snackBar = liveData {
                                                    emit(Event(SnackBarEffect(CONTENT_LABEL_ERROR)))
                                                })
                                        Crashlytics.log(Log.ERROR, LOG_TAG, lce.packet.errorMessage)
                                        emit(Event(null))
                                    }
                                }
                            }
                        }
                    } else {
                        _viewEffect.value = _viewEffect.value?.copy(notifyItemChanged = liveData {
                            emit(Event(NotifyItemChangedEffect(event.position)))
                        })
                        _viewEffect.value = _viewEffect.value?.copy(signIn = liveData {
                            emit(Event(SignInEffect(true)))
                        })
                        liveData<Event<ContentLabeled?>> { emit(Event(null)) }
                    })
            is ContentShared -> _viewEffect.value = _viewEffect.value?.copy(
                    shareContentIntent = liveData {
                        emit(Event(ShareContentIntentEffect(getContent(event.content.id))))
                    })
            is ContentSourceOpened -> _viewEffect.value = _viewEffect.value?.copy(
                    openContentSourceIntent = liveData {
                        emit(Event(OpenContentSourceIntentEffect(event.url)))
                    })
            is UpdateAds -> _viewEffect.value = _viewEffect.value?.copy(updateAds = liveData {
                emit(Event(UpdateAdsEffect()))
            })
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

    private fun getContentList(event: ContentViewEvent, feedType: FeedType, isRealtime: Boolean,
                               timeframe: Timestamp) =
            if (feedType == MAIN)
                switchMap(getMainFeedList(isRealtime, timeframe)) { lce ->
                    when (lce) {
                        is Loading -> {
                            if (event is SwipeToRefresh)
                                _viewEffect.value = _viewEffect.value?.copy(swipeToRefresh = liveData {
                                    emit(Event(SwipeToRefreshEffect(true)))
                                })
                            switchMap(queryMainContentList(timeframe)) { pagedList ->
                                liveData { emit(pagedList) }
                            }
                        }
                        is Lce.Content -> {
                            if (event is SwipeToRefresh)
                                _viewEffect.value = _viewEffect.value?.copy(swipeToRefresh = liveData {
                                    emit(Event(SwipeToRefreshEffect(false)))
                                })
                            switchMap(lce.packet.pagedList!!) { pagedList ->
                                liveData { emit(pagedList) }
                            }
                        }
                        is Error -> {
                            Crashlytics.log(Log.ERROR, LOG_TAG, lce.packet.errorMessage)
                            if (event is SwipeToRefresh)
                                _viewEffect.value = _viewEffect.value?.copy(swipeToRefresh = liveData {
                                    emit(Event(SwipeToRefreshEffect(false)))
                                })
                            _viewEffect.value = _viewEffect.value?.copy(snackBar = liveData {
                                emit(Event(SnackBarEffect(
                                        if (event is FeedLoad) CONTENT_REQUEST_NETWORK_ERROR
                                        else CONTENT_REQUEST_SWIPE_TO_REFRESH_ERROR)))
                            })
                            switchMap(queryMainContentList(timeframe)) { pagedList ->
                                liveData { emit(pagedList) }
                            }
                        }
                    }
                }
            else switchMap(queryLabeledContentList(feedType)) { pagedList ->
                _viewEffect.value = _viewEffect.value?.copy(screenEmpty = liveData {
                    emit(Event(ScreenEmptyEffect(pagedList.isEmpty())))
                })
                liveData { emit(pagedList) }
            }

    /**
     * Get audiocast player for PlayerNotificationManager in [AudioService].
     *
     * @param contentId String ID of content
     * @param filePath String location in Google Cloud Storage
     * @param imageUrl String preview image of content
     * @return MediatorLiveData<Event<ContentPlayer>> audio player
     */
    private fun getAudioPlayer(contentId: String, filePath: String, imageUrl: String) =
            getContentUri(contentId, filePath).combinePlayerData(
                    bitmapToByteArray(imageUrl)) { a, b ->
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
    private fun getContentUri(contentId: String, filePath: String) =
            switchMap(ContentRepository.getContentUri(contentId, filePath)) { lce ->
                liveData {
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
        emitSource(switchMap(ContentRepository.bitmapToByteArray(url)) { lce ->
            liveData {
                when (lce) {
                    is Lce.Content -> emit(Event(ContentBitmap(
                            lce.packet.image, lce.packet.errorMessage)))
                    is Error -> {
                        Crashlytics.log(Log.WARN, LOG_TAG,
                                "bitmapToByteArray error or null - ${lce.packet.errorMessage}")
                        emit(Event(ContentBitmap(lce.packet.image, lce.packet.errorMessage)))
                    }
                }
            }
        })
    }

    /**
     * Collects and combines errors from building the audio player.
     *
     * @param a Event<ContentUri> content mp3 file
     * @param b Event<ContentBitmap> content preview image
     * @return String combined error message
     */
    private fun getAudioPlayerErrors(a: Event<ContentUri>, b: Event<ContentBitmap>) =
            a.peekEvent()!!.errorMessage.apply { if (this.isNotEmpty()) this }.apply {
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