package com.cyryel.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyryel.data.auth.AuthRepository
import com.cyryel.data.notificacion.NotificacionData
import com.cyryel.data.notificacion.NotificacionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BandejaUiState(
    val notificaciones: List<NotificacionData> = emptyList(),
    val isLoading: Boolean = true,
    val unreadCount: Int = 0,
    val errorMessage: String? = null
)

@HiltViewModel
class BandejaViewModel @Inject constructor(
    private val notificacionRepository: NotificacionRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BandejaUiState())
    val uiState: StateFlow<BandejaUiState> = _uiState.asStateFlow()

    init {
        cargarNotificaciones()
    }

    fun cargarNotificaciones() {
        val userId = authRepository.getCurrentUserId() ?: return
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            notificacionRepository.getNotificaciones(userId).collect { notis ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        notificaciones = notis,
                        unreadCount = notis.count { n -> !n.read }
                    )
                }
            }
        }
    }

    fun marcarLeida(notificacion: NotificacionData) {
        if (notificacion.read) return
        viewModelScope.launch {
            notificacionRepository.marcarLeida(notificacion.id)
            _uiState.update { state ->
                val updated = state.notificaciones.map { n ->
                    if (n.id == notificacion.id) n.copy(read = true) else n
                }
                state.copy(
                    notificaciones = updated,
                    unreadCount = updated.count { !it.read }
                )
            }
        }
    }

    fun marcarTodasLeidas() {
        val userId = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            notificacionRepository.marcarTodasLeidas(userId)
            _uiState.update { state ->
                val updated = state.notificaciones.map { it.copy(read = true) }
                state.copy(notificaciones = updated, unreadCount = 0)
            }
        }
    }
}
