<task id="ses_0ea485f7cffev3P5RtEz1SODzk" state="completed">
<task_result>
# Análisis de Reglas de Seguridad Firestore — Tienda CYRYEL

## 1. Resumen General

Las reglas actuales protegen **10 colecciones** (`users`, `categories`, `flash`, `products`, `promotions`, `orders`, `notificaciones`, `auditLogs`, `paymentTimeouts`, más subcolecciones `savedLocations` y `pointsHistory`). Se definen **4 funciones helper** (`isSignedIn`, `isOwner`, `isAdmin`, `isDelivery`) y **2 funciones auxiliares** (`hasRequiredFields`, `isValidTimestamp`) más una función `isDeliveryForOrder` que nunca se invoca.

**Estado general: Funcional pero con varias fisuras de seguridad que deben corregirse antes de producción.** Las reglas cubren correctamente los casos de lectura pública para catálogo y protegen escrituras administrativas, pero adolecen de problemas graves en la colección `orders`, especialmente en los permisos de `list` y `update`.

---

## 2. Análisis por Sección

### `users/{userId}`

```js
match /users/{userId} {
  allow get: if isOwner(userId) || isAdmin() || 
                (isDelivery() && resource.data.role == 'delivery');
  allow list: if isAdmin();
  allow create: if request.auth != null && request.auth.uid == userId &&
                   hasRequiredFields(['email', 'name', 'role']);
  allow update: if isOwner(userId) || isAdmin();
  allow delete: if isAdmin();
}
```

✅ **Aciertos:**
- `create` exige que el UID del documento coincida con `request.auth.uid`, evitando suplantación.
- `list` solo para admin.
- `delete` solo para admin.

⚠️ **Problemas:**
- **Un delivery puede leer el perfil de otros deliveries.** La condición `(isDelivery() && resource.data.role == 'delivery')` permite que cualquier delivery lea el documento de *cualquier* otro delivery. No hay justificación funcional para esto. Un delivery debería poder leer solo su propio perfil o, como máximo, aquellos asignados a órdenes comunes. Esto expone datos personales (email, nombre, rol) de todos los deliveries entre sí.
- **`update` permite a `isOwner()` modificar su propio `role`.** Aunque el `create` exige `role`, un `update` de usuario no valida que `role` no pueda cambiarse. Un usuario podría promover su propia cuenta a `admin` o `delivery` mediante un update malicioso.
- **`create` no valida que `role` sea uno de los valores permitidos.** Cualquier usuario al registrarse puede poner `role: 'admin'` o `role: 'delivery'` en lugar de `role: 'user'`.

💡 **Sugerencias:**
- Eliminar la cláusula `(isDelivery() && resource.data.role == 'delivery')` del `get`. Si un delivery necesita leer datos de otro delivery, debe hacerse vía Cloud Function o petición administrativa.
- Validar que el `role` sea exactamente `'user'` en `create`, o usar una Cloud Function que asigne el rol post-registro.
- En `update`, denegar cambios al campo `role` (a menos que el escritor sea admin):

```js
allow update: if (isOwner(userId) && !('role' in request.resource.data.diff.delta))
               || isAdmin();
```

---

### `users/{userId}/savedLocations/{locationId}`

✅ No hay problemas reseñables. La lógica `isOwner` para CRUD es correcta. El `delete` adicional para admin es aceptable.

---

### `users/{userId}/pointsHistory/{entryId}`

✅ **Aciertos:** Solo lectura por owner/admin, solo escritura por admin.

⚠️ **Problemas:** El `list` se permite a owner, lo cual está bien, pero combinado con el diseño actual, un owner puede listar todo su historial de puntos sin restricciones. Esto es correcto funcionalmente.

---

### `categories`, `flash`, `products`, `promotions`

```js
allow read: if true;
allow write: if isAdmin();
```

✅ **Aciertos:** Lectura pública, escritura solo admin. Es el patrón correcto para un catálogo.

