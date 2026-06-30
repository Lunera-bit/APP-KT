package com.cyryel.data.category

interface CategoryRepository {
    suspend fun getCategories(): Result<List<Category>>
}
