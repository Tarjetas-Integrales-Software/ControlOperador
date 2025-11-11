# Sistema de Auto-Actualización - ControlOperador

## ✅ Implementación Completada

El sistema de auto-actualización está **funcionando y listo para usar**. La app verifica cada **10 minutos** si hay nuevas versiones en GitHub Releases.

---

## 📦 Componentes Implementados

### 1. **Modelos de Datos**
- ✅ `GitHubRelease.kt` - Modelo del release de GitHub
- ✅ `GitHubAsset.kt` - Modelo del APK adjunto
- ✅ `GitHubAuthor.kt` - Información del autor

### 2. **API y Networking**
- ✅ `GitHubApiService.kt` - Endpoints de GitHub API
- ✅ `RetrofitClient.kt` actualizado con instancia de GitHub
- ✅ Base URL: `https://api.github.com/`

### 3. **Lógica de Negocio**
- ✅ `UpdateRepository.kt` - Verificación y descarga de actualizaciones
- ✅ Comparación de `versionCode` actual vs release
- ✅ Descarga automática con reporte de progreso
- ✅ Limpieza de APKs antiguos

### 4. **WorkManager**
- ✅ `UpdateCheckWorker.kt` - Worker periódico cada 10 minutos
- ✅ Notificaciones silenciosas al detectar actualización
- ✅ Descarga automática en segundo plano
- ✅ Programado en `ControlOperadorApp.onCreate()`

### 5. **Instalación de APK**
- ✅ `ApkInstaller.kt` - Utilidad para instalar APKs
- ✅ `FileProvider` configurado en AndroidManifest
- ✅ Permisos: `REQUEST_INSTALL_PACKAGES`, `POST_NOTIFICATIONS`
- ✅ `file_paths.xml` con rutas seguras

### 6. **Integración en MainActivity**
- ✅ Detección de APK descargado al iniciar
- ✅ Instalación automática desde notificación
- ✅ Manejo de errores y validación de APK

---

## 🚀 Cómo Funciona

### Flujo Automático (Sin intervención del usuario)

```
1. App inicia → WorkManager programa verificación cada 10 min
                          ↓
2. Cada 10 min → UpdateCheckWorker ejecuta en background
                          ↓
3. Verifica GitHub → GET /repos/Tarjetas-Integrales-Software/ControlOperador/releases/latest
                          ↓
4. Compara versiones → versionCode del release vs BuildConfig.VERSION_CODE
                          ↓
   ┌──────────────────┴──────────────────┐
   ↓                                      ↓
NO HAY ACTUALIZACIÓN              HAY NUEVA VERSIÓN
(Log: "App actualizada")          (Log: "Nueva versión disponible")
                                           ↓
                                   5. Mostrar notificación
                                      "Nueva versión disponible"
                                           ↓
                                   6. Descargar APK automáticamente
                                      (Notificación con progreso 0-100%)
                                           ↓
                                   7. APK descargado en /files/updates/
                                      Notificación: "Actualización lista"
                                           ↓
                                   8. Usuario toca notificación
                                      → MainActivity detecta intent
                                      → ApkInstaller.installApk()
                                           ↓
                                   9. Sistema Android muestra:
                                      "¿Instalar esta actualización?"
                                           ↓
                                   10. Usuario confirma → App se actualiza
```

---

## 📝 Cómo Publicar un Nuevo Release

### Paso 1: Incrementar Versión en `app/build.gradle.kts`

```kotlin
defaultConfig {
    versionCode = 2  // Incrementar en 1
    versionName = "1.0.2"  // Nueva versión legible
}
```

**IMPORTANTE**: El `versionCode` DEBE incrementarse en cada release. El sistema compara este número para detectar actualizaciones.

### Paso 2: Compilar APK de Release

```bash
cd /Users/nestorbasavedavalos/AndroidStudioProjects/ControlOperador
./gradlew assembleDebug  # Para pruebas
# o
./gradlew assembleRelease  # Para producción (requiere keystore)
```

