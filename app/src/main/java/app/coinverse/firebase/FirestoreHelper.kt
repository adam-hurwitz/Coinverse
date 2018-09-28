package app.coinverse.firebase

import android.content.Context
import app.coinverse.BuildConfig.DEBUG
import app.coinverse.utils.auth.Auth.APP_API_ID_STAGING
import app.coinverse.utils.auth.Auth.APP_ID_PRODUCTION
import app.coinverse.utils.auth.Auth.APP_ID_STAGING
import app.coinverse.utils.auth.Auth.CONTENT
import app.coinverse.utils.auth.Auth.CONTENT_PRODUCTION_ID
import app.coinverse.utils.auth.Auth.CONTENT_PRODUCTION_URL
import app.coinverse.utils.auth.Auth.CONTENT_STAGING_ID
import app.coinverse.utils.auth.Auth.CONTENT_STAGING_URL
import app.coinverse.utils.auth.Auth.PRICE
import app.coinverse.utils.auth.Auth.PRICE_PRODUCTION_PROJECT_ID
import app.coinverse.utils.auth.Auth.PRICE_PRODUCTION_URL
import app.coinverse.utils.auth.Auth.PRICE_STAGING_PROJECT_ID
import app.coinverse.utils.auth.Auth.PRICE_STAGING_URL
import com.firebase.client.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore

object FirestoreHelper {
    fun initialize(context: Context) {
        Firebase.setAndroidContext(context)
        var priceFirestoreEnabled = false
        var contentServiceEnabled = false
        for (app in FirebaseApp.getApps(context)) {
            when (app.name) {
                PRICE -> priceFirestoreEnabled = true
                CONTENT -> contentServiceEnabled = true
            }
        }
        if (priceFirestoreEnabled == false) {
            if (DEBUG) {
                initializeFirestore(context, PRICE_STAGING_URL, PRICE_STAGING_PROJECT_ID, PRICE)
            } else {
                initializeFirestore(context, PRICE_PRODUCTION_URL, PRICE_PRODUCTION_PROJECT_ID, PRICE)
            }
        }
        if (contentServiceEnabled == false) {
            if (DEBUG) {
                initializeFirestore(context, CONTENT_STAGING_URL, CONTENT_STAGING_ID, CONTENT)
            } else {
                initializeFirestore(context, CONTENT_PRODUCTION_URL, CONTENT_PRODUCTION_ID, CONTENT)
            }
        }
    }

    private fun initializeFirestore(context: Context, databaseUrl: String, projectId: String,
                                    firestoreName: String) {
        val APP_ID: String
        val APP_API_ID: String
        if (DEBUG) {
            APP_ID = APP_ID_STAGING
            APP_API_ID = APP_API_ID_STAGING
        } else {
            APP_ID = APP_ID_PRODUCTION
            APP_API_ID = APP_ID_PRODUCTION
        }
        FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                        .setApplicationId(APP_ID) // Required for Analytics.
                        .setApiKey(APP_API_ID) // Required for Auth.
                        .setDatabaseUrl(databaseUrl) // Required for RTDB.
                        .setProjectId(projectId)
                        .build(),
                firestoreName)
        FirebaseFirestore.getInstance(FirebaseApp.getInstance(firestoreName))
    }
}