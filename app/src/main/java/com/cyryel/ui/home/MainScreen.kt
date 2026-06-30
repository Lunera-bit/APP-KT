package com.cyryel.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cyryel.R
import com.cyryel.ui.billetera.BilleteraScreen
import com.cyryel.ui.cart.CartSection
import com.cyryel.ui.cart.CartViewModel
import com.cyryel.ui.catalog.CatalogSection
import com.cyryel.ui.orders.OrdersScreen

private data class Tab(
    val label: String,
    val iconRes: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToProduct: (String) -> Unit = {},
    onNavigateToCheckout: () -> Unit = {},
    onNavigateToOrderDetail: (String) -> Unit = {},
    onNavigateToCategory: (String) -> Unit = {},
    onNavigateToPromotions: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToOffers: () -> Unit = {},
    onNavigateToHistorial: () -> Unit = {},
    onSignOut: () -> Unit = {},
    modifier: Modifier = Modifier,
    cartViewModel: CartViewModel = hiltViewModel()
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    val tabs = listOf(
        Tab("Inicio", R.drawable.ic_home),
        Tab("Tienda", R.drawable.ic_shop),
        Tab("Carrito", R.drawable.ic_cart),
        Tab("Pedidos", R.drawable.ic_list),
        Tab("Billetera", R.drawable.ic_cash)
    )

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "CYRYEL STORE",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(painterResource(tab.iconRes), contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            0 -> InicioTab(
                modifier = Modifier.padding(innerPadding),
                cartViewModel = cartViewModel,
                onProductClick = onNavigateToProduct,
                onCategoryClick = onNavigateToCategory,
                onPromotionsClick = onNavigateToPromotions
            )
            1 -> TiendaTab(
                modifier = Modifier.padding(innerPadding),
                cartViewModel = cartViewModel,
                onProductClick = onNavigateToProduct,
                onSearchClick = onNavigateToSearch
            )
            2 -> CarritoTab(
                modifier = Modifier.padding(innerPadding),
                cartViewModel = cartViewModel,
                onCheckoutClick = onNavigateToCheckout
            )
            3 -> PedidosTab(
                modifier = Modifier.padding(innerPadding),
                onOrderClick = onNavigateToOrderDetail
            )
            4 -> BilleteraTab(
                modifier = Modifier.padding(innerPadding),
                onNavigateToOffers = onNavigateToOffers,
                onNavigateToHistorial = onNavigateToHistorial
            )
        }
    }
}

@Composable
private fun InicioTab(
    modifier: Modifier = Modifier,
    cartViewModel: CartViewModel,
    onProductClick: (String) -> Unit,
    onCategoryClick: (String) -> Unit,
    onPromotionsClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Bienvenido a CYRYEL",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Los mejores productos para ti",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        CatalogSection(
            onAddToCart = cartViewModel::addProduct,
            onProductClick = onProductClick,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun TiendaTab(
    modifier: Modifier = Modifier,
    cartViewModel: CartViewModel,
    onProductClick: (String) -> Unit,
    onSearchClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Tienda",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        CatalogSection(
            onAddToCart = cartViewModel::addProduct,
            onProductClick = onProductClick,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CarritoTab(
    modifier: Modifier = Modifier,
    cartViewModel: CartViewModel,
    onCheckoutClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Carrito",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        CartSection(
            modifier = Modifier.fillMaxWidth(),
            viewModel = cartViewModel,
            onCheckoutClick = onCheckoutClick
        )
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
