package app.coinverse.firebase

import app.coinverse.utils.*
import com.google.firebase.firestore.FirebaseFirestore

object FirestoreCollections {
    val messageCenterCollection = FirebaseFirestore.getInstance().collection(MESSAGE_CENTER)
    val contentEthBtcCollection = FirebaseFirestore.getInstance().collection("$PRICE/$ETH_BTC/$PRICES")
    val contentEnCollection = FirebaseFirestore.getInstance().collection("$CONTENT/$FEEDS/$EN")
    val usersCollection = FirebaseFirestore.getInstance().collection(USERS)
}