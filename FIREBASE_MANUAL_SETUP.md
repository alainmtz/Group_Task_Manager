# ============================================
# CHECKLIST DE CONFIGURACIÓN DE FIREBASE
# ============================================
# Fecha: 9 de diciembre de 2025
# Proyecto: agenda-solar
# ============================================

## ✅ PASO 1: Resolver Error de Firebase CLI

**Problema resuelto:** El error "Invalid project id: Kotlin" se debía a que Firebase CLI interpretaba "kotlin" del path como un project ID.

**Solución aplicada:**
```bash
firebase use default --project agenda-solar
```

✅ **Estado:** RESUELTO

---

## ⚠️ PASO 2: Inicializar Planes en Firestore

**Problema actual:** Firebase Admin SDK requiere credenciales para acceder a Firestore.

### Cómo proveer credenciales para ejecutar scripts Node.js (opcional)

Si quieres ejecutar los scripts (`functions/init-plans.js` o `init-plans-direct.js`) desde tu máquina/servidor, crea una service account y descarga la clave JSON. Puedes usar cualquiera de estas opciones:

- Opción A (archivo): exporta `SERVICE_ACCOUNT_KEY_PATH` apuntando al JSON descargado:

```bash
export SERVICE_ACCOUNT_KEY_PATH="$HOME/.config/gcloud/agenda-solar-sa.json"
node functions/init-plans.js
```

- Opción B (ADC con gcloud): usa Application Default Credentials (recomendado si tienes `gcloud`):

```bash
gcloud auth application-default login
node functions/init-plans.js
```

- Opción C (variable de entorno base64): si prefieres no dejar el archivo en disco, base64-encodea el JSON y pásalo:

```bash
export SERVICE_ACCOUNT_KEY_JSON=$(base64 /path/to/key.json)
node functions/init-plans.js
```

**Crear service account (pasos resumidos):**

1. Abre: https://console.cloud.google.com/iam-admin/serviceaccounts?project=agenda-solar
2. Click `CREATE SERVICE ACCOUNT` → Nombre: `agenda-deployer` → ID: `agenda-deployer`
3. Asigna rol: `Firestore Owner` o `Project Editor` (mínimo `Cloud Datastore Owner` / `Firestore Admin` para escribir datos)
4. En la sección `Keys` añade una nueva llave JSON y descárgala
5. Guarda ese archivo en un lugar seguro y no lo subas al repo

> ⚠️ Nunca comites la clave JSON al repositorio. Trata el archivo como una credencial secreta.

### Opción A: Usar Firebase CLI para deployment (RECOMENDADO)

No podemos usar scripts Node.js directamente sin credenciales, pero podemos usar la Firebase Console.

#### Pasos manuales (5 minutos):

1. **Abre Firebase Console:**
   ```
   https://console.firebase.google.com/project/agenda-solar/firestore/data
   ```

2. **Crea la colección `/plans`:**
   - Click en "Start collection"
   - Collection ID: `plans`
   - Click "Next"

3. **Agrega los 4 documentos:**

   **Documento 1: `free`**
   ```
   Document ID: free
   
   Copiar y pegar todo el contenido del objeto "free" desde plans-data.json:
   - id: "free"
   - name: "Free Plan"
   - tier: "free"
   - priceMonthly: 0
   - priceYearly: 0
   - features: { ... todo el objeto features ... }
   ```

   **Documento 2: `pro`**
   ```
   Document ID: pro
   (Copiar contenido del objeto "pro" desde plans-data.json)
   ```

   **Documento 3: `business`**
   ```
   Document ID: business
   (Copiar contenido del objeto "business" desde plans-data.json)
   ```

   **Documento 4: `enterprise`**
   ```
   Document ID: enterprise
   (Copiar contenido del objeto "enterprise" desde plans-data.json)
   ```

4. **Verificar:** Deberías ver 4 documentos en la colección `/plans`

✅ **Estado:** PENDIENTE (manual)

---

## 📋 PASO 3: Crear Tu Empresa Enterprise

Una vez que los planes estén en Firestore, necesitas:

### A. Registrarte en la app

1. Instala la app en tu dispositivo:
   ```bash
   ./gradlew installDebug
   ```

2. Abre la app y regístrate con tu email

3. Obtén tu Firebase Auth UID:
   - Ve a: https://console.firebase.google.com/project/agenda-solar/authentication/users
   - Busca tu email
   - Copia el UID (algo como: `abc123xyz789...`)

### B. Crear tu empresa Enterprise manualmente

1. **Abre Firebase Console:**
   ```
   https://console.firebase.google.com/project/agenda-solar/firestore/data
   ```

2. **Crea la colección `/companies`** (si no existe)

