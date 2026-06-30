package com.cyryel.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.cyryel.R
import com.cyryel.data.category.Category
import com.cyryel.data.product.Product
import com.cyryel.ui.billetera.BilleteraScreen
import com.cyryel.ui.cart.CartViewModel
import com.cyryel.ui.orders.OrdersScreen
import com.cyryel.ui.profile.ProfileViewModel
import com.cyryel.ui.theme.AmarilloVibrante
import com.cyryel.ui.theme.AzulRey

private data class Tab(val label: String, val iconRes: Int)

private fun parseHexColor(hex: String): Color {
    return try {
        val clean = hex.removePrefix("#")
        Color(("FF$clean").toLong(radix = 16))
    } catch (_: Exception) {
        AzulRey
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToProduct: (String) -> Unit = {},
    onNavigateToOrderDetail: (String) -> Unit = {},
    onNavigateToCategory: (String) -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToOffers: () -> Unit = {},
    onNavigateToHistorial: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToCart: () -> Unit = {},
    modifier: Modifier = Modifier,
    cartViewModel: CartViewModel = hiltViewModel(),
    homeViewModel: HomeViewModel = hiltViewModel()
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val cartState by cartViewModel.uiState.collectAsStateWithLifecycle()
    val homeState by homeViewModel.uiState.collectAsStateWithLifecycle()

    val tabs = listOf(
        Tab("Inicio", R.drawable.ic_home),
        Tab("Pedidos", R.drawable.ic_list),
        Tab("Billetera", R.drawable.ic_cash),
        Tab("Perfil", R.drawable.ic_profile)
    )

    Scaffold(
        modifier = modifier,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(AmarilloVibrante),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "C",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = AzulRey
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "CYRYEL STORE",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = "Mas stock • Mejor precio",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    if (onNavigateToCart != {}) {
                        val cartCount = cartState.items.sumOf { it.cantidad }
                        IconButton(onClick = onNavigateToCart) {
                            BadgedBox(
                                badge = {
                                    if (cartCount > 0) {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.error
                                        ) {
                                            Text(
                                                text = if (cartCount > 99) "99+" else cartCount.toString(),
                                                color = MaterialTheme.colorScheme.onError,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Filled.ShoppingCart,
                                    contentDescription = "Carrito",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                    IconButton(onClick = onNavigateToNotifications) {
                        Icon(
                            Icons.Filled.Notifications,
                            contentDescription = "Notificaciones",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToSearch() }
                        .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 12.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = "Buscar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Buscar productos...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                painterResource(tab.iconRes),
                                contentDescription = tab.label
                            )
                        },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AmarilloVibrante,
                            selectedTextColor = AzulRey,
                            indicatorColor = AzulRey.copy(alpha = 0.1f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            0 -> InicioTab(
                modifier = Modifier.padding(innerPadding),
                homeState = homeState,
                onRefresh = homeViewModel::loadData,
                cartViewModel = cartViewModel,
                onProductClick = onNavigateToProduct,
                onCategoryClick = onNavigateToCategory
            )
            1 -> PedidosTab(
                modifier = Modifier.padding(innerPadding),
                onOrderClick = onNavigateToOrderDetail
            )
            2 -> BilleteraTab(
                modifier = Modifier.padding(innerPadding),
                onNavigateToOffers = onNavigateToOffers,
                onNavigateToHistorial = onNavigateToHistorial
            )
            3 -> PerfilTab(
                modifier = Modifier.padding(innerPadding),
                onNavigateToSettings = onNavigateToSettings
            )
        }
    }
}

@Composable
private fun InicioTab(
    modifier: Modifier = Modifier,
    homeState: HomeUiState,
    onRefresh: () -> Unit,
    cartViewModel: CartViewModel,
    onProductClick: (String) -> Unit,
    onCategoryClick: (String) -> Unit
) {
    when {
        homeState.isLoading -> {
            Row(
                modifier = modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator()
            }
        }
        else -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PromocionesSection(homeState.promotions)
                PedidoRapidoSection(
                    products = homeState.quickProducts,
                    onAddToCart = cartViewModel::addProduct,
                    onProductClick = onProductClick
                )
                CategoriasSection(
                    categories = homeState.categories,
                    onCategoryClick = onCategoryClick
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun PromocionesSection(promotions: List<com.cyryel.data.promotion.Promotion>) {
    Text(
        text = "Promociones",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = AzulRey
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (promotions.isEmpty()) {
                Text(
                    text = "No hay promociones de momento",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                promotions.forEach { promo ->
                    Text(text = promo.name, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun PedidoRapidoSection(
    products: List<Product>,
    onAddToCart: (Product) -> Unit,
    onProductClick: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AzulRey)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Pedido rapido",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(Modifier.height(12.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(products, key = { it.id }) { product ->
                    QuickProductCard(
                        product = product,
                        onAddToCart = onAddToCart,
                        onProductClick = onProductClick
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { products.forEach(onAddToCart) },
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AmarilloVibrante,
                    contentColor = AzulRey
                )
            ) {
                Text(
                    text = "Agregar todo",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun QuickProductCard(
    product: Product,
    onAddToCart: (Product) -> Unit,
    onProductClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .clickable { onProductClick(product.id) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (product.foto.isNotBlank()) {
                AsyncImage(
                    model = product.foto,
                    contentDescription = product.nombre,
                    modifier = Modifier.size(80.dp),
                    contentScale = ContentScale.Fit
                )
            }
            Text(
                text = product.nombre,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = AzulRey
            )
            Text(
                text = "S/ ${"%.2f".format(product.precio)}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = AzulRey
            )
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = { onAddToCart(product) },
                modifier = Modifier.fillMaxWidth().height(32.dp),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AzulRey)
            ) {
                Text(
                    text = "Agregar",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun CategoriasSection(
    categories: List<Category>,
    onCategoryClick: (String) -> Unit
) {
    Text(
        text = "Categorias de productos",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = AzulRey
    )
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxWidth().height(categories.size.let { ((it + 1) / 2) * 120 + 8 }.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(2.dp)
    ) {
        items(categories, key = { it.id }) { category ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clickable { onCategoryClick(category.name) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (category.colorStart.isNotBlank())
                        parseHexColor(category.colorStart)
                    else AzulReyClaro
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    if (category.imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = category.imageUrl,
                            contentDescription = null,
                            modifier = Modifier.align(Alignment.BottomEnd).size(60.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PedidosTab(
    modifier: Modifier = Modifier,
    onOrderClick: (String) -> Unit
) {
    OrdersScreen(
        onBack = {},
        onOrderClick = onOrderClick,
        modifier = modifier
    )
}

@Composable
private fun BilleteraTab(
    modifier: Modifier = Modifier,
    onNavigateToOffers: () -> Unit,
    onNavigateToHistorial: () -> Unit
) {
    BilleteraScreen(
        onNavigateToOffers = onNavigateToOffers,
        onNavigateToHistorial = onNavigateToHistorial,
        modifier = modifier
    )
}

@Composable
private fun PerfilTab(
    modifier: Modifier = Modifier,
    onNavigateToSettings: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when {
            uiState.isLoading -> {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator()
                }
            }
            else -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = uiState.userName.ifBlank { "Usuario" },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = uiState.userEmail,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        if (uiState.userPhone.isNotBlank()) {
                            Text(
                                text = uiState.userPhone,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Button(
                    onClick = onNavigateToSettings,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Configuracion")
                }
            }
        }
    }
}
