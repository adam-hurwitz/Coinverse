package app.coinverse.feed.viewmodel

import android.util.Log.ERROR
import android.view.View
import android.widget.ProgressBar.GONE
import android.widget.ProgressBar.VISIBLE
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.coinverse.R.string.*
import app.coinverse.analytics.Analytics
import app.coinverse.feed.FeedFragment
import app.coinverse.feed.FeedRepository
import app.coinverse.feed.models.*
import app.coinverse.feed.models.FeedViewEffectType.*
import app.coinverse.feed.models.FeedViewEventType.*
import app.coinverse.utils.*
import app.coinverse.utils.ContentType.*
import app.coinverse.utils.FeedType.*
import app.coinverse.utils.Status.LOADING
import app.coinverse.utils.Status.SUCCESS
import com.crashlytics.android.Crashlytics
import com.google.firebase.Timestamp
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class FeedViewModel(private val repository: FeedRepository,
                    private val analytics: Analytics,
                    private val feedType: FeedType,
                    private val timeframe: Timeframe,
                    private val isRealtime: Boolean) : ViewModel(), FeedViewEvents {
    private val LOG_TAG = FeedViewModel::class.java.simpleName

    private val _state = _FeedViewState(feedType, setToolbar(feedType))
    val state = FeedViewState(_state)
    private val _effects = _FeedViewEffects()
    val effects = FeedViewEffects(_effects)

    init {
        getFeed(FeedLoad(feedType, timeframe, isRealtime))
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
        getFeed(SwipeToRefresh(feedType, timeframe, isRealtime))
    }

    @ExperimentalCoroutinesApi
    override fun contentSelected(event: ContentSelected) {
        val contentSelected = ContentSelected(event.content, event.position)
        when (contentSelected.content.contentType) {
            ARTICLE -> repository.getAudiocast(contentSelected).onEach { resource ->
                when (resource.status) {
                    LOADING -> {
                        setContentLoadingStatus(contentSelected.content.id, VISIBLE)
                        _effects._notifyItemChanged.value = NotifyItemChangedEffect(contentSelected.position)
                    }
                    SUCCESS -> {
                        setContentLoadingStatus(contentSelected.content.id, GONE)
                        _effects._notifyItemChanged.value = NotifyItemChangedEffect(contentSelected.position)
                        _state._contentToPlay.value = resource.data
                    }
                    Status.ERROR -> {
                        setContentLoadingStatus(contentSelected.content.id, GONE)
                        _effects._notifyItemChanged.value = NotifyItemChangedEffect(contentSelected.position)
                        _effects._snackBar.value = SnackBarEffect(
                                if (resource.message.equals(TTS_CHAR_LIMIT_ERROR))
                                    TTS_CHAR_LIMIT_ERROR_MESSAGE
                                else CONTENT_PLAY_ERROR)
                    }
                }
            }.launchIn(viewModelScope)
            YOUTUBE -> {
                setContentLoadingStatus(contentSelected.content.id, View.GONE)
                _effects._notifyItemChanged.value = NotifyItemChangedEffect(contentSelected.position)
                _state._contentToPlay.value =
                        ContentToPlay(contentSelected.position, contentSelected.content, "")
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

    @ExperimentalCoroutinesApi
    override fun contentLabeled(event: ContentLabeled) {
        if (event.user != null && !event.user.isAnonymous) {
            repository.editContentLabels(
                    feedType = event.feedType,
                    actionType = event.actionType,
                    content = event.content,
                    user = event.user,
                    position = event.position).onEach { resource ->
                when (resource.status) {
                    SUCCESS -> {
                        if (event.feedType == MAIN) {
                            analytics.labelContentFirebaseAnalytics(event.content!!)
                            //TODO: Move to Cloud Function.
                            // Use with WorkManager.
                            // Return error in ContentLabeled.
                            analytics.updateActionAnalytics(event.actionType, event.content, event.user)
                            if (event.isMainFeedEmptied)
                                analytics.updateFeedEmptiedActionsAndAnalytics(event.user.uid)
                        }
                        _effects._notifyItemChanged.value = NotifyItemChangedEffect(event.position)
                        _state._contentLabeledPosition.value = event.position
                    }
                    Status.ERROR -> {
                        _effects._snackBar.value = SnackBarEffect(CONTENT_LABEL_ERROR)
                        Crashlytics.log(ERROR, LOG_TAG, resource.message)
                        _state._contentLabeledPosition.value = null
                    }
                }
            }.launchIn(viewModelScope)
        } else {
            _effects._notifyItemChanged.value = NotifyItemChangedEffect(event.position)
            _effects._signIn.value = SignInEffect(true)
        }
    }

    override fun contentShared(event: ContentShared) {
        _effects._shareContentIntent.value =
                ShareContentIntentEffect(repository.getContent(event.content.id))
    }

    override fun contentSourceOpened(event: ContentSourceOpened) {
        _effects._openContentSourceIntent.value = OpenContentSourceIntentEffect(event.url)
    }

    override fun updateAds(event: UpdateAds) {
        _effects._updateAds.value = UpdateAdsEffect()
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

    @ExperimentalCoroutinesApi
    private fun getFeed(event: FeedViewEventType) {
        val timeframe =
                if (event is FeedLoad) getTimeframe(event.timeframe)
                else if (event is SwipeToRefresh) getTimeframe(event.timeframe)
                else null
        if (feedType == MAIN)
            repository.getMainFeedNetwork(isRealtime, timeframe!!).onEach { resource ->
                when (resource.status) {
                    LOADING -> {
                        if (event is SwipeToRefresh)
                            _effects._swipeToRefresh.value = SwipeToRefreshEffect(true)
                        getMainFeedLocal(timeframe)
                    }
                    SUCCESS -> {
                        if (event is SwipeToRefresh)
                            _effects._swipeToRefresh.value = SwipeToRefreshEffect(false)
                        resource.data?.collect { pagedList ->
                            _state._feedList.value = pagedList
                        }
                    }
                    Status.ERROR -> {
                        Crashlytics.log(ERROR, LOG_TAG, resource.message)
                        if (event is SwipeToRefresh)
                            _effects._swipeToRefresh.value = SwipeToRefreshEffect(false)
                        _effects._snackBar.value = SnackBarEffect(
                                if (event is FeedLoad) CONTENT_REQUEST_NETWORK_ERROR
                                else CONTENT_REQUEST_SWIPE_TO_REFRESH_ERROR)
                        getMainFeedLocal(timeframe)
                    }
                }
            }.launchIn(viewModelScope)
        else
            repository.getLabeledFeedRoom(feedType).onEach { pagedList ->
                _effects._screenEmpty.value = ScreenEmptyEffect(pagedList.isEmpty())
                _state._feedList.value = pagedList
            }.launchIn(viewModelScope)
    }

    @ExperimentalCoroutinesApi
    private fun getMainFeedLocal(timeframe: Timestamp) {
        repository.getMainFeedRoom(timeframe).onEach { pagedList ->
            _state._feedList.value = pagedList
        }.launchIn(viewModelScope)

    }

    private fun setContentLoadingStatus(contentId: String, visibility: Int) {
        if (visibility == VISIBLE) _effects._contentLoadingIds.add(contentId)
        else _effects._contentLoadingIds.remove(contentId)
    }
}