# Plan de Implementación: CRM de Engagement (Automatizaciones)

## Descripción General

CRM integrado dentro de la app Android que automatiza la relación con el cliente:
alertas de productos favoritos, recomendaciones personalizadas, programa de fidelidad
y recordatorios de stock/promociones. **No es un panel admin** — son automatizaciones
que la app ejecuta para cada usuario basándose en su comportamiento de compra.

---

## Stack Existente (no se modifica)

| Componente | Tecnología |
|------------|-----------|
| Backend | Firebase Cloud Functions (TypeScript, Node 20) |
| Base de datos | Firestore |
| Notificaciones | FCM (Firebase Cloud Messaging) |
| App Android | Kotlin + Jetpack Compose + Hilt |
| Auth | Firebase Auth + Google Sign-In |
| Navegación | Navigation Compose (role-based) |

---

## Arquitectura General

```
┌─────────────────────────────────────────────────────┐
│                    FIREBASE                         │
│                                                     │
│  ┌─────────────┐  ┌──────────────┐  ┌───────────┐ │
│  │ Firestore    │  │ Cloud Funcs  │  │    FCM    │ │
│  │              │  │              │  │           │ │
│  │ users/{uid}  │  │ analyzeUser  │  │ Push      │ │
│  │  └─crmData   │  │ Favorites    │  │ personali-│ │
│  │ orders/{id}  │  │ checkFavorite│  │ zadas     │ │
│  │ products/{id}│  │ Promotions   │  │           │ │
│  │ promotions/  │  │ checkFavorite│  └───────────┘ │
│  │              │  │ Restock      │                 │
│  └──────────────┘  │ sendLoyalty  │                 │
│                    │ Milestones   │                 │
│                    └──────────────┘                 │
└─────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────┐
│                APP ANDROID                          │
│                                                     │
│  ┌──────────┐  ┌──────────┐  ┌──────────────────┐ │
│  │ Home     │  │ Billetera│  │ Notificaciones   │ │
│  │ "Para ti"│  │ "Tu nivel│  │ Tipos CRM        │ │
│  │ favoritos│  │  fidelid"│  │ (favoritos,      │ │
│  └──────────┘  └──────────┘  │  hitos, stock)   │ │
│                              └──────────────────┘ │
└─────────────────────────────────────────────────────┘
```

---

## Modelo de Datos Firestore

### Nueva subcolección: `users/{uid}/crmData`

```typescript
// users/{uid}/crmData (documento único)
{
  favoriteProducts: [
    {
      productId: "abc123",
      productName: "Shampoo XYZ",
      frequency: 5,          // cuántas veces lo compró
      lastBoughtAt: Timestamp // última fecha de compra
    }
    // ... top 5 productos más frecuentes
  ],
  totalOrders: 12,           // total de órdenes completadas
  totalSpent: 1580.50,       // monto total acumulado
  loyaltyTier: "frecuente",  // nuevo | frecuente | vip
  updatedAt: Timestamp
}
```

### Cálculo de loyaltyTier

| Tier | Condición | Beneficio |
|------|-----------|-----------|
| `nuevo` | 0-2 compras completadas | — |
| `frecuente` | 3-9 compras completadas | Badge "⭐ Frecuente" |
| `vip` | 10+ compras completadas | Badge "🏆 VIP" + notificaciones prioritarias |

---

## Fase 1: Cloud Functions (Backend)

**Archivo:** `functions/src/index.ts`

### 1.1 `analyzeUserFavorites`

```
Tipo:       pubsub.schedule("0 2 * * *")  // diario a las 2am Lima
Propósito:  Analizar patrones de compra y actualizar crmData
```

**Lógica:**
1. Obtener todos los usuarios con `role == "user"`
2. Para cada usuario:
   a. Query `orders` donde `userId == uid` y `status == "entregado"`
   b. Iterar items de cada orden, contar frecuencia por `productId`
   c. Top 5 productos → guardar en `crmData.favoriteProducts`
   d. Calcular `totalOrders`, `totalSpent`, `loyaltyTier`
