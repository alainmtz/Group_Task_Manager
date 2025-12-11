# Optimización de Costos - Sistema de Auditoría

## 💰 Análisis de Costos Actual

### Tamaño de Datos
- **Por evento de auditoría**: ~250-300 bytes
- **Incluye**: métricas, contexto, metadata, error details

### Costos Firebase (Plan Blaze)
```
Escrituras: $0.18 USD / 100,000 operaciones
Lecturas: $0.06 USD / 100,000 operaciones
Almacenamiento: $0.18 USD / GB / mes
```

### Proyección por Escenario

#### Startup (50 usuarios)
- **Operaciones/mes**: 7,500
- **Costo**: ~$0.03/mes
- **Incremento vs sin auditoría**: +0.6%
- ✅ **DESPRECIABLE**

#### Mediana (500 usuarios)
- **Operaciones/mes**: 150,000
- **Costo**: ~$0.52/mes
- **Incremento**: +2.1%
- ✅ **MÍNIMO**

#### Enterprise (5,000 usuarios)
- **Operaciones/mes**: 2,250,000
- **Costo**: ~$8/mes
- **Costo por usuario**: $0.0016
- **Incremento**: +5.3%
- ✅ **JUSTIFICADO** (compliance + analytics)

#### Escala Masiva (50,000 usuarios)
- **Operaciones/mes**: 22,500,000
- **Costo**: ~$80/mes
- **Incremento**: +6.7%
- ⚠️ **CONSIDERAR OPTIMIZACIONES**

---

## 🎯 Optimizaciones para Implementar

### 1. Logging Selectivo por Plan de Empresa

**Cuándo**: Cuando tengas >1,000 usuarios activos

**Implementación**:
```kotlin
// En AuditLogger.kt
private fun shouldStoreInFirestore(event: AuditEvent, companyPlan: String?): Boolean {
    return when (companyPlan) {
        "FREE" -> {
            // Solo operaciones críticas
            event.action in listOf(
                AuditAction.GROUP_DELETE,
                AuditAction.MEMBER_REMOVE
            ) && event.status == AuditStatus.FAILED
        }
        
        "PRO" -> {
            // Operaciones críticas + errores
            event.action in listOf(
                AuditAction.GROUP_DELETE,
                AuditAction.MEMBER_REMOVE,
                AuditAction.COUNTER_UPDATE
            ) || event.status in listOf(
                AuditStatus.FAILED,
                AuditStatus.PERMISSION_DENIED
            )
        }
        
        "BUSINESS" -> {
            // Todo excepto operaciones muy frecuentes
            event.action != AuditAction.TASK_DELETE || 
            event.status != AuditStatus.SUCCESS
        }
        
        "ENTERPRISE" -> true  // Todo
        
        else -> false  // Default: solo logcat
    }
}

// Modificar storeInFirestore()
private fun storeInFirestore(event: AuditEvent) {
    val companyPlan = event.context?.companyPlan
    
    if (!shouldStoreInFirestore(event, companyPlan)) {
        return  // Solo log en logcat
    }
    
    try {
        // ... código actual de almacenamiento
    } catch (e: Exception) {
        Log.w(TAG, "Could not store audit log in Firestore: ${e.message}")
    }
}
```

**Ahorro estimado**: 60-70% en planes FREE/PRO

---

### 2. Sampling para Operaciones Rutinarias

**Cuándo**: Cuando superes 100,000 operaciones/mes

**Implementación**:
```kotlin
// En AuditLogger.kt
private fun shouldSample(event: AuditEvent): Boolean {
    // Siempre loguear operaciones críticas y errores
    if (event.status in listOf(AuditStatus.FAILED, AuditStatus.PERMISSION_DENIED)) {
        return true
    }
    
    if (event.action in listOf(
        AuditAction.GROUP_DELETE,
        AuditAction.MEMBER_REMOVE,
        AuditAction.COUNTER_UPDATE
    )) {
        return true
    }
    
    // Sampling para operaciones rutinarias exitosas
    return when (event.action) {
        AuditAction.TASK_DELETE -> Math.random() < 0.1    // 10%
        AuditAction.CHAT_DELETE -> Math.random() < 0.05   // 5%
        AuditAction.GROUP_LEAVE -> Math.random() < 0.2    // 20%
        AuditAction.MEMBER_ADD -> Math.random() < 0.3     // 30%
        else -> true
    }
}

// Modificar log()
fun log(event: AuditEvent) {
    // Siempre log a logcat
    logToLogcat(event)
    
    // Sampling para Firestore
    if (shouldSample(event)) {
        storeInFirestore(event)
    }
}
```

**Ahorro estimado**: 40-50% en volumen total

---

### 3. Retención Automática por Plan

**Cuándo**: Implementar desde el inicio para controlar almacenamiento

