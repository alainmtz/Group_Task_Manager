# 🚀 Guía Rápida: Configurar Firebase en 10 Minutos

## PASO 1: Crear Planes (5 minutos)

### Opción A: Firebase Console (Recomendado)

1. **Abre:** https://console.firebase.google.com/project/agenda-solar/firestore/data

2. **Crea colección:** Click "Start collection" → ID: `plans`

3. **Agrega 4 documentos con estos IDs:**
   - `free`
   - `pro`  
   - `business`
   - `enterprise`

4. **Para cada documento, copia TODOS los campos desde `plans-data.json`**
   
   Por ejemplo, para `free`:
   ```
   id: "free"
   name: "Free Plan"  
   tier: "free"
   priceMonthly: 0
   priceYearly: 0
   features: {
     maxGroups: 1
     maxMembersPerGroup: 5
     maxActiveTasks: 10
     maxStorageGB: 0
     maxStorageMB: 100
     maxPhotosPerMonth: 5
     canUseBudgets: false
     ... (18 campos en total)
   }
   ```

   **💡 TIP:** Firebase Console te permite pegar JSON directamente. Click en "⋮" → "Edit JSON"

---

## PASO 2: Registrarte y Crear Tu Empresa (5 minutos)

### A. Instalar y registrarte

```bash
./gradlew installDebug
```

- Abre la app
- Regístrate con tu email

### B. Obtener tu UID

1. **Abre:** https://console.firebase.google.com/project/agenda-solar/authentication/users
2. Busca tu email
3. **Copia el UID** (ejemplo: `xYz123AbC456...`)

### C. Crear tu empresa Enterprise

1. **Abre:** https://console.firebase.google.com/project/agenda-solar/firestore/data

2. **Crea colección:** `companies` (si no existe)

3. **Agrega documento:**
   ```
   Document ID: company_TU_UID_AQUI
   ```

4. **Agrega estos campos:**
   ```
   id: "company_TU_UID_AQUI"
   name: "Mi Empresa (Enterprise)"
   ownerId: "TU_UID_AQUI"
   adminIds: ["TU_UID_AQUI"]
   memberIds: ["TU_UID_AQUI"]
   planId: "enterprise"
   planTier: "enterprise"
   subscriptionStatus: "ACTIVE"
   subscriptionStartDate: [Timestamp] now
   subscriptionEndDate: null
   autoRenew: true
   paymentMethod: null
   activeTasksCount: 0
   groupsCount: 0
   storageUsedBytes: 0
   photosUploadedThisMonth: 0
   lastPhotoResetDate: [Timestamp] now
   createdAt: [Timestamp] now
   updatedAt: [Timestamp] now
   ```

   **💡 TIP:** Para Timestamps, selecciona tipo "timestamp" y click "Set to current time"

5. **Actualiza tu usuario:**
   - Ve a `/users/TU_UID_AQUI`
   - Agrega/actualiza:
     ```
     companyId: "company_TU_UID_AQUI"
     role: "OWNER"
     updatedAt: [Timestamp] now
     ```

---

## PASO 3: Verificar (1 minuto)

1. Cierra sesión en la app
2. Vuelve a entrar
3. **¡Listo!** Ahora tienes:
   - ✓ Grupos ilimitados
   - ✓ Miembros ilimitados
   - ✓ Tareas ilimitadas
   - ✓ Storage ilimitado
   - ✓ Todas las features Enterprise

---

## ⚠️ Notas Importantes

- **Usar tipos correctos:** String para texto, number para números, timestamp para fechas
- **Arrays:** Para `adminIds` y `memberIds`, usa tipo "array" con strings
- **null:** Para `subscriptionEndDate` y `paymentMethod`, usa tipo "null"
- **-1 = ilimitado:** En Enterprise, todos los max* son -1

---

## 🆘 Problemas Comunes

**"No tengo permisos":**
- Verifica que `role` sea "OWNER" en mayúsculas
- Verifica que tu UID esté en `memberIds` de la empresa

**"Dice que no tengo plan Enterprise":**
- Verifica que `planTier` sea "enterprise" (minúsculas)
- Verifica que exista `/plans/enterprise`

**"La app crashea":**
- Verifica que TODOS los campos tengan el tipo correcto
- Revisa Logcat para ver el error específico

---

## 📊 Estructura Esperada en Firestore

```
/plans
  /free (documento)
  /pro (documento)
  /business (documento)
  /enterprise (documento)

/companies
  /company_TU_UID (documento)

/users
  /TU_UID (documento con companyId)
```

---

**¿Necesitas ayuda?** Revisa `FIREBASE_MANUAL_SETUP.md` para guía detallada.
