package com.cyryel.ui.delivery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cyryel.R
import com.cyryel.data.delivery.DeliveryAssignment
import com.cyryel.data.order.Order
import com.cyryel.ui.theme.AmarilloVibrante
import com.cyryel.ui.theme.AzulRey
import com.cyryel.ui.theme.AzulReyClaro

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryMainScreen(
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DeliveryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    if (uiState.selectedDelivery != null) {
        DeliveryDetailScreen(
            onBack = { viewModel.clearSelection() },
            onAccept = {
                val pair = uiState.selectedDelivery ?: return@DeliveryDetailScreen
                viewModel.acceptDelivery(pair.first.id, pair.second.id)
            },
            onStartDelivery = {
                val pair = uiState.selectedDelivery ?: return@DeliveryDetailScreen
                viewModel.startDelivery(pair.first.id, pair.second.id)
            },
            onCompleteDelivery = { code ->
                val pair = uiState.selectedDelivery ?: return@DeliveryDetailScreen
                viewModel.completeDelivery(pair.first.id, pair.second.id, code)
            }
        )
        return
    }

    val tabs = listOf(
        TabData("Pedidos", R.drawable.ic_shop),
        TabData("Perfil", R.drawable.ic_profile)
    )

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Repartidor") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AzulRey,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(painterResource(tab.iconRes), contentDescription = tab.label)
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
            0 -> DeliveryPedidosTab(
                modifier = Modifier.padding(innerPadding),
                uiState = uiState,
                onAccept = { deliveryId, orderId ->
                    viewModel.acceptDelivery(deliveryId, orderId)
                },
                onDeliveryClick = { assignment, order ->
                    viewModel.selectDelivery(assignment, order)
                }
            )
            1 -> DeliveryPerfilTab(
                isAvailable = uiState.isAvailable,
                onToggleAvailability = { viewModel.toggleAvailability() },
                modifier = Modifier.padding(innerPadding),
                onSignOut = onSignOut
            )
        }
    }
}

private data class TabData(val label: String, val iconRes: Int)

@Composable
private fun DeliveryPedidosTab(
    modifier: Modifier = Modifier,
    uiState: DeliveryUiState,
    onAccept: (String, String) -> Unit,
    onDeliveryClick: (DeliveryAssignment, Order) -> Unit
) {
    var innerTab by remember { mutableIntStateOf(0) }
    val innerTabs = listOf("Disponibles", "Mis Pedidos", "Historial")

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = innerTab, containerColor = AzulRey, contentColor = Color.White) {
            innerTabs.forEachIndexed { index, title ->
                Tab(
                    selected = innerTab == index,
                    onClick = { innerTab = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (innerTab == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (innerTab == index) AmarilloVibrante else Color.White.copy(alpha = 0.7f)
                        )
                    }
                )
            }
        }

        when (innerTab) {
            0 -> PedidosDisponiblesContent(
                deliveries = uiState.availableDeliveries,
                isLoading = uiState.isLoading,
                onAccept = onAccept,
                onDeliveryClick = onDeliveryClick
            )
            1 -> MisPedidosContent(
                deliveries = uiState.myDeliveries.filter { it.first.status != "entregado" },
                isLoading = uiState.isLoading,
                onDeliveryClick = onDeliveryClick
            )
            2 -> HistorialContent(
                deliveries = uiState.myDeliveries.filter { it.first.status == "entregado" },
                isLoading = uiState.isLoading,
                onDeliveryClick = onDeliveryClick
            )
        }
    }
}

@Composable
private fun PedidosDisponiblesContent(
    deliveries: List<Pair<DeliveryAssignment, Order>>,
    isLoading: Boolean,
    onAccept: (String, String) -> Unit,
    onDeliveryClick: (DeliveryAssignment, Order) -> Unit
) {
    if (isLoading && deliveries.isEmpty()) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator()
        }
        return
    }

    if (deliveries.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "No hay pedidos disponibles",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Los pedidos apareceran aqui cuando esten listos para repartir",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(deliveries, key = { it.first.id }) { (assignment, order) ->
            DeliveryOrderCard(
                order = order,
                assignment = assignment,
                isAcceptButton = false,
                onClick = { onDeliveryClick(assignment, order) },
                onAccept = { onAccept(assignment.id, order.id) }
            )
        }
    }
}

@Composable
private fun MisPedidosContent(
    deliveries: List<Pair<DeliveryAssignment, Order>>,
    isLoading: Boolean,
    onDeliveryClick: (DeliveryAssignment, Order) -> Unit
) {
    if (isLoading && deliveries.isEmpty()) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator()
        }
        return
    }

    if (deliveries.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "No tienes pedidos activos",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(deliveries, key = { it.first.id }) { (assignment, order) ->
            DeliveryOrderCard(
                order = order,
                assignment = assignment,
                isAcceptButton = false,
                onClick = { onDeliveryClick(assignment, order) }
            )
        }
    }
}

@Composable
private fun HistorialContent(
    deliveries: List<Pair<DeliveryAssignment, Order>>,
    isLoading: Boolean,
    onDeliveryClick: (DeliveryAssignment, Order) -> Unit
) {
    if (isLoading && deliveries.isEmpty()) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) { CircularProgressIndicator() }
        return
    }

    if (deliveries.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Sin historial",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Los pedidos entregados apareceran aqui",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(deliveries, key = { it.first.id }) { (assignment, order) ->
            DeliveryOrderCard(
                order = order,
                assignment = assignment,
                isAcceptButton = false,
                onClick = { onDeliveryClick(assignment, order) }
            )
        }
    }
}

@Composable
private fun DeliveryOrderCard(
    order: Order,
    assignment: DeliveryAssignment,
    isAcceptButton: Boolean,
    onClick: () -> Unit = {},
    onAccept: () -> Unit = {}
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Pedido #${order.id.take(8)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = AzulReyClaro
                )
                Text(
                    text = "S/ ${"%.2f".format(order.total)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = order.deliveryAddress.street,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (order.deliveryAddress.reference.isNotBlank()) {
                Text(
                    text = "Ref: ${order.deliveryAddress.reference}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = "${order.items.size} producto(s) - ${order.items.sumOf { it.quantity }} unidades",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (assignment.status != "disponible") {
                Spacer(Modifier.height(4.dp))
                val statusText = when (assignment.status) {
                    "aceptado" -> "Estado: Aceptado"
                    "en_camino" -> "Estado: En camino"
                    "entregado" -> "Estado: Entregado"
                    else -> "Estado: ${assignment.status}"
                }
                val statusColor = when (assignment.status) {
                    "entregado" -> Color(0xFF2E7D32)
                    else -> AmarilloVibrante
                }
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = statusColor
                )
            }

            if (isAcceptButton) {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onAccept,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AzulRey)
                ) {
                    Text("Aceptar pedido", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DeliveryPerfilTab(
    isAvailable: Boolean,
    onToggleAvailability: () -> Unit,
    modifier: Modifier = Modifier,
    onSignOut: () -> Unit
) {

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Disponibilidad",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AzulRey
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isAvailable) "Disponible para recoger pedidos" else "No disponible",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked = isAvailable,
                        onCheckedChange = { onToggleAvailability() }
                    )
                }
            }
        }

        HorizontalDivider()

        Button(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Cerrar sesion", fontWeight = FontWeight.Bold)
        }
    }
}
