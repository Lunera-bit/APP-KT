# CYRYEL STORE

**Aplicación nativa de Android para CYRYEL E.I.R.L. (Chancay, Perú)**

Sistema de ventas móvil con catálogo de productos, carrito de compras, checkout multi-paso, seguimiento de pedidos en tiempo real, billetera de puntos, notificaciones push y panel de repartidor integrado.

[![Android CI](https://github.com/Lunera-bit/APP-KT/actions/workflows/android-tests.yml/badge.svg)](https://github.com/Lunera-bit/APP-KT/actions/workflows/android-tests.yml)
![Kotlin](https://img.shields.io/badge/Kotlin-2.3.20-7F52FF?logo=kotlin)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202024.06-4285F4?logo=jetpackcompose)
![Firebase](https://img.shields.io/badge/Firebase-BOM%2034.16.0-FFCA28?logo=firebase)
![License](https://img.shields.io/badge/License-Proprietary-red)

---

## Stack Técnico

### Core

| Componente | Versión | Descripción |
|---|---|---|
| **Kotlin** | `2.3.20` | Lenguaje principal |
| **Gradle** | `8.14.3` | Sistema de build (Kotlin DSL) |
| **Android Gradle Plugin** | `8.9.1` | Plugin de compilación Android |
| **compileSdk / targetSdk** | `35` | Android 15 |
| **minSdk** | `24` | Android 7.0 (Nougat) |
| **JVM Target** | `17` | Java 17 compatible |

### UI & Arquitectura

| Librería | Versión | Descripción |
|---|---|---|
| **Jetpack Compose BOM** | `2024.06.00` | UI declarativa |
| **Material 3** | BOM managed | Sistema de diseño |
| **Compose Navigation** | `2.8.5` | Navegación entre pantallas |
| **Lifecycle ViewModel Compose** | `2.8.7` | ViewModel con Compose |
| **Lifecycle Runtime Compose** | `2.8.7` | Ciclo de vida reactivo |
| **Activity Compose** | `1.10.1` | Integración Activity-Compose |
| **Hilt** | `2.57.2` | Dependency injection |
| **Hilt Navigation Compose** | `1.2.0` | Navegación con Hilt |
| **Coil Compose** | `2.7.0` | Carga de imágenes |
| **Lottie Compose** | `6.4.0` | Animaciones JSON |
| **Gson** | `2.11.0` | Serialización JSON |

### Firebase

| Servicio | Versión / BOM | Descripción |
|---|---|---|
| **Firebase BOM** | `34.16.0` | Bill of Materials |
| **Firebase Auth** | BOM managed | Autenticación (Google Sign-In) |
| **Cloud Firestore** | BOM managed | Base de datos en tiempo real |
| **Firebase Messaging** | BOM managed | Notificaciones push (FCM) |
| **Firebase Functions** | BOM managed | Cloud Functions |
| **Firebase Analytics** | BOM managed | Análisis de uso |
| **Firebase App Check** | BOM managed | Protección de APIs |
| **Play Services Auth** | `21.2.0` | Google Sign-In |
| **Play Services Location** | `21.3.0` | GPS / geolocalización |
| **Kotlinx Coroutines Play Services** | `1.8.1` | Bridges coroutines-Firebase |

### Base de Datos Local

| Librería | Versión | Descripción |
|---|---|---|
| **Room Runtime** | `2.8.4` | ORM SQLite |
| **Room KTX** | `2.8.4` | Extensions coroutine |
| **Room Compiler (kapt)** | `2.8.4` | Generación de código |

### Mapas & Geolocalización

| Librería | Versión | Descripción |
|---|---|---|
| **Mapbox Maps SDK** | `11.25.0` (NDK 27) | Mapas interactivos |
| **Mapbox Maps Compose** | `11.25.0` | Integración Compose |

### AndroidX

| Librería | Versión |
|---|---|
| **Core KTX** | `1.15.0` |
| **AndroidX Hilt** | `1.2.0` |
| **Google Material** | `1.12.0` |

### Testing

| Librería | Versión | Tipo |
|---|---|---|
| **JUnit 4** | `4.13.2` | Unit testing |
| **MockK** | `1.13.13` | Mocking (JVM + Android) |
| **Turbine** | `1.2.0` | Flow testing |
| **Coroutines Test** | `1.8.1` | Coroutine testing |
| **Espresso Core** | `3.6.1` | UI testing |
| **Compose UI Test JUnit4** | BOM managed | Compose testing |
| **AndroidX Test Ext JUnit** | `1.2.1` | Test extensions |

### CI/CD & Seguridad

| Herramienta | Versión | Descripción |
|---|---|---|
| **GitHub Actions** | - | CI/CD pipeline |
| **OWASP Dependency-Check** | `11.1.1` | Escaneo de vulnerabilidades |
| **Firebase Emulator** | - | Desarrollo local |
| **Gradle Managed Devices** | - | API 27 + API 35 |

---

## Funcionalidades

- **Catálogo de productos** con búsqueda, filtros por categoría y promociones destacadas
- **Carrito de compras** con persistencia en memoria, selección de dirección y método de pago
- **Checkout multi-paso** (5 pasos) con mapa Mapbox 3D para geolocalización de entrega
- **Billetera de puntos** con historial, canje por productos y ofertas especiales
- **Panel de repartidor** integrado con tracking GPS en tiempo real y confirmación por código
- **Notificaciones push** con deep links a pedidos y promociones
- **Perfil de usuario** editable con CRUD de direcciones
- **Dark mode** completo con detección del sistema
- **Splash screen** con animación Lottie y force-update

---

## Arquitectura

```
app/src/main/java/com/CYRYEL/com/
├── data/                  # Capa de datos
│   ├── auth/              # Autenticación (AuthRepository)
│   ├── cart/              # Carrito de compras (CartManager)
│   ├── category/          # Categorías
│   ├── config/            # Configuración del servidor
│   ├── delivery/          # Sistema de entregas
│   ├── local/             # Room database (AppDatabase, ProductDao)
│   ├── notificacion/      # Notificaciones in-app
│   ├── order/             # Pedidos
│   ├── product/           # Productos (ProductRepository, Mappers)
│   ├── promotion/         # Promociones
│   └── user/              # Usuarios
├── di/                    # Dependency Injection (Hilt)
│   └── FirebaseModule.kt
├── navigation/            # Navegación Compose
│   └── NavGraph.kt
├── ui/                    # Capa de presentación (MVVM)
│   ├── auth/              # Login (Google Sign-In)
│   ├── billetera/         # Billetera y puntos
│   ├── cart/              # Carrito
│   ├── catalog/           # Catálogo de productos
│   ├── checkout/          # Flujo de checkout (5 pasos)
│   ├── delivery/          # Panel de repartidor
│   ├── home/              # Pantalla principal
│   ├── notifications/     # Bandeja de notificaciones
│   ├── orders/            # Historial de pedidos
│   ├── productdetail/     # Detalle de producto
│   ├── profile/           # Perfil y direcciones
│   ├── promotions/        # Lista de promociones
│   ├── search/            # Búsqueda
│   ├── settings/          # Configuración
│   ├── splash/            # Splash y force-update
│   ├── theme/             # Tema Compose (Light/Dark)
│   └── util/              # Utilidades (WhatsApp, Toast)
├── MainActivity.kt        # Activity principal
└── TiendaCyryelApp.kt     # Application class (Hilt)
```

---

## Requisitos

- Android Studio Ladybug (2024.2.1) o superior
- JDK 17
- SDK Android 35
- Cuenta de Firebase con proyecto configurado
- Token de Mapbox

---

## Configuración

```bash
# 1. Clonar
git clone https://github.com/Lunera-bit/APP-KT.git
cd APP-KT

# 2. Crear local.properties
echo "MAPBOX_ACCESS_TOKEN=tu_token" > local.properties
echo "MAPBOX_SECRET_TOKEN=tu_secret" >> local.properties
echo "KEYSTORE_PATH=/ruta/keystore.jks" >> local.properties
echo "KEYSTORE_PASSWORD=tu_password" >> local.properties
echo "KEY_ALIAS=tu_alias" >> local.properties
echo "KEY_PASSWORD=tu_password" >> local.properties

# 3. Colocar google-services.json de Firebase en app/

# 4. Build
./gradlew assembleDebug
```

### Firebase Emulator (desarrollo local)

```bash
firebase emulators:start
```

| Emulador | Puerto |
|---|---|
| Auth | `:9099` |
| Firestore | `:8080` |
| Functions | `:5001` |
| Emulator UI | `:4000` |

---

## Testing

```
169 pruebas unitarias + 38 instrumentadas = 207 total
```

```bash
./gradlew test                          # Unit tests
./gradlew connectedAndroidTest          # Instrumented tests
./gradlew pixel4Api27DebugAndroidTest   # Managed device API 27
./gradlew pixel4Api35DebugAndroidTest   # Managed device API 35
```

### Cobertura

| Métrica | Resultado | Meta |
|---|---|---|
| Declaraciones | ~78% | ≥ 70% |
| Ramas | ~82% | ≥ 70% |
| Caminos críticos | ~93% | ≥ 80% |

---

## Seguridad

Escaneo OWASP Dependency-Check integrado en CI/CD.

| Métrica | Resultado |
|---|---|
| CVEs iniciales | 710 |
| CVEs remanentes | 35 (-95%) |
| Vulnerabilidades críticas (CVSS ≥ 7.0) | 0 |

---

## Estructura del Repositorio

```
APP-KT/
├── app/                    # Módulo principal Android
├── functions/              # Firebase Cloud Functions (TypeScript)
├── firestore.rules         # Reglas de seguridad Firestore
├── firestore.indexes.json  # Índices Firestore
├── firebase.json           # Configuración Firebase
├── owasp-suppressions.xml  # Supresiones OWASP
├── build.gradle.kts        # Build script raíz
├── settings.gradle.kts     # Configuración de módulos
├── gradle.properties       # Propiedades Gradle
└── gradle/                 # Gradle wrapper
```

---

## Licencia

© CYRYEL E.I.R.L. Todos los derechos reservados.
