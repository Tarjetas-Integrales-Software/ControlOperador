# 📱 Modo Kiosko Completo - Pantalla Inmersiva

## ✅ Implementación Completada

Se ha mejorado el **modo kiosko** para ocultar completamente las barras del sistema (barra de estado y barra de navegación) desde el inicio de la aplicación.

---

## 🎯 Funcionalidades Implementadas

### 1. **Ocultamiento Completo de Barras del Sistema**

#### Barra de Estado (Superior)
- ❌ **Oculta**: Hora, fecha, batería, señal, notificaciones
- ✅ Más espacio para contenido de la aplicación

#### Barra de Navegación (Inferior)
- ❌ **Oculta**: Botones de Atrás, Home, Multitarea
- ✅ Previene salida accidental de la app

### 2. **Modo Inmersivo Sticky (Pegajoso)**

**Comportamiento:**
1. Al iniciar, las barras están **completamente ocultas**
2. Si el usuario desliza desde el borde, las barras aparecen **temporalmente**
3. Después de **3 segundos de inactividad**, se ocultan automáticamente
4. Al cambiar de app y volver, las barras se ocultan automáticamente

### 3. **Compatibilidad Multi-Versión Android**

#### Android 11+ (API 30+)
```kotlin
// Usa WindowInsetsController (API moderna)
window.insetsController?.hide(
    WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars()
)
controller.systemBarsBehavior = BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
```

#### Android 10 y anteriores (API 29-)
```kotlin
// Usa systemUiVisibility (API legacy)
window.decorView.systemUiVisibility = (
    SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    or SYSTEM_UI_FLAG_FULLSCREEN
    or SYSTEM_UI_FLAG_HIDE_NAVIGATION
    // ... más flags
)
```

### 4. **Listeners de Auto-Restauración**

**Android 11+:**
- `setOnApplyWindowInsetsListener`: Detecta cuando las barras aparecen y programa su ocultamiento

**Android 10-:**
- `setOnSystemUiVisibilityChangeListener`: Detecta cambios en visibilidad y restaura el modo inmersivo

### 5. **Restauración al Recuperar Foco**

```kotlin
override fun onWindowFocusChanged(hasFocus: Boolean) {
    super.onWindowFocusChanged(hasFocus)
    if (hasFocus) {
        enableKioskMode() // Re-aplicar modo inmersivo
    }
}
```

**Se ejecuta cuando:**
- Usuario regresa de otra app
- Usuario presiona el botón de encendido y desbloquea
- Se cierra un diálogo del sistema
- Se completa una llamada telefónica

---

## 🎨 Configuración en Temas (themes.xml)

```xml
<style name="Theme.ControlOperador.NoActionBar">
    <item name="windowActionBar">false</item>
    <item name="windowNoTitle">true</item>
    
    <!-- Pantalla completa inmersiva -->
    <item name="android:windowFullscreen">true</item>
    <item name="android:windowDrawsSystemBarBackgrounds">true</item>
    <item name="android:windowTranslucentStatus">false</item>
    <item name="android:windowTranslucentNavigation">false</item>
    
    <!-- Barras transparentes -->
    <item name="android:statusBarColor">@android:color/transparent</item>
    <item name="android:navigationBarColor">@android:color/transparent</item>
</style>
```

---

## 📋 Flujo de Ejecución

1. **onCreate()**
   - `showAndroidIdDialog()` - Muestra ID del dispositivo por 50s
   - `enableKioskMode()` - Oculta barras del sistema
   
2. **onWindowFocusChanged(hasFocus: true)**
   - Re-aplica modo kiosko al recuperar foco

3. **Usuario desliza desde borde**
   - Barras aparecen temporalmente (transient)
   - Listener detecta aparición
   - Después de 3 segundos → barras se ocultan automáticamente

4. **onBackPressed()**
   - Bloqueado en pantalla de login
   - Requiere logout explícito para salir

---

## ⚙️ Configuraciones Adicionales

### Mantener Pantalla Encendida
```kotlin
window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
```
✅ **Útil para tablets montadas en camiones**

