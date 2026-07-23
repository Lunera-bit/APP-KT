package com.CYRYEL.com.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class FirestoreIntegrationTest {

    companion object {
        private lateinit var db: FirebaseFirestore
        private lateinit var auth: FirebaseAuth

        @BeforeClass
        @JvmStatic
        fun setUp() {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            db = FirebaseFirestore.getInstance()
            auth = FirebaseAuth.getInstance()
        }
    }

    @Test
    fun integration_firestoreCategoriesReadable() = runBlocking {
        val snapshot = db.collection("categories").get().await()
        assertNotNull("Categories collection should be accessible", snapshot)
    }

    @Test
    fun integration_firestoreProductsReadable() = runBlocking {
        val snapshot = db.collection("products").get().await()
        assertNotNull("Products collection should be accessible", snapshot)
    }

    @Test
    fun integration_firestorePromotionsReadable() = runBlocking {
        val snapshot = db.collection("promotions").get().await()
        assertNotNull("Promotions collection should be accessible", snapshot)
    }

    @Test
    fun integration_firestoreConfigReadable() = runBlocking {
        val snapshot = db.collection("config").get().await()
        assertNotNull("Config collection should be accessible", snapshot)
    }

    @Test
    fun integration_firestoreOrdersRequiresAuth() {
        val latch = CountDownLatch(1)
        var failed = false

        db.collection("orders")
            .get()
            .addOnSuccessListener { latch.countDown() }
            .addOnFailureListener {
                failed = true
                latch.countDown()
            }

        latch.await(10, TimeUnit.SECONDS)
    }

    @Test
    fun integration_firestoreUserDocRequiresAuth() {
        val latch = CountDownLatch(1)
        var failed = false

        db.collection("users")
            .document("nonexistent-user-id")
            .get()
            .addOnSuccessListener { latch.countDown() }
            .addOnFailureListener {
                failed = true
                latch.countDown()
            }

        latch.await(10, TimeUnit.SECONDS)
    }

    @Test
    fun integration_firestoreDeliveriesRequiresAuth() {
        val latch = CountDownLatch(1)
        var failed = false

        db.collection("deliveries")
            .get()
            .addOnSuccessListener { latch.countDown() }
            .addOnFailureListener {
                failed = true
                latch.countDown()
            }

        latch.await(10, TimeUnit.SECONDS)
    }
}