3. Actualizar documento `users/{uid}/crmData`

**Firestore queries necesarias:**
```
orders.where("userId", "==", uid).where("status", "==", "entregado")
```

**Nota:** Usar `limit(100)` por usuario para evitar timeouts. Los usuarios con más de 100 órdenes se procesan en la siguiente ejecución con paginación.

### 1.2 `checkFavoritePromotions`

```
Tipo:       firestore.document("promotions/{promotionId}").onUpdate
Propósito:  Notificar a usuarios cuando una promoción afecta su producto favorito
```

**Trigger:** Cuando cambia `isActive` de `false` a `true`, o cuando `stockRemaining` pasa de 0 a > 0.

**Lógica:**
1. Obtener productos de la promoción (`promotion.products[].productId`)
2. Query `users` donde `crmData.favoriteProducts` contenga alguno de esos productIds
3. Para cada usuario encontrado:
   a. Crear notificación en `notificaciones` collection
   b. Enviar FCM personalizada con deep link a la promoción

**Payload FCM:**
```json
{
  "notification": {
    "title": "🔥 ¡Tu producto favorito está en oferta!",
    "body": "{productName} con {discountPercent}% de descuento"
  },
  "data": {
    "type": "favorite_on_promo",
    "promotionId": "...",
    "productId": "...",
    "link": "cyryel://promotion/{promotionId}"
  }
}
```

**Optimización:** Para evitar duplicados, verificar si ya existe una notificación para ese usuario+promoción en las últimas 24h.

### 1.3 `checkFavoriteRestock`

```
Tipo:       firestore.document("products/{productId}").onUpdate
Propósito:  Notificar cuando un producto favorito vuelve a stock
```

**Trigger:** Cuando `stock` pasa de 0 a > 0.

**Lógica:**
1. Obtener el productId del documento actualizado
2. Query `users` donde `crmData.favoriteProducts.productId == productId`
3. Para cada usuario:
   a. Crear notificación en Firestore
   b. Enviar FCM personalizada

**Payload FCM:**
```json
{
  "notification": {
    "title": "✅ ¡Tu producto favorito está de vuelta!",
    "body": "{productName} ya está disponible nuevamente"
  },
  "data": {
    "type": "favorite_back_in_stock",
    "productId": "...",
    "link": "cyryel://product/{productId}"
  }
}
```

**Nota:** El query `crmData.favoriteProducts` necesita un índice compuesto. Alternativa: usar `arrayContains` en el campo `productId` dentro de los elementos del array.

### 1.4 `sendLoyaltyMilestones`

```
Tipo:       pubsub.schedule("0 3 * * *")  // diario a las 3am Lima
Propósito:  Enviar recompensas por hitos de fidelidad
```

**Lógica:**
1. Obtener todos los usuarios con `role == "user"`
2. Para cada usuario:
   a. Contar órdenes con `status == "entregado"`
   b. Verificar si alcanzó un hito nuevo (5, 10, 20, 50, 100 compras)
   c. Si sí: crear notificación + otorgar beneficio

**Hítos y beneficios:**

| Compras | Beneficio | Notificación |
|---------|-----------|-------------|
| 5 | Badge "Frecuente" + 5 puntos bonus | "¡Llegaste a 5 compras! Te damos 5 puntos de regalo" |
| 10 | Badge "VIP" + 15 puntos bonus | "Eres VIP 🏆 ¡15 puntos extra por tu fidelidad!" |
| 20 | 30 puntos bonus | "20 compras contigo. ¡30 puntos de regalo!" |
| 50 | 50 puntos + badge "Leyenda" | "¡Eres una leyenda! 50 puntos extra" |
| 100 | 100 puntos | "100 compras. ¡Gracias por tu confianza! 100 puntos" |

