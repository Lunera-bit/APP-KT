package com.cyryel.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyryel.data.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AuthUiState(isAuthenticated = authRepository.isLoggedIn())
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        if (authRepository.isLoggedIn()) {
            viewModelScope.launch {
                authRepository.getCurrentUserId()?.let { uid ->
                    authRepository.saveFcmToken(uid)
                }
            }
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

    fun signIn() {
        val currentState = _uiState.value
        if (currentState.email.isBlank() || currentState.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Ingresa correo y contrasena") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val result = authRepository.signIn(
                email = currentState.email,
                password = currentState.password
            )

            if (result.isSuccess) {
                authRepository.getCurrentUserId()?.let { uid ->
                    authRepository.saveFcmToken(uid)
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isAuthenticated = true,
                        password = "",
                        errorMessage = null
                    )
                }
            } else {
                val message = result.exceptionOrNull()?.localizedMessage ?: "No se pudo iniciar sesion"
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isAuthenticated = false,
                        errorMessage = message
                    )
                }
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val result = authRepository.signInWithGoogle(idToken)

            if (result.isSuccess) {
                authRepository.getCurrentUserId()?.let { uid ->
                    authRepository.saveFcmToken(uid)
                }
                _uiState.update {
                    it.copy(isLoading = false, isAuthenticated = true, errorMessage = null)
                }
            } else {
                val message = result.exceptionOrNull()?.localizedMessage ?: "No se pudo iniciar sesion con Google"
                _uiState.update {
                    it.copy(isLoading = false, isAuthenticated = false, errorMessage = message)
                }
            }
        }
    }

    fun signOut() {
        authRepository.signOut()
        _uiState.update {
            it.copy(
                isAuthenticated = false,
                password = "",
                errorMessage = null
            )
        }
    }
}
