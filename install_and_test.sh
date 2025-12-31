#!/bin/bash

# Script simplificado para instalar y probar auto-update
# Ejecuta: ./install_and_test.sh

echo "🔍 Buscando ADB..."

# Buscar ADB en ubicaciones comunes
ADB=""
if command -v adb &> /dev/null; then
    ADB="adb"
elif [ -f "$HOME/Library/Android/sdk/platform-tools/adb" ]; then
    ADB="$HOME/Library/Android/sdk/platform-tools/adb"
elif [ -f "/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/adb" ]; then
    ADB="/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/adb"
else
    echo "❌ ADB no encontrado"
    echo ""
    echo "Para instalar manualmente:"
    echo "1. Transfiere app/build/outputs/apk/debug/app-debug.apk a tu dispositivo"
    echo "2. Instala el APK"
    echo "3. Abre la app"
    echo "4. El WorkManager se ejecutará en 15 minutos"
    echo ""
    echo "Para ver logs sin adb:"
    echo "- Usa una app como 'Logcat Reader' desde Play Store"
    echo "- Filtra por 'UpdateCheckWorker'"
    exit 1
fi

echo "✓ ADB encontrado: $ADB"
echo ""

# Instalar APK
echo "📦 Instalando APK..."
$ADB install -r app/build/outputs/apk/debug/app-debug.apk

if [ $? -eq 0 ]; then
    echo "✅ APK instalado correctamente"
    echo ""
    echo "🚀 Iniciando aplicación..."
    $ADB shell am start -n com.example.controloperador/.MainActivity
    sleep 2
    
    echo ""
    echo "📊 Estado de WorkManager:"
    echo "────────────────────────────────────────"
    $ADB shell dumpsys jobscheduler | grep -A 5 "UpdateCheckWorker" || echo "Worker programado (se ejecutará en 15 min)"
    echo ""
    
    echo "💡 Para ver logs en tiempo real:"
    echo "   $ADB logcat | grep -E 'UpdateCheckWorker|UpdateRepository'"
    echo ""
    echo "🔄 Para forzar ejecución inmediata:"
    echo "   $ADB shell am broadcast -a androidx.work.diagnostics.REQUEST_DIAGNOSTICS -p com.example.controloperador"
else
    echo "❌ Error instalando APK"
    exit 1
fi
