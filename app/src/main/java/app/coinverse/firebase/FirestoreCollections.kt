package app.coinverse.firebase

import app.coinverse.utils.*
import com.google.firebase.firestore.FirebaseFirestore

object FirestoreCollections {
    val contentEthBtcCollection = FirebaseFirestore.getInstance().collection("$PRICE_COLLECTION/$ETH_BTC_DOC/$PRICES_COLLECTION")
    val contentEnCollection = FirebaseFirestore.getInstance().collection("$CONTENT_COLLECTION/$FEEDS_DOC/$EN_COLLECTION")
    val usersCollection = FirebaseFirestore.getInstance().collection(USERS_COLLECTION)
}