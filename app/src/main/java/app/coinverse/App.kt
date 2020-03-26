package app.coinverse

import android.app.Application
import app.coinverse.dependencyInjection.DaggerAppComponent
import app.coinverse.dependencyInjection.UtilsModule
import app.coinverse.utils.AD_UNIT_ID
import com.mopub.common.MoPub
import com.mopub.common.SdkConfiguration
import com.mopub.common.SdkInitializationListener

class App : Application() {
    val appComponent = DaggerAppComponent.builder()
            .utilsModule(UtilsModule(this))
            .build()

    override fun onCreate() {
        super.onCreate()
        appComponent.firebaseHelper()
        MoPub.initializeSdk(this, SdkConfiguration.Builder(AD_UNIT_ID).build(), initSdkListener())
    }

    private fun initSdkListener() = SdkInitializationListener { /* MoPub SDK initialized.*/ }
}