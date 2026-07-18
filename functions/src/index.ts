import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';

// Inicializar Admin si aún no está inicializado
admin.initializeApp();

// ==============================
// CONSTANTES DE PUNTOS (EN CÓDIGO)
// Temporal: centralizar valores en código para ajustes rápidos.
// Más adelante se pueden mover a Firestore o functions.config()
// ==============================
const POINTS_BLOCK_AMOUNT_50 = 50; // monto en moneda por bloque pequeño
const POINTS_PER_BLOCK_50 = 1; // puntos por cada S/50
const DEFAULT_PROMO_UNIT_POINTS = 5; // puntos por unidad de promoción (fallback)

// Reglas anteriores (desactivadas, se mantienen como referencia):
// const POINTS_BLOCK_AMOUNT_600 = 600; // monto en moneda por bloque grande
// const POINTS_BLOCK_AMOUNT_300 = 300; // monto en moneda por bloque mediano
// const POINTS_PER_BLOCK_600 = 30; // puntos por cada S/600
// const POINTS_PER_BLOCK_300 = 10; // puntos por cada S/300

// ==============================
// CONSTANTE DE RESERVA DE STOCK
// ==============================
const STOCK_RESERVE = 4; // unidades de reserva paras evitar quedarse sin stock

/**
 * Calcula puntos basado en monto total:
 * - Cada S/50 completo = 1 punto
 * Ejemplos:
 *   S/50 = 1 punto
 *   S/100 = 2 puntos
 *   S/125 = 2 puntos
 *   S/1000 = 20 puntos
 */
function calculateBasePoints(total: number): number {
  if (total <= 0) return 0;

  // Bloques de 50 completos
  const blocksOf50 = Math.floor(total / POINTS_BLOCK_AMOUNT_50);

  return blocksOf50 * POINTS_PER_BLOCK_50;
}

/**
 * Verifica si existe una orden duplicada del mismo usuario con el mismo total
 * creada en los últimos 5 segundos.
 * Sirve para prevenir envíos duplicados accidentales por clicks múltiples.
 */
async function checkForDuplicateOrder(userId: string, total: number, currentOrderId: string): Promise<boolean> {
  const db = admin.firestore();
  const now = admin.firestore.Timestamp.now();
  const fiveSecondsAgo = new admin.firestore.Timestamp(now.seconds - 5, now.nanoseconds);

  try {
    const query = db.collection('orders')
      .where('userId', '==', userId)
      .where('total', '==', total)
      .where('createdAt', '>=', fiveSecondsAgo)
      .where('createdAt', '<=', now);

    const snapshot = await query.get();
    
    // Si hay más de un documento (el actual + otro duplicado), es un duplicado
    if (snapshot.size > 1) {
      // Verificar que al menos dos sean diferentes al actual
      let duplicateCount = 0;
      snapshot.forEach(doc => {
        if (doc.id !== currentOrderId) {
          duplicateCount++;
        }
      });
      return duplicateCount > 0;
    }
    
    return false;
  } catch (error) {
    functions.logger.error('[checkForDuplicateOrder] Error verificando duplicados:', error);
    // En caso de error, no bloquear el proceso
    return false;
  }
}

const STORE_LATITUDE = -11.567832;
const STORE_LONGITUDE = -77.269716;
const FREE_DELIVERY_RADIUS = 500;

function haversineDistance(lat1: number, lng1: number, lat2: number, lng2: number): number {
  const R = 6371000;
  const dLat = (lat2 - lat1) * Math.PI / 180;
  const dLng = (lng2 - lng1) * Math.PI / 180;
  const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
            Math.sin(dLng / 2) * Math.sin(dLng / 2);
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

/**
 * Calcula puntos basado en monto total:
 * - Cada S/50 completo = 1 punto
 * Ejemplos:
 *   S/50 = 1 punto
 *   S/100 = 2 puntos
 *   S/125 = 2 puntos
 *   S/1000 = 20 puntos
 *
 * `users/{uid}` hacia los custom claims del usuario (auth).
 * - Si `users/{uid}.role` existe, se escribe como `customClaims.role`.
 * - Si se elimina o queda vacío, se elimina el claim `role`.
 */
export const syncUserRoleToClaims = functions.firestore
  .document('users/{uid}')
  .onWrite(async (change, context) => {
    const uid = context.params.uid;
    const after = change.after.exists ? change.after.data() : null;

    const newRole = after && after.role ? String(after.role) : null;

    try {
      functions.logger.log(`[syncUserRoleToClaims] Trigger for uid=${uid}, newRole=${newRole}`);
      const userRecord = await admin.auth().getUser(uid).catch(() => null);
      const currentClaims = (userRecord && userRecord.customClaims) ? { ...userRecord.customClaims } : {};

      if (newRole) {
        // Solo actualizar si cambió
        if (currentClaims.role === newRole) {
          functions.logger.log(`[syncUserRoleToClaims] No change for ${uid}, role already '${newRole}'`);
          return null;
        }
        currentClaims.role = newRole;
        await admin.auth().setCustomUserClaims(uid, currentClaims);
        functions.logger.log(`Set custom claim role='${newRole}' for ${uid}`);
      } else {
        // Si no hay role en Firestore, eliminar el claim si existe
        if (!('role' in currentClaims)) return null;
        delete currentClaims.role;
        await admin.auth().setCustomUserClaims(uid, currentClaims);
        functions.logger.log(`Removed custom claim 'role' for ${uid}`);
      }

      return null;
    } catch (err) {
      functions.logger.error('Error syncing role claim for', uid, err);
      throw err;
    }
  });

/**
 * Revierte puntos si una orden es cancelada o devuelta (o su paymentStatus es 'reembolsado').
 * - Solo revierte si previamente se otorgaron puntos (`pointsAwarded: true`) y aún no se revirtieron.
 * - Usa `pointsAwardAmount` si está presente, si no, recalcula usando la misma regla.
 */
export const onOrderPointsReverted = functions.firestore
  .document('orders/{orderId}')
  .onUpdate(async (change, context) => {
    const before = change.before.data();
    const after = change.after.data();
    const orderId = context.params.orderId;

    try {
      const prevStatus = before?.status || null;
      const newStatus = after?.status || null;
      const prevPayment = before?.paymentStatus || null;
      const newPayment = after?.paymentStatus || null;

      const shouldRevertStatus = (prevStatus !== newStatus) && (newStatus === 'cancelado' || newStatus === 'devuelto');
      const shouldRevertPayment = (prevPayment !== newPayment) && (newPayment === 'reembolsado');
      if (!shouldRevertStatus && !shouldRevertPayment) return null;

      if (!after?.pointsAwarded) {
        console.log(`[onOrderPointsReverted] Orden ${orderId} no tiene puntos otorgados, nada que revertir`);
        return null;
      }

      if (after?.pointsReverted) {
        console.log(`[onOrderPointsReverted] Orden ${orderId} ya tiene puntos revertidos`);
        return null;
      }

      const userId = after?.userId;
      if (!userId) {
        console.warn(`[onOrderPointsReverted] Orden ${orderId} no tiene userId`);
        return null;
      }

      // Determinar cantidad a revertir
      let pointsToRevert = Number(after?.pointsAwardAmount || 0);
      if (!pointsToRevert || pointsToRevert <= 0) {
        // Recalcular con misma lógica que awarding
        const total = Number(after?.total || after?.subtotal || 0);
        const basePoints = calculateBasePoints(total);
        const items = Array.isArray(after?.items) ? after.items : [];
        let promoPoints = 0;
        let productPoints = 0;
        try {
          const promoQtyMap: { [id: string]: number } = {};
          const productQtyMap: { [id: string]: number } = {};
          for (const it of items) {
            try {
              if (!it) continue;
              const isPromo = !!(it.isPromotion || it.promotionData || it.promotionId);
              const qty = Number(it.quantity || 0) || 0;

              if (isPromo) {
                const pid = it.promotionId || (it.promotionData && (it.promotionData.id || it.promotionData.promotionId)) || null;
                if (pid) {
                  promoQtyMap[pid] = (promoQtyMap[pid] || 0) + qty;
                } else {
                  promoPoints += qty * DEFAULT_PROMO_UNIT_POINTS;
                }
              } else {
                if (it.redeemedByPoints) continue;
                const productId = it.productId;
                if (!productId) continue;
                productQtyMap[productId] = (productQtyMap[productId] || 0) + qty;
              }
            } catch (e) { /* ignore */ }
          }

          const db = admin.firestore();
          const promoIds = Object.keys(promoQtyMap);
          if (promoIds.length > 0) {
            const promoDocs = await Promise.all(promoIds.map(id => db.collection('promotions').doc(id).get()));
            promoDocs.forEach((pd) => {
              if (!pd.exists) return;
              const pdata: any = pd.data() || {};
              const pointsPerUnit = (typeof pdata.points === 'number') ? pdata.points : DEFAULT_PROMO_UNIT_POINTS;
              const q = promoQtyMap[pd.id] || 0;
              promoPoints += pointsPerUnit * q;
            });
          }

          const productIds = Object.keys(productQtyMap);
          if (productIds.length > 0) {
            const productDocs = await Promise.all(productIds.map(id => db.collection('products').doc(id).get()));
            productDocs.forEach((pd) => {
              if (!pd.exists) return;
              const pdata: any = pd.data() || {};
              const pointsPerUnit = (typeof pdata.points === 'number') ? pdata.points : 0;
              const q = productQtyMap[pd.id] || 0;
              productPoints += pointsPerUnit * q;
            });
          }
        } catch (e) {
          console.warn('[onOrderPointsReverted] Error calculando puntos de promociones/productos', e);
        }
        pointsToRevert = (basePoints || 0) + (promoPoints || 0) + (productPoints || 0);
      }

      if (!pointsToRevert || pointsToRevert <= 0) {
        console.log(`[onOrderPointsReverted] Orden ${orderId} no tiene puntos a revertir (calculado=0)`);
        await change.after.ref.update({ pointsReverted: false, pointsRevertedAt: admin.firestore.FieldValue.serverTimestamp() });
        return null;
      }

      const db = admin.firestore();
      
      // Puntos a revertir: puntos otorgados - puntos que se usaron en el checkout
      const pointsUsedInCheckout = Number(after?.pointsUsed || 0);
      const pointsToRevertFromUser = pointsToRevert - pointsUsedInCheckout;
      
      await db.runTransaction(async (tx) => {
        const userRef = db.collection('users').doc(userId);
        const userSnap = await tx.get(userRef);
        const userData = userSnap.exists ? userSnap.data() : undefined;
        const currentPoints = (userData && typeof (userData as any).points === 'number') ? (userData as any).points : 0;

        // Nuevos puntos: revertir puntos ganados - devolver puntos usados
        // Si se usaron 60 puntos y se otorgaron 40, al cancelar:
        // - Se restan los 40 otorgados
        // - Se suman los 60 que se usaron (porque no se completó la compra)
        const newPoints = Math.max(0, (currentPoints || 0) - pointsToRevertFromUser + pointsUsedInCheckout);
        tx.update(userRef, { points: newPoints });

        const histRef = userRef.collection('pointsHistory').doc();
        tx.set(histRef, {
          type: 'revert',
          reason: shouldRevertStatus ? 'order_cancelled_or_returned' : 'order_refunded',
          orderId: orderId,
          points: -pointsToRevertFromUser + pointsUsedInCheckout,
          breakdown: { revertedAward: -pointsToRevert, refundedUsed: pointsUsedInCheckout },
          createdAt: admin.firestore.FieldValue.serverTimestamp()
        });

        tx.update(change.after.ref, {
          pointsReverted: true,
          pointsRevertedAmount: pointsToRevert,
          pointsRevertedAt: admin.firestore.FieldValue.serverTimestamp()
        });
      });

      console.log(`[onOrderPointsReverted] Revertidos ${pointsToRevert} pts al usuario ${userId} por orden ${orderId}`);
      return null;
    } catch (err) {
      console.error('[onOrderPointsReverted] Error procesando reversión:', err);
      return null;
    }
  });

// (Firebase Admin ya fue inicializado arriba si era necesario)

// ============================================================================
// HELPER FUNCTIONS
// ============================================================================

/**
 * Verifica si el usuario tiene rol de admin
 */
async function isAdmin(userId: string): Promise<boolean> {
  try {
    const userDoc = await admin.firestore().collection("users").doc(userId).get();
    return userDoc.exists && userDoc.data()?.["role"] === "admin";
  } catch (error) {
    console.error("Error verificando rol admin:", error);
    return false;
  }
}

/**
 * Verifica si el usuario tiene rol de delivery
 */
async function isDelivery(userId: string): Promise<boolean> {
  try {
    const userDoc = await admin.firestore().collection("users").doc(userId).get();
    return userDoc.exists && userDoc.data()?.["role"] === "delivery";
  } catch (error) {
    console.error("Error verificando rol delivery:", error);
    return false;
  }
}

/**
 * Crear log de auditoría en Firestore
 */
async function createAuditLog(
  userId: string,
  action: string,
  details: string,
  targetId?: string,
  targetName?: string,
  metadata?: any
): Promise<void> {
  try {
    const userDoc = await admin.firestore().collection("users").doc(userId).get();
    const userData = userDoc.data();

    await admin.firestore().collection("auditLogs").add({
      timestamp: admin.firestore.FieldValue.serverTimestamp(),
      userId: userId,
      userName: userData?.name || "Usuario Desconocido",
      userEmail: userData?.email || "",
      action: action,
      targetId: targetId || null,
      targetName: targetName || null,
      details: details,
      metadata: metadata || null,
    });

    console.log(`✅ Log de auditoría creado: ${action}`);
  } catch (error) {
    console.error("❌ Error creando log de auditoría:", error);
  }
}

// ============================================================================
// PROMOCIONES Y NOTIFICACIONES
// ============================================================================

/**
 * Cloud Function que se dispara cuando se crea una nueva promoción
 * Envía notificaciones FCM a todos los usuarios con token registrado
 */
export const onPromotionCreated = functions.firestore
  .document("promotions/{promotionId}")
  .onCreate(async (snap, context) => {
    const promotion = snap.data();
    const promotionId = context.params.promotionId;

    console.log("📢 Nueva promoción creada:", promotionId);
    console.log("Nombre:", promotion.name);

    try {
      // Obtener todos los usuarios con tokens FCM
      const usersSnapshot = await admin
        .firestore()
        .collection("users")
        .where("fcmToken", "!=", null)
        .get();

      if (usersSnapshot.empty) {
        console.log("⚠️ No hay usuarios con tokens FCM");
        return null;
      }

      console.log(`👥 ${usersSnapshot.size} usuarios encontrados con tokens`);

      // Crear array de tokens únicos
      const tokens: string[] = [];
      usersSnapshot.forEach((doc) => {
        const userData = doc.data();
        if (userData.fcmToken) {
          tokens.push(userData.fcmToken);
        }
      });

      if (tokens.length === 0) {
        console.log("⚠️ No hay tokens válidos");
        return null;
      }

      // Crear mensaje de notificación
      const message = {
        notification: {
          title: "🎉 Nueva Promoción en Cyryel",
          body: `${promotion.name} - ¡Ahorra ${promotion.discountPercent}%!`,
          imageUrl: promotion.imageUrl ?? null, // Imagen de la promoción
        },
        data: {
          type: "promotion",
          promotionId: promotionId,
          promotionName: promotion.name || "",
          discountPercent: String(promotion.discountPercent || 0),
          finalPrice: String(promotion.finalPrice || 0),
        },
        android: {
          priority: "high" as const,
          notification: {
            sound: "default",
            channelId: "default",
            priority: "high" as const,
            defaultSound: true,
            defaultVibrateTimings: true,
            imageUrl: promotion.imageUrl ?? null, // Imagen grande en Android
          },
        },
      };

      // Enviar notificaciones en lotes de 500 (límite de FCM)
      const batchSize = 500;
      const batches = [];

      for (let i = 0; i < tokens.length; i += batchSize) {
        const batchTokens = tokens.slice(i, i + batchSize);
        batches.push(
          admin.messaging().sendEachForMulticast({
            tokens: batchTokens,
            ...message,
          })
        );
      }

      // Ejecutar todos los batches
      const results = await Promise.all(batches);

      // Contar éxitos y fallos
      let successCount = 0;
      let failureCount = 0;

      results.forEach((result) => {
        successCount += result.successCount;
        failureCount += result.failureCount;
      });

      console.log(`✅ Notificaciones enviadas: ${successCount}`);
      console.log(`❌ Notificaciones fallidas: ${failureCount}`);

      // Guardar estadísticas en Firestore
      await snap.ref.update({
        notificationStats: {
          sentAt: admin.firestore.FieldValue.serverTimestamp(),
          successCount: successCount,
          failureCount: failureCount,
          totalUsers: tokens.length,
        },
      });

      return {success: true, sent: successCount, failed: failureCount};
    } catch (error) {
      console.error("❌ Error enviando notificaciones:", error);
      return {success: false, error: String(error)};
    }
  });

/**
 * Cloud Function que notifica a todos los admins cuando se crea un nuevo pedido
 * - Crea un documento en `notificaciones` para cada admin
 * - Envía push FCM a los tokens registrados de los admins
 */
export const onOrderCreatedNotifyAdmins = functions.firestore
  .document('orders/{orderId}')
  .onCreate(async (snap, context) => {
    const order = snap.data();
    const orderId = context.params.orderId;

    console.log('[onOrderCreatedNotifyAdmins] Nuevo pedido creado:', orderId);

    try {
      const adminsSnapshot = await admin.firestore().collection('users').where('role', '==', 'admin').get();

      if (adminsSnapshot.empty) {
        console.log('[onOrderCreatedNotifyAdmins] No se encontraron admins');
        return null;
      }

      // Verificar qué admins ya tienen notificación para este pedido (evitar duplicados)
      const existingSnapshot = await admin.firestore()
        .collection('notificaciones')
        .where('orderId', '==', orderId)
        .where('tipo', '==', 'admin')
        .get();
      const existingUserIds = new Set(existingSnapshot.docs.map(d => d.data().userId));

      const tokens: string[] = [];
      const notiPromises: Promise<FirebaseFirestore.DocumentReference>[] = [];

      adminsSnapshot.forEach((doc) => {
        const userData: any = doc.data() || {};
        if (existingUserIds.has(doc.id)) {
          console.log(`[onOrderCreatedNotifyAdmins] Admin ${doc.id} ya tiene notificación para pedido ${orderId}, saltando`);
          if (userData.fcmToken && !tokens.includes(userData.fcmToken)) tokens.push(userData.fcmToken);
          return;
        }
        if (userData.fcmToken && !tokens.includes(userData.fcmToken)) tokens.push(userData.fcmToken);

        const deliveryMethodRaw = order?.deliveryMethod || '';
        const deliveryLabel = (deliveryMethodRaw === 'tienda') ? 'Recojo en Tienda' : (deliveryMethodRaw === 'domicilio' ? 'Envío a Domicilio' : String(deliveryMethodRaw));

        // Crear notificación en Firestore para el admin
        // Usamos tipo 'admin' para que el cliente la identifique como notificación administrativa
        notiPromises.push(
          admin.firestore().collection('notificaciones').add({
            userId: doc.id,
            titulo: 'Nuevo pedido',
            mensaje: `Nuevo pedido ${orderId} ${order?.userName ? 'de ' + order.userName + '.' : '.'} Método: ${deliveryLabel}`,
            fecha: admin.firestore.FieldValue.serverTimestamp(),
            tipo: 'admin',
            read: false,
            orderId: orderId,
            deliveryMethod: deliveryMethodRaw || null,
            link: `cyryel://admin/pedidos/${orderId}`
          })
        );
      });

      // Esperar a que se creen las notificaciones en Firestore
      await Promise.all(notiPromises);

      if (tokens.length === 0) {
        console.log('[onOrderCreatedNotifyAdmins] No hay tokens FCM entre los admins');
        return null;
      }

      // Preparar payload FCM
      const deliveryMethodRaw = order?.deliveryMethod || '';
      const deliveryLabel = (deliveryMethodRaw === 'tienda') ? 'Recojo en Tienda' : (deliveryMethodRaw === 'domicilio' ? 'Envío a Domicilio' : String(deliveryMethodRaw));

      const message: any = {
        notification: {
          title: '📦 Nuevo pedido',
          body: `Pedido ${orderId}${order?.userName ? ' de ' + order.userName : ''} — ${deliveryLabel}`
        },
        data: {
          // Señalamos explícitamente que es una notificación para admins
          type: 'admin',
          orderId: orderId,
          deliveryMethod: deliveryMethodRaw,
          link: `cyryel://admin/pedidos/${orderId}`
        },
        android: {
          priority: 'high',
          notification: {
            sound: 'default',
            channelId: 'default',
            priority: 'high'
          }
        }
      };

      // Enviar en batches de 500
      const batchSize = 500;
      for (let i = 0; i < tokens.length; i += batchSize) {
        const batchTokens = tokens.slice(i, i + batchSize);
        await admin.messaging().sendEachForMulticast({ tokens: batchTokens, ...message });
      }

      console.log(`[onOrderCreatedNotifyAdmins] Notificaciones enviadas a ${tokens.length} admins`);
      return null;
    } catch (error) {
      console.error('[onOrderCreatedNotifyAdmins] Error notificando admins:', error);
      return null;
    }
  });

/**
 * Cloud Function HTTP para enviar notificación de prueba
 * Útil para testing desde el admin panel
 */
export const sendTestNotification = functions.https.onCall(
  async (data, context) => {
    // Verificar autenticación
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Debes estar autenticado"
      );
    }

    const {userId, title, body} = data;

    try {
      // Obtener token del usuario
      const userDoc = await admin
        .firestore()
        .collection("users")
        .doc(userId)
        .get();

      if (!userDoc.exists) {
        throw new functions.https.HttpsError(
          "not-found",
          "Usuario no encontrado"
        );
      }

      const userData = userDoc.data();
      if (!userData?.fcmToken) {
        throw new functions.https.HttpsError(
          "failed-precondition",
          "Usuario no tiene token FCM"
        );
      }

      // Enviar notificación
      const message = {
        notification: {
          title: title || "Notificación de Prueba",
          body: body || "Esta es una notificación de prueba"
        },
        token: userData.fcmToken,
        android: {
          priority: "high" as const,
          notification: {
            sound: "default",
            channelId: "default",
            priority: "high" as const
          },
        },
      };

      await admin.messaging().send(message);

      return {success: true, message: "Notificación enviada"};
    } catch (error) {
      console.error("Error enviando notificación de prueba:", error);
      throw new functions.https.HttpsError(
        "internal",
        "Error enviando notificación"
      );
    }
  }
);