3. **Agrega tu documento de empresa:**
   ```
   Document ID: FulltimeCuba25
   
   Campos:
   - id: "FulltimeCuba25"
   - name: "Fulltime Cuba"
   - ownerId: "vudP9Mr4zJU4rSWazLiMzA9TI4g1"
   - adminIds: ["vudP9Mr4zJU4rSWazLiMzA9TI4g1"]
   - memberIds: ["vudP9Mr4zJU4rSWazLiMzA9TI4g1"]
   - planId: "enterprise"
   - planTier: "enterprise"
   - subscriptionStatus: "ACTIVE"
   - subscriptionStartDate: [Timestamp] now
   - subscriptionEndDate: null
   - autoRenew: true
   - paymentMethod: null
   - activeTasksCount: 0
   - groupsCount: 0
   - storageUsedBytes: 0
   - photosUploadedThisMonth: 0
   - lastPhotoResetDate: [Timestamp] now
   - createdAt: [Timestamp] now
   - updatedAt: [Timestamp] now
   ```

4. **Actualiza tu documento de usuario:**
   - Ve a `/users/TU_UID_AQUI`
   - Agrega o actualiza estos campos:
     ```
     - companyId: "FulltimeCuba25"
     - role: "OWNER"
     - updatedAt: [Timestamp] now
     ```

✅ **Estado:** PENDIENTE (requiere registro en app)

---

## 🎯 PASO 4: Verificación Final

Después de completar los pasos anteriores:

### A. Verifica en Firestore:
```
https://console.firebase.google.com/project/agenda-solar/firestore/data
```

Deberías ver:
- ✓ Colección `/plans` con 4 documentos (free, pro, business, enterprise)
- ✓ Colección `/companies` con tu empresa
- ✓ Documento en `/users/TU_UID` con companyId y role

### B. Verifica en la app:

1. Cierra sesión y vuelve a entrar
2. Deberías tener acceso a:
   - ✓ Crear grupos ilimitados
   - ✓ Agregar miembros ilimitados
   - ✓ Crear tareas ilimitadas
   - ✓ Subir fotos ilimitadas
   - ✓ Todas las funcionalidades Enterprise

---

## 📊 ESTADO ACTUAL DEL DEPLOYMENT

### ✅ COMPLETADO:
- [x] Modelos Kotlin creados (Plan, Company, FeatureFlags, etc.)
- [x] Código compila sin errores
- [x] Firestore rules deployed
- [x] Firestore indexes deployed (14 indexes activos)
- [x] firebase.json configurado
- [x] .firebaserc corregido (resuelto problema de "kotlin")
- [x] plans-data.json generado con datos completos

### ⚠️ PENDIENTE MANUAL:
- [ ] Crear colección `/plans` en Firestore Console
- [ ] Agregar 4 documentos de planes manualmente
- [ ] Registrarte en la app
- [ ] Obtener tu UID
- [ ] Crear tu empresa Enterprise en Firestore Console
- [ ] Actualizar tu documento de usuario

### ❌ BLOQUEADO (No crítico):
- Scripts Node.js requieren service account credentials
- Alternativa manual funciona perfectamente

---

## 🔗 LINKS RÁPIDOS

- **Firebase Console:** https://console.firebase.google.com/project/agenda-solar
- **Firestore Data:** https://console.firebase.google.com/project/agenda-solar/firestore/data
- **Authentication:** https://console.firebase.google.com/project/agenda-solar/authentication/users
- **Rules:** https://console.firebase.google.com/project/agenda-solar/firestore/rules
- **Indexes:** https://console.firebase.google.com/project/agenda-solar/firestore/indexes

---

## 📝 NOTAS IMPORTANTES

1. **Indexes:** Los 14 indexes pueden tardar unos minutos en construirse. Verifica el estado en la consola.

2. **Security Rules:** Ya están deployed y protegen:
   - `/companies`: Solo miembros pueden leer, solo owner/admin pueden escribir
   - `/plans`: Solo lectura para usuarios autenticados
   - Colecciones existentes mantienen backward compatibility

3. **Enterprise Plan:** Tu plan Enterprise nunca expira (`subscriptionEndDate: null`)

4. **Timestamps:** Usa el tipo "Timestamp" de Firestore en la consola, no "Date"

---

## 🆘 TROUBLESHOOTING

### Si la app no reconoce tu empresa:
1. Verifica que `companyId` en `/users/TU_UID` sea correcto
2. Verifica que exista `/companies/company_TU_UID`
3. Cierra sesión y vuelve a entrar
4. Revisa los logs de la app en Logcat

### Si dice que no tienes permisos:
1. Verifica que `role` en `/users/TU_UID` sea "OWNER"
2. Verifica que tu UID esté en `ownerId` y `memberIds` de la empresa
3. Verifica que las Firestore rules estén deployed correctamente

### Si los features no funcionan:
1. Verifica que `planTier` en la empresa sea "enterprise"
2. Verifica que exista el documento `/plans/enterprise`
3. Verifica que `subscriptionStatus` sea "ACTIVE"

---

**Tiempo estimado:** 15-20 minutos en total
**Dificultad:** Fácil (solo copiar/pegar en Firebase Console)
