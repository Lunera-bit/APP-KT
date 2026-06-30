package com.cyryel.ui.promotions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyryel.data.promotion.PromotionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PromotionsViewModel @Inject constructor(
    private val promotionRepository: PromotionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PromotionsUiState())
    val uiState: StateFlow<PromotionsUiState> = _uiState.asStateFlow()

    init {
        loadPromotions()
    }

    fun loadPromotions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = promotionRepository.getActivePromotions()
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(isLoading = false, promotions = result.getOrDefault(emptyList()))
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Error al cargar promociones"
                    )
                }
            }
        }
    }
}
