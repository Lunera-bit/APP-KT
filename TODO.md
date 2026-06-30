# TODO — Migración Ionic → Kotlin Nativo (Shop Usuario)

## Estado actual

Build: ✅ **Compila sin errores** (29/06/2026)
Checkout: ✅ **5 pasos completos con UI rediseñada** — paso 3 simplificado, paso 4 con copia al portapapeles, paso 5 con Atras+Confirmar lado a lado
Mapbox: ✅ **Click-to-place con CircleAnnotation, GPS FAB, sin moverse al desplazar**
Autenticación: ✅ **Usuario creado en Firestore tras Google Sign-In + auto-fill checkout**

---

## Fase 1: Fundación ✅ (Completada)

| # | Tarea | Estado |
|---|---|---|
| **1.1** | **Unificar navegación — NavHost + BottomNav (4 tabs)** | ✅ |
| **1.2** | **Persistir carrito con SavedStateHandle** | ✅ |
| **1.3** | **Crear usuario en Firestore tras login + auto-fill checkout** | ✅ |
| 1.4 | Agregar `.limit()` a queries Firestore | ⏳ Pendiente |

### Lo que se hizo en 1.1
- `MainScreen.kt` con `Scaffold` + `NavigationBar` (4 tabs: Inicio, Pedidos, Billetera, Perfil)
- Inner `NavHost` para cambiar entre tabs
- `AuthScreen.kt` simplificado: solo login + `onNavigateToMain`
- `HomeV2.kt`: eliminado BottomNavBar, onSignOut, onCartClick
- `NavGraph.kt`: rutas `auth`, `main`, `product/{productId}`, `profile`, `settings`, `checkout`
- `MainActivity.kt`: usa `AppNavGraph` con `rememberNavController`
- `getProductById` en repositorios
- `ProductDetailViewModel.kt`: carga producto por ID desde Firestore

### Lo que se hizo en 1.2
- `CartViewModel`: `SavedStateHandle` con serialización JSON (org.json)
- `CartManager`: `getTotalQuantityInCart()` y `restoreItems()`
- Stock validation: `inCart + quantity > stock`
- Feedback Snackbar: `CartFeedback`/`AddToCartFeedback` sealed classes

### Lo que se hizo en 1.3
- `AuthViewModel`: crea documento `users/{uid}` en Firestore tras Google Sign-In
- `UserRepository`/`FirebaseUserRepository`: guarda nombre, email, uid
- `CartViewModel.loadUserProfile()`: auto-fill checkout (nombre, teléfono, documento, dirección) desde `users/{uid}`

### Íconos SVG
- 7 SVGs de `svg/` convertidos a VectorDrawable XML en `res/drawable/`
- Bottom nav: `ic_home`, `ic_cart`, `ic_gift`, `ic_profile`
- Header Home: `ic_cart` con badge + `ic_bell` para notificaciones
- `indicatorColor`: `Color.White.copy(alpha = 0.15f)`
- Search icon: `ic_search.xml` (reemplazo de unicode `⌕`)
- Pin map: `ic_pin.xml`, GPS crosshair: `ic_my_location.xml`
- Logo: `logo.png` en drawable para header HomeV2

---

## Fase 2: Inicio + Categorías 🟡

| # | Tarea | Estado |
|---|---|---|
| 2.1 | BottomNavigationView con 4 tabs funcionales | ✅ |
| 2.2 | Flash Banners slider (colección `flash`) con auto-scroll | ⏳ |
| 2.3 | "Pedido rápido" slider horizontal con random + "Agregar todo" | 🟡 Parcial |
| 2.4 | Categories grid 2 columnas con colores gradiente | 🟡 Parcial |
| 2.5 | Category picker modal (bottom sheet) con búsqueda | ⏳ |
| 2.6 | **Promociones slider** horizontal en Home (antes de Pedido rápido) | ✅ |
| 2.7 | **Header HomeV2**: logo.png en círculo amarillo + cart badge offset + bell | ✅ |

### Detalle
- Promociones ya no son tab → slider horizontal en Inicio con título externo
- `CatalogViewModel` expandido con `promotions` state
- `PromotionRepository` + `FirebasePromotionRepository` + DI en `FirebaseModule`

---

## Fase 3: Billetera (Puntos) ⏳

| # | Tarea | Estado |
|---|---|---|
| 3.1 | Pantalla Billetera: saldo, ofertas slider, rewards, canjear | ⏳ |
| 3.2 | Historial de Puntos (subcolección `pointsHistory`) | ⏳ |

