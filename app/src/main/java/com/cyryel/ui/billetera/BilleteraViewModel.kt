package com.cyryel.ui.billetera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyryel.data.auth.AuthRepository
import com.cyryel.data.promotion.PromotionRepository
import com.cyryel.data.user.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BilleteraViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val promotionRepository: PromotionRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BilleteraUiState())
    val uiState: StateFlow<BilleteraUiState> = _uiState.asStateFlow()

    init {
        loadBilletera()
    }

    fun loadBilletera() {
        val userId = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val userResult = userRepository.getUser(userId)
            val offersResult = promotionRepository.getActivePromotions()

            if (userResult.isSuccess) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        user = userResult.getOrNull(),
                        offers = offersResult.getOrDefault(emptyList())
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = userResult.exceptionOrNull()?.localizedMessage ?: "Error al cargar billetera"
                    )
                }
            }
        }
    }
}
