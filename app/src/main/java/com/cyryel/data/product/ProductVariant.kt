package com.cyryel.data.product

data class ProductVariant(
    val nombre: String,
    val precio: Double,
    val cantidad: Int? = null
)
