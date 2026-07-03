package com.cyryel.ui.notifications

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyryel.data.auth.AuthRepository
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class NotificationDebugState(
    val fcmToken: String = "Cargando...",
    val tokenSavedInFirestore: Boolean? = null,
    val firestoreToken: String = "",
    val userId: String = "",
    val logs: List<FcmLogEntry> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationDebugState())
    val uiState: StateFlow<NotificationDebugState> = _uiState.asStateFlow()

    init {
        loadDebugInfo()
    }

    fun loadDebugInfo() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val userId = authRepository.getCurrentUserId() ?: ""
            _uiState.update { it.copy(userId = userId) }

            var firestoreToken: String? = null
            if (userId.isNotBlank()) {
                try {
                    firestoreToken = authRepository.getFcmToken(userId)
                    _uiState.update {
                        it.copy(
                            tokenSavedInFirestore = firestoreToken != null,
                            firestoreToken = firestoreToken ?: "No encontrado"
                        )
                    }
                } catch (e: Exception) {
                    _uiState.update { it.copy(tokenSavedInFirestore = false) }
                }
            }

            var errorMsg: String? = null
            val fcmToken = try {
                FirebaseMessaging.getInstance().getToken().await()
            } catch (e1: Exception) {
                Log.w("FCM_DEBUG", "getToken() failed", e1)
                try {
                    FirebaseMessaging.getInstance().token.await()
                } catch (e2: Exception) {
                    Log.w("FCM_DEBUG", "token.await() also failed", e2)
                    errorMsg = "getToken: ${e1.localizedMessage ?: e1.javaClass.simpleName} | token: ${e2.localizedMessage ?: e2.javaClass.simpleName}"
                    firestoreToken?.also { Log.d("FCM_DEBUG", "Falling back to Firestore token") }
                    firestoreToken
                }
            }

            if (fcmToken != null) {
                _uiState.update { it.copy(fcmToken = fcmToken) }
                Log.d("FCM_DEBUG", "Token obtained: ${fcmToken.take(30)}...")
            } else {
                _uiState.update {
                    it.copy(
                        fcmToken = if (errorMsg != null) "Error: $errorMsg" else "No disponible",
                        errorMessage = errorMsg
                    )
                }
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    logs = FcmDebugStore.getLogs()
                )
            }
        }
    }

    fun refreshLogs() {
        _uiState.update { it.copy(logs = FcmDebugStore.getLogs()) }
        loadDebugInfo()
    }

    fun clearLogs() {
        FcmDebugStore.clear()
        _uiState.update { it.copy(logs = emptyList()) }
    }
}
