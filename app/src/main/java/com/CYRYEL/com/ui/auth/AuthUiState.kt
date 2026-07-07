package com.CYRYEL.com.ui.auth

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val errorMessage: String? = null,
    val termsAccepted: Boolean = false,
    val role: String = "user",
    val isRoleLoaded: Boolean = false
)
