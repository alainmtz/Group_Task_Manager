# Métricas y Analytics Avanzadas - Sistema de Auditoría

## Nuevas Capacidades

### 1. Métricas de Performance (`OperationMetrics`)

Cada evento ahora incluye métricas de rendimiento:

```kotlin
data class OperationMetrics(
    val durationMs: Long,           // Tiempo de ejecución en milisegundos
    val retryCount: Int = 0,        // Número de reintentos
    val resourcesAffected: Int = 0, // Recursos modificados/eliminados
    val dataSize: Long? = null      // Tamaño de datos (bytes), opcional
)
```

#### Ejemplo de Log con Métricas:
```
[1765480270613-9319] CHAT_DELETE on CHAT_THREAD(abc123) -> SUCCESS | 452ms, 1 resources
[1765480270989-7344] TASK_DELETE on TASK(def456) -> SUCCESS | 340ms, 5 resources
[1765480271396-6422] GROUP_DELETE on GROUP(ghi789) -> SUCCESS | 826ms, 1 resources
```

### 2. Contexto Operacional (`OperationContext`)

Información contextual enriquecida para análisis empresarial:

```kotlin
data class OperationContext(
    val companyId: String?,         // ID de la empresa
    val companyPlan: String?,       // FREE, PRO, BUSINESS, ENTERPRISE
    val deviceInfo: String?,        // Info del dispositivo (futuro)
    val appVersion: String?,        // Versión de la app (futuro)
    val sourceScreen: String?,      // Pantalla de origen
    val triggerType: String?        // USER_ACTION, AUTOMATIC, SCHEDULED
)
```

#### Ejemplo de Log con Contexto:
```
=== Operation Summary ===
Total: 5
Success: 4
Failed: 1
Duration: 1850ms
Resources Affected: 8

📋 Context:
  Company: xyz123
  Plan: ENTERPRISE
  Screen: GroupDetailScreen
  Trigger: USER_ACTION

⚠️ Partial failures detected:
  - CHAT_DELETE on CHAT_THREAD (452ms): PERMISSION_DENIED
```

### 3. Resumen Agregado Mejorado

El resumen ahora incluye métricas agregadas:

```kotlin
data class OperationSummary(
    val totalOperations: Int,
    val successfulOperations: Int,
    val failedOperations: Int,
    val skippedOperations: Int,
    val criticalOperationSuccess: Boolean,
    val totalDurationMs: Long,      // ⭐ NUEVO
    val resourcesAffected: Int      // ⭐ NUEVO
)
```

## Casos de Uso Empresariales

### 1. Análisis de Performance

#### Identificar Operaciones Lentas
```javascript
// Query en Firestore
auditLogs
  .where("metrics.durationMs", ">", 2000) // Más de 2 segundos
  .orderBy("metrics.durationMs", "desc")
```

**Acción:** Optimizar queries o índices de Firestore

#### Dashboard de Performance por Plan
```javascript
// Agrupar por companyPlan
const performanceByPlan = {
  FREE: { avgDuration: 1250ms, p95: 2100ms },
  PRO: { avgDuration: 980ms, p95: 1800ms },
  ENTERPRISE: { avgDuration: 720ms, p95: 1200ms }
}
```

**Insight:** Plans superiores tienen mejor performance (infraestructura dedicada)

### 2. Análisis de Impacto

#### Calcular Recursos Afectados por Operación
```javascript
auditLogs
  .where("action", "==", "GROUP_DELETE")
  .get()
  .then(snapshot => {
    const totalResources = snapshot.docs.reduce((sum, doc) => 
      sum + (doc.data().metrics?.resourcesAffected || 0), 0
    );
    console.log(`Total resources deleted: ${totalResources}`);
  });
```

**Uso:** Reportes de actividad, compliance, rollback estimation

### 3. Análisis por Plan y Compañía

#### Detectar Uso Intensivo por Plan
```javascript
// Empresas con mayor volumen de operaciones
auditLogs
  .where("context.companyPlan", "==", "ENTERPRISE")
  .where("timestamp", ">", last30Days)
  .get()
  .then(snapshot => {
    const byCompany = groupBy(snapshot.docs, 'context.companyId');
    const topUsers = Object.entries(byCompany)
      .map([companyId, ops] => ({
        companyId,
        totalOps: ops.length,
        avgDuration: avg(ops.map(o => o.metrics?.durationMs))
      }))
      .sort((a, b) => b.totalOps - a.totalOps)
      .slice(0, 10);
  });
```

