package app.coinverse.firebase

import app.coinverse.utils.auth.Auth.CONTENT
import app.coinverse.utils.auth.Auth.PRICE
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore

object FirestoreCollections {

    // Content feed.
    const val CONTENT_COLLECTION = "content"
    const val FEEDS = "feeds"
    const val MAIN_COLLECTION = "main"
    const val ARCHIVED_COLLECTION = "archivedCollection"
    const val SAVED_COLLECTION = "savedCollection"

    // Actions log.
    const val START_ACTION_COLLECTION = "startActions"
    const val CONSUME_ACTION_COLLECTION = "consumeActions"
    const val FINISH_ACTION_COLLECTION = "finishActions"
    const val SAVE_ACTION_COLLECTION = "saveActions"
    const val SHARE_ACTION_COLLECTION = "shareActions"
    const val ARCHIVE_ACTION_COLLECTION = "archiveActions"

    // Actions counters.
    const val VIEW_COUNT = "viewCount"
    const val CONSUME_COUNT = "consumeCount"
    const val START_COUNT = "startCount"
    const val FINISH_COUNT = "finishCount"
    const val ORGANIZE_COUNT = "organizeCount"
    const val SHARE_COUNT = "shareCount"
    const val CLEAR_FEED_COUNT = "clearFeedCount"
    const val ARCHIVE_COUNT = "archiveCount"

    // Quality scores.
    const val INVALID_SCORE = 0.0
    const val SAVE_SCORE = 1.0
    const val START_SCORE = 1.0
    const val CONSUME_SCORE = 2.0
    const val FINISH_SCORE = 3.0
    const val SHARE_SCORE = 3.0
    const val ARCHIVE_SCORE = -1.0 // Not opened.

    // Users
    const val USERS_COLLECTION = "users"

    private const val MAX_PRICE_DIFFERENCE_COLLECTION = "maximumPercentDifference"

    val priceDifferenceCollection = FirebaseFirestore
            .getInstance(FirebaseApp.getInstance(PRICE))
            .collection(MAX_PRICE_DIFFERENCE_COLLECTION)

    val contentCollection = FirebaseFirestore
            .getInstance(FirebaseApp.getInstance(CONTENT))
            .collection(CONTENT_COLLECTION)
            .document(FEEDS)
            .collection(MAIN_COLLECTION)
    val usersCollection = FirebaseFirestore
            .getInstance(FirebaseApp.getInstance(CONTENT))
            .collection(USERS_COLLECTION)
}