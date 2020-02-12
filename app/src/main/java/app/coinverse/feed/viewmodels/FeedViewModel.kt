package app.coinverse.feed.viewmodels

import android.util.Log
import android.view.View
import android.widget.ProgressBar.GONE
import android.widget.ProgressBar.VISIBLE
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagedList
import app.coinverse.R.string.*
import app.coinverse.analytics.Analytics.labelContentFirebaseAnalytics
import app.coinverse.analytics.Analytics.updateActionAnalytics
import app.coinverse.analytics.Analytics.updateFeedEmptiedActionsAndAnalytics
import app.coinverse.feed.models.*
import app.coinverse.feed.models.FeedViewEffectType.*
import app.coinverse.feed.models.FeedViewEventType.*
import app.coinverse.feed.models.FeedViewEventType.ContentLabeled
import app.coinverse.feed.network.FeedRepository.editContentLabels
import app.coinverse.feed.network.FeedRepository.getAudiocast
import app.coinverse.feed.network.FeedRepository.getContent
import app.coinverse.feed.network.FeedRepository.getLabeledFeedRoom
import app.coinverse.feed.network.FeedRepository.getMainFeedNetwork
import app.coinverse.feed.network.FeedRepository.getMainFeedRoom
import app.coinverse.feed.views.FeedFragment
import app.coinverse.utils.*
import app.coinverse.utils.ContentType.*
import app.coinverse.utils.DateAndTime.getTimeframe
import app.coinverse.utils.FeedType.*
import app.coinverse.utils.models.Lce
import app.coinverse.utils.models.Lce.Error
import app.coinverse.utils.models.Lce.Loading
import app.coinverse.utils.models.ToolbarState
import com.crashlytics.android.Crashlytics
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

