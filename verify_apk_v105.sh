#!/bin/bash

echo "🔍 Verificación de APK v1.0.5"
echo "═══════════════════════════════════════════════════"
echo ""

APK_PATH="$HOME/Desktop/ControlOperador-v1.0.5-release.apk"

if [ ! -f "$APK_PATH" ]; then
    echo "❌ Error: No se encontró el APK en $APK_PATH"
    exit 1
fi

echo "✅ APK encontrado: ControlOperador-v1.0.5-release.apk"
echo ""

# Tamaño
SIZE=$(ls -lh "$APK_PATH" | awk '{print $5}')
echo "📏 Tamaño: $SIZE"
echo ""

# Verificar firma (si aapt está disponible)
if command -v aapt &> /dev/null; then
    echo "📋 Información del paquete:"
    echo "─────────────────────────────────────────────────"
    aapt dump badging "$APK_PATH" | grep -E "package:|versionCode|versionName"
    echo ""
    
    echo "📱 Permisos principales:"
    echo "─────────────────────────────────────────────────"
    aapt dump badging "$APK_PATH" | grep "uses-permission" | head -5
    echo ""
fi

# Verificar firma con jarsigner (si está disponible)
if command -v jarsigner &> /dev/null; then
    echo "🔐 Verificación de firma:"
    echo "─────────────────────────────────────────────────"
    jarsigner -verify -verbose -certs "$APK_PATH" 2>&1 | grep -E "jar verified|CN=|SHA256"
    echo ""
fi

# Verificar certificado con keytool
if command -v keytool &> /dev/null; then
    echo "🔒 Certificado del APK:"
    echo "─────────────────────────────────────────────────"
    keytool -printcert -jarfile "$APK_PATH" | grep -E "Owner:|Issuer:|SHA256:" | head -3
    echo ""
fi

echo "═══════════════════════════════════════════════════"
echo "✅ Verificación completada"
echo ""
echo "🔗 Publicar release en:"
echo "   https://github.com/Tarjetas-Integrales-Software/ControlOperador/releases/new?tag=v1.0.5"
echo ""
echo "📝 Checklist:"
echo "   1. Adjuntar: $APK_PATH"
echo "   2. Título: Control Operador v1.0.5 - Diseño Moderno Panel de Respuestas"
echo "   3. Marcar 'Set as the latest release'"
echo "   4. Publicar"
echo ""
echo "🎨 Novedades: Panel de respuestas con gradiente naranja moderno"
echo "📱 Compatible con: v1.0.3 (actualización directa sin desinstalar)"
echo ""
