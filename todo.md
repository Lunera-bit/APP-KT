# TODO — Migración Ionic → Kotlin Nativo (Shop Usuario)

## Estado actual

Build: ✅ **Compila sin errores** (07/07/2026)
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
| 1.4 | Agregar `.limit()` a queries Firestore | ✅ |

---

## Fase 2: Inicio + Categorías 🟡

| # | Tarea | Estado |
|---|---|---|
| 2.1 | BottomNavigationView con 4 tabs funcionales | ✅ |
| 2.2 | Flash Banners slider (colección `flash`) con auto-scroll | ⏳ Pendiente |
| 2.3 | "Pedido rápido" slider horizontal con random + "Agregar todo" | ✅ |
| 2.4 | Categories grid 2 columnas con colores gradiente | ✅ |
| 2.5 | Category picker modal (bottom sheet) con búsqueda | ⏳ Pendiente |
| 2.6 | **Promociones slider** horizontal en Home (antes de Pedido rápido) | ✅ |
| 2.7 | **Header**: logo en círculo amarillo + cart badge offset + bell badge | ✅ |

---

## Fase 3: Billetera (Puntos) ✅

| # | Tarea | Estado |
|---|---|---|
| 3.1 | Pantalla Billetera: saldo, ofertas slider, rewards, canjear | ✅ |
| 3.2 | Historial de Puntos (subcolección `pointsHistory`) | ✅ |
| 3.3 | Canjear con productos reales (query `pointsToRedeem > 0`) desde Firestore | ✅ |
| 3.4 | Foto producto + "Ganas X pts al comprarlo" + navegación a detalle | ✅ |
| 3.5 | Stock reserve (STOCK_RESERVE=4) + availableStock en toda la UI | ✅ |
| 3.6 | Canje con verificación de stock (availableStock) en CartManager y UI | ✅ |

---

## Fase 4: Checkout ✅

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
| 4.9 | Cuentas bancarias desde Firestore (`config/bancos`) | ✅ |
| 4.10 | Seleccionar dirección guardada en checkout | ✅ |
| 4.11 | Coordenadas tienda actualizadas (C. Belén 310, Chancay) | ✅ |

---

## Fase 5: Pedidos ✅

| # | Tarea | Estado |
|---|---|---|
| 5.1 | Pantalla Pedidos: lista con filtros por estado + rango fechas | ✅ |
| 5.2 | Detalle de pedido: info completa | ✅ |
| 5.3 | Cancelar pedido (vía Cloud Function `updateOrderStatus`) | ✅ |
| 5.4 | SnapshotListener en tiempo real para órdenes recientes | ✅ |
| 5.5 | Scroll infinito (paginación load 5 + near-end detection) | ✅ |
| 5.6 | Mapa tracking delivery en tiempo real con Mapbox | ✅ |

---

## Fase 6: UI/UX — Mejoras ✅

