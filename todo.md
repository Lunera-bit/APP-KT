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

## Fase 3: Billetera (Puntos) 🟢

| # | Tarea | Estado |
|---|---|---|
| 3.1 | Pantalla Billetera: saldo, ofertas slider, rewards, canjear | ✅ |
| 3.2 | Historial de Puntos (subcolección `pointsHistory`) | ✅ |
| 3.3 | Canjear con productos reales (query `pointsToRedeem > 0`) desde Firestore | ✅ |
| 3.4 | Foto producto + "Ganas X pts al comprarlo" + navegación a detalle | ✅ |

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
- [ ] Mover tokens Mapbox a `local.properties` + `BuildConfig`
- [ ] Agregar `.limit()` a queries Firestore
- [ ] `fallbackToDestructiveMigration()` en Room
- [ ] Tests unitarios para ViewModels/Repos

### Pendiente corto plazo (de code review)

| # | Tarea | Prioridad | Archivo |
|---|---|---|---|
| 6 | Habilitar R8/ProGuard (`isMinifyEnabled = true`) | Alta | `app/build.gradle.kts:41` |
| 8 | Snapshots tiempo real en OrderDetailScreen (reemplazar `get()` por `addSnapshotListener`) | Alta | `OrderDetailViewModel.kt` |
| 9 | Implementar `cancelOrder()` en repositorio (botón ya existe en UI) | Alta | `OrderRepository.kt`, `FirebaseOrderRepository.kt` |
| 10 | Agregar `intent-filter` para deep links de notificaciones (`cyryel://order/{id}`) | Alta | `AndroidManifest.xml` |
| 11 | CRUD direcciones (agregar/editar/eliminar) + seleccionar en checkout | Alta | Nuevo screen + `MainScreen.kt` PerfilTab |
| 12 | Mapa tracking en detalle de pedido (escuchar `delivery_locations/{orderId}`) | Alta | `OrderDetailScreen.kt` |
| 13 | Manejar userId null en `CheckoutViewModel.loadUserProfile()` (evita UI atascada) | Alta | `CheckoutViewModel.kt:60` |
| — | Mover cuentas bancarias hardcodeadas fuera de UI (Firebase Remote Config o backend) | Crítica | `CheckoutScreen.kt:1289` |
| — | `CartManager.clear()` usar `_items.update {}` en vez de `_items.value = emptyList()` | Media | `CartManager.kt` |

## Fase 8: Perfil y Configuración ⏳ Pendiente

| # | Tarea | Estado |
|---|---|---|
| 8.1 | Perfil editable (nombre, teléfono, documento) guardar en Firestore | ⏳ |
| 8.2 | CRUD direcciones (agregar/editar/eliminar) + seleccionar en checkout | ⏳ |
| 8.3 | Settings completo: notificaciones toggle, tema, cambiar contraseña, editar datos | ⏳ |
| 8.4 | Settings "Acerca de" con versión y contacto | ✅ Básico |

## Fase 9: Delivery App (repositorio separado) 🔮 Planeado

| # | Tarea | Estado |
|---|---|---|
| 9.1 | Modelo Firestore: `dispatch_queue`, `delivery_batches`, `delivery_locations`, `drivers` | 🔮 |
| 9.2 | Pantallas delivery: Online toggle, pedido entrante (timer), entrega activa (4 pasos), earnings | 🔮 |
| 9.3 | Cloud Function dispatch: asignar repartidor + batch multi-orden | 🔮 |
| 9.4 | Tracking en tiempo real: `addSnapshotListener` en `delivery_locations/{driverId}` | 🔮 |
| 9.5 | Mapa delivery con ruta (Mapbox Directions API) + marcador 3D carro (ModelLayer .glb) | 🔮 |
| 9.6 | Notificaciones push al usuario cuando cambia estado del pedido | 🔮 |

---

## Review de agentes (04/07/2026)

### 🧠 Kotlin Code Senior — Recomendaciones arquitectónicas

**Sistema de Puntos:**
- [ ] Crear `PointsRepository` con reglas desde Firestore (no hardcodeadas en `BilleteraUiState`)
- [ ] `CartItem` debe soportar `redeemedByPoints: Boolean`, `pointsUsed: Int`, `promotionId: String?`
- [ ] Usar Cloud Function para transacciones atómicas al canjear (evitar race conditions)
- [ ] Registrar canje en `users/{uid}/pointsHistory`

**Delivery Dinámico:**
- [ ] Crear `DeliveryRepository` con tarifas desde Firestore (`baseRate`, `ratePerKm`, `freeDeliveryRadiusKm`, `maxDeliveryRadiusKm`)
- [ ] Implementar `CalculateDeliveryCostUseCase` con Haversine distance
- [ ] Agregar `shipping` y `deliveryDistance` a `CheckoutUiState`
- [ ] Extraer coordenadas tienda a `StoreConfig` en Firestore (hoy en 3 sitios distintos)

**Datos Bancarios:**
- [ ] **Crítico**: Mover cuentas de `CheckoutScreen.kt` a colección `config/banking/accounts` en Firestore

**Tracking Mapa:**
- [ ] Extraer `MapPickerDialog` a componente reutilizable (`MapboxMapView`)
- [ ] Usar `SnapshotListener` a `deliveries/{orderId}/location` para tracking delivery
- [ ] Polyline entre tienda ⇨ repartidor ⇨ destino con Mapbox Directions API

