package com.CYRYEL.com.data.user

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseUserRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth
) : UserRepository {

    override suspend fun getUser(userId: String): Result<User> {
        return try {
            val doc = firestore.collection("users").document(userId).get().await()
            if (doc.exists()) {
                val data = doc.data ?: return Result.failure(Exception("User data is null"))
                Result.success(
                    User(
                        id = doc.id,
                        email = data["email"] as? String ?: "",
                        name = data["name"] as? String ?: "",
                        phone = data["phone"] as? String ?: "",
                        photoUrl = data["photoUrl"] as? String,
                        documentNumber = data["documentNumber"] as? String ?: "",
                        ruc = data["ruc"] as? String ?: "",
                        role = data["role"] as? String ?: "user",
                        fcmToken = data["fcmToken"] as? String ?: "",
                        points = (data["points"] as? Long)?.toInt() ?: 0,
                        isAvailable = (data["isAvailable"] as? Boolean) ?: true,
                        addresses = parseAddresses(data["addresses"])
                    )
                )
            } else {
                Result.failure(Exception("Usuario no encontrado"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveUser(user: User): Result<Unit> {
        return try {
            firestore.collection("users").document(user.id)
                .set(user, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateUser(userId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            firestore.collection("users").document(userId)
                .set(updates, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getCurrentUserId(): String? {
        return firebaseAuth.currentUser?.uid
    }

    private fun parseAddresses(raw: Any?): List<Address> {
        if (raw !is List<*>) return emptyList()
        return raw.mapNotNull { item ->
            if (item !is Map<*, *>) return@mapNotNull null
            @Suppress("UNCHECKED_CAST")
            val map = item as Map<String, Any?>
            try {
                Address(
                    id = map["id"] as? String ?: "",
                    name = map["name"] as? String ?: "",
                    type = map["type"] as? String ?: "home",
                    street = map["street"] as? String ?: "",
                    city = map["city"] as? String ?: "",
                    state = map["state"] as? String ?: "",
                    zipCode = map["zipCode"] as? String ?: "",
                    reference = map["reference"] as? String ?: "",
                    latitude = (map["latitude"] as? Number)?.toDouble() ?: 0.0,
                    longitude = (map["longitude"] as? Number)?.toDouble() ?: 0.0,
                    isDefault = (map["default"] as? Boolean) ?: false
                )
            } catch (_: Exception) { null }
        }
    }
}
