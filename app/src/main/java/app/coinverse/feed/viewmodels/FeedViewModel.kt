package app.coinverse.feed.viewmodels

import android.util.Log
import android.view.View
import android.widget.ProgressBar.GONE
import android.widget.ProgressBar.VISIBLE
import androidx.lifecycle.*
import app.coinverse.R.string.*
import app.coinverse.analytics.Analytics.labelContentFirebaseAnalytics
import app.coinverse.analytics.Analytics.updateActionAnalytics
import app.coinverse.analytics.Analytics.updateFeedEmptiedActionsAndAnalytics
import app.coinverse.feed.FeedRepository.editContentLabels
import app.coinverse.feed.FeedRepository.getAudiocast
import app.coinverse.feed.FeedRepository.getContent
import app.coinverse.feed.FeedRepository.getMainFeedList
import app.coinverse.feed.FeedRepository.queryLabeledContentList
import app.coinverse.feed.FeedRepository.queryMainContentList
import app.coinverse.feed.models.*
import app.coinverse.feed.models.FeedViewEffectType.*
import app.coinverse.feed.models.FeedViewEventType.*
import app.coinverse.feed.models.FeedViewEventType.ContentLabeled
import app.coinverse.feed.views.FeedFragment
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
import kotlinx.coroutines.flow.collect

class FeedViewModel(private val state: SavedStateHandle,
                    private val feedType: FeedType,
                    private val timeframe: Timeframe,
                    private val isRealtime: Boolean) : ViewModel(), FeedViewEvents {
    //TODO: Add isRealtime Boolean for paid feature.
    val viewState: LiveData<FeedViewState> get() = _viewState
    val viewEffect: LiveData<FeedViewEffects> get() = _viewEffect
    val feedPosition = state.get<Int>(FEED_POSITION_KEY).let { position ->
        if (position == null) 0 else position
    }
    private val LOG_TAG = FeedViewModel::class.java.simpleName
    private val _viewState = MutableLiveData<FeedViewState>()
    private val _viewEffect = MutableLiveData<FeedViewEffects>()
    private val contentLoadingSet = hashSetOf<String>()

    init {
        _viewState.value = FeedViewState(
                feedType = feedType,
                timeframe = timeframe,
                toolbar = setToolbar(feedType),
                contentList = getContentList(FeedLoad(feedType, timeframe, isRealtime)))
        _viewEffect.value = FeedViewEffects(updateAds = liveData { emit(Event(UpdateAdsEffect())) })
    }

    /** View events */
    fun attachEvents(fragment: FeedFragment) {
        fragment.initEvents(this)
    }

    override fun feedLoadComplete(event: FeedLoadComplete) {
        _viewEffect.send(ScreenEmptyEffect(!event.hasContent))
    }

    override fun swipeToRefresh(event: SwipeToRefresh) {
        _viewState.value = _viewState.value?.copy(contentList = getContentList(event))
    }

    override fun contentSelected(event: ContentSelected) {
        val contentSelected = ContentSelected(event.position, event.content)
        when (contentSelected.content.contentType) {
            ARTICLE -> _viewState.value = _viewState.value?.copy(contentToPlay = liveData {
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
                _viewState.value = _viewState.value?.copy(
                        contentToPlay = liveData<Event<ContentToPlay?>> {
                            emit(Event(ContentToPlay(contentSelected.position,
                                    contentSelected.content, "", "")))
                        })
            }
            NONE -> throw IllegalArgumentException("contentType expected, contentType is 'NONE'")
        }
    }

    override fun contentSwipeDrawed(event: ContentSwipeDrawed) {
        _viewEffect.send(EnableSwipeToRefreshEffect(false))
    }

    override fun contentSwiped(event: ContentSwiped) {
        _viewEffect.send(ContentSwipedEffect(event.feedType, event.actionType, event.position))
    }

    override fun contentLabeled(event: ContentLabeled) {
        _viewState.value = _viewState.value?.copy(contentLabeled = liveData {
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
                                //TODO: Move to Cloud Function.
                                // Use with WorkManager.
                                // Return error in ContentLabeled.
                                updateActionAnalytics(event.actionType, event.content, event.user)
                                if (event.isMainFeedEmptied)
                                    updateFeedEmptiedActionsAndAnalytics(event.user.uid)
                            }
                            _viewEffect.send(NotifyItemChangedEffect(event.position))
                            emit(Event(app.coinverse.feed.models.ContentLabeled(event.position, "")))
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
    }

    override fun contentShared(event: ContentShared) {
        _viewEffect.send(ShareContentIntentEffect(getContent(event.content.id)))
    }

    override fun contentSourceOpened(event: ContentSourceOpened) {
        _viewEffect.send(OpenContentSourceIntentEffect(event.url))
    }

    override fun updateAds(event: UpdateAds) {
        _viewEffect.send(UpdateAdsEffect())
    }

    fun saveFeedPosition(position: Int) {
        state.set(FEED_POSITION_KEY, position)
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

    private fun getContentList(event: FeedViewEventType) = liveData {
        val timeframe =
                if (event is FeedLoad) getTimeframe(event.timeframe)
                else if (event is SwipeToRefresh) getTimeframe(event.timeframe)
                else null
        if (feedType == MAIN) getMainFeedList(isRealtime, timeframe!!).collect { lce ->
            when (lce) {
                is Loading -> {
                    if (event is SwipeToRefresh) _viewEffect.send(SwipeToRefreshEffect(true))
                    emitSource(queryMainContentList(timeframe))
                }
                is Lce.Content -> {
                    if (event is SwipeToRefresh) _viewEffect.send(SwipeToRefreshEffect(false))
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

    fun getContentLoadingStatus(contentId: String?) =
            if (contentLoadingSet.contains(contentId)) VISIBLE else GONE

    private fun setContentLoadingStatus(contentId: String, visibility: Int) {
        if (visibility == VISIBLE) contentLoadingSet.add(contentId)
        else contentLoadingSet.remove(contentId)
    }
}