**Acción:** 
- Identificar candidatos para upgrades
- Detectar patrones de abuso
- Planificar capacidad

### 4. Análisis de Errores y Reintentos

#### Tasas de Éxito por Operación
```javascript
const errorRateByAction = {
  GROUP_DELETE: {
    total: 1000,
    success: 950,
    failed: 30,
    permissionDenied: 20,
    successRate: 95%
  },
  CHAT_DELETE: {
    total: 1000,
    success: 800,
    failed: 50,
    permissionDenied: 150,
    successRate: 80% // ⚠️ Menor tasa - revisar permisos
  }
}
```

#### Operaciones con Reintentos
```javascript
auditLogs
  .where("metrics.retryCount", ">", 0)
  .get()
```

**Insight:** Alto número de reintentos puede indicar problemas de red o backend

### 5. Segmentación por Pantalla/Trigger

#### Análisis de Fuentes de Operaciones
```javascript
const operationsBySource = {
  "GroupDetailScreen": 1500,
  "GroupListScreen": 300,
  "AdminPanel": 50
}

const operationsByTrigger = {
  "USER_ACTION": 1700,
  "AUTOMATIC": 150,    // ej: cleanup automático
  "SCHEDULED": 0
}
```

**Uso:** Optimizar UX en pantallas con mayor actividad

## Dashboards Recomendados

### Dashboard Ejecutivo

#### KPIs Principales
```
📊 Operaciones Totales (30 días): 125,430
✅ Tasa de Éxito: 94.5%
⚡ Duración Promedio: 875ms
📈 Crecimiento: +12% vs mes anterior

Por Plan:
- FREE: 85,200 ops (68%)
- PRO: 32,100 ops (26%)
- ENTERPRISE: 8,130 ops (6%)

Top Operaciones:
1. TASK_DELETE: 45,200
2. GROUP_DELETE: 12,800
3. MEMBER_REMOVE: 8,500
```

### Dashboard de Performance

#### Gráficos Sugeridos
1. **Línea temporal**: Duración promedio por día
2. **Histograma**: Distribución de duraciones
3. **Heatmap**: Operaciones por hora del día
4. **Percentiles**: P50, P95, P99 de duraciones

#### Alertas
```yaml
- name: "Slow Operations"
  condition: avgDuration > 2000ms
  threshold: 10% of ops
  action: send_alert_to_devops

- name: "High Failure Rate"
  condition: failureRate > 10%
  action: send_alert_to_oncall

- name: "Permission Denials Spike"
  condition: permissionDeniedRate > 20%
  action: check_firestore_rules
```

### Dashboard de Compliance

#### Reportes de Auditoría
```
📋 Reporte Trimestral - Q4 2025

Operaciones Críticas:
- GROUP_DELETE: 12,800 (100% auditadas)
- MEMBER_REMOVE: 8,500 (100% auditadas)

Por Usuario Top 10:
1. user_xyz: 450 ops (todas autorizadas)
2. user_abc: 320 ops (2 fallos de permisos)

Errores Críticos: 3 (0.02%)
- Detalle disponible en logs individuales
- Todos con trail completo

Retención: 365 días
Próxima revisión: 2026-04-01
```

## Queries Útiles para Analytics

### 1. Performance por Plan
```javascript
db.collection('auditLogs')
  .where('context.companyPlan', 'in', ['FREE', 'PRO', 'ENTERPRISE'])
  .where('timestamp', '>', startDate)
  .get()
  .then(docs => {
    const byPlan = groupBy(docs, 'context.companyPlan');
    
    Object.entries(byPlan).forEach(([plan, ops]) => {
      console.log(`${plan}:`, {
        count: ops.length,
        avgDuration: avg(ops.map(o => o.metrics.durationMs)),
        p95Duration: percentile(ops.map(o => o.metrics.durationMs), 95),
        successRate: ops.filter(o => o.status === 'SUCCESS').length / ops.length
      });
    });
  });
```

### 2. Impacto por Compañía
```javascript
// Total de recursos afectados por compañía
db.collection('auditLogs')
  .where('context.companyId', '!=', null)
  .get()
  .then(docs => {
    const byCompany = {};
    
    docs.forEach(doc => {
      const companyId = doc.data().context.companyId;
      const resourcesAffected = doc.data().metrics?.resourcesAffected || 0;
      
      byCompany[companyId] = (byCompany[companyId] || 0) + resourcesAffected;
    });
    
    // Top 10 compañías por impacto
    return Object.entries(byCompany)
      .sort((a, b) => b[1] - a[1])
      .slice(0, 10);
  });
```

