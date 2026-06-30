package com.cyryel.data.product

import com.cyryel.data.local.ProductEntity

fun Product.toEntity(cachedAt: Long): ProductEntity {
    return ProductEntity(
        id = id,
        nombre = nombre,
        categoria = categoria,
        codigo = codigo,
        foto = foto,
        precio = precio,
        stock = stock,
        unidad = unidad,
        description = description,
        isActive = isActive,
        keywords = keywords,
        variantes = variantes,
        points = points,
        pointsToRedeem = pointsToRedeem,
        updatedAt = updatedAt,
        cachedAt = cachedAt
    )
}

fun ProductEntity.toDomain(): Product {
    return Product(
        id = id,
        nombre = nombre,
        categoria = categoria,
        codigo = codigo,
        foto = foto,
        precio = precio,
        stock = stock,
        unidad = unidad,
        description = description,
        isActive = isActive,
        keywords = keywords,
        variantes = variantes,
        points = points,
        pointsToRedeem = pointsToRedeem,
        updatedAt = updatedAt
    )
}