/**
 * Cloud Function para enviar notificación a un usuario específico desde el admin panel
 * Guarda en Firestore y envía FCM push si el usuario tiene token
 */
export const sendIndividualNotification = functions.https.onCall(
  async (data, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Debes estar autenticado"
      );
    }

    const { userId, title, message, tipo, link, orderId, promotionId } = data;

    if (!userId || !title || !message) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "userId, title y message son requeridos"
      );
    }

    try {
      const userIsAdmin = await isAdmin(context.auth.uid);
      if (!userIsAdmin) {
        throw new functions.https.HttpsError(
          "permission-denied",
          "Solo admins pueden enviar notificaciones"
        );
      }

      const userDoc = await admin
        .firestore()
        .collection("users")
        .doc(userId)
        .get();

      if (!userDoc.exists) {
        throw new functions.https.HttpsError(
          "not-found",
          "Usuario no encontrado"
        );
      }

      const userData = userDoc.data()!;

      // Guardar en Firestore
      const notifData: any = {
        userId,
        titulo: title,
        mensaje: message,
        tipo: tipo || "info",
        read: false,
        deliveryRelated: false,
        orderId: orderId || null,
        promoId: promotionId || null,
        link: link || null,
        fecha: admin.firestore.FieldValue.serverTimestamp(),
      };

      await admin.firestore().collection("notificaciones").add(notifData);

      let pushSent = false;

      // Enviar FCM push si tiene token
      if (userData.fcmToken) {
        const fcmData: any = {
          type: tipo || "info",
        };
        if (orderId) fcmData.orderId = orderId;
        if (promotionId) fcmData.promotionId = promotionId;
        if (link) fcmData.link = link;

        const fcmMessage: any = {
          notification: {
            title,
            body: message,
          },
          data: fcmData,
          token: userData.fcmToken,
          android: {
            priority: "high" as const,
            notification: {
              sound: "default",
              channelId: "default",
              priority: "high" as const,
            },
          },
        };

        await admin.messaging().send(fcmMessage);
        pushSent = true;
      }

      // Log de auditoría
      await createAuditLog(
        context.auth.uid,
        "individual_notification",
        `Envió notificación individual "${title}" a ${userData.name || userId}`,
        userId,
        userData.name || "Usuario",
        { title, message, tipo, pushSent }
      );

      return {
        success: true,
        pushSent,
        message: "Notificación enviada correctamente",
      };
    } catch (error) {
      console.error("Error enviando notificación individual:", error);
      throw new functions.https.HttpsError(
        "internal",
        "Error enviando notificación"
      );
    }
  }
);

/**
 * Cloud Function que limpia tokens FCM inválidos
 * Se ejecuta diariamente a las 3:00 AM
 */
export const cleanInvalidTokens = functions.pubsub
  .schedule("0 3 * * *")
  .timeZone("America/Lima")
  .onRun(async (context) => {
    console.log("[cleanInvalidTokens] 🧹 Iniciando limpieza de tokens inválidos");
    console.log("[cleanInvalidTokens] 🕐 Ejecutando a las 3:00 AM hora de Lima");
    console.log("🧹 Iniciando limpieza de tokens inválidos");

    try {
      const usersSnapshot = await admin
        .firestore()
        .collection("users")
        .where("fcmToken", "!=", null)
        .get();

      if (usersSnapshot.empty) {
        console.log("No hay tokens para verificar");
        return null;
      }

      const invalidTokens: string[] = [];

      // Verificar cada token
      for (const doc of usersSnapshot.docs) {
        const userData = doc.data();
        const token = userData.fcmToken;

        if (!token) continue;

        try {
          // Intentar enviar mensaje vacío para validar token
          await admin.messaging().send(
            {
              token: token,
              data: {test: "validation"},
            },
            true // dryRun: no envía realmente
          );
        } catch (error: any) {
          if (
            error.code === "messaging/invalid-registration-token" ||
            error.code === "messaging/registration-token-not-registered"
          ) {
            invalidTokens.push(doc.id);
            console.log(`❌ Token inválido para usuario: ${doc.id}`);

            // Eliminar token inválido
            await doc.ref.update({
              fcmToken: admin.firestore.FieldValue.delete(),
              fcmTokenUpdatedAt: admin.firestore.FieldValue.delete(),
            });
          }
        }
      }

      console.log(`✅ Limpieza completada. Tokens eliminados: ${invalidTokens.length}`);
      return {cleaned: invalidTokens.length};
    } catch (error) {
      console.error("Error en limpieza de tokens:", error);
      return {error: String(error)};
    }
  });

/**
 * Cloud Function que desactiva promociones vencidas
 * Se ejecuta diariamente a las 00:00
 */
