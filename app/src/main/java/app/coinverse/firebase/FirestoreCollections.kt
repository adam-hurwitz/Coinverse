package app.coinverse.firebase

import app.coinverse.BuildConfig
import app.coinverse.utils.BuildType.open
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfig.getInstance

fun firebaseApp(isOpenShared: Boolean) =
        if (BuildConfig.BUILD_TYPE == open.name && isOpenShared) FirebaseApp.getInstance(open.name)
        else FirebaseApp.getInstance()

// Price
val contentEthBtcCollection = FirebaseFirestore.getInstance(firebaseApp(true))
        .collection(FirebaseRemoteConfig.getInstance().getString("price_eth_btc_collection"))
// Content
val contentEnCollection = FirebaseFirestore.getInstance(firebaseApp(true))
        .collection(FirebaseRemoteConfig.getInstance().getString("content_en_collection"))
// User
val USERS_DOCUMENT = getInstance().getString("users_document")
val usersDocument = FirebaseFirestore.getInstance().document(USERS_DOCUMENT)
val ACCOUNT_DOCUMENT = getInstance().getString("account_document")
val COLLECTIONS_DOCUMENT = getInstance().getString("collections_document")
val SAVE_COLLECTION = getInstance().getString("save_collection")
val DISMISS_COLLECTION = getInstance().getString("dismiss_collection")
val ACTIONS_DOCUMENT = getInstance().getString("actions_document")
val START_ACTION_COLLECTION = getInstance().getString("start_actions")
val CONSUME_ACTION_COLLECTION = getInstance().getString("consume_actions")
val FINISH_ACTION_COLLECTION = getInstance().getString("finish_actions")
val SAVE_ACTION_COLLECTION = getInstance().getString("save_actions")
val SHARE_ACTION_COLLECTION = getInstance().getString("share_actions")
val DISMISS_ACTION_COLLECTION = getInstance().getString("dismiss_actions")
// Counters
val VIEW_COUNT = getInstance().getString("view_count")
val CONSUME_COUNT = getInstance().getString("consume_count")
val START_COUNT = getInstance().getString("start_count")
val FINISH_COUNT = getInstance().getString("finish_count")
val ORGANIZE_COUNT = getInstance().getString("organize_count")
val SHARE_COUNT = getInstance().getString("share_count")
val CLEAR_FEED_COUNT = getInstance().getString("clear_feed_count")
val DISMISS_COUNT = getInstance().getString("dismiss_count")
val MESSAGE_CENTER_UNREAD_COUNT = getInstance().getString("message_center_unread_count")
// Quality scores.
val INVALID_SCORE = getInstance().getDouble("invalid_score")
val SAVE_SCORE = getInstance().getDouble("save_score")
val START_SCORE = getInstance().getDouble("start_score")
val CONSUME_SCORE = getInstance().getDouble("consume_score")
val FINISH_SCORE = getInstance().getDouble("finish_score")
val SHARE_SCORE = getInstance().getDouble("share_score")
val DISMISS_SCORE = getInstance().getDouble("dismiss_score") // Not opened.