# Guía de Auto-Actualización desde GitHub Releases

## 📋 Tabla de Contenidos
1. [Descripción General](#descripción-general)
2. [Configuración en GitHub](#configuración-en-github)
3. [Arquitectura del Sistema](#arquitectura-del-sistema)
4. [Configuración en Android](#configuración-en-android)
5. [Flujo de Actualización](#flujo-de-actualización)
6. [Seguridad y Permisos](#seguridad-y-permisos)
7. [Testing y Deployment](#testing-y-deployment)
8. [Troubleshooting](#troubleshooting)

---

## 📖 Descripción General

Este sistema permite que la aplicación **ControlOperador** se actualice automáticamente descargando APKs desde **GitHub Releases**.

### Características
- ✅ Verificación automática cada **10 minutos**
- ✅ Descarga en segundo plano con notificación de progreso
- ✅ Instalación semi-automática (requiere confirmación del usuario)
- ✅ Notificaciones con release notes
- ✅ Configuración para habilitar/deshabilitar auto-update
- ✅ Soporte para releases públicos y privados (con token)
- ✅ Verificación de integridad del APK
- ✅ Manejo de errores de red y storage

---

## 🔧 Configuración en GitHub

### 1. Estructura del Repositorio

**Opción A: Repositorio Principal (Recomendado)**
```
Tarjetas-Integrales-Software/ControlOperador
├── .github/
├── app/
├── gradle/
└── README.md
```

**Opción B: Repositorio Separado (Más Seguro)**
```
Tarjetas-Integrales-Software/ControlOperador-Releases
├── README.md
└── Releases tab (solo APKs)
```

**Recomendación:** Usar un repositorio separado si el código fuente es privado pero quieres que los APKs estén accesibles para dispositivos sin autenticación compleja.

---

### 2. Convenciones de Versionado

**IMPORTANTE:** El sistema compara `versionCode` (número entero) del APK con el release de GitHub.

#### En `app/build.gradle.kts`:
```kotlin
android {
    defaultConfig {
        versionCode = 7      // Incrementar en cada release
        versionName = "1.0.7" // Versión legible para humanos
    }
}
```

#### Naming Convention para Releases:
```
Tag:         v1.0.7
Release Name: v1.0.7 - Mejoras en sistema de reportes
APK Name:    ControlOperador-v1.0.7-release.apk
```

**Reglas:**
- Tag DEBE empezar con `v` seguido del `versionName`
- APK DEBE seguir formato: `ControlOperador-v{version}-release.apk`
- El `versionCode` DEBE incrementarse en cada release (no puede repetirse)

---

### 3. Crear un Release en GitHub

#### Paso 1: Compilar APK de Release

```bash
# En terminal desde el directorio del proyecto
./gradlew assembleRelease

# APK generado en:
# app/build/outputs/apk/release/app-release.apk
```

#### Paso 2: Firmar el APK (OBLIGATORIO para instalación)

**Opción A: Firma Automática con Android Studio**

1. Abrir Android Studio
2. Build → Generate Signed Bundle / APK
3. Seleccionar **APK**
4. Crear o usar keystore existente:
   ```
   Key store path: /path/to/keystore.jks
   Key store password: ******
   Key alias: control-operador
   Key password: ******
   ```
5. Seleccionar **release** build variant
6. Guardar APK firmado

**Opción B: Firma Manual con Terminal**

```bash
# Crear keystore (solo primera vez)
keytool -genkey -v -keystore control-operador.keystore.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias control-operador

# Firmar APK
jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 \
  -keystore control-operador.keystore.jks \
  app/build/outputs/apk/release/app-release-unsigned.apk \
  control-operador

# Alinear APK (optimización)
zipalign -v 4 \
  app/build/outputs/apk/release/app-release-unsigned.apk \
  ControlOperador-v1.0.7-release.apk
```

**⚠️ IMPORTANTE:** Guarda el keystore y passwords en un lugar seguro. Si lo pierdes, no podrás actualizar la app instalada.

#### Paso 3: Renombrar APK

```bash
# Desde app/build/outputs/apk/release/
mv app-release.apk ControlOperador-v1.0.7-release.apk
```

#### Paso 4: Publicar en GitHub

1. Ir a **Releases** en GitHub: `https://github.com/{owner}/{repo}/releases`
2. Click en **Draft a new release**
3. Configurar release:

```
Tag version: v1.0.7
Target: main (o la rama que uses)
Release title: v1.0.7 - Mejoras en sistema de reportes

Description:
## 🆕 Novedades
- ✨ Gráficas de reportes con MPAndroidChart
- 🔄 Botón de sincronización manual
- 🐛 Fix: orden de gráficas ahora es correcto

## 🔧 Mejoras
- RecyclerView scrolleable con altura fija
- Manejo de apellido_materno vacío

## ⚙️ Información Técnica
- Version Code: 7
- Version Name: 1.0.7
- Min SDK: 29 (Android 10)
- Target SDK: 36

## 📥 Instalación
1. Descarga `ControlOperador-v1.0.7-release.apk`
2. Si tienes la app instalada, actualiza directamente
3. Si es primera instalación, habilita "Instalar apps desconocidas"
```

4. **Attach binaries**: Arrastrar `ControlOperador-v1.0.7-release.apk`
5. ✅ Marcar como **Latest release** (importante para la API)
6. Click en **Publish release**

---

### 4. Configurar Acceso a Releases

#### Releases Públicos (Recomendado para esta app)

✅ **Sin configuración adicional**. La API de GitHub permite acceso público a releases:
```
GET https://api.github.com/repos/{owner}/{repo}/releases/latest
```

#### Releases Privados (Requiere Token)

Si el repositorio es privado:

1. Crear Personal Access Token:
   - GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)
   - Generate new token
   - Scopes necesarios: `repo` (Full control of private repositories)
   - Copiar token: `ghp_xxxxxxxxxxxxxxxxxxxx`

2. Almacenar token en Android:
   ```kotlin
   // En RetrofitClient.kt o local.properties
   private const val GITHUB_TOKEN = "ghp_xxxxxxxxxxxxxxxxxxxx"
   ```

3. Agregar header en requests:
   ```kotlin
   @Headers("Authorization: Bearer $GITHUB_TOKEN")
   ```

**⚠️ SEGURIDAD:** No commitear tokens en el repositorio. Usar `local.properties` o variables de entorno.

---

## 🏗️ Arquitectura del Sistema

### Componentes Android

```
app/src/main/java/com/example/controloperador/
├── data/
│   ├── api/
│   │   ├── GitHubApiService.kt        # API de GitHub Releases
│   │   └── model/
│   │       ├── GitHubRelease.kt       # Modelo de respuesta
│   │       └── GitHubAsset.kt         # Modelo de APK asset
│   ├── repository/
│   │   └── UpdateRepository.kt        # Lógica de actualización
│   └── update/
│       ├── AppUpdateManager.kt        # Coordinador principal
│       ├── ApkDownloader.kt           # Descarga de APK
│       └── ApkInstaller.kt            # Instalación de APK
├── workers/
│   └── UpdateCheckWorker.kt           # WorkManager cada 10 min
└── ui/
    └── update/
        ├── UpdateViewModel.kt         # Estados de actualización
        ├── UpdateDialogFragment.kt    # UI de notificación
        └── UpdateNotificationHelper.kt # Notificaciones del sistema
```

### Flujo de Datos

```
┌─────────────────────────────────────────────────────────────────┐
│  WorkManager (cada 10 min)                                      │
│  └─> UpdateCheckWorker                                          │
└─────────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│  UpdateRepository                                               │
│  ├─> GitHubApiService.getLatestRelease()                        │
│  ├─> Comparar versionCode actual vs release                     │
│  └─> Emitir LiveData<UpdateState>                               │
└─────────────────────────────────────────────────────────────────┘
                            │
                ┌───────────┴───────────┐
                ▼                       ▼
    ┌───────────────────┐   ┌───────────────────┐
    │ No hay actualización│   │ Actualización disponible│
    └───────────────────┘   └───────────────────┘
                                        │
                                        ▼
                        ┌───────────────────────────────┐
                        │ Notificación / Dialog         │
                        │ "Nueva versión disponible"    │
                        └───────────────────────────────┘
                                        │
                        ┌───────────────┴───────────────┐
                        ▼                               ▼
            Usuario: "Actualizar"        Usuario: "Después"
                        │
                        ▼
            ┌───────────────────────────┐
            │ ApkDownloader              │
            │ ├─> Descargar APK          │
            │ ├─> Verificar SHA256       │
            │ └─> Progress notifications │
            └───────────────────────────┘
                        │
                        ▼
            ┌───────────────────────────┐
            │ ApkInstaller               │
            │ ├─> FileProvider URI       │
            │ └─> Intent.ACTION_VIEW     │
            └───────────────────────────┘
                        │
                        ▼
            ┌───────────────────────────┐
            │ Sistema Android            │
            │ "¿Instalar esta app?"      │
            └───────────────────────────┘
```

---

## ⚙️ Configuración en Android

### 1. Dependencias (app/build.gradle.kts)

```kotlin
dependencies {
    // Retrofit para GitHub API (ya instalado)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    
    // WorkManager para verificación periódica (agregar si no existe)
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    
    // Coroutines (ya instalado)
    implementation(libs.kotlinx.coroutines.android)
}
```

### 2. Permisos en AndroidManifest.xml

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    
    <!-- Permisos existentes -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    
    <!-- NUEVOS: Para auto-update -->
    <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
    <uses-permission android:name="android.permission.DOWNLOAD_WITHOUT_NOTIFICATION" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    
    <application
        android:name=".ControlOperadorApp"
        ... >
        
        <!-- FileProvider para instalar APK -->
        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="${applicationId}.fileprovider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_paths" />
        </provider>
        
        <!-- Actividades, etc. -->
    </application>
</manifest>
```

### 3. FileProvider Paths (res/xml/file_paths.xml)

Crear archivo `app/src/main/res/xml/file_paths.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<paths xmlns:android="http://schemas.android.com/apk/res/android">
    <!-- Directorio interno para APKs descargados -->
    <files-path
        name="apk_updates"
        path="updates/" />
    
    <!-- Caché para descargas temporales -->
    <cache-path
        name="apk_cache"
        path="updates/" />
</paths>
```

### 4. Configuración de Versionado

**Actualizar `app/build.gradle.kts`:**

```kotlin
android {
    defaultConfig {
        applicationId = "com.example.controloperador"
        minSdk = 29
        targetSdk = 36
        
        // IMPORTANTE: Incrementar en cada release
        versionCode = 7
        versionName = "1.0.7"
        
        // BuildConfig para acceder a versión desde código
        buildConfigField("int", "VERSION_CODE", "${versionCode}")
        buildConfigField("String", "VERSION_NAME", "\"${versionName}\"")
        buildConfigField("String", "GITHUB_REPO_OWNER", "\"Tarjetas-Integrales-Software\"")
        buildConfigField("String", "GITHUB_REPO_NAME", "\"ControlOperador\"")
    }
    
    buildFeatures {
        viewBinding = true
        buildConfig = true  // IMPORTANTE: Habilitar BuildConfig
    }
}
```

---

## 🔄 Flujo de Actualización

### 1. Verificación Automática

```
Cada 10 minutos:
1. UpdateCheckWorker se ejecuta en background
2. Llama a GitHub API: GET /repos/{owner}/{repo}/releases/latest
3. Compara versionCode del release vs BuildConfig.VERSION_CODE
4. Si hay nueva versión:
   a. Muestra notificación persistente
   b. Emite LiveData en UpdateRepository
   c. Si MainActivity está activa, muestra dialog
```

### 2. Descarga de APK

```
Usuario presiona "Actualizar":
1. ApkDownloader inicia descarga en background
2. Guarda APK en: /data/data/com.example.controloperador/files/updates/
3. Muestra notificación con progreso (0-100%)
4. Verifica integridad (opcional: SHA256 del release)
5. Al completar, lanza instalación
```

### 3. Instalación

```
Después de descargar:
1. ApkInstaller crea URI con FileProvider
2. Lanza Intent.ACTION_VIEW con MIME type application/vnd.android.package-archive
3. Sistema Android muestra "¿Instalar esta app?"
4. Usuario confirma → App se actualiza
5. Al reiniciar, nueva versión está activa
```

### 4. Manejo de Errores

```
Errores comunes:
- Sin internet → Retry en siguiente verificación (10 min)
- APK corrupto → Reintento de descarga (máx 3 intentos)
- Storage lleno → Limpiar APKs antiguos
- Usuario cancela instalación → Notificación persiste
- Permisos denegados → Mostrar instrucciones
```

---

## 🔒 Seguridad y Permisos

### Permisos Runtime (Android 10+)

**1. Instalar APKs desconocidos**

```kotlin
// Verificar si el usuario ya dio permiso
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    if (!requireContext().packageManager.canRequestPackageInstalls()) {
        // Mostrar diálogo explicativo
        // Luego redirigir a configuración:
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            data = Uri.parse("package:${requireContext().packageName}")
        }
        startActivity(intent)
    }
}
```

**2. Notificaciones (Android 13+)**

```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    if (ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            REQUEST_CODE_NOTIFICATIONS
        )
    }
}
```

### Verificación de Integridad

**Opcional: Verificar SHA256 del APK**

1. Calcular hash del APK descargado:
```kotlin
fun calculateSHA256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { fis ->
        val buffer = ByteArray(8192)
        var bytesRead: Int
        while (fis.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}
```

2. Publicar hash en release notes:
```markdown
## 🔒 Verificación
SHA256: a3f5b8c9d2e1f0a7b4c3d2e1f0a9b8c7d6e5f4a3b2c1d0e9f8a7b6c5d4e3f2a1
```

3. Comparar antes de instalar:
```kotlin
val downloadedHash = calculateSHA256(apkFile)
if (downloadedHash != expectedHash) {
    throw SecurityException("APK integrity check failed")
}
```

---

## 🧪 Testing y Deployment

### Fase 1: Testing Local

**1. Setup de prueba:**
```bash
# Crear rama de testing
git checkout -b feature/auto-update

# Incrementar versionCode
# En app/build.gradle.kts: versionCode = 8

# Compilar APK de prueba
./gradlew assembleDebug
```

**2. Crear release de prueba en GitHub:**
- Tag: `v1.0.8-beta`
- Marcar como **Pre-release** (no es latest)
- Subir APK de debug (firmado con debug keystore)

**3. Modificar temporalmente el código:**
```kotlin
// En GitHubApiService.kt
@GET("repos/{owner}/{repo}/releases/tags/v1.0.8-beta")  // Forzar pre-release
suspend fun getTestRelease(): GitHubRelease
```

**4. Instalar versión anterior (versionCode 7)**
**5. Probar actualización a v1.0.8-beta**
**6. Verificar:**
- ✅ Notificación aparece
- ✅ Descarga funciona
- ✅ Instalación exitosa
- ✅ App reinicia con nueva versión

### Fase 2: Release de Producción

**1. Incrementar versionCode en `build.gradle.kts`**
```kotlin
versionCode = 8
versionName = "1.0.8"
```

**2. Compilar APK firmado:**
```bash
./gradlew assembleRelease
# o usar Android Studio: Build → Generate Signed Bundle / APK
```

**3. Renombrar APK:**
```bash
mv app-release.apk ControlOperador-v1.0.8-release.apk
```

**4. Crear release en GitHub:**
- Tag: `v1.0.8`
- Title: `v1.0.8 - Auto-Update System`
- Description: Release notes detallados
- Subir APK
- ✅ Marcar como **Latest release**

**5. Desplegar en dispositivos:**

```bash
# Instalar manualmente primera vez (con versionCode < 8)
adb install ControlOperador-v1.0.7-release.apk

# Esperar 10 minutos (o forzar verificación)
# La app detectará v1.0.8 y actualizará
```

### Fase 3: Monitoreo

**Logs a revisar:**
```kotlin
// En UpdateCheckWorker.kt
Log.d("UpdateCheck", "Versión actual: ${BuildConfig.VERSION_CODE}")
Log.d("UpdateCheck", "Versión disponible: ${release.versionCode}")
Log.d("UpdateCheck", "¿Actualizar?: ${needsUpdate}")

// En ApkDownloader.kt
Log.d("ApkDownload", "Progreso: ${progress}%")
Log.d("ApkDownload", "Descarga completada: ${apkFile.absolutePath}")

// En ApkInstaller.kt
Log.d("ApkInstall", "Instalando desde URI: $apkUri")
```

**Logcat filter:**
```bash
adb logcat -s "UpdateCheck:D" "ApkDownload:D" "ApkInstall:D"
```

---

## 🛠️ Troubleshooting

### Problema 1: "No se puede instalar la app"

**Causa:** Permisos de instalación no otorgados

**Solución:**
```
1. Configuración → Aplicaciones → ControlOperador
2. Configuración avanzada → Instalar apps desconocidas
3. Permitir
```

**Código para detectar:**
```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    if (!packageManager.canRequestPackageInstalls()) {
        // Mostrar instrucciones al usuario
    }
}
```

---

### Problema 2: "La descarga falla silenciosamente"

**Causa:** Storage insuficiente o permisos de escritura

**Solución:**
```kotlin
// Verificar storage disponible
val freeSpace = context.filesDir.usableSpace
val apkSize = release.assets[0].size
if (freeSpace < apkSize * 1.5) {  // 50% margen
    throw IOException("Storage insuficiente")
}

// Limpiar APKs antiguos
val updatesDir = File(context.filesDir, "updates")
updatesDir.listFiles()?.forEach { it.delete() }
```

---

### Problema 3: "La verificación nunca se ejecuta"

**Causa:** WorkManager constraints muy restrictivos

**Diagnóstico:**
```bash
# Ver estado de Workers
adb shell dumpsys jobscheduler | grep UpdateCheckWorker
```

**Solución:**
```kotlin
// En UpdateCheckWorker setup
val constraints = Constraints.Builder()
    .setRequiredNetworkType(NetworkType.CONNECTED)  // Solo conectividad
    // NO agregar: setRequiresCharging, setRequiresBatteryNotLow
    .build()
```

---

### Problema 4: "API de GitHub retorna 403 Forbidden"

**Causa:** Rate limiting de GitHub API (60 req/hora sin auth)

**Solución:**
```kotlin
// Implementar cache de última verificación
val lastCheck = preferences.getLong("last_update_check", 0)
val now = System.currentTimeMillis()
if (now - lastCheck < 10 * 60 * 1000) {  // 10 minutos
    return  // Skip verificación
}

// O usar token de GitHub (5000 req/hora)
@Headers("Authorization: Bearer $GITHUB_TOKEN")
```

---

### Problema 5: "APK descargado no coincide con release"

**Causa:** Caché de GitHub CDN

**Solución:**
```kotlin
// Agregar timestamp a URL de descarga
val apkUrl = release.assets[0].browserDownloadUrl + "?t=${System.currentTimeMillis()}"

// O usar asset_id en lugar de browser_download_url
@Streaming
@GET("repos/{owner}/{repo}/releases/assets/{assetId}")
@Headers("Accept: application/octet-stream")
suspend fun downloadAsset(@Path("assetId") assetId: Long): ResponseBody
```

---

## 📝 Checklist Final

### Antes de Primera Publicación

- [ ] **Keystore creado y guardado de forma segura**
- [ ] **versionCode y versionName actualizados en build.gradle.kts**
- [ ] **BuildConfig habilitado para acceder a VERSION_CODE**
- [ ] **Permisos agregados en AndroidManifest.xml**
- [ ] **FileProvider configurado con file_paths.xml**
- [ ] **GitHubApiService implementado con endpoint correcto**
- [ ] **UpdateRepository con lógica de comparación de versiones**
- [ ] **WorkManager programado para cada 10 minutos**
- [ ] **UI de notificación implementada (dialog o notification)**
- [ ] **ApkDownloader con progreso funcional**
- [ ] **ApkInstaller con FileProvider URI**
- [ ] **Testing en dispositivo físico (no emulador)**

### En Cada Release

- [ ] **Incrementar versionCode en 1**
- [ ] **Actualizar versionName según convención**
- [ ] **Compilar APK firmado con keystore de producción**
- [ ] **Renombrar APK con formato: ControlOperador-v{version}-release.apk**
- [ ] **Crear tag en Git: v{version}**
- [ ] **Publicar release en GitHub con descripción**
- [ ] **Marcar como "Latest release"**
- [ ] **Probar actualización en dispositivo con versión anterior**
- [ ] **Documentar cambios en CHANGELOG.md**

### Monitoreo Post-Release

- [ ] **Verificar logs de UpdateCheckWorker**
- [ ] **Confirmar que notificación aparece en dispositivos**
- [ ] **Revisar tasa de éxito de descargas**
- [ ] **Monitorear crashes relacionados con instalación**
- [ ] **Validar que versionCode se incrementó correctamente**

---

## 🔗 Referencias

### APIs de GitHub
- [Releases API](https://docs.github.com/en/rest/releases/releases?apiVersion=2022-11-28)
- [Assets Download](https://docs.github.com/en/rest/releases/assets?apiVersion=2022-11-28)
- [Rate Limiting](https://docs.github.com/en/rest/overview/resources-in-the-rest-api#rate-limiting)

### Android Documentation
- [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)
- [FileProvider](https://developer.android.com/reference/androidx/core/content/FileProvider)
- [Install Unknown Apps](https://developer.android.com/reference/android/provider/Settings#ACTION_MANAGE_UNKNOWN_APP_SOURCES)
- [Package Installation](https://developer.android.com/guide/topics/manifest/provider-element#Permissions)

### Firma de APKs
- [App Signing](https://developer.android.com/studio/publish/app-signing)
- [jarsigner Documentation](https://docs.oracle.com/javase/8/docs/technotes/tools/windows/jarsigner.html)
- [zipalign](https://developer.android.com/studio/command-line/zipalign)

---

## 📞 Soporte

Si encuentras problemas durante la implementación:

1. **Revisar logs de Logcat** con filtro `UpdateCheck`, `ApkDownload`, `ApkInstall`
2. **Verificar estado de WorkManager**: `adb shell dumpsys jobscheduler`
3. **Probar endpoint de GitHub** manualmente: `curl -i https://api.github.com/repos/{owner}/{repo}/releases/latest`
4. **Validar permisos**: Settings → Apps → ControlOperador → Permissions

---

## 🚀 Próximos Pasos

Después de leer esta guía:

1. **Responder preguntas iniciales** sobre configuración preferida
2. **Implementar código Kotlin** (modelos, API service, repository, workers, UI)
3. **Configurar FileProvider** y permisos
4. **Crear primer release de prueba** en GitHub
5. **Testing en dispositivo físico**
6. **Deployment de primera versión con auto-update**

---

**Fecha de creación:** 10 de noviembre de 2025  
**Versión del documento:** 1.0  
**Autor:** GitHub Copilot + Equipo ControlOperador
