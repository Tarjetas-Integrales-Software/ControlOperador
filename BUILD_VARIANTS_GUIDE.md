# 🔧 Guía de Build Variants - URLs Automáticas

## ✅ ¿Qué se implementó?

El proyecto ahora **cambia automáticamente** la URL del backend según el tipo de build:

- **Debug** (desarrollo) → `http://172.16.20.10:8000/api/v1/`
- **Release** (producción) → `https://backtransportistas.tarjetasintegrales.mx:806/api/v1/`

## 🚀 Cómo usar

### Para Desarrollo (Debug)

**Opción 1: Desde Android Studio**
1. Asegúrate que esté en modo **debug** (arriba a la izquierda)
2. Presiona **Run** (▶️) o **Debug** (🐛)
3. La app usará automáticamente: `http://172.16.20.10:8000/api/v1/`

**Opción 2: Desde terminal**
```bash
./gradlew installDebug
```

### Para Producción (Release)

**Opción 1: APK firmado desde Android Studio**
1. **Build** → **Generate Signed Bundle / APK**
2. Selecciona **APK**
3. Elige tu keystore
4. Selecciona build variant: **release**
5. La app usará automáticamente: `https://backtransportistas.tarjetasintegrales.mx:806/api/v1/`

**Opción 2: Bundle firmado (Google Play)**
```bash
./gradlew bundleRelease
```

**Opción 3: APK sin firmar (testing)**
```bash
./gradlew assembleRelease
```

## 📁 Archivos modificados

### 1. `app/build.gradle.kts`

```kotlin
defaultConfig {
    // ...
    
    // URL para DEBUG (desarrollo)
    buildConfigField("String", "BASE_URL", "\"http://172.16.20.10:8000/api/v1/\"")
}

buildTypes {
    release {
        // ...
        
        // URL para RELEASE (producción)
        buildConfigField("String", "BASE_URL", "\"https://backtransportistas.tarjetasintegrales.mx:806/api/v1/\"")
    }
}

buildFeatures {
    viewBinding = true
    buildConfig = true  // ← Habilitado para usar BuildConfig
}
```

### 2. `RetrofitClient.kt`

```kotlin
import com.example.controloperador.BuildConfig

object RetrofitClient {
    // Obtiene automáticamente la URL según el build type
    private val BASE_URL = BuildConfig.BASE_URL
    
    // ...
}
```

## 🔍 Verificar qué URL está usando

### Opción 1: En el código

```kotlin
import com.example.controloperador.BuildConfig

Log.d("API", "Base URL: ${BuildConfig.BASE_URL}")
Log.d("API", "Is Debug: ${BuildConfig.DEBUG}")
```

### Opción 2: En Logcat

Cuando la app haga una petición, verás en Logcat:
```
D/OkHttp: --> GET http://172.16.20.10:8000/api/v1/auth/login
```

## 🎯 Build Variants en Android Studio

Para cambiar entre Debug y Release sin necesidad de recompilar:

1. **View** → **Tool Windows** → **Build Variants**
2. Cambia entre:
   - `debug` → Usa desarrollo
   - `release` → Usa producción

## 🔄 Agregar más variantes (opcional)

Si necesitas más entornos (staging, testing, etc.):

```kotlin
// En build.gradle.kts

android {
    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", "\"http://172.16.20.10:8000/api/v1/\"")
        }
        
        release {
            buildConfigField("String", "BASE_URL", "\"https://backtransportistas.tarjetasintegrales.mx:806/api/v1/\"")
        }
        
        // Agregar staging
        create("staging") {
            initWith(getByName("debug"))
            buildConfigField("String", "BASE_URL", "\"https://staging.tarjetasintegrales.mx/api/v1/\"")
        }
    }
}
```

## ⚠️ Importante

### Sync Gradle después de cambios

Cada vez que modifiques `build.gradle.kts`:
1. **File** → **Sync Project with Gradle Files**
2. O haz clic en el ícono 🐘 que aparece arriba

### Limpiar proyecto si hay errores

Si `BuildConfig.BASE_URL` no se reconoce:
```bash
./gradlew clean
./gradlew build
```

O desde Android Studio:
- **Build** → **Clean Project**
- **Build** → **Rebuild Project**

## 📊 Comparación: Antes vs Ahora

### ❌ Antes (Manual)
```kotlin
// Desarrollo
private const val BASE_URL = "http://172.16.20.10:8000/api/v1/"

// Para producción tenías que:
// 1. Comentar la línea de arriba
// 2. Descomentar esta:
// private const val BASE_URL = "https://backtransportistas.tarjetasintegrales.mx:806/api/v1/"
```

**Problemas:**
- 😰 Fácil olvidar cambiar
- 😰 Riesgo de subir URL incorrecta
- 😰 Tedioso comentar/descomentar

### ✅ Ahora (Automático)
```kotlin
private val BASE_URL = BuildConfig.BASE_URL
```

**Ventajas:**
- ✅ Cambio automático según build type
- ✅ Sin riesgo de error
- ✅ Una sola configuración en `build.gradle.kts`
- ✅ Puedes tener múltiples entornos fácilmente

## 🐛 Troubleshooting

### Error: "Unresolved reference: BuildConfig"

**Solución:**
1. Verifica que `buildConfig = true` esté en `build.gradle.kts`
2. Sync Gradle
3. Build → Rebuild Project

### Error: "Cannot access BASE_URL"

**Solución:**
1. Clean Project
2. Sync Gradle
3. Rebuild Project

### La URL no cambia

**Solución:**
1. Verifica el Build Variant actual (View → Tool Windows → Build Variants)
2. Cambia a `release` para producción
3. Reinstala la app (Uninstall del dispositivo primero)

## 📝 Notas adicionales

- El **logging interceptor** de OkHttp está habilitado siempre
- Considera deshabilitarlo en producción para mejor performance:

```kotlin
val loggingInterceptor = HttpLoggingInterceptor().apply {
    level = if (BuildConfig.DEBUG) {
        HttpLoggingInterceptor.Level.BODY
    } else {
        HttpLoggingInterceptor.Level.NONE
    }
}
```

## ✨ Resultado Final

🎉 Ahora puedes desarrollar y generar versiones de producción sin preocuparte por cambiar URLs manualmente. Todo es automático según el tipo de build.

---

**Última actualización:** 24 de octubre de 2025
