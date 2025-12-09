# ✅ Checklist de Despliegue - Cloud Functions

## Pre-Despliegue

### Configuración Inicial
- [ ] Node.js instalado (versión 18 o superior)
- [ ] Firebase CLI instalado (`npm install -g firebase-tools`)
- [ ] Autenticado en Firebase (`firebase login`)
- [ ] Plan Blaze habilitado en Firebase Console

### Configuración del Proyecto
- [ ] Archivo `.firebaserc` actualizado con tu project-id
  ```bash
  # Ver proyectos disponibles
  firebase projects:list
  
  # O usar comando interactivo
  firebase use --add
  ```

- [ ] Archivo `google-services.json` actualizado en `app/`
- [ ] Dependencias instaladas (`cd functions && npm install`)

## Despliegue

### Primera Vez
- [ ] Verificar proyecto activo: `firebase projects:list`
- [ ] Desplegar funciones: `firebase deploy --only functions`
- [ ] Verificar en Firebase Console que aparezcan las 4 funciones
- [ ] Revisar logs iniciales: `firebase functions:log`

### Funciones Desplegadas
Deberías ver estas 4 funciones en Firebase Console:

- [ ] ✅ **sendUpdateEvent** (HTTPS Callable)
- [ ] ✅ **onChatMessageCreated** (Firestore Trigger)
- [ ] ✅ **onUserProfileUpdated** (Firestore Trigger)
- [ ] ✅ **cleanupInactiveTokens** (Scheduled)

## Testing

### Verificación en la App
- [ ] Compilar app: `./gradlew assembleDebug`
- [ ] Instalar en dispositivo: `./gradlew installDebug`
- [ ] Usuario autenticado en la app
- [ ] Permisos de notificaciones habilitados

### Probar Eventos
- [ ] Crear una tarea nueva → Verificar evento TASK_CREATED
- [ ] Agregar subtarea → Verificar evento SUBTASK_CREATED
- [ ] Cambiar estado de subtarea → Verificar evento SUBTASK_STATUS_CHANGED
- [ ] Enviar mensaje de chat → Verificar notificación
- [ ] Actualizar perfil → Verificar evento USER_PROFILE_UPDATED

### Verificar Logs
```bash
# Logs en tiempo real de función principal
firebase functions:log --only sendUpdateEvent

# Ver últimos 100 logs
firebase functions:log --lines 100

# Filtrar solo errores
firebase functions:log --filter error
```

Buscar en logs:
- [ ] "Event: TASK_UPDATED | Sent: X/Y" (éxito)
- [ ] No hay errores de autenticación
- [ ] No hay "No FCM tokens found" (si sí hay, usuarios no tienen tokens)

## Post-Despliegue

### Monitoreo Inicial (Primeros 3 días)
- [ ] Revisar métricas diarias en Firebase Console → Functions
- [ ] Verificar tasa de errores < 5%
- [ ] Confirmar que eventos llegan a usuarios
- [ ] Revisar tiempo de respuesta promedio < 2s

### Optimización
- [ ] Identificar eventos más frecuentes
- [ ] Verificar si hay tokens inválidos recurrentes
- [ ] Ajustar logs si hay demasiado ruido
- [ ] Considerar índices de Firestore si hay queries lentas

### Alertas (Recomendado)
En Firebase Console → Alerts, configurar:
- [ ] Alerta de tasa de errores > 10%
- [ ] Alerta de invocaciones inusuales
- [ ] Alerta de tiempo de ejecución > 10s

## Troubleshooting

### Si algo falla

#### Error: "No project active"
```bash
firebase use --add
# Selecciona tu proyecto
```

#### Error: "Billing account not configured"
1. Ve a Firebase Console
2. Settings → Usage and billing
3. Habilita plan Blaze (tiene capa gratuita)

#### Error: "Permission denied"
```bash
firebase login --reauth
```

#### Eventos no llegan a la app
1. [ ] Verificar que usuario esté autenticado
2. [ ] Verificar permisos de notificaciones en dispositivo
3. [ ] Revisar Logcat: buscar "FCM token"
4. [ ] Verificar en Firestore que usuarios tengan campo `fcmToken`
5. [ ] Revisar logs de función: `firebase functions:log --only sendUpdateEvent`

#### Notificaciones duplicadas
- [ ] Verificar que no haya múltiples llamadas a `sendUpdateEvent()`
- [ ] Revisar que no haya listeners duplicados en la app

#### Función muy lenta
1. [ ] Revisar métricas en Firebase Console
2. [ ] Verificar tamaño de userIds (max recomendado: 100)
3. [ ] Considerar batching para grupos grandes

## Mantenimiento

### Semanal
- [ ] Revisar métricas de uso en Firebase Console
- [ ] Verificar que cleanupInactiveTokens está corriendo
- [ ] Revisar logs de errores

### Mensual
- [ ] Revisar costos en Billing
- [ ] Analizar patrones de uso
- [ ] Optimizar si es necesario

## Rollback (Si necesitas revertir)

### Rollback de funciones
```bash
# Ver versiones anteriores
firebase functions:list

# Rollback a versión anterior (si algo salió mal)
# No hay comando directo, necesitas re-desplegar código anterior
```

### Plan de contingencia
1. [ ] Tener copia del código anterior
2. [ ] Documentar cambios realizados
3. [ ] Tener acceso a logs antes del cambio

## Comandos Rápidos

```bash
# Ver estado
firebase projects:list
firebase functions:list

# Desplegar
firebase deploy --only functions:sendUpdateEvent  # Solo una función
firebase deploy --only functions                   # Todas

# Logs
firebase functions:log --only sendUpdateEvent
firebase functions:log --filter error
firebase functions:log --since 1h

# Eliminar función
firebase functions:delete nombreFuncion

# Testing local (opcional)
firebase emulators:start
```

## Notas Importantes

⚠️ **Plan Blaze Requerido**
- Necesitas habilitar facturación
- Capa gratuita muy generosa: 2M invocaciones/mes
- Costo estimado real: $0-3 USD/mes

⚠️ **Tokens FCM**
- Usuarios deben tener campo `fcmToken` en Firestore
- Se guarda automáticamente en `MyFirebaseMessagingService`
- Tokens inválidos se limpian automáticamente

⚠️ **Límites de FCM**
- Max 500 tokens por mensaje multicast
- Tu código ya maneja esto correctamente

## ✅ Despliegue Exitoso Cuando...

1. ✅ Firebase Console muestra 4 funciones activas
2. ✅ Logs muestran eventos siendo enviados
3. ✅ App recibe actualizaciones en tiempo real
4. ✅ No hay errores en logs por > 1 hora
5. ✅ Tasa de éxito > 95%

## 🎉 Listo para Producción

Una vez que todos los checks pasen, tu sistema de eventos reactivos está **100% funcional** en producción.

**Última actualización:** Diciembre 2025
