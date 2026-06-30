package com.cyryel

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CyryelMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token generated")
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                FirebaseFirestore.getInstance()
                    .collection("users").document(uid)
                    .set(mapOf("fcmToken" to token), SetOptions.merge())
                    .addOnSuccessListener { Log.d("FCM", "Token refreshed for $uid") }
                    .addOnFailureListener { Log.w("FCM", "Token refresh failed for $uid", it) }
            } catch (e: Exception) {
                Log.w("FCM", "Token refresh failed for $uid", e)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        message.notification?.let { notification ->
            Log.d("FCM", "Notification: ${notification.title} - ${notification.body}")
        }
    }
}
