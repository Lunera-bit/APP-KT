package com.cyryel.ui.categoryproducts

import com.cyryel.data.product.Product

data class CategoryProductsUiState(
    val isLoading: Boolean = true,
    val products: List<Product> = emptyList(),
    val categoryName: String = "",
    val errorMessage: String? = null
)
