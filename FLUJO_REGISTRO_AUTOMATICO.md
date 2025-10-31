# 🔄 Sistema de Registro Automático de Entrada/Salida

## ✅ Implementación Completa

### 📋 Flujo de Datos Automático

```
┌─────────────────────────────────────────────────────────────────┐
│                    FLUJO COMPLETO DEL SISTEMA                   │
└─────────────────────────────────────────────────────────────────┘

1️⃣ INICIO DE SESIÓN (LOGIN)
   ┌────────────────┐
   │ LoginFragment  │
   │                │
   │ 1. Usuario     │──────► LoginViewModel.validateOperatorCode()
   │    ingresa     │                │
   │    código      │                ▼
   │    (5 dígitos) │        AuthRepository.login()
   │                │                │
   │                │                ▼
   │                │        POST /api/v1/secomsa/auth/login
   │                │                │
   │                │                ▼
   │                │        LoginResponse {
   │                │          operator: {
   │                │            operator_code: "12345"
   │                │            nombre: "Juan"
   │                │            apellido_paterno: "Pérez"
   │                │            apellido_materno: "García"
   │                │          }
   │                │        }
   │                │                │
   │ 2. Success     │◄───────────────┘
   │    handleLogin │
   │    Success()   │
   │                │
   │ 3. Guardar     │──────► SessionManager.saveOperatorSession()
   │    sesión      │
   │                │
   │ 4. REGISTRAR   │──────► AttendanceRepository.registerEntry()
   │    ENTRADA     │                │
   │    (AUTOMÁTICO)│                ▼
   │                │        INSERT INTO reportes (
   │                │          operatorCode,    -- "12345"
   │                │          nombre,          -- "Juan"
   │                │          apellidoPaterno, -- "Pérez"
   │                │          apellidoMaterno, -- "García"
   │                │          entrada,         -- Date.now()
   │                │          salida,          -- NULL
   │                │          tiempoOperando,  -- 0.0
   │                │          enviado          -- 0
   │                │        )
   │                │                │
   │                │                ▼
   │                │        ✅ Registro ID: 123
   │                │        📅 Entrada: 29/10/2025 08:30:00
   │                │
   │ 5. Navegar     │──────► HomeFragment
   │    al Home     │
   └────────────────┘


2️⃣ TRABAJO DEL OPERADOR
   ┌────────────────┐
   │ SlideshowFrag. │
   │  (Reportes)    │
   │                │
   │ • Muestra      │◄───────┐
   │   tabla con    │        │
   │   registros    │        │ LiveData<List<AttendanceLog>>
   │                │        │ (actualización automática)
   │ • Gráfica      │        │
   │   de barras    │        │
   │                │        │
   │ • Gráfica      │        │
   │   de dona      │        │
   │                │        │
   │ • Estado:      │        │
   │   ✓ Enviado    │        │
   │   ⚠ Pendiente  │        │
   └────────────────┘        │
                             │
                      ┌──────┴────────┐
                      │ Room Database │
                      │ tabla:        │
                      │  "reportes"   │
                      └───────────────┘


3️⃣ CIERRE DE SESIÓN (LOGOUT)
   ┌────────────────┐
   │ MainActivity   │
   │                │
   │ 1. Usuario     │──────► showLogoutDialog()
   │    presiona    │                │
   │    "Cerrar     │                ▼
   │    Sesión"     │        ¿Confirmar?
   │                │                │
   │                │                ▼ Sí
   │ 2. Confirma    │──────► performLogout()
   │                │                │
   │                │                ▼
   │ 3. Obtener     │        operatorCode = sessionManager.getOperatorCode()
   │    código      │                │
   │                │                ▼
   │ 4. REGISTRAR   │──────► registerExitAndSync(operatorCode)
   │    SALIDA      │                │
   │    (AUTOMÁTICO)│                │
   │                │        ┌───────┴────────┐
   │                │        │                │
   │                │        ▼                ▼
   │                │   A. Registrar    B. Sincronizar
   │                │      Salida          Reportes
   │                │        │                │
   │                │        ▼                │
   │                │   repository           │
   │                │   .registerExit()      │
   │                │        │                │
   │                │        ▼                │
   │                │   UPDATE reportes       │
   │                │   SET                   │
   │                │     salida = now(),    │
   │                │     tiempoOperando =   │
   │                │       (salida-entrada) │
   │                │   WHERE                 │
   │                │     operatorCode='...' │
   │                │     AND salida IS NULL │
   │                │        │                │
   │                │        ▼                │
   │                │   ✅ Salida registrada │
   │                │   📅 Salida: 29/10/2025│
   │                │       17:45:00         │
   │                │   ⏱ Tiempo: 9.25 hrs   │
   │                │        │                │
   │                │        └────────────────┤
   │                │                         │
   │                │                         ▼
   │                │              C. Sync reporte actual
   │                │                         │
   │                │                         ▼
   │                │              syncSingleReport(exitLog)
   │                │                         │
   │                │                         ▼
   │                │              POST /api/v1/secomsa/reportes
   │                │              {
   │                │                reportes: [{
   │                │                  id: 123,
   │                │                  operator_code: "12345",
   │                │                  nombre: "Juan",
   │                │                  apellido_paterno: "Pérez",
   │                │                  apellido_materno: "García",
   │                │                  entrada: "2025-10-29T08:30:00Z",
   │                │                  salida: "2025-10-29T17:45:00Z",
   │                │                  tiempo_operando: 9.25
   │                │                }]
   │                │              }
   │                │                         │
   │                │                         ▼
   │                │              ¿Exitoso?
   │                │                │       │
   │                │         ┌──────┴───────┴──────┐
   │                │         │ Sí                  │ No
   │                │         ▼                     ▼
   │                │    UPDATE reportes       (Queda enviado=0
   │                │    SET enviado=1          para reintento)
   │                │    WHERE id=123
   │                │         │
   │                │         └──────────────────────┤
   │                │                                 │
   │                │                                 ▼
   │                │              D. Sync reportes pendientes
   │                │                                 │
   │                │                                 ▼
   │                │              syncUnsentReports()
   │                │                                 │
   │                │                                 ▼
   │                │              SELECT * FROM reportes
   │                │              WHERE enviado=0
   │                │                AND salida IS NOT NULL
   │                │                                 │
   │                │                                 ▼
   │                │              ¿Hay pendientes?
   │                │                │       │
   │                │         ┌──────┴───────┴──────┐
   │                │         │ Sí                  │ No
   │                │         ▼                     ▼
   │                │    POST /api/v1/      (Nada que enviar)
   │                │    secomsa/reportes
   │                │    con TODOS los
   │                │    registros
   │                │         │
   │                │         ▼
   │                │    Marcar exitosos
   │                │    como enviado=1
   │                │         │
   │                │         └──────────────────────┤
   │                │                                 │
   │ 5. Mostrar     │                                 ▼
   │    resultado   │◄────────────────────────────────┘
   │                │
   │  Toast:        │
   │  "✓ Sesión     │
   │   cerrada      │
   │   N reportes   │
   │   sincronizados│
   │                │
   │ 6. Limpiar     │──────► sessionManager.clearSession()
   │    sesión      │
   │                │
   │ 7. Navegar a   │──────► LoginFragment
   │    Login       │
   └────────────────┘
```

