# Plan de Delivery

## Flujo de estados

### Order.status
```
pendiente → confirmado → en_reparto → entregado
```

### Delivery.status
```
disponible → aceptado → en_camino → entregado
```

### Secuencia completa

| Paso | Quién | Order.status | Delivery.status |
|------|-------|-------------|-----------------|
| Cliente crea pedido (deliveryMethod="delivery") | Cliente | `pendiente` | — |
| Admin confirma | Admin | `confirmado` | se crea como `disponible` |
| Repartidor acepta | Repartidor | `en_reparto` | `aceptado` |
| Repartidor va al cliente | Repartidor | `en_reparto` | `en_camino` |
| Repartidor ingresa código de confirmación | Repartidor | `entregado` | `entregado` |

> Pedidos con `deliveryMethod="tienda"` no pasan por delivery.
> Solo pedidos `deliveryMethod="delivery"` generan delivery disponible.

## Asignación: "Primero en llegar, se lo lleva"

1. Admin confirma pedido → se crea delivery `disponible`
2. Notificación push a todos los repartidores libres
3. En la app ven una lista de pedidos disponibles
4. El primero que toca **"Aceptar"** se lo lleva (transacción atómica en Firestore)
5. Los demás ven "Ya no disponible"
6. Si nadie acepta en X minutos → notificar al admin

## Firestore

### Colecciones nuevas

**deliveries/{deliveryId}**
```
orderId: string
status: string (disponible, aceptado, en_camino, entregado)
confirmationCode: string (4 dígitos)
assignedAt: timestamp
acceptedAt: timestamp (nullable)
deliveredAt: timestamp (nullable)
deliveryPersonId: string (nullable hasta que alguien acepte)
fcmToken: string (del repartidor que aceptó)
```

**delivery_locations/{orderId}**
```
latitude: number
longitude: number
updatedAt: timestamp
```

### Colecciones existentes (modificar)

**users/{userId}**
```
+ role: string (cliente, repartidor, admin)
+ isAvailable: boolean (para repartidores)
```

**orders/{orderId}**
```
+ deliveryConfirmed: boolean
+ confirmationCode: string (el mismo del delivery)
```

## Pantallas (app Android, rol = repartidor)

### Navegación

Si `role == "repartidor"` → **DeliveryNavGraph** en lugar de MainScreen de cliente.

Bottom nav con:
1. **Pedidos** — DeliveryHomeScreen
2. **Historial** — entregas completadas
3. **Perfil** — toggle disponible/no disponible

### DeliveryHomeScreen
- Tab1: **Disponibles** — pedidos `disponible` que nadie ha aceptado
- Tab2: **Mis pedidos** — pedidos que el repartidor aceptó (activos)
- Cada card: dirección, items, tiempo estimado, botón "Aceptar"

### DeliveryOrderScreen
- Datos del cliente y dirección
- Mapa con ruta (cliente ↔ repartidor)
- Botones según estado:
  - `aceptado` → "Recogí el pedido" → `en_camino`
  - `en_camino` → solicitar código de confirmación → input 4 dígitos
  - Código válido → `entregado`

### LocationUploader
- Servicio en background mientras haya pedido activo
- Sube ubicación cada N segundos a `delivery_locations/{orderId}`
- Se detiene al entregar

## Cloud Functions (servidor)

- **onOrderConfirmed**: si `deliveryMethod == "delivery"`, crear delivery `disponible` con código aleatorio de 4 dígitos. Notificar a repartidores.
- **onDeliveryAccepted**: transacción atómica, validar que siga `disponible`, asignar a quien aceptó. Notificar al admin.
- **onDeliveryConfirmed**: validar código, cambiar Order.status a `entregado`. Notificar al cliente.
- **checkExpiredDeliveries** (cron/scheduled): deliveries `disponible` con más de X min → notificar admin.

## Pendiente de definir

- Tiempo de expiración para deliveries `disponible` (¿5 min? ¿10 min?)
- ¿El repartidor puede rechazar después de aceptar?
- ¿Código de confirmación lo genera el sistema o el cliente?
- ¿Notificaciones por SMS además de push?