---

## Fase 4: Checkout ✅ (Completado)

| # | Tarea | Estado |
|---|---|---|
| 4.1 | Step indicator + flujo multi-step (5 pasos) | ✅ |
| 4.2 | Step 1: Datos personales (DNI/RUC, nombre, teléfono 9 dígitos) | ✅ |
| 4.3 | Step 2: Mapa Mapbox click-to-place + delivery/pickup + costo | ✅ |
| 4.4 | **Step 3: Resumen del pedido (simplificado, sin iconos)** | ✅ |
| 4.5 | **Step 4: Método de pago (copia al portapapeles)** | ✅ |
| 4.6 | **Step 5: Confirmación + crear orden + WhatsApp + Compartir** | ✅ |
| 4.7 | Auto-complete desde `users/{uid}` | ✅ |

### Lo que se hizo en Checkout
- `CartUiState.kt`: 20+ campos
- `CartViewModel.kt`: `nextStep()` validaciones, `placeOrder()`, `calculateDeliveryCost()` (Haversine), WhatsApp URL
- `CheckoutScreen.kt` (última versión):
  - **Título**: "Checkout" (antes "Carrito")
  - **Step 3**: Lista limpia en Card único (sin ElevatedCards ni iconos de carrito)
  - **Step 4**: Cards clickeables con `Modifier.clickable`, barra BrandBlue selección, CheckCircle. Cada cuenta bancaria tiene texto **"Copiar"** que usa `ClipboardManager` + `Toast`
  - **Step 5**: Secciones (Person, Place, Info, ShoppingCart). Total en card **BrandBlue** (antes AccentOrange fosforecente). Post-creación: check verde + WhatsApp + Share. **Atras + Confirmar Pedido lado a lado** (cada uno `weight(1f)`, `height(48.dp)`)
- Título "Checkout", navegación restaurada para paso 1 (Siguiente visible)

---

## Fase 5: Pedidos 🟡

| # | Tarea | Estado |
|---|---|---|
| 5.1 | Pantalla Pedidos: lista con filtros por estado + rango fechas | 🟡 Parcial |
| 5.2 | Detalle de pedido: info + mapa tracking en vivo | ⏳ |
| 5.3 | Cancelar pedido (solo pendiente/confirmado) | ⏳ |

### Lo que se hizo en 5.1
- `OrderRepository`: `getOrdersByUserId(userId): Result<List<OrderSummary>>`
- `FirebaseOrderRepository`: query `orders` con `userId` + `createdAt DESC`
- `Order.kt`: expandido con `OrderItemSummary` y `DeliveryAddress`
- Pendiente: `OrdersViewModel` + `OrdersScreen` composable

---

## Fase 6: Code Review — Hallazgos (29/06/2026)

Calificación general: **C+** (aceptable con deficiencias importantes)
Revisor: `kotlin-code-reviewer` — 47 hallazgos (7 críticos, 12 altos, 16 medios, 12 bajos)

### 🔴 Críticos (7)

| # | Hallazgo | Archivo/Línea | Sugerencia |
|---|---|---|---|
| H1 | Secreto Mapbox `sk.*` hardcodeado | `settings.gradle.kts:20` | Mover a `local.properties`, gitignore |
| H2 | Token público Mapbox hardcodeado | `MapboxMapView.kt:183` | Usar `BuildConfig` |
| H3 | Queries Firestore sin `.limit()` | `FirebaseProductRepository.kt`, `FirebasePromotionRepository.kt`, `FirebaseOrderRepository.kt`, `CategoriesViewModel.kt` | Agregar `.limit(30)` |
| H4 | `Product.toEntity()` pierde variantes/puntos | `ProductMappers.kt:5-18` | Agregar `variantesJson` a `ProductEntity` |
| H5 | CategoriesViewModel inyecta Firestore directo | `CategoriesViewModel.kt:18` | Crear `CategoryRepository` |
| H6 | Auth no reactivo (no escucha cambios) | `AuthViewModel.kt:26` | Usar `FirebaseAuth.AuthStateListener` con `callbackFlow` |
| H7 | `isMinifyEnabled = false` en release | `app/build.gradle.kts:28` | Habilitar R8/ProGuard |

### 🟠 Altos (12)