### Orientación Landscape Forzada
```xml
android:screenOrientation="landscape"
```
✅ **Orientación horizontal bloqueada**

### Launch Mode Single Task
```xml
android:launchMode="singleTask"
```
✅ **Solo una instancia de la actividad**

---

## 🧪 Pruebas Realizadas

### ✅ Compilación Exitosa
```bash
BUILD SUCCESSFUL in 4s
38 actionable tasks: 16 executed, 22 up-to-date
```

### ⚠️ Warnings (No Críticos)
- Deprecation warnings de APIs antiguas (necesarias para compatibilidad Android 10-)
- No afectan funcionalidad

---

## 📱 Comportamiento Esperado

### Al Iniciar la App
1. **Pantalla completamente llena** sin barras visibles
2. Diálogo de Android ID por 50 segundos
3. Contenido ocupa todo el espacio disponible

### Durante Uso Normal
- ✅ Sin barras visibles
- ✅ Navegación por drawer lateral
- ✅ No se puede salir con botón atrás

### Al Deslizar desde Bordes
- 📱 Barras aparecen **temporalmente**
- ⏱️ Se ocultan **automáticamente en 3s**
- 🔄 Modo inmersivo se restaura

### Al Cambiar de App y Volver
- 🔄 Modo kiosko se **restaura automáticamente**
- ✅ Barras se ocultan inmediatamente

---

## 🚀 Próximos Pasos (Opcional)

### Para Kiosko Más Estricto (Modo Admin)

1. **Device Owner / Profile Owner**
   ```kotlin
   // Requiere configuración MDM (Mobile Device Management)
   dpm.setLockTaskPackages(admin, arrayOf(packageName))
   startLockTask()
   ```

2. **Bloquear Home Button Permanentemente**
   - Requiere permisos de administrador del dispositivo
   - Configuración a nivel de MDM

3. **Deshabilitar Panel de Notificaciones**
   ```kotlin
   // Solo con permisos de sistema
   dpm.setStatusBarDisabled(admin, true)
   ```

### Instalación como Device Owner
```bash
adb shell dpm set-device-owner com.example.controloperador/.DeviceAdminReceiver
```

---

## 📝 Archivos Modificados

1. ✅ `MainActivity.kt`
   - Mejorado `enableKioskMode()` con listeners
   - Agregado listener de WindowInsets (Android 11+)
   - Agregado listener de SystemUiVisibility (Android 10-)
   - `onWindowFocusChanged()` restaura modo kiosko

2. ✅ `themes.xml`
   - Agregadas configuraciones de pantalla completa
   - Barras del sistema transparentes
   - `windowFullscreen = true`

3. ✅ `AndroidManifest.xml`
   - Ya configurado correctamente
   - Theme: `Theme.ControlOperador.NoActionBar`
   - Orientación: `landscape`

---

## 🎓 Conceptos Clave

### SYSTEM_UI_FLAG_IMMERSIVE_STICKY
- Las barras se ocultan automáticamente después de aparecer
- Usuario puede hacerlas aparecer con swipe
- Se ocultan solas después de tocar la pantalla

### WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
- Versión moderna de IMMERSIVE_STICKY
- Barras aparecen al deslizar desde borde
- Se ocultan automáticamente

### setDecorFitsSystemWindows(false)
- El contenido se dibuja **debajo** de las barras del sistema
- Permite pantalla verdaderamente completa

---

## ✨ Resultado Final

```
┌─────────────────────────────────┐
│                                 │ ← Sin barra de estado
│                                 │
│        CONTENIDO DE LA APP      │
│          (Pantalla Llena)       │
│                                 │
│                                 │
└─────────────────────────────────┘ ← Sin barra de navegación
```

**Experiencia de usuario tipo kiosko profesional para tablets en camiones de transporte público.**

---

**Fecha de Implementación:** 27 de octubre de 2025  
**Versión Android Mínima:** API 29 (Android 10)  
**Versión Android Target:** API 36 (Android 14+)