---

### `orders/{orderId}` — EL PUNTO CRÍTICO

```js
match /orders/{orderId} {
  allow get: if isOwner(resource.data.userId) || isDelivery() || isAdmin();
  allow list: if isAdmin() || isDelivery() || isSignedIn();
  allow create: if isSignedIn() && hasRequiredFields([...]) &&
                   request.resource.data.userId == request.auth.uid;
  allow update: if (isOwner(resource.data.userId) && !('assignedDeliveryId' in request.resource.data.diff.delta)) ||
                   (isDelivery() && (request.auth.uid == resource.data.assignedDeliveryId || !('assignedDeliveryId' in resource.data))) ||
                   isAdmin();
  allow delete: if isAdmin();
}
```

✅ **Aciertos:**
- `get` correcto: owner por `userId`, delivery, admin.
- `create` correcto: obliga `userId == auth.uid` y campos requeridos.
- `delete` solo admin.
- La intención de proteger `assignedDeliveryId` de modificaciones por el owner es buena.

⚠️ **Problemas GRAVES:**

**1. `list: if isAdmin() || isDelivery() || isSignedIn()` — FILTRADO DEL LADO DEL CLIENTE**
Cualquier usuario autenticado puede listar TODAS las órdenes del sistema. Aunque la app filtre en el cliente por `userId`, esto es inseguro porque:
- Cualquier usuario puede hacer una consulta sin filtro y obtener todas las órdenes (con direcciones, totales, métodos de pago, etc.).
- Viola el principio de menor privilegio.
- **Solución:** Restringir `list` a admin. Para usuarios normales, usar `get` por ID conocido. Si se necesita una lista del historial del usuario, añadir una subcolección `users/{userId}/orders` con referencias, o crear un índice compuesto y usar `resource.data.userId == request.auth.uid`:

```js
allow list: if isAdmin() || isDelivery() || 
               (isSignedIn() && resource.data.userId == request.auth.uid);
```

**2. Update de delivery — condición `!('assignedDeliveryId' in resource.data)` es demasiado permisiva**
Un delivery NO asignado a una orden puede modificar cualquier orden que *aún no tenga* un `assignedDeliveryId`. Es decir, cualquier delivery puede reclamar cualquier orden y modificarla a su antojo. Debería requerirse un mecanismo de asignación explícito (ej: solo admin asigna, o el delivery solo puede asignarse a sí mismo mediante una Cloud Function que valide zona/estado).

```js
// Más seguro:
allow update: if (isOwner(resource.data.userId) && !('assignedDeliveryId' in request.resource.data.diff.delta)) ||
               (isDelivery() && request.auth.uid == resource.data.assignedDeliveryId && 
                request.resource.data.diff.delta.keys().hasOnly(['status', 'deliveredAt', 'notes'])) ||
               isAdmin();
```

**3. Owner puede actualizar cualquier campo excepto `assignedDeliveryId`**
Actualmente un owner puede modificar `total`, `paymentMethod`, `status`, `items`, etc. después de creada la orden. Esto permite a un usuario cambiar el total de su orden a 0, o modificar items después del pago. Las actualizaciones del owner deberían limitarse a campos inocuos como `deliveryAddress` o `notes` antes de cierto estado.

💡 **Sugerencias generales para `orders`:**
- Implementar un estado de workflow (ej: `pending → confirmed → preparing → in_transit → delivered → completed`) y validar transiciones permitidas en Security Rules o Cloud Functions.
- Usar Cloud Functions como autoridad única para cambios de estado críticos (pago, asignación de delivery, confirmación).
- Para el delivery, usar `hasOnly()` para limitar los campos que puede modificar.

---

### `notificaciones/{notificationId}`

