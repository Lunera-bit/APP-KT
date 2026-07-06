package com.cyryel

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@AndroidEntryPoint
class LocationUploaderService : Service() {

    @Inject
    lateinit var firestore: FirebaseFirestore

    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    serviceScope.launch {
                        uploadLocation(location.latitude, location.longitude)
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        startLocationUpdates()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        serviceScope.cancel()
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL_MS
        ).apply {
            setMinUpdateIntervalMillis(FASTEST_INTERVAL_MS)
        }.build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (_: SecurityException) {

        }
    }

    private suspend fun uploadLocation(latitude: Double, longitude: Double) {
        val uid = firebaseAuth.currentUser?.uid ?: return
        try {
            val deliveries = firestore.collection("deliveries")
                .whereEqualTo("deliveryPersonId", uid)
                .whereIn("status", listOf("aceptado", "en_camino"))
                .get().await()

            for (doc in deliveries.documents) {
                firestore.collection("deliveries").document(doc.id)
                    .update(
                        mapOf(
                            "lastLocation" to com.google.firebase.firestore.GeoPoint(latitude, longitude),
                            "lastLocationUpdatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                        )
                    )
            }
        } catch (_: Exception) {

        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, LOCATION_CHANNEL_ID)
            .setContentTitle("CYRYEL Delivery")
            .setContentText("Compartiendo ubicacion en tiempo real")
            .setSmallIcon(R.drawable.ic_stat_4081896)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val LOCATION_CHANNEL_ID = "location_service"
        private const val UPDATE_INTERVAL_MS = 15000L
        private const val FASTEST_INTERVAL_MS = 10000L

        fun start(context: Context) {
            val intent = Intent(context, LocationUploaderService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LocationUploaderService::class.java))
        }
    }
}
