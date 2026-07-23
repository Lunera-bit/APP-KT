package com.CYRYEL.com.data.user

interface UserRepository {
    suspend fun getUser(userId: String): Result<User>
    suspend fun saveUser(user: User): Result<Unit>
    suspend fun updateUser(userId: String, updates: Map<String, Any>): Result<Unit>
    fun getCurrentUserId(): String?
}