**Implementación**:
```javascript
// En functions/index.js
const admin = require('firebase-admin');
const functions = require('firebase-functions');

exports.cleanOldAuditLogs = functions.pubsub
  .schedule('0 2 * * 0')  // Domingos a las 2 AM
  .timeZone('America/Mexico_City')
  .onRun(async (context) => {
    const db = admin.firestore();
    
    // Retención por plan (días)
    const retentionDays = {
      'FREE': 30,
      'PRO': 90,
      'BUSINESS': 180,
      'ENTERPRISE': 365,
      'unknown': 30  // Default
    };
    
    // Procesar por batch para no sobrecargar
    for (const [plan, days] of Object.entries(retentionDays)) {
      const cutoffDate = new Date();
      cutoffDate.setDate(cutoffDate.getDate() - days);
      
      let deletedCount = 0;
      let hasMore = true;
      
      while (hasMore) {
        const snapshot = await db.collection('auditLogs')
          .where('context.companyPlan', '==', plan)
          .where('timestamp', '<', cutoffDate)
          .limit(500)  // Batch size
          .get();
        
        if (snapshot.empty) {
          hasMore = false;
          break;
        }
        
        const batch = db.batch();
        snapshot.docs.forEach(doc => batch.delete(doc.ref));
        await batch.commit();
        
        deletedCount += snapshot.size;
        
        // Esperar un poco entre batches
        await new Promise(resolve => setTimeout(resolve, 1000));
      }
      
      console.log(`Cleaned ${deletedCount} logs for plan ${plan} (older than ${days} days)`);
    }
    
    return null;
  });
```

**Desplegar**:
```bash
cd functions
firebase deploy --only functions:cleanOldAuditLogs
```

**Ahorro estimado**: 50-80% en almacenamiento a largo plazo

---

### 4. Agregación Diaria Pre-calculada

**Cuándo**: Cuando tengas dashboards o reportes frecuentes

**Implementación**:
```javascript
// En functions/index.js
exports.aggregateDailyMetrics = functions.pubsub
  .schedule('0 1 * * *')  // Diario a la 1 AM
  .onRun(async (context) => {
    const db = admin.firestore();
    const yesterday = new Date();
    yesterday.setDate(yesterday.getDate() - 1);
    yesterday.setHours(0, 0, 0, 0);
    
    const today = new Date(yesterday);
    today.setDate(today.getDate() + 1);
    
    // Obtener logs del día anterior
    const snapshot = await db.collection('auditLogs')
      .where('timestamp', '>=', yesterday)
      .where('timestamp', '<', today)
      .get();
    
    // Calcular métricas agregadas
    const metrics = {
      date: yesterday.toISOString().split('T')[0],
      totalOps: snapshot.size,
      successCount: 0,
      failedCount: 0,
      avgDuration: 0,
      byAction: {},
      byPlan: {},
      byStatus: {}
    };
    
    let totalDuration = 0;
    
    snapshot.docs.forEach(doc => {
      const data = doc.data();
      
      // Contadores por status
      metrics.byStatus[data.status] = (metrics.byStatus[data.status] || 0) + 1;
      if (data.status === 'SUCCESS') metrics.successCount++;
      else if (data.status === 'FAILED') metrics.failedCount++;
      
      // Contadores por acción
      metrics.byAction[data.action] = (metrics.byAction[data.action] || 0) + 1;
      
      // Contadores por plan
      const plan = data.context?.companyPlan || 'unknown';
      metrics.byPlan[plan] = (metrics.byPlan[plan] || 0) + 1;
      
      // Duración
      if (data.metrics?.durationMs) {
        totalDuration += data.metrics.durationMs;
      }
    });
    
    metrics.avgDuration = snapshot.size > 0 ? Math.round(totalDuration / snapshot.size) : 0;
    metrics.successRate = snapshot.size > 0 ? (metrics.successCount / snapshot.size) : 0;
    
    // Guardar métricas agregadas
    await db.collection('auditMetrics').doc(metrics.date).set(metrics);
    
    console.log(`Aggregated metrics for ${metrics.date}: ${snapshot.size} operations`);
    
    return null;
  });
```

**Consultas después**:
```javascript
// En lugar de consultar 10,000 logs individuales
db.collection('auditLogs').get()  // ❌ Costoso

// Usar métricas agregadas
db.collection('auditMetrics')
  .where('date', '>=', '2025-12-01')
  .where('date', '<=', '2025-12-31')
  .get()  // ✅ Solo 31 docs
```

**Ahorro estimado**: 90-95% en lecturas para reportes

---

### 5. Compresión de Metadata

**Cuándo**: Si almacenamiento se vuelve un problema

