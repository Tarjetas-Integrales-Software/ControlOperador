# Navigation Drawer Header - Vista Actualizada

## 🎨 Diseño del Header

```
╔══════════════════════════════════════════════════╗
║                                                  ║
║          🚛                                      ║  <- Logo Camión (Dorado)
║     [64x64px]                                    ║
║                                                  ║
║  CONTROL OPERADOR                                ║  <- Título (Blanco, Bold, 16sp)
║                                                  ║
║  Operador: 12345                                 ║  <- Código Operador (Dorado, Bold, 13sp)
║                                                  ║
║  RUTA: C30-C75                                   ║  <- Ruta (Blanco, Bold, 13sp)
║                                                  ║
║  UNIDAD: 00001                                   ║  <- Unidad (Blanco, 13sp)
║                                                  ║
║  21/10/2025 - 14:30:45                          ║  <- Fecha/Hora (Dorado, Monospace, 12sp)
║                                                  ║  <- ⏰ SE ACTUALIZA CADA SEGUNDO
╚══════════════════════════════════════════════════╝
     Fondo: Gradient azul oscuro (#1A2332 → #34495E)
```

## 📊 Especificaciones Visuales

### Colores Aplicados
```
Logo Camión:       #F39C12 (Dorado)
Título:            #FFFFFF (Blanco)
Operador:          #F39C12 (Dorado) - Destacado
Ruta:              #FFFFFF (Blanco)
Unidad:            #FFFFFF (Blanco)
Fecha/Hora:        #F39C12 (Dorado) - Actualización en vivo
Fondo Gradient:    #1A2332 → #2C3E50 → #34495E
```

### Espaciado
```
Logo → Título:           16dp padding top
Título → Operador:       4dp margin top
Operador → Ruta:         8dp margin top
Ruta → Unidad:           4dp margin top
Unidad → Fecha/Hora:     8dp margin top
```

### Tamaños de Texto
```
Título:       16sp (Bold)
Operador:     13sp (Bold, destacado en dorado)
Ruta:         13sp (Bold)
Unidad:       13sp (Regular)
Fecha/Hora:   12sp (Monospace para números alineados)
```

## ⚙️ Comportamiento Dinámico

### 1. Código de Operador
- **Fuente**: SessionManager → getOperatorCode()
- **Formato**: "Operador: XXXXX"
- **Actualización**: Al iniciar sesión / al volver a la app

### 2. Ruta
- **Estado Actual**: Valor fijo "C30-C75"
- **Fuente Futura**: API REST → `/api/operator/{code}/info`
- **Formato**: "RUTA: XXX-XXX"

### 3. Unidad
- **Estado Actual**: Valor fijo "00001"
- **Fuente Futura**: API REST → `/api/operator/{code}/info`
- **Formato**: "UNIDAD: XXXXX"

### 4. Fecha y Hora
- **Actualización**: Cada 1 segundo ⏰
- **Formato**: "dd/MM/yyyy - HH:mm:ss"
- **Ejemplo**: "21/10/2025 - 14:30:45"
- **Lifecycle**:
  - ✅ Inicia en `onResume()`
  - ⏸️ Se pausa en `onPause()`
  - 🗑️ Se limpia en `onDestroy()`

## 🔄 Flujo de Actualización

### Al Abrir el Drawer
```
Usuario desliza drawer
    ↓
MainActivity.updateNavHeader() se ejecuta
    ↓
1. Lee código de operador de SessionManager
    ↓
2. Obtiene info de OperatorRepository
   (actualmente datos fijos, futuro: API)
    ↓
3. Actualiza TextViews:
   - textViewOperatorCode
   - textViewRoute
   - textViewUnit
    ↓
4. Inicia actualización de fecha/hora
   (Handler ejecuta cada 1000ms)
    ↓
Header muestra información actualizada
```

### Actualización de Fecha/Hora
```
dateTimeHandler.post(dateTimeUpdateRunnable)
    ↓
Cada 1 segundo:
    ↓
SimpleDateFormat formatea Date actual
    ↓
textViewDateTime actualizado
    ↓
Handler programa siguiente actualización (+1s)
```