## 📊 Estructura de la Tabla "reportes"

```sql
CREATE TABLE reportes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    operatorCode TEXT NOT NULL,           -- "12345"
    nombre TEXT NOT NULL,                 -- "Juan"
    apellidoPaterno TEXT NOT NULL,        -- "Pérez"
    apellidoMaterno TEXT NOT NULL,        -- "García"
    entrada INTEGER NOT NULL,             -- 1730200200000 (timestamp)
    salida INTEGER,                       -- 1730233500000 (timestamp) o NULL
    tiempoOperando REAL NOT NULL DEFAULT 0.0,  -- 9.25 (horas)
    enviado INTEGER NOT NULL DEFAULT 0    -- 0 o 1
);
```

### 📝 Ejemplo de Registro

**Al hacer LOGIN:**
```sql
INSERT INTO reportes VALUES (
    123,              -- id (auto-increment)
    '12345',          -- operatorCode
    'Juan',           -- nombre
    'Pérez',          -- apellidoPaterno
    'García',         -- apellidoMaterno
    1730200200000,    -- entrada (29/10/2025 08:30:00)
    NULL,             -- salida (aún no ha salido)
    0.0,              -- tiempoOperando
    0                 -- enviado (pendiente)
);
```

**Al hacer LOGOUT:**
```sql
-- 1. Actualizar registro
UPDATE reportes 
SET 
    salida = 1730233500000,        -- 29/10/2025 17:45:00
    tiempoOperando = 9.25          -- (17:45 - 08:30) = 9.25 hrs
WHERE 
    operatorCode = '12345'
    AND salida IS NULL;            -- Solo el registro abierto

-- 2. Enviar al servidor (si tiene conexión)
-- Si exitoso:
UPDATE reportes SET enviado = 1 WHERE id = 123;
```

