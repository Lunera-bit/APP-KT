package com.cyryel.data

import com.cyryel.data.product.Product

object ForcedPackConfig {

    private val forcedPackMap: Map<String, Int> = mapOf(
        "041e6520-991a-439e-b888-ad609642fb58" to 4,
        "35fa9ad7-d37d-4391-9368-0f9e394b7ab7" to 6,
        "8f3011c9-a95d-4f14-b510-73c621420457" to 12,
        "b62aa545-8c19-44a7-aaeb-dda4c1b79d9b" to 6,
        "55af7ff5-0c84-4c36-9221-5c74cf95ecf7" to 4,
        "2e71fb57-9e53-4f24-9f57-b03535830a59" to 6,
        "c591738a-89c4-4826-b648-9ed4ad7c4904" to 12,
        "20250725172121" to 4,
        "20250725173041" to 6,
        "20250725173551" to 12,
        "20250725173640" to 6,
        "20250725173002" to 4,
        "20250725173134" to 6,
        "20250725173403" to 12
    )

    fun getPackSize(product: Product): Int? {
        return forcedPackMap[product.id] ?: forcedPackMap[product.codigo]
    }

    fun getPackSize(productId: String, codigo: String = ""): Int? {
        return forcedPackMap[productId] ?: forcedPackMap[codigo]
    }

    fun isForcedPackProduct(product: Product): Boolean {
        return getPackSize(product) != null
    }

    fun isForcedPackProduct(productId: String, codigo: String = ""): Boolean {
        return getPackSize(productId, codigo) != null
    }

    val allForcedPackIds: Set<String>
        get() = forcedPackMap.keys
}
