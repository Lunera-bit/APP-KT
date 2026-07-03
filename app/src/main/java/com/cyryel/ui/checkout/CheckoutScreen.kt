package com.cyryel.ui.checkout

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import kotlin.random.Random
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Geocoder
import com.cyryel.ui.util.showToast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.TextButton
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.viewinterop.AndroidView
import androidx.appcompat.content.res.AppCompatResources
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager

import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.cyryel.BuildConfig
import com.cyryel.R
import com.cyryel.ui.theme.AmarilloVibrante
import com.cyryel.ui.theme.AzulRey
import com.cyryel.ui.util.openWhatsAppWithOrder

private const val STORE_LATITUDE = -11.56544559
private const val STORE_LONGITUDE = -77.27104991

private val stepIcons = listOf(
    Icons.Filled.ShoppingCart,
    Icons.Filled.Home,
    Icons.Filled.Person,
    Icons.Filled.Check,
    Icons.Filled.Check
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    onBack: () -> Unit,
    onOrderCreated: (String) -> Unit = {},
    onGoHome: () -> Unit = onBack,
    modifier: Modifier = Modifier,
    viewModel: CheckoutViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Checkout", fontWeight = FontWeight.Bold) },
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
        }
    ) { innerPadding ->
        if (uiState.orderCreatedMessage != null) {
            OrderCreatedContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                uiState = uiState,
                onViewOrder = {
                    if (uiState.orderId.isNotBlank()) {
                        onOrderCreated(uiState.orderId)
                    } else {
                        onBack()
                    }
                },
                onGoHome = onGoHome
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                CheckoutStepper(
                    currentStep = uiState.currentStep,
                    onStepClick = { viewModel.goToStep(it) }
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AnimatedContent(
                        targetState = uiState.currentStep,
                        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                        label = "checkout_step"
                    ) { step ->
                        when (step) {
                            CheckoutStep.REVIEW -> StepReview(
                                items = uiState.items,
                                subtotal = uiState.subtotal
                            )
                            CheckoutStep.DELIVERY -> StepDelivery(
                                deliveryMethod = uiState.deliveryMethod,
                                street = uiState.street,
                                city = uiState.city,
                                reference = uiState.reference,
                                fieldErrors = uiState.fieldErrors,
                                onDeliveryMethodChange = viewModel::onDeliveryMethodChange,
                                onStreetChange = viewModel::onStreetChange,
                                onCityChange = viewModel::onCityChange,
                                onReferenceChange = viewModel::onReferenceChange
                            )
                            CheckoutStep.CONTACT -> StepContact(
                                recipientName = uiState.recipientName,
                                phone = uiState.phone,
                                notes = uiState.notes,
                                documentType = uiState.documentType,
                                documentNumber = uiState.documentNumber,
                                fieldErrors = uiState.fieldErrors,
                                onRecipientChange = viewModel::onRecipientChange,
                                onPhoneChange = viewModel::onPhoneChange,
                                onNotesChange = viewModel::onNotesChange,
                                onDocumentTypeChange = viewModel::onDocumentTypeChange,
                                onDocumentNumberChange = viewModel::onDocumentNumberChange
                            )
                            CheckoutStep.PAYMENT -> StepPayment(
                                paymentMethod = uiState.paymentMethod,
                                subtotal = uiState.subtotal,
                                onPaymentMethodChange = viewModel::onPaymentMethodChange
                            )
                            CheckoutStep.CONFIRM -> StepConfirm(
                                items = uiState.items,
                                subtotal = uiState.subtotal,
                                deliveryMethod = uiState.deliveryMethod,
                                street = uiState.street,
                                city = uiState.city,
                                recipientName = uiState.recipientName,
                                phone = uiState.phone,
                                notes = uiState.notes,
                                documentType = uiState.documentType,
                                documentNumber = uiState.documentNumber,
                                paymentMethod = uiState.paymentMethod
                            )
                        }
                    }
                }

                if (uiState.errorMessage != null) {
                    Text(
                        text = uiState.errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                BottomNavigationBar(
                    currentStep = uiState.currentStep,
                    isPlacingOrder = uiState.isPlacingOrder,
                    canProceed = when (uiState.currentStep) {
                        CheckoutStep.REVIEW -> uiState.canProceedFromStep0
                        CheckoutStep.DELIVERY -> uiState.canProceedFromStep1
                        CheckoutStep.CONTACT -> uiState.canProceedFromStep2
                        CheckoutStep.PAYMENT -> uiState.canProceedFromStep3
                        CheckoutStep.CONFIRM -> uiState.canProceedFromStep4
                    },
                    onNext = viewModel::nextStep,
                    onBack = { viewModel.goToStep(uiState.currentStep.previous()) },
                    onPlaceOrder = viewModel::placeOrder
                )
            }
        }
    }
}

