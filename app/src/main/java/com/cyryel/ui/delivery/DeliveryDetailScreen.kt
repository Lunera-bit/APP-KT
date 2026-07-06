package com.cyryel.ui.delivery

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cyryel.data.delivery.DeliveryAssignment
import com.cyryel.data.order.Order
import com.cyryel.ui.theme.AmarilloVibrante
import com.cyryel.ui.theme.AzulRey

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryDetailScreen(
    onBack: () -> Unit,
    onAccept: () -> Unit,
    onStartDelivery: () -> Unit,
    onCompleteDelivery: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DeliveryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pair = uiState.selectedDelivery ?: return

    val (assignment, order) = pair
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showRationale by remember { mutableStateOf(false) }
    var permissionDenied by remember { mutableStateOf(false) }
    var permanentlyDenied by remember { mutableStateOf(false) }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            permissionDenied = false
            permanentlyDenied = false
            onAccept()
        } else {
            permissionDenied = true
            if (!ActivityCompat.shouldShowRequestPermissionRationale(
                    context as android.app.Activity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                permanentlyDenied = true
            }
        }
    }

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

    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false },
            title = { Text("Permiso de ubicacion necesario") },
            text = {
                Column {
                    Text(
                        "Para aceptar pedidos de delivery necesitamos acceder a tu ubicacion:\n\n" +
                        "• Asignarte pedidos cercanos\n" +
                        "• Compartir ubicacion en tiempo real con el cliente\n" +
                        "• Registrar punto de recogida y entrega\n\n" +
                        "Tu ubicacion solo se usa mientras tienes pedidos activos.\n\n" +
                        "IMPORTANTE: Para que funcione correctamente en segundo plano:\n" +
                        "1. Desactiva \"Pausar actividad de la app si no se usa\"\n" +
                        "2. Configura bateria como \"Sin restricciones\"\n\n" +
                        "Puedes hacerlo desde el boton de abajo."
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Abrir configuracion de la app")
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    showRationale = false
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }) {
                    Text("Continuar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRationale = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = modifier,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text("Detalle del pedido") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atras")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = AzulRey,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OrderInfoCard(order)
                DeliveryAddressCard(order, context)
                MapCard(order, context)
                PaymentInfoCard(order)
                ItemsCard(order)
                DeliveryTimelineCard(assignment)

                when (assignment.status) {
                    "disponible" -> {
                        Spacer(Modifier.height(4.dp))
                        Button(
                            onClick = {
                                if (context.checkSelfPermission(
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    permissionDenied = false
                                    permanentlyDenied = false
                                    onAccept()
                                } else {
                                    showRationale = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AzulRey)
                        ) {
                            Text("Aceptar pedido", fontWeight = FontWeight.Bold)
                        }
                        if (permissionDenied && !permanentlyDenied) {
                            Spacer(Modifier.height(8.dp))
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Text(
                                    text = "Permiso denegado temporalmente. Vuelve a intentarlo.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                        if (permanentlyDenied) {
                            Spacer(Modifier.height(8.dp))
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "El permiso de ubicacion fue denegado permanentemente. Para aceptar pedidos, activalo en Configuracion.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = {
                                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                data = Uri.fromParts("package", context.packageName, null)
                                            }
                                            context.startActivity(intent)
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Abrir Configuracion")
                                    }
                                }
                            }
                        }
                    }
                    "aceptado" -> {
                        Spacer(Modifier.height(4.dp))
                        Button(
                            onClick = onStartDelivery,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AzulRey)
                        ) {
                            Text("Recogi el pedido", fontWeight = FontWeight.Bold)
                        }
                    }
                    "en_camino" -> {
                        Spacer(Modifier.height(4.dp))
                        var code by remember { mutableStateOf("") }
                        OutlinedTextField(
                            value = code,
                            onValueChange = { if (it.length <= 4) code = it },
                            label = { Text("Codigo de confirmacion") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { onCompleteDelivery(code) },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            enabled = code.length == 4,
                            colors = ButtonDefaults.buttonColors(containerColor = AzulRey)
                        ) {
                            Text("Entregar pedido", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.padding(32.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(color = AzulRey)
                        Text(
                            text = uiState.loadingMessage ?: "Cargando...",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderInfoCard(order: Order) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AzulRey.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Pedido #${order.id.take(8)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AzulRey
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${order.items.size} producto(s) - ${order.items.sumOf { it.quantity }} unidades",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Total: S/ ${"%.2f".format(order.total)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = AzulRey
            )
        }
    }
}

@Composable
private fun DeliveryAddressCard(order: Order, context: android.content.Context) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Direccion de entrega",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = AzulRey
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = order.deliveryAddress.street,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = order.deliveryAddress.city,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (order.deliveryAddress.reference.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Ref: ${order.deliveryAddress.reference}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 4.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = order.deliveryAddress.recipientName.ifBlank { "Cliente" },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (order.deliveryAddress.phone.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${order.deliveryAddress.phone}"))
                        context.startActivity(intent)
                    }
                ) {
                    Icon(
                        Icons.Filled.Phone,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp),
                        tint = AzulRey
                    )
                    Text(
                        text = order.deliveryAddress.phone,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AzulRey
                    )
                }
            }
        }
    }
}

