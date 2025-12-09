#!/bin/bash

# Script de despliegue para Cloud Functions
# Uso: ./deploy.sh [all|sendUpdateEvent|chat|profile|cleanup]

set -e

echo "🚀 Desplegando Cloud Functions para AgendaColaborativa..."

case "${1:-all}" in
  all)
    echo "📦 Desplegando todas las funciones..."
    firebase deploy --only functions
    ;;
  
  sendUpdateEvent)
    echo "📦 Desplegando función: sendUpdateEvent..."
    firebase deploy --only functions:sendUpdateEvent
    ;;
  
  chat)
    echo "📦 Desplegando función: onChatMessageCreated..."
    firebase deploy --only functions:onChatMessageCreated
    ;;
  
  profile)
    echo "📦 Desplegando función: onUserProfileUpdated..."
    firebase deploy --only functions:onUserProfileUpdated
    ;;
  
  cleanup)
    echo "📦 Desplegando función: cleanupInactiveTokens..."
    firebase deploy --only functions:cleanupInactiveTokens
    ;;
  
  *)
    echo "❌ Opción no válida: $1"
    echo "Uso: ./deploy.sh [all|sendUpdateEvent|chat|profile|cleanup]"
    exit 1
    ;;
esac

echo "✅ Despliegue completado!"
echo ""
echo "📊 Ver logs: firebase functions:log"
echo "🔍 Ver dashboard: https://console.firebase.google.com"