## 🔍 Consultas para Mostrar Reportes

### 1. Todos los reportes (tabla principal)
```kotlin
// En SlideshowFragment
viewModel.allReportes.observe { reportes ->
    adapter.submitList(reportes)
}

// SQL ejecutado:
SELECT * FROM reportes 
ORDER BY entrada DESC
```

### 2. Estadísticas semanales (gráficas)
```kotlin
// En SlideshowViewModel
val stats = repository.getWeeklyStats()

// SQL ejecutado:
SELECT 
    DATE(entrada/1000, 'unixepoch') as date,
    SUM(tiempoOperando) as totalHours
FROM reportes
WHERE entrada >= :startDate
GROUP BY DATE(entrada/1000, 'unixepoch')
ORDER BY date
```

### 3. Reportes no sincronizados
```kotlin
// En MainActivity (logout)
val unsentLogs = repository.getUnsyncedLogs()

// SQL ejecutado:
SELECT * FROM reportes
WHERE enviado = 0 
  AND salida IS NOT NULL
ORDER BY entrada ASC
```

## 📱 Experiencia del Usuario

### ✅ LOGIN (Automático)
1. Usuario ingresa clave de 5 dígitos
2. Sistema valida con backend
3. **AUTOMÁTICAMENTE** crea registro en tabla local:
   - ✓ Guarda código del operador
   - ✓ Guarda nombre completo (desde LoginResponse)
   - ✓ Registra hora actual como "entrada"
   - ✓ Deja "salida" vacía (NULL)
   - ✓ Marca como "no enviado" (enviado=0)
4. Navega al Home
5. **Log visible:**
   ```
   ✓ Entrada registrada exitosamente
     - ID registro: 123
     - Operador: 12345
     - Nombre completo: Juan Pérez García
     - Hora entrada: 29/10/2025 08:30:00
   ```

### 📊 DURANTE LA SESIÓN
- Usuario puede ir a "Reportes" (Slideshow)
- Ve tabla con todos sus registros
- Ve gráficas de horas trabajadas
- Iconos muestran estado:
  - 🟢 ✓ = Enviado al servidor
  - 🟠 ⚠ = Pendiente de enviar

### ✅ LOGOUT (Automático)
1. Usuario presiona "Cerrar Sesión"
2. Confirma en diálogo
3. **AUTOMÁTICAMENTE** el sistema:
   - ✓ Busca registro con salida=NULL
   - ✓ Actualiza con hora actual como "salida"
   - ✓ Calcula tiempo operado (salida - entrada)
   - ✓ Intenta enviar ese reporte al servidor
   - ✓ Busca TODOS los reportes con enviado=0
   - ✓ Intenta enviarlos todos
   - ✓ Marca como enviado=1 los exitosos
4. Muestra resultado:
   - "✓ Sesión cerrada, 5 reportes sincronizados"
   - "⚠ Sesión cerrada, 3 sincronizados, 2 pendientes"
   - "⚠ Sesión cerrada (sin conexión)"
5. Navega al Login
6. **Log visible:**
   ```
   ✓ Salida registrada - ID: 123
     Tiempo operado: 9.25 horas
   ✓ Reporte actual sincronizado exitosamente
   Resultado de sincronización:
     - Exitosos: 5
     - Fallidos: 0
   ```

## 🔄 Reintentos Automáticos

### Escenario: Sin conexión al cerrar sesión
1. Usuario cierra sesión sin internet
2. Sistema:
   - ✓ Registra salida localmente
   - ⚠ No puede enviar al servidor
   - ✓ Mantiene enviado=0
3. Usuario ve: "⚠ Sesión cerrada (5 reportes pendientes)"

