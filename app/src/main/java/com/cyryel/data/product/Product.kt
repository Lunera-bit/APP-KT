package com.cyryel.data.product

const val STOCK_RESERVE = 4

val Product.availableStock: Int
    get() = maxOf(0, stock - STOCK_RESERVE)

data class Product(
    val id: String = "",
    val nombre: String = "",
    val categoria: String = "",
    val codigo: String = "",
    val foto: String = "",
    val precio: Double = 0.0,
    val stock: Int = 0,
    val unidad: String = "",
    val description: String = "",
    val isActive: Boolean = true,
    val keywords: List<String> = emptyList(),
    val variantes: List<ProductVariant> = emptyList(),
    val points: Int = 0,
    val pointsToRedeem: Int = 0,
    val updatedAt: Long = 0L
)
