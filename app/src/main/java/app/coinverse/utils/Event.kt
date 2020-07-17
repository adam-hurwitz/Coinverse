package app.coinverse.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform

/**
 * Returns a flow which performs the given [action] on each value of the original flow's [Event].
 */
fun <T> Flow<Event<T?>>.onEachEvent(action: suspend (T) -> Unit): Flow<T> = transform { value ->
    value.getContentIfNotHandled()?.let {
        action(it)
        return@transform emit(it)
    }
}

/**
 * Used as a wrapper for data that is exposed via an observable that represents an event.
 * Developed by Jose Alcérreca.
 * See [https://gist.github.com/JoseAlcerreca/5b661f1800e1e654f07cc54fe87441af]
 */
open class Event<out T>(private val content: T) {

    var hasBeenHandled = false
        private set // Allow external read but not write

    /**
     * Returns the content and prevents its use again.
     */
    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }

    /**
     * Returns the content, even if it's already been handled.
     */
    fun peekContent(): T = content
}