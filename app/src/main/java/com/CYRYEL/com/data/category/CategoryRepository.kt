package com.CYRYEL.com.data.category

interface CategoryRepository {
    suspend fun getCategories(): Result<List<Category>>
}