**Implementación**:
```kotlin
// En AuditLogger.kt
private fun compressMetadata(metadata: Map<String, Any>): String {
    // Convertir a JSON compacto
    val json = JSONObject(metadata).toString()
    
    // Comprimir con gzip (opcional para muy alto volumen)
    // val compressed = compress(json)
    
    return json
}

private fun storeInFirestore(event: AuditEvent) {
    try {
        db.collection(AUDIT_COLLECTION)
            .add(mapOf(
                "eventId" to event.eventId,
                "ts" to event.timestamp,  // Nombre corto
                "uid" to event.userId,
                "act" to event.action.name,
                "res" to event.resource.name,
                "rid" to event.resourceId,
                "sts" to event.status.name,
                "met" to event.metrics?.let {
                    mapOf(
                        "d" to it.durationMs,      // d = duration
                        "r" to it.resourcesAffected // r = resources
                    )
                },
                "ctx" to event.context?.let {
                    mapOf(
                        "cid" to it.companyId,     // cid = companyId
                        "pln" to it.companyPlan,   // pln = plan
                        "scr" to it.sourceScreen,  // scr = screen
                        "trg" to it.triggerType    // trg = trigger
                    )
                }
                // metadata como JSON comprimido si es muy grande
            ))
    } catch (e: Exception) {
        Log.w(TAG, "Could not store audit log: ${e.message}")
    }
}
```

**Ahorro estimado**: 20-30% en almacenamiento

---

## 📊 Plan de Implementación por Fase

### Fase 1: Inicio (0-1,000 usuarios)
- ✅ **Implementado**: Sistema completo con métricas
- 📝 **Pendiente**: Retención automática (#3)
- **Costo**: $0.03-0.50/mes
- **Acción**: Ninguna, monitorear costos

### Fase 2: Crecimiento (1,000-5,000 usuarios)
- 🔄 **Implementar**: 
  - Logging selectivo por plan (#1)
  - Retención automática (#3)
- **Costo esperado**: $2-8/mes
- **Timeline**: Cuando costos superen $2/mes

### Fase 3: Escala (5,000-50,000 usuarios)
- 🔄 **Implementar**:
  - Sampling (#2)
  - Agregación diaria (#4)
- **Costo esperado**: $10-50/mes
- **Timeline**: Cuando costos superen $10/mes

### Fase 4: Enterprise (50,000+ usuarios)
- 🔄 **Implementar**:
  - Compresión (#5)
  - Exportación a BigQuery
  - Archivado en Cloud Storage
- **Costo esperado**: $50-200/mes
- **Timeline**: Cuando costos superen $50/mes

---

## 🎯 Métricas a Monitorear

### Dashboard en Firebase Console

**Crear alertas para**:
```yaml
Alertas de Costo:
  - name: "High Firestore Writes"
    metric: auditLogs writes/day
    threshold: 10,000
    action: Revisar sampling

  - name: "Storage Growth"
    metric: auditLogs collection size
    threshold: 1 GB
    action: Implementar retención

  - name: "Read Spike"
    metric: auditLogs reads/day
    threshold: 50,000
    action: Usar agregación
```

### Queries Útiles

**Ver consumo diario**:
```javascript
// En Firebase Console > Firestore
// Ir a Usage tab y revisar:
// - Document reads/writes por colección
// - Storage por colección
```

**Calcular costo real**:
```javascript
const writes = 150000;  // del mes
const reads = 15000;
const storageGB = 1.5;

const cost = 
  (writes / 100000 * 0.18) +
  (reads / 100000 * 0.06) +
  (storageGB * 0.18);

console.log(`Costo mensual auditoría: $${cost.toFixed(2)}`);
```

---

## 📖 Referencias

**Documentación Firebase**:
- [Firestore Pricing](https://firebase.google.com/pricing)
- [Best Practices](https://firebase.google.com/docs/firestore/best-practices)
- [Understanding Costs](https://firebase.google.com/docs/firestore/pricing-example)

**Optimización adicional**:
- [BigQuery Export](https://firebase.google.com/docs/firestore/extend-with-functions)
- [Cloud Storage Archiving](https://cloud.google.com/storage/pricing)

---

## ✅ Checklist de Implementación

```markdown
## Ahora (< 1,000 usuarios)
- [x] Sistema de auditoría con métricas
- [x] Logging a Firestore
- [ ] Retención automática (30 días por defecto)

## Próximamente (1,000-5,000 usuarios)
- [ ] Logging selectivo por plan
- [ ] Configurar retención por plan
- [ ] Dashboard de costos

## Futuro (5,000+ usuarios)
- [ ] Implementar sampling
- [ ] Agregación diaria
- [ ] Exportación a BigQuery
- [ ] Compresión de datos
```

---

## 💡 Recomendación Final

**Para tu fase actual**:
- ✅ **MANTENER** el sistema tal como está
- ✅ Costo actual: **DESPRECIABLE** (<$0.50/mes)
- ✅ Beneficio vs costo: **EXCELENTE** (ROI 10x+)
- ✅ Implementar retención (#3) en próximas semanas

**Trigger para optimizar**:
- Cuando costos de auditoría > **$5/mes**
- Cuando tengas > **1,000 usuarios activos**
- Cuando recibas alerta de Firebase por alto uso

**No optimizar prematuramente** - el costo actual es negligible comparado con el valor que aporta.