### Siguiente logout con conexión:
1. Usuario vuelve al día siguiente
2. Inicia sesión (nuevo registro de entrada)
3. Trabaja normalmente
4. Cierra sesión **CON INTERNET**
5. Sistema:
   - ✓ Registra salida de hoy
   - ✓ Envía reporte de hoy
   - **✓ REINTENTA enviar los 5 reportes pendientes de ayer**
   - ✓ Marca todos como enviado=1
6. Usuario ve: "✓ Sesión cerrada, 6 reportes sincronizados"

## 🎯 Archivos Modificados

### 1️⃣ LoginFragment.kt
```kotlin
// AGREGADO:
- registerEntryInDatabase() método nuevo
- Import de lifecycleScope y ControlOperadorApp
- Logs detallados del proceso

// MODIFICADO:
- handleLoginSuccess() ahora llama a registerEntryInDatabase()
```

### 2️⃣ LoginViewModel.kt
```kotlin
// AGREGADO:
- lastLoginResponse: LoginResponse? variable privada
- getLastLoginResponse() método público

// MODIFICADO:
- authenticateWithServer() guarda LoginResponse completo
```

### 3️⃣ MainActivity.kt
```kotlin
// AGREGADO:
- registerExitAndSync() método completo nuevo
- Import de lifecycleScope y kotlinx.coroutines.launch
- Diálogo de progreso durante sincronización
- Logs detallados del proceso
- Mensajes Toast con resultados

// MODIFICADO:
- performLogout() ahora llama a registerExitAndSync()
```

## ✅ Compilación

```bash
BUILD SUCCESSFUL in 8s
```

## 🧪 Casos de Prueba

### ✅ Prueba 1: Login normal
1. Abrir app
2. Ingresar código: `12345`
3. **Resultado esperado:**
   - Navega al Home
   - Registro creado en tabla con entrada=now()
   - Log: "✓ Entrada registrada exitosamente"

### ✅ Prueba 2: Logout normal (con internet)
1. Estar logueado
2. Cerrar sesión
3. **Resultado esperado:**
   - Diálogo "Guardando información..."
   - Registro actualizado con salida=now()
   - Reporte enviado al servidor
   - Toast: "✓ Sesión cerrada, 1 reporte sincronizado"
   - Navega al Login

### ✅ Prueba 3: Logout sin internet
1. Estar logueado
2. Desactivar WiFi/datos
3. Cerrar sesión
4. **Resultado esperado:**
   - Registro actualizado localmente
   - NO se envía al servidor (enviado=0)
   - Toast: "⚠ Sesión cerrada (1 reporte pendiente)"
   - Navega al Login

### ✅ Prueba 4: Ver reportes en Slideshow
1. Hacer login/logout varias veces
2. Navegar a "Reportes"
3. **Resultado esperado:**
   - Tabla muestra todos los registros
   - Fechas de entrada/salida visibles
   - Tiempo operado calculado
   - Iconos:
     - ✓ azul = enviado
     - ⚠ dorado = pendiente
   - Gráficas actualizadas

### ✅ Prueba 5: Múltiples sesiones pendientes
1. Login/logout sin internet (3 veces)
2. Activar internet
3. Login/logout con internet
4. **Resultado esperado:**
   - Toast: "✓ Sesión cerrada, 4 reportes sincronizados"
   - Todos marcados como enviado=1
   - Gráficas muestran todas las sesiones

## 📝 Datos de LoginResponse

El backend devuelve:
```json
{
  "success": true,
  "data": {
    "operator": {
      "operator_code": "12345",
      "nombre": "Juan",
      "apellido_paterno": "Pérez",
      "apellido_materno": "García"
    }
  }
}
```

Estos datos se usan directamente para llenar la tabla:
- `operator_code` → `operatorCode`
- `nombre` → `nombre`
- `apellido_paterno` → `apellidoPaterno`
- `apellido_materno` → `apellidoMaterno`

## 🎉 Sistema Completo Funcionando

✅ Login automático registra entrada
✅ Logout automático registra salida
✅ Sincronización inmediata del reporte cerrado
✅ Reintentos de reportes pendientes
✅ UI actualizada en tiempo real (LiveData)
✅ Gráficas con datos reales
✅ Manejo de errores sin interrumpir flujo
✅ Logs detallados para debugging
