package app.coinverse.user.viewmodel

import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import app.coinverse.user.UserRepository

class UserViewModelFactory(owner: SavedStateRegistryOwner, private val repository: UserRepository)
    : AbstractSavedStateViewModelFactory(owner, null) {
    override fun <T : ViewModel?> create(key: String, modelClass: Class<T>, state: SavedStateHandle) =
            UserViewModel(repository = repository) as T
}