```js
allow get: if isOwner(resource.data.userId);
allow list: if isOwner(resource.data.userId);
allow create: if isSignedIn() && hasRequiredFields(['userId', 'titulo', 'mensaje', 'tipo', 'fecha']);
allow update: if isOwner(resource.data.userId);
allow delete: if isOwner(resource.data.userId) || isAdmin();
```

✅ **Aciertos:** Owner checks correctos.

⚠️ **Problemas:**
- `create` permite a CUALQUIER usuario autenticado crear notificaciones para cualquier `userId` (no se valida que `userId == request.auth.uid`). Cualquier usuario podría crear notificaciones falsas dirigidas a otros usuarios.
- **Solución:** Validar que `request.resource.data.userId == request.auth.uid` en `create`. Las notificaciones administrativas deberían generarse desde una Cloud Function con Admin SDK (que usa service account y evade Security Rules):

```js
allow create: if isSignedIn() && 
                hasRequiredFields(['userId', 'titulo', 'mensaje', 'tipo', 'fecha']) &&
                request.resource.data.userId == request.auth.uid;
```

---

### `auditLogs/{logId}` y `paymentTimeouts/{timeoutId}`

```js
allow read: if isAdmin();
allow write: if false;
```

✅ Correcto: solo lectura admin, escritura deshabilitada para todo cliente. Estas colecciones son escritas exclusivamente por Cloud Functions (Backend), que usan Admin SDK y no pasan por Security Rules.

---

## 3. `isDeliveryForOrder(orderId)` — Definida pero nunca usada

```js
function isDeliveryForOrder(orderId) {
  return isDelivery() && isSignedIn();
}
```

**Problema:** La función está definida pero **nunca es invocada** en ninguna regla. Además, su implementación es idéntica a `isDelivery()` puesto que el parámetro `orderId` no se utiliza en el cuerpo. Parece una función a medio diseñar que pretendía verificar si un delivery está asignado a una orden específica.

**Corrección:**
- Si no se necesita, eliminarla para reducir código muerto.
- Si se necesita, completar la implementación:

```js
function isDeliveryForOrder(orderId) {
  return isDelivery() && 
         get(/databases/$(database)/documents/orders/$(orderId)).data.assignedDeliveryId == request.auth.uid;
}
```

Y luego usarla en `orders update` para la rama de delivery.

---

## 4. `orders` — `list` para usuarios normales

**Gravedad: ALTA**

Cualquier usuario autenticado puede ejecutar:

```js
db.collection("orders").get()
```

y obtener todas las órdenes del sistema con datos sensibles (direcciones, totales, métodos de pago, nombres). Aunque la app filtre por `userId` en el lado del cliente, esto no es una defensa — un atacante puede usar la consola del navegador, Postman, o simplemente interceptar la petición de Firestore SDK.

**Mitigación inmediata:**

```js
allow list: if isAdmin() || 
               isDelivery() ||
               (isSignedIn() && resource.data.userId == request.auth.uid);
```

Con esta regla, si un usuario normal intenta listar sin un filtro `where("userId", "==", uid)`, Firestore rechazará la consulta automáticamente (gracias al *query-based security* de Firestore).

---

## 5. `orders` — Update de delivery sin `assignedDeliveryId`

**Gravedad: ALTA**

```js
(isDelivery() && (request.auth.uid == resource.data.assignedDeliveryId || !('assignedDeliveryId' in resource.data)))
```

El segundo branch `!('assignedDeliveryId' in resource.data)` significa: "cualquier delivery puede modificar cualquier orden que no tenga aún un delivery asignado". Esto permite:
- Que cualquier delivery modifique el `status`, `total`, `items` de una orden sin asignar.
- Que un delivery se asigne a sí mismo modificando `assignedDeliveryId` en el update.

**Mitigación:**

1. Solo admin puede asignar un delivery a una orden.
2. Un delivery solo puede modificar órdenes donde `assignedDeliveryId == request.auth.uid`.
3. Un delivery solo puede modificar campos específicos (`status` a `in_transit`/`delivered`, `deliveredAt`, `notes`):