export const deactivateExpiredPromotions = functions.pubsub
  .schedule("0 0 * * *")
  .timeZone("America/Lima")
  .onRun(async (context) => {
    console.log("🔍 Verificando promociones vencidas");

    try {
      const now = admin.firestore.Timestamp.now();
      
      // Buscar promociones activas con fecha de vencimiento
      const promotionsSnapshot = await admin
        .firestore()
        .collection("promotions")
        .where("active", "==", true)
        .where("expiresAt", "!=", null)
        .where("expiresAt", "<=", now)
        .get();

      if (promotionsSnapshot.empty) {
        console.log("✅ No hay promociones vencidas");
        return {deactivated: 0};
      }

      console.log(`⚠️ ${promotionsSnapshot.size} promociones vencidas encontradas`);

      const batch = admin.firestore().batch();
      const deactivatedIds: string[] = [];

      promotionsSnapshot.forEach((doc) => {
        const promoData = doc.data();
        batch.update(doc.ref, {
          active: false,
          deactivatedAt: admin.firestore.FieldValue.serverTimestamp(),
          deactivationReason: "expired"
        });
        deactivatedIds.push(doc.id);
        console.log(`🔴 Desactivando promoción: ${promoData.name} (${doc.id})`);
      });

      await batch.commit();

      console.log(`✅ ${deactivatedIds.length} promociones desactivadas por vencimiento`);
      return {deactivated: deactivatedIds.length, ids: deactivatedIds};
    } catch (error) {
      console.error("❌ Error desactivando promociones:", error);
      return {error: String(error)};
    }
  });

/**
 * Otorga puntos cuando un pedido pasa a paymentStatus = 'completado'.
 * Reglas:
 * - Puntos base según total (calculateBasePoints)
 * - Por cada producto normal comprado -> sumar `products.points * quantity`
 * - Por cada promoción comprada -> 5 puntos por unidad (o el valor configurado en la promoción)
 * - La función es idempotente: marca `pointsAwarded` en la orden para no volver a otorgar
 */
export const onOrderPaymentCompleted = functions.firestore
  .document('orders/{orderId}')
  .onWrite(async (change, context) => {
    const before = change.before.exists ? change.before.data() : null;
    const after = change.after.exists ? change.after.data() : null;
    const orderId = context.params.orderId;

    try {
      // Solo reaccionar cuando paymentStatus pase a 'completado'
      const prevPayment = before?.paymentStatus || null;
      const newPayment = after?.paymentStatus || null;
      if (prevPayment === 'completado' || newPayment !== 'completado') {
        return null;
      }

      // Evitar volver a otorgar si ya se otorgaron puntos
      if (after?.pointsAwarded) {
        console.log(`[onOrderPaymentCompleted] Orden ${orderId} ya tiene puntos otorgados`);
        return null;
      }

      const userId = after?.userId;
      if (!userId) {
        console.warn(`[onOrderPaymentCompleted] Orden ${orderId} no tiene userId`);
        return null;
      }

      const total = Number(after?.total || after?.subtotal || 0);

      // calcular puntos base por monto
      const basePoints = calculateBasePoints(total);

      // calcular promociones: buscar items con isPromotion o promotionData o promotionId
      const items = Array.isArray(after?.items) ? after.items : [];
      let promoPoints = 0;
      let promoUnits = 0; // para logging
      let productPoints = 0;
      let productUnits = 0;
      try {
        // Agrupar por promotionId las cantidades
        const promoQtyMap: { [id: string]: number } = {};
        const productQtyMap: { [id: string]: number } = {};
        for (const it of items) {
          try {
            if (!it) continue;
            const isPromo = !!(it.isPromotion || it.promotionData || it.promotionId);
            const qty = Number(it.quantity || 0) || 0;

            if (isPromo) {
              promoUnits += qty;
              const pid = it.promotionId || (it.promotionData && (it.promotionData.id || it.promotionData.promotionId)) || null;
              if (pid) {
                promoQtyMap[pid] = (promoQtyMap[pid] || 0) + qty;
              } else {
                // Si no hay id, usar fallback unitario
                promoPoints += qty * DEFAULT_PROMO_UNIT_POINTS;
              }
            } else {
              // No otorgar puntos de producto para ítems canjeados con puntos
              if (it.redeemedByPoints) continue;
              const productId = it.productId;
              if (!productId) continue;
              productQtyMap[productId] = (productQtyMap[productId] || 0) + qty;
              productUnits += qty;
            }
          } catch (e) { /* ignore item errors */ }
        }

        const db = admin.firestore();
        const promoIds = Object.keys(promoQtyMap);
        if (promoIds.length > 0) {
          const promoDocs = await Promise.all(promoIds.map(id => db.collection('promotions').doc(id).get()));
          promoDocs.forEach((pd) => {
            if (!pd.exists) return;
            const pdata: any = pd.data() || {};
            const pointsPerUnit = (typeof pdata.points === 'number') ? pdata.points : DEFAULT_PROMO_UNIT_POINTS;
            const q = promoQtyMap[pd.id] || 0;
            promoPoints += pointsPerUnit * q;
          });
        }

        const productIds = Object.keys(productQtyMap);
        if (productIds.length > 0) {
          const productDocs = await Promise.all(productIds.map(id => db.collection('products').doc(id).get()));
          productDocs.forEach((pd) => {
            if (!pd.exists) return;
            const pdata: any = pd.data() || {};
            const pointsPerUnit = (typeof pdata.points === 'number') ? pdata.points : 0;
            const q = productQtyMap[pd.id] || 0;
            productPoints += pointsPerUnit * q;
          });
        }
      } catch (e) {
        console.warn('[onOrderPaymentCompleted] Error calculando puntos de promociones/productos', e);
      }

      const pointsToAward = (basePoints || 0) + (promoPoints || 0) + (productPoints || 0);
      
      // Procesar puntos usados (descuento de puntos del usuario)
      const pointsUsed = Number(after?.pointsUsed || 0);
      
      // Marcar orden si no hay puntos que otorgar y tampoco se usaron puntos
      if (!pointsToAward && !pointsUsed) {
        console.log(`[onOrderPaymentCompleted] Orden ${orderId} sin movimiento de puntos`);
        await change.after.ref.update({ pointsAwarded: false, pointsAwardedAt: admin.firestore.FieldValue.serverTimestamp() });
        return null;
      }

      // Ejecutar transacción para actualizar puntos
      const db = admin.firestore();
      await db.runTransaction(async (tx) => {
        const userRef = db.collection('users').doc(userId);

        // Calcular delta neto de puntos: restar usados + sumar ganados
        const netPointsDelta = pointsToAward - pointsUsed;
        if (netPointsDelta !== 0) {
          tx.set(userRef, { points: admin.firestore.FieldValue.increment(netPointsDelta) }, { merge: true });
        }

        // Registrar puntos ganados
        if (pointsToAward > 0) {
          const histRef = userRef.collection('pointsHistory').doc();
          tx.set(histRef, {
            type: 'earn',
            reason: 'order_completed',
            orderId: orderId,
            points: pointsToAward,
            breakdown: { basePoints, promoPoints, promoUnits, productPoints, productUnits, totalAmount: total },
            createdAt: admin.firestore.FieldValue.serverTimestamp()
          });
        }

        // Registrar puntos usados como descuento
        if (pointsUsed > 0) {
          const histRef = userRef.collection('pointsHistory').doc();
          tx.set(histRef, {
            type: 'redeem',
            reason: 'checkout_discount',
            orderId: orderId,
            points: -pointsUsed,
            description: `Descuento de ${pointsUsed} puntos en checkout`,
            createdAt: admin.firestore.FieldValue.serverTimestamp()
          });
        }

        // Marcar la orden como procesada
        tx.update(change.after.ref, {
          pointsAwarded: true,
          pointsAwardAmount: pointsToAward,
          pointsAwardedAt: admin.firestore.FieldValue.serverTimestamp()
        });
      });

      console.log(`[onOrderPaymentCompleted] Orden ${orderId}: usuario ${userId}, ganados ${pointsToAward} pts, usados ${pointsUsed} pts, neto ${pointsToAward - pointsUsed}`);
      return null;
    } catch (err) {
      console.error('[onOrderPaymentCompleted] Error procesando orden:', err);
      return null;
    }
  });

// ============================================================================
// GESTIÓN DE ROLES
// ============================================================================

/**
 * Cloud Function para cambiar el rol de un usuario
 * Solo admins pueden ejecutarla
 */
export const setUserRole = functions.https.onCall(async (data, context) => {
  // Verificar autenticación
  if (!context.auth) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "Debes estar autenticado"
    );
  }

  const {targetUserId, newRole} = data;

  // Validar parámetros
  if (!targetUserId || !newRole) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "targetUserId y newRole son requeridos"
    );
  }

  // Validar rol válido
  const validRoles = ["user", "admin", "delivery"];
  if (!validRoles.includes(newRole)) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      `Rol inválido. Debe ser uno de: ${validRoles.join(", ")}`
    );
  }

  try {
    // Verificar que el usuario que ejecuta sea admin
    const isUserAdmin = await isAdmin(context.auth.uid);
    if (!isUserAdmin) {
      throw new functions.https.HttpsError(
        "permission-denied",
        "Solo admins pueden cambiar roles"
      );
    }

    // Verificar que el usuario objetivo existe
    const targetUserDoc = await admin
      .firestore()
      .collection("users")
      .doc(targetUserId)
      .get();

    if (!targetUserDoc.exists) {
      throw new functions.https.HttpsError("not-found", "Usuario no encontrado");
    }

    // Actualizar rol
    const oldRole = targetUserDoc.data()?.role || "user";
    const targetUserName = targetUserDoc.data()?.name || "Usuario";

    await admin
      .firestore()
      .collection("users")
      .doc(targetUserId)
      .update({
        role: newRole,
        roleUpdatedAt: admin.firestore.FieldValue.serverTimestamp(),
        roleUpdatedBy: context.auth.uid,
      });

    // Crear log de auditoría
    await createAuditLog(
      context.auth.uid,
      "role_change",
      `Cambió rol de ${oldRole} a ${newRole}`,
      targetUserId,
      targetUserName,
      { oldRole, newRole }
    );

    console.log(
      `✅ Rol actualizado: Usuario ${targetUserId} ahora es ${newRole}`
    );
    console.log(`👤 Actualizado por admin: ${context.auth.uid}`);

    return {
      success: true,
      message: `Rol actualizado a ${newRole}`,
      userId: targetUserId,
      newRole: newRole,
    };
  } catch (error: any) {
    console.error("Error cambiando rol:", error);
    if (error instanceof functions.https.HttpsError) {
      throw error;
    }
    throw new functions.https.HttpsError("internal", "Error cambiando rol");
  }
});

/**
 * Callable: addFunds
 * data: { amount: number }
 * Auth required. Adds funds and awards points based on calculateBasePoints().
 */
export const addFunds = functions.https.onCall(async (data, context) => {
  if (!context.auth) throw new functions.https.HttpsError('unauthenticated', 'Auth required');
  const uid = context.auth.uid;
  const amount = Number(data?.amount || 0);
  if (!amount || amount <= 0) throw new functions.https.HttpsError('invalid-argument', 'amount must be > 0');

  const db = admin.firestore();
  const userRef = db.collection('users').doc(uid);
  try {
    const pointsDelta = calculateBasePoints(amount);
    await db.runTransaction(async (tx) => {
      const snap = await tx.get(userRef);
      const userData: any = snap.exists ? snap.data() : {};
      const currentPoints = (userData && typeof userData.points === 'number') ? userData.points : 0;
      const newPoints = currentPoints + pointsDelta;
      tx.set(userRef, { points: newPoints, updatedAt: admin.firestore.FieldValue.serverTimestamp() }, { merge: true });

      const histRef = userRef.collection('pointsHistory').doc();
      tx.set(histRef, {
        type: 'funds_add',
        amount,
        pointsDelta,
        description: 'Recarga via app',
        createdAt: admin.firestore.FieldValue.serverTimestamp()
      });
    });

    return { success: true, pointsDelta };
  } catch (err) {
    console.error('[addFunds] Error:', err);
    throw new functions.https.HttpsError('internal', 'Error adding funds');
  }
});

/**
 * Callable: withdrawFunds
 * data: { amount: number }
 * Auth required. Performs withdrawal and records history (doesn't change points by default).
 */
export const withdrawFunds = functions.https.onCall(async (data, context) => {
  if (!context.auth) throw new functions.https.HttpsError('unauthenticated', 'Auth required');
  const uid = context.auth.uid;
  const amount = Number(data?.amount || 0);
  if (!amount || amount <= 0) throw new functions.https.HttpsError('invalid-argument', 'amount must be > 0');

  const db = admin.firestore();
  const userRef = db.collection('users').doc(uid);
  try {
    await db.runTransaction(async (tx) => {
      // No es necesario leer puntos aquí
      tx.set(userRef, { updatedAt: admin.firestore.FieldValue.serverTimestamp() }, { merge: true });

      const histRef = userRef.collection('pointsHistory').doc();
      tx.set(histRef, {
        type: 'funds_withdraw',
        amount: -amount,
        pointsDelta: 0,
        description: 'Retiro via app',
        createdAt: admin.firestore.FieldValue.serverTimestamp()
      });
    });

    return { success: true };
  } catch (err) {
    console.error('[withdrawFunds] Error:', err);
    throw new functions.https.HttpsError('internal', 'Error withdrawing funds');
  }
});
/**
 * Callable: redeemReward
 * data: { rewardId: string, rewardName?: string, costPoints: number }
 * Auth required. Redeems a reward decreasing points and adding pointsHistory entry.
 */
