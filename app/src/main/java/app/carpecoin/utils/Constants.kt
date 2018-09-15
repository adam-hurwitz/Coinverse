package app.carpecoin.utils

object Constants {
    // Firebase
    const val RC_SIGN_IN = 123
    const val TIMESTAMP = "timestamp"

    // Home
    const val ON_BACK_PRESS_DELAY_IN_MILLIS = 500L
    const val SIGNIN_DIALOG_FRAGMENT_TAG = "signInDialogFragmentTag"

    // Content feed.
    const val CELL_CONTENT_MARGIN = 32
    const val PREFETCH_DISTANCE = 24
    const val PAGE_SIZE = 12

    // Analytics

    // Views
    const val PROFILE = "PROFILE"

    // Events
    const val OPEN_CONTENT_EVENT = "open_content"
    const val SAVED_EVENT = "saved_content"
    const val ARCHIVED_EVENT = "archived_content"
    const val EMPTIED_MAIN_FEED_EVENT = "emptied_main_feed"

    // Params
    const val QUALITY_SCORE_PARAM = "quality_score"
    const val TIMESTAMP_PARAM = "timestamp"
    const val CREATOR_PARAM = "creator"
    const val FEED_TYPE_PARAM = "feed_type"
    //TODO: Add params QualityScore, Timestamp, Creator, FeedType
}