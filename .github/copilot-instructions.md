# ControlOperador - AI Coding Instructions

## Project Overview
ControlOperador is an Android application for a transport company (empresa de transportes de camiones). Built with Kotlin, it follows the Navigation Drawer template with MVVM architecture, Material Design components, and implements session-based authentication for operators.

## Business Context
- **Purpose**: Control system for truck operators in a public transport company
- **Users**: Truck operators who authenticate with a 5-digit numeric code
- **Theme**: Executive and modern design with transport company branding (dark blue-grey primary, gold accents)

## Architecture Patterns

### Authentication System
- **LoginFragment**: Entry point requiring 5-digit operator code validation
- **SessionManager**: Manages operator session persistence using SharedPreferences
- **Session Duration**: 8-hour timeout with auto-renewal on app resume
- **Navigation Protection**: Login screen blocks access to all other screens until authenticated

### MVVM with Fragment-based Navigation
- **MainActivity**: Single activity hosting navigation drawer with AppBar and FAB
- **Fragments**: Each screen (Login, Home, Gallery/Unidades, Slideshow/Reportes) is a Fragment with associated ViewModel
- **ViewModels**: Handle UI-related data using LiveData pattern and sealed classes for state management
- **Navigation**: Centralized in `mobile_navigation.xml` with Navigation Component

### Key Architectural Files
- `MainActivity.kt`: Entry point with session verification, drawer control, and logout handling
- `LoginFragment.kt` + `LoginViewModel.kt`: Authentication UI and logic with 5-digit validation
- `SessionManager.kt`: Session persistence and timeout management
- `mobile_navigation.xml`: Navigation graph with login as startDestination
- `activity_main_drawer.xml`: Drawer menu with logout option

## Code Conventions

### View Binding Pattern
All fragments follow this binding pattern:
```kotlin
private var _binding: FragmentHomeBinding? = null
private val binding get() = _binding!!

override fun onDestroyView() {
    super.onDestroyView()
    _binding = null  // Always nullify to prevent memory leaks
}
```

### Package Structure
```
com.example.controloperador/
├── MainActivity.kt
├── data/
│   ├── api/
│   │   ├── ApiService.kt           (Retrofit interface)
│   │   ├── RetrofitClient.kt       (Singleton Retrofit configuration)
│   │   ├── AuthRepository.kt       (API calls & error handling)
│   │   └── model/
│   │       └── ApiResponse.kt      (Response models)
│   ├── model/                      (Data models)
│   └── MessageRepository.kt        (Mock data repository)
└── ui/
    ├── login/
    │   ├── LoginFragment.kt
    │   ├── LoginViewModel.kt       (Uses AuthRepository)
    │   └── SessionManager.kt
    ├── home/
    │   ├── HomeFragment.kt
    │   └── HomeViewModel.kt
    ├── chat/
    └── voice/
```

### ViewModel Pattern
ViewModels use sealed classes for state management and expose LiveData:
```kotlin
sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val operatorCode: String) : LoginState()
    data class Error(val message: String) : LoginState()
}

private val _loginState = MutableLiveData<LoginState>()
val loginState: LiveData<LoginState> = _loginState
```

### Authentication Flow (REST API)
1. App starts → `mobile_navigation.xml` shows `LoginFragment` (startDestination)
2. User enters 5-digit code → `LoginViewModel` validates format locally
3. API call → `AuthRepository.login()` sends POST to Laravel backend `/api/v1/auth/login`
4. Backend validates → Checks operator code in database and returns operator data
5. On success → `SessionManager.saveOperatorSession()` stores session in SharedPreferences
6. Navigation → `findNavController().navigate(R.id.action_login_to_home)` with popUpTo to prevent back navigation
7. `MainActivity` hides drawer/toolbar on login screen, shows on authenticated screens
8. Session persists across app restarts with 8-hour timeout

**API Integration:**
- **Backend**: Laravel 7 REST API (see `BACKEND_API_SETUP.md`)
- **Client**: Retrofit 2.9.0 + OkHttp 4.12.0
- **Base URL**: Configurable in `RetrofitClient.BASE_URL`
  - Emulador: `http://10.0.2.2:8000/api/`
  - Dispositivo físico: `http://192.168.X.X:8000/api/`
  - Producción: `https://tu-dominio.com/api/`

## Build System

### Gradle Configuration
- **Version Catalog**: Dependencies managed in `gradle/libs.versions.toml`
- **Build Features**: ViewBinding enabled globally
- **Target SDK**: 36 (latest), Min SDK: 29
- **Kotlin**: Version 2.0.21 with JVM target 11

### Key Dependencies
- Navigation Component (fragment-ktx, ui-ktx)
- Material Design Components
- AndroidX Lifecycle (livedata-ktx, viewmodel-ktx)
- View Binding (enabled in build.gradle.kts)
- **Retrofit 2.9.0**: HTTP client for API calls
- **OkHttp 4.12.0**: HTTP logging interceptor
- **Gson 2.10.1**: JSON serialization/deserialization

## Development Workflows

### Adding New Screens
1. Create package under `ui/` (e.g., `ui/newscreen/`)
2. Add Fragment + ViewModel following existing pattern
3. Add destination to `mobile_navigation.xml`
4. Add menu item to drawer menu (if needed)
5. Update string resources in Spanish

