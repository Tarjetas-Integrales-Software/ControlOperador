# Control Operador - Sistema de Login

## 🚛 Descripción
Sistema de control para operadores de una empresa de transporte de camiones. La aplicación requiere autenticación con clave numérica de 5 dígitos para acceder al sistema.

## 🔐 Sistema de Autenticación

### Claves de Operador Válidas (Desarrollo)
Para propósitos de desarrollo y prueba, las siguientes claves son válidas:

- `12345`
- `54321`
- `11111`
- `99999`
- `00001`

**Nota:** En producción, estas claves deben provenir de un servidor seguro o base de datos encriptada.

## 🎨 Diseño

### Paleta de Colores Corporativa
- **Primario**: `#1A2332` (Azul gris oscuro - profesional)
- **Acento Dorado**: `#F39C12` (Oro/Naranja - identidad de transporte)
- **Acento Azul**: `#3498DB` (Azul brillante - acciones)
- **Fondo Login**: `#ECF0F1` (Gris claro)

### Tema Ejecutivo y Moderno
- Diseño sobrio y profesional
- Cards con elevación y bordes redondeados
- Iconos de camión y transporte
- Tipografía clara y legible
- Colores que transmiten confianza y autoridad

## 🏗️ Arquitectura

### Componentes Principales

1. **LoginFragment** (`ui/login/LoginFragment.kt`)
   - Interfaz de usuario para ingreso de clave
   - Validación en tiempo real
   - Feedback visual de errores
   - Navegación automática tras login exitoso

2. **LoginViewModel** (`ui/login/LoginViewModel.kt`)
   - Lógica de validación de clave (5 dígitos numéricos)
   - Estados del login usando sealed classes
   - Gestión de códigos de operador válidos

3. **SessionManager** (`ui/login/SessionManager.kt`)
   - Persistencia de sesión con SharedPreferences
   - Timeout de sesión: 8 horas
   - Auto-renovación en `onResume()`
   - Métodos para guardar/verificar/limpiar sesión

### Flujo de Autenticación

```
Inicio App
    ↓
LoginFragment (startDestination)
    ↓
Usuario ingresa clave de 5 dígitos
    ↓
LoginViewModel valida formato y código
    ↓
Si válido → SessionManager.saveOperatorSession()
    ↓
Navegación a HomeFragment
    ↓
MainActivity muestra drawer y toolbar
```

### Protección de Navegación

- **Login Screen**: Drawer y toolbar ocultos
- **Otras Screens**: Requieren sesión activa
- **Back Navigation**: Bloqueada desde Home hacia Login
- **Logout**: Limpia sesión y retorna a Login

## 📱 Funcionalidades

### Pantalla de Login
- ✅ Campo de entrada numérico de 5 dígitos
- ✅ Validación en tiempo real
- ✅ Mensajes de error descriptivos
- ✅ Diseño Material Design moderno
- ✅ Teclado numérico automático
- ✅ Login con tecla Enter/Done

### Navigation Drawer Header (Información del Operador)
- ✅ **Código de Operador**: Muestra el código del operador autenticado
- ✅ **Ruta**: Información de la ruta asignada (C30-C75) *
- ✅ **Unidad**: Número de unidad asignada (00001) *
- ✅ **Fecha y Hora**: Reloj en tiempo real actualizado cada segundo ⏰

\* *Actualmente valores fijos. Preparado para integración con API REST.*

### Gestión de Sesión
- ✅ Persistencia entre reinicios de app
- ✅ Timeout de 8 horas
- ✅ Renovación automática
- ✅ Logout con confirmación

### Navegación Protegida
- ✅ Acceso bloqueado sin autenticación
- ✅ Drawer menu solo para usuarios autenticados
- ✅ Información dinámica en header del drawer
- ✅ Opción de cerrar sesión

## 🚀 Compilación y Ejecución

### Requisitos
- Android Studio Jellyfish | 2023.3.1 o superior
- Kotlin 2.0.21
- Min SDK: 29 (Android 10)
- Target SDK: 36

### Instrucciones

