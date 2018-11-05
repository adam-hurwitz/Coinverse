package app.coinverse.firebase

import com.google.firebase.firestore.FirebaseFirestore

object FirestoreCollections {

    // Home
    val messageCenterCollection = FirebaseFirestore.getInstance().collection("messageCenter")

    // Prices
    val priceCollection = FirebaseFirestore.getInstance().collection("price")
            .document("eth-btc").collection("prices")

    // Content feed.
    const val DISMISS_COLLECTION = "dismissCollection"
    const val SAVE_COLLECTION = "saveCollection"
    val contentCollection = FirebaseFirestore.getInstance().collection("content")
            .document("feeds").collection("main")

    // Users
    val usersCollection = FirebaseFirestore.getInstance().collection("users")
    val userMessageCenter = "messageCenter"

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
}