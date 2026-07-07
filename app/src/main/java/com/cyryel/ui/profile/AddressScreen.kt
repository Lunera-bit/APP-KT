package com.cyryel.ui.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Geocoder
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.compose.foundation.text.KeyboardOptions
import androidx.appcompat.content.res.AppCompatResources
import com.CYRYEL.com.BuildConfig
import com.CYRYEL.com.R
import com.cyryel.data.auth.AuthRepository
import com.cyryel.data.user.Address
import com.cyryel.data.user.UserRepository
import com.cyryel.ui.theme.AzulRey
import com.cyryel.ui.theme.AzulReyClaro
import com.cyryel.ui.util.showToast
import com.google.android.gms.location.LocationServices
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.common.MapboxOptions
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class AddressUiState(
    val isLoading: Boolean = true,
    val addresses: List<Address> = emptyList(),
    val showForm: Boolean = false,
    val editingAddress: Address? = null,
    val formName: String = "",
    val formType: String = "home",
    val formStreet: String = "",
    val formCity: String = "",
    val formLatitude: Double = -11.56545313746308,
    val formLongitude: Double = -77.27110282305334,
    val formReference: String = "",
    val formIsDefault: Boolean = false,
    val isSaving: Boolean = false,
    val deleteConfirmId: String? = null,
    val showReplaceDefaultDialog: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class AddressViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddressUiState())
    val uiState: StateFlow<AddressUiState> = _uiState.asStateFlow()

    init {
        loadAddresses()
    }

    private fun loadAddresses() {
        val userId = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = userRepository.getUser(userId)
            if (result.isSuccess) {
                val user = result.getOrNull()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        addresses = user?.addresses ?: emptyList()
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = result.exceptionOrNull()?.localizedMessage
                    )
                }
            }
        }
    }

    fun showAddForm() {
        _uiState.update {
            it.copy(
                showForm = true,
                editingAddress = null,
                formName = "",
                formType = "home",
                formStreet = "",
                formCity = "",
                formReference = "",
                formLatitude = -11.56545313746308,
                formLongitude = -77.27110282305334,
                formIsDefault = it.addresses.isEmpty()
            )
        }
    }

    fun showEditForm(address: Address) {
        _uiState.update {
            it.copy(
                showForm = true,
                editingAddress = address,
                formName = address.name,
                formType = address.type,
                formStreet = address.street,
                formCity = address.city,
                formReference = address.reference,
                formLatitude = address.latitude,
                formLongitude = address.longitude,
                formIsDefault = address.isDefault
            )
        }
    }

    fun hideForm() {
        _uiState.update { it.copy(showForm = false, editingAddress = null) }
    }

    fun onFormNameChange(value: String) {
        _uiState.update { it.copy(formName = value) }
    }

    fun onFormTypeChange(type: String) {
        _uiState.update { it.copy(formType = type) }
    }

    fun onFormStreetChange(value: String) {
        _uiState.update { it.copy(formStreet = value) }
    }

    fun onFormCityChange(value: String) {
        _uiState.update { it.copy(formCity = value) }
    }

    fun onFormReferenceChange(value: String) {
        _uiState.update { it.copy(formReference = value) }
    }

    fun onFormLatitudeChange(value: Double) {
        _uiState.update { it.copy(formLatitude = value) }
    }

    fun onFormLongitudeChange(value: Double) {
        _uiState.update { it.copy(formLongitude = value) }
    }

    fun onFormIsDefaultChange(isDefault: Boolean) {
        val state = _uiState.value
        if (isDefault && state.addresses.any { it.isDefault && it.id != state.editingAddress?.id }) {
            _uiState.update { it.copy(showReplaceDefaultDialog = true) }
        } else {
            _uiState.update { it.copy(formIsDefault = isDefault) }
        }
    }

    fun confirmReplaceDefault() {
        _uiState.update { it.copy(formIsDefault = true, showReplaceDefaultDialog = false) }
    }

    fun cancelReplaceDefault() {
        _uiState.update { it.copy(showReplaceDefaultDialog = false) }
    }

    fun updateFromMap(lat: Double, lng: Double, street: String, city: String) {
        _uiState.update {
            it.copy(
                formLatitude = lat,
                formLongitude = lng,
                formStreet = street.ifBlank { it.formStreet },
                formCity = city.ifBlank { it.formCity }
            )
        }
    }

    fun saveAddress() {
        val state = _uiState.value
        val userId = authRepository.getCurrentUserId() ?: return

        if (state.formStreet.isBlank()) {
            _uiState.update { it.copy(errorMessage = "La direccion es requerida") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            val currentAddresses = state.addresses.toMutableList()
            val targetId: String

            if (state.editingAddress != null) {
                targetId = state.editingAddress.id
                val index = currentAddresses.indexOfFirst { it.id == targetId }
                if (index != -1) {
                    currentAddresses[index] = currentAddresses[index].copy(
                        name = state.formName,
                        type = state.formType,
                        street = state.formStreet,
                        city = state.formCity,
                        reference = state.formReference,
                        latitude = state.formLatitude,
                        longitude = state.formLongitude,
                        isDefault = state.formIsDefault
                    )
                }
            } else {
                targetId = UUID.randomUUID().toString()
                val newAddress = Address(
                    id = targetId,
                    name = state.formName,
                    type = state.formType,
                    street = state.formStreet,
                    city = state.formCity,
                    reference = state.formReference,
                    latitude = state.formLatitude,
                    longitude = state.formLongitude,
                    isDefault = state.formIsDefault
                )
                currentAddresses.add(newAddress)
            }

            if (state.formIsDefault) {
                currentAddresses.replaceAll { it.copy(isDefault = it.id == targetId) }
            }

            val result = userRepository.updateUser(userId, mapOf("addresses" to currentAddresses))
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        showForm = false,
                        editingAddress = null,
                        addresses = currentAddresses,
                        successMessage = "Direccion guardada correctamente"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = result.exceptionOrNull()?.localizedMessage
                    )
                }
            }
        }
    }

    fun requestDelete(addressId: String) {
        _uiState.update { it.copy(deleteConfirmId = addressId) }
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun cancelDelete() {
        _uiState.update { it.copy(deleteConfirmId = null) }
    }

    fun confirmDelete() {
        val addressId = _uiState.value.deleteConfirmId ?: return
        val userId = authRepository.getCurrentUserId() ?: return

        viewModelScope.launch {
            val currentAddresses = _uiState.value.addresses.toMutableList()
            currentAddresses.removeAll { it.id == addressId }

            val result = userRepository.updateUser(userId, mapOf("addresses" to currentAddresses))
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        addresses = currentAddresses,
                        deleteConfirmId = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        deleteConfirmId = null,
                        errorMessage = result.exceptionOrNull()?.localizedMessage
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddressViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            context.showToast(it)
            viewModel.clearSuccessMessage()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Direcciones") },
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
        },
        floatingActionButton = {
            if (!uiState.showForm) {
                FloatingActionButton(
                    onClick = viewModel::showAddForm,
                    containerColor = AzulRey
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Agregar direccion")
                }
            }
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

            uiState.showForm -> {
                AddressForm(
                    state = uiState,
                    onNameChange = viewModel::onFormNameChange,
                    onTypeChange = viewModel::onFormTypeChange,
                    onStreetChange = viewModel::onFormStreetChange,
                    onCityChange = viewModel::onFormCityChange,
                    onReferenceChange = viewModel::onFormReferenceChange,
                    onIsDefaultChange = viewModel::onFormIsDefaultChange,
                    onUpdateFromMap = viewModel::updateFromMap,
                    onSave = viewModel::saveAddress,
                    onCancel = viewModel::hideForm,
                    modifier = Modifier.padding(innerPadding)
                )
            }

            else -> {
                if (uiState.addresses.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "No tienes direcciones guardadas",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Agrega una direccion para usarla en tus pedidos",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.addresses, key = { it.id }) { address ->
                            AddressItemCard(
                                address = address,
                                onEdit = { viewModel.showEditForm(address) },
                                onDelete = { viewModel.requestDelete(address.id) }
                            )
                        }
                    }
                }
            }
        }

        if (uiState.deleteConfirmId != null) {
            AlertDialog(
                onDismissRequest = viewModel::cancelDelete,
                title = { Text("Eliminar direccion") },
                text = { Text("¿Estas seguro de eliminar esta direccion?") },
                confirmButton = {
                    TextButton(onClick = viewModel::confirmDelete) {
                        Text("Eliminar", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::cancelDelete) {
                        Text("Cancelar")
                    }
                }
            )
        }

        if (uiState.showReplaceDefaultDialog) {
            AlertDialog(
                onDismissRequest = viewModel::cancelReplaceDefault,
                title = { Text("Reemplazar predeterminada") },
                text = { Text("Ya tienes una direccion predeterminada. ¿Deseas reemplazarla por esta?") },
                confirmButton = {
                    TextButton(onClick = viewModel::confirmReplaceDefault) {
                        Text("Si")
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::cancelReplaceDefault) {
                        Text("No")
                    }
                }
            )
        }
    }
}

