const {onCall} = require('firebase-functions/v2/https');
const {onDocumentCreated, onDocumentUpdated} = require('firebase-functions/v2/firestore');
const {onSchedule} = require('firebase-functions/v2/scheduler');
const {setGlobalOptions} = require('firebase-functions/v2');
const admin = require('firebase-admin');

admin.initializeApp();

// Configuración global para todas las funciones
setGlobalOptions({
  region: 'us-central1',
  maxInstances: 10
});

/**
 * Genera el contenido de notificación según el tipo de evento
 */
function getNotificationContent(eventType, eventData) {
  const notifications = {
    'TASK_CREATED': {
      title: '📋 Nueva Tarea',
      body: 'Se ha creado una nueva tarea'
    },
    'SUBTASK_CREATED': {
      title: '✅ Nueva Subtarea',
      body: 'Se agregó una subtarea a tu tarea'
    },
    'SUBTASK_STATUS_CHANGED': {
      title: '🔄 Estado Actualizado',
      body: 'El estado de una subtarea cambió'
    },
    'SUBTASK_UPDATED': {
      title: '📝 Subtarea Actualizada',
      body: 'Se modificó una subtarea'
    },
    'SUBTASK_DELETED': {
      title: '🗑️ Subtarea Eliminada',
      body: 'Se eliminó una subtarea'
    },
    'TASK_DELETED': {
      title: '🗑️ Tarea Eliminada',
      body: 'Se eliminó una tarea'
    },
    'BID_PLACED': {
      title: '💰 Nueva Oferta',
      body: 'Recibiste una nueva oferta'
    },
    'BID_ACCEPTED': {
      title: '✅ Oferta Aceptada',
      body: 'Tu oferta fue aceptada'
    },
    'BID_REJECTED': {
      title: '❌ Oferta Rechazada',
      body: 'Tu oferta fue rechazada'
    },
    'BUDGET_APPROVED': {
      title: '✅ Presupuesto Aprobado',
      body: 'El presupuesto fue aprobado'
    },
    'BUDGET_REJECTED': {
      title: '❌ Presupuesto Rechazado',
      body: 'El presupuesto fue rechazado'
    },
    'BUDGET_STATUS_CHANGED': {
      title: '💵 Cambio de Presupuesto',
      body: 'El estado del presupuesto cambió'
    },
    'GROUP_CREATED': {
      title: '👥 Nuevo Grupo',
      body: 'Se creó un nuevo grupo'
    },
    'GROUP_MEMBER_ADDED': {
      title: '👤 Miembro Agregado',
      body: 'Se agregó un miembro al grupo'
    },
    'POSTPONEMENT_REQUESTED': {
      title: '📅 Solicitud de Prórroga',
      body: 'Se solicitó posponer una subtarea'
    },
    'POSTPONEMENT_ACCEPTED': {
      title: '✅ Prórroga Aprobada',
      body: 'Tu solicitud de prórroga fue aceptada'
    },
    'POSTPONEMENT_REJECTED': {
      title: '❌ Prórroga Rechazada',
      body: 'Tu solicitud de prórroga fue rechazada'
    },
    'CHAT_DELETED': {
      title: '💬 Chat Eliminado',
      body: 'Un chat ha sido eliminado'
    }
  };

  return notifications[eventType] || {
    title: '🔔 Actualización',
    body: 'Hay cambios en tus tareas'
  };
}

/**
 * Cloud Function para enviar eventos de actualización vía FCM
 * 
 * Esta función envía mensajes FCM silenciosos (data-only) a múltiples usuarios
 * para notificarles sobre cambios en tareas, subtareas, presupuestos, etc.
 * 
 * Los mensajes son procesados por MyFirebaseMessagingService en el cliente
 * que los emite al UpdateEventBus para disparar actualizaciones reactivas en la UI.
 */
