#!/bin/bash

# Script para verificar información del APK v1.0.4

echo "🔍 Verificando APK de ControlOperador v1.0.4..."
echo ""

APK_PATH="$HOME/Desktop/ControlOperador-v1.0.4-release.apk"

if [ ! -f "$APK_PATH" ]; then
    echo "❌ Error: No se encontró el APK en $APK_PATH"
    exit 1
fi

echo "📦 Información del APK:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Tamaño del archivo
SIZE=$(ls -lh "$APK_PATH" | awk '{print $5}')
echo "📏 Tamaño: $SIZE"

# Información detallada usando aapt (si está disponible)
if command -v aapt &> /dev/null; then
    echo ""
    echo "📋 Detalles del paquete:"
    aapt dump badging "$APK_PATH" | grep -E "package:|versionCode|versionName|sdkVersion"
    echo ""
    echo "🔐 Firma:"
    aapt dump badging "$APK_PATH" | grep "application-label"
else
    echo ""
    echo "ℹ️  Instala Android SDK Build Tools para ver más detalles"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✅ APK listo para publicar en GitHub"
echo ""
echo "🔗 URL para crear release:"
echo "   https://github.com/Tarjetas-Integrales-Software/ControlOperador/releases/new?tag=v1.0.4"
echo ""
echo "📝 Recuerda:"
echo "   1. Seleccionar tag: v1.0.4"
echo "   2. Título: Control Operador v1.0.4"
echo "   3. Adjuntar: $APK_PATH"
echo "   4. Marcar como 'Latest release'"
echo "   5. Publicar"
echo ""
