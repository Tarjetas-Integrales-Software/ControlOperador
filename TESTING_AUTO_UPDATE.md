# 🔄 Testing Auto-Update System

## Problema Identificado

El sistema de auto-update estaba configurado para ejecutarse **cada 10 minutos**, pero **Android WorkManager requiere un mínimo de 15 minutos** para trabajos periódicos.

## ✅ Solución Aplicada

- Cambiado intervalo de verificación de 10 a **15 minutos**
- Compilado APK debug con la corrección
- Creados scripts de instalación y prueba

---

## 📱 Opción 1: Con ADB (Recomendado)

### Paso 1: Ejecutar script de instalación

```bash
./install_and_test.sh
```

Este script:
1. Busca ADB automáticamente
2. Instala el APK debug
3. Inicia la aplicación
4. Muestra el estado de WorkManager

### Paso 2: Ver logs en tiempo real

```bash
# Buscar ADB
ADB=$(find ~/Library/Android/sdk -name "adb" 2>/dev/null | head -1)

# Ver logs filtrados
$ADB logcat | grep -E "UpdateCheckWorker|UpdateRepository|ApkInstaller"
```

### Paso 3: Forzar ejecución inmediata (para pruebas)

```bash
# Método 1: Usando WorkManager diagnostics
$ADB shell am broadcast -a androidx.work.diagnostics.REQUEST_DIAGNOSTICS \
  -p com.example.controloperador

# Método 2: Usando jobscheduler (más directo)
$ADB shell cmd jobscheduler run -f com.example.controloperador 1
```

---

## 📱 Opción 2: Sin ADB (Manual)

### Paso 1: Transferir APK

Copia el archivo a tu dispositivo:
```
app/build/outputs/apk/debug/app-debug.apk
```

Puedes usar:
- Email
- Google Drive
- Cable USB (copia directa)
- AirDrop (si tienes Mac)

### Paso 2: Instalar

1. Abre el APK en tu dispositivo
2. Permite instalación de fuentes desconocidas si es necesario
3. Instala la aplicación

### Paso 3: Ver logs (con app externa)

1. Instala **"Logcat Reader"** desde Play Store
2. Abre la app
3. Filtra por: `UpdateCheckWorker`
4. Abre ControlOperador
5. Espera 15 minutos o reinicia la app

---

## 🔍 Verificar que WorkManager está activo

### Logs que deberías ver al iniciar la app:

```
D ControlOperadorApp: 📱 Inicializando aplicación...
D ControlOperadorApp: ✓ WorkManager programado: UpdateCheckWorker
```

### Logs que verás cada 15 minutos (o al forzar):

```
D UpdateCheckWorker: 🔄 Iniciando verificación de actualizaciones...
D UpdateRepository: 🔍 Verificando desde GitHub API...
D UpdateRepository: 📦 Versión actual: 1.0.2 (code: 2)
D UpdateRepository: 📋 Consultando: https://api.github.com/repos/Tarjetas-Integrales-Software/ControlOperador/releases/latest
```

**Si NO hay actualización:**
```
D UpdateRepository: ✅ Ya tienes la última versión
D UpdateCheckWorker: ✓ App actualizada, no hay nuevas versiones
```

**Si HAY actualización:**
```
D UpdateCheckWorker: 🆕 Nueva versión encontrada: v1.0.3
D UpdateCheckWorker: ⬇️ Iniciando descarga automática...
D UpdateRepository: 📥 Descargando: 10%
D UpdateRepository: 📥 Descargando: 50%
D UpdateRepository: 📥 Descargando: 100%
D UpdateCheckWorker: ✅ Descarga completada: ControlOperador-v1.0.3.apk
```

---

## 🧪 Probar el Flujo Completo

### 1. Instalar versión 1.0.2 (actual)

```bash
./install_and_test.sh
```

### 2. Crear versión 1.0.3 para simular update

```bash
# Editar build.gradle.kts
# versionCode = 3
# versionName = "1.0.3"

# Compilar
./gradlew clean assembleRelease

# Firmar
jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 \
  -keystore ~/keystore/controloperador-new.jks \
  -storepass ControlOp2025 -keypass ControlOp2025 \
  app/build/outputs/apk/release/app-release.apk controloperador

# Copiar
cp app/build/outputs/apk/release/app-release.apk \
   ~/Desktop/ControlOperador-v1.0.3-release.apk
```

