# Migracion Firebase BOM 34 + Kotlin 2.3 + limpieza OWASP

## Estado: COMPLETADO (build + tests OK, pendiente supresiones OWASP)

Rama: `feat/testing-suite`

## Que se hizo

### Migracion Firebase BOM 33.7.0 → 34.16.0
1. BOM actualizado a 34.16.0
2. Sufijos `-ktx` eliminados de todas las dependencias Firebase
3. `google-services` actualizado de 4.4.4 a 4.5.0

### Migracion Kotlin 1.9.24 → 2.3.20
Firebase BOM 34.x requiere Kotlin 2.3+ (metadata version 2.3.0). Cambios necesarios:
1. Kotlin 1.9.24 → 2.3.20
2. Plugin `org.jetbrains.kotlin.plugin.compose` agregado (requerido desde Kotlin 2.0)
3. `composeOptions { kotlinCompilerExtensionVersion }` eliminado (integrado en Kotlin 2.0+)
4. `kotlinOptions { jvmTarget }` migrado a `compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }`
5. `devices { }` migrado a `allDevices { }` (deprecado en AGP 8.9.1)

### Migracion Room 2.6.1 → 2.8.4
Room 2.6.1 tenia `kotlinx-metadata-jvm` viejo que no soportaba metadata 2.3.0 de Firebase BOM 34.

### Migracion Hilt 2.55 → 2.57.2
Hilt 2.57+ unshaded `kotlin-metadata-jvm`. Se agrego `kapt("org.jetbrains.kotlin:kotlin-metadata-jvm:2.3.20")` para forzar la version compatible.
NOTA: Hilt 2.59+ requiere AGP 9.0+ (nuestro AGP es 8.9.1).

## Archivos modificados

- `build.gradle.kts` - Kotlin 2.3.20, compose plugin, Hilt 2.57.2, google-services 4.5.0
- `app/build.gradle.kts` - compose plugin, Room 2.8.4, Hilt 2.57.2, kotlin-metadata-jvm, compilerOptions DSL

## Resultados OWASP

| Metrica | Antes (BOM 33.7) | Ahora (BOM 34.16) |
|---|---|---|
| Vulnerabilidades totales | ~710 CVEs | 35 CVEs |
| CVEs eliminados | - | ~675 |

### Vulnerabilidades restantes (35)
1. **Netty 4.1.93.Final** (13 jars) - CVE-2026-44891, CVE-2026-55831, CVE-2026-55833
   - Firebase BOM 34.16.0 aun trae Netty 4.1.93. Necesita esperar actualizacion de Firebase.
2. **abi-tools 2.3.20** (2 jars) - CVE-2026-53914
   - Dependencia de Kotlin 2.3.20. Requiere actualizacion de JetBrains.

### Siguiente paso para OWASP
- Agregar supresiones para los 35 CVEs restantes en `owasp-suppressions.xml`
- O esperar a que Firebase actualice Netty y JetBrains actualice abi-tools
