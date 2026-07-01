package com.cyryel.data.auth

interface AuthRepository {
    fun isLoggedIn(): Boolean
    fun getCurrentUserId(): String?
    fun getCurrentUserEmail(): String?
    suspend fun signIn(email: String, password: String): Result<Unit>
    suspend fun signInWithGoogle(idToken: String): Result<Unit>
    fun signOut()
    suspend fun saveFcmToken(userId: String)
    suspend fun getFcmToken(userId: String): String?
}
