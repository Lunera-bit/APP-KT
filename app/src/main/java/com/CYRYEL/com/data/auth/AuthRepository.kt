package com.CYRYEL.com.data.auth

import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun isLoggedIn(): Boolean
    fun getCurrentUserId(): String?
    fun getCurrentUserEmail(): String?
    fun authStateFlow(): Flow<FirebaseUser?>
    suspend fun signIn(email: String, password: String): Result<Unit>
    suspend fun signInWithGoogle(idToken: String): Result<Unit>
    fun signOut()
    suspend fun saveFcmToken(userId: String)
    suspend fun getFcmToken(userId: String): String?
}
