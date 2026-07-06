package com.cyryel.data.product

import com.cyryel.data.category.Category
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    suspend fun getRandomProducts(limit: Int = 20): Result<List<Product>>
    suspend fun getProductById(productId: String): Result<Product>
    fun observeProduct(productId: String): Flow<Result<Product>>
    suspend fun getProductsByCategory(category: String, limit: Int = 20): Result<List<Product>>
    suspend fun getProductsByCategoryPaged(category: String, limit: Int = 20, startAfter: String? = null): Result<ProductPage>
    suspend fun searchProducts(query: String, limit: Int = 50): Result<List<Product>>
    suspend fun searchProductsByCategory(query: String, category: String, limit: Int = 50): Result<List<Product>>
    suspend fun getCategories(): Result<List<Category>>
    suspend fun getRedeemableProducts(limit: Int = 50): Result<List<Product>>
}