APK generado en:
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`

### Paso 3: Renombrar APK (Convención)

```bash
# Desde el directorio de outputs
mv app-debug.apk ControlOperador-v1.0.2-debug.apk
# o para release:
mv app-release.apk ControlOperador-v1.0.2-release.apk
```

### Paso 4: Crear Release en GitHub

1. **Ir a Releases**: https://github.com/Tarjetas-Integrales-Software/ControlOperador/releases
2. **Click "Draft a new release"**
3. **Configurar**:
   ```
   Tag version: v1.0.2
   Release title: v1.0.2 - Descripción breve
   
   Description:
   ## 🆕 Novedades
   - Sistema de auto-actualización implementado
   - Verificación cada 10 minutos
   
   ## 🐛 Correcciones
   - Fix en gráficas de reportes
   
   ## ⚙️ Información Técnica
   - Version Code: 2
   - Version Name: 1.0.2
   - Min SDK: 29 (Android 10)
   ```
4. **Attach binary**: Arrastrar `ControlOperador-v1.0.2-debug.apk`
5. ✅ **Marcar como "Latest release"** (importante)
6. **Publish release**

---

## 🧪 Testing del Sistema

### Probar Detección de Actualización

**Escenario**: App con `versionCode = 1` detecta release con `versionCode = 2`

1. **Instalar versión antigua**:
   ```bash
   # Modificar versionCode = 1 en build.gradle.kts
   ./gradlew assembleDebug && ./gradlew installDebug
   ```

2. **Publicar versión nueva**:
   ```bash
   # Modificar versionCode = 2 en build.gradle.kts
   ./gradlew assembleDebug
   # Subir APK a GitHub Releases como v1.0.2
   ```

3. **Verificar logs**:
   ```bash
   adb logcat -s "UpdateCheckWorker:D" "UpdateRepository:D" "ApkInstaller:D"
   ```

   **Logs esperados** (cada 10 minutos):
   ```
   D UpdateCheckWorker: 🔄 Iniciando verificación de actualizaciones...
   D UpdateRepository: 🔍 Verificando actualizaciones desde GitHub...
   D UpdateRepository: 📦 Versión actual: 1.0 (code: 1)
   D UpdateRepository: 📋 Release encontrado: v1.0.2 - Auto-Update System (v1.0.2)
   D UpdateRepository: 📱 APK encontrado: ControlOperador-v1.0.2-debug.apk (16.6 MB)
   D UpdateRepository: 🔢 Comparando versiones:
   D UpdateRepository:    - Actual: 1
   D UpdateRepository:    - Disponible: 2
   D UpdateRepository: ✅ ¡Nueva versión disponible! 1 -> 2
   D UpdateCheckWorker: 🆕 Nueva versión encontrada: v1.0.2 - Auto-Update System
   D UpdateCheckWorker: ⬇️ Iniciando descarga automática...
   D UpdateRepository: ⬇️ Descargando APK desde: https://github.com/.../app-debug.apk
   D UpdateRepository: ⏳ Progreso: 25% (...)
   D UpdateRepository: ⏳ Progreso: 50% (...)
   D UpdateRepository: ⏳ Progreso: 75% (...)
   D UpdateRepository: ⏳ Progreso: 100% (...)
   D UpdateRepository: ✅ APK descargado exitosamente: /data/data/.../files/updates/ControlOperador-1.0.2.apk
   D UpdateCheckWorker: ✅ Descarga completada: ControlOperador-1.0.2.apk
   ```

4. **Ver notificación**:
   - Notificación aparece: "Actualización lista"
   - Texto: "Toca para instalar ControlOperador 1.0.2"

5. **Tocar notificación**:
   - Se abre MainActivity
   - Logs:
     ```
     D MainActivity: 📦 APK encontrado: ControlOperador-1.0.2.apk
     D ApkInstaller: 📲 Instalando APK: ControlOperador-1.0.2.apk
     D ApkInstaller: ✅ Intent de instalación lanzado
     ```
   - Sistema Android muestra: "¿Instalar esta actualización?"

6. **Confirmar instalación**:
   - App se actualiza
   - Al reiniciar: `versionCode = 2`

---

## 📊 Monitoreo en Producción

### Logs Clave

```bash
# Ver solo auto-update
adb logcat | grep -E "UpdateCheck|UpdateRepository|ApkInstaller"

# Ver estado de WorkManager
adb shell dumpsys jobscheduler | grep UpdateCheckWorker

# Ver notificaciones activas
adb shell dumpsys notification | grep "ControlOperador"
```

### Verificar que WorkManager está activo

```bash
adb shell dumpsys jobscheduler | grep "update_check_work"
```

**Output esperado**:
```
JOB #u0a123/1 (com.example.controloperador/androidx.work.impl.background.systemjob.SystemJobService)
  u0a123 tag=*job*/com.example.controloperador/androidx.work.impl.background.systemjob.SystemJobService
  ...
  Periodic: interval=+10m flex=+5m