| # | Tarea | Estado |
|---|---|---|
| 6.1 | Dark theme con `isSystemInDarkTheme()` | ✅ |
| 6.2 | Splash screen con Lottie + pulso | ✅ |
| 6.3 | Colores texto: AzulRey → AzulReyClaro (#2C5F8A) para legibilidad en dark | ✅ |
| 6.4 | Animaciones checkout: confetti.json + success_check.json (transparente) | ✅ |
| 6.5 | Botón Google con icono transparente | ✅ |
| 6.6 | Mapa 3D rediseñado (full-screen, controles flotantes) | ✅ |

---

## Fase 7: Perfil y Configuración ✅

| # | Tarea | Estado |
|---|---|---|
| 7.1 | Perfil editable inline (nombre, teléfono, DNI, RUC) guardar en Firestore | ✅ |
| 7.2 | CRUD direcciones completo (agregar/editar/eliminar con mapa Mapbox) | ✅ |
| 7.3 | Settings: switches permisos notificación + ubicación | ✅ |
| 7.4 | Settings "Acerca de" con versión y contacto | ✅ |
| 7.5 | Settings: cerrar sesión | ✅ |

---

## Fase 8: Notificaciones ✅

| # | Tarea | Estado |
|---|---|---|
| 8.1 | Data layer: NotificacionData, NotificacionRepository, FirebaseNotificacionRepository | ✅ |
| 8.2 | BandejaNotificacionesScreen con LazyColumn, icono por tipo, contador no leídas | ✅ |
| 8.3 | Badge de notificaciones en MainScreen (contador sobre últimas 50) | ✅ |
| 8.4 | SnapshotListener en tiempo real para notificaciones | ✅ |
| 8.5 | Botón "Leer todo" (batch update) | ✅ |
| 8.6 | Deep links desde notificación a detalle de pedido | ✅ |
| 8.7 | Deduplicación notificaciones admin en Cloud Function | ✅ |

---

## Fase 9: Delivery App (integrada en mismo APK) ✅

| # | Tarea | Estado |
|---|---|---|
| 9.1 | Modelo Firestore: `deliveries/{orderId}` con status (disponible/aceptado/en_camino/entregado), `assignedDeliveryId`, `deliveryPerson` | ✅ |
| 9.2 | Rol `delivery` en AuthScreen + NavGraph bifurca a DeliveryMainScreen | ✅ |
| 9.3 | Delivery tabs: Pedidos (aceptar) + Perfil (toggle disponibilidad conectado a Firestore) | ✅ |
| 9.4 | DeliveryRepository + FirebaseDeliveryRepository con transacciones (aceptar, startDelivery, completeDelivery) | ✅ |
| 9.5 | Cloud Function `onOrderReadyForDelivery` genera código 4 dígitos en `deliveryConfirmationCode` | ✅ |
| 9.6 | DeliveryDetailScreen: botones "Aceptar pedido", "Recogí el pedido", input código + "Entregar pedido" | ✅ |
| 9.7 | LocationUploaderService (foreground service) sube ubicación cada 15s a deliveries activos | ✅ |
| 9.8 | Diálogo explicativo antes de solicitar permiso `ACCESS_FINE_LOCATION` | ✅ |
| 9.9 | Botón "Abrir Configuración" si permiso denegado permanentemente | ✅ |
| 9.10 | MapCard con 2 botones: "Ver ubicación" (geo) + "Navegar" (google.navigation) | ✅ |
| 9.11 | Código confirmación visible en OrderDetailScreen (cliente) con toggle mostrar/ocultar | ✅ |
| 9.12 | Notificaciones FCM al cambiar estado delivery (Cloud Function onOrderUpdated) | ✅ |
| 9.13 | Historial de entregas completadas en tab "Historial" | ✅ |
| 9.14 | Mapa Mapbox con tracking en vivo en OrderDetailScreen (cliente) | ✅ |
| 9.15 | SnapshotListener ubicación repartidor en OrderDetailViewModel | ✅ |
| 9.16 | Navegación automática a pedidos tras entregar + overlay "Verificando código..." | ✅ |

---

## Fase 10: Promociones — Detalle y Deep Links ✅

| # | Tarea | Estado |
|---|---|---|
| 10.1 | PromotionDetailScreen (imagen, precios, descuento %, lista productos, agregar al carrito) | ✅ |
| 10.2 | PromotionDetailViewModel (carga por ID desde Firestore, addToCart via CartManager) | ✅ |
| 10.3 | getPromotionById en PromotionRepository + FirebasePromotionRepository | ✅ |
| 10.4 | Deep link `cyryel://promotion/{promotionId}` en FCM + manifest + NavGraph | ✅ |
| 10.5 | Notificación in-app → promoción: manejo de `promociones` links en Bandeja | ✅ |
| 10.6 | Reemplazar diálogo de promoción en MainScreen por navegación a PromotionDetailScreen | ✅ |
| 10.7 | Compatibilidad carrito: addPromotionProducts en CartManager (mismo método que antes) | ✅ |

---

## Fase 11: SearchScreen rediseñada ✅

| # | Tarea | Estado |
|---|---|---|
| 11.1 | SearchScreen muestra grid de categorías cuando no hay query | ✅ |
| 11.2 | SearchViewModel carga categorías desde CategoryRepository en init | ✅ |
| 11.3 | Tap categoría → navega a CategoryProductsScreen (con su propio buscador interno) | ✅ |
| 11.4 | Search field busca en todos los productos (comportamiento original) | ✅ |

---

## Fase 12: Mejoras Delivery y Carrito ✅

| # | Tarea | Estado |
|---|---|---|
| 12.1 | DeliveryMainScreen y DeliveryDetailScreen: mostrar notas del pedido (`order.notes`) | ✅ |
| 12.2 | OrderDetailScreen: agrupar items por promoción con nombre y cantidad de bundles | ✅ |
| 12.3 | CartManager: no mezclar items de promoción con productos regulares del mismo ID (`promotionId == null`) | ✅ |
| 12.4 | `removeProduct` acepta `promotionId` para eliminar item correcto | ✅ |

---

## Fase 13: Configuración Android — Namespace y App Check ✅

| # | Tarea | Estado |
|---|---|---|
| 13.1 | Namespace unificado: `com.cyryel` → `com.CYRYEL.com` (coincide con applicationId) | ✅ |
| 13.2 | `firebase-appcheck-debug` cambiado a `debugImplementation` | ✅ |
| 13.3 | Regla ProGuard genérica de Firebase eliminada (la manejan las dependencias) | ✅ |
| 13.4 | Actualizados todos los imports de `R` y `BuildConfig` al nuevo namespace (15 archivos) | ✅ |

---

## Admin Dashboard (web) — https://tienda-leon-6b457.web.app

| # | Tarea | Estado |
|---|---|---|
| Dashboard-1 | App Check corregido: usar `appId` y `apiKey` de la web (no Android) | ✅ |
| Dashboard-2 | `.replace()` seguros contra undefined en OrdersPage (evita crash en filtro fecha) | ✅ |
| Dashboard-3 | Configuración Bot: documento `config/bot_config`, campo `token_expires_at` con datetime-local, `updatedAt` al guardar | ✅ |
| Dashboard-4 | CI/CD: GitHub Actions despliega automáticamente al pushear en `main` | ✅ |

---

## Pendiente para producción
- [x] Deep links externos: `intent-filter` en `AndroidManifest.xml` para `cyryel://order/{id}`
- [x] Agregar `.limit()` faltantes a queries Firestore en repositorios
- [x] R8/ProGuard habilitado para release
- [x] Auth reactivo con `AuthStateListener`
- [x] Purgar `google-services.json` del historial git (filter-repo)
- [x] Eliminar cuentas bancarias hardcodeadas de WhatsAppUtil (cargar desde Firestore)
- [x] Mover teléfono tienda fuera de WhatsAppUtil a CheckoutUiState
- [ ] Tests unitarios (CartManager, ViewModels, Repos)
- [ ] Subir APK release a Play Store

---

## Deuda técnica pendiente

| # | Tarea | Prioridad | Archivo |
|---|---|---|---|
| 1 | Personalizar Typography escalas | Media | `ui/theme/Type.kt` |
| 2 | Migrar colores hardcodeados a `MaterialTheme.colorScheme` | Media | Varios |
| 3 | Contraste dark mode: `AzulReyClaro` → tono más claro | Baja | `ui/theme/Color.kt` |
| 4 | Touch targets < 48dp (stepper, QuantitySelector, FABs) | Baja | Varios |
| 5 | contentDescription en iconos interactivos | Baja | Varios |
| 6 | Manejar estados vacío/error en SearchScreen e InicioTab | Baja | `SearchScreen.kt`, `MainScreen.kt` |

---

## Notas técnicas

- **Build**: compila sin errores
- **Dark theme**: `TiendaCyryelTheme` con `isSystemInDarkTheme()`, `darkColorScheme`
- **Checkout**: 5 pasos, animaciones Lottie (confetti loop + success transparente)
- **Mapbox**: SDK v11, CircleAnnotation, toggle 3D/plano (pitch 60°/0°), zoom 18.0
- **Store coordinates**: `-11.567832, -77.269716` (C. Belén 310, Chancay)
- **Carrito**: solo memoria (`StateFlow`), sin Room
- **Room DB versión**: 6
- **MAPBOX_ACCESS_TOKEN**: configurado en `local.properties` / `BuildConfig`
- **Cuentas bancarias**: desde Firestore `config/bancos` (no hardcode)
- **Notificaciones**: SnapshotListener con limit 50 + badge solo entre visibles
