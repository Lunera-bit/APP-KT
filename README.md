# CYRYEL STORE - Android App

Aplicación nativa de Android para CYRYEL E.I.R.L. (Chancay, Perú). Sistema de ventas móvil con catálogo de productos, carrito de compras, checkout, seguimiento de pedidos, billetera de puntos y panel de repartidor. **En producción en Google Play Store.**

## Stack Técnico

| Componente | Tecnología |
|---|---|
| Lenguaje | Kotlin 2.3.20 |
| UI | Jetpack Compose (Material 3) |
| Arquitectura | MVVM + Clean Architecture |
| DI | Hilt 2.57.2 |
| Base de datos local | Room 2.8.4 |
| Backend | Firebase BOM 34.16.0 |
| Autenticación | Firebase Auth + Google Sign-In |
| Base de datos | Cloud Firestore |
| Notificaciones | Firebase Cloud Messaging |
| Mapas | Mapbox SDK 11.25.0 |
| Animaciones | Lottie Compose 6.4.0 |
| Testing | JUnit 4 + MockK + Turbine + Espresso |
| Seguridad | OWASP Dependency-Check |
| CI/CD | GitHub Actions |

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
├── di/                    # Dependency Injection
│   └── FirebaseModule.kt  # Módulo Hilt
├── navigation/            # Navegación
│   └── NavGraph.kt        # Rutas Compose Navigation
├── ui/                    # Capa de presentación
│   ├── auth/              # Login (Google Sign-In)
│   ├── billetera/         # Billetera y puntos
│   ├── cart/              # Carrito
│   ├── catalog/           # Catálogo de productos
│   ├── checkout/          # Flujo de checkout
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
│   ├── theme/             # Tema Compose
│   └── util/              # Utilidades (WhatsApp, Toast)
└── MainActivity.kt        # Activity principal
```

## Requisitos

- Android Studio Ladybug (2024.2.1) o superior
- JDK 17
- SDK Android 35 (compileSdk)
- Mínimo: Android 7.0 (API 24)
- Cuenta de Firebase con proyecto configurado
- Token de Mapbox

## Configuración

1. Clonar el repositorio:
```bash
git clone https://github.com/Lunera-bit/APP-KT.git
cd APP-KT
```

2. Crear `local.properties` en la raíz:
```properties
MAPBOX_ACCESS_TOKEN=tu_token_mapbox
MAPBOX_SECRET_TOKEN=tu_secret_mapbox
KEYSTORE_PATH=/ruta/a/tu/keystore.jks
KEYSTORE_PASSWORD=tu_password
KEY_ALIAS=tu_alias
KEY_PASSWORD=tu_password_key
```

3. Colocar `google-services.json` de Firebase en `app/`.

4. Sincronizar y build:
```bash
./gradlew assembleDebug
```

## Firebase Emulator (desarrollo local)

```bash
firebase emulators:start
```

Emuladores disponibles: Auth (:9099), Firestore (:8080), Functions (:5001).

## Testing

169 pruebas unitarias + 38 instrumentadas = 207 total.

```bash
# Pruebas unitarias
./gradlew test

# Pruebas instrumentadas (requiere emulador o dispositivo)
./gradlew connectedAndroidTest

# Gradle Managed Devices
./gradlew pixel4Api27DebugAndroidTest
./gradlew pixel4Api35DebugAndroidTest
```

### Cobertura de código

| Nivel | Cobertura |
|---|---|
| Declaraciones | ~78% |
| Ramas | ~82% |
| Caminos críticos | ~93% |

## Seguridad

Escaneo OWASP Dependency-Check configurado. Resultado: 710 CVEs reducidos a 35 CVEs (-95%), 0 vulnerabilidades críticas (CVSS >= 7.0).

## Estructura del Proyecto

```
APP-KT/
├── app/                    # Módulo principal Android
├── docs/                   # Documentación y scripts de informe
├── functions/              # Firebase Cloud Functions (TypeScript)
├── svg/                    # Assets de diseño
├── firestore.rules         # Reglas de seguridad Firestore
├── firestore.indexes.json  # Índices Firestore
├── firebase.json           # Configuración Firebase
├── owasp-suppressions.xml  # Supresiones OWASP
├── build.gradle.kts        # Build script raíz
├── settings.gradle.kts     # Configuración de módulos
└── gradle/                 # Gradle wrapper
```

## Licencia

© CYRYEL E.I.R.L. Todos los derechos reservados.