@Composable
private fun MapCard(order: Order, context: android.content.Context) {
    val lat = order.deliveryAddress.latitude
    val lng = order.deliveryAddress.longitude

    if (lat == 0.0 && lng == 0.0) return

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Ubicacion",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = AzulRey
            )
            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    val uri = "geo:0,0?q=$lat,$lng"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                    intent.setPackage("com.google.android.apps.maps")
                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                    } else {
                        val webUri = "https://www.google.com/maps/search/?api=1&query=$lat,$lng"
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webUri)))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null)
                Spacer(Modifier.padding(4.dp))
                Text("Ver ubicacion", fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    val uri = "google.navigation:q=$lat,$lng"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                    intent.setPackage("com.google.android.apps.maps")
                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                    } else {
                        val webUri = "https://www.google.com/maps/dir/?api=1&destination=$lat,$lng"
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webUri)))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34A853)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null)
                Spacer(Modifier.padding(4.dp))
                Text("Navegar", fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Lat: ${"%.6f".format(lat)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Lng: ${"%.6f".format(lng)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PaymentInfoCard(order: Order) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Metodo de pago",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = AzulRey
            )
            Spacer(Modifier.height(8.dp))
            val paymentLabel = when (order.paymentMethod) {
                "contra_entrega" -> "Contra entrega"
                "codigo" -> "Codigo de pago"
                else -> order.paymentMethod
            }
            val paymentStatusLabel = when (order.paymentStatus) {
                "pendiente" -> "Pendiente"
                "completado" -> "Completado"
                "fallido" -> "Fallido"
                else -> order.paymentStatus
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Metodo: $paymentLabel",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = paymentStatusLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = when (order.paymentStatus) {
                        "completado" -> Color(0xFF2E7D32)
                        "pendiente" -> AmarilloVibrante
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}

@Composable
private fun ItemsCard(order: Order) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Productos",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = AzulRey
            )
            Spacer(Modifier.height(8.dp))

            order.items.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.ShoppingCart,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Column {
                            Text(
                                text = item.productName,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "x${item.quantity}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = "S/ ${"%.2f".format(item.subtotal)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (index < order.items.lastIndex) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total", fontWeight = FontWeight.Bold)
                Text(
                    text = "S/ ${"%.2f".format(order.total)}",
                    fontWeight = FontWeight.Bold,
                    color = AzulRey
                )
            }
        }
    }
}

private fun formatTimestamp(millis: Long?): String? {
    if (millis == null || millis == 0L) return null
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(millis))
}

@Composable
private fun DeliveryTimelineCard(assignment: DeliveryAssignment) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    val items = listOfNotNull(
        "Pedido creado" to formatTimestamp(assignment.assignedAt),
        if (assignment.acceptedAt != null) "Aceptado" to dateFormat.format(Date(assignment.acceptedAt)) else null,
        if (assignment.startedAt != null) "Recogido" to dateFormat.format(Date(assignment.startedAt)) else null,
        if (assignment.deliveredAt != null) "Entregado" to dateFormat.format(Date(assignment.deliveredAt)) else null
    )

    if (items.size <= 1) return

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Estado del delivery",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = AzulRey
            )
            Spacer(Modifier.height(12.dp))
            val dotColor = MaterialTheme.colorScheme.outlineVariant
            items.forEachIndexed { index, (label, time) ->
                val isCompleted = time != null && when (label) {
                    "Pedido creado" -> true
                    "Aceptado" -> assignment.acceptedAt != null
                    "Recogido" -> assignment.startedAt != null
                    "Entregado" -> assignment.deliveredAt != null
                    else -> false
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isCompleted) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .drawBehind {
                                    drawCircle(
                                        color = dotColor,
                                        radius = 6.dp.toPx()
                                    )
                                }
                        )
                    }
                    Spacer(Modifier.padding(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isCompleted) FontWeight.Medium else FontWeight.Normal,
                            color = if (isCompleted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = time ?: "-",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (index < items.lastIndex) {
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}
