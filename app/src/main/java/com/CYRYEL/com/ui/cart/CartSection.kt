package com.CYRYEL.com.ui.cart

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.CYRYEL.com.data.ForcedPackConfig
import com.CYRYEL.com.R
import com.CYRYEL.com.ui.theme.AmarilloVibrante
import com.CYRYEL.com.ui.theme.AzulRey
import com.CYRYEL.com.ui.theme.AzulReyClaro

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    onBack: () -> Unit,
    onCheckout: () -> Unit,
    onProductClick: (String) -> Unit = {},
    onPromotionClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: CartViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.ShoppingCart, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text("Mi Carrito", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atras")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AzulRey,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            if (uiState.items.isNotEmpty()) {
                val redeemedItems = uiState.items.filter { it.redeemedByPoints }
                val pointsUsed = redeemedItems.sumOf { it.product.pointsToRedeem * it.quantity }
                CartBottomBar(
                    subtotal = uiState.subtotal,
                    itemCount = uiState.itemCount,
                    pointsUsed = pointsUsed,
                    enabled = true,
                    onCheckout = onCheckout
                )
            }
        }
    ) { innerPadding ->
        if (uiState.items.isEmpty()) {
            EmptyCart(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "${uiState.items.size} ${if (uiState.items.size == 1) "producto" else "productos"} en tu carrito",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(uiState.items, key = { "${it.productId}|${it.variantName.orEmpty()}|${it.redeemedByPoints}|${it.promotionId.orEmpty()}" }) { item ->
                    val forcedPackSize = ForcedPackConfig.getPackSize(item.product)
                    val isPromoItem = item.promotionId != null
                    CartItemCard(
                        imageUrl = item.product.foto,
                        name = item.productName,
                        price = item.price,
                        quantity = item.quantity,
                        subtotal = item.subtotal,
                        hasVariants = item.variantName != null,
                        forcedPackSize = forcedPackSize,
                        productId = item.productId,
                        redeemedByPoints = item.redeemedByPoints,
                        pointsToRedeem = item.product.pointsToRedeem,
                        isPromoItem = isPromoItem,
                        onProductClick = if (isPromoItem && onPromotionClick != null) {
                            { onPromotionClick(item.promotionId ?: "") }
                        } else {
                            onProductClick
                        },
                        onIncrease = { if (!item.redeemedByPoints && !isPromoItem) viewModel.addProduct(item.product, item.variantName) },
                        onDecrease = { if (!item.redeemedByPoints && !isPromoItem) viewModel.decreaseProduct(item.productId, item.variantName, false) },
                        onRemove = { viewModel.removeProduct(item.productId, item.variantName, item.redeemedByPoints, item.promotionId) }
                    )
                }
                item {
                    Spacer(Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
private fun CartBottomBar(
    subtotal: Double,
    itemCount: Int,
    pointsUsed: Int = 0,
    enabled: Boolean,
    onCheckout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            )
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Subtotal ($itemCount items)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "S/ ${"%.2f".format(subtotal)}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = AzulReyClaro
                )
                if (pointsUsed > 0) {
                    Text(
                        text = "$pointsUsed pts por canje",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = AmarilloVibrante
                    )
                }
            }
            Button(
                onClick = onCheckout,
                enabled = enabled,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AmarilloVibrante,
                    contentColor = AzulReyClaro
                ),
                modifier = Modifier.height(50.dp)
            ) {
                Text(
                    text = "Crear orden",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
private fun CartItemCard(
    imageUrl: String,
    name: String,
    price: Double,
    quantity: Int,
    subtotal: Double,
    hasVariants: Boolean = false,
    forcedPackSize: Int? = null,
    productId: String = "",
    redeemedByPoints: Boolean = false,
    pointsToRedeem: Int = 0,
    isPromoItem: Boolean = false,
    onProductClick: (String) -> Unit = {},
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onProductClick(productId) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = if (imageUrl.isNotBlank()) imageUrl else R.drawable.ic_placeholder_image,
                contentDescription = name,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (redeemedByPoints) {
                        Box(
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(AmarilloVibrante.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "pts",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = AmarilloVibrante
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                if (redeemedByPoints) {
                    Text(
                        text = "${pointsToRedeem * quantity} pts",
                        style = MaterialTheme.typography.bodySmall,
                        color = AmarilloVibrante,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "S/ ${"%.2f".format(price)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = AzulReyClaro,
                            fontWeight = FontWeight.Bold
                        )
                        if (quantity > 1) {
                            Text(
                                text = " x $quantity",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (redeemedByPoints) {
                        Text(
                            text = "Canjeado con puntos",
                            style = MaterialTheme.typography.labelSmall,
                            color = AmarilloVibrante,
                            fontWeight = FontWeight.Medium
                        )
                    } else if (isPromoItem) {
                        Text(
                            text = "${quantity} ${if (quantity == 1) "unidad" else "unidades"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        QuantitySelector(
                            quantity = quantity,
                            onIncrease = onIncrease,
                            onDecrease = onDecrease,
                            hasVariants = hasVariants,
                            forcedPackSize = forcedPackSize
                        )
                    }
                    if (!redeemedByPoints) {
                        Text(
                            text = "S/ ${"%.2f".format(subtotal)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = AzulReyClaro
                        )
                    }
                }
            }
            Spacer(Modifier.width(4.dp))
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Eliminar",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun QuantitySelector(
    quantity: Int,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    hasVariants: Boolean = false,
    forcedPackSize: Int? = null
) {
    val isLocked = hasVariants || forcedPackSize != null
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        IconButton(
            onClick = onDecrease,
            enabled = !isLocked && quantity > 1,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (isLocked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f) else AzulReyClaro.copy(alpha = 0.25f)),
            colors = IconButtonDefaults.iconButtonColors(contentColor = if (isLocked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f) else AzulReyClaro)
        ) {
            Text(
                text = "-",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isLocked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f) else AzulReyClaro
            )
        }
        Text(
            text = "$quantity",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(32.dp),
            textAlign = TextAlign.Center,
            color = if (isLocked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else AzulReyClaro
        )
        IconButton(
            onClick = onIncrease,
            enabled = !isLocked,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (isLocked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f) else AzulRey),
            colors = IconButtonDefaults.iconButtonColors(contentColor = if (isLocked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f) else Color.White)
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = "Aumentar",
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun EmptyCart(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.ShoppingCart,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Tu carrito esta vacio",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Agrega productos para empezar tu pedido",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}
