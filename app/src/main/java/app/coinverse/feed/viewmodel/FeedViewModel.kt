package app.coinverse.feed.viewmodel

import android.util.Log.ERROR
import android.view.View
import android.widget.ProgressBar.GONE
import android.widget.ProgressBar.VISIBLE
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.coinverse.R.string.app_name
import app.coinverse.R.string.dismissed
import app.coinverse.R.string.saved
import app.coinverse.analytics.Analytics
import app.coinverse.feed.FeedFragment
import app.coinverse.feed.FeedRepository
import app.coinverse.feed.models.ContentToPlay
import app.coinverse.feed.models.FeedViewEffect
import app.coinverse.feed.models.FeedViewEffectType.ContentSwipedEffect
import app.coinverse.feed.models.FeedViewEffectType.EnableSwipeToRefreshEffect
import app.coinverse.feed.models.FeedViewEffectType.NotifyItemChangedEffect
import app.coinverse.feed.models.FeedViewEffectType.OpenContentSourceIntentEffect
import app.coinverse.feed.models.FeedViewEffectType.ScreenEmptyEffect
import app.coinverse.feed.models.FeedViewEffectType.ShareContentIntentEffect
import app.coinverse.feed.models.FeedViewEffectType.SignInEffect
import app.coinverse.feed.models.FeedViewEffectType.SnackBarEffect
import app.coinverse.feed.models.FeedViewEffectType.SwipeToRefreshEffect
import app.coinverse.feed.models.FeedViewEffectType.UpdateAdsEffect
import app.coinverse.feed.models.FeedViewEvent
import app.coinverse.feed.models.FeedViewEventType
import app.coinverse.feed.models.FeedViewEventType.ContentLabeled
import app.coinverse.feed.models.FeedViewEventType.ContentSelected
import app.coinverse.feed.models.FeedViewEventType.ContentShared
import app.coinverse.feed.models.FeedViewEventType.ContentSourceOpened
import app.coinverse.feed.models.FeedViewEventType.ContentSwipeDrawed
import app.coinverse.feed.models.FeedViewEventType.ContentSwiped
import app.coinverse.feed.models.FeedViewEventType.FeedLoad
import app.coinverse.feed.models.FeedViewEventType.FeedLoadComplete
import app.coinverse.feed.models.FeedViewEventType.SwipeToRefresh
import app.coinverse.feed.models.FeedViewEventType.UpdateAds
import app.coinverse.feed.models.FeedViewState
import app.coinverse.feed.models._FeedViewEffect
import app.coinverse.feed.models._FeedViewState
import app.coinverse.utils.CONTENT_LABEL_ERROR
import app.coinverse.utils.CONTENT_PLAY_ERROR
import app.coinverse.utils.CONTENT_REQUEST_NETWORK_ERROR
import app.coinverse.utils.CONTENT_REQUEST_SWIPE_TO_REFRESH_ERROR
import app.coinverse.utils.ContentType.ARTICLE
import app.coinverse.utils.ContentType.NONE
import app.coinverse.utils.ContentType.YOUTUBE
import app.coinverse.utils.FeedType
import app.coinverse.utils.FeedType.DISMISSED
import app.coinverse.utils.FeedType.MAIN
import app.coinverse.utils.FeedType.SAVED
import app.coinverse.utils.Status
import app.coinverse.utils.Status.LOADING
import app.coinverse.utils.Status.SUCCESS
import app.coinverse.utils.TTS_CHAR_LIMIT_ERROR
import app.coinverse.utils.TTS_CHAR_LIMIT_ERROR_MESSAGE
import app.coinverse.utils.Timeframe
import app.coinverse.utils.ToolbarState
import app.coinverse.utils.getTimeframe
import com.crashlytics.android.Crashlytics
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext

