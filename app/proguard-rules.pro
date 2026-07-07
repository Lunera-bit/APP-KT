# Datos Firestore - mantener serialización con toObject()/set()
-keep class com.CYRYEL.com.data.** { *; }

# Room entities
-keep class com.CYRYEL.com.data.local.ProductEntity { *; }

# Deliveries
-keep class com.CYRYEL.com.data.delivery.** { *; }

# Mapbox geometry
-dontwarn com.mapbox.**
-keep class com.mapbox.geojson.** { *; }
-keep class com.mapbox.maps.** { *; }

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn com.google.gson.**
-keep class com.google.gson.** { *; }

# Firebase (reglas manejadas por las propias dependencias)

# Services
-keep class com.CYRYEL.com.service.** { *; }

# Hilt/Dagger (generado en compilación, mantener por si hay reflection)
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }

# Lottie
-dontwarn com.airbnb.lottie.**
-keep class com.airbnb.lottie.** { *; }

# Coil
-dontwarn coil.**
-keep class coil.** { *; }

# Kotlin coroutines
-dontwarn kotlinx.coroutines.**
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep BuildConfig
-keep class com.CYRYEL.com.BuildConfig { *; }
