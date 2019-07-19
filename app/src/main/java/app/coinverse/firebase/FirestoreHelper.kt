package app.coinverse.firebase

import android.content.Context
import android.util.Log
import androidx.databinding.library.BuildConfig.DEBUG
import app.coinverse.R
import app.coinverse.utils.auth.*
import com.firebase.client.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

val LOG_TAG = FirestoreHelper.javaClass.simpleName

object FirestoreHelper {
    fun initialize(context: Context) {
        Firebase.setAndroidContext(context)
        if (DEBUG) initializeFirestore(context, CONTENT_STAGING_URL, CONTENT_STAGING_ID)
        else initializeFirestore(context, CONTENT_PRODUCTION_URL, CONTENT_PRODUCTION_ID)
        initializeRemoteConfig()
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

    private fun initializeRemoteConfig() {
        val firebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        firebaseRemoteConfig.setConfigSettings(FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(DEBUG)
                .build())
        firebaseRemoteConfig.setDefaults(R.xml.remote_config_defaults)

        var cacheExpiration = 3600L
        if (firebaseRemoteConfig.info.configSettings.isDeveloperModeEnabled) {
            cacheExpiration = 0
        }

        firebaseRemoteConfig.fetch(cacheExpiration).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // After config data is successfully fetched, it must be activated before newly fetched
                // values are returned.
                firebaseRemoteConfig.activateFetched()
            } else {
                Log.e(LOG_TAG, "Remote Config Fetch Failed")
            }
        }

    }
}