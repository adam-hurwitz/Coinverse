package app.coinverse.home

import android.app.Application
import app.coinverse.content.room.CoinverseDatabase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.firestore.FirebaseFirestore

class HomeRepository(application: Application) {

    private val LOG_TAG = HomeRepository::class.java.simpleName

    private var analytics: FirebaseAnalytics
    private var firestore: FirebaseFirestore
    private var database: CoinverseDatabase

    init {
        analytics = FirebaseAnalytics.getInstance(application)
        firestore = FirebaseFirestore.getInstance()
        database = CoinverseDatabase.getAppDatabase(application)
    }
}