package com.cyryel.data.product

import com.cyryel.data.ForcedPackConfig
import com.cyryel.data.category.Category
import com.cyryel.data.local.ProductDao
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.tasks.await
import java.text.Normalizer
import javax.inject.Inject

class FirebaseProductRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val productDao: ProductDao
) : ProductRepository {

    private val gson = Gson()
    private val categoryCache = mutableMapOf<String, CachedCategory>()
    private val CACHE_TTL_MS = 5 * 60 * 1000L

    private data class CachedCategory(
        val products: MutableList<Product> = mutableListOf(),
        var lastDocId: String? = null,
        var timestamp: Long = 0L
    )

    override suspend fun getRandomProducts(limit: Int): Result<List<Product>> {
        return try {
            val snapshot = firestore.collection("products")
                .limit(30)
                .get()
                .await()
            val all = snapshot.documents.map { document ->
                productFromDocument(document)
            }.filter { it.nombre.isNotBlank() && it.isActive && it.availableStock > 5 && !ForcedPackConfig.isForcedPackProduct(it) }

            val cacheTimestamp = System.currentTimeMillis()
            if (all.isNotEmpty()) {
                productDao.upsertAll(all.map { it.toEntity(cacheTimestamp) })
                productDao.deleteAllExcept(all.map { it.id })
            }

            val sampled = if (all.size <= limit) all else all.shuffled().take(limit)
            Result.success(sampled)
        } catch (exception: Exception) {
            val cached = productDao.getAllProducts()
                .map { it.toDomain() }
                .filter { it.isActive && it.availableStock > 5 && !ForcedPackConfig.isForcedPackProduct(it) }

            if (cached.isNotEmpty()) {
                val sampled = if (cached.size <= limit) cached else cached.shuffled().take(limit)
                Result.success(sampled)
            } else {
                Result.failure(exception)
            }
        }
    }

    override suspend fun getProductById(productId: String): Result<Product> {
        return try {
            val doc = firestore.collection("products").document(productId).get().await()
            if (doc.exists()) {
                val product = productFromDocument(doc)
                Result.success(product)
            } else {
                Result.failure(Exception("Producto no encontrado"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getProductsByCategory(category: String, limit: Int): Result<List<Product>> {
        return getProductsByCategoryPaged(category, limit, null).map { it.products }
    }

    override suspend fun getProductsByCategoryPaged(
        category: String,
        limit: Int,
        startAfter: String?
    ): Result<ProductPage> {
        return try {
            val now = System.currentTimeMillis()

            if (startAfter == null) {
                val cached = categoryCache[category]
                if (cached != null && now - cached.timestamp < CACHE_TTL_MS) {
                    return Result.success(ProductPage(cached.products.toList(), cached.lastDocId))
                }
                categoryCache[category] = CachedCategory(timestamp = now)
            }

            var query = firestore.collection("products")
                .whereEqualTo("categoria", category)
                .orderBy("nombre")
                .limit(limit.toLong())

            if (startAfter != null) {
                val lastDoc = firestore.collection("products").document(startAfter).get().await()
                if (lastDoc.exists()) {
                    query = query.startAfter(lastDoc)
                }
            }

            val snapshot = query.get().await()
            val products = snapshot.documents.mapNotNull { productFromDocument(it) }
            val newLastDocId = snapshot.documents.lastOrNull()?.id

            val cached = categoryCache[category]
            if (cached != null) {
                val existingIds = cached.products.map { it.id }.toSet()
                for (p in products) {
                    if (p.id !in existingIds) {
                        cached.products.add(p)
                    }
                }
                cached.lastDocId = newLastDocId
            }
            Result.success(ProductPage(products, newLastDocId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchProducts(query: String, limit: Int): Result<List<Product>> {
        return try {
            val norm = try {
                Normalizer.normalize(query.lowercase(), Normalizer.Form.NFD)
                    .replace(Regex("[\\u0300-\\u036f]"), "")
                    .replace(Regex("[^a-z0-9\\s]"), " ")
                    .replace(Regex("\\s+"), " ")
                    .trim()
            } catch (_: Exception) {
                query.lowercase().trim()
            }
            val tokens = norm.split(" ").filter { it.length >= 2 }

            if (tokens.isNotEmpty()) {
                val token = tokens.first()
                val snapshot = firestore.collection("products")
                    .whereArrayContains("searchKeywords", token)
                    .limit(limit.toLong())
                    .get()
                    .await()
                val products = snapshot.documents.mapNotNull { productFromDocument(it) }
                    .filter { it.isActive }
                return Result.success(products)
            }

            val start = query.uppercase()
            val end = start + "\uf8ff"
            val snapshot = firestore.collection("products")
                .orderBy("nombre")
                .whereGreaterThanOrEqualTo("nombre", start)
                .whereLessThanOrEqualTo("nombre", end)
                .limit(limit.toLong())
                .get()
                .await()
            val products = snapshot.documents.mapNotNull { productFromDocument(it) }
                .filter { it.isActive }
            Result.success(products)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchProductsByCategory(query: String, category: String, limit: Int): Result<List<Product>> {
        return try {
            val norm = try {
                Normalizer.normalize(query.lowercase(), Normalizer.Form.NFD)
                    .replace(Regex("[\\u0300-\\u036f]"), "")
                    .replace(Regex("[^a-z0-9\\s]"), " ")
                    .replace(Regex("\\s+"), " ")
                    .trim()
            } catch (_: Exception) {
                query.lowercase().trim()
            }
            val tokens = norm.split(" ").filter { it.length >= 2 }

            if (tokens.isNotEmpty()) {
                val snapshot = firestore.collection("products")
                    .whereArrayContains("searchKeywords", tokens.first())
                    .limit((limit * 3).toLong())
                    .get()
                    .await()
                val products = snapshot.documents.mapNotNull { productFromDocument(it) }
                    .filter { it.isActive && it.categoria == category }
                    .take(limit)
                return Result.success(products)
            }

            val snapshot = firestore.collection("products")
                .whereEqualTo("categoria", category)
                .orderBy("nombre")
                .limit(limit.toLong())
                .get()
                .await()
            val products = snapshot.documents.mapNotNull { productFromDocument(it) }
                .filter { it.isActive }
            Result.success(products)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getRedeemableProducts(limit: Int): Result<List<Product>> {
        return try {
            val snapshot = firestore.collection("products")
                .whereGreaterThan("pointsToRedeem", 0)
                .limit(limit.toLong())
                .get()
                .await()
            val products = snapshot.documents.mapNotNull { productFromDocument(it) }
                .filter { it.isActive }
            Result.success(products)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

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

    private fun productFromDocument(document: com.google.firebase.firestore.DocumentSnapshot): Product {
        return Product(
            id = document.id,
            nombre = document.getString("nombre").orEmpty(),
            categoria = document.getString("categoria").orEmpty(),
            codigo = document.getString("codigo").orEmpty(),
            foto = document.getString("foto").orEmpty(),
            precio = (document.getDouble("precio") ?: 0.0),
            stock = (document.getLong("stock") ?: 0L).toInt(),
            unidad = document.getString("unidad").orEmpty(),
            isActive = document.getBoolean("isActive") ?: true,
            description = document.getString("description") ?: "",
            keywords = parseKeywords(document.get("keywords")),
            variantes = parseVariantes(document.get("variantes")),
            points = (document.getLong("points") ?: 0L).toInt(),
            pointsToRedeem = (document.getLong("pointsToRedeem") ?: 0L).toInt(),
            updatedAt = document.getTimestamp("updatedAt")?.toDate()?.time ?: 0L
        )
    }

    private fun parseVariantes(raw: Any?): List<ProductVariant> {
        return when (raw) {
            is List<*> -> raw.mapNotNull { item ->
                when (item) {
                    is Map<*, *> -> ProductVariant(
                        nombre = (item["nombre"] as? String).orEmpty(),
                        precio = (item["precio"] as? Number)?.toDouble() ?: 0.0,
                        cantidad = (item["cantidad"] as? Number)?.toInt()
                    )
                    else -> null
                }
            }
            is String -> try {
                val type = object : TypeToken<List<ProductVariant>>() {}.type
                gson.fromJson(raw, type)
            } catch (_: Exception) { emptyList() }
            else -> emptyList()
        }
    }

    private fun parseKeywords(raw: Any?): List<String> {
        return when (raw) {
            is List<*> -> raw.mapNotNull { it as? String }
            is String -> try {
                val type = object : TypeToken<List<String>>() {}.type
                gson.fromJson(raw, type)
            } catch (_: Exception) { emptyList() }
            else -> emptyList()
        }
    }
}
