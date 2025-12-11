# Guía de Auditoría y Logs para Planes Enterprise

## Overview

El sistema de auditoría captura automáticamente todas las operaciones críticas realizadas en la aplicación, proporcionando un trail completo para compliance, debugging y análisis.

## Cómo Ver los Logs

### 1. Durante Desarrollo (Logcat)

Los logs se muestran automáticamente en logcat con el tag `AuditLogger`:

```bash
adb logcat | grep "AuditLogger"
```

**Ejemplo de salida:**

``` bash
I/AuditLogger: [1702345678-1234] GROUP_DELETE on GROUP(abc123) by user xyz789 -> SUCCESS
I/AuditLogger: === Operation Summary ===
I/AuditLogger: Total: 5
I/AuditLogger: Success: 4
I/AuditLogger: Failed: 1
W/AuditLogger: [1702345678-5678] CHAT_DELETE on CHAT_THREAD(def456) -> PERMISSION_DENIED
```

### 2. En Producción (Firebase Console)

1. Abrir [Firebase Console](https://console.firebase.google.com)
2. Seleccionar proyecto "agenda-solar"
3. Navegar a **Firestore Database**
4. Abrir colección `auditLogs`

**Estructura de un documento:**

```json
{
  "eventId": "1702345678-1234",
  "timestamp": "2025-12-11T19:15:30Z",
  "userId": "geG3IBQONCRxQuRHD0WFNbBhFc33",
  "action": "GROUP_DELETE",
  "resource": "GROUP",
  "resourceId": "bTpSfH20uqd5D7v1si2P",
  "status": "SUCCESS",
  "metadata": {
    "companyId": "xyz123",
    "tasksAffected": 5
  },
  "errorDetails": null
}
```

### 3. Queries Útiles en Firestore

#### Ver todas las eliminaciones de grupos

```javascript
auditLogs
  .where("action", "==", "GROUP_DELETE")
  .orderBy("timestamp", "desc")
```

#### Ver operaciones de un usuario específico

```javascript
auditLogs
  .where("userId", "==", "USER_ID_HERE")
  .orderBy("timestamp", "desc")
```

#### Ver todos los errores

```javascript
auditLogs
  .where("status", "in", ["FAILED", "PERMISSION_DENIED"])
  .orderBy("timestamp", "desc")
```

#### Ver operaciones de las últimas 24 horas

```javascript
auditLogs
  .where("timestamp", ">", new Date(Date.now() - 86400000))
  .orderBy("timestamp", "desc")
```

## Interpretación de Logs

### Estados (Status)

| Status | Significado | Acción Requerida |
|--------|-------------|------------------|
| `SUCCESS` | Operación completada exitosamente | Ninguna |
| `PARTIAL_SUCCESS` | Algunas sub-operaciones fallaron | Revisar errorDetails |
| `FAILED` | Operación falló completamente | Investigar y posible retry |
| `PERMISSION_DENIED` | Sin permisos suficientes | Esperado en algunos casos, revisar si recurrente |
| `SKIPPED` | Operación omitida (ej: no había chat) | Normal, no requiere acción |

### Acciones (Actions)

| Action | Descripción |
|--------|-------------|
| `GROUP_DELETE` | Eliminación de grupo |
| `GROUP_LEAVE` | Usuario abandona grupo |
| `MEMBER_REMOVE` | Remover miembro de grupo |
| `CHAT_DELETE` | Eliminación de chat thread |
| `TASK_DELETE` | Eliminación de tarea(s) |
| `COUNTER_UPDATE` | Actualización de contadores de company |

### Recursos (Resources)

- `GROUP`: Grupos de trabajo
- `CHAT_THREAD`: Hilos de chat
- `TASK`: Tareas
- `COMPANY_COUNTER`: Contadores a nivel de empresa

## Casos de Uso

### 1. Investigar por qué un grupo no se eliminó

``` text
1. Buscar en auditLogs por resourceId (groupId)
2. Ver el status de GROUP_DELETE
3. Si status == FAILED, revisar errorDetails
4. Verificar si las sub-operaciones (CHAT_DELETE, TASK_DELETE) fallaron
```

### 2. Auditoría de compliance

```text
1. Filtrar por usuario y rango de fechas
2. Exportar resultados
3. Revisar todas las acciones críticas (DELETE, REMOVE)
```

### 3. Análisis de errores recurrentes

```text
1. Buscar status == PERMISSION_DENIED
2. Agrupar por action y resource
3. Si > 10% de operaciones fallan, investigar reglas de Firestore
```

### 4. Dashboard ejecutivo

Métricas clave:

- Total de operaciones por día
- Tasa de éxito vs fallo
- Operaciones críticas (GROUP_DELETE, etc.)
- Usuarios más activos
- Errores por tipo

## Exportación de Datos

### Manual (Firebase Console)

1. Navegar a `auditLogs`
2. Usar filtros para reducir dataset
3. No hay exportación directa, usar script

### Programática (Cloud Functions)

```javascript
// Ejemplo: Exportar logs del último mes
const admin = require('firebase-admin');
const db = admin.firestore();

async function exportAuditLogs() {
  const oneMonthAgo = new Date();
  oneMonthAgo.setMonth(oneMonthAgo.getMonth() - 1);
  
  const snapshot = await db.collection('auditLogs')
    .where('timestamp', '>', oneMonthAgo)
    .get();
  
  const logs = snapshot.docs.map(doc => ({
    id: doc.id,
    ...doc.data()
  }));
  
  // Guardar en Cloud Storage o exportar a CSV
  return logs;
}
```

## Retención de Datos

### Configuración Recomendada

- **Desarrollo**: 30 días (limpieza manual)
- **Producción Free/Pro**: 90 días
- **Producción Enterprise**: 1-3 años

### Limpieza Automática (Cloud Function)

```javascript
// Ejecutar diariamente con Cloud Scheduler
exports.cleanOldAuditLogs = functions.pubsub
  .schedule('0 3 * * *') // 3 AM diario
  .onRun(async (context) => {
    const db = admin.firestore();
    const cutoffDate = new Date();
    cutoffDate.setDate(cutoffDate.getDate() - 90); // 90 días
    
    const snapshot = await db.collection('auditLogs')
      .where('timestamp', '<', cutoffDate)
      .limit(500) // Batch delete
      .get();
    
    const batch = db.batch();
    snapshot.docs.forEach(doc => batch.delete(doc.ref));
    
    await batch.commit();
    console.log(`Deleted ${snapshot.size} old audit logs`);
  });
```

## Seguridad y Permisos

### Quién Puede Ver Logs

Según `firestore.rules`:

```javascript
// Solo admins de la company pueden leer
allow read: if isAuthenticated() && getUserData().role == 'admin';
```

Para dar acceso a un usuario:

```javascript
// En Firestore, actualizar documento de usuario
{
  "role": "admin", // Cambiar de "member" a "admin"
  // ... otros campos
}
```

### Inmutabilidad

Los logs NO pueden ser modificados o eliminados por usuarios:

```javascript
allow update: if false;
allow delete: if false;
```

Solo Cloud Functions con service account pueden eliminar logs antiguos.

## Troubleshooting

### Los logs no aparecen en Firestore

1. Verificar reglas de Firestore (deben permitir `create`)
2. Revisar logcat - si aparecen warnings "Could not store audit log"
3. Verificar conectividad a Firebase
4. El almacenamiento es "best-effort" - no bloquea operaciones

### Demasiados logs generados

1. Considerar sampling (solo loguear 10% de operaciones rutinarias)
2. Implementar diferentes niveles de logging por ambiente
3. Usar Cloud Functions para agregar logs antes de almacenar

### Necesito buscar un evento específico

1. Usar el `eventId` (timestamp-random)
2. O combinar: timestamp + userId + action
3. Índices compuestos recomendados:
   - `userId + timestamp`
   - `action + status + timestamp`
   - `resource + resourceId`

## Roadmap de Mejoras

1. **Dashboard Web Admin** (Q1 2026)
   - Visualización gráfica de métricas
   - Filtros avanzados
   - Exportación a PDF/CSV

2. **Alertas Automáticas** (Q2 2026)
   - Notificar cuando tasa de fallos > threshold
   - Alertas por acciones sospechosas

3. **Machine Learning** (Q3 2026)
   - Detección de patrones anómalos
   - Predicción de problemas

## Soporte

Para consultas sobre el sistema de auditoría:

- **Desarrollo**: Revisar `AUDIT_SYSTEM_DOCUMENTATION.md`
- **Producción**: Contactar equipo de DevOps
- **Compliance**: Contactar equipo legal/compliance