exports.sendUpdateEvent = onCall(async (request) => {
  // Verificar autenticación
  if (!request.auth) {
    throw new Error('Must be authenticated to send events');
  }

  const { userIds, eventType, data: eventData } = request.data;

  // Validar parámetros
  if (!userIds || !Array.isArray(userIds) || userIds.length === 0) {
    throw new Error('userIds must be a non-empty array');
  }

  if (!eventType || typeof eventType !== 'string') {
    throw new Error('eventType is required and must be a string');
  }

  try {
    // Obtener tokens FCM de todos los usuarios
    const tokensPromises = userIds.map(async (userId) => {
      try {
        const userDoc = await admin.firestore()
          .collection('users')
          .doc(userId)
          .get();
        
        if (!userDoc.exists) {
          console.warn(`User ${userId} does not exist`);
          return null;
        }
        
        return userDoc.data()?.fcmToken || null;
      } catch (error) {
        console.error(`Error fetching token for user ${userId}:`, error);
        return null;
      }
    });

    const tokens = (await Promise.all(tokensPromises))
      .filter(token => token != null && token !== '' && typeof token === 'string' && token.length > 0);

    if (tokens.length === 0) {
      console.log('No FCM tokens found for users:', userIds);
      return { 
        success: true, 
        sent: 0, 
        failed: 0,
        message: 'No tokens available' 
      };
    }

    console.log(`Sending event ${eventType} to ${tokens.length} tokens`);

    // Preparar mensaje FCM con payload de data
    const messageData = {
      eventType: eventType,
    };

    // Convertir todos los valores a strings (requerido por FCM data messages)
    if (eventData) {
      Object.keys(eventData).forEach(key => {
        const value = eventData[key];
        if (value != null) {
          messageData[key] = String(value);
        }
      });
    }

    // Generar título y cuerpo de notificación según el tipo de evento
    const notificationContent = getNotificationContent(eventType, eventData);

    // Enviar mensajes individualmente usando sendEach
    const messages = tokens.map(token => ({
      token: token,
      notification: notificationContent,
      data: messageData,
      android: {
        priority: 'high',
        notification: {
          channelId: 'high_importance_channel',
          priority: 'high',
          sound: 'default'
        }
      }
    }));

    const response = await admin.messaging().sendEach(messages);

    // Log de resultados
    console.log(`Event: ${eventType} | Sent: ${response.successCount}/${tokens.length}`);
    
    if (response.failureCount > 0) {
      console.log(`Failed to send ${response.failureCount} messages`);
      response.responses.forEach((resp, idx) => {
        if (!resp.success) {
          const errorCode = resp.error?.code || 'unknown';
          const errorMsg = resp.error?.message || 'Unknown error';
          console.error(`Token ${idx} failed: ${errorCode} - ${errorMsg}`);
          
          // Si el token es inválido, limpiarlo de Firestore
          if (errorCode === 'messaging/invalid-registration-token' ||
              errorCode === 'messaging/registration-token-not-registered' ||
              errorCode === 'messaging/invalid-argument') {
            const invalidToken = tokens[idx];
            console.log(`Cleaning invalid token: ${invalidToken.substring(0, 20)}...`);
            
            // Buscar y limpiar el token inválido (async sin await)
            admin.firestore()
              .collection('users')
              .where('fcmToken', '==', invalidToken)
              .get()
              .then(snapshot => {
                snapshot.forEach(doc => {
                  console.log(`Removing token from user: ${doc.id}`);
                  doc.ref.update({ fcmToken: admin.firestore.FieldValue.delete() });
                });
              })
              .catch(err => console.error('Error cleaning invalid token:', err));
          }
        }
      });
    }

    return {
      success: true,
      sent: response.successCount,
      failed: response.failureCount,
      eventType: eventType
    };
  } catch (error) {
    console.error('Error sending update event:', error);
    throw new functions.https.HttpsError(
      'internal', 
      `Failed to send event: ${error.message}`
    );
  }
});

/**
 * Trigger de Firestore: Cuando se crea un nuevo mensaje de chat
 * Envía notificación a todos los miembros del chat excepto al remitente
 */
exports.onChatMessageCreated = onDocumentCreated(
  'tasks/{taskId}/messages/{messageId}',
  async (event) => {
    const message = event.data.data();
    const { taskId } = event.params;
    const senderId = message.senderId;
    const senderName = message.senderName || 'Someone';
    const messageText = message.text || 'Sent a message';

    try {
      // Obtener información de la tarea
      const taskDoc = await admin.firestore()
        .collection('tasks')
        .doc(taskId)
        .get();

      if (!taskDoc.exists) {
        console.log('Task not found:', taskId);
        return null;
      }

      const task = taskDoc.data();
      const taskTitle = task.title || 'Task';
      
      // Obtener todos los miembros (creador + asignados)
      const memberIds = [
        task.creatorId,
        ...(task.assignedUserIds || []),
        ...(task.memberIds || [])
      ];
      const uniqueMemberIds = [...new Set(memberIds)]
        .filter(id => id && id !== senderId);

      if (uniqueMemberIds.length === 0) {
        console.log('No members to notify');
        return null;
      }

      // Obtener tokens FCM
      const tokensSnapshot = await admin.firestore()
        .collection('users')
        .where(admin.firestore.FieldPath.documentId(), 'in', uniqueMemberIds.slice(0, 10))
        .get();

      const tokens = tokensSnapshot.docs
        .map(doc => doc.data().fcmToken)
        .filter(token => token);

      if (tokens.length === 0) {
        console.log('No FCM tokens found');
        return null;
      }

      // Enviar notificación con sonido (notification + data)
      const chatMessages = tokens.map(token => ({
        token: token,
        notification: {
          title: `${senderName} in ${taskTitle}`,
          body: messageText.substring(0, 100)
        },
        data: {
          eventType: 'MESSAGE_RECEIVED',
          taskId: taskId,
          messageId: event.data.id
        },
        android: {
          priority: 'high',
          notification: {
            channelId: 'chat_messages',
            priority: 'high',
            sound: 'default'
          }
        }
      }));
      
      await admin.messaging().sendEach(chatMessages);

      console.log(`Chat notification sent to ${tokens.length} users`);
      return null;
    } catch (error) {
      console.error('Error sending chat notification:', error);
      return null;
    }
  });

