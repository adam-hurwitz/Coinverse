package app.coinverse.feed

import android.content.SharedPreferences
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.lifecycle.ViewModel
import app.coinverse.R.string.app_name
import app.coinverse.R.string.dismissed
import app.coinverse.R.string.saved
import app.coinverse.analytics.Analytics
import app.coinverse.feed.data.FeedRepository
import app.coinverse.feed.state.FeedView
import app.coinverse.feed.state.FeedViewIntentType.FeedLoad
import app.coinverse.feed.state.FeedViewIntentType.LabelContent
import app.coinverse.feed.state.FeedViewIntentType.SelectContent
import app.coinverse.feed.state.FeedViewIntentType.SwipeToRefresh
import app.coinverse.feed.state.FeedViewState
import app.coinverse.feed.state.FeedViewState.ClearAdjacentAds
import app.coinverse.feed.state.FeedViewState.Feed
import app.coinverse.feed.state.FeedViewState.OpenContent
import app.coinverse.feed.state.FeedViewState.OpenContentSource
import app.coinverse.feed.state.FeedViewState.ShareContent
import app.coinverse.feed.state.FeedViewState.SignIn
import app.coinverse.feed.state.FeedViewState.SwipeContent
import app.coinverse.feed.state.FeedViewState.UpdateAds
import app.coinverse.utils.CONTENT_LABEL_ERROR
import app.coinverse.utils.CONTENT_REQUEST_NETWORK_ERROR
import app.coinverse.utils.CONTENT_REQUEST_SWIPE_TO_REFRESH_ERROR
import app.coinverse.utils.ContentType.ARTICLE
import app.coinverse.utils.ContentType.NONE
import app.coinverse.utils.ContentType.YOUTUBE
import app.coinverse.utils.ERROR
import app.coinverse.utils.FeedType
import app.coinverse.utils.FeedType.DISMISSED
import app.coinverse.utils.FeedType.MAIN
import app.coinverse.utils.FeedType.SAVED
import app.coinverse.utils.PLAYER_OPEN_STATUS_KEY
import app.coinverse.utils.Status
import app.coinverse.utils.Status.LOADING
import app.coinverse.utils.Status.SUCCESS
import app.coinverse.utils.TTS_CHAR_LIMIT_ERROR_MESSAGE
import app.coinverse.utils.Timeframe
import app.coinverse.utils.ToolbarState
import app.coinverse.utils.getTimeframe
import app.coinverse.utils.onEachEvent
import app.topcafes.dependencyinjection.getViewModelScope
import com.crashlytics.android.Crashlytics
import com.google.firebase.Timestamp
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@ExperimentalCoroutinesApi
class FeedViewModel @AssistedInject constructor(
        @Assisted private val coroutineScopeProvider: CoroutineScope?,
        @Assisted private val feedType: FeedType,
        @Assisted private val timeframe: Timeframe,
        @Assisted private val isRealtime: Boolean,
        private val sharedPreferences: SharedPreferences,
        private val repository: FeedRepository,
        private val analytics: Analytics
) : ViewModel() {

    @AssistedInject.Factory
    interface Factory {
        fun create(
                coroutineScopeProvider: CoroutineScope? = null,
                feedType: FeedType,
                timeframe: Timeframe,
                isRealtime: Boolean
        ): FeedViewModel
    }

    private val LOG_TAG = FeedViewModel::class.java.simpleName
    private val coroutineScope = getViewModelScope(coroutineScopeProvider)
    private val state = MutableStateFlow<FeedViewState?>(null)
    private val toolbarState = setToolbar(feedType)
    private var isContentSwipe = false

    fun bindIntents(view: FeedView) {
        view.initState().onEach {
            state.filterNotNull().collect {
                view.render(it)
            }
        }.launchIn(coroutineScope)

        view.loadFromNetwork().onEach {
            loadFromNetwork(FeedLoad(feedType, timeframe, isRealtime))
            state.value = UpdateAds()
        }.launchIn(coroutineScope)

        view.swipeToRefresh().onEachEvent {
            swipeToRefresh(it)
        }.launchIn(coroutineScope)

        view.selectContent().onEachEvent { selectContent ->
            selectContent(selectContent)
        }.launchIn(coroutineScope)

        view.swipeContent().onEach { swipeContent ->
            isContentSwipe = true
            if (swipeContent.isSwiped)
                state.value = SwipeContent(swipeContent.actionType, swipeContent.position)
            else state.value = FeedViewState.SwipeToRefresh(false)
        }.launchIn(coroutineScope)

        view.labelContent().onEach { labelContent ->
            labelContent(labelContent)
        }.launchIn(coroutineScope)

        view.shareContent().onEach { shareContent ->
            state.value = ShareContent(shareContent)
        }.launchIn(coroutineScope)

        view.openContentSource().onEach { url ->
            state.value = OpenContentSource(url)
        }.launchIn(coroutineScope)

        view.updateAds().onEachEvent {
            state.value = UpdateAds()
        }.launchIn(coroutineScope)
    }

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

    private fun loadFromNetwork(event: FeedLoad) {
        val timeframe = getTimeframe(event.timeframe)
        if (feedType == MAIN)
            repository.getMainFeedNetwork(isRealtime, timeframe).onEach { resource ->
                when (resource.status) {
                    LOADING -> getMainFeedLocal(timeframe, null)
                    SUCCESS -> state.value = Feed(toolbarState = toolbarState, feed = resource.data!!)
                    Status.ERROR -> {
                        Crashlytics.log(ERROR, LOG_TAG, resource.message)
                        getMainFeedLocal(timeframe, CONTENT_REQUEST_NETWORK_ERROR)
                    }
                }
            }.launchIn(coroutineScope)
        else repository.getLabelFeedRoom(feedType).onEach { pagedList ->
            state.value = Feed(toolbarState = toolbarState, feed = pagedList)
        }.launchIn(coroutineScope)
        isContentSwipe = false
    }

    private fun getMainFeedLocal(timeframe: Timestamp, error: String?) {
        repository.getMainFeedRoom(timeframe).onEach { pagedList ->
            val isContentOpen = sharedPreferences.getBoolean(PLAYER_OPEN_STATUS_KEY, false)
            if (!isContentSwipe && !isContentOpen)
                state.value = Feed(
                        toolbarState = toolbarState,
                        feed = pagedList,
                        error = if (error != null) error else null
                )
        }.launchIn(coroutineScope)
    }

    private fun swipeToRefresh(swipeToRefresh: SwipeToRefresh) {
        repository.getMainFeedNetwork(isRealtime, getTimeframe(swipeToRefresh.timeframe)).onEach {
            when (it.status) {
                LOADING -> state.value = FeedViewState.SwipeToRefresh(true)
                SUCCESS -> state.value = FeedViewState.SwipeToRefresh(false)
                Status.ERROR -> state.value = FeedViewState.SwipeToRefresh(
                        false, CONTENT_REQUEST_SWIPE_TO_REFRESH_ERROR)
            }
        }.launchIn(coroutineScope)
    }

    private fun selectContent(selectContent: SelectContent) {
        when (selectContent.content.contentType) {
            ARTICLE -> repository.getAudiocast(selectContent).onEach { resource ->
                when (resource.status) {
                    LOADING -> state.value = OpenContent(
                            isLoading = true,
                            position = selectContent.position,
                            contentId = selectContent.content.id
                    )
                    SUCCESS -> state.value = OpenContent(
                            isLoading = false,
                            position = selectContent.position,
                            contentId = selectContent.content.id,
                            content = resource.data?.content!!,
                            filePath = resource.data.filePath
                    )
                    Status.ERROR -> state.value = OpenContent(
                            isLoading = false,
                            position = selectContent.position,
                            contentId = selectContent.content.id,
                            error = TTS_CHAR_LIMIT_ERROR_MESSAGE
                    )
                }
            }.launchIn(coroutineScope)
            YOUTUBE -> state.value = OpenContent(
                    isLoading = false,
                    position = selectContent.position,
                    content = selectContent.content)
            NONE -> throw IllegalArgumentException("contentType expected, contentType is 'NONE'")
        }
    }

    private fun labelContent(labelContent: LabelContent) {
        if (labelContent.user != null && !labelContent.user.isAnonymous)
            repository.editContentLabels(
                    feedType = labelContent.feedType,
                    actionType = labelContent.actionType,
                    content = labelContent.content,
                    user = labelContent.user,
                    position = labelContent.position
            ).onEach { resource ->
                when (resource.status) {
                    SUCCESS -> {
                        if (labelContent.feedType == MAIN) {
                            analytics.labelContentFirebaseAnalytics(labelContent.content!!)
                            analytics.updateActionAnalytics(
                                    actionType = labelContent.actionType,
                                    content = labelContent.content,
                                    user = labelContent.user
                            )
                            if (labelContent.isMainFeedEmptied)
                                analytics.updateFeedEmptiedActionsAndAnalytics(labelContent.user.uid)
                        }
                        state.value = ClearAdjacentAds(labelContent.position)
                    }
                    Status.ERROR -> {
                        Crashlytics.log(ERROR, LOG_TAG, resource.message)
                        state.value = ClearAdjacentAds(
                                position = ERROR,
                                error = CONTENT_LABEL_ERROR
                        )
                    }
                }
            }.launchIn(coroutineScope)
        else {
            state.value = SignIn(position = labelContent.position)
            isContentSwipe = false
        }
    }
}
