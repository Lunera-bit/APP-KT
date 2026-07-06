package com.cyryel.data.config

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseConfigRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : ConfigRepository {

    override suspend fun getBankAccounts(): Result<List<BankAccountData>> {
        return try {
            val doc = firestore.collection("config").document("bancos").get().await()
            val accounts = if (doc.exists()) {
                val raw = doc.get("accounts") as? List<*> ?: emptyList<Any>()
                raw.mapNotNull { item ->
                    when (item) {
                        is Map<*, *> -> BankAccountData(
                            name = (item["name"] as? String) ?: "",
                            number = (item["number"] as? String) ?: ""
                        )
                        else -> null
                    }
                }
            } else {
                emptyList()
            }
            Result.success(accounts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
