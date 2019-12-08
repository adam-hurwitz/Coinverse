package app.coinverse.firebase

import android.content.Context
import android.util.Log
import app.coinverse.BuildConfig
import app.coinverse.R
import app.coinverse.utils.BuildType.open
import app.coinverse.utils.auth.*
import com.crashlytics.android.Crashlytics
import com.firebase.client.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
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

    private fun initializeRemoteConfig() {
        val firebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        firebaseRemoteConfig.setConfigSettingsAsync(FirebaseRemoteConfigSettings.Builder().build())
        firebaseRemoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
        val cacheExpiration = 3600L
        try {
            firebaseRemoteConfig.fetch(cacheExpiration)
            firebaseRemoteConfig.fetchAndActivate()
        } catch (exception: FirebaseRemoteConfigException) {
            Crashlytics.log(Log.ERROR, LOG_TAG, "initializeRemoteConfig: ${exception.localizedMessage}")
        }
    }
}