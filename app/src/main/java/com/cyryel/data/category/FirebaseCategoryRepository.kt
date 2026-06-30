package com.cyryel.data.category

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseCategoryRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : CategoryRepository {

    override suspend fun getCategories(): Result<List<Category>> {
        return try {
            val snapshot = firestore.collection("categories")
                .orderBy("orden")
                .limit(20)
                .get()
                .await()
            val categories = snapshot.documents.map { doc ->
                val data = doc.data ?: return@map null
                Category(
                    id = doc.id,
                    name = data["name"] as? String ?: data["nombre"] as? String ?: "",
                    icon = data["icon"] as? String ?: "",
                    imageUrl = data["imageUrl"] as? String ?: data["foto"] as? String ?: data["imagen"] as? String ?: data["url"] as? String ?: "",
                    orden = (data["orden"] as? Long)?.toInt() ?: 0,
                    isActive = data["isActive"] as? Boolean ?: true,
                    colorStart = data["colorStart"] as? String ?: data["color"] as? String ?: "",
                    colorEnd = data["colorEnd"] as? String ?: ""
                )
            }.filterNotNull()
            Result.success(categories)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
