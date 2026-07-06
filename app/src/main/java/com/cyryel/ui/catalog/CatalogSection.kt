package com.cyryel.ui.catalog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cyryel.data.product.Product
import com.cyryel.data.product.availableStock
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage

@Composable
fun CatalogSection(
    onAddToCart: (Product) -> Unit,
    onProductClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: CatalogViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val uiError = uiState.errorMessage
    val uiProductsEmpty = uiState.products.isEmpty()
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Catalogo",
                style = MaterialTheme.typography.titleLarge
            )
            OutlinedButton(onClick = viewModel::loadProducts) {
                Text("Recargar")
            }
        }

        when {
            uiState.isLoading -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiError != null -> {
                Text(
                    text = uiError,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            uiProductsEmpty -> {
                Text(
                    text = "No hay productos disponibles",
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.products, key = { it.id }) { product ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onProductClick(product.id) }
                                .padding(12.dp)
                        ) {
                            if (product.foto.isNotBlank()) {
                                AsyncImage(
                                    model = product.foto,
                                    contentDescription = product.nombre,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                            Text(
                                text = product.nombre,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Text(
                                text = "S/ ${"%.2f".format(product.precio)}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Stock: ${product.availableStock} ${product.unidad}",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Button(
                                onClick = { onAddToCart(product) },
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Text("Agregar al carrito")
                            }
                        }
                    }
                }
            }
        }
    }
}
