package com.cyryel.data.auth

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    override fun isLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }

    override fun getCurrentUserId(): String? {
        return firebaseAuth.currentUser?.uid
    }

    override fun getCurrentUserEmail(): String? {
        return firebaseAuth.currentUser?.email
    }

    override suspend fun signIn(email: String, password: String): Result<Unit> {
        return try {
            firebaseAuth.signInWithEmailAndPassword(email.trim(), password).await()
            Result.success(Unit)
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    override suspend fun signInWithGoogle(idToken: String): Result<Unit> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            firebaseAuth.signInWithCredential(credential).await()
            Result.success(Unit)
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    override suspend fun saveFcmToken(userId: String) {
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            val updates = mapOf<String, Any>(
                "fcmToken" to token,
                "fcmTokenUpdatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            firestore.collection("users").document(userId)
                .set(updates, SetOptions.merge())
                .await()
            Log.d("FCM", "Token saved for user $userId")
        } catch (e: Exception) {
            Log.w("FCM", "Failed to save FCM token for $userId", e)
        }
    }

    override suspend fun getFcmToken(userId: String): String? {
        return try {
            val doc = firestore.collection("users").document(userId).get().await()
            val token = doc.getString("fcmToken")
            Log.d("FCM", "Token retrieved for $userId: ${token != null}")
            token
        } catch (e: Exception) {
            Log.w("FCM", "Failed to get FCM token for $userId", e)
            null
        }
    }

    override fun signOut() {
        firebaseAuth.signOut()
    }
}
