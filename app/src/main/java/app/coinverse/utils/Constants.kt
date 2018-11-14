package app.coinverse.utils

import com.google.firebase.remoteconfig.FirebaseRemoteConfig

// Firebase
const val RC_SIGN_IN = 123
const val TIMESTAMP = "timestamp"
const val QUALITY_SCORE = "qualityScore"

// Room
const val DATABASE_NAME = "coinverse-db"

// Home
val MESSAGE_CENTER_COLLECTION = FirebaseRemoteConfig.getInstance().getString("message_center_collection")

const val PRICEGRAPH_FRAGMENT_TAG = "priceGraphFragmentTag"
const val SIGNIN_DIALOG_FRAGMENT_TAG = "signinDialogFragmentTag"
const val APP_BAR_EXPANDED_KEY = "appBarCollapsedKey"
const val SAVED_CONTENT_EXPANDED_KEY = "savedContentExpandedKey"

// User
val USERS_COLLECTION = FirebaseRemoteConfig.getInstance().getString("users_collection")
val SIGN_OUT_ON_BACK_PRESS_DELAY_IN_MILLIS = FirebaseRemoteConfig.getInstance().getLong("sign_out_on_back_press_delay_in_millis")
val SAVED_BOTTOM_SHEET_PEEK_HEIGHT = FirebaseRemoteConfig.getInstance().getDouble("saved_bottom_sheet_peek_height").toInt()
val SAVE_COLLECTION = FirebaseRemoteConfig.getInstance().getString("save_collection")
val DISMISS_COLLECTION = FirebaseRemoteConfig.getInstance().getString("dismiss_collection")

const val SIGNIN_TYPE_KEY = "signInTypeKey"

// Content
val CONTENT_COLLECTION = FirebaseRemoteConfig.getInstance().getString("content_collection")
val FEEDS_DOC = FirebaseRemoteConfig.getInstance().getString("feeds_doc")
val EN_COLLECTION = FirebaseRemoteConfig.getInstance().getString("en_collection")
val CONTENT_IMAGE_CORNER_RADIUS = FirebaseRemoteConfig.getInstance().getDouble("content_image_corner_radius").toInt()
val CELL_CONTENT_MARGIN = FirebaseRemoteConfig.getInstance().getDouble("cell_content_margin").toInt()
val PREFETCH_DISTANCE = FirebaseRemoteConfig.getInstance().getDouble("prefetch_distance").toInt()
val SWIPE_CONTENT_Y_MARGIN_DP = FirebaseRemoteConfig.getInstance().getDouble("swipe_content_y_margin_dp").toInt()
val PAGE_SIZE = FirebaseRemoteConfig.getInstance().getDouble("page_size").toInt()
val YOUTUBE_PORTRAIT_HEIGHT_DIVISOR = FirebaseRemoteConfig.getInstance().getDouble("youtube_portrait_height_divisor").toInt()
val YOUTUBE_LANDSCAPE_WIDTH_DIVISOR = FirebaseRemoteConfig.getInstance().getDouble("youtube_landscape_width_divisor")
val YOUTUBE_LANDSCAPE_HEIGHT_DIVISOR = FirebaseRemoteConfig.getInstance().getDouble("youtube_landscape_height_divisor")
val CONTENT_FEED_VISIBILITY_DELAY = FirebaseRemoteConfig.getInstance().getLong("content_feed_visibility_delay")
val CONSUME_THRESHOLD = FirebaseRemoteConfig.getInstance().getDouble("consume_threshold")
val FINISH_THRESHOLD = FirebaseRemoteConfig.getInstance().getDouble("finish_threshold")

const val CONTENT_FEED_FRAGMENT_TAG = "contentFeedFragmentTag"
const val SAVED_CONTENT_TAG = "savedContentTag"
const val YOUTUBE_DIALOG_FRAGMENT_TAG = "youtubeDialogFragmentKey"
const val FEED_TYPE_KEY = "feedType"
const val CONTENT_KEY = "contentKey"
const val CONTENT_RECYCLER_VIEW_STATE = "contentRecyclerViewState"
const val YOUTUBE_IS_PLAYING_KEY = "youtubeIsPlayingKey"
const val YOUTUBE_CURRENT_TIME_KEY = "youtubeCurrentTimeKey"