**Blockers producción:**
- [ ] R8/ProGuard habilitado
- [ ] Auth reactivo con `authState: StateFlow<User?>`
- [ ] Tests unitarios (al menos `CartManager`, `CalculateDeliveryCostUseCase`)

---

### 🔍 Kotlin Code Reviewer — Deuda técnica y priorización

**Fase 0 — Hacer AHORA (seguridad + estabilidad):**
- [ ] Habilitar R8 con ProGuard rules (`build.gradle.kts`)
- [ ] Hacer `AuthRepository` reactivo con `AuthStateListener`
- [ ] Revisar queries sin índice compuesto, agregar `.limit()` consistente
- [ ] Mover `STORE_LATITUDE/LONGITUDE` a `StoreConfig` en Firestore

**Fase 1 — Refactor base (desbloquea las 4 features):**
- [ ] `CartItem`: agregar `redeemedByPoints`, `pointsUsed`, `promotionId`, `discountedPrice`. Eliminar `product: Product` (usar solo `productId`)
- [ ] `CartManager.addProduct()` con quantity, points, promotion (eliminar `repeat(N)`)
- [ ] `CreateOrderRequest`: agregar `pointsUsed`, `pointsDiscount`, `shipping` como requerido
- [ ] `FirebaseOrderRepository`: recibir y guardar puntos/shipping reales
- [ ] `CheckoutUiState`: agregar `shipping`, `deliveryDistance`, `pointsUsed`, `pointsDiscount`, `userPointsBalance`
- [ ] `ForcedPackConfig`: mover a campo `packSize` en Firestore

**Fase 2 — Refactor arquitectónico:**
- [ ] Extraer `MapPickerDialog` a componente reutilizable
- [ ] Dividir `CheckoutScreen.kt` en múltiples archivos por paso
- [ ] Agregar capa `domain` con use cases (`CalculateShippingUseCase`, `RedeemPointsUseCase`, `ApplyPromotionUseCase`)
- [ ] Agregar `ShippingRepository` con reglas de delivery
- [ ] Configurar `FirestoreSnapshotListeners` en repositorios
- [ ] Transacciones atómicas para operaciones de puntos

**Evaluación deuda técnica:** 4/10 mantenibilidad, 2/10 testeabilidad, 3/10 seguridad, 2/10 preparación para features. Estima ~40-60h de refactor antes de implementar features nuevas.

---

### 🎨 Kotlin UI Expert — UI/UX y diseño

**Problemas actuales:**
- [ ] **Typography vacío** — personalizar escalas (`displayLarge` a `bodySmall`)
- [ ] **Colores hardcodeados** — migrar a `MaterialTheme.colorScheme.*` (hoy usas `AzulRey`, `AzulReyClaro` directo)
- [ ] **Dark theme**: contraste bajo de `AzulReyClaro` sobre fondo oscuro (~2.9:1, no cumple WCAG AA)
- [ ] **Touch targets < 48dp** — stepper circles, QuantitySelector, zoom FABs
- [ ] **Sin `contentDescription`** en iconos interactivos (accesibilidad TalkBack)

**UX para próximas features:**
- [ ] **Productos canjeables**: badge amarillo "🟡 Canjeado" + precio como "S/ X + Y pts"
- [ ] **Compra con puntos**: segundo CTA "🪙 Comprar con puntos — XXX pts" en detalle; si no alcanza, mostrar "Necesitas YYY pts más"
- [ ] **Carrito**: items canjeados con fondo amarillo tenue + icono estrella; bottom bar con "Subtotal efectivo" / "Puntos a canjear"
- [ ] **Delivery cost**: card animada con distancia y costo al seleccionar ubicación; ocultar si es recojo en tienda
- [ ] **Tracking mapa**: marcador animado del repartidor + info card (nombre, ETA, botones llamar/WhatsApp)
- [ ] **Promos en carrito**: sección "Ofertas aplicadas" con badges `-15%` y desglose de descuentos

**Mejoras visuales priorizadas:**
| Prioridad | Mejora | Esfuerzo |
|-----------|--------|----------|
| Alta | Personalizar Typography | 30 min |
| Alta | Hardcode colors → scheme | 2-3h |
| Alta | Touch targets a 48dp | 1h |
| Alta | Content descriptions | 30 min |
| Media | Badge "Canjeado" en OrderDetail | 1h |
| Media | Botón "Comprar con puntos" | 2h |
| Media | Desglose efectivo+puntos en cart/checkout | 3-4h |
| Media | Tracker map en OrderDetail | 4-6h |
| Baja | Skeleton loaders | 2h |
| Baja | Transición slide entre pasos checkout | 1h |

---

## Roadmap recomendado (post-review)

1. **Fase 0 — Bloqueantes** (1 semana): R8, Auth reactivo, StoreConfig, datos bancarios en Firestore
2. **Fase 1 — Refactor base** (1-2 semanas): CartItem, CartManager, CreateOrderRequest, DeliveryRepository
3. **Fase 2 — Features** (2 semanas): Canje puntos, delivery dinámico, promos en carrito, tracking mapa
4. **Fase 3 — Calidad** (ongoing): Tests, UI polish, accesibilidad, offline support

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
