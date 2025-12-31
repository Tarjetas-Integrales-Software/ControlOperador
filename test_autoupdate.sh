#!/bin/bash

# Script para probar el sistema de auto-actualización
# Requiere dispositivo Android conectado por USB con depuración activada

# Colores para output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Buscar ADB
ADB=""
if command -v adb &> /dev/null; then
    ADB="adb"
elif [ -f "$ANDROID_HOME/platform-tools/adb" ]; then
    ADB="$ANDROID_HOME/platform-tools/adb"
elif [ -f "$HOME/Library/Android/sdk/platform-tools/adb" ]; then
    ADB="$HOME/Library/Android/sdk/platform-tools/adb"
else
    echo -e "${RED}❌ ADB no encontrado${NC}"
    echo "Por favor instala Android SDK o agrega adb a tu PATH"
    exit 1
fi

echo -e "${BLUE}=== Test Auto-Update Sistema ===${NC}\n"

# 1. Verificar dispositivo conectado
echo -e "${YELLOW}1. Verificando dispositivo conectado...${NC}"
DEVICE=$($ADB devices | grep -w "device" | head -1 | awk '{print $1}')
if [ -z "$DEVICE" ]; then
    echo -e "${RED}❌ No hay dispositivo conectado${NC}"
    echo "Conecta tu dispositivo y activa la depuración USB"
    exit 1
fi
echo -e "${GREEN}✓ Dispositivo conectrado: $DEVICE${NC}\n"

# 2. Instalar APK debug
echo -e "${YELLOW}2. Instalando APK debug...${NC}"
$ADB install -r app/build/outputs/apk/debug/app-debug.apk
if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Error instalando APK${NC}"
    exit 1
fi
echo -e "${GREEN}✓ APK instalado${NC}\n"

# 3. Iniciar la aplicación
echo -e "${YELLOW}3. Iniciando aplicación...${NC}"
$ADB shell am start -n com.example.controloperador/.MainActivity
sleep 3
echo -e "${GREEN}✓ App iniciada${NC}\n"

# 4. Verificar WorkManager
echo -e "${YELLOW}4. Verificando WorkManager...${NC}"
$ADB shell dumpsys jobscheduler | grep -A 10 "UpdateCheckWorker"
echo ""

# 5. Forzar ejecución del UpdateCheckWorker
echo -e "${YELLOW}5. Forzando ejecución de UpdateCheckWorker...${NC}"
$ADB shell cmd jobscheduler run -f com.example.controloperador 1
echo -e "${GREEN}✓ Worker forzado${NC}\n"

# 6. Mostrar logs en tiempo real
echo -e "${YELLOW}6. Monitoreando logs (Ctrl+C para salir)...${NC}"
echo -e "${BLUE}Filtrando: UpdateCheckWorker, UpdateRepository, ApkInstaller${NC}\n"
$ADB logcat -c  # Limpiar logs
$ADB logcat | grep -E "UpdateCheckWorker|UpdateRepository|ApkInstaller|WorkManager"
