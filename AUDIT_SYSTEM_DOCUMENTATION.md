# Sistema de Auditoría Empresarial

## Descripción

Sistema de logging estructurado para cumplimiento normativo, debugging avanzado y análisis en planes Enterprise.

## Arquitectura

### Componentes

1. **AuditEvent**: Modelo de datos para eventos auditables
2. **AuditLogger**: Servicio central de logging
3. **OperationResult**: Resultado de operaciones multi-paso con trail completo

### Flujo de Datos

```
Operación → AuditEvents → AuditLogger → [Logcat + Firestore]
                                              ↓
                                    Reportes/Analytics
```

## Características

### 1. Logging Estructurado

Cada evento captura:
- **Timestamp**: Marca temporal precisa
- **UserId**: Quién realizó la acción
- **Action**: Tipo de operación (GROUP_DELETE, TASK_DELETE, etc.)
- **Resource**: Tipo de recurso afectado
- **Status**: SUCCESS, FAILED, PARTIAL_SUCCESS, PERMISSION_DENIED, SKIPPED
- **Metadata**: Contexto adicional (IDs, contadores, etc.)
- **ErrorDetails**: Información detallada de errores

### 2. Operaciones Multi-Paso

`OperationResult` permite:
- Tracking de múltiples sub-operaciones
- Identificación de fallos parciales
- Diferenciación entre fallos críticos y no críticos
- Resumen ejecutivo de la operación

### 3. Niveles de Logging

- **INFO**: Operaciones exitosas
- **WARN**: Fallos no críticos (PERMISSION_DENIED, PARTIAL_SUCCESS)
- **ERROR**: Fallos críticos
- **DEBUG**: Operaciones omitidas (SKIPPED)

### 4. Almacenamiento Dual

- **Logcat**: Inmediato, para debugging en desarrollo
- **Firestore**: Persistente, para reportes y compliance (solo Enterprise)

## Ejemplo de Uso

### Operación Simple

```kotlin
auditLogger.log(AuditEvent(
    userId = userId,
    action = AuditAction.GROUP_LEAVE,
    resource = AuditResource.GROUP,
    resourceId = groupId,
    status = AuditStatus.SUCCESS,
    metadata = mapOf("remainingMembers" to 5)
))
```

### Operación Compleja

```kotlin
private suspend fun deleteGroupInternal(groupId: String): OperationResult {
    val auditEvents = mutableListOf<AuditEvent>()
    
    // Operación 1: Eliminar chat
    val chatEvent = try {
        deleteChatThread(groupId)
        AuditEvent(/* success */)
    } catch (e: Exception) {
        AuditEvent(/* failed with details */)
    }
    auditEvents.add(chatEvent)
    
    // Operación 2: Eliminar tareas (continúa si falla)
    // Operación 3: Eliminar grupo (CRÍTICA)
    
    val result = OperationResult(
        success = criticalOperationSuccess,
        events = auditEvents,
        summary = OperationSummary(...)
    )
    
    auditLogger.logOperationResult(result)
    return result
}
```

## Salida de Logs

### Ejemplo de Log Exitoso

```
[AuditLogger] === Operation Summary ===
Total: 5
Success: 4
Failed: 1
Skipped: 0
Critical Success: true

⚠️ Partial failures detected:
  - CHAT_DELETE on CHAT_THREAD: PERMISSION_DENIED: Missing or insufficient permissions.
```

### Ejemplo de Evento Individual

```
[AuditLogger] [1702345678-1234] GROUP_DELETE on GROUP(bTpSfH20uqd5D7v1si2P) by user geG3IBQONCRxQuRHD0WFNbBhFc33 -> SUCCESS | metadata: {companyId=xyz123, tasksAffected=5}
```

## Seguridad en Firestore

### Reglas Recomendadas

```javascript
match /auditLogs/{logId} {
  // Solo administradores pueden leer logs
  allow read: if isCompanyAdmin(request.auth.uid);
  
  // El sistema escribe automáticamente (auth service account)
  allow write: if false; // Solo backend puede escribir
}

function isCompanyAdmin(userId) {
  let user = get(/databases/$(database)/documents/users/$(userId));
  return user.data.role == 'admin';
}
```

## Ventajas

### Para Desarrollo
- Debugging preciso con contexto completo
- Identificación rápida de fallos parciales
- Trazabilidad de operaciones complejas

### Para Enterprise
- **Compliance**: Registro de todas las operaciones críticas
- **Auditoría**: Trail completo con timestamps y usuarios
- **Analytics**: Análisis de patrones de uso y errores
- **Reportes**: Datos estructurados para dashboards

### Para Usuarios
- Transparencia en operaciones
- Mejor manejo de errores
- Recuperación automática de fallos no críticos

## Mejores Prácticas

1. **Siempre loguea operaciones críticas** (eliminar, modificar permisos)
2. **Usa PARTIAL_SUCCESS** cuando algunas sub-operaciones fallan
3. **Incluye metadata relevante** (IDs, contadores, estado anterior)
4. **Diferencia entre errores recuperables y no recuperables**
5. **No bloquees la operación principal** por fallos en logging
6. **Usa PERMISSION_DENIED** específicamente para errores de permisos

## Roadmap

### Fase 1 (Actual)
- ✅ Modelo de datos
- ✅ Logger con salida a logcat
- ✅ Integración con operaciones de grupos

### Fase 2
- [ ] Almacenamiento en Firestore (solo Enterprise)
- [ ] Dashboard de auditoría en Admin Panel
- [ ] Filtros y búsqueda de eventos

### Fase 3
- [ ] Alertas automáticas por patrones anómalos
- [ ] Exportación de reportes (CSV, PDF)
- [ ] Retención y archivado de logs

### Fase 4
- [ ] Machine Learning para detección de anomalías
- [ ] Integración con sistemas externos (SIEM)
- [ ] Análisis predictivo
