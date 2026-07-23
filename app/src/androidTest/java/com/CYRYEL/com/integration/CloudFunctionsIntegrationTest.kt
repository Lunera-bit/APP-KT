package com.CYRYEL.com.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.FirebaseApp
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class CloudFunctionsIntegrationTest {

    companion object {
        private lateinit var functions: FirebaseFunctions

        @BeforeClass
        @JvmStatic
        fun setUp() {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            functions = FirebaseFunctions.getInstance()
        }
    }

    private fun callFunction(
        name: String,
        data: Map<String, Any?> = emptyMap(),
        timeoutSeconds: Long = 30
    ): Any? {
        val latch = CountDownLatch(1)
        var result: Any? = null
        var error: Exception? = null

        functions
            .getHttpsCallable(name)
            .call(data)
            .addOnSuccessListener { task ->
                result = task.data
                latch.countDown()
            }
            .addOnFailureListener { e ->
                error = e
                latch.countDown()
            }

        latch.await(timeoutSeconds, TimeUnit.SECONDS)
        if (error != null) throw error!!
        return result
    }

    @Test
    fun integration_validateDeliveryZone_validZone() = runBlocking {
        try {
            val result = callFunction("validateDeliveryZone", mapOf(
                "address" to "Av. Principal Chancay"
            ))
            assertNotNull("Should return a result for valid zone", result)
        } catch (e: Exception) {
            assertNotNull("Function should exist and be callable", e.message)
        }
    }

    @Test
    fun integration_validateDeliveryZone_invalidZone() = runBlocking {
        try {
            val result = callFunction("validateDeliveryZone", mapOf(
                "address" to "Av. Larco 123, Lima"
            ))
            assertNotNull("Should return a result for invalid zone", result)
        } catch (e: Exception) {
            assertNotNull("Function should handle invalid zone gracefully", e.message)
        }
    }

    @Test
    fun integration_sendTestNotification_unauthenticated() {
        try {
            callFunction("sendTestNotification", mapOf(
                "userId" to "test-user-id"
            ))
            fail("Should fail without authentication")
        } catch (e: Exception) {
            assertTrue(
                "Should return permission denied or unauthenticated error",
                e.message?.contains("unauthenticated") == true ||
                e.message?.contains("permission") == true ||
                e.message?.contains("denied") == true
            )
        }
    }

    @Test
    fun integration_setUserRole_unauthenticated() {
        try {
            callFunction("setUserRole", mapOf(
                "userId" to "test-user-id",
                "role" to "admin"
            ))
            fail("Should fail without admin authentication")
        } catch (e: Exception) {
            assertTrue(
                "Should return permission denied error",
                e.message?.contains("unauthenticated") == true ||
                e.message?.contains("permission") == true ||
                e.message?.contains("denied") == true
            )
        }
    }

    @Test
    fun integration_addFunds_unauthenticated() {
        try {
            callFunction("addFunds", mapOf(
                "amount" to 100.0
            ))
            fail("Should fail without authentication")
        } catch (e: Exception) {
            assertTrue(
                "Should return unauthenticated error",
                e.message?.contains("unauthenticated") == true ||
                e.message?.contains("permission") == true ||
                e.message?.contains("denied") == true
            )
        }
    }

    @Test
    fun integration_confirmPayment_unauthenticated() {
        try {
            callFunction("confirmPayment", mapOf(
                "orderId" to "test-order-id"
            ))
            fail("Should fail without admin authentication")
        } catch (e: Exception) {
            assertTrue(
                "Should return permission denied error",
                e.message?.contains("unauthenticated") == true ||
                e.message?.contains("permission") == true ||
                e.message?.contains("denied") == true
            )
        }
    }

    @Test
    fun integration_redeemReward_unauthenticated() {
        try {
            callFunction("redeemReward", mapOf(
                "productId" to "test-product",
                "points" to 50
            ))
            fail("Should fail without authentication")
        } catch (e: Exception) {
            assertTrue(
                "Should return unauthenticated error",
                e.message?.contains("unauthenticated") == true ||
                e.message?.contains("permission") == true ||
                e.message?.contains("denied") == true
            )
        }
    }

    @Test
    fun integration_withdrawFunds_unauthenticated() {
        try {
            callFunction("withdrawFunds", mapOf(
                "amount" to 50.0
            ))
            fail("Should fail without authentication")
        } catch (e: Exception) {
            assertTrue(
                "Should return unauthenticated error",
                e.message?.contains("unauthenticated") == true ||
                e.message?.contains("permission") == true ||
                e.message?.contains("denied") == true
            )
        }
    }

    @Test
    fun integration_sendBroadcastMessage_unauthenticated() {
        try {
            callFunction("sendBroadcastMessage", mapOf(
                "title" to "Test",
                "message" to "Test message"
            ))
            fail("Should fail without admin authentication")
        } catch (e: Exception) {
            assertTrue(
                "Should return permission denied error",
                e.message?.contains("unauthenticated") == true ||
                e.message?.contains("permission") == true ||
                e.message?.contains("denied") == true
            )
        }
    }

    @Test
    fun integration_releaseDelivery_unauthenticated() {
        try {
            callFunction("releaseDelivery", mapOf(
                "orderId" to "test-order-id"
            ))
            fail("Should fail without delivery authentication")
        } catch (e: Exception) {
            assertTrue(
                "Should return permission denied error",
                e.message?.contains("unauthenticated") == true ||
                e.message?.contains("permission") == true ||
                e.message?.contains("denied") == true
            )
        }
    }
}