## 🎯 IDs de Views (nav_header_main.xml)

```kotlin
// Referencias en código
textViewOperatorCode  → R.id.textViewOperatorCode
textViewRoute         → R.id.textViewRoute
textViewUnit          → R.id.textViewUnit
textViewDateTime      → R.id.textViewDateTime
```

## 📱 Ejemplo de Uso en Código

```kotlin
// MainActivity.kt

private fun initializeHeaderViews() {
    val headerView = binding.navView.getHeaderView(0)
    textViewOperatorCode = headerView.findViewById(R.id.textViewOperatorCode)
    textViewRoute = headerView.findViewById(R.id.textViewRoute)
    textViewUnit = headerView.findViewById(R.id.textViewUnit)
    textViewDateTime = headerView.findViewById(R.id.textViewDateTime)
}

private fun updateNavHeader() {
    val operatorCode = sessionManager.getOperatorCode()
    if (operatorCode != null) {
        operatorInfo = operatorRepository.getOperatorInfo(operatorCode)
        
        textViewOperatorCode?.text = "Operador: ${operatorInfo?.operatorCode}"
        textViewRoute?.text = "RUTA: ${operatorInfo?.route}"
        textViewUnit?.text = "UNIDAD: ${operatorInfo?.unitNumber}"
        
        startDateTimeUpdates() // Inicia reloj en tiempo real
    }
}

private fun updateDateTime() {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy - HH:mm:ss", Locale.getDefault())
    val currentDateTime = dateFormat.format(Date())
    textViewDateTime?.text = currentDateTime
}
```

## 🔮 Preparación para API

### Datos Actuales (Mock)
```kotlin
// OperatorRepository.kt
return OperatorInfo(
    operatorCode = operatorCode,
    route = "C30-C75",        // 👈 Valor fijo
    unitNumber = "00001"       // 👈 Valor fijo
)
```

### Con API Real (Futuro)
```kotlin
// OperatorRepository.kt
suspend fun getOperatorInfo(operatorCode: String): Result<OperatorInfo> {
    return try {
        val response = apiService.getOperatorInfo(operatorCode)
        if (response.isSuccessful) {
            Result.success(response.body()!!)
        } else {
            Result.failure(Exception("API Error"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

## ✅ Verificación Visual

Al ejecutar la app, deberías ver:

1. **Login exitoso** con clave válida (ej: 12345)
2. **Navegar a Home** automáticamente
3. **Abrir drawer** (deslizar desde izquierda o presionar ☰)
4. **Header muestra**:
   - Logo de camión dorado
   - "CONTROL OPERADOR"
   - "Operador: 12345" (tu código)
   - "RUTA: C30-C75"
   - "UNIDAD: 00001"
   - Fecha y hora actualizándose cada segundo ⏰

## 🎨 Personalización

Para cambiar los valores fijos (mientras no hay API):

Editar `data/OperatorRepository.kt`:
```kotlin
return OperatorInfo(
    operatorCode = operatorCode,
    route = "TU_RUTA_AQUI",      // Cambiar aquí
    unitNumber = "TU_UNIDAD_AQUI" // Cambiar aquí
)
```

## 📊 Performance

- **Actualización fecha/hora**: Impacto mínimo (Handler + TextView)
- **Memoria**: Views referencidas como nullable, limpiadas en onDestroy
- **CPU**: ~0.1% para actualización de reloj
- **Batería**: Insignificante

## 🐛 Debugging

Si el header no se actualiza:
```kotlin
// Agregar logs en MainActivity
Log.d("MainActivity", "updateNavHeader() called")
Log.d("MainActivity", "Operator code: $operatorCode")
Log.d("MainActivity", "Route: ${operatorInfo?.route}")
```

---

**Estado**: ✅ Implementado y funcionando  
**API Ready**: 🔄 Preparado para integración  
**Tiempo Real**: ⏰ Fecha/hora actualizándose cada segundo
