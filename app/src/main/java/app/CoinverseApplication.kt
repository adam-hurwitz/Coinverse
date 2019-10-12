package app

import android.app.Application
import app.coinverse.analytics.Analytics
import app.coinverse.content.room.CoinverseDatabase
import app.coinverse.firebase.FirebaseHelper
import app.coinverse.utils.AD_UNIT_ID
import app.coinverse.utils.resourcesUtil
import com.google.firebase.analytics.FirebaseAnalytics
import com.mopub.common.MoPub
import com.mopub.common.SdkConfiguration
import com.mopub.common.SdkInitializationListener

class CoinverseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseHelper.init(this)
        Analytics.init(FirebaseAnalytics.getInstance(this))
        CoinverseDatabase.init(this)
        resourcesUtil = resources
        MoPub.initializeSdk(this, SdkConfiguration.Builder(AD_UNIT_ID).build(), initSdkListener())
    }

    private fun initSdkListener() = SdkInitializationListener { /* MoPub SDK initialized.*/ }
}