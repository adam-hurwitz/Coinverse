package app.coinverse.utils

object Constants {
    // Firebase
    const val RC_SIGN_IN = 123
    const val TIMESTAMP = "timestamp"
    const val QUALITY_SCORE = "qualityScore"

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
    const val VIEW_CONTENT_EVENT = "view_content"
    const val START_CONTENT_EVENT = "start_content"
    const val FINISH_CONTENT_EVENT = "finish_content"
    const val ORGANIZE_EVENT = "organize_content"
    const val SHARE_EVENT = "share_content"
    const val CLEAR_FEED_EVENT = "clear_feed"
    const val ARCHIVE_EVENT = "archive_content"
    // Params
    const val USER_ID_PARAM = "user_id"
    const val QUALITY_SCORE_PARAM = "quality_score"
    const val TIMESTAMP_PARAM = "timestamp"
    const val CREATOR_PARAM = "creator"
    const val FEED_TYPE_PARAM = "feed_type"
}