| # | Hallazgo | Archivo/Línea |
|---|---|---|
| H8 | `getRandomProducts()` carga todo el catálogo | `FirebaseProductRepository.kt:51-58` |
| H9 | Race condition en validación de stock | `ProductDetailViewModel.kt`, `CartViewModel.kt` |
| H10 | Dark mode no soportado (colores Dark* definidos pero no usados) | `Theme.kt:27-28` |
| H11 | `getOrdersByUserId()` sin paginación | `FirebaseOrderRepository.kt:16-19` |
| H12 | `shipping` hardcodeado a 0.0 | `FirebaseOrderRepository.kt:109` |
| H13 | `paymentStatus` siempre "pendiente" (contra-entrega debería ser "completado") | `FirebaseOrderRepository.kt:103` |
| H14 | `CartSection` ignora variantes al decrementar | `CartSection.kt:72` |
| H15 | `HomeV2` usa `collectAsState()` (no lifecycle-aware) | `HomeV2.kt:79,80,104` |
| H16 | JSONArray parseado en main thread | `CartViewModel.kt:339-362` |
| H17 | `CartViewModel` God class (473 líneas) | `CartViewModel.kt` |
| H18 | `set()` sin merge sobrescribe documento Firestore | `FirebaseUserRepository.kt:30` |
| H19 | Room sin `fallbackToDestructiveMigration()` | `AppDatabase.kt:8` |

### 🟡 Medios (16)

| # | Hallazgo |
|---|---|
| H20 | Validación stock duplicada (ProductDetailVM + CartVM) |
| H21 | `CartSection.kt` no se usa en flujo principal |
| H22 | `CatalogSection.kt` no se usa en flujo principal |
| H23 | `clampQuantity` definida dentro de @Composable |
| H24 | `signIn(email, password)` nunca se usa |
| H25 | `onEmailChange`/`onPasswordChange` nunca se usan |
| H26 | `createOrder()` tiene 15 parámetros |
| H27 | Usa `kapt` en vez de `ksp` para Room |
| H28 | `Promotion.stockRemaining` usa `Int.MAX_VALUE` |
| H29 | Sin política de invalidación de caché Room |
| H30 | `isLoading = false` sin importar resultado en AccountVM |
| H31 | `reverseGeocode()` no cachea resultados |
| H32 | `STYLE_URI` hardcodeado |
| H33 | Sin tests unitarios para ViewModels/Repos |
| H34 | `CartItem.product` almacena copia completa del Product |
| H35 | `AddToCartFeedback` y `CartFeedback` duplicados |

### 🟢 Bajos (12)
Typos, imports no usados, `Typography()` vacío, formato moneda repetido, `ProductMappers.kt` con typo, SettingsScreen sin efecto, etc.

### ✅ Cosas que están MUY bien
1. **Hilt DI** — uso ejemplar de `@HiltViewModel`, `@Module`, `@InstallIn`
2. **Arquitectura MVVM + Repositorios** — interfaces separadas de implementaciones Firebase
3. **StateFlow/SharedFlow** — separación estado persistente vs eventos one-shot
4. **Persistencia carrito** con SavedStateHandle
5. **Cache offline con Room** — fallback cuando no hay conexión
6. **Patrón `Result<T>`** para operaciones Firebase
7. **Mapbox click-to-place + GPS FAB** completo
8. **Validaciones checkout** — DNI 8 díg, RUC 11, teléfono 9
9. **Haversine** correcto para costo de envío
10. **Material3** en toda la UI
11. **Navegación** con `saveState/restoreState/launchSingleTop`
12. **Auto-fill checkout** desde perfil Firestore
13. **Estructura de paquetes** limpia
14. `collectAsStateWithLifecycle()` en 90% de los casos
15. **Firebase completo** — Auth, Firestore, Messaging, Google Sign-In

---

## Cómo ejecutar

```bash
# Compilar
./gradlew assembleDebug
```

## Notas técnicas

- **Build**: compila sin errores
- **Checkout**: título "Checkout", paso 3 simplificado sin iconos, paso 4 con copia al portapapeles (ClipboardManager + Toast), paso 5 con Atras + Confirmar lado a lado (weight 1f, height 48dp), total en BrandBlue
- **Mapbox**: SDK v11, CircleAnnotation + CircleAnnotationState, click-to-place (onMapClickListener devuelve Point geográfico), GPS FAB, reverseGeocode
- **16 KB alignment**: artifacts `-ndk27`
- **Tema**: forzado claro siempre
- **Queries sin `.limit()`** → prioridad Fase 1.4
- **Tokens Mapbox expuestos** en settings.gradle.kts (sk.*) y MapboxMapView.kt (pk.*) — rotar antes de producción
- **Índice compuesto requerido**: `orders` collection, `userId` ASC + `createdAt` DESC