// Price
val PRICE_COLLECTION = FirebaseRemoteConfig.getInstance().getString("price_collection")
val ETH_BTC_DOC = FirebaseRemoteConfig.getInstance().getString("eth_btc_doc")
val PRICES_COLLECTION = FirebaseRemoteConfig.getInstance().getString("prices_collection")

// Actions

// Collections
val START_ACTION_COLLECTION = FirebaseRemoteConfig.getInstance().getString("start_actions")
val CONSUME_ACTION_COLLECTION = FirebaseRemoteConfig.getInstance().getString("consume_actions")
val FINISH_ACTION_COLLECTION = FirebaseRemoteConfig.getInstance().getString("finish_actions")
val SAVE_ACTION_COLLECTION = FirebaseRemoteConfig.getInstance().getString("save_actions")
val SHARE_ACTION_COLLECTION = FirebaseRemoteConfig.getInstance().getString("share_actions")
val DISMISS_ACTION_COLLECTION = FirebaseRemoteConfig.getInstance().getString("dismiss_actions")

// Counters
val VIEW_COUNT = FirebaseRemoteConfig.getInstance().getString("view_count")
val CONSUME_COUNT = FirebaseRemoteConfig.getInstance().getString("consume_count")
val START_COUNT = FirebaseRemoteConfig.getInstance().getString("start_count")
val FINISH_COUNT = FirebaseRemoteConfig.getInstance().getString("finish_count")
val ORGANIZE_COUNT = FirebaseRemoteConfig.getInstance().getString("organize_count")
val SHARE_COUNT = FirebaseRemoteConfig.getInstance().getString("share_count")
val CLEAR_FEED_COUNT = FirebaseRemoteConfig.getInstance().getString("clear_feed_count")
val DISMISS_COUNT = FirebaseRemoteConfig.getInstance().getString("dismiss_count")
val MESSAGE_CENTER_UNREAD_COUNT = FirebaseRemoteConfig.getInstance().getString("message_center_unread_count")

// Quality scores.
val INVALID_SCORE = FirebaseRemoteConfig.getInstance().getDouble("invalid_score")
val SAVE_SCORE = FirebaseRemoteConfig.getInstance().getDouble("save_score")
val START_SCORE = FirebaseRemoteConfig.getInstance().getDouble("start_score")
val CONSUME_SCORE = FirebaseRemoteConfig.getInstance().getDouble("consume_score")
val FINISH_SCORE = FirebaseRemoteConfig.getInstance().getDouble("finish_score")
val SHARE_SCORE = FirebaseRemoteConfig.getInstance().getDouble("share_score")
val DISMISS_SCORE = FirebaseRemoteConfig.getInstance().getDouble("dismiss_score") // Not opened.

// Analytics

// Views
val YOUTUBE_VIEW = FirebaseRemoteConfig.getInstance().getString("youtube_view")
val PROFILE_VIEW = FirebaseRemoteConfig.getInstance().getString("profile_view")
// Events
val VIEW_CONTENT_EVENT = FirebaseRemoteConfig.getInstance().getString("view_content_event")
val START_CONTENT_EVENT = FirebaseRemoteConfig.getInstance().getString("start_content_event")
val CONSUME_CONTENT_EVENT = FirebaseRemoteConfig.getInstance().getString("consume_content_event")
val FINISH_CONTENT_EVENT = FirebaseRemoteConfig.getInstance().getString("finish_content_event")
val ORGANIZE_EVENT = FirebaseRemoteConfig.getInstance().getString("organize_content_event")
val SHARE_EVENT = FirebaseRemoteConfig.getInstance().getString("share_content_event")
val CLEAR_FEED_EVENT = FirebaseRemoteConfig.getInstance().getString("clear_feed_event")
val DISMISS_EVENT = FirebaseRemoteConfig.getInstance().getString("dismiss_content_event")

// Params
val USER_ID_PARAM = FirebaseRemoteConfig.getInstance().getString("user_id_param")
val QUALITY_SCORE_PARAM = FirebaseRemoteConfig.getInstance().getString("quality_score_param")
val TIMESTAMP_PARAM = FirebaseRemoteConfig.getInstance().getString("timestamp_param")
val CREATOR_PARAM = FirebaseRemoteConfig.getInstance().getString("creator_name_param")
val FEED_TYPE_PARAM = FirebaseRemoteConfig.getInstance().getString("feed_type_param")