export const redeemReward = functions.https.onCall(async (data, context) => {
  if (!context.auth) throw new functions.https.HttpsError('unauthenticated', 'Auth required');
  const uid = context.auth.uid;
  const rewardId = String(data?.rewardId || '');
  const rewardName = String(data?.rewardName || '');
  const costPoints = Number(data?.costPoints || 0);
  if (!rewardId || !costPoints || costPoints <= 0) throw new functions.https.HttpsError('invalid-argument', 'Invalid reward data');

  const db = admin.firestore();
  const userRef = db.collection('users').doc(uid);
  try {
    await db.runTransaction(async (tx) => {
      const snap = await tx.get(userRef);
      const userData: any = snap.exists ? snap.data() : {};
      const currentPoints = (userData && typeof userData.points === 'number') ? userData.points : 0;
      if (currentPoints < costPoints) throw new functions.https.HttpsError('failed-precondition', 'Insufficient points');

      const newPoints = currentPoints - costPoints;
      tx.set(userRef, { points: newPoints, updatedAt: admin.firestore.FieldValue.serverTimestamp() }, { merge: true });

      const histRef = userRef.collection('pointsHistory').doc();
      tx.set(histRef, {
        type: 'redeem',
        rewardId,
        rewardName,
        points: -costPoints,
        createdAt: admin.firestore.FieldValue.serverTimestamp()
      });
    });

    return { success: true };
  } catch (err) {
    console.error('[redeemReward] Error:', err);
    if (err instanceof functions.https.HttpsError) throw err;
    throw new functions.https.HttpsError('internal', 'Error redeeming reward');
  }
});

// ============================================================================
// GESTIÓN DE PEDIDOS
// ============================================================================

/**
 * Cloud Function para que un repartidor suelte un pedido que aceptó
 * pero ya no quiere repartir. Revierte el delivery a "disponible"
 * y la orden a "pendiente" para que otro repartidor pueda tomarlo.
 */
export const releaseDelivery = functions.https.onCall(
  async (data, context) => {
    const uid = context.auth?.uid || data.uid;
    if (!uid) {
      throw new functions.https.HttpsError("unauthenticated", "Debes estar autenticado");
    }

    const { deliveryId, orderId } = data;
    if (!deliveryId || !orderId) {
      throw new functions.https.HttpsError("invalid-argument", "deliveryId y orderId son requeridos");
    }

    try {
      const userIsDelivery = await isDelivery(uid);
      if (!userIsDelivery) {
        throw new functions.https.HttpsError("permission-denied", "Solo repartidores pueden soltar pedidos");
      }

      await admin.firestore().runTransaction(async (tx) => {
        const deliveryRef = admin.firestore().collection("deliveries").doc(deliveryId);
        const deliveryDoc = await tx.get(deliveryRef);

        if (!deliveryDoc.exists) {
          throw new functions.https.HttpsError("not-found", "Delivery no encontrado");
        }

        const deliveryData = deliveryDoc.data()!;
        if (deliveryData.status !== "aceptado") {
          throw new functions.https.HttpsError("failed-precondition", "Solo puedes soltar pedidos en estado aceptado");
        }

        if (deliveryData.deliveryPersonId !== uid) {
          throw new functions.https.HttpsError("permission-denied", "No puedes soltar un pedido que no aceptaste");
        }

        const now = admin.firestore.FieldValue.serverTimestamp();

        tx.update(deliveryRef, {
          status: "disponible",
          deliveryPersonId: admin.firestore.FieldValue.delete(),
          fcmToken: admin.firestore.FieldValue.delete(),
          acceptedAt: admin.firestore.FieldValue.delete(),
          updatedAt: now
        });

        const orderRef = admin.firestore().collection("orders").doc(orderId);
        tx.update(orderRef, {
          status: "pendiente",
          assignedDeliveryId: admin.firestore.FieldValue.delete(),
          deliveryPersonName: admin.firestore.FieldValue.delete(),
          deliveryAcceptedAt: admin.firestore.FieldValue.delete(),
          deliveryPersonLocation: admin.firestore.FieldValue.delete(),
          updatedAt: now
        });
      });

      console.log(`✅ Delivery ${deliveryId} liberado por repartidor ${uid}`);

      await createAuditLog(
        uid,
        "delivery_release",
        `Repartidor liberó pedido ${orderId}`,
        orderId,
        `Pedido #${orderId}`,
        { deliveryId }
      );

      return { success: true, message: "Pedido liberado correctamente" };
    } catch (error: any) {
      console.error("Error liberando delivery:", error);
      if (error instanceof functions.https.HttpsError) throw error;
      throw new functions.https.HttpsError("internal", "Error al liberar el pedido");
    }
  }
);

/**
 * Cloud Function para actualizar el estado de un pedido
 * Valida permisos según el rol del usuario
 */
export const updateOrderStatus = functions.https.onCall(
  async (data, context) => {
    // Verificar autenticación
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Debes estar autenticado"
      );
    }

    const {orderId, newStatus} = data;

    // Validar parámetros
    if (!orderId || !newStatus) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "orderId y newStatus son requeridos"
      );
    }

    // Estados válidos (deben coincidir con OrderStatus del frontend)
    const validStatuses = [
      "pendiente",
      "confirmado",
      "listo_para_recoger",
      "en_camino",
      "entregado",
      "cancelado",
      "devuelto",
    ];

    if (!validStatuses.includes(newStatus)) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        `Estado inválido. Debe ser uno de: ${validStatuses.join(", ")}`
      );
    }

    try {
      const userId = context.auth.uid;
      const userIsAdmin = await isAdmin(userId);
      const userIsDelivery = await isDelivery(userId);

      // Obtener el pedido
      const orderDoc = await admin
        .firestore()
        .collection("orders")
        .doc(orderId)
        .get();

      if (!orderDoc.exists) {
        throw new functions.https.HttpsError(
          "not-found",
          "Pedido no encontrado"
        );
      }

      const orderData = orderDoc.data();
      const currentStatus = orderData?.status;

      // Validar permisos según rol
      if (!userIsAdmin) {
        // Si es delivery
        if (userIsDelivery) {
          // Delivery solo puede cambiar a en_camino y entregado
          if (!["en_camino", "entregado"].includes(newStatus)) {
            throw new functions.https.HttpsError(
              "permission-denied",
              "Delivery solo puede cambiar a 'en camino' o 'entregado'"
            );
          }

          // Delivery solo puede actualizar si el pedido está confirmado o en camino
          if (!["confirmado", "en_camino"].includes(currentStatus)) {
            throw new functions.https.HttpsError(
              "permission-denied",
              "Solo puedes actualizar pedidos confirmados o en camino"
            );
          }
        } else {
          // Usuario normal solo puede cancelar
          if (newStatus !== "cancelado") {
            throw new functions.https.HttpsError(
              "permission-denied",
              "Solo puedes cancelar tu pedido"
            );
          }

          // Solo puede cancelar su propio pedido
          if (orderData?.userId !== userId) {
            throw new functions.https.HttpsError(
              "permission-denied",
              "Solo puedes cancelar tus propios pedidos"
            );
          }

          // Solo puede cancelar si está pendiente
          if (currentStatus !== "pendiente") {
            throw new functions.https.HttpsError(
              "permission-denied",
              "Solo puedes cancelar pedidos pendientes"
            );
          }
        }
      }

      // Actualizar estado
      await admin
        .firestore()
        .collection("orders")
        .doc(orderId)
        .update({
          status: newStatus,
          statusUpdatedAt: admin.firestore.FieldValue.serverTimestamp(),
          statusUpdatedBy: userId,
        });

      if (newStatus === "cancelado" || newStatus === "devuelto") {
        try {
          const deliveryRef = admin.firestore().collection("deliveries").doc(orderId);
          const deliverySnap = await deliveryRef.get();
          if (deliverySnap.exists) {
            await deliveryRef.update({
              status: "cancelado",
              cancelledAt: admin.firestore.FieldValue.serverTimestamp(),
              cancelReason: `Orden ${newStatus === "cancelado" ? "cancelada" : "devuelta"}`,
              updatedAt: admin.firestore.FieldValue.serverTimestamp()
            });
            console.log(`✅ Delivery cancelado para orden ${orderId}`);
          }
        } catch (delErr) {
          console.error(`⚠️ Error cancelando delivery para orden ${orderId}:`, delErr);
        }
      }

      console.log(`✅ Estado de pedido actualizado: ${orderId}`);
      console.log(`📦 Estado anterior: ${currentStatus} → nuevo: ${newStatus}`);
      console.log(`👤 Actualizado por: ${userId}`);

      // Crear log de auditoría
      const orderNumber = orderData?.orderNumber || orderId;
      await createAuditLog(
        userId,
        "order_status",
        `Cambió estado de ${currentStatus} a ${newStatus}`,
        orderId,
        `Pedido #${orderNumber}`,
        { oldStatus: currentStatus, newStatus, orderNumber }
      );

// Notificar al usuario del pedido si no es él quien actualiza
      if (orderData?.userId && orderData.userId !== userId) {
        try {
          const statusMessages: {[key: string]: string} = {
            confirmado: "Tu pedido #{orderId} ha sido confirmado",
            listo_para_recoger: "Tu pedido #{orderId} está listo para recoger",
            en_camino: "Tu pedido #{orderId} está en camino",
            entregado: "Tu pedido #{orderId} ha sido entregado",
            cancelado: "Tu pedido #{orderId} ha sido cancelado",
            devuelto: "Tu pedido #{orderId} ha sido devuelto",
          };

          const statusTitles: {[key: string]: string} = {
            confirmado: "Pedido confirmado",
            listo_para_recoger: "Pedido listo para recoger",
            en_camino: "Pedido en camino",
            entregado: "Pedido entregado",
            cancelado: "Pedido cancelado",
            devuelto: "Pedido devuelto",
          };

          // Construir el cuerpo del mensaje reemplazando el marcador {orderId}
          const messageBody = (statusMessages[newStatus] || "Estado actualizado").replace('{orderId}', orderId);

          // Guardar notificación en Firestore
          await admin
                .firestore()
                .collection("notificaciones")
                .add({
                  userId: orderData.userId,
                  titulo: statusTitles[newStatus] || "Estado de Pedido",
                  mensaje: messageBody,
                  tipo: "pedido",
                  orderId: orderId,
                  read: false,
                  deliveryRelated: true,
                  fecha: admin.firestore.FieldValue.serverTimestamp(),
                  link: `cyryel://shop/pedidos/${orderId}`
                });

          // Obtener el token FCM del documento del usuario
          let userFcmToken: string | null = null;
          try {
            const userDoc = await admin
              .firestore()
              .collection("users")
              .doc(orderData.userId)
              .get();
            
            if (userDoc.exists) {
              userFcmToken = userDoc.data()?.["fcmToken"] || null;
            }
          } catch (userError) {
            console.error("Error obteniendo token FCM del usuario:", userError);
          }

          // Enviar push notification si el usuario tiene token FCM
          if (userFcmToken) {
            await admin.messaging().send({
              notification: {
                title: statusTitles[newStatus] || "Estado de Pedido",
                body: messageBody,
              },
              data: {
                type: "order_status",
                orderId: orderId,
                newStatus: newStatus,
                link: `cyryel://shop/pedidos/${orderId}`,
                click_action: "FLUTTER_NOTIFICATION_CLICK"
              },
              token: userFcmToken,
              android: {
                priority: "high" as const,
                notification: {
                  sound: "default",
                  channelId: "default",
                  priority: "high" as const,
                },
              },
            });
            console.log("📲 Push notification enviada al usuario");
          } else {
            console.log("⚠️ Usuario no tiene token FCM, solo notificación en Firestore");
          }

          console.log("📬 Notificación creada para el usuario");
        } catch (notifError) {
          console.error("Error creando notificación:", notifError);
          // No lanzar error, la actualización del pedido ya fue exitosa
        }
      }

      return {
        success: true,
        message: "Estado actualizado",
        orderId: orderId,
        oldStatus: currentStatus,
        newStatus: newStatus,
      };
    } catch (error: any) {
      console.error("Error actualizando estado de pedido:", error);
      if (error instanceof functions.https.HttpsError) {
        throw error;
      }
      throw new functions.https.HttpsError(
        "internal",
        "Error actualizando estado"
      );
    }
  }
);

/**
 * Cloud Function para confirmar pago en efectivo
 * Solo admins pueden ejecutarla
 */
export const confirmPayment = functions.https.onCall(async (data, context) => {
  // Verificar autenticación
  if (!context.auth) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "Debes estar autenticado"
    );
  }

  const {orderId, paymentMethod, notes} = data;

  // Validar parámetros
  if (!orderId) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "orderId es requerido"
    );
  }

  try {
    // Verificar que el usuario sea admin
    const userIsAdmin = await isAdmin(context.auth.uid);
    if (!userIsAdmin) {
      throw new functions.https.HttpsError(
        "permission-denied",
        "Solo admins pueden confirmar pagos"
      );
    }

    // Obtener el pedido
    const orderDoc = await admin
      .firestore()
      .collection("orders")
      .doc(orderId)
      .get();

    if (!orderDoc.exists) {
      throw new functions.https.HttpsError("not-found", "Pedido no encontrado");
    }

    const orderData = orderDoc.data();

    // Verificar que el pago no esté ya confirmado
    if (orderData?.paymentStatus === "completado") {
      throw new functions.https.HttpsError(
        "failed-precondition",
        "El pago ya está confirmado"
      );
    }

    // Actualizar estado de pago y confirmar orden
    await admin
      .firestore()
      .collection("orders")
      .doc(orderId)
      .update({
        status: "confirmado",
        paymentStatus: "completado",
        paymentMethod: paymentMethod || "Codigo",
        paymentConfirmedAt: admin.firestore.FieldValue.serverTimestamp(),
        paymentConfirmedBy: context.auth.uid,
        paymentNotes: notes || "",
      });

    console.log(`✅ Pago confirmado para pedido: ${orderId}`);
    console.log(`💰 Método de pago: ${paymentMethod || "Codigo"}`);
    console.log(`👤 Confirmado por admin: ${context.auth.uid}`);

    // Crear log de auditoría
    const orderNumber = orderData?.orderNumber || orderId;
    await createAuditLog(
      context.auth.uid,
      "payment_confirm",
      `Confirmó pago en ${paymentMethod || "efectivo"}`,
      orderId,
      `Pedido #${orderNumber}`,
      { paymentMethod: paymentMethod || "Codigo", notes, orderNumber }
    );

    // Notificar al usuario
    if (orderData?.userId) {
      try {
        // Guardar notificación en Firestore
        await admin
          .firestore()
          .collection("notificaciones")
          .add({
            userId: orderData.userId,
            titulo: "Pago Confirmado",
            mensaje: `Tu pago ha sido confirmado. Tu pedido será procesado. (ID: ${orderId})`,
            tipo: "pedido",
            orderId: orderId,
            read: false,
            deliveryRelated: false,
            fecha: admin.firestore.FieldValue.serverTimestamp(),
            link: `cyryel://shop/pedidos/${orderId}`
          });

        // Enviar push notification si el usuario tiene token FCM
        if (orderData.fcmToken) {
          await admin.messaging().send({
            notification: {
              title: "Pago Confirmado",
              body: `Tu pago ha sido confirmado. Tu pedido será procesado. (ID: ${orderId})`,
            },
            data: {
              type: "payment_status",
              orderId: orderId,
              link: `cyryel://shop/pedidos/${orderId}`,
              click_action: "FLUTTER_NOTIFICATION_CLICK"
            },
            token: orderData.fcmToken,
            android: {
              priority: "high" as const,
                notification: {
                sound: "default",
                channelId: "default",
                priority: "high" as const,
              },
            },
          });
          console.log("📲 Push notification de pago enviada al usuario con ID de pedido");
        } else {
          console.log("⚠️ Usuario no tiene token FCM, solo notificación en Firestore");
        }

        console.log("📬 Notificación de pago enviada al usuario");
      } catch (notifError) {
        console.error("Error creando notificación:", notifError);
      }
    }
    
    // Respuesta exitosa de confirmPayment
    return {
      success: true,
      message: "Pago confirmado exitosamente",
      orderId: orderId,
      paymentMethod: paymentMethod || "cash",
    };
  } catch (error: any) {
    console.error("Error confirmando pago:", error);
    if (error instanceof functions.https.HttpsError) {
      throw error;
    }
    throw new functions.https.HttpsError("internal", "Error confirmando pago");
  }
});