```js
allow update: if (isOwner(resource.data.userId) && 
                   !('assignedDeliveryId' in request.resource.data.diff.delta) &&
                   request.resource.data.diff.delta.keys().hasOnly(['deliveryAddress', 'notes'])) ||
               (isDelivery() && 
                request.auth.uid == resource.data.assignedDeliveryId &&
                request.resource.data.diff.delta.keys().hasOnly(['status', 'deliveredAt', 'notes'])) ||
               isAdmin();
```

---

## 6. `users` — `get` para delivery de otros deliveries

**Gravedad: MEDIA**

```js
(isDelivery() && resource.data.role == 'delivery')
```

Un delivery puede leer el perfil (email, nombre, teléfono, rol) de cualquier otro delivery en el sistema. Esto expone información personal innecesariamente.

**Alternativas:**
- Eliminar esta cláusula por completo: `allow get: if isOwner(userId) || isAdmin();`. Si un delivery necesita datos de otro delivery, debería hacerse mediante una consulta administrativa o una Cloud Function con lógica de negocio.
- Si es necesario para funcionalidad (ej: ver repartidores disponibles en el mapa), entonces limitar los campos expuestos mediante `request.resource.data` y `resource.data`, o mejor aún, usar una subcolección pública limitada.

---

## 7. `isValidTimestamp` — Definida pero nunca usada

```js
function isValidTimestamp() {
  return request.resource.data.createdAt <= now || 
         !request.resource.data.keys().hasAny(['createdAt']);
}
```

**Problema:** Código muerto. Definida pero jamás invocada en ninguna regla.

Además, la lógica es cuestionable: permite que un documento no tenga `createdAt` (segundo branch), lo que en la práctica anula la validación. Si se trataba de evitar timestamps futuros, el primer branch `createdAt <= now` es correcto, pero el segundo permite evadirlo simplemente omitiendo el campo.

**Recomendación:** Eliminar la función si no se necesita, o implementarla correctamente:

```js
function isValidTimestamp() {
  return 'createdAt' in request.resource.data && 
         request.resource.data.createdAt <= now;
}
```

---

## 8. Evaluación General

| Aspecto | Estado |
|---|---|
| **Lectura de catálogo** | ✅ Correcto (público) |
| **Escritura admin** | ✅ Correcto |
| **Protección de usuarios** | ⚠️ Parcial — rol modificable por owner, deliveries se ven entre sí |
| **Órdenes — get** | ✅ Correcto |
| **Órdenes — list** | ❌ Grave — cualquier usuario ve todas las órdenes |
| **Órdenes — update** | ❌ Grave — delivery puede modificar órdenes sin asignar |
| **Órdenes — create** | ✅ Correcto |
| **Notificaciones — create** | ⚠️ Cualquier usuario puede crear notifs para otros |
| **Código muerto** | ⚠️ `isDeliveryForOrder` e `isValidTimestamp` definidas pero no usadas |
| **Validación de campos** | ⚠️ `hasRequiredFields` no valida tipos ni valores permitidos (ej: `role`) |

**Puntuación estimada: 6/10** — Funcional para desarrollo pero inseguro para producción.

---

## 9. Recomendaciones Priorizadas

### Prioritarias (deben hacerse ANTES de producción)

| # | Cambio | Colección | Gravedad |
|---|---|---|---|
| 1 | **Restringir `list` en `orders`**: solo admin, delivery, o usuario con filtro `userId == auth.uid` | `orders` | 🔴 Alta |
| 2 | **Restringir `update` de delivery**: solo si `assignedDeliveryId == auth.uid` y solo campos específicos (`status`, `deliveredAt`, `notes`) | `orders` | 🔴 Alta |
| 3 | **Restringir `create` de `notificaciones`**: obligar `userId == request.auth.uid` | `notificaciones` | 🔴 Alta |
| 4 | **Restringir `update` de usuario**: impedir que el owner cambie su propio `role` | `users` | 🔴 Alta |
| 5 | **Restringir `create` de usuario**: validar que `role` sea exactamente `'user'` | `users` | 🔴 Alta |

