package app.coinverse.feed.viewmodel

import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import app.coinverse.analytics.Analytics
import app.coinverse.feed.FeedRepository
import app.coinverse.utils.FeedType
import app.coinverse.utils.Timeframe

class FeedViewModelFactory(
        owner: SavedStateRegistryOwner,
        private val repository: FeedRepository,
        private val analytics: Analytics,
        private val feedType: FeedType,
        private val timeframe: Timeframe,
        private val isRealtime: Boolean) : AbstractSavedStateViewModelFactory(owner, null) {
    override fun <T : ViewModel?> create(key: String, modelClass: Class<T>, state: SavedStateHandle) =
            FeedViewModel(
                    stateHandle = state,
                    repository = repository,
                    analytics = analytics,
                    feedType = feedType,
                    timeframe = timeframe,
                    isRealtime = isRealtime) as T
}