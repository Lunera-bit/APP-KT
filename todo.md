# TODO — Migración Ionic → Kotlin Nativo (Shop Usuario)

## Estado actual

Build: ✅ **Compila sin errores** (05/07/2026)
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
| 5.6 | Mapa tracking delivery en tiempo real | ⏳ Pendiente |

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

## Fase 9: Delivery App (integrada en mismo APK) 🟡

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
| 9.12 | SnapshotListener en tiempo real para notificaciones al usuario cuando cambia estado delivery | 🔮 |
| 9.13 | Earnings / historial de entregas completadas | 🔮 |

---

## Pendiente para producción

- [ ] Flash Banners slider (colección `flash`) con auto-scroll en Home
- [ ] Category picker modal (bottom sheet) con búsqueda
- [ ] Mapa tracking delivery en OrderDetailScreen (SnapshotListener a `lastLocation` del repartidor)
- [ ] Mover tokens Mapbox a `local.properties` + `BuildConfig`
- [ ] Deep links externos: agregar `intent-filter` en `AndroidManifest.xml` para `cyryel://order/{id}`
- [ ] Agregar `.limit()` faltantes a queries Firestore en repositorios
- [ ] R8/ProGuard habilitado para release
- [ ] Auth reactivo con `AuthStateListener`
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
