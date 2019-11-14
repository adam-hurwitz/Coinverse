package app.coinverse.firebase

import android.content.Context
import android.util.Log
import androidx.databinding.library.BuildConfig.DEBUG
import app.coinverse.BuildConfig
import app.coinverse.R
import app.coinverse.utils.BuildType.open
import app.coinverse.utils.auth.*
import com.crashlytics.android.Crashlytics
import com.firebase.client.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

private val LOG_TAG = FirebaseHelper.javaClass.simpleName

object FirebaseHelper {
    fun init(context: Context) {
        if (BuildConfig.BUILD_TYPE == open.name) {
            var openSharedStatus = false
            FirebaseApp.getApps(context).map { app ->
                if (app.name.equals(open.name)) openSharedStatus = true
            }
            if (!openSharedStatus)
                FirebaseApp.initializeApp(
                        context,
                        FirebaseOptions.Builder()
                                .setApplicationId(APP_ID_OPEN_SHARED)
                                .setApiKey(APP_API_KEY_OPEN_SHARED)
                                .setDatabaseUrl(DATABASE_URL_OPEN_SHARED)
                                .setProjectId(PROJECT_ID_OPEN_SHARED)
                                .setStorageBucket(STORAGE_BUCKET_OPEN_SHARED)
                                .build(),
                        open.name)
        }
        Firebase.setAndroidContext(context)
        initializeRemoteConfig()
    }

    // FIXME - Update deprecated code.
    private fun initializeRemoteConfig() {
        val firebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        firebaseRemoteConfig.setConfigSettings(FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(DEBUG)
                .build())
        firebaseRemoteConfig.setDefaults(R.xml.remote_config_defaults)
        var cacheExpiration = 3600L
        if (firebaseRemoteConfig.info.configSettings.isDeveloperModeEnabled) cacheExpiration = 0
        // TODO - Refactor addOnCompleteListeners to await() coroutine. See [ContentRepository]
        firebaseRemoteConfig.fetch(cacheExpiration).addOnCompleteListener { task ->
            // After config data is successfully fetched, it must be activated before newly fetched
            // values are returned.
            if (task.isSuccessful) firebaseRemoteConfig.activateFetched()
            else Crashlytics.log(Log.ERROR, LOG_TAG, "Remote Config Fetch Failed")
        }

    }
}