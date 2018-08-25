package app.carpecoin.firebase

import android.content.Context
import app.carpecoin.utils.auth.Auth
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
            when(app.name){
                Auth.PRICE_FIRESTORE_NAME -> priceFirestoreEnabled = true
                Auth.CONTENT_FIRESTORE_NAME -> contentServiceEnabled = true
            }
        }
        if (priceFirestoreEnabled == false) {
            initializeFirestore(context, Auth.PRICE_FIRESTORE_DATABASE_URL,
                    Auth.PRICE_FIRESTORE_PROJECT_ID, Auth.PRICE_FIRESTORE_NAME)
        }
        if (contentServiceEnabled == false) {
            initializeFirestore(context, Auth.CONTENT_FIRESTORE_DATABASE_URL,
                    Auth.CONTENT_FIRESTORE_PROJECT_ID, Auth.CONTENT_FIRESTORE_NAME)
        }
    }

    private fun initializeFirestore(context: Context, databaseUrl: String, projectId: String,
                                    firestoreName: String) {
        FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                        .setApplicationId(Auth.APP_ID) // Required for Analytics.
                        .setApiKey(Auth.APP_API_ID) // Required for Auth.
                        .setDatabaseUrl(databaseUrl) // Required for RTDB.
                        .setProjectId(projectId)
                        .build(),
                firestoreName)
        FirebaseFirestore.getInstance(FirebaseApp.getInstance(firestoreName))
    }
}