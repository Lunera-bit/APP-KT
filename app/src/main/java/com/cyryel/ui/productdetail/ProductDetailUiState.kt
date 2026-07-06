package com.cyryel.ui.productdetail

import com.cyryel.data.product.Product
import com.cyryel.data.product.availableStock

data class ProductDetailUiState(
    val isLoading: Boolean = true,
    val product: Product? = null,
    val selectedVariantIndex: Int = -1,
    val quantity: Int = 1,
    val forcedPackSize: Int? = null,
    val errorMessage: String? = null,
    val cartQuantity: Int = 0
) {
    val displayPrice: Double
        get() = if (selectedVariantIndex >= 0 && product != null) {
            product.variantes[selectedVariantIndex].precio
        } else {
            product?.precio ?: 0.0
        }

    val displayStock: Int
        get() {
            val p = product ?: return 0
            val real = maxOf(0, p.availableStock - cartQuantity)
            if (forcedPackSize != null) {
                val groups = real / forcedPackSize
                return groups * forcedPackSize
            }
            return maxOf(0, real)
        }
}
