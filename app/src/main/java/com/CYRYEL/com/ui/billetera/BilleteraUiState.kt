package com.CYRYEL.com.ui.billetera

import com.CYRYEL.com.data.product.Product
import com.CYRYEL.com.data.promotion.Promotion
import com.CYRYEL.com.data.user.User

data class BilleteraUiState(
    val isLoading: Boolean = true,
    val user: User? = null,
    val pointsHistory: List<PointsTransaction> = emptyList(),
    val offers: List<Promotion> = emptyList(),
    val redeemableProducts: List<Product> = emptyList(),
    val errorMessage: String? = null
)

data class PointsTransaction(
    val id: String = "",
    val type: String = "", // "earned" o "redeemed"
    val amount: Int = 0,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class PointsRules(
    val pointsPer300: Int = 10,
    val pointsPer600: Int = 30,
    val promoUnitPoints: Int = 5
)
