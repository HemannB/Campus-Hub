package br.com.uri.campushub.integration

import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings

object FirebaseEmulatorConfig {
    private const val FIREBASE_APP_NAME = "campushub-integration"
    private var authConfigured = false
    private var firestoreConfigured = false

    @Synchronized
    fun auth(): FirebaseAuth {
        val auth = FirebaseAuth.getInstance(firebaseApp())
        if (!authConfigured) {
            auth.useEmulator("10.0.2.2", 9099)
            authConfigured = true
        }
        return auth
    }

    @Synchronized
    fun firestore(): FirebaseFirestore {
        val firestore = FirebaseFirestore.getInstance(firebaseApp())
        if (!firestoreConfigured) {
            firestore.useEmulator("10.0.2.2", 8080)
            firestore.firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                .build()
            firestoreConfigured = true
        }
        return firestore
    }

    private fun firebaseApp(): FirebaseApp {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return FirebaseApp.getApps(context)
            .firstOrNull { app -> app.name == FIREBASE_APP_NAME }
            ?: FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                    .setProjectId("demo-campushub")
                    .setApplicationId("1:1234567890:android:campushub-integration")
                    .setApiKey("fake-api-key")
                    .build(),
                FIREBASE_APP_NAME
            )
    }
}
