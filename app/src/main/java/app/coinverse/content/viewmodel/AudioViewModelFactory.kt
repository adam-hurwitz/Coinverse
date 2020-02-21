package app.coinverse.content.viewmodel

import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import app.coinverse.content.ContentRepository

class AudioViewModelFactory(
        owner: SavedStateRegistryOwner, private val repository: ContentRepository)
    : AbstractSavedStateViewModelFactory(owner, null) {
    override fun <T : ViewModel?> create(key: String, modelClass: Class<T>, state: SavedStateHandle) =
            AudioViewModel(repository) as T
}