package com.cyryel.ui.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyryel.R
import com.cyryel.ui.theme.AmarilloVibrante
import com.cyryel.ui.theme.AzulRey
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.LineLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.viewinterop.AndroidView
import com.mapbox.geojson.Feature
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    orderId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OrderDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(orderId) {
        viewModel.loadOrder(orderId)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Detalle del Pedido") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atras")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Row(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.errorMessage != null -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = uiState.errorMessage ?: "", color = MaterialTheme.colorScheme.error)
                    Button(onClick = { viewModel.loadOrder(orderId) }, modifier = Modifier.padding(top = 16.dp)) {
                        Text("Reintentar")
                    }
                }
            }

            uiState.order != null -> {
                val order = uiState.order!!
                val dateFormat = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Pedido #${order.id.take(8)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Estado: ${order.status.replace("_", " ").replaceFirstChar { it.uppercase() }}",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Text(
                                text = dateFormat.format(Date(order.createdAt)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = "Productos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            order.items.forEachIndexed { index, item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = item.productName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (item.redeemedByPoints) {
                                                Box(
                                                    modifier = Modifier
                                                        .padding(start = 6.dp)
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
                                        if (item.redeemedByPoints) {
                                            Text(
                                                text = "${item.quantity} × ${item.pointsUsed} pts",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = AmarilloVibrante,
                                                fontWeight = FontWeight.Medium
                                            )
                                        } else {
                                            Text(
                                                text = "${item.quantity} × S/ ${"%.2f".format(item.price)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    if (item.redeemedByPoints) {
                                        Text(
                                            text = "${item.pointsUsed} pts",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = AmarilloVibrante,
                                            modifier = Modifier.padding(start = 12.dp)
                                        )
                                    } else {
                                        Text(
                                            text = "S/ ${"%.2f".format(item.subtotal)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(start = 12.dp)
                                        )
                                    }
                                }
                                if (index < order.items.lastIndex) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                }
                            }
                        }
                    }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            TotalRow("Subtotal", order.subtotal)
                            if (order.shipping > 0) TotalRow("Envio", order.shipping)
                            if (order.pointsDiscount > 0) {
                                TotalRow("Descuento puntos", -order.pointsDiscount)
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            TotalRow("Total", order.total, bold = true)
                        }
                    }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Direccion de entrega", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(order.deliveryAddress.street, style = MaterialTheme.typography.bodyMedium)
                            Text(order.deliveryAddress.city, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Recibe: ${order.deliveryAddress.recipientName}", style = MaterialTheme.typography.bodySmall)
                            Text("Tel: ${order.deliveryAddress.phone}", style = MaterialTheme.typography.bodySmall)

                            Spacer(Modifier.height(8.dp))
                            Text("Metodo de pago", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                order.paymentMethod.replace("_", " ").replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    if (order.deliveryMethod == "domicilio" && order.deliveryConfirmationCode.isNotBlank()) {
                        var showCode by remember { mutableStateOf(false) }
                        val borderColor = if (showCode) AzulRey else MaterialTheme.colorScheme.outlineVariant
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .drawBehind {
                                    drawRoundRect(
                                        color = borderColor,
                                        cornerRadius = CornerRadius(12.dp.toPx()),
                                        style = Stroke(
                                            width = 2.dp.toPx(),
                                            pathEffect = PathEffect.dashPathEffect(
                                                floatArrayOf(10.dp.toPx(), 6.dp.toPx())
                                            )
                                        )
                                    )
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (showCode)
                                    AzulRey.copy(alpha = 0.06f)
                                else
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            "Codigo de confirmacion",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = AzulRey
                                        )
                                    }
                                    TextButton(onClick = { showCode = !showCode }) {
                                        Text(
                                            if (showCode) "Ocultar" else "Mostrar",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                if (showCode) {
                                    Spacer(Modifier.height(12.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(AzulRey.copy(alpha = 0.1f))
                                            .padding(vertical = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            order.deliveryConfirmationCode,
                                            style = MaterialTheme.typography.displaySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = AzulRey,
                                            letterSpacing = 12.sp
                                        )
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "Comparte este codigo con el repartidor para confirmar la entrega",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "Toca \"Mostrar\" para ver el codigo de confirmacion",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    if (order.deliveryAcceptedAt != null || order.deliveryStartedAt != null) {
                        val items = listOfNotNull(
                            if (order.deliveryAcceptedAt != null)
                                "Aceptado por repartidor" to formatTimestamp(order.deliveryAcceptedAt)
                            else null,
                            if (order.deliveryStartedAt != null)
                                "Repartidor en camino" to formatTimestamp(order.deliveryStartedAt)
                            else null,
                            if (order.status == "entregado")
                                "Entregado" to formatTimestamp(order.updatedAt)
                            else null
                        )

                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Seguimiento del delivery",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = AzulRey
                                )
                                Spacer(Modifier.height(12.dp))
                                items.forEachIndexed { index, (label, time) ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF2E7D32),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.padding(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        Text(
                                            text = time,
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

                    if (uiState.deliveryLatitude != null && uiState.deliveryLongitude != null) {
                        DeliveryTrackMapCard(
                            destLatitude = order.deliveryAddress.latitude,
                            destLongitude = order.deliveryAddress.longitude,
                            deliveryLatitude = uiState.deliveryLatitude!!,
                            deliveryLongitude = uiState.deliveryLongitude!!
                        )
                    }

                    if (order.notes.isNotBlank()) {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Notas", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(order.notes, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    if (order.status == "pendiente" && !uiState.cancelSuccess) {
                        var showDialog by remember { mutableStateOf(false) }

                        if (showDialog) {
                            AlertDialog(
                                onDismissRequest = { showDialog = false },
                                title = { Text("Cancelar pedido") },
                                text = { Text("¿Estás seguro de cancelar este pedido? Esta acción no se puede deshacer.") },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            showDialog = false
                                            viewModel.cancelOrder(orderId)
                                        },
                                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                                    ) {
                                        Text("Sí, cancelar")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDialog = false }) {
                                        Text("No")
                                    }
                                }
                            )
                        }

                        OutlinedButton(
                            onClick = { showDialog = true },
                            enabled = !uiState.isCancelling,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                        ) {
                            if (uiState.isCancelling) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    color = Color.Red,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                            Text("Cancelar pedido")
                        }
                    }

                    if (uiState.cancelSuccess) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Pedido cancelado",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TotalRow(label: String, amount: Double, bold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = if (bold) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "S/ ${"%.2f".format(amount)}",
            style = if (bold) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = if (bold) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

private fun formatTimestamp(millis: Long?): String {
    if (millis == null) return "-"
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(millis))
}

@Composable
private fun DeliveryTrackMapCard(
    destLatitude: Double,
    destLongitude: Double,
    deliveryLatitude: Double,
    deliveryLongitude: Double
) {
    if (destLatitude == 0.0 && destLongitude == 0.0) return

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Ubicacion del repartidor",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = AzulRey
            )
            Spacer(Modifier.height(8.dp))

            Box(modifier = Modifier.fillMaxWidth().height(280.dp)) {
                var mapView by remember { mutableStateOf<MapView?>(null) }
                var destAnnotationMgr by remember { mutableStateOf<com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager?>(null) }
                var deliveryAnnotationMgr by remember { mutableStateOf<com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager?>(null) }
                var routeSource by remember { mutableStateOf<GeoJsonSource?>(null) }
                val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

                AndroidView(
                    factory = { ctx ->
                        MapView(ctx).apply {
                            mapboxMap.loadStyleUri("mapbox://styles/mapbox/streets-v12") { style ->
                                val destPoint = Point.fromLngLat(destLongitude, destLatitude)
                                val deliveryPoint = Point.fromLngLat(deliveryLongitude, deliveryLatitude)

                                val annotations = annotations
                                destAnnotationMgr = annotations.createPointAnnotationManager()
                                destAnnotationMgr?.create(
                                    PointAnnotationOptions().withPoint(destPoint).withIconSize(1.5)
                                )

                                deliveryAnnotationMgr = annotations.createPointAnnotationManager()
                                deliveryAnnotationMgr?.create(
                                    PointAnnotationOptions().withPoint(deliveryPoint).withIconSize(1.5)
                                )

                                val src = GeoJsonSource.Builder("route-source")
                                    .geometry(LineString.fromLngLats(emptyList()))
                                    .build()
                                style.addSource(src)
                                routeSource = src

                                style.addLayer(
                                    LineLayer("route-layer", "route-source")
                                        .lineColor(android.graphics.Color.parseColor("#0066FF"))
                                        .lineWidth(4.0)
                                        .lineOpacity(0.8)
                                )

                                mapboxMap.setCamera(
                                    CameraOptions.Builder()
                                        .center(destPoint)
                                        .zoom(12.0)
                                        .build()
                                )
                            }
                            mapView = this
                            post {
                                if (lifecycleOwner.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) {
                                    this@apply.onResume()
                                } else if (lifecycleOwner.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) {
                                    this@apply.onStart()
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { }
                )

                DisposableEffect(lifecycleOwner) {
                    val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                        when (event) {
                            androidx.lifecycle.Lifecycle.Event.ON_RESUME -> mapView?.onResume()
                            androidx.lifecycle.Lifecycle.Event.ON_STOP -> mapView?.onStop()
                            androidx.lifecycle.Lifecycle.Event.ON_DESTROY -> mapView?.onDestroy()
                            else -> {}
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                LaunchedEffect(deliveryLatitude, deliveryLongitude) {
                    val deliveryPoint = Point.fromLngLat(deliveryLongitude, deliveryLatitude)
                    deliveryAnnotationMgr?.deleteAll()
                    deliveryAnnotationMgr?.create(
                        PointAnnotationOptions().withPoint(deliveryPoint).withIconSize(1.5)
                    )

                    val routeJson = withContext(Dispatchers.IO) {
                        fetchRoute(destLatitude, destLongitude, deliveryLatitude, deliveryLongitude)
                    }
                    if (routeJson != null) {
                        val geometry = routeJson.geometry()
                        if (geometry != null) {
                            routeSource?.geometry(geometry)
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = "Destino: ${"%.4f".format(destLatitude)}, ${"%.4f".format(destLongitude)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Repartidor: ${"%.4f".format(deliveryLatitude)}, ${"%.4f".format(deliveryLongitude)}",
                style = MaterialTheme.typography.bodySmall,
                color = AzulRey
            )
        }
    }
}

private suspend fun fetchRoute(
    fromLat: Double, fromLng: Double,
    toLat: Double, toLng: Double
): Feature? {
    return try {
        val token = com.cyryel.BuildConfig.MAPBOX_ACCESS_TOKEN
        val url = "https://api.mapbox.com/directions/v5/mapbox/driving/$fromLng,$fromLat;$toLng,$toLat?geometries=geojson&access_token=$token&overview=full&steps=false"
        val result = java.net.URL(url).readText()
        val obj = com.google.gson.Gson().fromJson(result, com.google.gson.JsonObject::class.java)
        val routes = obj?.getAsJsonArray("routes")
        if (routes != null && routes.size() > 0) {
            val geometry = routes[0].asJsonObject.getAsJsonObject("geometry")
            Feature.fromJson(geometry.toString())
        } else null
    } catch (_: Exception) {
        null
    }
}
