# TODO — Migración Ionic → Kotlin Nativo (Shop Usuario)

## Estado actual

Build: ✅ **Compila sin errores** (03/07/2026)
Checkout: ✅ **5 pasos completos con animaciones Lottie, dark theme, mapa 3D**
Mapbox: ✅ **Click-to-place con CircleAnnotation, GPS FAB, toggle 3D/plano, zoom +/-**
Autenticación: ✅ **Usuario creado en Firestore tras Google Sign-In + auto-fill checkout**
Dark theme: ✅ **Soporte completo con `isSystemInDarkTheme()`**
Splash: ✅ **Animación Lottie con pulso + windowBackground eliminó flash cold start**
Iconos app: ✅ **Iconos desde Ionic en webp para mipmap**

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
- `CartManager` simplificado a solo `StateFlow` en memoria (sin Room)
- Eliminados `CartDao.kt`, `CartItemEntity.kt`, `cartDao()` de `AppDatabase` y `FirebaseModule`
- `CheckoutViewModel` usa `SavedStateHandle` para persistir `orderId` tras crear pedido (evita volver a paso 1 vacío al reabrir app)

### Lo que se hizo en 1.3
- `AuthViewModel`: crea documento `users/{uid}` en Firestore tras Google Sign-In
- `UserRepository`/`FirebaseUserRepository`: guarda nombre, email, uid
- `CheckoutViewModel.loadUserProfile()`: auto-fill checkout (nombre, teléfono, documento, dirección) desde `users/{uid}`

### Iconos app
- Iconos webp desde `src/assets/icons/` (Ionic) copiados a `mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher.webp` e `ic_launcher_foreground.webp`
- Adaptive icon XML corregido en `mipmap-anydpi-v26/` (referencia circular arreglada)
- Icono Google login: `res/drawable/google.png`

---

## Fase 2: Inicio + Categorías 🟡

| # | Tarea | Estado |
|---|---|---|
| 2.1 | BottomNavigationView con 4 tabs funcionales | ✅ |
| 2.2 | Flash Banners slider (colección `flash`) con auto-scroll | ⏳ |
| 2.3 | "Pedido rápido" slider horizontal con random + "Agregar todo" | ✅ |
| 2.4 | Categories grid 2 columnas con colores gradiente | 🟡 Parcial |
| 2.5 | Category picker modal (bottom sheet) con búsqueda | ⏳ |
| 2.6 | **Promociones slider** horizontal en Home (antes de Pedido rápido) | ✅ |
| 2.7 | **Header**: logo en círculo amarillo + cart badge offset + bell | ✅ |

---

## Fase 3: Billetera (Puntos) 🟡

| # | Tarea | Estado |
|---|---|---|
| 3.1 | Pantalla Billetera: saldo, ofertas slider, rewards, canjear | ✅ |
| 3.2 | Historial de Puntos (subcolección `pointsHistory`) | ✅ |

---

## Fase 4: Checkout ✅ (Completado)

| # | Tarea | Estado |
|---|---|---|
| 4.1 | Step indicator + flujo multi-step (5 pasos) | ✅ |
| 4.2 | Step 1: Revisar pedido + datos personales | ✅ |
| 4.3 | Step 2: Mapa Mapbox 3D/plano + delivery/pickup + referencia | ✅ |
| 4.4 | **Step 3: Información de contacto** | ✅ |
| 4.5 | **Step 4: Método de pago (copia al portapapeles)** | ✅ |
| 4.6 | **Step 5: Confirmación + crear orden + Lottie animaciones** | ✅ |
| 4.7 | Auto-complete desde `users/{uid}` | ✅ |
| 4.8 | Dark theme en checkout | ✅ |

### Lo que se hizo en Checkout
- `CheckoutUiState.kt`: 20+ campos con `OrderSnapshot`
- `CheckoutViewModel.kt`: `nextStep()` validaciones, `placeOrder()`, `SavedStateHandle` para persistir éxito
- `CheckoutScreen.kt`:
  - Step indicator horizontal con círculos + línea conectora
  - Step 1: Lista de productos con precios, subtotal, notas
  - Step 2: Mapa Mapbox full-screen con toggle 3D/plano (pitch 60°/0°), zoom +/-, GPS, bottom Row Cerrar+Confirmar. Flujo de referencia al confirmar.
  - Step 3: Teléfono con `PhoneVisualTransformation` (formato `999 999 999`), DNI/RUC, nombre
  - Step 4: Cards bancarias con copia al portapapeles
  - Step 5: Resumen + Lottie confetti (loop infinito) + check success con fondo transparente
  - Post-creación: WhatsApp + Share + botones Ver Pedido / Ir a Inicio
- Mapa 3D: `MapPickerDialog` con `PointAnnotation`, zoom 18.0, pitch 60°
- Store coordinates: `-11.56545313746308, -77.27110282305334`

---

## Fase 5: Pedidos ✅

| # | Tarea | Estado |
|---|---|---|
| 5.1 | Pantalla Pedidos: lista con filtros por estado + rango fechas | ✅ |
| 5.2 | Detalle de pedido: info + mapa tracking | ✅ |
| 5.3 | Cancelar pedido (solo pendiente/confirmado) | ⏳ |

### Lo que se hizo en 5.1
- `OrdersScreen` con topBar sin flecha volver, filtros por estado + rango fechas
- OrderID con `#` anotado (hash en AzulRey, ID en AzulReyClaro)
- `OrdersViewModel` con carga paginada
- `OrderDetailScreen` con info completa

