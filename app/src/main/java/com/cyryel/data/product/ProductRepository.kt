package com.cyryel.data.product

import com.cyryel.data.category.Category

interface ProductRepository {
    suspend fun getRandomProducts(limit: Int = 20): Result<List<Product>>
    suspend fun getProductById(productId: String): Result<Product>
    suspend fun getProductsByCategory(category: String, limit: Int = 20): Result<List<Product>>
    suspend fun getCategories(): Result<List<Category>>
}
