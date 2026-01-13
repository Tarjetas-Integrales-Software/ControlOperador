#!/bin/bash

# Script para instalar APK debug con botón de actualización manual

echo "📦 Instalando versión con botón de actualización..."
echo ""

# Buscar ADB
ADB=""
if command -v adb &> /dev/null; then
    ADB="adb"
elif [ -f "$HOME/Library/Android/sdk/platform-tools/adb" ]; then
    ADB="$HOME/Library/Android/sdk/platform-tools/adb"
else
    echo "❌ ADB no encontrado"
    echo ""
    echo "Instala manualmente:"
    echo "1. Copia app/build/outputs/apk/debug/app-debug.apk a la tablet"
    echo "2. Instala desde la tablet"
    echo "3. Abre la app"
    echo "4. Toca el ícono de descarga en la barra superior"
    exit 1
fi

# Instalar
$ADB install -r app/build/outputs/apk/debug/app-debug.apk

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Instalación exitosa"
    echo ""
    echo "📋 Instrucciones:"
    echo "1. Abre ControlOperador en la tablet"
    echo "2. En la barra superior, verás un ícono de descarga ⬇"
    echo "3. Toca ese ícono"
    echo "4. Verás: 'Se descargó la versión 1.0.3. ¿Deseas instalarla ahora?'"
    echo "5. Toca 'Instalar'"
    echo "6. Confirma en el instalador de Android"
    echo "7. ¡Listo! App actualizada a v1.0.3"
    echo ""
else
    echo "❌ Error instalando"
    echo ""
    echo "Prueba manualmente:"
    echo "- Copia app/build/outputs/apk/debug/app-debug.apk a la tablet"
    echo "- Instala desde archivos"
fi
