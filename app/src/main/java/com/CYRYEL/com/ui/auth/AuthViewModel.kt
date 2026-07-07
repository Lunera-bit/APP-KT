package com.CYRYEL.com.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.CYRYEL.com.data.auth.AuthRepository
import com.CYRYEL.com.data.user.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    val authStateFlow = authRepository.authStateFlow()

    private val _uiState = MutableStateFlow(
        AuthUiState(isAuthenticated = authRepository.isLoggedIn())
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        if (authRepository.isLoggedIn()) {
            viewModelScope.launch {
                val uid = authRepository.getCurrentUserId() ?: return@launch
                authRepository.saveFcmToken(uid)
                loadUserRole(uid)
            }
        } else {
            _uiState.update { it.copy(isRoleLoaded = true) }
        }
    }

    private suspend fun loadUserRole(uid: String) {
        userRepository.getUser(uid).onSuccess { user ->
            _uiState.update { it.copy(role = user.role, isRoleLoaded = true) }
        }.onFailure {
            _uiState.update { it.copy(isRoleLoaded = true) }
        }
    }

    fun onEmailChange(newEmail: String) {
        _uiState.update { it.copy(email = newEmail, errorMessage = null) }
    }

    fun onPasswordChange(newPassword: String) {
        _uiState.update { it.copy(password = newPassword, errorMessage = null) }
    }

    fun onTermsAcceptedChange(accepted: Boolean) {
        _uiState.update { it.copy(termsAccepted = accepted, errorMessage = null) }
    }

    private suspend fun afterLogin() {
        val uid = authRepository.getCurrentUserId() ?: return
        authRepository.saveFcmToken(uid)
        loadUserRole(uid)
        _uiState.update {
            it.copy(isLoading = false, isAuthenticated = true, errorMessage = null)
        }
    }

    fun signIn() {
        val currentState = _uiState.value
        if (currentState.email.isBlank() || currentState.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Ingresa correo y contrasena") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, password = "") }

            val result = authRepository.signIn(
                email = currentState.email,
                password = currentState.password
            )

            if (result.isSuccess) {
                afterLogin()
            } else {
                val message = result.exceptionOrNull()?.localizedMessage ?: "No se pudo iniciar sesion"
                _uiState.update {
                    it.copy(isLoading = false, isAuthenticated = false, errorMessage = message)
                }
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val result = authRepository.signInWithGoogle(idToken)

            if (result.isSuccess) {
                afterLogin()
            } else {
                val message = result.exceptionOrNull()?.localizedMessage ?: "No se pudo iniciar sesion con Google"
                _uiState.update {
                    it.copy(isLoading = false, isAuthenticated = false, errorMessage = message)
                }
            }
        }
    }

    fun showError(message: String) {
        _uiState.update { it.copy(isLoading = false, errorMessage = message) }
    }

    fun signOut() {
        authRepository.signOut()
        _uiState.update {
            AuthUiState()
        }
    }
}
