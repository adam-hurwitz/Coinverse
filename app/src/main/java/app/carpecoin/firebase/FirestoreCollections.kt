package app.carpecoin.firebase

import app.carpecoin.utils.Constants
import app.carpecoin.utils.auth.Auth
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore

object FirestoreCollections {
    // Content feed/
    const val CONTENT_COLLECTION = "content"
    const val TIMEFRAME = "timeframe"
    const val ALL_COLLECTION = "all"
    const val WEEK_COLLECTION = "week"

    // Users.
    const val USERS_COLLECTION = "users"

    private const val MAX_PRICE_DIFFERENCE_COLLECTION = "maximumPercentDifference"
    val priceDifferenceCollection = FirebaseFirestore
            .getInstance(FirebaseApp.getInstance(Auth.PRICE_FIRESTORE_NAME))
            .collection(MAX_PRICE_DIFFERENCE_COLLECTION)
    val contentCollection = FirebaseFirestore
            .getInstance(FirebaseApp.getInstance(Auth.CONTENT_FIRESTORE_NAME))
            .collection(CONTENT_COLLECTION)
            .document(TIMEFRAME)
    val usersCollection = FirebaseFirestore
            .getInstance(FirebaseApp.getInstance(Auth.CONTENT_FIRESTORE_NAME))
            .collection(USERS_COLLECTION)
}