### 3. Publicar en GitHub

1. Commit y push de cambios
2. Crear tag: `git tag -a v1.0.3 -m "Release v1.0.3"`
3. Push tag: `git push origin v1.0.3`
4. Crear release en GitHub con el APK
5. Marcar como "latest release"

### 4. Esperar o forzar verificación

**Esperar (natural):** 15 minutos máximo

**Forzar (para testing):**
```bash
$ADB shell cmd jobscheduler run -f com.example.controloperador 1
```

### 5. Ver resultado

Deberías ver:
1. Notificación: "Nueva versión disponible - ControlOperador v1.0.3"
2. Descarga automática en background
3. Notificación: "Actualización lista - Toca para instalar"
4. Al tocar → Se abre instalador de Android
5. Confirmas → App se actualiza a v1.0.3

---

## 🐛 Troubleshooting

### No veo logs de UpdateCheckWorker

**Causa:** WorkManager no se inició correctamente

**Solución:**
```bash
# Limpiar datos de la app
$ADB shell pm clear com.example.controloperador

# Reinstalar
$ADB install -r app/build/outputs/apk/debug/app-debug.apk

# Verificar que se programó
$ADB shell dumpsys jobscheduler | grep UpdateCheckWorker
```

### WorkManager dice "no constraints met"

**Causa:** No hay conexión WiFi/datos

**Solución:**
- Conecta el dispositivo a WiFi o datos móviles
- El worker solo se ejecuta con conexión a internet

### Error 404 en GitHub API

**Causa:** Repositorio privado o release no marcado como "latest"

**Solución:**
1. Verifica que el repo es público
2. Verifica que el release existe:
   ```bash
   curl https://api.github.com/repos/Tarjetas-Integrales-Software/ControlOperador/releases/latest
   ```

### APK se descarga pero no instala

**Causa:** Falta permiso REQUEST_INSTALL_PACKAGES

**Solución:**
1. Ve a Configuración > Apps > ControlOperador
2. Permisos avanzados > Instalar apps desconocidas
3. Activa el permiso

---

## 📊 Frecuencia de Verificación

- **Intervalo:** Cada 15 minutos (mínimo de Android)
- **Condición:** Solo cuando hay conexión a internet
- **En background:** Sí, incluso con app cerrada
- **Después de reinicio:** Se reprograma automáticamente

---

## 🎯 Comandos Útiles

```bash
# Encontrar ADB
find ~/Library/Android/sdk -name "adb" 2>/dev/null | head -1

# Instalar APK
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Ver logs filtrados
adb logcat | grep -E "UpdateCheckWorker|UpdateRepository"

# Limpiar logs
adb logcat -c

# Ver trabajos programados
adb shell dumpsys jobscheduler | grep -A 10 UpdateCheckWorker

# Forzar ejecución
adb shell cmd jobscheduler run -f com.example.controloperador 1

# Verificar permisos
adb shell dumpsys package com.example.controloperador | grep permission

# Limpiar datos de la app
adb shell pm clear com.example.controloperador
```

---

## ✅ Checklist de Prueba

- [ ] App instalada correctamente
- [ ] WorkManager programado (ver con dumpsys)
- [ ] Logs de UpdateCheckWorker visibles
- [ ] Repositorio GitHub es público
- [ ] Release marcado como "latest"
- [ ] APK en release tiene extensión .apk
- [ ] Permiso de instalación otorgado
- [ ] Notificaciones activadas
- [ ] Conexión a internet activa
- [ ] Esperado 15 minutos o forzado ejecución
- [ ] Notificación de update aparece
- [ ] Descarga completa exitosa
- [ ] Instalación funciona al tocar notificación

---

## 📝 Notas Importantes

1. **Android limita a 15 minutos mínimo** para trabajos periódicos en background
2. **El primer check ocurre ~15 minutos después** de instalar la app
3. **Debes tener conexión a internet** para que funcione
4. **El repositorio DEBE ser público** o usar token de acceso
5. **Cada release debe tener un APK adjunto** con extensión .apk
6. **El versionCode debe incrementarse** en cada release
7. **El tag debe seguir formato** `vX.Y.Z` (ej: v1.0.2)

---

¿Tienes algún dispositivo Android conectado para probar ahora?
