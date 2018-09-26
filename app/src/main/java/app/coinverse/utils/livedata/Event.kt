package app.coinverse.utils.livedata

/**
 * Used as a wrapper for data that is exposed via a LiveData that represents an event.
 */
open class Event<out T>(private val event: T) {

    var hasBeenHandled = false
        private set // Allow external read but not write

    /**
     * Returns the event and prevents its use again.
     */
    fun getIfEventNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            event
        }
    }

    /**
     * Returns the event, even if it's already been handled.
     */
    fun peekEvent(): T = event
}