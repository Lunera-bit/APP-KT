package com.CYRYEL.com.ui.promotiondetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.CYRYEL.com.data.cart.CartManager
import com.CYRYEL.com.data.promotion.Promotion
import com.CYRYEL.com.data.promotion.PromotionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PromotionDetailUiState(
    val isLoading: Boolean = true,
    val promotion: Promotion? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class PromotionDetailViewModel @Inject constructor(
    private val promotionRepository: PromotionRepository,
    private val cartManager: CartManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PromotionDetailUiState())
    val uiState: StateFlow<PromotionDetailUiState> = _uiState.asStateFlow()

    fun loadPromotion(promotionId: String) {
        _uiState.update { PromotionDetailUiState(isLoading = true) }
        viewModelScope.launch {
            try {
                val promotion = promotionRepository.getPromotionById(promotionId)
                _uiState.update {
                    it.copy(isLoading = false, promotion = promotion, errorMessage = if (promotion == null) "Promocion no encontrada" else null)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.localizedMessage ?: "Error al cargar promocion")
                }
            }
        }
    }

    fun addToCart() {
        val promotion = _uiState.value.promotion ?: return
        cartManager.addPromotionProducts(promotion)
    }
}
