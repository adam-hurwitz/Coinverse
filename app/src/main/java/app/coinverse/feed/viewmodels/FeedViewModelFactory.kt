package app.coinverse.feed.viewmodels

import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import app.coinverse.utils.FeedType
import app.coinverse.utils.Timeframe

class FeedViewModelFactory(
        private val owner: SavedStateRegistryOwner,
        private val feedType: FeedType,
        private val timeframe: Timeframe,
        private val isRealtime: Boolean) : AbstractSavedStateViewModelFactory(owner, null) {
    override fun <T : ViewModel?> create(key: String, modelClass: Class<T>, state: SavedStateHandle) =
            FeedViewModel(state, feedType, timeframe, isRealtime) as T
}