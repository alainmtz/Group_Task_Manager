# Cloud Function para Eventos FCM

Esta Cloud Function debe desplegarse en Firebase Functions para enviar mensajes FCM silenciosos que disparen actualizaciones de UI.

## Archivo: functions/index.js

```javascript
const functions = require('firebase-functions');
const admin = require('firebase-admin');

admin.initializeApp();

exports.sendUpdateEvent = functions.https.onCall(async (data, context) => {
  // Verify authentication
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'Must be authenticated');
  }

  const { userIds, eventType, data: eventData } = data;

  if (!userIds || !Array.isArray(userIds) || userIds.length === 0) {
    throw new functions.https.HttpsError('invalid-argument', 'userIds must be a non-empty array');
  }

  if (!eventType) {
    throw new functions.https.HttpsError('invalid-argument', 'eventType is required');
  }

  try {
    // Get FCM tokens for all users
    const tokensPromises = userIds.map(async (userId) => {
      const userDoc = await admin.firestore().collection('users').doc(userId).get();
      return userDoc.data()?.fcmToken;
    });

    const tokens = (await Promise.all(tokensPromises)).filter(token => token != null);

    if (tokens.length === 0) {
      console.log('No FCM tokens found for users:', userIds);
      return { success: true, sent: 0 };
    }

    // Prepare FCM message with data payload (silent notification)
    const message = {
      data: {
        eventType: eventType,
        ...eventData
      },
      tokens: tokens
    };

    // Send multicast message
    const response = await admin.messaging().sendMulticast(message);

    console.log(`Successfully sent ${response.successCount} messages`);
    if (response.failureCount > 0) {
      console.log(`Failed to send ${response.failureCount} messages`);
      response.responses.forEach((resp, idx) => {
        if (!resp.success) {
          console.error(`Error sending to token ${tokens[idx]}:`, resp.error);
        }
      });
    }

    return {
      success: true,
      sent: response.successCount,
      failed: response.failureCount
    };
  } catch (error) {
    console.error('Error sending update event:', error);
    throw new functions.https.HttpsError('internal', error.message);
  }
});
```

## Instrucciones de Despliegue

### 1. Inicializar Firebase Functions (si no está inicializado)

```bash
cd /home/alain/proyectos/kotlin/AgendaColaborativa
firebase init functions
```

**Selecciona**:

- Lenguaje: JavaScript
- Instalar dependencias: Yes

### 2. Instalar dependencias necesarias

```bash
cd functions
npm install firebase-admin firebase-functions
```

### 3. Copiar el código de la función

Copia el código de arriba al archivo `functions/index.js`

### 4. Desplegar la función

```bash
firebase deploy --only functions:sendUpdateEvent
```

### 5. Verificar en Firebase Console

Ve a Firebase Console → Functions para verificar que la función se desplegó correctamente.

## Alternativa Sin Cloud Function (Usando Firestore Triggers)

Si no quieres usar Cloud Functions, puedes usar un enfoque alternativo con **Firestore Triggers locales** o simplemente **notificaciones en base de datos**:

### Opción 2A: Collection "events" con listeners

Crea una collection `events` donde escribes eventos y cada cliente escucha cambios:

```kotlin
// En lugar de sendUpdateEvent(), escribe en Firestore:
val event = hashMapOf(
    "type" to eventType,
    "userIds" to userIds,
    "data" to data,
    "timestamp" to FieldValue.serverTimestamp()
)
db.collection("events").add(event)

// En cada app, escucha eventos:
db.collection("events")
    .whereArrayContains("userIds", currentUserId)
    .orderBy("timestamp", Query.Direction.DESCENDING)
    .limit(10)
    .addSnapshotListener { snapshot, error ->
        snapshot?.documentChanges?.forEach { change ->
            if (change.type == DocumentChange.Type.ADDED) {
                val event = change.document
                val eventType = event.getString("type")
                // Trigger UI update
            }
        }
    }
```

### Opción 2B: Campo "lastUpdate" en Task/User documents

Actualiza un timestamp cuando cambian estados:

```kotlin
// Al cambiar estado:
tasksCollection.document(taskId).update("lastUpdate", FieldValue.serverTimestamp())

// En cada pantalla, observa el campo:
getTaskFlow(taskId).collect { task ->
    // Se recolecta automáticamente cuando lastUpdate cambia
}
```

## Recomendación

Para tu caso, **Opción 2B** es la más simple y eficiente:

- No requiere Cloud Function (sin costos adicionales)
- No requiere listeners adicionales (usas los que ya tienes)
- Firestore ya notifica cambios automáticamente
- Solo necesitas actualizar un timestamp cuando hay cambios

Ya tienes flows con `.snapshots()` que se actualizan automáticamente cuando Firestore detecta cambios.
