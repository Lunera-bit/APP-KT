package com.cyryel.data.category

data class Category(
    val id: String = "",
    val name: String = "",
    val icon: String = "",
    val imageUrl: String = "",
    val orden: Int = 0,
    val isActive: Boolean = true,
    val colorStart: String = "",
    val colorEnd: String = ""
)
