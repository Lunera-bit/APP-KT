package com.cyryel.ui.search

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.cyryel.data.category.Category
import com.cyryel.data.product.Product
import com.cyryel.ui.theme.AzulRey
import com.cyryel.ui.theme.AzulReyClaro

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onProductClick: (String) -> Unit,
    onCategoryClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Buscar") },
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
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)
        ) {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = viewModel::onQueryChange,
                label = { Text("Buscar productos...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(16.dp))

            if (uiState.showCategories) {
                CategoriesGrid(
                    categories = uiState.categories,
                    onCategoryClick = onCategoryClick
                )
            } else {
                SearchResults(
                    isLoading = uiState.isLoading,
                    results = uiState.results,
                    errorMessage = uiState.errorMessage,
                    hasSearched = uiState.hasSearched,
                    onProductClick = onProductClick
                )
            }
        }
    }
}

@Composable
private fun CategoriesGrid(
    categories: List<Category>,
    onCategoryClick: (String) -> Unit
) {
    if (categories.isEmpty()) {
        Text(
            text = "No hay categorias disponibles",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        Text(
            text = "Categorias",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AzulReyClaro
        )
        Spacer(Modifier.height(12.dp))
        categories.chunked(2).forEach { rowCats ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowCats.forEach { category ->
                    CategoryCard(
                        category = category,
                        onClick = { onCategoryClick(category.name) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowCats.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun CategoryCard(
    category: Category,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(130.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (category.colorStart.isNotBlank())
                parseHexColor(category.colorStart)
            else AzulRey
        )
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            if (category.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = category.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(80.dp),
                    contentScale = ContentScale.Fit
                )
            }
            Text(
                text = category.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SearchResults(
    isLoading: Boolean,
    results: List<Product>,
    errorMessage: String?,
    hasSearched: Boolean,
    onProductClick: (String) -> Unit
) {
    when {
        isLoading -> {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(color = AzulRey)
            }
        }

        errorMessage != null -> {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error
            )
        }

        results.isNotEmpty() -> {
            Text(
                text = "${results.size} resultado(s) encontrado(s)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(results, key = { it.id }) { product ->
                    ProductSearchItem(
                        product = product,
                        onClick = { onProductClick(product.id) }
                    )
                }
            }
        }

        hasSearched -> {
            Text(
                text = "No se encontraron productos",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProductSearchItem(
    product: Product,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(8.dp)) {
            if (product.foto.isNotBlank()) {
                AsyncImage(
                    model = product.foto,
                    contentDescription = product.nombre,
                    modifier = Modifier
                        .height(70.dp)
                        .padding(end = 12.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(product.nombre, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    text = "S/ ${"%.2f".format(product.precio)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun parseHexColor(hex: String): Color {
    return try {
        val clean = hex.removePrefix("#")
        Color(("FF$clean").toLong(radix = 16))
    } catch (_: Exception) {
        AzulRey
    }
}