### 3. Análisis de Bottlenecks
```javascript
// Operaciones que toman más tiempo
db.collection('auditLogs')
  .orderBy('metrics.durationMs', 'desc')
  .limit(100)
  .get()
  .then(docs => {
    const slowOps = docs.map(doc => ({
      action: doc.data().action,
      duration: doc.data().metrics.durationMs,
      resourcesAffected: doc.data().metrics.resourcesAffected,
      companyPlan: doc.data().context?.companyPlan,
      timestamp: doc.data().timestamp
    }));
    
    // Agrupar por acción para ver patrones
    const bottlenecksByAction = groupBy(slowOps, 'action');
    console.log('Top bottlenecks by action:', bottlenecksByAction);
  });
```

### 4. Patrones Temporales
```javascript
// Distribución de operaciones por hora del día
db.collection('auditLogs')
  .where('timestamp', '>', last7Days)
  .get()
  .then(docs => {
    const byHour = new Array(24).fill(0);
    
    docs.forEach(doc => {
      const hour = new Date(doc.data().timestamp).getHours();
      byHour[hour]++;
    });
    
    console.log('Operations by hour:', byHour);
    // Visualizar en heatmap para identificar peak hours
  });
```

## Integración con BI Tools

### Exportación a BigQuery

```javascript
// Cloud Function para exportar a BigQuery diariamente
exports.exportToBigQuery = functions.pubsub
  .schedule('0 2 * * *') // 2 AM diario
  .onRun(async () => {
    const yesterday = new Date();
    yesterday.setDate(yesterday.getDate() - 1);
    yesterday.setHours(0, 0, 0, 0);
    
    const snapshot = await db.collection('auditLogs')
      .where('timestamp', '>=', yesterday)
      .where('timestamp', '<', new Date())
      .get();
    
    const rows = snapshot.docs.map(doc => {
      const data = doc.data();
      return {
        eventId: data.eventId,
        timestamp: data.timestamp,
        userId: data.userId,
        action: data.action,
        status: data.status,
        durationMs: data.metrics?.durationMs,
        resourcesAffected: data.metrics?.resourcesAffected,
        companyId: data.context?.companyId,
        companyPlan: data.context?.companyPlan,
        errorType: data.errorDetails?.errorType
      };
    });
    
    // Insert into BigQuery
    await bigquery
      .dataset('analytics')
      .table('audit_logs')
      .insert(rows);
  });
```

### Conexión con Looker Studio

1. Conectar BigQuery como fuente
2. Crear métricas calculadas:
   - **Success Rate**: `COUNT(SUCCESS) / COUNT(*)`
   - **Avg Duration**: `AVG(durationMs)`
   - **P95 Duration**: `PERCENTILE(durationMs, 95)`
   - **Resources per Operation**: `AVG(resourcesAffected)`

3. Visualizaciones:
   - Scorecard: Operaciones totales
   - Time Series: Operaciones por día
   - Bar Chart: Operaciones por plan
   - Scatter Plot: Duración vs Recursos Afectados

## Beneficios de las Métricas

### Para DevOps
- ⚡ Identificar operaciones lentas
- 🔍 Debugging con contexto completo
- 📊 Capacity planning basado en uso real
- 🚨 Alertas proactivas de degradación

### Para Product
- 📈 Entender patrones de uso
- 🎯 Optimizar features más usados
- 💡 Data-driven product decisions
- 🆙 Identificar oportunidades de upgrade

### Para Compliance
- ✅ Trail completo con performance
- 📋 Reportes automáticos
- 🔐 Evidencia de operaciones críticas
- 📅 Retención y archivado

### Para Ventas/CS
- 💰 Identificar power users
- 📊 Usage reports para clientes
- 🎯 Targeting para upsells
- 🤝 Proactive support basado en métricas

## Roadmap de Analytics

### Fase 1 (Completado ✅)
- Métricas de duración
- Contexto de compañía y plan
- Conteo de recursos afectados
- Logging estructurado

### Fase 2 (Q1 2026)
- Dashboard web admin
- Exportación a BigQuery
- Alertas automáticas
- Reportes programados

### Fase 3 (Q2 2026)
- Machine Learning para anomalías
- Predicción de bottlenecks
- Recomendaciones de optimización
- Cost analysis por operación

### Fase 4 (Q3 2026)
- Real-time analytics
- Self-service BI para clientes
- Benchmarking entre empresas
- Predictive capacity planning
