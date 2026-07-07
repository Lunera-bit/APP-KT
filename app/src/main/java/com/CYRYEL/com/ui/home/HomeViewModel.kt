package com.CYRYEL.com.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.CYRYEL.com.data.ForcedPackConfig
import com.CYRYEL.com.data.auth.AuthRepository
import com.CYRYEL.com.data.category.Category
import com.CYRYEL.com.data.category.CategoryRepository
import com.CYRYEL.com.data.notificacion.NotificacionRepository
import com.CYRYEL.com.data.product.Product
import com.CYRYEL.com.data.product.ProductRepository
import com.CYRYEL.com.data.product.availableStock
import com.CYRYEL.com.data.promotion.Promotion
import com.CYRYEL.com.data.promotion.PromotionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val quickProducts: List<Product> = emptyList(),
    val categories: List<Category> = emptyList(),
    val promotions: List<Promotion> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val agregarTodoUsado: Boolean = false,
    val unreadNotifCount: Int = 0
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository,
    private val promotionRepository: PromotionRepository,
    private val notificacionRepository: NotificacionRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
        observePromotions()
        cargarNotifCount()
    }

    private var notifCountJob: kotlinx.coroutines.Job? = null

    fun cargarNotifCount() {
        notifCountJob?.cancel()
        val userId = authRepository.getCurrentUserId() ?: return
        notifCountJob = viewModelScope.launch {
            notificacionRepository.getNotificaciones(userId).collect { notis ->
                val unread = notis.count { !it.read }
                _uiState.update { it.copy(unreadNotifCount = unread) }
            }
        }
    }

    fun marcarAgregarTodoUsado() {
        _uiState.update { it.copy(agregarTodoUsado = true) }
    }

    private var lastQuickRefreshTime = 0L
    private val quickRefreshInterval = 5 * 60 * 1000L

    fun refreshQuickProducts() {
        val now = System.currentTimeMillis()
        if (now - lastQuickRefreshTime < quickRefreshInterval) return
        lastQuickRefreshTime = now
        viewModelScope.launch {
            val productsResult = productRepository.getRandomProducts(limit = 10)
            val products = productsResult.getOrDefault(emptyList())
            val inStockProducts = products.filter { it.availableStock > 5 && !ForcedPackConfig.isForcedPackProduct(it) }
            _uiState.update {
                it.copy(
                    quickProducts = inStockProducts.take(6),
                    agregarTodoUsado = false
                )
            }
        }
    }

    private fun observePromotions() {
        viewModelScope.launch {
            promotionRepository.getActivePromotions().collect { promotions ->
                _uiState.update { it.copy(promotions = promotions) }
            }
        }
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val productsResult = productRepository.getRandomProducts(limit = 10)
            val categoriesResult = categoryRepository.getCategories()

            val products = productsResult.getOrDefault(emptyList())
            val categories = categoriesResult.getOrDefault(emptyList())
            val inStockProducts = products.filter { it.availableStock > 5 && !ForcedPackConfig.isForcedPackProduct(it) }

            if (productsResult.isFailure) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        quickProducts = inStockProducts.take(6),
                        categories = categories,
                        errorMessage = productsResult.exceptionOrNull()?.localizedMessage
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        quickProducts = inStockProducts.take(6),
                        categories = categories,
                        errorMessage = null
                    )
                }
            }
        }
    }
}
