package com.cyryel.data.product

data class ProductPage(
    val products: List<Product>,
    val lastDocId: String?
)