/**
 * Trigger de Firestore: Cuando un usuario actualiza su perfil
 * Envía evento de actualización a todos sus contactos/grupos
 */
exports.onUserProfileUpdated = onDocumentUpdated(
  'users/{userId}',
  async (event) => {
    const before = event.data.before.data();
    const after = event.data.after.data();
    const { userId } = event.params;

    // Solo procesar si cambió el nombre o la foto de perfil
    if (before.name === after.name && 
        before.profilePictureUrl === after.profilePictureUrl) {
      return null;
    }

    try {
      // Encontrar todas las tareas donde el usuario es miembro
      const tasksSnapshot = await admin.firestore()
        .collection('tasks')
        .where('memberIds', 'array-contains', userId)
        .get();

      if (tasksSnapshot.empty) {
        console.log('User not in any tasks');
        return null;
      }

      // Recolectar IDs únicos de todos los miembros de esas tareas
      const allMemberIds = new Set();
      tasksSnapshot.docs.forEach(doc => {
        const task = doc.data();
        [task.creatorId, ...(task.assignedUserIds || []), ...(task.memberIds || [])]
          .forEach(id => {
            if (id && id !== userId) allMemberIds.add(id);
          });
      });

      if (allMemberIds.size === 0) {
        return null;
      }

      // Obtener tokens en batches (Firestore limit: 10 items en 'in' query)
      const memberIdArray = Array.from(allMemberIds);
      const batchSize = 10;
      const tokens = [];

      for (let i = 0; i < memberIdArray.length; i += batchSize) {
        const batch = memberIdArray.slice(i, i + batchSize);
        const snapshot = await admin.firestore()
          .collection('users')
          .where(admin.firestore.FieldPath.documentId(), 'in', batch)
          .get();
        
        snapshot.docs.forEach(doc => {
          const token = doc.data().fcmToken;
          if (token) tokens.push(token);
        });
      }

      if (tokens.length === 0) {
        return null;
      }

      // Enviar evento silencioso de actualización
      const profileMessages = tokens.map(token => ({
        token: token,
        data: {
          eventType: 'USER_PROFILE_UPDATED',
          userId: userId
        }
      }));
      
      await admin.messaging().sendEach(profileMessages);

      console.log(`Profile update event sent to ${tokens.length} users`);
      return null;
    } catch (error) {
      console.error('Error sending profile update event:', error);
      return null;
    }
  });

/**
 * Función programada: Limpiar tokens FCM inválidos semanalmente
 * Elimina tokens de usuarios que no han estado activos en 90 días
 */
exports.cleanupInactiveTokens = onSchedule(
  {
    schedule: 'every sunday 03:00',
    timeZone: 'America/Mexico_City'
  },
  async (event) => {
    const ninetyDaysAgo = Date.now() - (90 * 24 * 60 * 60 * 1000);
    
    try {
      const snapshot = await admin.firestore()
        .collection('users')
        .where('lastActive', '<', new Date(ninetyDaysAgo))
        .where('fcmToken', '!=', null)
        .get();

      const batch = admin.firestore().batch();
      let count = 0;

      snapshot.docs.forEach(doc => {
        batch.update(doc.ref, {
          fcmToken: admin.firestore.FieldValue.delete()
        });
        count++;
      });

      if (count > 0) {
        await batch.commit();
        console.log(`Cleaned ${count} inactive FCM tokens`);
      }

      return null;
    } catch (error) {
      console.error('Error cleaning tokens:', error);
      return null;
    }
  });
