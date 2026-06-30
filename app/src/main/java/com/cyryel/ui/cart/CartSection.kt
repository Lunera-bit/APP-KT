package com.cyryel.ui.cart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun CartSection(
    modifier: Modifier = Modifier,
    viewModel: CartViewModel = hiltViewModel(),
    onCheckoutClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = modifier) {
        Text(
            text = "Carrito",
            style = MaterialTheme.typography.titleLarge
        )

        if (uiState.items.isEmpty()) {
            Text(
                text = "Aun no agregaste productos",
                modifier = Modifier.padding(top = 8.dp)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.items.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.productName)
                            Text(
                                text = "${item.quantity} x S/ ${"%.2f".format(item.price)} = S/ ${"%.2f".format(item.subtotal)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(onClick = { viewModel.decreaseProduct(item.productId) }) {
                                Text("-")
                            }
                            Button(onClick = { viewModel.addProduct(item.product) }) {
                                Text("+")
                            }
                        }
                    }
                }
            }

            Text(
                text = "Subtotal: S/ ${"%.2f".format(uiState.subtotal)}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        Text(
            text = "Tipo de entrega",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 12.dp)
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            singleLine = true
        )

        OutlinedTextField(
            value = uiState.city,
            onValueChange = viewModel::onCityChange,
            label = { Text("Ciudad") },
            isError = uiState.fieldErrors.containsKey("city"),
            supportingText = uiState.fieldErrors["city"]?.let { { Text(it) } },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            singleLine = true
        )

        OutlinedTextField(
            value = uiState.recipientName,
            onValueChange = viewModel::onRecipientChange,
            label = { Text("Nombre receptor") },
            isError = uiState.fieldErrors.containsKey("recipientName"),
            supportingText = uiState.fieldErrors["recipientName"]?.let { { Text(it) } },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            singleLine = true
        )

        OutlinedTextField(
            value = uiState.phone,
            onValueChange = viewModel::onPhoneChange,
            label = { Text("Telefono") },
            isError = uiState.fieldErrors.containsKey("phone"),
            supportingText = uiState.fieldErrors["phone"]?.let { { Text(it) } },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            singleLine = true
        )

        OutlinedTextField(
            value = uiState.notes,
            onValueChange = viewModel::onNotesChange,
            label = { Text("Notas") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )

        val errorMsg = uiState.errorMessage
        if (errorMsg != null) {
            Text(
                text = errorMsg,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        val orderMsg = uiState.orderCreatedMessage
        if (orderMsg != null) {
            Text(
                text = orderMsg,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Button(
            onClick = onCheckoutClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        ) {
            Text("Ir al checkout")
        }
    }
}
