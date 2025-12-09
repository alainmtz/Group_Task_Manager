# Firebase Functions - Agenda Colaborativa

## Funciones Implementadas

### 1. `sendUpdateEvent` (Callable Function)

Función principal para enviar eventos de actualización vía FCM a múltiples usuarios.

**Uso desde Android:**

```kotlin

notificationService.sendUpdateEvent(
    userIds = listOf("userId1", "userId2"),
    eventType = "TASK_UPDATED",
    data = mapOf("taskId" to "123", "status" to "completed")
)
```

**Eventos soportados:**

- TASK_CREATED, TASK_UPDATED, TASK_DELETED, TASK_STATUS_CHANGED, TASK_ASSIGNMENT_CHANGED
- SUBTASK_CREATED, SUBTASK_UPDATED, SUBTASK_DELETED, SUBTASK_STATUS_CHANGED
- SUBTASK_COMPLETION_APPROVED, SUBTASK_COMPLETION_REJECTED
- BUDGET_DISTRIBUTED, BUDGET_STATUS_CHANGED, EARNING_STATUS_CHANGED
- BID_PLACED, BID_ACCEPTED, BID_REJECTED
- GROUP_CREATED, GROUP_UPDATED, GROUP_DELETED, GROUP_MEMBER_ADDED, GROUP_MEMBER_REMOVED
- MESSAGE_RECEIVED, CHAT_THREAD_UPDATED
- USER_PROFILE_UPDATED

### 2. `onChatMessageCreated` (Firestore Trigger)

Se activa automáticamente cuando se crea un mensaje en `tasks/{taskId}/messages/{messageId}`.

Envía notificación push con sonido a todos los miembros del chat excepto el remitente.

### 3. `onUserProfileUpdated` (Firestore Trigger)

Se activa cuando un usuario actualiza su perfil (nombre o foto).

Envía evento silencioso a todos los usuarios que comparten tareas con él.

### 4. `cleanupInactiveTokens` (Scheduled Function)

Ejecuta cada domingo a las 3 AM (Mexico City timezone).

Limpia tokens FCM de usuarios inactivos por más de 90 días.

## Instalación

### 1. Instalar Node.js

Asegúrate de tener Node.js 18 o superior instalado.

### 2. Instalar Firebase CLI

```bash
npm install -g firebase-tools
```

### 3. Login a Firebase

```bash
firebase login
```

### 4. Inicializar proyecto (si es necesario)

```bash
firebase init

```

Selecciona:

- Functions: Configure Cloud Functions
- Firestore: Configure security rules and indexes files
- Storage: Configure security rules file
- Emulators: Set up local emulators

### 5. Instalar dependencias

```bash
cd functions
npm install
```

## Despliegue

### Desplegar todas las funciones

```bash
firebase deploy --only functions
```

### Desplegar solo una función específica

```bash
firebase deploy --only functions:sendUpdateEvent
```

### Ver logs en tiempo real

```bash
firebase functions:log
```

## Testing Local con Emulators

### Iniciar emuladores

```bash

firebase emulators:start
```

Esto iniciará:

- Functions en http://localhost:5001
- Firestore en http://localhost:8080
- Storage en http://localhost:9199
- UI de emuladores en http://localhost:4000

### Configurar app para usar emuladores (desarrollo)

En tu app Android, agrega en `build.gradle.kts`:

```kotlin
android {
    buildTypes {
        debug {
            buildConfigField("Boolean", "USE_EMULATORS", "true")
        }
        release {
            buildConfigField("Boolean", "USE_EMULATORS", "false")
        }
    }
}
```

En tu código de inicialización:

```kotlin

if (BuildConfig.USE_EMULATORS) {
    Firebase.functions.useEmulator("10.0.2.2", 5001) // Android emulator
    Firebase.firestore.useEmulator("10.0.2.2", 8080)
}
```

## Monitoreo

### Ver métricas en Firebase Console

1. Ve a Firebase Console → Functions
2. Revisa invocaciones, errores y tiempo de ejecución
3. Configura alertas para errores críticos

### Logs estructurados

Los logs incluyen información detallada:

- Número de mensajes enviados/fallidos
- Tokens inválidos detectados
- Errores con código y mensaje

## Costos Estimados

Firebase Functions pricing (plan Blaze - pay as you go):

- **Invocaciones gratuitas**: 2 millones/mes

- **Tiempo de cómputo gratuito**: 400,000 GB-segundos/mes
- **Transferencia de red gratuita**: 5 GB/mes

Para una app con 1000 usuarios activos:

- ~100,000 eventos/día = 3M eventos/mes

- Costo estimado: **$0-5 USD/mes**

## Seguridad

### Autenticación requerida

Todas las funciones callable requieren que el usuario esté autenticado.

### Validación de parámetros

Se validan todos los inputs antes de procesar.

### Limpieza automática

Tokens inválidos son eliminados automáticamente para mantener la base de datos limpia.

## Troubleshooting

### Error: "unauthenticated"

- Verifica que el usuario esté autenticado antes de llamar la función
- Asegúrate de que el token de autenticación sea válido

### Error: "No tokens available"

- Los usuarios no tienen tokens FCM registrados
- Verifica que `MyFirebaseMessagingService` esté guardando tokens correctamente

### Mensajes no llegan

1. Verifica logs: `firebase functions:log`
2. Revisa que los tokens FCM sean válidos
3. Verifica que el cliente tenga permisos de notificaciones
4. Asegúrate de que la app esté registrada en Firebase Console

### Testing local no funciona

- Verifica que los emuladores estén corriendo
- Asegúrate de usar la IP correcta (10.0.2.2 para emulador Android)
- Revisa que `google-services.json` esté actualizado
