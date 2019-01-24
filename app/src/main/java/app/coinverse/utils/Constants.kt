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
const val USER_KEY = "userKey"

// User
val USERS_COLLECTION = FirebaseRemoteConfig.getInstance().getString("users_collection")
val SIGN_OUT_ON_BACK_PRESS_DELAY_IN_MILLIS = FirebaseRemoteConfig.getInstance().getLong("sign_out_on_back_press_delay_in_millis")
val SAVED_BOTTOM_SHEET_PEEK_HEIGHT = FirebaseRemoteConfig.getInstance().getDouble("saved_bottom_sheet_peek_height").toInt()
val SAVE_COLLECTION = FirebaseRemoteConfig.getInstance().getString("save_collection")
val DISMISS_COLLECTION = FirebaseRemoteConfig.getInstance().getString("dismiss_collection")
val DEFAULT_LAT = FirebaseRemoteConfig.getInstance().getDouble("default_lat")
val DEFAULT_LNG = FirebaseRemoteConfig.getInstance().getString("default_lng")

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
val CONTENT_DIALOG_PORTRAIT_HEIGHT_DIVISOR = FirebaseRemoteConfig.getInstance().getDouble("content_dialog_portrait_height_divisor").toInt()
val CONTENT_DIALOG_LANDSCAPE_WIDTH_DIVISOR = FirebaseRemoteConfig.getInstance().getDouble("youtube_landscape_width_divisor")
val CONTENT_DIALOG_LANDSCAPE_HEIGHT_DIVISOR = FirebaseRemoteConfig.getInstance().getDouble("content_dialog_landscape_height_divisor")
val CONTENT_FEED_VISIBILITY_DELAY = FirebaseRemoteConfig.getInstance().getLong("content_feed_visibility_delay")
val CONSUME_THRESHOLD = FirebaseRemoteConfig.getInstance().getDouble("consume_threshold")
val FINISH_THRESHOLD = FirebaseRemoteConfig.getInstance().getDouble("finish_threshold")
val DEBUG_ENABLED_PARAM = FirebaseRemoteConfig.getInstance().getString("debug_enabled_param")
val CONTENT_ID_PARAM = FirebaseRemoteConfig.getInstance().getString("content_id_param")
val CONTENT_TITLE_PARAM = FirebaseRemoteConfig.getInstance().getString("content_title_param")
val CONTENT_PREVIEW_IMAGE_PARAM = FirebaseRemoteConfig.getInstance().getString("content_image_param")
val FILE_PATH_PARAM = FirebaseRemoteConfig.getInstance().getString("file_path_param")
val ERROR_PATH_PARAM = FirebaseRemoteConfig.getInstance().getString("error_path_param")
val GET_AUDIOCAST_FUNCTION = FirebaseRemoteConfig.getInstance().getString("get_audiocast_function")
val TTS_CHAR_LIMIT_ERROR = FirebaseRemoteConfig.getInstance().getString("tts_char_limit_error")
val TTS_CHAR_LIMIT_MESSAGE = FirebaseRemoteConfig.getInstance().getString("tts_char_limit_message")
val BOTTOM_SHEET_COLLAPSE_DELAY = FirebaseRemoteConfig.getInstance().getLong("bottom_sheet_collapse_delay")
val CONTENT_SHARE_TYPE = FirebaseRemoteConfig.getInstance().getString("content_share_type")
val CONTENT_SHARE_SUBJECT_PREFFIX = FirebaseRemoteConfig.getInstance().getString("content_share_subject_prefix")
val CONTENT_SHARE_TEXT_PREFFIX = FirebaseRemoteConfig.getInstance().getString("content_share_text_prefix")
val CONTENT_SHARE_TITLE = FirebaseRemoteConfig.getInstance().getString("content_share_title_prefix")
const val RIGHT_SWIPE = 8
const val LEFT_SWIPE = 4
const val CONTENT_FEED_FRAGMENT_TAG = "contentFeedFragmentTag"
const val CONTENT_DIALOG_FRAGMENT_TAG = "contentDialogFragmentTag"
const val SAVED_CONTENT_TAG = "savedContentTag"
const val FEED_TYPE_KEY = "feedType"
const val CONTENT_KEY = "contentKey"
const val ADAPTER_POSITION_KEY = 122218133
const val CONTENT_RECYCLER_VIEW_STATE = "contentRecyclerViewState"
const val CONTENT_RECYCLER_VIEW_POSITION = "contentRecyclerViewPosition"
const val MEDIA_IS_PLAYING_KEY = "mediaIsPlayingKey"
const val MEDIA_CURRENT_TIME_KEY = "mediaCurrentTimeKey"
const val KEY_TRACK_SELECTOR_PARAMETERS = "track_selector_parameters"
const val KEY_WINDOW = "window"
const val KEY_POSITION = "position"
const val KEY_AUTO_PLAY = "auto_play"
const val KEY_SEEK_TO_POSITION_MILLIS = "seek_to_position_millis"
const val CLICK_SPAM_PREVENTION_THRESHOLD = 1250

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
val AUDIOCAST_VIEW = FirebaseRemoteConfig.getInstance().getString("audiocast_view")
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

// Utils
const val LOG_TAG_UTILS = "Utils.kt"
const val REQUEST_CODE_LOC_PERMISSION = 1909

// Ads
val AD_UNIT_ID = FirebaseRemoteConfig.getInstance().getString("ad_unit_id")
val MOPUB_KEYWORDS = FirebaseRemoteConfig.getInstance().getString("mopub_keywords")