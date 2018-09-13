package app.carpecoin.firebase

import app.carpecoin.utils.auth.Auth
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore

object FirestoreCollections {
    // Content feed.
    const val CONTENT_COLLECTION = "content"
    const val FEED = "feed"
    const val MAIN_COLLECTION = "main"
    const val ARCHIVED_COLLECTION = "archived"
    const val SAVED_COLLECTION = "saved"

    // Users
    const val USERS_COLLECTION = "users"

    private const val MAX_PRICE_DIFFERENCE_COLLECTION = "maximumPercentDifference"
    val priceDifferenceCollection = FirebaseFirestore
            .getInstance(FirebaseApp.getInstance(Auth.PRICE_FIRESTORE_NAME))
            .collection(MAX_PRICE_DIFFERENCE_COLLECTION)
    val contentCollection = FirebaseFirestore
            .getInstance(FirebaseApp.getInstance(Auth.CONTENT_FIRESTORE_NAME))
            .collection(CONTENT_COLLECTION)
            .document(FEED)
    val usersCollection = FirebaseFirestore
            .getInstance(FirebaseApp.getInstance(Auth.CONTENT_FIRESTORE_NAME))
            .collection(USERS_COLLECTION)
}