# Tienda CYRYEL Native (Kotlin)

Proyecto Android nativo base para migrar gradualmente desde Ionic/Angular.

## Stack inicial
- Kotlin + Jetpack Compose
- MVVM (base)
- Hilt (inyeccion de dependencias)
- Firebase (Analytics, Auth, Firestore, Messaging)

## Requisitos
- Android Studio (version reciente)
- JDK 17
- Variable de entorno `JAVA_HOME` apuntando al JDK 17
- Android SDK instalado (API 35)

## Ejecutar
1. Abrir carpeta `android-native-kotlin` en Android Studio.
2. Esperar sincronizacion de Gradle.
3. Ejecutar modulo `app`.

## Nota
Si ejecutas por terminal y aparece `JAVA_HOME is not set`, configura esa variable y vuelve a correr `gradlew.bat`.