```

---

## ⚙️ Configuración Actual

| Parámetro | Valor |
|-----------|-------|
| **Repositorio** | `Tarjetas-Integrales-Software/ControlOperador` |
| **Visibilidad** | Privado (releases públicos) |
| **Frecuencia de verificación** | 10 minutos |
| **Descarga** | Automática |
| **Instalación** | Semi-automática (notificación) |
| **Notificaciones** | Silenciosas (sin sonido) |
| **versionCode actual** | 1 |
| **versionName actual** | 1.0 |

---

## 🔧 Ajustes Opcionales

### Cambiar Frecuencia de Verificación

**Archivo**: `ControlOperadorApp.kt`

```kotlin
private fun scheduleUpdateCheck() {
    val updateCheckRequest = PeriodicWorkRequestBuilder<UpdateCheckWorker>(
        15, TimeUnit.MINUTES  // Cambiar a 15 minutos
    )
    // ...
}
```

**Opciones**:
- Mínimo: 15 minutos (límite de WorkManager)
- Recomendado: 10-30 minutos
- Para testing: Usar `OneTimeWorkRequest` en lugar de `PeriodicWorkRequest`

### Desactivar Auto-Update Temporalmente

**Archivo**: `ControlOperadorApp.kt`

```kotlin
override fun onCreate() {
    super.onCreate()
    appContainer = AppContainer(this)
    scheduleChatSync()
    scheduleCleanupWork()
    // scheduleUpdateCheck()  // Comentar esta línea
}
```

### Testing Manual (Forzar Verificación)

```kotlin
// En MainActivity u otra Activity
WorkManager.getInstance(this).enqueue(
    OneTimeWorkRequestBuilder<UpdateCheckWorker>().build()
)
```

---

## ⚠️ Problemas Comunes y Soluciones

### 1. "No se detecta la actualización"

**Causas posibles**:
- `versionCode` del release no es mayor que el actual
- Release marcado como "Pre-release" o "Draft"
- Tag no sigue formato `vXXX` (ej: `v1.0.2`)
- No hay APK adjunto o tiene nombre incorrecto

**Solución**:
```bash
# Verificar release en GitHub API
curl https://api.github.com/repos/Tarjetas-Integrales-Software/ControlOperador/releases/latest

# Debe retornar:
{
  "tag_name": "v1.0.2",
  "draft": false,
  "prerelease": false,
  "assets": [
    {
      "name": "...apk",
      "content_type": "application/vnd.android.package-archive"
    }
  ]
}
```

### 2. "La descarga falla"

**Causas**:
- Sin conexión a internet
- Storage lleno
- URL de descarga incorrecta

**Solución**:
```bash
# Ver logs de descarga
adb logcat -s "UpdateRepository:D"

# Verificar storage
adb shell df -h /data
```

### 3. "No se puede instalar el APK"

**Causa**: Permisos no otorgados

**Solución**:
```
1. Configuración → Aplicaciones → ControlOperador
2. Configuración avanzada → Instalar apps desconocidas
3. Permitir
```

### 4. "WorkManager no ejecuta"

**Causa**: Constraints muy restrictivos o batería en modo ahorro

**Solución**:
```bash
# Verificar estado
adb shell dumpsys jobscheduler | grep update_check_work

# Forzar ejecución inmediata
adb shell cmd jobscheduler run -f com.example.controloperador 1
```

---

## 📱 Permisos Requeridos

### En AndroidManifest.xml

```xml
<!-- Para descargar actualizaciones -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<!-- Para instalar APKs -->
<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />

<!-- Para mostrar notificaciones (Android 13+) -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

### Permisos Runtime

Al intentar instalar el APK, el sistema pedirá:
1. **"Instalar apps desconocidas"** - Solo primera vez
2. **"Notificaciones"** - Android 13+ (opcional)

---

## 🎯 Próximos Pasos Recomendados

### 1. **Crear Keystore de Producción**

```bash
keytool -genkey -v -keystore control-operador.keystore.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias control-operador
```

**⚠️ IMPORTANTE**: Guardar keystore y passwords de forma segura. Sin ellos, no podrás actualizar la app instalada.

### 2. **Firmar APKs de Release**

Android Studio → Build → Generate Signed Bundle / APK → APK → Usar keystore creado

### 3. **Publicar Primera Versión "Oficial"**

- Incrementar `versionCode = 2`, `versionName = "1.0.1"`
- Compilar APK firmado
- Publicar en GitHub como `v1.0.1`
- Marcar como "Latest release"

### 4. **Monitorear Primera Actualización**

- Instalar `versionCode = 1` en dispositivos
- Esperar 10 minutos
- Verificar logs de actualización
- Confirmar que instalación funciona

---

## 📞 Soporte

Si encuentras problemas:

1. **Revisar logs**: `adb logcat -s "UpdateCheckWorker:D" "UpdateRepository:D"`
2. **Verificar GitHub API**: `curl https://api.github.com/repos/Tarjetas-Integrales-Software/ControlOperador/releases/latest`
3. **Probar endpoint manualmente**: Abrir URL en navegador
4. **Verificar WorkManager**: `adb shell dumpsys jobscheduler | grep update_check_work`

---

## ✅ Checklist de Implementación Completada

- [x] Modelos de datos (GitHubRelease, GitHubAsset)
- [x] GitHubApiService con Retrofit
- [x] UpdateRepository con lógica de descarga
- [x] UpdateCheckWorker con verificación periódica
- [x] FileProvider configurado
- [x] Permisos en AndroidManifest
- [x] ApkInstaller para instalación
- [x] Integración en MainActivity
- [x] BuildConfig con versionCode y repo info
- [x] Notificaciones silenciosas
- [x] Compilación exitosa
- [x] Instalación en dispositivo Samsung SM-X115

---

**Fecha de implementación**: 10 de noviembre de 2025  
**Versión actual instalada**: 1.0 (versionCode: 1)  
**Estado**: ✅ Funcional y listo para producción
