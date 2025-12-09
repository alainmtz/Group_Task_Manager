#!/bin/bash

# Script para monitorear logs de Cloud Functions en tiempo real
# Uso: ./monitor-logs.sh [funcion]
# Sin argumentos: monitorea sendUpdateEvent
# Con argumento: monitorea función específica

PROJECT="agenda-solar"
FUNCTION="${1:-sendUpdateEvent}"

echo "📊 Monitoreando logs de: $FUNCTION"
echo "🔄 Proyecto: $PROJECT"
echo "⏱️  Actualizando cada 10 segundos... (Ctrl+C para salir)"
echo "=================================================="
echo ""

# Loop continuo
while true; do
    clear
    echo "📊 Logs de $FUNCTION - $(date '+%H:%M:%S')"
    echo "=================================================="
    
    # Mostrar últimas 15 líneas de logs
    firebase --project=$PROJECT functions:log \
        --only $FUNCTION \
        --lines 15 \
        2>/dev/null || echo "❌ Error obteniendo logs"
    
    echo ""
    echo "=================================================="
    echo "Actualizando en 10 segundos... (Ctrl+C para salir)"
    
    sleep 10
done
