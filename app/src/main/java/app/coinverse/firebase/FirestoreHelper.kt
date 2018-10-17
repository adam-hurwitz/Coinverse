package app.coinverse.firebase

import android.content.Context
import app.coinverse.BuildConfig.DEBUG
import app.coinverse.utils.auth.*
import com.firebase.client.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore

object FirestoreHelper {

    fun initialize(context: Context) {
        Firebase.setAndroidContext(context)
        if (DEBUG) initializeFirestore(context, CONTENT_STAGING_URL, CONTENT_STAGING_ID)
        else initializeFirestore(context, CONTENT_PRODUCTION_URL, CONTENT_PRODUCTION_ID)
    }

    private fun initializeFirestore(context: Context, databaseUrl: String, projectId: String) {
        if (!FirebaseFirestore.getInstance().app.isDefaultApp) {
            val APP_ID: String
            val APP_API_ID: String
            if (DEBUG) {
                APP_ID = APP_ID_STAGING
                APP_API_ID = APP_API_ID_STAGING
            } else {
                APP_ID = APP_ID_PRODUCTION
                APP_API_ID = APP_ID_PRODUCTION
            }
            FirebaseApp.initializeApp(context, FirebaseOptions.Builder()
                    .setApplicationId(APP_ID) // Required for Analytics.
                    .setApiKey(APP_API_ID) // Required for Auth.
                    .setDatabaseUrl(databaseUrl) // Required for RTDB.
                    .setProjectId(projectId)
                    .build())
        }
    }
}