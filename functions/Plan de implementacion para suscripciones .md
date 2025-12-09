PLAN DE IMPLEMENTACIÓN PARA LANZAR SUSCRIPCIONES (Kotlin + Firebase)

Abarca técnico + negocio + arquitectura.
ETAPA 1 — Auditoría del estado actual (1–2 días)
✔️ 1. Revisar estructura de datos actual en Firestore
Valida:
Colecciones principales
Subcolecciones
Escrituras redundantes
Lecturas útiles
Esquema para multi-empresa
📌 Objetivo: evitar que el modelo actual reviente los costos cuando escales.
✔️ 2. Revisar flujo de tareas/subtareas
Especialmente:
Campos de evidencia
Estatus
Permisos (jefe/miembro)
Pujas/budget
✔️ 3. Detectar puntos sensibles para implementar Paywall
Ejemplo:
Evidencias ilimitadas
Grupos ilimitados
Reportes
Jerarquía de aprobación
ETAPA 2 — Estructura técnica para soportar suscripciones (4–7 días)
🔹 2.1 Crear un modelo de “planes” en Firestore
Colección sugerida:
/plans - free - pro - business - enterprise
Cada plan con:
Límite de fotos por mes
Límite de grupos
Límite de usuarios por grupo
Límite de tareas activas
Acceso a presupuestos/pujas
Acceso a jerarquía de aprobación
Acceso a chat extendido
Espacio en Storage asignado
Esto permite cambiar reglas sin actualizar la app.
🔹 2.2 Crear estructura “subscription” por empresa/grupo
Por cada empresa:
/companies/{companyId}/billing planId: “pro” nextBillingDate: 2025-01-01 storageUsed: 530MB maxStorage: 10GB usersAllowed: 10
⚠️ Tu app orientada a equipos → siempre usar empresa/grupo como unidad de cobro, no usuario individual.
🔹 2.3 Crear “Feature Flags” (habilitar/deshabilitar funciones dinámicamente)
Ejemplo:
class FeatureFlags( val canUploadUnlimitedPhotos: Boolean, val maxTeams: Int, val maxTasksActive: Int, val canUseBudgets: Boolean, val canApproveTasks: Boolean )
En Kotlin, cargarlo al iniciar la app:
suspend fun loadPlanFeatures(companyId: String): FeatureFlags { val snapshot = firestore.collection("companies") .document(companyId) .collection("billing") .document("current") .get() .await() val planId = snapshot.getString("planId") ?: "free" val planSnap = firestore.collection("plans") .document(planId) .get() .await() return planSnap.toObject(FeatureFlags::class.java)!! }
ETAPA 3 — Integración con Google Play Billing (Kotlin) (5–7 días)
🔹 3.1 Crear productos en Google Play Console
Plan sugerido:
ProductoTipoPrecioPlan PROSuscripción mensual$6.99Plan PRO AnualSuscripción anual$69Plan BusinessSuscripción mensual$39Plan Business Anual$399Addon 100GBConsumible$4.99Addon 1TBConsumible$19.99
🔹 3.2 Implementar BillingClient en Kotlin
Inicializar BillingClient
val billingClient = BillingClient.newBuilder(context) .setListener(purchasesUpdatedListener) .enablePendingPurchases() .build()
Conectarse y consultar productos
billingClient.startConnection(object : BillingClientStateListener { override fun onBillingServiceDisconnected() {} override fun onBillingSetupFinished(billingResult: BillingResult) { if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) { queryAvailablePlans() } } })
🔹 3.3 Implementar verificación segura en backend
⚠️ Nunca confíes solo en la app.
Usa Firebase Functions para:
Verificar recibos
Actualizar plan en Firestore
Crear registro de billing
Activar/desactivar funciones
ETAPA 4 — Crear el Paywall y sistema de límites (3–6 días)
🔥 Funciones que deben estar detrás de un Paywall:
Evidencias ilimitadas
Más de 1 grupo
Más de X usuarios por grupo
Aprobación jerárquica
Pujas/presupuestos
Mensajería avanzada (multimedia)