class FeedViewModel(private val repository: FeedRepository,
                    private val analytics: Analytics,
                    private val feedType: FeedType,
                    private val timeframe: Timeframe,
                    private val isRealtime: Boolean) : ViewModel(), FeedViewEvent {
    private val LOG_TAG = FeedViewModel::class.java.simpleName

    private val _state = _FeedViewState(feedType, setToolbar(feedType))
    val state = FeedViewState(_state)
    private val _effect = _FeedViewEffect()
    val effect = FeedViewEffect(_effect)

    init {
        getFeed(FeedLoad(feedType, timeframe, isRealtime))
        _effect._updateAds.value = UpdateAdsEffect()
    }

    /** View events */
    fun launchViewEvents(fragment: FeedFragment) {
        fragment.attachViewEvents(this)
    }

    override fun feedLoadComplete(event: FeedLoadComplete) {
        _effect._screenEmpty.value = ScreenEmptyEffect(!event.hasContent)
    }

    override fun swipeToRefresh(event: SwipeToRefresh) {
        getFeed(SwipeToRefresh(feedType, timeframe, isRealtime))
    }

    @ExperimentalCoroutinesApi
    override fun contentSelected(event: ContentSelected) {
        val contentSelected = ContentSelected(event.content, event.position)
        when (contentSelected.content.contentType) {
            ARTICLE -> repository.getAudiocast(contentSelected).onEach { resource ->
                withContext(Dispatchers.Main) {
                    when (resource.status) {
                        LOADING -> {
                            setContentLoadingStatus(contentSelected.content.id, VISIBLE)
                            _effect._notifyItemChanged.value = NotifyItemChangedEffect(contentSelected.position)
                        }
                        SUCCESS -> {
                            setContentLoadingStatus(contentSelected.content.id, GONE)
                            _effect._notifyItemChanged.value = NotifyItemChangedEffect(contentSelected.position)
                            _state._contentToPlay.value = resource.data
                        }
                        Status.ERROR -> {
                            setContentLoadingStatus(contentSelected.content.id, GONE)
                            _effect._notifyItemChanged.value = NotifyItemChangedEffect(contentSelected.position)
                            _effect._snackBar.value = SnackBarEffect(
                                    if (resource.message.equals(TTS_CHAR_LIMIT_ERROR))
                                        TTS_CHAR_LIMIT_ERROR_MESSAGE
                                    else CONTENT_PLAY_ERROR)
                        }
                    }
                }
            }.flowOn(Dispatchers.IO).launchIn(viewModelScope)
            YOUTUBE -> {
                setContentLoadingStatus(contentSelected.content.id, View.GONE)
                _effect._notifyItemChanged.value = NotifyItemChangedEffect(contentSelected.position)
                _state._contentToPlay.value =
                        ContentToPlay(contentSelected.position, contentSelected.content, "")
            }
            NONE -> throw IllegalArgumentException("contentType expected, contentType is 'NONE'")
        }
    }

    override fun contentSwipeDrawed(event: ContentSwipeDrawed) {
        _effect._enableSwipeToRefresh.value = EnableSwipeToRefreshEffect(false)
    }

    override fun contentSwiped(event: ContentSwiped) {
        _effect._contentSwiped.value = ContentSwipedEffect(event.feedType, event.actionType, event.position)
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
                        withContext(Dispatchers.Main) {
                            _state._contentLabeledPosition.value = event.position
                        }
                    }
                    Status.ERROR -> {
                        withContext(Dispatchers.Main) {
                            _state._contentLabeledPosition.value = null
                            _effect._snackBar.value = SnackBarEffect(CONTENT_LABEL_ERROR)
                        }
                        Crashlytics.log(ERROR, LOG_TAG, resource.message)
                    }
                }
            }.flowOn(Dispatchers.IO).launchIn(viewModelScope)
        } else {
            _effect._notifyItemChanged.value = NotifyItemChangedEffect(event.position)
            _effect._signIn.value = SignInEffect(true)
        }
    }

    override fun contentShared(event: ContentShared) {
        _effect._shareContentIntent.value =
                ShareContentIntentEffect(repository.getContent(event.content.id))
    }

    override fun contentSourceOpened(event: ContentSourceOpened) {
        _effect._openContentSourceIntent.value = OpenContentSourceIntentEffect(event.url)
    }

    override fun updateAds(event: UpdateAds) {
        _effect._updateAds.value = UpdateAdsEffect()
    }

    fun getContentLoadingStatus(contentId: String?) =
            if (effect.contentLoadingIds.contains(contentId)) VISIBLE else GONE

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
                            _effect._swipeToRefresh.value = SwipeToRefreshEffect(true)
                        getMainFeedLocal(timeframe)
                    }
                    SUCCESS -> {
                        if (event is SwipeToRefresh)
                            _effect._swipeToRefresh.value = SwipeToRefreshEffect(false)
                        resource.data?.collect { pagedList ->
                            _state._feedList.value = pagedList
                        }
                    }
                    Status.ERROR -> {
                        Crashlytics.log(ERROR, LOG_TAG, resource.message)
                        if (event is SwipeToRefresh)
                            _effect._swipeToRefresh.value = SwipeToRefreshEffect(false)
                        _effect._snackBar.value = SnackBarEffect(
                                if (event is FeedLoad) CONTENT_REQUEST_NETWORK_ERROR
                                else CONTENT_REQUEST_SWIPE_TO_REFRESH_ERROR)
                        getMainFeedLocal(timeframe)
                    }
                }
            }.launchIn(viewModelScope)
        else
            repository.getLabeledFeedRoom(feedType).onEach { pagedList ->
                _effect._screenEmpty.value = ScreenEmptyEffect(pagedList.isEmpty())
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
        if (visibility == VISIBLE) _effect._contentLoadingIds.add(contentId)
        else _effect._contentLoadingIds.remove(contentId)
    }
}