// ============================================================================
// VALIDACIÓN DE ZONA DE COBERTURA
// ============================================================================

/**
 * Cloud Function para validar que una dirección esté dentro de la zona de cobertura
 * Actualmente solo se permite delivery en Chancay
 */
export const validateDeliveryZone = functions.https.onCall(async (data, context) => {
  const { district, city, province } = data;

  try {
    console.log('[validateDeliveryZone] Validando dirección:', { district, city, province });

    // Normalizar los textos para comparación (quitar tildes, minúsculas, espacios extra)
    const normalizeText = (text: string): string => {
      if (!text) return '';
      return text
        .toLowerCase()
        .trim()
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '') // Eliminar tildes
        .replace(/\s+/g, ' '); // Normalizar espacios
    };

    const districtNorm = normalizeText(district || '');
    const cityNorm = normalizeText(city || '');
    const provinceNorm = normalizeText(province || '');

    // Verificar que sea Chancay
    const isChancay = districtNorm === 'chancay' || 
                      cityNorm === 'chancay' ||
                      districtNorm.includes('chancay') ||
                      cityNorm.includes('chancay');

    // Verificar provincia si está disponible
    const isHuaral = !province || provinceNorm === 'huaral' || provinceNorm.includes('huaral');

    if (isChancay && isHuaral) {
      console.log('[validateDeliveryZone] ✅ Dirección válida - dentro de Chancay');
      return {
        valid: true,
        message: 'Dirección dentro de la zona de cobertura',
        zone: 'Chancay, Huaral, Lima'
      };
    } else {
      console.log('[validateDeliveryZone] ❌ Dirección fuera de zona de cobertura');
      return {
        valid: false,
        message: 'Actualmente solo realizamos entregas a domicilio en la ciudad de Chancay. Puedes optar por recojo en tienda.',
        zone: 'Fuera de cobertura',
        suggestedAction: 'pickup' // Sugerir recojo en tienda
      };
    }
  } catch (error: any) {
    console.error('[validateDeliveryZone] Error validando zona:', error);
    // En caso de error, permitir continuar pero registrar el error
    return {
      valid: true,
      message: 'No se pudo validar la zona. El pedido será revisado.',
      zone: 'Verificación pendiente',
      warning: true
    };
  }
});

// ============================================================================
// NOTIFICACIÓN PUSH AL CREAR PEDIDO
// ============================================================================

/**
 * Cloud Function que se dispara cuando se crea un nuevo pedido
 * Envía notificación push al usuario con el ID del pedido
 */
export const onOrderCreatedNotification = functions.firestore
  .document("orders/{orderId}")
  .onCreate(async (snap, context) => {
    const orderId = context.params.orderId;
    const orderData = snap.data();

    console.log("🆕 Nuevo pedido creado:", orderId);
    try {
      // Guardar notificación en Firestore
      await admin
        .firestore()
        .collection("notificaciones")
        .add({
          userId: orderData.userId,
          titulo: "Pedido creado",
          mensaje: `Tu pedido ha sido creado exitosamente. (ID: ${orderId})`,
          tipo: "pedido",
          orderId: orderId,
          read: false,
          deliveryRelated: false,
          fecha: admin.firestore.FieldValue.serverTimestamp(),
          link: `cyryel://shop/pedidos/${orderId}`
        });

      // Obtener el token FCM del documento del usuario (no del pedido)
      let fcmToken: string | null = null;
      if (orderData.userId) {
        try {
          const userDoc = await admin
            .firestore()
            .collection("users")
            .doc(orderData.userId)
            .get();
          
          if (userDoc.exists) {
            fcmToken = userDoc.data()?.["fcmToken"] || null;
            console.log(`👤 Token FCM del usuario ${orderData.userId}: ${fcmToken ? 'Encontrado' : 'No encontrado'}`);
          }
        } catch (userError) {
          console.error("❌ Error obteniendo token FCM del usuario:", userError);
        }
      }

      // Enviar push notification si el usuario tiene token FCM
      if (fcmToken) {
        try {
          await admin.messaging().send({
            notification: {
              title: "Pedido creado",
              body: `Tu pedido ha sido creado exitosamente. (ID: ${orderId})`,
            },
            data: {
              type: "order_created",
              orderId: orderId,
              link: `cyryel://shop/pedidos/${orderId}`,
              click_action: "FLUTTER_NOTIFICATION_CLICK"
            },
            token: fcmToken,
            android: {
              priority: "high" as const,
              notification: {
                sound: "default",
                channelId: "default",
                priority: "high" as const,
              },
            },
          });
          console.log("📲 Push notification de creación de pedido enviada al usuario");
        } catch (pushError) {
          console.error("❌ Error enviando push notification:", pushError);
          // Si el token es inválido, eliminarlo
          if (String(pushError).includes("invalid-registration-token") || 
              String(pushError).includes("registration-token-not-registered")) {
            try {
              await admin
                .firestore()
                .collection("users")
                .doc(orderData.userId)
                .update({
                  fcmToken: admin.firestore.FieldValue.delete(),
                  fcmTokenUpdatedAt: admin.firestore.FieldValue.delete(),
                });
              console.log("🧹 Token FCM inválido eliminado del usuario");
            } catch (cleanupError) {
              console.error("Error eliminando token inválido:", cleanupError);
            }
          }
        }
      } else {
        console.log("⚠️ Usuario no tiene token FCM, solo notificación en Firestore");
      }

      return {success: true, orderId: orderId};
    } catch (error) {
      console.error("❌ Error enviando notificación de pedido creado:", error);
      return {success: false, error: String(error)};
    }
  });

// ============================================================================
// NOTIFICACIÓN DE BIENVENIDA
// ============================================================================

/**
 * Cloud Function que se dispara cuando se crea un nuevo usuario
 * Envía notificación de bienvenida solo la primera vez (al crear cuenta)
 */
export const onUserCreated = functions.firestore
  .document("users/{userId}")
  .onCreate(async (snap, context) => {
    const userId = context.params.userId;
    const userData = snap.data();

    console.log("👤 Nuevo usuario creado:", userId);
    console.log("📧 Email:", userData.email);

    try {
      // Crear notificación de bienvenida en Firestore
      await admin
        .firestore()
        .collection("notificaciones")
        .add({
          userId: userId,
          titulo: "¡Bienvenido a CYRYEL!",
          mensaje:
            "Gracias por registrarte. Aquí recibirás actualizaciones de tus pedidos y ofertas.",
          tipo: "info",
          read: false,
          deliveryRelated: false,
          orderId: null,
          promoId: null,
          fecha: admin.firestore.FieldValue.serverTimestamp(),
        });

      // Si el usuario ya tiene token FCM, enviar push notification
      if (userData.fcmToken) {
        await admin.messaging().send({
          notification: {
            title: "¡Bienvenido a CYRYEL!",
            body:
              "Gracias por registrarte. Aquí recibirás actualizaciones de tus pedidos y ofertas.",
          },
          token: userData.fcmToken,
          android: {
            priority: "high" as const,
            notification: {
              sound: "default",
              channelId: "default",
              priority: "high" as const,
            },
          },
        });
        console.log("✅ Push notification de bienvenida enviada");
      } else {
        console.log(
          "⚠️ Usuario no tiene token FCM aún, solo notificación en Firestore"
        );
      }

      return {success: true, userId: userId};
    } catch (error) {
      console.error("❌ Error enviando notificación de bienvenida:", error);
      return {success: false, error: String(error)};
    }
  });

// ============================================================================
// MENSAJES BROADCAST
// ============================================================================

/**
 * Cloud Function para enviar mensaje a todos los usuarios
 * Solo admins pueden ejecutarla
 */
export const sendBroadcastMessage = functions.https.onCall(
  async (data, context) => {
    // Verificar autenticación
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Debes estar autenticado"
      );
    }

    const {title, message, sendPush} = data;

    // Validar parámetros
    if (!title || !message) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "title y message son requeridos"
      );
    }

    try {
      // Verificar que el usuario sea admin
      const userIsAdmin = await isAdmin(context.auth.uid);
      if (!userIsAdmin) {
        throw new functions.https.HttpsError(
          "permission-denied",
          "Solo admins pueden enviar mensajes broadcast"
        );
      }

      console.log("📢 Enviando mensaje broadcast");
      console.log("📝 Título:", title);
      console.log("✉️ Mensaje:", message);
      console.log("📲 Enviar push:", sendPush);

      // Obtener todos los usuarios
      const usersSnapshot = await admin
        .firestore()
        .collection("users")
        .get();

      if (usersSnapshot.empty) {
        throw new functions.https.HttpsError(
          "not-found",
          "No hay usuarios en la base de datos"
        );
      }

      console.log(`👥 ${usersSnapshot.size} usuarios encontrados`);

      // Crear notificaciones en Firestore para todos los usuarios
      const batch = admin.firestore().batch();
      const notificacionesRef = admin.firestore().collection("notificaciones");

      usersSnapshot.forEach((userDoc) => {
        const newNotifRef = notificacionesRef.doc();
        batch.set(newNotifRef, {
          userId: userDoc.id,
          titulo: title,
          mensaje: message,
          tipo: "info",
          read: false,
          deliveryRelated: false,
          orderId: null,
          promoId: null,
          fecha: admin.firestore.FieldValue.serverTimestamp(),
        });
      });

      await batch.commit();
      console.log("✅ Notificaciones creadas en Firestore");

      let pushCount = 0;
      let pushFailures = 0;

      // Si se debe enviar push notification
      if (sendPush) {
        // Obtener usuarios con token FCM
        const usersWithTokens = usersSnapshot.docs.filter(
          (doc) => doc.data().fcmToken
        );

        if (usersWithTokens.length > 0) {
          console.log(
            `📲 ${usersWithTokens.length} usuarios con token FCM`
          );

          const tokens = usersWithTokens.map((doc) => doc.data().fcmToken);

          // Crear mensaje FCM
          const fcmMessage = {
            notification: {
              title: title,
              body: message
            },
            data: {
              type: "broadcast",
              adminId: context.auth.uid,
            },
            android: {
              priority: "high" as const,
              notification: {
                sound: "default",
                channelId: "default",
                priority: "high" as const
              },
            },
          };

          // Enviar en lotes de 500 (límite de FCM)
          const batchSize = 500;
          const batches = [];

          for (let i = 0; i < tokens.length; i += batchSize) {
            const batchTokens = tokens.slice(i, i + batchSize);
            batches.push(
              admin.messaging().sendEachForMulticast({
                tokens: batchTokens,
                ...fcmMessage,
              })
            );
          }

          const results = await Promise.all(batches);

          results.forEach((result) => {
            pushCount += result.successCount;
            pushFailures += result.failureCount;
          });

          console.log(`✅ Push notifications enviadas: ${pushCount}`);
          console.log(`❌ Push notifications fallidas: ${pushFailures}`);
        } else {
          console.log("⚠️ No hay usuarios con token FCM");
        }
      }

      // Crear log de auditoría
      await createAuditLog(
        context.auth.uid,
        "broadcast_message",
        `Envió mensaje: "${title}"`,
        undefined,
        undefined,
        {
          title,
          messagePreview: message.substring(0, 100),
          sendPush,
          totalUsers: usersSnapshot.size,
          pushSent: pushCount,
        }
      );

      return {
        success: true,
        totalUsers: usersSnapshot.size,
        notificationsCreated: usersSnapshot.size,
        pushNotificationsSent: pushCount,
        pushNotificationsFailed: pushFailures,
      };
    } catch (error: any) {
      console.error("❌ Error enviando mensaje broadcast:", error);
      if (error instanceof functions.https.HttpsError) {
        throw error;
      }
      throw new functions.https.HttpsError(
        "internal",
        "Error enviando mensaje broadcast"
      );
    }
  }
);

// ============================================================================
// TRIGGER: DESCONTAR STOCK AL CREAR ORDEN
// ============================================================================