@Composable
private fun AddressForm(
    state: AddressUiState,
    onNameChange: (String) -> Unit,
    onTypeChange: (String) -> Unit,
    onStreetChange: (String) -> Unit,
    onCityChange: (String) -> Unit,
    onReferenceChange: (String) -> Unit,
    onIsDefaultChange: (Boolean) -> Unit,
    onUpdateFromMap: (Double, Double, String, String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMap by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (state.editingAddress != null) "Editar direccion" else "Nueva direccion",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = state.formName,
            onValueChange = onNameChange,
            label = { Text("Nombre (ej: Casa, Tienda X)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Text(
            text = "Tipo de direccion",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = AzulReyClaro
        )
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onTypeChange("home") }
            ) {
                RadioButton(
                    selected = state.formType == "home",
                    onClick = { onTypeChange("home") }
                )
                Text("Casa")
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onTypeChange("work") }
            ) {
                RadioButton(
                    selected = state.formType == "work",
                    onClick = { onTypeChange("work") }
                )
                Text("Trabajo")
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onTypeChange("other") }
            ) {
                RadioButton(
                    selected = state.formType == "other",
                    onClick = { onTypeChange("other") }
                )
                Text("Otro")
            }
        }

        OutlinedTextField(
            value = state.formStreet,
            onValueChange = onStreetChange,
            label = { Text("Direccion *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = false
        )

        OutlinedTextField(
            value = state.formCity,
            onValueChange = onCityChange,
            label = { Text("Ciudad") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = false
        )

        OutlinedTextField(
            value = state.formReference,
            onValueChange = onReferenceChange,
            label = { Text("Referencia (opcional)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        if (state.formLatitude != 0.0 && state.formLongitude != 0.0) {
            Text(
                text = "Lat: ${"%.6f".format(state.formLatitude)}, Lng: ${"%.6f".format(state.formLongitude)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        OutlinedButton(
            onClick = { showMap = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Home, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Elegir ubicacion en el mapa")
        }

        if (showMap) {
            AddressMapPickerDialog(
                initialLat = state.formLatitude,
                initialLng = state.formLongitude,
                onConfirm = { lat, lng, addr, cty ->
                    onUpdateFromMap(lat, lng, addr, cty)
                    showMap = false
                },
                onDismiss = { showMap = false }
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onIsDefaultChange(!state.formIsDefault) }
        ) {
            RadioButton(
                selected = state.formIsDefault,
                onClick = { onIsDefaultChange(!state.formIsDefault) }
            )
            Text("Establecer como direccion por defecto")
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancelar")
            }
            Button(
                onClick = onSave,
                modifier = Modifier.weight(1f),
                enabled = !state.isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = AzulRey)
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Filled.Check, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Guardar")
                }
            }
        }
    }
}

@Composable
private fun AddressMapPickerDialog(
    initialLat: Double,
    initialLng: Double,
    onConfirm: (Double, Double, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var latitude by remember { mutableDoubleStateOf(initialLat) }
    var longitude by remember { mutableDoubleStateOf(initialLng) }
    var street by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var annotationManager by remember { mutableStateOf<com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    remember { MapboxOptions.accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN; Unit }

    var currentZoom by remember { mutableDoubleStateOf(18.0) }
    var currentPitch by remember { mutableDoubleStateOf(60.0) }

    var locationGranted by remember { mutableStateOf(false) }
    var requestingLocation by remember { mutableStateOf(false) }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        locationGranted = granted
        if (granted) requestingLocation = true
    }

    fun moveCamera(lat: Double, lng: Double, zoom: Double = currentZoom) {
        mapView?.mapboxMap?.setCamera(
            CameraOptions.Builder()
                .center(Point.fromLngLat(lng, lat))
                .zoom(zoom)
                .pitch(currentPitch)
                .build()
        )
    }

    fun placePin(lat: Double, lng: Double) {
        annotationManager?.deleteAll()
        annotationManager?.create(
            PointAnnotationOptions()
                .withPoint(Point.fromLngLat(lng, lat))
                .withIconImage("pin-icon")
        )
    }

    fun updateFromLocation(lat: Double, lng: Double) {
        latitude = lat
        longitude = lng
        moveCamera(lat, lng)
        placePin(lat, lng)
        try {
            val addresses = Geocoder(context).getFromLocation(lat, lng, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                street = listOfNotNull(addr.thoroughfare, addr.subThoroughfare).joinToString(" ").ifBlank { addr.getAddressLine(0) }
                city = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: ""
            }
        } catch (_: Exception) { }
    }

    LaunchedEffect(requestingLocation) {
        if (requestingLocation) {
            try {
                val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                fusedClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        updateFromLocation(location.latitude, location.longitude)
                    }
                }
            } catch (_: Exception) { }
            requestingLocation = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val surfaceColor = MaterialTheme.colorScheme.surface

        Box(modifier = Modifier.fillMaxSize().background(surfaceColor)) {
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        mapboxMap.loadStyleUri("mapbox://styles/hola231341/cmr55bvuc002n01qodr8m2j2p") { style ->
                            try {
                                val pinBitmap = vectorToBitmap(ctx, R.drawable.ic_pin)
                                if (pinBitmap != null) style.addImage("pin-icon", pinBitmap)
                                moveCamera(latitude, longitude)
                            } catch (_: Exception) { }
                        }
                        setOnTouchListener { _, event ->
                            if (event.action == android.view.MotionEvent.ACTION_UP) {
                                val screenPoint = com.mapbox.maps.ScreenCoordinate(event.x.toDouble(), event.y.toDouble())
                                val point = mapboxMap.coordinateForPixel(screenPoint)
                                updateFromLocation(point.latitude(), point.longitude())
                            }
                            false
                        }
                        annotationManager = annotations.createPointAnnotationManager()
                        if (initialLat != -11.56545313746308 || initialLng != -77.27110282305334) {
                            placePin(initialLat, initialLng)
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

            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 48.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = surfaceColor.copy(alpha = 0.95f))
            ) {
                Box(modifier = Modifier.padding(8.dp)) {
                    OutlinedTextField(
                        value = street,
                        onValueChange = {},
                        label = { Text("Direccion") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = false,
                        textStyle = TextStyle(fontSize = 13.sp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        currentZoom = (currentZoom + 0.5).coerceAtMost(22.0)
                        mapView?.mapboxMap?.setCamera(
                            CameraOptions.Builder()
                                .center(Point.fromLngLat(longitude, latitude))
                                .zoom(currentZoom)
                                .pitch(currentPitch)
                                .build()
                        )
                    },
                    modifier = Modifier.size(40.dp),
                    containerColor = surfaceColor.copy(alpha = 0.9f),
                    contentColor = AzulReyClaro
                ) { Text("+", fontSize = 22.sp, fontWeight = FontWeight.Bold) }

                FloatingActionButton(
                    onClick = {
                        currentZoom = (currentZoom - 0.5).coerceAtLeast(1.0)
                        mapView?.mapboxMap?.setCamera(
                            CameraOptions.Builder()
                                .center(Point.fromLngLat(longitude, latitude))
                                .zoom(currentZoom)
                                .pitch(currentPitch)
                                .build()
                        )
                    },
                    modifier = Modifier.size(40.dp),
                    containerColor = surfaceColor.copy(alpha = 0.9f),
                    contentColor = AzulReyClaro
                ) { Text("\u2212", fontSize = 22.sp, fontWeight = FontWeight.Bold) }

                FloatingActionButton(
                    onClick = {
                        currentPitch = if (currentPitch == 60.0) 0.0 else 60.0
                        mapView?.mapboxMap?.setCamera(
                            CameraOptions.Builder()
                                .center(Point.fromLngLat(longitude, latitude))
                                .zoom(currentZoom)
                                .pitch(currentPitch)
                                .build()
                        )
                    },
                    modifier = Modifier.size(40.dp),
                    containerColor = surfaceColor.copy(alpha = 0.9f),
                    contentColor = AzulReyClaro
                ) { Text(if (currentPitch == 60.0) "\u2B21" else "\u2B14", fontSize = 18.sp, fontWeight = FontWeight.Bold) }

                FloatingActionButton(
                    onClick = {
                        if (locationGranted) {
                            requestingLocation = true
                        } else {
                            locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    },
                    modifier = Modifier.size(40.dp),
                    containerColor = surfaceColor.copy(alpha = 0.9f),
                    contentColor = AzulReyClaro
                ) { Text("\u2316", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 85.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = surfaceColor, contentColor = AzulReyClaro),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Cerrar", fontWeight = FontWeight.Medium) }
                Button(
                    onClick = {
                        if (street.isBlank()) {
                            context.showToast("Seleccione una ubicacion en el mapa")
                        } else {
                            onConfirm(latitude, longitude, street, city)
                        }
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AzulRey),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Confirmar", fontWeight = FontWeight.Bold) }
            }
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

@Composable
private fun AddressItemCard(
    address: Address,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Home,
                contentDescription = null,
                tint = AzulReyClaro,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (address.name.isNotBlank()) {
                    Text(
                        text = address.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = AzulRey
                    )
                }
                Text(
                    text = address.street,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (address.city.isNotBlank()) {
                    Text(
                        text = address.city,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (address.reference.isNotBlank()) {
                    Text(
                        text = address.reference,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (address.isDefault) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "(Default)",
                        style = MaterialTheme.typography.bodySmall,
                        color = AzulRey,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = "Editar", tint = AzulReyClaro)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