1. Abrir proyecto en Android Studio
2. Sync Gradle files
3. Ejecutar en emulador o dispositivo físico
4. Ingresar una de las claves válidas listadas arriba

## 📚 Estructura del Proyecto

```
app/src/main/
├── java/com/example/controloperador/
│   ├── MainActivity.kt                 # Actividad principal con actualización de header
│   ├── data/
│   │   └── OperatorRepository.kt      # Repositorio para datos del operador (preparado para API)
│   └── ui/
│       ├── login/
│       │   ├── LoginFragment.kt       # UI de login
│       │   ├── LoginViewModel.kt      # Lógica de autenticación
│       │   └── SessionManager.kt      # Gestión de sesión
│       ├── home/                      # Pantalla principal
│       ├── gallery/                   # Pantalla de unidades
│       └── slideshow/                 # Pantalla de reportes
└── res/
    ├── drawable/
    │   ├── ic_truck_logo.xml          # Logo de camión
    │   ├── ic_operator.xml            # Icono de operador
    │   ├── ic_login.xml               # Icono de login
    │   └── ic_logout.xml              # Icono de logout
    ├── layout/
    │   ├── fragment_login.xml         # Layout de login
    │   ├── nav_header_main.xml        # Header con info dinámica del operador
    │   └── ...
    ├── navigation/
    │   └── mobile_navigation.xml      # Grafo de navegación
    └── values/
        ├── colors.xml                 # Paleta corporativa
        ├── strings.xml                # Textos en español
        └── themes.xml                 # Tema ejecutivo
```

## 🔧 Configuración para Producción

### 1. Gestión Segura de Claves
Reemplazar el set hardcoded en `LoginViewModel.kt`:

```kotlin
// Integrar con backend/API
private suspend fun validateWithServer(code: String): Boolean {
    // Llamada a API REST o Room Database
    return authRepository.validateOperatorCode(code)
}
```

### 2. Timeout Configurable
Ajustar en `SessionManager.kt`:

```kotlin
companion object {
    private const val SESSION_TIMEOUT = 8 * 60 * 60 * 1000L // Modificar según necesidad
}
```

### 3. Encriptación de Sesión
Considerar usar EncryptedSharedPreferences para datos sensibles.

## 📖 Patrones y Convenciones

### ViewBinding
```kotlin
private var _binding: FragmentLoginBinding? = null
private val binding get() = _binding!!

override fun onDestroyView() {
    super.onDestroyView()
    _binding = null  // Prevenir memory leaks
}
```

### ViewModel States
```kotlin
sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val operatorCode: String) : LoginState()
    data class Error(val message: String) : LoginState()
}
```

### Observación de LiveData
```kotlin
loginViewModel.loginState.observe(viewLifecycleOwner) { state ->
    when (state) {
        is LoginState.Success -> handleSuccess(state.operatorCode)
        is LoginState.Error -> showError(state.message)
        // ...
    }
}
```

## 🎯 Próximos Pasos Sugeridos

1. **Integración con Backend**
   - API REST para validación de operadores
   - Endpoint para obtener información de ruta y unidad
   - Base de datos de operadores y permisos
   - **Ver `API_INTEGRATION.md` para guía completa** 📡

2. **Seguridad Mejorada**
   - Encriptación de sesión
   - Biometría opcional
   - Intentos de login limitados

3. **Funcionalidades Adicionales**
   - Recuperación de clave
   - Gestión de permisos por rol
   - Auditoría de accesos
   - Notificaciones push
   - Modo offline con sincronización

## 📚 Documentación Adicional

- **`API_INTEGRATION.md`** - Guía completa para integrar la API REST
- **`HEADER_DESIGN.md`** - Especificaciones del diseño del navigation drawer header
- **`DESIGN_GUIDE.md`** - Guía visual y flujos de usuario
- **`.github/copilot-instructions.md`** - Instrucciones para AI coding agents

## 📄 Licencia
Proyecto interno para empresa de transporte.

---

**Versión**: 1.0  
**Última actualización**: Octubre 2025
