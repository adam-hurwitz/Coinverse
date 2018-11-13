package app.coinverse.utils

// Firebase
const val RC_SIGN_IN = 123
const val TIMESTAMP = "timestamp"
const val QUALITY_SCORE = "qualityScore"

// Room
const val DATABASE_NAME = "coinverse-db"

// Home
const val ON_BACK_PRESS_DELAY_IN_MILLIS = 500L
const val SAVED_BOTTOM_SHEET_PEEK_HEIGHT = 128
const val PRICEGRAPH_FRAGMENT_TAG = "priceGraphFragmentTag"
const val SIGNIN_DIALOG_FRAGMENT_TAG = "signinDialogFragmentTag"

const val APP_BAR_EXPANDED_KEY = "appBarCollapsedKey"
const val SAVED_CONTENT_EXPANDED_KEY = "savedContentExpandedKey"

// Sign in.
const val SIGNIN_TYPE_KEY = "signInTypeKey"

// User
const val USERS = "users"
const val MESSAGE_CENTER = "messageCenter"

// Content feed.
const val CONTENT = "content"
const val FEEDS = "feeds"
const val EN = "en"
const val DISMISS_COLLECTION = "dismissCollection"
const val SAVE_COLLECTION = "saveCollection"
const val CONTENT_FEED_FRAGMENT_TAG = "contentFeedFragmentTag"
const val SAVED_CONTENT_TAG = "savedContentTag"
const val YOUTUBE_DIALOG_FRAGMENT_TAG = "youtubeDialogFragmentKey"
const val CONTENT_IMAGE_CORNER_RADIUS = 56

const val FEED_TYPE_KEY = "feedType"
const val CONTENT_KEY = "contentKey"
const val CONTENT_RECYCLER_VIEW_STATE = "contentRecyclerViewState"
const val YOUTUBE_IS_PLAYING_KEY = "youtubeIsPlayingKey"
const val YOUTUBE_CURRENT_TIME_KEY = "youtubeCurrentTimeKey"

const val CELL_CONTENT_MARGIN = 32
const val PREFETCH_DISTANCE = 24
const val SWIPE_CONTENT_Y_MARGIN_DP = 16
const val PAGE_SIZE = 12
const val YOUTUBE_PORTRAIT_HEIGHT_DIVISOR = 2
const val YOUTUBE_LANDSCAPE_WIDTH_DIVISOR = 1.2
const val YOUTUBE_LANDSCAPE_HEIGHT_DIVISOR = 1.2
const val CONTENT_FEED_VISIBILITY_DELAY = 300L

// Price
const val PRICE = "price"
const val ETH_BTC = "eth-btc"
const val PRICES = "prices"

// Actions

// Actions log.
const val START_ACTION_COLLECTION = "startActions"
const val CONSUME_ACTION_COLLECTION = "consumeActions"
const val FINISH_ACTION_COLLECTION = "finishActions"
const val SAVE_ACTION_COLLECTION = "saveActions"
const val SHARE_ACTION_COLLECTION = "shareActions"
const val DISMISS_ACTION_COLLECTION = "dismissActions"

// Actions counters.
const val VIEW_COUNT = "viewCount"
const val CONSUME_COUNT = "consumeCount"
const val START_COUNT = "startCount"
const val FINISH_COUNT = "finishCount"
const val ORGANIZE_COUNT = "organizeCount"
const val SHARE_COUNT = "shareCount"
const val CLEAR_FEED_COUNT = "clearFeedCount"
const val DISMISS_COUNT = "dismissCount"
const val MESSAGE_CENTER_UNREAD_COUNT = "messageCenterUnreadCount"

// Quality scores.
const val INVALID_SCORE = 0.0
const val SAVE_SCORE = 1.0
const val START_SCORE = 1.0
const val CONSUME_SCORE = 2.0
const val FINISH_SCORE = 3.0
const val SHARE_SCORE = 3.0
const val DISMISS_SCORE = -1.0 // Not opened.

// Analytics

// Views
const val YOUTUBE_VIEW = "YOUTUBE_VIEW"
const val PROFILE_VIEW = "PROFILE_VIEW"
// Events
const val VIEW_CONTENT_EVENT = "view_content"
const val START_CONTENT_EVENT = "start_content"
const val CONSUME_CONTENT_EVENT = "consume_content"
const val FINISH_CONTENT_EVENT = "finish_content"
const val ORGANIZE_EVENT = "organize_content"
const val SHARE_EVENT = "share_content"
const val CLEAR_FEED_EVENT = "clear_feed"
const val DISMISS_EVENT = "dismiss_content"
const val CONSUME_THRESHOLD = 0.3333333333
const val FINISH_THRESHOLD = 0.95
// Params
const val USER_ID_PARAM = "user_id"
const val QUALITY_SCORE_PARAM = "quality_score"
const val TIMESTAMP_PARAM = "timestamp"
const val CREATOR_PARAM = "creator_name"
const val FEED_TYPE_PARAM = "feed_type"