@Composable
private fun CheckoutStepper(
    currentStep: CheckoutStep,
    onStepClick: (CheckoutStep) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            CheckoutStep.entries.forEachIndexed { index, step ->
                val isCompleted = step.ordinal < currentStep.ordinal
                val isCurrent = step == currentStep
                val bgColor = when {
                    isCompleted -> AzulRey
                    isCurrent -> AmarilloVibrante
                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                }
                val contentColor = when {
                    isCompleted -> Color.White
                    isCurrent -> AzulRey
                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(bgColor)
                            .clickable(enabled = isCompleted || isCurrent) { onStepClick(step) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCompleted) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = contentColor,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Text(
                                text = "${step.step + 1}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = contentColor
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = step.title.split(" ").first(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isCurrent) AzulRey else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomNavigationBar(
    currentStep: CheckoutStep,
    isPlacingOrder: Boolean,
    canProceed: Boolean,
    onNext: () -> Unit,
    onPlaceOrder: () -> Unit,
    onBack: () -> Unit = {}
) {
    val isFirstStep = currentStep == CheckoutStep.REVIEW
    val isLastStep = currentStep == CheckoutStep.CONFIRM
    val buttonLabel = if (isLastStep) {
        if (isPlacingOrder) "Creando pedido..." else "Confirmar pedido"
    } else {
        "Continuar"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!isFirstStep) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Atras", fontWeight = FontWeight.Medium)
                }
            }
            Button(
                onClick = if (isLastStep) onPlaceOrder else onNext,
                enabled = canProceed && !isPlacingOrder,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isLastStep) Color(0xFF2E7D32) else AzulRey,
                    contentColor = Color.White
                )
            ) {
                if (isPlacingOrder) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        text = buttonLabel,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    if (!isLastStep) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepReview(
    items: List<com.cyryel.data.cart.CartItem>,
    subtotal: Double
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Revisa tu pedido",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AzulRey
        )
        Text(
            text = "Verifica que los productos y cantidades sean correctos",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        items.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.productName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (item.variantName != null) {
                            Text(
                                text = item.variantName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "S/ ${"%.2f".format(item.price)} x ${item.quantity}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                Text(
                    text = "S/ ${"%.2f".format(item.subtotal)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = AzulRey
                )
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = AzulRey.copy(alpha = 0.05f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Subtotal",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "S/ ${"%.2f".format(subtotal)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = AzulRey
                )
            }
        }
    }
}

@Composable
private fun StepDelivery(
    deliveryMethod: String,
    street: String,
    city: String,
    reference: String,
    fieldErrors: Map<String, String>,
    onDeliveryMethodChange: (String) -> Unit,
    onStreetChange: (String) -> Unit,
    onCityChange: (String) -> Unit,
    onReferenceChange: (String) -> Unit
) {
    var latitude by remember { mutableDoubleStateOf(STORE_LATITUDE) }
    var longitude by remember { mutableDoubleStateOf(STORE_LONGITUDE) }
    var placedMarker by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Tipo de entrega",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AzulRey
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DeliveryMethodCard(
                selected = deliveryMethod == "domicilio",
                icon = Icons.Filled.Home,
                title = "Delivery",
                description = "Recibe en tu domicilio",
                onClick = { onDeliveryMethodChange("domicilio") },
                modifier = Modifier.weight(1f)
            )
            DeliveryMethodCard(
                selected = deliveryMethod == "tienda",
                icon = Icons.Filled.Home,
                title = "Recojo en tienda",
                description = "Recoge en nuestra tienda",
                onClick = { onDeliveryMethodChange("tienda") },
                modifier = Modifier.weight(1f)
            )
        }
        if (deliveryMethod == "domicilio") {
            var showMapPicker by remember { mutableStateOf(false) }
            var pickerLat by remember { mutableDoubleStateOf(latitude) }
            var pickerLng by remember { mutableDoubleStateOf(longitude) }
            var pickerStreet by remember { mutableStateOf(street) }
            var pickerCity by remember { mutableStateOf(city) }
            var pickerRef by remember { mutableStateOf(reference) }

            Button(
                onClick = {
                    pickerLat = latitude
                    pickerLng = longitude
                    pickerStreet = street
                    pickerCity = city
                    pickerRef = reference
                    showMapPicker = true
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AzulRey)
            ) {
                Icon(Icons.Filled.Home, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Elegir ubicación en el mapa")
            }

            if (showMapPicker) {
                MapPickerDialog(
                    initialLat = pickerLat,
                    initialLng = pickerLng,
                    initialStreet = pickerStreet,
                    initialCity = pickerCity,
                    initialRef = pickerRef,
                    onConfirm = { lat, lng, addr, cty, ref ->
                        latitude = lat
                        longitude = lng
                        placedMarker = true
                        onStreetChange(addr)
                        onCityChange(cty)
                        onReferenceChange(ref)
                        showMapPicker = false
                    },
                    onDismiss = { showMapPicker = false }
                )
            }

            if (placedMarker) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = AzulRey.copy(alpha = 0.08f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Ubicación seleccionada:", fontWeight = FontWeight.Bold, color = AzulRey)
                        if (street.isNotBlank()) Text("Dirección: $street")
                        if (city.isNotBlank()) Text("Ciudad: $city")
                        if (reference.isNotBlank()) Text("Referencia: $reference")
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Home,
                        contentDescription = null,
                        tint = AzulRey,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Recogeras tu pedido en nuestra tienda fisica. Te notificaremos cuando este listo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun MapPickerDialog(
    initialLat: Double,
    initialLng: Double,
    initialStreet: String,
    initialCity: String,
    initialRef: String,
    onConfirm: (Double, Double, String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var latitude by remember { mutableDoubleStateOf(initialLat) }
    var longitude by remember { mutableDoubleStateOf(initialLng) }
    var street by remember { mutableStateOf(initialStreet) }
    var city by remember { mutableStateOf(initialCity) }
    var reference by remember { mutableStateOf(initialRef) }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var annotationManager by remember { mutableStateOf<com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        mapboxMap.loadStyleUri("mapbox://styles/mapbox/standard") { style ->
                            try {
                                val pinBitmap = vectorToBitmap(ctx, R.drawable.ic_pin)
                                if (pinBitmap != null) style.addImage("pin-icon", pinBitmap)
                                mapboxMap.setCamera(
                                    CameraOptions.Builder()
                                        .center(Point.fromLngLat(longitude, latitude))
                                        .zoom(14.0)
                                        .pitch(0.0)
                                        .build()
                                )
                            } catch (_: Exception) { }
                        }
                        setOnTouchListener { _, event ->
                            if (event.action == android.view.MotionEvent.ACTION_UP) {
                                val screenPoint = com.mapbox.maps.ScreenCoordinate(
                                    event.x.toDouble(), event.y.toDouble()
                                )
                                val point = mapboxMap.coordinateForPixel(screenPoint)
                                latitude = point.latitude()
                                longitude = point.longitude()
                                mapboxMap.setCamera(
                                    CameraOptions.Builder()
                                        .center(Point.fromLngLat(point.longitude(), point.latitude()))
                                        .zoom(14.0)
                                        .pitch(0.0)
                                        .build()
                                )
                                annotationManager?.deleteAll()
                                annotationManager?.create(
                                    PointAnnotationOptions()
                                        .withPoint(Point.fromLngLat(point.longitude(), point.latitude()))
                                        .withIconImage("pin-icon")
                                )
                                reverseGeocodeAddress(
                                    context, point.latitude(), point.longitude(),
                                    onAddressFound = { addr, cty ->
                                        if (addr != null) street = addr
                                        if (cty != null) city = cty
                                    }
                                )
                            }
                            false
                        }
                        annotationManager = annotations.createPointAnnotationManager()
                        if (initialLat != STORE_LATITUDE || initialLng != STORE_LONGITUDE) {
                            annotationManager?.create(
                                PointAnnotationOptions()
                                    .withPoint(Point.fromLngLat(initialLng, initialLat))
                                    .withIconImage("pin-icon")
                            )
                        }
                        mapView = this
                        post {
                            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                                this.onResume()
                            } else if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                                this.onStart()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { }
            )

            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_RESUME -> mapView?.onResume()
                        Lifecycle.Event.ON_PAUSE -> { }
                        Lifecycle.Event.ON_STOP -> mapView?.onStop()
                        Lifecycle.Event.ON_DESTROY -> mapView?.onDestroy()
                        else -> {}
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Color.White.copy(alpha = 0.95f),
                        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Ubicación de entrega", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AzulRey)
                OutlinedTextField(
                    value = street,
                    onValueChange = { street = it },
                    label = { Text("Dirección") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("Ciudad") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = reference,
                    onValueChange = { reference = it },
                    label = { Text("Referencia (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Button(
                    onClick = { onConfirm(latitude, longitude, street, city, reference) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AzulRey)
                ) {
                    Text("Confirmar ubicación")
                }
            }
        }
    }
}

private fun reverseGeocodeAddress(
    context: Context,
    lat: Double,
    lng: Double,
    onAddressFound: (String?, String?) -> Unit
) {
    try {
        val geocoder = Geocoder(context)
        val addresses = geocoder.getFromLocation(lat, lng, 1)
        if (!addresses.isNullOrEmpty()) {
            val addr = addresses[0]
            val street = listOfNotNull(
                addr.thoroughfare,
                addr.subThoroughfare
            ).joinToString(" ").ifBlank {
                addr.getAddressLine(0)
            }
            val city = addr.locality ?: addr.subAdminArea ?: addr.adminArea
            onAddressFound(street, city)
        } else {
            onAddressFound(null, null)
        }
    } catch (_: Exception) {
        onAddressFound(null, null)
    }
}

@Composable
private fun DeliveryMethodCard(
    selected: Boolean,
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (selected) AzulRey else MaterialTheme.colorScheme.outlineVariant
    val bgColor = if (selected) AzulRey.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
    val iconTint = if (selected) AzulRey else MaterialTheme.colorScheme.onSurfaceVariant

    OutlinedCard(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(if (selected) 2.dp else 1.dp, borderColor),
        colors = CardDefaults.outlinedCardColors(containerColor = bgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (selected) "✓ $title" else title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) AzulRey else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun StepContact(
    recipientName: String,
    phone: String,
    notes: String,
    documentType: String,
    documentNumber: String,
    fieldErrors: Map<String, String>,
    onRecipientChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onDocumentTypeChange: (String) -> Unit,
    onDocumentNumberChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Informacion de contacto",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AzulRey
        )
        Text(
            text = "Quien recibira el pedido?",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = recipientName,
            onValueChange = onRecipientChange,
            label = { Text("Nombre completo del receptor") },
            isError = fieldErrors.containsKey("recipientName"),
            supportingText = fieldErrors["recipientName"]?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = phone,
            onValueChange = { onPhoneChange(it.filter { c -> c.isDigit() }.take(9)) },
            label = { Text("Telefono (9 digitos)") },
            isError = fieldErrors.containsKey("phone"),
            supportingText = fieldErrors["phone"]?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            prefix = { Text("+51 ") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Text(
            text = "Documento de identidad",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DocumentTypeCard(
                selected = documentType == "dni",
                title = "DNI",
                onClick = { onDocumentTypeChange("dni") },
                modifier = Modifier.weight(1f)
            )
            DocumentTypeCard(
                selected = documentType == "ruc",
                title = "RUC",
                onClick = { onDocumentTypeChange("ruc") },
                modifier = Modifier.weight(1f)
            )
        }
        OutlinedTextField(
            value = documentNumber,
            onValueChange = {
                val maxLen = if (documentType == "dni") 8 else 11
                onDocumentNumberChange(it.filter { c -> c.isDigit() }.take(maxLen))
            },
            label = { Text(if (documentType == "dni") "Numero de DNI" else "Numero de RUC") },
            placeholder = { Text(if (documentType == "dni") "8 digitos" else "11 digitos") },
            isError = fieldErrors.containsKey("documentNumber"),
            supportingText = fieldErrors["documentNumber"]?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            label = { Text("Notas del pedido (opcional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4
        )
    }
}

@Composable
private fun DocumentTypeCard(
    selected: Boolean,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (selected) AzulRey else MaterialTheme.colorScheme.outlineVariant
    val bgColor = if (selected) AzulRey.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface

    OutlinedCard(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(if (selected) 2.dp else 1.dp, borderColor),
        colors = CardDefaults.outlinedCardColors(containerColor = bgColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selected) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = AzulRey,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) AzulRey else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun StepPayment(
    paymentMethod: String,
    subtotal: Double,
    onPaymentMethodChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Metodo de pago",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AzulRey
        )
        Text(
            text = "Selecciona como deseas pagar",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        PaymentOptionCard(
            selected = paymentMethod == "contra_entrega",
            icon = Icons.Filled.ShoppingCart,
            title = "Pago contra entrega",
            description = "Paga en efectivo al recibir tu pedido",
            onClick = { onPaymentMethodChange("contra_entrega") }
        )
        PaymentOptionCard(
            selected = paymentMethod == "codigo",
            icon = Icons.Filled.Home,
            title = "Pago por codigo",
            description = "Genera un codigo de pago y pagalo en cualquier agente",
            onClick = { onPaymentMethodChange("codigo") }
        )
        if (paymentMethod == "contra_entrega") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = AmarilloVibrante.copy(alpha = 0.15f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Pagas cuando recibes tu pedido. Aceptamos efectivo y Yape.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (paymentMethod == "codigo") {
            val context = LocalContext.current
            val accounts = listOf(
                BankAccount("BBVA", "0011-0264-0200275841"),
                BankAccount("BCP", "2957127650060"),
                BankAccount("InterBank", "00229500712765006041"),
                BankAccount("Yape / Plin", "925510147")
            )
            Text(
                text = "Selecciona una cuenta para transferir",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
            accounts.forEach { account ->
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = account.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = AzulRey
                            )
                            Text(
                                text = account.number,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText(account.name, account.number))
                                context.showToast("✓  Copiado: ${account.name}")
                            },
                            border = BorderStroke(1.dp, AzulRey)
                        ) {
                            Text("Copiar", color = AzulRey, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Titular: CYRYEL Eirl",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Monto a pagar: S/ ${"%.2f".format(subtotal)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AzulRey,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = AmarilloVibrante.copy(alpha = 0.15f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = AzulRey,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Luego de transferir, envianos el comprobante por WhatsApp para confirmar tu pedido.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private data class BankAccount(
    val name: String,
    val number: String
)

@Composable
private fun PaymentOptionCard(
    selected: Boolean,
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    val borderColor = if (selected) AzulRey else MaterialTheme.colorScheme.outlineVariant
    val bgColor = if (selected) AzulRey.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface

    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(if (selected) 2.dp else 1.dp, borderColor),
        colors = CardDefaults.outlinedCardColors(containerColor = bgColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) AzulRey else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) AzulRey else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (selected) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = AzulRey,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun StepConfirm(
    items: List<com.cyryel.data.cart.CartItem>,
    subtotal: Double,
    deliveryMethod: String,
    street: String,
    city: String,
    recipientName: String,
    phone: String,
    notes: String,
    documentType: String,
    documentNumber: String,
    paymentMethod: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Confirma tu pedido",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AzulRey
        )
        Text(
            text = "Revisa toda la informacion antes de confirmar",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        ConfirmSection(
            title = "Productos",
            content = {
                items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${item.productName} x${item.quantity}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "S/ ${"%.2f".format(item.subtotal)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Subtotal",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "S/ ${"%.2f".format(subtotal)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = AzulRey
                    )
                }
            }
        )

        ConfirmSection(
            title = "Entrega",
            content = {
                InfoRow("Metodo", if (deliveryMethod == "domicilio") "Delivery" else "Recojo en tienda")
                if (deliveryMethod == "domicilio") {
                    InfoRow("Direccion", street)
                    InfoRow("Ciudad", city)
                }
            }
        )

        ConfirmSection(
            title = "Contacto",
            content = {
                InfoRow("Receptor", recipientName)
                InfoRow("Telefono", phone)
                InfoRow(documentType.uppercase(), documentNumber)
                if (notes.isNotBlank()) InfoRow("Notas", notes)
            }
        )

        ConfirmSection(
            title = "Pago",
            content = {
                InfoRow(
                    "Metodo",
                    if (paymentMethod == "contra_entrega") "Pago contra entrega" else "Pago por codigo"
                )
            }
        )
    }
}

@Composable
private fun ConfirmSection(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = AzulRey
            )
            Spacer(Modifier.height(6.dp))
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun HorizontalDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    )
}

@Composable
private fun OrderCreatedContent(
    modifier: Modifier = Modifier,
    uiState: CheckoutUiState,
    onViewOrder: () -> Unit,
    onGoHome: () -> Unit = onViewOrder
) {
    val context = LocalContext.current
    val composition by rememberLottieComposition(LottieCompositionSpec.Asset("success_check.json"))
    val textAlpha = remember { Animatable(0f) }
    val buttonsAlpha = remember { Animatable(0f) }
    var whatsAppOpened by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        textAlpha.animateTo(1f, animationSpec = tween(600))
        buttonsAlpha.animateTo(1f, animationSpec = tween(400))
    }

    LaunchedEffect(uiState.orderId) {
        if (uiState.orderId.isNotBlank() && !whatsAppOpened) {
            kotlinx.coroutines.delay(1500)
            openWhatsAppWithOrder(context, uiState)
            whatsAppOpened = true
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        ConfettiEffect()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            LottieAnimation(
                composition = composition,
                modifier = Modifier.size(140.dp),
                iterations = 1
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = uiState.orderCreatedMessage ?: "Pedido creado",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = AzulRey,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer(alpha = textAlpha.value)
            )

            if (uiState.orderId.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Nro. pedido: ${uiState.orderId}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = AzulRey.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.graphicsLayer(alpha = textAlpha.value)
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Gracias por tu compra. Te notificaremos cuando tu pedido sea procesado.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer(alpha = textAlpha.value)
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { openWhatsAppWithOrder(context, uiState) },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF25D366),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer(alpha = buttonsAlpha.value)
            ) {
                Text("Enviar por WhatsApp", fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onViewOrder,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AzulRey,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer(alpha = buttonsAlpha.value)
            ) {
                Text("Ver mi pedido", fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = onGoHome,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer(alpha = buttonsAlpha.value)
            ) {
                Text("Volver al inicio")
            }
        }
    }
}

private data class ConfettiParticle(
    val startX: Float,
    val startY: Float,
    val size: Float,
    val color: Color,
    val speed: Float,
    val sway: Float,
    val swaySpeed: Float
)

@Composable
private fun ConfettiEffect(modifier: Modifier = Modifier) {
    val particles = remember {
        (0 until 100).map {
            ConfettiParticle(
                startX = Random.nextFloat(),
                startY = Random.nextFloat(),
                size = Random.nextFloat() * 16f + 6f,
                color = Color(
                    red = Random.nextFloat(),
                    green = Random.nextFloat(),
                    blue = Random.nextFloat(),
                    alpha = 1f
                ),
                speed = Random.nextFloat() * 0.4f + 0.2f,
                sway = Random.nextFloat() * 80f + 30f,
                swaySpeed = Random.nextFloat() * 2f + 0.5f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        particles.forEach { p ->
            val raw = (time * p.speed + p.startY * 1000f) % 1000f / 1000f
            val y = raw * h * 1.2f - h * 0.1f
            val angle = (raw * p.swaySpeed * kotlin.math.PI * 3f).toFloat()
            val x = p.startX * w + kotlin.math.sin(angle.toDouble()).toFloat() * p.sway
            val alpha = if (raw > 0.9f) (1f - raw) * 10f else 1f
            drawRect(
                color = p.color.copy(alpha = alpha),
                topLeft = Offset(x, y),
                size = Size(p.size, p.size * 0.6f)
            )
        }
    }
}

private fun vectorToBitmap(context: Context, drawableRes: Int): Bitmap? {
    val drawable = AppCompatResources.getDrawable(context, drawableRes) ?: return null
    val bitmap = Bitmap.createBitmap(
        drawable.intrinsicWidth.takeIf { it > 0 } ?: 36,
        drawable.intrinsicHeight.takeIf { it > 0 } ?: 36,
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}
