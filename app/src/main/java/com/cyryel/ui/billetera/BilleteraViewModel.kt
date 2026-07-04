package com.cyryel.ui.billetera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyryel.data.auth.AuthRepository
import com.cyryel.data.product.ProductRepository
import com.cyryel.data.promotion.PromotionRepository
import com.cyryel.data.user.UserRepository
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.tasks.await
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
    private val productRepository: ProductRepository,
    private val authRepository: AuthRepository,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _uiState = MutableStateFlow(BilleteraUiState())
    val uiState: StateFlow<BilleteraUiState> = _uiState.asStateFlow()

    val pointsRules = PointsRules()

    init {
        loadBilletera()
    }

    fun loadBilletera() {
        val userId = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val userResult = userRepository.getUser(userId)
            val offersResult = promotionRepository.getActivePromotions()
            val redeemableResult = productRepository.getRedeemableProducts()

            if (userResult.isSuccess) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        user = userResult.getOrNull(),
                        offers = offersResult.getOrDefault(emptyList()),
                        redeemableProducts = redeemableResult.getOrDefault(emptyList())
                    )
                }
                loadPointsHistory(userId)
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

    private suspend fun loadPointsHistory(userId: String) {
        try {
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("pointsHistory")
                .get()
                .await()

            val history = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                val createdAt = (data["createdAt"] as? com.google.firebase.Timestamp)?.toDate()?.time
                    ?: (data["createdAt"] as? Long) ?: 0L
                val points = ((data["points"] as? Long) ?: 0).toInt()
                val type = data["type"] as? String ?: if (points > 0) "earned" else "redeemed"
                val reason = data["reason"] as? String ?: ""
                val orderId = data["orderId"] as? String ?: ""

                val description = when (reason) {
                    "order_completed" -> "Pedido completado"
                    "checkout_discount" -> "Descuento en compra"
                    else -> reason.replaceFirstChar { it.uppercase() }.ifEmpty {
                        if (points > 0) "Puntos ganados" else "Puntos canjeados"
                    }
                }

                PointsTransaction(
                    id = doc.id,
                    type = type,
                    amount = points,
                    description = description + if (orderId.isNotBlank()) " · #$orderId" else "",
                    createdAt = createdAt
                )
            }

            _uiState.update {
                it.copy(pointsHistory = history.sortedByDescending { it.createdAt })
            }
        } catch (_: Exception) {
            // Si no existe la subcolección, ignoramos
        }
    }
}