### Navigation Setup
Top-level destinations defined in `MainActivity.onCreate()`:
```kotlin
appBarConfiguration = AppBarConfiguration(
    setOf(R.id.nav_login, R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow),
    drawerLayout
)
```

Destination change listener controls UI visibility:
```kotlin
navController.addOnDestinationChangedListener { _, destination, _ ->
    when (destination.id) {
        R.id.nav_login -> {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            supportActionBar?.hide()
        }
        else -> {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            supportActionBar?.show()
        }
    }
}
```

### Session Management
**SessionManager** provides:
- `saveOperatorSession(code: String)`: Store authenticated operator
- `isSessionActive()`: Check if session exists and hasn't expired
- `getOperatorCode()`: Retrieve current operator code
- `clearSession()`: Logout and clear all session data
- `renewSession()`: Extend session time (called in `MainActivity.onResume()`)

### Logout Flow
1. User selects "Cerrar Sesión" from drawer menu
2. `MainActivity.showLogoutDialog()` shows confirmation dialog
3. On confirm → `SessionManager.clearSession()` + navigate to login
4. Login screen appears, drawer/toolbar hidden

### Testing Structure
- Unit tests: `app/src/test/` with JUnit 4
- UI tests: `app/src/androidTest/` with Espresso
- Basic test setup exists but project appears early-stage

## Resource Management

### Color Scheme (Transport Company Theme)
```xml
primary_dark: #1A2332     (Dark blue-grey for headers/primary)
accent_gold: #F39C12      (Gold/Orange for accents/CTAs)
accent_blue: #3498DB      (Bright blue for actions)
login_background: #ECF0F1 (Light grey backgrounds)
```

### Layout Structure
- `activity_main.xml`: DrawerLayout with included app_bar_main
- `app_bar_main.xml`: CoordinatorLayout with Toolbar + FAB
- `fragment_login.xml`: Login screen with MaterialCardView and TextInputLayout
- `fragment_*.xml`: Individual fragment layouts
- `nav_header_main.xml`: Navigation drawer header (shows operator code)

### Navigation Menu
Drawer menu items:
- Home, Unidades (Gallery), Reportes (Slideshow)
- Logout option in separate "Sesión" group

### String Resources
All strings are in **Spanish** for transport company operators. Key patterns:
- Login: `login_title`, `operator_code_hint`, `error_invalid_code`
- Session: `session_expired`, `logout_confirm`
- Menu: `menu_home`, `menu_gallery` (Unidades), `menu_slideshow` (Reportes)

## Integration Points
- **Navigation Component**: Handles fragment transactions and back stack
- **Material Design**: Navigation drawer, FAB, and theming
- **Data Binding**: Fragment ViewBinding for type-safe view references
- **Lifecycle**: ViewModel survival across configuration changes

## Common Patterns
- Fragment lifecycle management with proper binding cleanup
- ViewModel observation in `onCreateView()`
- Navigation configuration in single activity
- Resource externalization in strings.xml
- **Coroutines**: All API calls use `suspend` functions with `viewModelScope.launch`
- **Error Handling**: AuthRepository returns sealed `Result` class (Success, Error, NetworkError, Timeout)
- **Loading States**: LoginViewModel exposes Loading state for UI progress indicators

## API Communication

### Retrofit Configuration (`RetrofitClient.kt`)
- **Singleton pattern**: Single instance for entire app
- **Interceptors**: HttpLoggingInterceptor for debugging
- **Timeouts**: 30s connect/read/write
- **Converters**: Gson for JSON

### AuthRepository Pattern
```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val code: Int? = null) : Result<Nothing>()
    object NetworkError : Result<Nothing>()
    object Timeout : Result<Nothing>()
}
```

Methods:
- `suspend fun login(operatorCode: String): Result<LoginResponse>`
- `suspend fun verify(operatorCode: String): Result<VerifyResponse>`
- `suspend fun logout(operatorCode: String): Result<Unit>`
- `suspend fun checkConnection(): Boolean`

### Error Handling Strategy
1. **Validation local** en ViewModel (formato 5 dígitos)
2. **API call** con try-catch para exceptions de red
3. **HTTP codes** manejados específicamente:
   - 401: Clave incorrecta
   - 422: Validación fallida
   - 429: Rate limiting
   - 500: Error del servidor
4. **UI feedback** con mensajes en español descriptivos

### Network Requirements
- **Permissions**: `INTERNET`, `ACCESS_NETWORK_STATE` en AndroidManifest
- **Cleartext Traffic**: Enabled for localhost development (API level 28+)
- **Base URL**: Must be configured in `RetrofitClient.BASE_URL` before deployment

## Backend Integration
- **Framework**: Laravel 7.x REST API
- **Documentation**: See `BACKEND_API_SETUP.md` for complete setup
- **Endpoints**:
  - `POST /api/v1/auth/login` - Authenticate operator
  - `POST /api/v1/auth/verify` - Check if code exists
  - `POST /api/v1/auth/logout` - Close session
  - `GET /api/health` - Server health check
- **Response Format**: JSON with `success`, `message`, `data` fields
- **Authentication**: Simple code-based (no tokens required for this version)