/**
 * Cloud Function trigger que se ejecuta cuando se crea una nueva orden.
 * Descuenta automáticamente el stock de los productos comprados.
 * Usa transacciones para garantizar consistencia y prevenir condiciones de carrera.
 */
export const onOrderCreated = functions.firestore
  .document("orders/{orderId}")
  .onCreate(async (snapshot, context) => {
    const order = snapshot.data();
    const orderId = context.params.orderId;

    try {
      console.log(`[onOrderCreated] Procesando orden ${orderId}`);

      // Validar que la orden tenga items
      if (!order.items || order.items.length === 0) {
        console.warn(`[onOrderCreated] Orden ${orderId} no tiene items`);
        return;
      }

      // Verificar si es una orden duplicada
      const isDuplicate = await checkForDuplicateOrder(order.userId, order.total, orderId);
      if (isDuplicate) {
        console.warn(`[onOrderCreated] ⚠️ ORDEN DUPLICADA DETECTADA: ${orderId} del usuario ${order.userId}. Cancelando procesamiento.`);
        // Eliminar automáticamente la orden duplicada
        const db = admin.firestore();
        await db.collection('orders').doc(orderId).delete();
        console.log(`[onOrderCreated] Orden duplicada ${orderId} eliminada automáticamente.`);
        return;
      }

      const db = admin.firestore();

      // ==========================================================
      // CALCULAR COSTO DE DELIVERY (SERVER-SIDE)
      // ==========================================================
      if (order.deliveryMethod === "domicilio" && order.latitude && order.longitude) {
        try {
          const configDoc = await db.collection("config").doc("delivery").get();
          const basePrice = configDoc.data()?.basePrice || 4.50;
          const costPerKm = configDoc.data()?.costPerKm || 1.30;

          const dist = haversineDistance(STORE_LATITUDE, STORE_LONGITUDE, order.latitude, order.longitude);

          let shipping: number;
          if (dist <= FREE_DELIVERY_RADIUS) {
            shipping = 0;
          } else {
            const distKm = dist / 1000;
            if (distKm <= 1.0) {
              shipping = basePrice;
            } else {
              shipping = Math.round((basePrice + (distKm - 1.0) * costPerKm) * 2) / 2;
            }
          }

          await db.collection("orders").doc(orderId).update({ shipping });
          console.log(`[onOrderCreated] Delivery cost calculado: S/ ${shipping} para orden ${orderId} (distancia: ${Math.round(dist)}m)`);
        } catch (deliveryErr) {
          console.error(`[onOrderCreated] Error calculando delivery cost:`, deliveryErr);
        }
      }

      // Usar transacción para garantizar consistencia atómica
      await db.runTransaction(async (transaction) => {
        const productUpdates: { ref: FirebaseFirestore.DocumentReference; quantity: number; currentStock: number; currentSoldCount?: number }[] = [];
        const promotionUpdates: { ref: FirebaseFirestore.DocumentReference; quantity: number; promoData: any }[] = [];

        // FASE 1: Leer todos los productos y promociones, validar stock
        for (const item of order.items) {
          // Verificar si es una promoción
          if (item.isPromotion && item.promotionData) {
            console.log(`[onOrderCreated] Item es promoción: ${item.promotionData.id}`);
            
            const promoRef = db.collection("promotions").doc(item.promotionData.id);
            const promoDoc = await transaction.get(promoRef);

            if (promoDoc.exists) {
              const promoData = promoDoc.data();
              const currentPromoSold = promoData?.soldCount || 0;
              const promoStockLimit = promoData?.stockLimit || 0;

              // Validar que la promoción tenga stock disponible antes de aceptar la orden
              if (promoStockLimit > 0 && (currentPromoSold + item.quantity) > promoStockLimit) {
                throw new Error(
                  `Promoción ${item.promotionData.id} sin stock suficiente. Disponible: ${Math.max(0, promoStockLimit - currentPromoSold)}, Solicitado: ${item.quantity}`
                );
              }
              promotionUpdates.push({
                ref: promoRef,
                quantity: item.quantity,
                promoData: promoData
              });

              // Descontar stock de cada producto en la promoción
              for (const promoProduct of item.promotionData.products) {
                const productRef = db.collection("products").doc(promoProduct.productId);
                const productDoc = await transaction.get(productRef);

                if (!productDoc.exists) {
                  throw new Error(`Producto ${promoProduct.productId} no existe`);
                }

                const productData = productDoc.data();
                const currentStock = productData?.stock || 0;
                const totalQuantityNeeded = promoProduct.quantity * item.quantity;
                // Calcular stock disponible restando la reserva
                const availableStock = Math.max(0, currentStock - STOCK_RESERVE);

                // Validar stock considerando la reserva
                if (availableStock < totalQuantityNeeded) {
                  throw new Error(
                    `Stock insuficiente para ${promoProduct.productName} en promoción. ` +
                    `Disponible: ${availableStock}, Necesario: ${totalQuantityNeeded} (Reserva: ${STOCK_RESERVE})`
                  );
                }

                productUpdates.push({
                  ref: productRef,
                  quantity: totalQuantityNeeded,
                  currentStock: currentStock
                });
              }
            }
          } else {
            // Item normal (no promoción)
            const productRef = db.collection("products").doc(item.productId);
            const productDoc = await transaction.get(productRef);

            if (!productDoc.exists) {
              throw new Error(`Producto ${item.productId} no existe`);
            }

            const productData = productDoc.data();
            const currentStock = productData?.stock || 0;
            const currentSoldCount = productData?.soldCount || 0; // AGREGAR
            // Calcular stock disponible restando la reserva
            const availableStock = Math.max(0, currentStock - STOCK_RESERVE);

            // Validar que haya stock suficiente considerando la reserva
            if (availableStock < item.quantity) {
              throw new Error(
                `Stock insuficiente para ${item.productName}. ` +
                `Disponible: ${availableStock}, Solicitado: ${item.quantity} (Reserva: ${STOCK_RESERVE})`
              );
            }

            productUpdates.push({
              ref: productRef,
              quantity: item.quantity,
              currentStock: currentStock,
              currentSoldCount: currentSoldCount // AGREGAR
            });
          }
        }

        // FASE 1.5: Detectar promociones por promotionId (Android app)
        const promoIdItems: { [id: string]: any[] } = {};
        for (const item of order.items) {
          if (item.promotionId && !item.isPromotion && !item.promotionData) {
            if (!promoIdItems[item.promotionId]) {
              promoIdItems[item.promotionId] = [];
            }
            promoIdItems[item.promotionId].push(item);
          }
        }
        for (const [promotionId, items] of Object.entries(promoIdItems)) {
          const promoRef = db.collection("promotions").doc(promotionId);
          const promoDoc = await transaction.get(promoRef);
          if (promoDoc.exists) {
            const promoData = promoDoc.data()!;
            const promoProducts = promoData?.products || [];
            let bundleCount = 0;
            for (const pp of promoProducts) {
              const match = items.find((it: any) => it.productId === pp.productId);
              if (match) {
                const b = Math.floor((match.quantity || 0) / (pp.quantity || 1));
                if (b > bundleCount) bundleCount = b;
              }
            }
            if (bundleCount > 0) {
              const currentSold = promoData.soldCount || 0;
              const stockLimit = promoData.stockLimit || 0;
              if (stockLimit > 0 && (currentSold + bundleCount) > stockLimit) {
                throw new Error(
                  `Promoción ${promotionId} sin stock suficiente. ` +
                  `Disponible: ${Math.max(0, stockLimit - currentSold)}, Solicitado: ${bundleCount}`
                );
              }
              promotionUpdates.push({ ref: promoRef, quantity: bundleCount, promoData });
              console.log(`[onOrderCreated] Promoción ${promotionId} por promotionId: ${bundleCount} bundle(s)`);
            }
          }
        }

        // FASE 2: Actualizar todos los productos (solo si todos tienen stock)
        for (const update of productUpdates) {
          const newStock = update.currentStock - update.quantity;
          
          transaction.update(update.ref, {
            stock: newStock,
            updatedAt: admin.firestore.FieldValue.serverTimestamp()
          });

          console.log(
            `[onOrderCreated] Producto ${update.ref.id}: ` +
            `${update.currentStock} → ${newStock} (descontados: ${update.quantity})`
          );
        }

        // FASE 3: Actualizar contadores de promociones
        for (const promoUpdate of promotionUpdates) {
          const currentSoldCount = promoUpdate.promoData?.soldCount || 0;
          const newSoldCount = currentSoldCount + promoUpdate.quantity;
          const stockLimit = promoUpdate.promoData?.stockLimit || 0;

          const updateData: any = {
            soldCount: newSoldCount,
            updatedAt: admin.firestore.FieldValue.serverTimestamp()
          };

          // Actualizar stock restante
          if (stockLimit > 0) {
            updateData.stockRemaining = stockLimit - newSoldCount;
          }

          // Auto-desactivar si se agotó el stock
          if (stockLimit > 0 && newSoldCount >= stockLimit) {
            updateData.active = false;
            updateData.isActive = false;
            updateData.stockDepletedAt = admin.firestore.FieldValue.serverTimestamp();
            console.log(
              `[onOrderCreated] ⚠️ Promoción ${promoUpdate.ref.id} AGOTADA: ` +
              `${newSoldCount}/${stockLimit} vendidos`
            );
          }

          transaction.update(promoUpdate.ref, updateData);

          console.log(
            `[onOrderCreated] Promoción ${promoUpdate.ref.id}: ` +
            `soldCount ${currentSoldCount} → ${newSoldCount}`
          );
        }
      });

      console.log(`[onOrderCreated] ✅ Stock descontado exitosamente para orden ${orderId}`);

      // Actualizar la orden con timestamp de procesamiento de inventario
      await snapshot.ref.update({
        inventoryProcessed: true,
        inventoryProcessedAt: admin.firestore.FieldValue.serverTimestamp()
      });

      // ============================================================
      // PROGRAMAR TIMEOUT DE PAGO (1 HORA)
      // ============================================================
      // Solo programar timeout si el pago está pendiente y NO es contra entrega
      if (order.paymentStatus === "pendiente" && order.paymentMethod !== "contra_entrega") {
        console.log(`[onOrderCreated] ⏰ Programando timeout de pago para orden ${orderId}`);
        await admin.firestore().collection("paymentTimeouts").doc(orderId).set({
          orderId: orderId,
          userId: order.userId,
          createdAt: admin.firestore.FieldValue.serverTimestamp(),
          expiresAt: admin.firestore.Timestamp.fromMillis(
            Date.now() + 60 * 60 * 1000 // 1 hora en milisegundos
          ),
          processed: false,
          paymentStatus: order.paymentStatus,
          total: order.total
        });
        console.log(`[onOrderCreated] ✅ Timeout de pago programado (1 hora)`);

        // Notificar al admin que hay un pago pendiente
        try {
          const adminsSnapshot = await admin.firestore()
            .collection("users")
            .where("role", "==", "admin")
            .get();

          if (!adminsSnapshot.empty) {
            const adminTokens: string[] = [];
            const adminNotifPromises: Promise<FirebaseFirestore.DocumentReference>[] = [];

            const shortOrderId = orderId.substring(0, 8);
            const orderTotal = order?.total ? `S/ ${order.total.toFixed(2)}` : "";

            adminsSnapshot.forEach((doc) => {
              const userData = doc.data();
              if (userData?.fcmToken && !adminTokens.includes(userData.fcmToken)) {
                adminTokens.push(userData.fcmToken);
              }

              adminNotifPromises.push(
                admin.firestore().collection("notificaciones").add({
                  userId: doc.id,
                  titulo: "💳 Pago pendiente",
                  mensaje: `Pedido #${shortOrderId} ${orderTotal ? '(' + orderTotal + ')' : ''} tiene el pago pendiente. Por favor confírmalo en la dashboard.`,
                  tipo: "admin",
                  priority: "high",
                  read: false,
                  orderId: orderId,
                  link: `cyryel://admin/pedidos/${orderId}`,
                  fecha: admin.firestore.FieldValue.serverTimestamp()
                })
              );
            });

            await Promise.all(adminNotifPromises);

            if (adminTokens.length > 0) {
              const paymentNotifMessage = {
                notification: {
                  title: "💳 Pago pendiente",
                  body: `Pedido #${shortOrderId} ${orderTotal ? '(' + orderTotal + ')' : ''} — confirma el pago en la dashboard`
                },
                data: {
                  type: "payment_pending",
                  orderId: orderId,
                  link: `cyryel://admin/pedidos/${orderId}`
                },
                android: {
                  priority: "high" as const,
                  notification: {
                    sound: "default",
                    channelId: "default",
                    priority: "high" as const,
                    color: "#FFA000"
                  }
                }
              };

              for (let i = 0; i < adminTokens.length; i += 500) {
                const batchTokens = adminTokens.slice(i, i + 500);
                await admin.messaging().sendEachForMulticast({ tokens: batchTokens, ...paymentNotifMessage });
              }
            }

            console.log(`[onOrderCreated] 📲 Notificación de pago pendiente enviada a ${adminsSnapshot.size} admins`);
          }
        } catch (paymentNotifError) {
          console.error(`[onOrderCreated] ⚠️ Error enviando notificación de pago pendiente:`, paymentNotifError);
        }
      }

      // ============================================================
      // NOTIFICAR A DELIVERIES NUEVO PEDIDO
      // ============================================================
      try {
        // Resolve customer name from order data or users collection
        let customerName = (order && order.customerContact && order.customerContact.name) || (order && order.deliveryAddress && order.deliveryAddress.recipientName) || '';
        customerName = (typeof customerName === 'string' ? customerName.trim() : '') || '';
        if (!customerName && order && order.userId) {
          try {
            const userDoc = await admin.firestore().collection('users').doc(order.userId).get();
            if (userDoc.exists) {
              const ud = userDoc.data();
              customerName = (ud?.displayName || ud?.name || '').toString().trim();
            }
          } catch (udErr) {
            console.warn('onOrderCreated: error fetching user doc for customer name', udErr);
          }
        }
        if (!customerName) customerName = 'Cliente';

        const deliveriesSnapshot = await admin.firestore()
          .collection("users")
          .where("role", "==", "delivery")
          .where("fcmToken", "!=", null)
          .get();

        const tokens = deliveriesSnapshot.docs
          .map(doc => doc.data().fcmToken)
          .filter(token => !!token);

        const deliveryLink = `cyryel://delivery/pedidos/${orderId}`;

        // Crear notificación en Firestore para cada delivery, incluyendo id y nombre del cliente
        for (const deliveryDoc of deliveriesSnapshot.docs) {
          await admin.firestore().collection("notificaciones").add({
            userId: deliveryDoc.id,
            titulo: "🚚 Nuevo pedido disponible",
            mensaje: `Nuevo pedido #${orderId} de ${customerName}`,
            tipo: "delivery",
            orderId: orderId,
            deliveryRelated: true,
            fecha: admin.firestore.FieldValue.serverTimestamp(),
            read: false,
            // link for delivery app
            link: deliveryLink,
            customerName: customerName
          });
        }

        if (tokens.length > 0) {
              const message = {
            notification: {
              title: "Nuevo pedido disponible",
              body: `Pedido #${orderId.substring(0, 8)} de ${customerName} para entregar.`,
            },
            data: {
              type: "new_order",
              orderId: orderId,
              // provide delivery deep link for delivery app clients
              link: deliveryLink,
              customerName: customerName
            },
                android: {
                  priority: "high" as const,
                  notification: {
                    sound: "default",
                    channelId: "default",
                    priority: "high" as const
                  },
                },
          };

          // Enviar en lotes de 500, procesando cada batch para log detallado
          const batchSize = 500;
          let successCount = 0;
          let failureCount = 0;

          for (let i = 0; i < tokens.length; i += batchSize) {
            const batchTokens = tokens.slice(i, i + batchSize);
            try {
              const result = await admin.messaging().sendEachForMulticast({
                tokens: batchTokens,
                ...message,
              });

              successCount += result.successCount;
              failureCount += result.failureCount;

              if (result.failureCount > 0) {
                console.warn(`❌ Fallos al enviar push a deliveries en este lote: ${result.failureCount}`);
                // Revisar cada respuesta para obtener el token y el error
                result.responses.forEach(async (resp, idx) => {
                  if (!resp.success) {
                    const failedToken = batchTokens[idx];
                    console.error(`-- Token fallido: ${failedToken} - Error: ${resp.error?.code} ${resp.error?.message}`);

                    // Si el error indica token inválido, eliminarlo de la colección users
                    const code = resp.error?.code || '';
                    if (
                      code === 'messaging/invalid-registration-token' ||
                      code === 'messaging/registration-token-not-registered'
                    ) {
                      try {
                        const usersWithToken = await admin.firestore().collection('users').where('fcmToken', '==', failedToken).limit(1).get();
                        if (!usersWithToken.empty) {
                          const userDoc = usersWithToken.docs[0];
                          await userDoc.ref.update({
                            fcmToken: admin.firestore.FieldValue.delete(),
                            fcmTokenUpdatedAt: admin.firestore.FieldValue.delete(),
                          });
                          console.log(`🧹 Token inválido eliminado del usuario ${userDoc.id}`);
                        }
                      } catch (cleanupErr) {
                        console.error('Error limpiando token inválido:', cleanupErr);
                      }
                    }
                  }
                });
              }
            } catch (batchError) {
              console.error('Error enviando batch de push a deliveries:', batchError);
              // Contar todo el batch como fallido
              failureCount += Math.min(batchSize, tokens.length - i);
            }
          }

          console.log(`✅ Notificaciones push enviadas a deliveries: ${successCount}`);
          if (failureCount > 0) {
            console.warn(`❌ Fallos al enviar push a deliveries: ${failureCount}`);
          }
        } else {
          console.log("No hay deliveries con token FCM para notificar.");
        }
      } catch (notifError) {
        console.error("Error notificando a deliveries:", notifError);
      }

    } catch (error: any) {
      console.error(`[onOrderCreated] ❌ Error procesando orden ${orderId}:`, error);

      const errorMessage = error.message || "Error desconocido";

      // Marcar la orden con error de inventario para revisión manual
      await snapshot.ref.update({
        inventoryProcessed: false,
        inventoryError: errorMessage,
        inventoryErrorAt: admin.firestore.FieldValue.serverTimestamp()
      });

      // ============================================================
      // NOTIFICAR A ADMINS SOBRE ERROR DE INVENTARIO
      // ============================================================
      try {
        console.log(`[onOrderCreated] 📧 Notificando admins sobre error de inventario en orden ${orderId}`);
        
        const adminsSnapshot = await admin.firestore()
          .collection("users")
          .where("role", "==", "admin")
          .get();

        if (!adminsSnapshot.empty) {
          const tokens: string[] = [];
          const notificationPromises: Promise<FirebaseFirestore.DocumentReference>[] = [];

          // Extraer información de la orden para el mensaje
          const customerName = order?.customerContact?.name || 
                              order?.deliveryAddress?.recipientName || 
                              order?.userName || 
                              "Cliente";
          
          const orderTotal = order?.total ? `S/ ${order.total.toFixed(2)}` : "";
          const shortOrderId = orderId.substring(0, 8);

          // Crear mensaje descriptivo
          const errorTitle = "⚠️ Error de Stock en Pedido";
          const errorBody = `Pedido #${shortOrderId} de ${customerName} ${orderTotal ? '(' + orderTotal + ')' : ''} no pudo procesarse: ${errorMessage}`;

          adminsSnapshot.forEach((doc) => {
            const userData = doc.data();
            if (userData?.fcmToken) {
              tokens.push(userData.fcmToken);
            }

            // Crear notificación en Firestore para cada admin
            notificationPromises.push(
              admin.firestore().collection("notificaciones").add({
                userId: doc.id,
                titulo: errorTitle,
                mensaje: errorBody,
                tipo: "admin_alert",
                priority: "high",
                orderId: orderId,
                inventoryError: true,
                errorDetails: errorMessage,
                fecha: admin.firestore.FieldValue.serverTimestamp(),
                read: false,
                link: `cyryel://admin/pedidos/${orderId}`
              })
            );
          });

          // Guardar notificaciones en Firestore
          await Promise.all(notificationPromises);
          console.log(`[onOrderCreated] ✅ Notificaciones Firestore creadas para ${adminsSnapshot.size} admins`);

          // Enviar push notifications si hay tokens
          if (tokens.length > 0) {
            const message = {
              notification: {
                title: errorTitle,
                body: errorBody
              },
              data: {
                type: "inventory_error",
                orderId: orderId,
                error: errorMessage,
                priority: "high",
                link: `cyryel://admin/pedidos/${orderId}`
              },
              android: {
                priority: "high" as const,
                notification: {
                  sound: "default",
                  channelId: "default",
                  priority: "high" as const,
                  color: "#FF0000"
                }
              }
            };

            const response = await admin.messaging().sendEachForMulticast({
              tokens: tokens,
              ...message
            });

            console.log(
              `[onOrderCreated] 📲 Push notifications enviadas: ${response.successCount} exitosas, ${response.failureCount} fallidas`
            );
          } else {
            console.log("[onOrderCreated] No hay tokens FCM disponibles para admins");
          }
        } else {
          console.log("[onOrderCreated] No se encontraron admins para notificar");
        }
      } catch (notifError) {
        console.error(`[onOrderCreated] ⚠️ Error enviando notificación a admins:`, notifError);
        // No lanzar el error, solo log
      }

      // No lanzar el error para no reintentar (ya marcamos la orden con error)
      // El admin puede revisar ordenes con inventoryProcessed: false
    }
  });