**Persistencia de hitos:** Guardar en `crmData.lastMilestone` el último hito alcanzado para no repetir notificaciones.

---

## Fase 2: Data Layer Android

### 2.1 Nuevo archivo: `data/crm/CrmData.kt`

```kotlin
package com.CYRYEL.com.data.crm

data class CrmData(
    val favoriteProducts: List<FavoriteProduct> = emptyList(),
    val totalOrders: Int = 0,
    val totalSpent: Double = 0.0,
    val loyaltyTier: String = "nuevo",
    val updatedAt: Long = 0L
)

data class FavoriteProduct(
    val productId: String = "",
    val productName: String = "",
    val frequency: Int = 0,
    val lastBoughtAt: Long = 0L
)
```

### 2.2 Modificar: `data/user/UserRepository.kt`

```kotlin
// Agregar:
suspend fun getCrmData(userId: String): Result<CrmData>
```

### 2.3 Modificar: `data/user/FirebaseUserRepository.kt`

```kotlin
// Implementar getCrmData:
override suspend fun getCrmData(userId: String): Result<CrmData> {
    return try {
        val doc = firestore.collection("users")
            .document(userId)
            .collection("crmData")
            .document("profile")
            .get().await()
        if (doc.exists()) {
            // parsear crmData...
            Result.success(crmData)
        } else {
            Result.success(CrmData()) // default
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

### 2.4 Modificar: `data/product/ProductRepository.kt`

```kotlin
// Agregar:
suspend fun getProductsByIds(ids: List<String>): Result<List<Product>>
```

### 2.5 Modificar: `data/product/FirebaseProductRepository.kt`

```kotlin
// Implementar getProductsByIds:
override suspend fun getProductsByIds(ids: List<String>): Result<List<Product>> {
    return try {
        if (ids.isEmpty()) return Result.success(emptyList())
        // Firestore limita `in` a 10 elementos, hacer batches
        val products = mutableListOf<Product>()
        ids.chunked(10).forEach { chunk ->
            val snapshot = firestore.collection("products")
                .whereIn(FieldPath.documentId(), chunk)
                .get().await()
            snapshot.documents.mapNotNull { parseProduct(it) }.let { products.addAll(it) }
        }
        Result.success(products)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

### 2.6 Modificar: `di/FirebaseModule.kt`

No se necesitan cambios — `CrmData` es un data class puro, y los repos ya están registrados.

---

## Fase 3: UI — Sección "Para ti" en Home

### 3.1 Modificar: `ui/home/HomeViewModel.kt`

```kotlin
// Agregar a HomeUiState:
data class HomeUiState(
    // ... existente ...
    val favoriteProducts: List<Product> = emptyList(),
    val loyaltyTier: String = "nuevo",
    val totalOrders: Int = 0
)

// Agregar método:
fun loadCrmData() {
    val userId = authRepository.getCurrentUserId() ?: return
    viewModelScope.launch {
        val crmResult = userRepository.getCrmData(userId)
        if (crmResult.isSuccess) {
            val crmData = crmResult.getOrNull() ?: return@launch
            val productIds = crmData.favoriteProducts.map { it.productId }
            val products = productRepository.getProductsByIds(productIds).getOrDefault(emptyList())
            _uiState.update {
                it.copy(
                    favoriteProducts = products,
                    loyaltyTier = crmData.loyaltyTier,
                    totalOrders = crmData.totalOrders
                )
            }
        }
    }
}

// Llamar en init o en loadData()
```

### 3.2 Modificar: `ui/home/MainScreen.kt`

Agregar sección después de "Pedido rápido":

```kotlin
// Sección "Tus favoritos"
if (homeState.favoriteProducts.isNotEmpty()) {
    SectionHeader(
        title = "Tus favoritos",
        subtitle = when (homeState.loyaltyTier) {
            "vip" -> "🏆 VIP"
            "frecuente" -> "⭐ Frecuente"
            else -> ""
        }
    )
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(homeState.favoriteProducts) { product ->
            ProductFavoriteCard(
                product = product,
                onClick = { onNavigateToProduct(product.id) }
            )
        }
    }
}
```

### 3.3 Badge de tier en header del Home

```kotlin
// En el header (Row con logo), al lado del nombre:
when (homeState.loyaltyTier) {
    "vip" -> Badge(containerColor = Color(0xFFFFD700)) {
        Text("🏆 VIP", fontSize = 10.sp)
    }
    "frecuente" -> Badge(containerColor = AzulRey) {
        Text("⭐", fontSize = 10.sp)
    }
}
```

---

## Fase 4: UI — Notificaciones CRM

### 4.1 Modificar: `CyryelMessagingService.kt`

```kotlin
// En onMessageReceived, agregar manejo de tipos CRM:
"favorite_on_promo" -> {
    // Mostrar notificación con deep link a la promoción
    val promotionId = data["promotionId"]
    // Crear notification con intent a PromotionDetailScreen
}
"favorite_back_in_stock" -> {
    val productId = data["productId"]
    // Crear notification con intent a ProductDetailScreen
}
"loyalty_milestone" -> {
    // Mostrar notificación de hito alcanzado
}
```

### 4.2 Modificar: `ui/notifications/BandejaNotificacionesScreen.kt`

```kotlin
// Agregar casos para nuevos tipos:
"favorite_on_promo" -> {
    icon = Icons.Filled.LocalOffer
    color = Color(0xFFE91E63) // rosa
}
"favorite_back_in_stock" -> {
    icon = Icons.Filled.CheckCircle
    color = Color(0xFF4CAF50) // verde
}
"loyalty_milestone" -> {
    icon = Icons.Filled.EmojiEvents
    color = Color(0xFFFFD700) // dorado
}
```

---

## Fase 5: UI — Fidelidad en Billetera

### 5.1 Modificar: `ui/billetera/BilleteraViewModel.kt`

```kotlin
// Agregar al init:
fun loadCrmData() {
    val userId = authRepository.getCurrentUserId() ?: return
    viewModelScope.launch {
        val crmResult = userRepository.getCrmData(userId)
        if (crmResult.isSuccess) {
            _uiState.update {
                it.copy(crmData = crmResult.getOrNull())
            }
        }
    }
}

// Agregar a BilleteraUiState:
val crmData: CrmData? = null
```

### 5.2 Modificar: `ui/billetera/BilleteraScreen.kt`

```kotlin
// Nueva sección "Tu nivel de fidelidad":
Card(
    modifier = Modifier.fillMaxWidth().padding(16.dp)
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Tu nivel", style = MaterialTheme.typography.titleMedium)

        // Tier actual
        val tierLabel = when (crmData?.loyaltyTier) {
            "vip" -> "🏆 VIP"
            "frecuente" -> "⭐ Frecuente"
            else -> "🆕 Nuevo"
        }
        Text(tierLabel, style = MaterialTheme.typography.headlineSmall)

        // Barra de progreso al siguiente hito
        val currentOrders = crmData?.totalOrders ?: 0
        val nextMilestone = MILESTONES.firstOrNull { it > currentOrders } ?: currentOrders
        val prevMilestone = MILESTORIES.lastOrNull { it <= currentOrders } ?: 0
        val progress = if (nextMilestone > prevMilestone) {
            (currentOrders - prevMilestone).toFloat() / (nextMilestone - prevMilestone)
        } else 1f

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        )

        Text(
            "$currentOrders de $nextMilestone compras para el siguiente nivel",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

companion object {
    val MILESTONES = listOf(5, 10, 20, 50, 100)
}
```

---

## Fase 6: Firestore Security Rules

```javascript
// Agregar regla para crmData (solo el dueño puede leer):
match /users/{userId}/crmData/{document} {
  allow read: if request.auth != null && request.auth.uid == userId;
  allow write: if false; // Solo Cloud Functions puede escribir
}
```

---

## Fase 7: Firestore Indexes

```json
// firestore.indexes.json — agregar:
{
  "collectionGroup": "users",
  "fields": [
    { "fieldPath": "role", "order": "ASCENDING" },
    { "fieldPath": "fcmToken", "order": "ASCENDING" }
  ]
}
```

---

## Fase 8: Despliegue

### Cloud Functions
```bash
cd functions
npm run build
firebase deploy --only functions
```

### Firestore Rules
```bash
firebase deploy --only firestore:rules
```

### Firestore Indexes
```bash
firebase deploy --only firestore:indexes
```

---

## Cronograma Sugerido

| Semana | Fase | Descripción |
|--------|------|-------------|
| 1 | Fase 1 | Cloud Functions: `analyzeUserFavorites` + `checkFavoritePromotions` |
| 1 | Fase 2 | Data Layer Android: `CrmData.kt` + repositorios |
| 2 | Fase 1 | Cloud Functions: `checkFavoriteRestock` + `sendLoyaltyMilestones` |
| 2 | Fase 3 | UI: Sección "Para ti" en Home |
| 3 | Fase 4 | UI: Notificaciones CRM en BandejaNotificaciones |
| 3 | Fase 5 | UI: Fidelidad en Billetera |
| 3 | Fase 6-7 | Security Rules + Indexes |
| 4 | Fase 8 | Despliegue + Testing completo |

---

## Métricas a Monitorear

| Métrica | Dónde | Objetivo |
|---------|-------|---------|
| Tasa de apertura de notificaciones CRM | FCM Console | > 30% |
| Clics en "Para ti" section | Firebase Analytics | > 15% de usuarios activos |
| Retención semanal (D7) | Firebase Analytics | Incremento vs baseline |
| Usuarios que alcanzan tier VIP | Firestore query | > 5% de usuarios activos |
| Notificaciones de favoritos → compra | Tracking cruzado | > 5% de conversión |

---

## Riesgos y Mitigaciones

| Riesgo | Impacto | Mitigación |
|--------|---------|-----------|
| Cloud Function timeout (> 5min) | Alto | Paginación de usuarios, procesar en lotes de 500 |
| Query `crmData.favoriteProducts` lento | Medio | Índices compuestos en Firestore |
| Notificaciones spam (demasiadas push) | Alto | Rate limit: máximo 1 notificación CRM por usuario por día |
| Datos de crmData desactualizados | Bajo | `analyzeUserFavorites` corre diariamente, tolerable |
| Firestore reads excesivos | Medio | Usar `limit()` en queries, cachear crmData en la app |

---

## Archivos Modificados (Resumen)

### Android (app/)
| Archivo | Tipo |
|---------|------|
| `data/crm/CrmData.kt` | **NUEVO** |
| `data/user/UserRepository.kt` | Modificar |
| `data/user/FirebaseUserRepository.kt` | Modificar |
| `data/product/ProductRepository.kt` | Modificar |
| `data/product/FirebaseProductRepository.kt` | Modificar |
| `ui/home/HomeViewModel.kt` | Modificar |
| `ui/home/MainScreen.kt` | Modificar |
| `ui/billetera/BilleteraViewModel.kt` | Modificar |
| `ui/billetera/BilleteraScreen.kt` | Modificar |
| `CyryelMessagingService.kt` | Modificar |
| `ui/notifications/BandejaNotificacionesScreen.kt` | Modificar |

### Cloud Functions (functions/)
| Archivo | Tipo |
|---------|------|
| `functions/src/index.ts` | Modificar (agregar 4 funciones) |

### Firebase
| Archivo | Tipo |
|---------|------|
| `firestore.rules` | Modificar (regla crmData) |
| `firestore.indexes.json` | Modificar (índice users) |

**Total: 1 archivo nuevo, 12 archivos modificados, 0 dependencias nuevas**
