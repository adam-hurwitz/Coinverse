package app.carpecoin.firebase

import app.carpecoin.utils.auth.Auth
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore

object FirestoreCollections {
    // Content feed.
    const val PROD_COLLECTION = "prod"
    const val QA_COLLECTION = "qa"
    const val CONTENT = "content"
    const val FEEDS_COLLECTION = "feeds"
    const val MAIN = "main"
    const val CONTENT_COLLECTION = "content"
    const val ARCHIVED_COLLECTION = "archived"
    const val SAVED_COLLECTION = "saved"

    // Users
    const val USERS = "users"
    const val USERS_COLLECTION = "users"

    private const val MAX_PRICE_DIFFERENCE_COLLECTION = "maximumPercentDifference"
    val priceDifferenceCollection = FirebaseFirestore
            .getInstance(FirebaseApp.getInstance(Auth.PRICE_FIRESTORE_NAME))
            .collection(MAX_PRICE_DIFFERENCE_COLLECTION)
    val contentCollection = FirebaseFirestore
            .getInstance(FirebaseApp.getInstance(Auth.CONTENT_FIRESTORE_NAME))
            .collection(QA_COLLECTION)
            .document(CONTENT)
            .collection(FEEDS_COLLECTION)
            .document(MAIN)
            .collection(CONTENT_COLLECTION)
    val usersCollection = FirebaseFirestore
            .getInstance(FirebaseApp.getInstance(Auth.CONTENT_FIRESTORE_NAME))
            .collection(QA_COLLECTION)
            .document(USERS)
            .collection(USERS_COLLECTION)
}