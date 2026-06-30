package com.cyryel.ui.billetera

import com.cyryel.data.promotion.Promotion
import com.cyryel.data.user.User

data class BilleteraUiState(
    val isLoading: Boolean = true,
    val user: User? = null,
    val pointsHistory: List<PointsTransaction> = emptyList(),
    val offers: List<Promotion> = emptyList(),
    val errorMessage: String? = null
)

data class PointsTransaction(
    val id: String = "",
    val type: String = "", // "earned" o "redeemed"
    val amount: Int = 0,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
