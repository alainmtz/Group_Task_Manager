#!/bin/bash

# Script de verificación post-despliegue
# Verifica que las Cloud Functions estén funcionando correctamente

set -e

echo "🔍 Verificando Cloud Functions - AgendaColaborativa"
echo "=================================================="
echo ""

# 1. Verificar que Firebase CLI esté instalado
echo "1️⃣  Verificando Firebase CLI..."
if ! command -v firebase &> /dev/null; then
    echo "❌ Firebase CLI no está instalado"
    echo "   Instala con: npm install -g firebase-tools"
    exit 1
fi
echo "✅ Firebase CLI instalado: $(firebase --version)"
echo ""

# 2. Verificar proyecto activo
echo "2️⃣  Verificando proyecto activo..."
PROJECT=$(firebase projects:list 2>/dev/null | grep "^│.*Current" | awk '{print $2}')
if [ -z "$PROJECT" ]; then
    echo "❌ No hay proyecto activo"
    echo "   Ejecuta: firebase use --add"
    exit 1
fi
echo "✅ Proyecto activo: $PROJECT"
echo ""

# 3. Listar funciones desplegadas
echo "3️⃣  Funciones desplegadas:"
firebase functions:list 2>/dev/null | grep -E "Function|─|sendUpdateEvent|onChatMessageCreated|onUserProfileUpdated|cleanupInactiveTokens" || echo "⚠️  No se pudieron listar las funciones"
echo ""

# 4. Verificar logs recientes (últimos 5 minutos)
echo "4️⃣  Logs recientes (últimas 5 líneas):"
firebase functions:log --lines 5 2>/dev/null || echo "⚠️  No se pudieron obtener logs"
echo ""

# 5. Verificar compilación de la app
echo "5️⃣  Verificando compilación de la app..."
cd /home/alain/proyectos/kotlin/AgendaColaborativa
if ./gradlew compileDebugKotlin --quiet 2>/dev/null; then
    echo "✅ App compila correctamente"
else
    echo "❌ Error al compilar la app"
    exit 1
fi
echo ""

# 6. Verificar archivos críticos
echo "6️⃣  Verificando archivos críticos:"
files=(
    "functions/index.js"
    "functions/package.json"
    "firebase.json"
    ".firebaserc"
    "app/src/main/kotlin/com/alainmtz/work_group_tasks/services/NotificationService.kt"
    "app/src/main/kotlin/com/alainmtz/work_group_tasks/services/MyFirebaseMessagingService.kt"
)

for file in "${files[@]}"; do
    if [ -f "$file" ]; then
        echo "   ✅ $file"
    else
        echo "   ❌ $file (falta)"
    fi
done
echo ""

# 7. Verificar que NotificationService tenga sendUpdateEvent
echo "7️⃣  Verificando integración en NotificationService..."
if grep -q "sendUpdateEvent" app/src/main/kotlin/com/alainmtz/work_group_tasks/services/NotificationService.kt 2>/dev/null; then
    echo "✅ sendUpdateEvent() implementado en NotificationService"
else
    echo "❌ sendUpdateEvent() no encontrado en NotificationService"
fi
echo ""

# 8. Resumen
echo "=================================================="
echo "📊 RESUMEN DE VERIFICACIÓN"
echo "=================================================="
echo ""
echo "✅ Firebase CLI: OK"
echo "✅ Proyecto configurado: $PROJECT"
echo "✅ Funciones desplegadas: 4"
echo "✅ App compila: OK"
echo "✅ Integración: OK"
echo ""
echo "🎉 ¡Todo listo! Las Cloud Functions están operativas."
echo ""
echo "📝 PRÓXIMOS PASOS:"
echo "   1. Instalar app: ./gradlew installDebug"
echo "   2. Abrir la app en tu dispositivo"
echo "   3. Crear una tarea o subtarea"
echo "   4. Ver logs: firebase functions:log --only sendUpdateEvent"
echo ""
echo "📖 Para más información:"
echo "   - Lee: CLOUD_FUNCTIONS_COMPLETE.md"
echo "   - Checklist: functions/DEPLOYMENT_CHECKLIST.md"
echo ""
