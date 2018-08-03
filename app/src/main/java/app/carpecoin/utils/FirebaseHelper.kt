package app.carpecoin.utils

import android.content.Context
import app.carpecoin.utils.auth.Auth
import com.firebase.client.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseHelper {
    fun initialize(context: Context) {
        Firebase.setAndroidContext(context)
        var contentServiceEnabled = false
        for (app in FirebaseApp.getApps(context)) {
            if (app.name.equals(Auth.PRICE_SERVICE)) {
                contentServiceEnabled = true
            }
        }
        if (contentServiceEnabled == false) {
            FirebaseApp.initializeApp(
                    context,
                    FirebaseOptions.Builder()
                            .setApplicationId(Auth.PRICE_SERVICE_APP_ID) // Required for Analytics.
                            .setApiKey(Auth.PRICE_SERVICE_API_ID) // Required for Auth.
                            .setDatabaseUrl(Auth.DATABASE_URL) // Required for RTDB.
                            .setProjectId(Auth.PROJECT_ID)
                            .build(),
                    Auth.PRICE_SERVICE)
        }
        FirebaseFirestore.getInstance(FirebaseApp.getInstance(Auth.PRICE_SERVICE))
    }
}