### Importantes

| # | Cambio | Gravedad |
|---|---|---|
| 6 | Eliminar `isDelivery() && resource.data.role == 'delivery'` del `get` de `users` | 🟡 Media |
| 7 | Implementar o eliminar `isDeliveryForOrder(orderId)` — si se necesita, usar el parámetro | 🟡 Media |
| 8 | Implementar o eliminar `isValidTimestamp()` | 🟢 Baja |
| 9 | Usar `hasOnly()` en updates para limitar campos modificables por no-admin | 🟢 Baja |

### Opcionales (buena práctica)

| # | Cambio | Gravedad |
|---|---|---|
| 10 | Migrar cambios de estado de orden a Cloud Functions en vez de permitir updates directos | 🟢 Baja |
| 11 | Agregar validación de transiciones de estado (ej: `preparing` no puede saltar a `delivered`) | 🟢 Baja |
| 12 | Agregar rate limiting vía `request.limit` en colecciones públicas para prevenir abusos | 🟢 Baja |

---

### Versión corregida (snippet de las reglas clave)

```js
// ===== ORDERS (corregido) =====
match /orders/{orderId} {
  allow get: if isOwner(resource.data.userId) || isDelivery() || isAdmin();
  
  allow list: if isAdmin() || isDelivery() || 
                 (isSignedIn() && resource.data.userId == request.auth.uid);
  
  allow create: if isSignedIn() &&
                   hasRequiredFields(['userId', 'items', 'total', 'status', 
                                      'deliveryAddress', 'paymentMethod', 'paymentStatus']) &&
                   request.resource.data.userId == request.auth.uid;
  
  allow update: if (isOwner(resource.data.userId) && 
                     !('assignedDeliveryId' in request.resource.data.diff.delta) &&
                     request.resource.data.diff.delta.keys().hasOnly(['deliveryAddress', 'notes'])) ||
                   (isDelivery() && 
                    request.auth.uid == resource.data.assignedDeliveryId &&
                    request.resource.data.diff.delta.keys().hasOnly(['status', 'deliveredAt', 'notes'])) ||
                   isAdmin();
  
  allow delete: if isAdmin();
}

// ===== USERS (corregido) =====
match /users/{userId} {
  allow get: if isOwner(userId) || isAdmin();
  allow list: if isAdmin();
  allow create: if request.auth.uid == userId &&
                   hasRequiredFields(['email', 'name', 'role']) &&
                   request.resource.data.role == 'user';
  allow update: if (isOwner(userId) && 
                     !('role' in request.resource.data.diff.delta)) || 
                   isAdmin();
  allow delete: if isAdmin();
  // ... subcolecciones
}

// ===== NOTIFICACIONES (corregido) =====
match /notificaciones/{notificationId} {
  allow get: if isOwner(resource.data.userId);
  allow list: if isOwner(resource.data.userId);
  allow create: if isSignedIn() && 
                   hasRequiredFields(['userId', 'titulo', 'mensaje', 'tipo', 'fecha']) &&
                   resource.data.userId == request.auth.uid;
  allow update: if isOwner(resource.data.userId);
  allow delete: if isOwner(resource.data.userId) || isAdmin();
}
```

---

**Conclusión:** Las reglas actuales son un buen primer borrador con la estructura correcta, pero tienen 5 vulnerabilidades que deben corregirse antes de cualquier despliegue a producción. La más crítica es el `list` abierto en `orders`, que expone datos sensibles de todos los clientes. Se recomienda aplicar los cambios priorizados y luego ejecutar una suite de tests con el Emulator Suite para validar cada permiso.
</task_result>
</task>