package app.coinverse.priceGraph.viewmodel

import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import app.coinverse.priceGraph.PriceRepository

class PriceViewModelFactory(owner: SavedStateRegistryOwner, private val repository: PriceRepository)
    : AbstractSavedStateViewModelFactory(owner, null) {
    override fun <T : ViewModel?> create(key: String, modelClass: Class<T>, state: SavedStateHandle) =
            PriceViewModel(repository) as T
}