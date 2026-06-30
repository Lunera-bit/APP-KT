package com.cyryel.ui.checkout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cyryel.ui.cart.CartViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    onBack: () -> Unit,
    onOrderCreated: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: CartViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Checkout") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (uiState.items.isEmpty() && uiState.orderCreatedMessage == null) {
                Text(
                    text = "El carrito esta vacio",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 24.dp)
                )
                return@Column
            }

            if (uiState.items.isNotEmpty()) {
                Text(
                    text = "Resumen del pedido",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        uiState.items.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(item.productName, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = "x${item.quantity} @ S/ ${"%.2f".format(item.price)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "S/ ${"%.2f".format(item.subtotal)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Subtotal: S/ ${"%.2f".format(uiState.subtotal)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Tipo de entrega",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.onDeliveryMethodChange("domicilio") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (uiState.deliveryMethod == "domicilio") "✓ Delivery" else "Delivery")
                    }
                    OutlinedButton(
                        onClick = { viewModel.onDeliveryMethodChange("tienda") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (uiState.deliveryMethod == "tienda") "✓ Recojo en tienda" else "Recojo en tienda")
                    }
                }

                OutlinedTextField(
                    value = uiState.street,
                    onValueChange = viewModel::onStreetChange,
                    label = { Text("Direccion") },
                    isError = uiState.fieldErrors.containsKey("street"),
                    supportingText = uiState.fieldErrors["street"]?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = uiState.city,
                    onValueChange = viewModel::onCityChange,
                    label = { Text("Ciudad") },
                    isError = uiState.fieldErrors.containsKey("city"),
                    supportingText = uiState.fieldErrors["city"]?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = uiState.recipientName,
                    onValueChange = viewModel::onRecipientChange,
                    label = { Text("Nombre receptor") },
                    isError = uiState.fieldErrors.containsKey("recipientName"),
                    supportingText = uiState.fieldErrors["recipientName"]?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = uiState.phone,
                    onValueChange = viewModel::onPhoneChange,
                    label = { Text("Telefono") },
                    isError = uiState.fieldErrors.containsKey("phone"),
                    supportingText = uiState.fieldErrors["phone"]?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = uiState.notes,
                    onValueChange = viewModel::onNotesChange,
                    label = { Text("Notas") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (uiState.errorMessage != null) {
                    Text(
                        text = uiState.errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Button(
                    onClick = { viewModel.placeOrder() },
                    enabled = !uiState.isPlacingOrder,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    if (uiState.isPlacingOrder) {
                        CircularProgressIndicator(strokeWidth = 2.dp)
                    } else {
                        Text("Confirmar pedido - S/ ${"%.2f".format(uiState.subtotal)}")
                    }
                }
            }

            val orderCreated = uiState.orderCreatedMessage
            if (orderCreated != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Pedido creado",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = orderCreated,
                            modifier = Modifier.padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Button(
                            onClick = {
                                val orderId = orderCreated.substringAfter(": ").trim()
                                if (orderId.isNotBlank()) {
                                    onOrderCreated(orderId)
                                } else {
                                    onBack()
                                }
                            },
                            modifier = Modifier.padding(top = 12.dp)
                        ) {
                            Text("Ver pedido")
                        }
                    }
                }
            }
        }
    }
}
