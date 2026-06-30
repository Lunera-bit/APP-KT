package com.cyryel.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cyryel.data.product.ProductVariant

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val nombre: String,
    val categoria: String,
    val codigo: String,
    val foto: String,
    val precio: Double,
    val stock: Int,
    val unidad: String,
    val description: String = "",
    val isActive: Boolean,
    val keywords: List<String>,
    val variantes: List<ProductVariant>,
    val points: Int,
    val pointsToRedeem: Int,
    val updatedAt: Long,
    val cachedAt: Long
)
