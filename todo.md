# Pendientes / Features

## Checkout paso 2
- [x] Al cargar con dirección default, auto-seleccionar ubicación en mapa sin necesidad de click
- [x] Mostrar `reference` de la dirección guardada en la card y en "Ubicación seleccionada"

## Gestión de direcciones (Perfil)
- [x] Agregar campo opcional "Referencia" al formulario de dirección
- [x] Guardar/editar `reference` en Firestore
- [x] Mostrar referencia en las cards de dirección

## Pedido rápido (Inicio)
- [x] Refrescar productos del pedido rápido cada vez que se entra a la pestaña Inicio
- [x] Resetear "Agregar todo" al refrescar productos

## UI / UX
- [x] Dark theme: colores adaptables en map dialogs (checkout + address)
- [x] Cards de dirección: borde `outlineVariant` para visibilidad en light theme
- [x] Perfil: separar campos DNI y RUC (independientes, sin toggle)
- [x] App name: "Cyryel Store", versión 2.0.0
- [x] Google sign-out al cerrar sesión (account picker reaparece)
- [x] Eliminar `ProfileScreen.kt` (no usado, migrado a `ProfileComponents.kt`)
- [x] Street/city read-only en formulario (se llenan desde el map picker)
- [x] Solo 1 dirección default a la vez (confirmación al reemplazar)
- [x] Toast "Dirección guardada correctamente" al guardar