---

## Fase 6: UI/UX — Mejoras recientes 🟢

| # | Tarea | Estado |
|---|---|---|
| 6.1 | Dark theme con `isSystemInDarkTheme()` | ✅ |
| 6.2 | Splash screen con Lottie + pulso | ✅ |
| 6.3 | Colores texto: AzulRey → AzulReyClaro (#2C5F8A) para legibilidad en dark | ✅ |
| 6.4 | Animaciones checkout: confetti.json + success_check.json (transparente) | ✅ |
| 6.5 | Botón Google con icono transparente | ✅ |
| 6.6 | Mapa 3D rediseñado (full-screen, controles flotantes) | ✅ |

---

## Fase 7: Code Review — Hallazgos (29/06/2026)

Calificación general: **C+** (aceptable con deficiencias importantes)
Revisor: `kotlin-code-reviewer` — 47 hallazgos (7 críticos, 12 altos, 16 medios, 12 bajos)

### 🔴 Críticos (7)

| # | Hallazgo | Archivo/Línea | Sugerencia |
|---|---|---|---|
| H1 | Secreto Mapbox `sk.*` hardcodeado | `settings.gradle.kts:20` | Mover a `local.properties`, gitignore |
| H2 | Token público Mapbox hardcodeado | `MapboxMapView.kt:183` | Usar `BuildConfig` |
| H3 | Queries Firestore sin `.limit()` | Varios repositorios | Agregar `.limit(30)` |
| H4 | `Product.toEntity()` pierde variantes/puntos | `ProductMappers.kt:5-18` | Agregar `variantesJson` a `ProductEntity` |
| H5 | CategoriesViewModel inyecta Firestore directo | `CategoriesViewModel.kt:18` | Crear `CategoryRepository` |
| H6 | Auth no reactivo (no escucha cambios) | `AuthViewModel.kt:26` | Usar `FirebaseAuth.AuthStateListener` con `callbackFlow` |
| H7 | `isMinifyEnabled = false` en release | `app/build.gradle.kts:28` | Habilitar R8/ProGuard |

### 🟠 Altos (12)

| # | Hallazgo |
|---|---|
| H8 | `getRandomProducts()` carga todo el catálogo |
| H9 | Race condition en validación de stock |
| H10 | ~~Dark mode no soportado~~ ✅ **Corregido** |
| H11 | `getOrdersByUserId()` sin paginación |
| H12 | `shipping` hardcodeado a 0.0 |
| H13 | `paymentStatus` siempre "pendiente" |
| H14 | `CartSection` ignora variantes al decrementar |
| H15 | ~~`HomeV2` usa `collectAsState()`~~ ✅ **Corregido** |
| H16 | JSONArray parseado en main thread |
| H17 | `CartViewModel` God class |
| H18 | `set()` sin merge sobrescribe documento Firestore |
| H19 | Room sin `fallbackToDestructiveMigration()` |

### 🟡 Medios (16)
| # | Hallazgo |
|---|---|
| H20-H35 | Validación stock duplicada, archivos no usados, Promoción stockRemaining, etc. |

### 🟢 Bajos (12)
Typos, imports no usados, `Typography()` vacío, formato moneda repetido, etc.

### ✅ Cosas que están MUY bien
1. **Hilt DI** — uso ejemplar de `@HiltViewModel`, `@Module`, `@InstallIn`
2. **Arquitectura MVVM + Repositorios** — interfaces separadas de implementaciones Firebase
3. **StateFlow/SharedFlow** — separación estado persistente vs eventos one-shot
4. **Mapbox 3D** con toggle plano, GPS, click-to-place
5. **Dark theme** completo
6. **Animaciones Lottie** en checkout y splash
7. **Material3** en toda la UI
8. **Firebase completo** — Auth, Firestore, Messaging, Google Sign-In
9. **Validaciones checkout** — DNI 8 díg, RUC 11, teléfono 9
10. **Auto-fill checkout** desde perfil Firestore

---

## Pendiente para producción

- [ ] Subir APK release a Play Store (mismo keystore que Ionic)
- [ ] Verificar notificaciones push flujo completo
- [ ] Habilitar R8/ProGuard (`isMinifyEnabled = true`)
- [ ] Mover tokens Mapbox a `local.properties` + `BuildConfig`
- [ ] Agregar `.limit()` a queries Firestore
- [ ] `fallbackToDestructiveMigration()` en Room
- [ ] Tests unitarios para ViewModels/Repos

---

## Cómo ejecutar

```bash
# Compilar
./gradlew assembleDebug
```

## Notas técnicas

- **Build**: compila sin errores
- **Dark theme**: `TiendaCyryelTheme` con `isSystemInDarkTheme()`, `darkColorScheme`
- **Checkout**: 5 pasos, animaciones Lottie (confetti loop + success transparente)
- **Mapbox**: SDK v11, CircleAnnotation, toggle 3D/plano (pitch 60°/0°), zoom 18.0
- **Store coordinates**: `-11.56545313746308, -77.27110282305334`
- **Carrito**: solo memoria (`StateFlow`), sin Room
- **Room DB versión**: 6
- **MAPBOX_ACCESS_TOKEN**: configurado en `local.properties` / `BuildConfig`
- **Iconos app**: webp desde Ionic en mipmap, adaptive icon con foreground webp

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