// ============================================================================
// TRIGGER: DEVOLVER STOCK AL CANCELAR/DEVOLVER ORDEN
// ============================================================================

/**
 * Cloud Function trigger que se ejecuta cuando se actualiza una orden.
 * Si el estado cambia a 'cancelado' o 'devuelto', devuelve automáticamente
 * el stock de los productos al inventario.
 */
export const onOrderUpdated = functions.firestore
  .document("orders/{orderId}")
  .onUpdate(async (change, context) => {
    const beforeData = change.before.data();
    const afterData = change.after.data();
    const orderId = context.params.orderId;

    try {
      const oldStatus = beforeData.status;
      const newStatus = afterData.status;
      const statusChanged = oldStatus !== newStatus;

      // ==========================================================
      // NOTIFICAR AL USUARIO SOBRE CAMBIO DE ESTADO DEL PEDIDO
      // ==========================================================
      if (statusChanged && afterData.userId) {
        const statusTitles: {[key: string]: string} = {
          confirmado: "Pedido confirmado",
          listo_para_recoger: "Pedido listo para recoger",
          en_reparto: "Pedido en reparto",
          en_camino: "Pedido en camino",
          entregado: "Pedido entregado",
          cancelado: "Pedido cancelado",
          devuelto: "Pedido devuelto",
        };
        const statusMessages: {[key: string]: string} = {
          confirmado: `Tu pedido #${orderId.substring(0, 8)} ha sido confirmado`,
          listo_para_recoger: `Tu pedido #${orderId.substring(0, 8)} esta listo para recoger`,
          en_reparto: `Tu pedido #${orderId.substring(0, 8)} esta en reparto`,
          en_camino: `Tu pedido #${orderId.substring(0, 8)} esta en camino`,
          entregado: `Tu pedido #${orderId.substring(0, 8)} ha sido entregado`,
          cancelado: `Tu pedido #${orderId.substring(0, 8)} ha sido cancelado`,
          devuelto: `Tu pedido #${orderId.substring(0, 8)} ha sido devuelto`,
        };

        const title = statusTitles[newStatus] || "Estado actualizado";
        const body = statusMessages[newStatus] || `Tu pedido #${orderId.substring(0, 8)} cambio a ${newStatus}`;

        try {
          await admin.firestore().collection("notificaciones").add({
            userId: afterData.userId,
            titulo: title,
            mensaje: body,
            tipo: "pedido",
            orderId: orderId,
            read: false,
            deliveryRelated: true,
            fecha: admin.firestore.FieldValue.serverTimestamp(),
            link: `cyryel://shop/pedidos/${orderId}`
          });

          if (afterData.fcmToken) {
            await admin.messaging().send({
              notification: { title, body },
              data: {
                type: "order_status",
                orderId: orderId,
                newStatus: newStatus,
                link: `cyryel://shop/pedidos/${orderId}`
              },
              token: afterData.fcmToken,
              android: {
                priority: "high",
                notification: { sound: "default", channelId: "default", priority: "high" }
              }
            });
          }
          console.log(`[onOrderUpdated] Notificacion enviada para orden ${orderId} (${newStatus})`);
        } catch (notifErr) {
          console.error(`[onOrderUpdated] Error enviando notificacion:`, notifErr);
        }
      }

      // ==========================================================
      // RESTAURAR STOCK SI SE CANCELÓ O DEVOLVIÓ
      // ==========================================================
      const shouldRestoreStock = 
        (newStatus === "cancelado" || newStatus === "devuelto") && 
        oldStatus !== newStatus;

      if (!shouldRestoreStock) {
        return;
      }

      console.log(`[onOrderUpdated] Orden ${orderId} cambió a ${newStatus}, restaurando stock`);

      // Validar que la orden tenga items
      if (!afterData.items || afterData.items.length === 0) {
        console.warn(`[onOrderUpdated] Orden ${orderId} no tiene items`);
        return;
      }

      // Validar que el inventario se haya procesado previamente
      if (!afterData.inventoryProcessed) {
        console.warn(`[onOrderUpdated] Orden ${orderId} no tiene inventario procesado, nada que devolver`);
        return;
      }

      const db = admin.firestore();

      // Usar transacción para garantizar consistencia atómica
      await db.runTransaction(async (transaction) => {
        const productUpdates: { ref: FirebaseFirestore.DocumentReference; quantity: number; currentStock: number }[] = [];
        const promotionUpdates: { ref: FirebaseFirestore.DocumentReference; quantity: number; promoData: any }[] = [];

        // FASE 1: Leer todos los productos y promociones
        for (const item of afterData.items) {
          // Verificar si es una promoción
          if (item.isPromotion && item.promotionData) {
            console.log(`[onOrderUpdated] Item es promoción: ${item.promotionData.id}`);
            
            const promoRef = db.collection("promotions").doc(item.promotionData.id);
            const promoDoc = await transaction.get(promoRef);

            if (promoDoc.exists) {
              const promoData = promoDoc.data();
              promotionUpdates.push({
                ref: promoRef,
                quantity: item.quantity,
                promoData: promoData
              });

              // Devolver stock de cada producto en la promoción
              for (const promoProduct of item.promotionData.products) {
                const productRef = db.collection("products").doc(promoProduct.productId);
                const productDoc = await transaction.get(productRef);

                if (!productDoc.exists) {
                  console.warn(`[onOrderUpdated] Producto ${promoProduct.productId} no existe, omitiendo`);
                  continue;
                }

                const productData = productDoc.data();
                const currentStock = productData?.stock || 0;
                const totalQuantityToRestore = promoProduct.quantity * item.quantity;

                productUpdates.push({
                  ref: productRef,
                  quantity: totalQuantityToRestore,
                  currentStock: currentStock
                });
              }
            }
          } else {
            // Item normal (no promoción)
            const productRef = db.collection("products").doc(item.productId);
            const productDoc = await transaction.get(productRef);

            if (!productDoc.exists) {
              console.warn(`[onOrderUpdated] Producto ${item.productId} no existe, omitiendo`);
              continue;
            }

            const productData = productDoc.data();
            const currentStock = productData?.stock || 0;

            productUpdates.push({
              ref: productRef,
              quantity: item.quantity,
              currentStock: currentStock
            });
          }
        }

        // FASE 1.5: Detectar promociones por promotionId (Android app)
        const promoIdItems: { [id: string]: any[] } = {};
        for (const item of afterData.items) {
          if (item.promotionId && !item.isPromotion && !item.promotionData) {
            if (!promoIdItems[item.promotionId]) {
              promoIdItems[item.promotionId] = [];
            }
            promoIdItems[item.promotionId].push(item);
          }
        }
        for (const [promotionId, items] of Object.entries(promoIdItems)) {
          const promoRef = db.collection("promotions").doc(promotionId);
          const promoDoc = await transaction.get(promoRef);
          if (promoDoc.exists) {
            const promoData = promoDoc.data();
            const promoProducts = promoData?.products || [];
            let bundleCount = 0;
            for (const pp of promoProducts) {
              const match = items.find((it: any) => it.productId === pp.productId);
              if (match) {
                const b = Math.floor((match.quantity || 0) / (pp.quantity || 1));
                if (b > bundleCount) bundleCount = b;
              }
            }
            if (bundleCount > 0) {
              promotionUpdates.push({ ref: promoRef, quantity: bundleCount, promoData });
              console.log(`[onOrderUpdated] Promoción ${promotionId} por promotionId: ${bundleCount} bundle(s)`);
            }
          }
        }

        // FASE 2: Devolver stock a todos los productos
        for (const update of productUpdates) {
          const newStock = update.currentStock + update.quantity;
          
          transaction.update(update.ref, {
            stock: newStock,
            updatedAt: admin.firestore.FieldValue.serverTimestamp()
          });

          console.log(
            `[onOrderUpdated] Producto ${update.ref.id}: ` +
            `${update.currentStock} → ${newStock} (devueltos: ${update.quantity})`
          );
        }

        // FASE 3: Restaurar contadores de promociones
        for (const promoUpdate of promotionUpdates) {
          const currentSoldCount = promoUpdate.promoData?.soldCount || 0;
          const newSoldCount = Math.max(0, currentSoldCount - promoUpdate.quantity);
          const stockLimit = promoUpdate.promoData?.stockLimit || 0;

          const updateData: any = {
            soldCount: newSoldCount,
            updatedAt: admin.firestore.FieldValue.serverTimestamp()
          };

          // Actualizar stock restante
          if (stockLimit > 0) {
            updateData.stockRemaining = stockLimit - newSoldCount;
          }

          // Reactivar si estaba desactivada por falta de stock y ahora hay disponible
          if (stockLimit > 0 && !promoUpdate.promoData.active && newSoldCount < stockLimit) {
            updateData.active = true;
            updateData.isActive = true;
            updateData.stockRestoredAt = admin.firestore.FieldValue.serverTimestamp();
            console.log(
              `[onOrderUpdated] ✅ Promoción ${promoUpdate.ref.id} REACTIVADA: ` +
              `${newSoldCount}/${stockLimit} vendidos`
            );
          }

          transaction.update(promoUpdate.ref, updateData);

          console.log(
            `[onOrderUpdated] Promoción ${promoUpdate.ref.id}: ` +
            `soldCount ${currentSoldCount} → ${newSoldCount}`
          );
        }
      });

      console.log(`[onOrderUpdated] ✅ Stock restaurado exitosamente para orden ${orderId}`);

      // Marcar la orden como inventario restaurado
      await change.after.ref.update({
        inventoryRestored: true,
        inventoryRestoredAt: admin.firestore.FieldValue.serverTimestamp(),
        inventoryRestoredReason: newStatus
      });

    } catch (error: any) {
      console.error(`[onOrderUpdated] ❌ Error restaurando stock para orden ${orderId}:`, error);

      // Marcar la orden con error de restauración
      await change.after.ref.update({
        inventoryRestored: false,
        inventoryRestoreError: error.message || "Error desconocido",
        inventoryRestoreErrorAt: admin.firestore.FieldValue.serverTimestamp()
      });
    }
  });

