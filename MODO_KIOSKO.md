# Modo Kiosko - ControlOperador

## 🚛 Características Implementadas

La aplicación **ControlOperador** ahora funciona en **Modo Kiosko** optimizado para dispositivos dedicados en camiones de transporte público.

### ✅ Funcionalidades del Modo Kiosko

1. **Pantalla Completa Inmersiva**
   - Oculta automáticamente la barra de estado (hora, batería, notificaciones)
   - Oculta la barra de navegación (botones Atrás, Home, Recientes)
   - Se re-aplica automáticamente cuando la app recupera el foco

2. **Pantalla Siempre Encendida**
   - La pantalla permanece encendida mientras la app está activa
   - Ideal para dispositivos montados en camiones
   - Ahorra batería al evitar encendido/apagado constante

3. **Orientación Forzada a Landscape**
   - La app siempre se muestra en orientación horizontal
   - Optimizada para tablets y pantallas de camión
   - No rota aunque el dispositivo se voltee

4. **Protección contra Salida Accidental**
   - El botón "Atrás" está controlado:
     - En login: Muestra mensaje de modo kiosko
     - En home: Pide confirmación antes de cerrar sesión
     - En otras pantallas: Navega normalmente dentro de la app
   - Solo se puede salir mediante "Cerrar Sesión"

5. **Single Task Mode**
   - Solo puede haber una instancia de la app activa
   - Si se abre nuevamente, vuelve a la instancia existente

## 📱 Configuración Básica (Ya Implementada)

La app ya incluye todas estas configuraciones automáticamente:

```kotlin
// MainActivity.kt
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)
    
    // IMPORTANTE: enableKioskMode() DEBE llamarse DESPUÉS de setContentView()
    enableKioskMode() // ✅ Correcto
    
    // NO hacer esto:
    // enableKioskMode() antes de setContentView() ❌ Causa NullPointerException
}

private fun enableKioskMode() {
    try {
        // Manejo seguro con try-catch
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(...)
        } else {
            window.decorView.systemUiVisibility = ...
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
```

```xml
<!-- AndroidManifest.xml -->
- screenOrientation="landscape"
- launchMode="singleTask"
- configChanges="orientation|screenSize|keyboardHidden"
- WAKE_LOCK permission
```

## ⚠️ Solución de Problemas Comunes

### Error: NullPointerException en insetsController

**Problema:**
```
java.lang.NullPointerException: Attempt to invoke virtual method 
'android.view.WindowInsetsController getWindowInsetsController()' on a null object reference
```

**Solución:**
El método `enableKioskMode()` debe llamarse **DESPUÉS** de `setContentView()`, no antes.

```kotlin
// ❌ INCORRECTO
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableKioskMode() // ❌ Window aún no inicializado
    setContentView(binding.root)
}

// ✅ CORRECTO
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(binding.root)
    enableKioskMode() // ✅ Window ya inicializado
}
```

## 🔒 Configuración Avanzada de Kiosko (Opcional)

Para un kiosko **completamente bloqueado** en el dispositivo:

### Opción 1: Android Kiosk Mode (Requiere Device Owner)

1. **Instalar la app como Device Owner:**
   ```bash
   # Primero, hacer factory reset al dispositivo
   # Durante la configuración inicial, NO agregar cuenta Google
   
   # Instalar la app
   adb install app-debug.apk
   
   # Establecer como Device Owner (solo funciona en dispositivos sin cuenta)
   adb shell dpm set-device-owner com.example.controloperador/.MainActivity
   ```

2. **Habilitar Lock Task Mode en código:**
   ```kotlin
   // Agregar a MainActivity.kt en onCreate()
   val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
   if (dpm.isDeviceOwnerApp(packageName)) {
       startLockTask()
   }
   ```

### Opción 2: Usar App Launcher de Terceros

Apps recomendadas para kiosko:
- **KioWare** (de pago, muy completo)
- **SureLock** (de pago, para empresas)
- **Kiosk Browser Lockdown** (gratuito, básico)
- **Fully Kiosk Browser** (freemium)

### Opción 3: Samsung Knox (Dispositivos Samsung)

Si usan tablets Samsung, Knox ofrece:
- Knox Configure
- Knox Manage
- Modo kiosko nativo muy robusto

## 🎯 Configuración para Producción

### 1. Deshabilitar Opciones de Desarrollador
```bash
adb shell settings put global development_settings_enabled 0
```

### 2. Ocultar Barra de Estado Permanentemente
```bash
adb shell settings put global policy_control immersive.full=*
```

### 3. Deshabilitar Botones de Hardware (si es posible)
Depende del fabricante del dispositivo.

### 4. Configurar Auto-Start en Boot
```xml
<!-- Agregar a AndroidManifest.xml -->
<receiver android:name=".BootReceiver" android:enabled="true" android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED"/>
    </intent-filter>
</receiver>
```

```kotlin
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val startIntent = Intent(context, MainActivity::class.java)
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(startIntent)
        }
    }
}
```

## 🔧 Comandos ADB Útiles

```bash
# Ver si la app está en modo kiosko
adb shell dumpsys activity | grep -i "lockTaskMode"

# Salir del modo kiosko (para desarrollo)
adb shell am task lock stop

# Forzar orientación landscape
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 1

# Ver configuración actual
adb shell settings list system
```

## 🚨 Modo de Emergencia

Si el dispositivo queda bloqueado en modo kiosko durante desarrollo:

```bash
# Opción 1: Desinstalar la app
adb uninstall com.example.controloperador

# Opción 2: Salir de Lock Task Mode
adb shell am task lock stop

# Opción 3: Reiniciar en Safe Mode
# Mantener presionado botón de encendido + volumen abajo al arrancar
```

## 📊 Testing del Modo Kiosko

### Verificar que funciona:
1. ✅ Barras del sistema ocultas en pantalla completa
2. ✅ Botón "Atrás" no sale de la app
3. ✅ Botón "Home" no funciona (requiere Lock Task Mode)
4. ✅ Pantalla no se apaga sola
5. ✅ App siempre en landscape
6. ✅ Solo se puede salir con "Cerrar Sesión"

### Probar escenarios:
- Presionar botón Home
- Presionar botón Recientes
- Presionar botón Atrás múltiples veces
- Rotar el dispositivo
- Dejar inactivo por tiempo prolongado
- Abrir notificaciones (deslizar desde arriba)

## 🎨 Personalización Adicional

### Cambiar orientación a Portrait:
```xml
<!-- AndroidManifest.xml -->
android:screenOrientation="portrait"
```

### Permitir salida con botón Atrás:
```kotlin
// En MainActivity.kt, comentar o eliminar:
// override fun onBackPressed() { ... }
```

### Deshabilitar pantalla siempre encendida:
```kotlin
// En enableKioskMode(), comentar:
// window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
```

## 📞 Soporte

Para más información o problemas con el modo kiosko:
- Revisar logs: `adb logcat | grep ControlOperador`
- Verificar permisos en Configuración > Apps > ControlOperador
- Consultar documentación de Android Enterprise para kiosks corporativos

---

**Nota:** El modo kiosko básico ya está **100% funcional** sin configuración adicional. Las opciones avanzadas son para dispositivos dedicados que requieren bloqueo completo del sistema.
