# 🚀 Guía Rápida de Despliegue

## Requisitos Previos

1. **Firebase CLI instalado**
   ```bash
   npm install -g firebase-tools
   ```

2. **Autenticado en Firebase**
   ```bash
   firebase login
   ```

3. **Proyecto configurado**
   - Asegúrate de estar en el proyecto correcto:
   ```bash
   firebase projects:list
   firebase use --add  # Selecciona tu proyecto
   ```

## Despliegue Rápido

### Primera vez (Completo)
```bash
cd /home/alain/proyectos/kotlin/AgendaColaborativa/functions
npm install
cd ..
firebase deploy --only functions
```

### Solo función principal
```bash
./functions/deploy.sh sendUpdateEvent
```

### Todas las funciones
```bash
./functions/deploy.sh all
```

## Verificar Despliegue

1. **Ver logs en tiempo real**
   ```bash
   firebase functions:log --only sendUpdateEvent
   ```

2. **Consola de Firebase**
   - Abre: https://console.firebase.google.com
   - Ve a: Functions → Dashboard
   - Verifica que las funciones aparezcan activas

3. **Probar desde Android**
   - La app automáticamente usará la función desplegada
   - Revisa los logs en Logcat para ver las llamadas

## Configuración de la App

La configuración ya está lista en `NotificationService.kt`. 

Las funciones se invocan así:

```kotlin
// En cualquier ViewModel
notificationService.sendUpdateEvent(
    userIds = listOf("userId1", "userId2"),
    eventType = "TASK_UPDATED",
    data = mapOf("taskId" to taskId)
)
```

## Solución de Problemas

### Error: "No project active"
```bash
firebase use --add
# Selecciona tu proyecto de la lista
```

### Error: "Billing account not configured"
- Necesitas habilitar el plan Blaze (pay-as-you-go)
- Ve a Firebase Console → Settings → Usage and billing
- Nota: Tiene capa gratuita generosa (2M invocaciones/mes)

### Error: "Permission denied"
```bash
firebase login --reauth
```

### Ver estado del proyecto
```bash
firebase projects:list
firebase functions:list
```

## Testing Local (Opcional)

### Iniciar emuladores
```bash
firebase emulators:start
```

### Configurar app para usar emuladores
En `NotificationService.kt`:
```kotlin
init {
    if (BuildConfig.DEBUG) {
        functions = Firebase.functions
        functions.useEmulator("10.0.2.2", 5001)
    }
}
```

## Costos

**Plan Blaze** (pay-as-you-go con capa gratuita):
- ✅ 2M invocaciones/mes GRATIS
- ✅ 400,000 GB-segundos/mes GRATIS
- ✅ 5GB transferencia/mes GRATIS

**Estimación para tu app:**
- 1000 usuarios activos
- 100K eventos/día
- Costo: **$0-3 USD/mes** ✨

## Próximos Pasos

1. ✅ Desplegar funciones
2. ✅ Probar en app real
3. 📊 Monitorear logs primeros días
4. 🔔 Configurar alertas en Firebase Console
5. 📈 Revisar métricas semanalmente

## Comandos Útiles

```bash
# Ver todas las funciones desplegadas
firebase functions:list

# Ver logs filtrados
firebase functions:log --only sendUpdateEvent --lines 50

# Eliminar una función
firebase functions:delete onChatMessageCreated

# Ver uso/costos
firebase projects:describe
```

## Soporte

- 📖 Docs: https://firebase.google.com/docs/functions
- 💬 Stack Overflow: [firebase-cloud-functions]
- 🐛 Issues: Revisa logs con `firebase functions:log`