// ============================================================================
// ORDEN: VERIFICACIÓN AUTOMÁTICA DE TIMEOUTS DE PAGO
// ============================================================================

/**
 * Cloud Function programada que se ejecuta cada 10 minutos
 * Verifica y cancela órdenes con pago pendiente después de 1 hora
 */
export const checkPaymentTimeouts = functions.pubsub
  .schedule("every 10 minutes")
  .onRun(async (context) => {
    console.log("[checkPaymentTimeouts] 🔍 Verificando timeouts de pago...");

    try {
      const now = admin.firestore.Timestamp.now();

      // Buscar timeouts expirados que no han sido procesados
      const expiredTimeouts = await admin
        .firestore()
        .collection("paymentTimeouts")
        .where("processed", "==", false)
        .where("expiresAt", "<=", now)
        .limit(100) // Procesar máximo 100 a la vez
        .get();

      if (expiredTimeouts.empty) {
        console.log("[checkPaymentTimeouts] ✅ No hay timeouts expirados");
        return null;
      }

      console.log(`[checkPaymentTimeouts] 📋 ${expiredTimeouts.size} timeouts expirados encontrados`);

      const batch = admin.firestore().batch();
      const promises: Promise<any>[] = [];

      for (const timeoutDoc of expiredTimeouts.docs) {
        const timeoutData = timeoutDoc.data();
        const orderId = timeoutData.orderId;

        try {
          // Verificar el estado actual del pedido
          const orderRef = admin.firestore().collection("orders").doc(orderId);
          const orderSnap = await orderRef.get();

          if (!orderSnap.exists) {
            console.log(`[checkPaymentTimeouts] ⚠️ Orden ${orderId} no existe, marcando timeout como procesado`);
            batch.update(timeoutDoc.ref, { processed: true });
            continue;
          }

          const orderData = orderSnap.data();

          // Si el pago ya fue completado, marcar timeout como procesado
          if (orderData?.paymentStatus === "completado") {
            console.log(`[checkPaymentTimeouts] ✅ Pago de orden ${orderId} completado, cancelando timeout`);
            batch.update(timeoutDoc.ref, { 
              processed: true,
              processedAt: admin.firestore.FieldValue.serverTimestamp(),
              result: "payment_completed"
            });
            continue;
          }

          // Si el pago sigue pendiente después de 1 hora, enviar recordatorio al admin
          if (orderData?.paymentStatus === "pendiente") {
            console.log(`[checkPaymentTimeouts] ⏰ Orden ${orderId} con pago pendiente > 1 hora, enviando recordatorio al admin`);

            // Marcar timeout como procesado para no volver a notificar
            batch.update(timeoutDoc.ref, {
              processed: true,
              processedAt: admin.firestore.FieldValue.serverTimestamp(),
              result: "reminder_sent"
            });

            // Buscar admins para notificar
            try {
              const adminsSnapshot = await admin.firestore()
                .collection("users")
                .where("role", "==", "admin")
                .get();

              if (!adminsSnapshot.empty) {
                const adminTokens: string[] = [];
                const shortOrderId = orderId.substring(0, 8);
                const orderTotal = orderData?.total ? `S/ ${orderData.total.toFixed(2)}` : "";

                adminsSnapshot.forEach((doc) => {
                  const userData = doc.data();
                  if (userData?.fcmToken && !adminTokens.includes(userData.fcmToken)) {
                    adminTokens.push(userData.fcmToken);
                  }

                  promises.push(
                    admin.firestore().collection("notificaciones").add({
                      userId: doc.id,
                      titulo: "⏰ Recordatorio de pago",
                      mensaje: `El pedido #${shortOrderId} ${orderTotal ? '(' + orderTotal + ')' : ''} sigue sin pago. Por favor confírmalo en la dashboard.`,
                      tipo: "admin",
                      priority: "high",
                      read: false,
                      orderId: orderId,
                      link: `cyryel://admin/pedidos/${orderId}`,
                      fecha: admin.firestore.FieldValue.serverTimestamp()
                    })
                  );
                });

                if (adminTokens.length > 0) {
                  const reminderMessage = {
                    notification: {
                      title: "⏰ Recordatorio de pago",
                      body: `Pedido #${shortOrderId} ${orderTotal ? '(' + orderTotal + ')' : ''} — sigue pendiente de pago`
                    },
                    data: {
                      type: "payment_reminder",
                      orderId: orderId,
                      link: `cyryel://admin/pedidos/${orderId}`
                    },
                    android: {
                      priority: "high" as const,
                      notification: {
                        sound: "default",
                        channelId: "default",
                        priority: "high" as const,
                        color: "#FFA000"
                      }
                    }
                  };

                  for (let i = 0; i < adminTokens.length; i += 500) {
                    const batchTokens = adminTokens.slice(i, i + 500);
                    promises.push(
                      admin.messaging().sendEachForMulticast({ tokens: batchTokens, ...reminderMessage }).catch(error => {
                        console.error(`[checkPaymentTimeouts] Error enviando push recordatorio:`, error);
                      })
                    );
                  }
                }

                console.log(`[checkPaymentTimeouts] 📲 Recordatorio de pago enviado a ${adminsSnapshot.size} admins para orden ${orderId}`);
              }
            } catch (reminderError) {
              console.error(`[checkPaymentTimeouts] ⚠️ Error enviando recordatorio para orden ${orderId}:`, reminderError);
            }
          }
        } catch (error: any) {
          console.error(`[checkPaymentTimeouts] ❌ Error procesando timeout ${orderId}:`, error);
          batch.update(timeoutDoc.ref, {
            processed: true,
            processedAt: admin.firestore.FieldValue.serverTimestamp(),
            result: "error",
            error: error.message
          });
        }
      }

      // Ejecutar todas las actualizaciones
      await batch.commit();
      await Promise.allSettled(promises);

      console.log(`[checkPaymentTimeouts] ✅ ${expiredTimeouts.size} timeouts procesados`);

      return null;
    } catch (error: any) {
      console.error("[checkPaymentTimeouts] ❌ Error general:", error);
      throw error;
    }
  });

/**
 * Cloud Function que se dispara cuando se actualiza una orden
 * Cancela el timeout si el pago fue completado
 */
export const onOrderPaymentUpdated = functions.firestore
  .document("orders/{orderId}")
  .onUpdate(async (change, context) => {
    const orderId = context.params.orderId;
    const beforeData = change.before.data();
    const afterData = change.after.data();

    // Verificar si el estado de pago cambió
    if (beforeData.paymentStatus !== afterData.paymentStatus) {
      console.log(
        `[onOrderPaymentUpdated] 💳 Pago de orden ${orderId} cambió: ` +
        `${beforeData.paymentStatus} → ${afterData.paymentStatus}`
      );

      // Si el pago fue completado, cancelar el timeout
      if (afterData.paymentStatus === "completado") {
        try {
          const timeoutRef = admin.firestore().collection("paymentTimeouts").doc(orderId);
          const timeoutSnap = await timeoutRef.get();

          if (timeoutSnap.exists && !timeoutSnap.data()?.processed) {
            await timeoutRef.update({
              processed: true,
              processedAt: admin.firestore.FieldValue.serverTimestamp(),
              result: "payment_completed_early"
            });

            console.log(`[onOrderPaymentUpdated] ✅ Timeout cancelado para orden ${orderId}`);
          }
        } catch (error: any) {
          console.error(`[onOrderPaymentUpdated] ⚠️ Error cancelando timeout para orden ${orderId}:`, error);
        }
      }
    }

    return null;
  });

// ============================================================================
// DELIVERY: CREAR DELIVERY DISPONIBLE CUANDO LA ORDEN ESTÉ CONFIRMADA
// ============================================================================

/**
 * Cloud Function que se dispara cuando se crea o actualiza una orden.
 * Crea un documento en `deliveries/{orderId}` con `status: "disponible"`
 * cuando la orden está lista para delivery a domicilio.
 *
 * Casos:
 * - contra_entrega + domicilio: delivery disponible inmediato al crear orden
 * - codigo + domicilio: admin confirma pago → status pasa a "confirmado" → delivery creado
 */
export const onOrderReadyForDelivery = functions.firestore
  .document('orders/{orderId}')
  .onWrite(async (change, context) => {
    const after = change.after.exists ? change.after.data() : null;
    const orderId = context.params.orderId;

    // Solo procesar si la orden existe
    if (!after) return null;

    // Solo domicilio
    if (after.deliveryMethod !== "domicilio") return null;

    const isNewOrder = !change.before.exists;
    let shouldCreateDelivery = false;

    // Ambos métodos de pago crean delivery inmediato al crear la orden
    shouldCreateDelivery = isNewOrder;

    if (!shouldCreateDelivery) return null;

    try {
      // Verificar que no exista ya un delivery para esta orden
      const deliveryRef = admin.firestore().collection("deliveries").doc(orderId);
      const deliverySnap = await deliveryRef.get();
      if (deliverySnap.exists) {
        console.log(`[onOrderReadyForDelivery] ⚠️ Delivery ya existe para orden ${orderId}, saltando`);
        return null;
      }

      // Generar código de confirmación de 4 dígitos
      const confirmationCode = Math.floor(1000 + Math.random() * 9000).toString();

      const now = admin.firestore.FieldValue.serverTimestamp();

      // Crear documento en deliveries
      await deliveryRef.set({
        orderId: orderId,
        status: "disponible",
        confirmationCode: confirmationCode,
        assignedAt: now,
        createdAt: now,
        updatedAt: now
      });

      // Actualizar la orden con el código de confirmación
      await change.after.ref.update({
        deliveryConfirmationCode: confirmationCode,
        deliveryCreatedAt: now
      });

      console.log(`[onOrderReadyForDelivery] ✅ Delivery creado para orden ${orderId} con código ${confirmationCode}`);
      return null;
    } catch (error: any) {
      console.error(`[onOrderReadyForDelivery] ❌ Error creando delivery para orden ${orderId}:`, error);
      return null;
    }
  });