class FeedViewModel(private val stateHandle: SavedStateHandle,
                    private val feedType: FeedType,
                    private val timeframe: Timeframe,
                    private val isRealtime: Boolean) : ViewModel(), FeedViewEvents {
    private val LOG_TAG = FeedViewModel::class.java.simpleName

    private val _state = _FeedViewState(
            _feedViewType = feedType,
            _toolbarState = setToolbar(feedType),
            _feedPosition = stateHandle.get<Int>(FEED_POSITION_KEY).let { position ->
                if (position == null) 0 else position
            })
    val state = FeedViewState(_state)
    private val _effects = _FeedViewEffects()
    val effects = FeedViewEffects(_effects)

    init {
        viewModelScope.launch {
            getFeed(FeedLoad(feedType, timeframe, isRealtime)).collect()
        }
        _effects._updateAds.value = UpdateAdsEffect()
    }

    /** View events */
    fun attachEvents(fragment: FeedFragment) {
        fragment.initEvents(this)
    }

    override fun feedLoadComplete(event: FeedLoadComplete) {
        _effects._screenEmpty.value = ScreenEmptyEffect(!event.hasContent)
    }

    override fun swipeToRefresh(event: SwipeToRefresh) {
        viewModelScope.launch {
            getFeed(SwipeToRefresh(feedType, timeframe, isRealtime)).collect()
        }
    }

    override fun contentSelected(event: ContentSelected) {
        val contentSelected = ContentSelected(event.position, event.content)
        when (contentSelected.content.contentType) {
            ARTICLE -> viewModelScope.launch {
                getAudiocast(contentSelected).collect { lce ->
                    when (lce) {
                        is Loading -> {
                            setContentLoadingStatus(contentSelected.content.id, VISIBLE)
                            _effects._notifyItemChanged.value = NotifyItemChangedEffect(contentSelected.position)
                        }
                        is Lce.Content -> {
                            setContentLoadingStatus(contentSelected.content.id, GONE)
                            _effects._notifyItemChanged.value = NotifyItemChangedEffect(contentSelected.position)
                            _state._contentToPlay.value = lce.packet
                        }
                        is Error -> {
                            setContentLoadingStatus(contentSelected.content.id, GONE)
                            _effects._notifyItemChanged.value = NotifyItemChangedEffect(contentSelected.position)
                            _effects._snackBar.value = SnackBarEffect(
                                    if (lce.packet.filePath.equals(TTS_CHAR_LIMIT_ERROR))
                                        TTS_CHAR_LIMIT_ERROR_MESSAGE
                                    else CONTENT_PLAY_ERROR)
                            _state._contentToPlay.value = null
                        }
                    }
                }
            }
            YOUTUBE -> {
                setContentLoadingStatus(contentSelected.content.id, View.GONE)
                _effects._notifyItemChanged.value = NotifyItemChangedEffect(contentSelected.position)
                _state._contentToPlay.value =
                        ContentToPlay(contentSelected.position, contentSelected.content, "", "")
            }
            NONE -> throw IllegalArgumentException("contentType expected, contentType is 'NONE'")
        }
    }

    override fun contentSwipeDrawed(event: ContentSwipeDrawed) {
        _effects._enableSwipeToRefresh.value = EnableSwipeToRefreshEffect(false)
    }

    override fun contentSwiped(event: ContentSwiped) {
        _effects._contentSwiped.value = ContentSwipedEffect(event.feedType, event.actionType, event.position)
    }

    override fun contentLabeled(event: ContentLabeled) {
        viewModelScope.launch {
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
                            _effects._notifyItemChanged.value = NotifyItemChangedEffect(event.position)
                            _state._contentLabeled.value = app.coinverse.feed.models.ContentLabeled(event.position, "")
                        }
                        is Error -> {
                            _effects._snackBar.value = SnackBarEffect(CONTENT_LABEL_ERROR)
                            Crashlytics.log(Log.ERROR, LOG_TAG, lce.packet.errorMessage)
                            _state._contentLabeled.value = null
                        }
                    }
                }
            } else {
                _effects._notifyItemChanged.value = NotifyItemChangedEffect(event.position)
                _effects._signIn.value = SignInEffect(true)
                _state._contentLabeled.value = null
            }
        }
    }

    override fun contentShared(event: ContentShared) {
        _effects._shareContentIntent.value = ShareContentIntentEffect(getContent(event.content.id))
    }

    override fun contentSourceOpened(event: ContentSourceOpened) {
        _effects._openContentSourceIntent.value = OpenContentSourceIntentEffect(event.url)
    }

    override fun updateAds(event: UpdateAds) {
        _effects._updateAds.value = UpdateAdsEffect()
    }

    fun saveFeedPosition(position: Int) {
        stateHandle.set(FEED_POSITION_KEY, position)
    }

    fun getContentLoadingStatus(contentId: String?) =
            if (effects.contentLoadingIds.contains(contentId)) VISIBLE else GONE

    private fun setToolbar(feedType: FeedType) = ToolbarState(
            visibility = when (feedType) {
                MAIN -> GONE
                SAVED, DISMISSED -> VISIBLE
            },
            titleRes = when (feedType) {
                MAIN -> app_name
                SAVED -> saved
                DISMISSED -> dismissed
            },
            isActionBarEnabled = when (feedType) {
                SAVED, MAIN -> false
                DISMISSED -> true
            })

    //TODO: Optimize Flow scoping.
    private fun getFeed(event: FeedViewEventType) = flow<PagedList<Content>> {
        val timeframe =
                if (event is FeedLoad) getTimeframe(event.timeframe)
                else if (event is SwipeToRefresh) getTimeframe(event.timeframe)
                else null
        if (feedType == MAIN) getMainFeedNetwork(isRealtime, timeframe!!).collect { lce ->
            when (lce) {
                is Loading -> {
                    if (event is SwipeToRefresh)
                        _effects._swipeToRefresh.value = SwipeToRefreshEffect(true)
                    getMainFeedLocal(timeframe)
                }
                is Lce.Content -> {
                    if (event is SwipeToRefresh)
                        _effects._swipeToRefresh.value = SwipeToRefreshEffect(false)
                    lce.packet.pagedList!!.collect { pagedList ->
                        _state._feedList.value = pagedList
                    }
                }
                is Error -> {
                    Crashlytics.log(Log.ERROR, LOG_TAG, lce.packet.errorMessage)
                    if (event is SwipeToRefresh)
                        _effects._swipeToRefresh.value = SwipeToRefreshEffect(false)
                    _effects._snackBar.value = SnackBarEffect(
                            if (event is FeedLoad) CONTENT_REQUEST_NETWORK_ERROR
                            else CONTENT_REQUEST_SWIPE_TO_REFRESH_ERROR)
                    getMainFeedLocal(timeframe)
                }
            }
        } else getLabeledFeedRoom(feedType).collect { pagedList ->
            _effects._screenEmpty.value = ScreenEmptyEffect(pagedList.isEmpty())
            _state._feedList.value = pagedList
        }
    }

    private fun getMainFeedLocal(timeframe: Timestamp) {
        viewModelScope.launch {
            getMainFeedRoom(timeframe).collect { pagedList ->
                _state._feedList.value = pagedList
            }
        }
    }

    private fun setContentLoadingStatus(contentId: String, visibility: Int) {
        if (visibility == VISIBLE) _effects._contentLoadingIds.add(contentId)
        else _effects._contentLoadingIds.remove(contentId)
    }
}