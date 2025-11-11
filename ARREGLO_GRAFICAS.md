# Arreglo de Gráficas - ControlOperador

## Problema Identificado

Las gráficas (BarChart y PieChart) en la sección de Reportes mostraban "No chart data available" a pesar de haber datos en la tabla `reportes`. Los problemas encontrados fueron:

### 1. **Query SQL con filtro restrictivo**
```sql
-- Query ANTIGUA (no funcionaba)
WHERE entrada >= :startDate AND salida IS NOT NULL
```
- ❌ **Problema**: Solo contaba registros CERRADOS (con salida)
- ❌ **Problema**: Excluía sesiones ACTIVAS (operador trabajando ahora)
- ❌ **Problema**: Comparación de fechas incorrecta (Date vs Long)

### 2. **Falta de filtro por operador**
- ❌ El ViewModel tenía `currentOperatorCode` pero NO lo pasaba al Repository
- ❌ Mostraba estadísticas de TODOS los operadores mezclados
- ❌ No respetaba el contexto del operador actual

## Solución Implementada

### 1. **Query SQL Mejorada** (`AttendanceLogDao.kt`)

```sql
SELECT DATE(entrada / 1000, 'unixepoch', 'localtime') as date,
       SUM(CASE 
           WHEN salida IS NOT NULL THEN tiempoOperando
           ELSE CAST((strftime('%s', 'now') * 1000 - entrada) AS REAL) / 3600000.0
       END) as totalHours
FROM reportes 
WHERE entrada >= :startDateMillis 
  AND (:operatorCode IS NULL OR operatorCode = :operatorCode)
GROUP BY DATE(entrada / 1000, 'unixepoch', 'localtime')
ORDER BY date DESC
```

**Mejoras**:
- ✅ **Incluye registros abiertos**: Calcula tiempo hasta "ahora" si no hay salida
- ✅ **Filtro por operador**: Parámetro opcional para filtrar por código
- ✅ **Comparación correcta**: Usa `startDateMillis` (Long) en lugar de Date
- ✅ **Fecha normalizada**: Agrupa por día sin importar la hora

### 2. **Repository con Logging** (`AttendanceRepository.kt`)

```kotlin
suspend fun getWeeklyStats(operatorCode: String? = null): List<DailyStats> {
    val calendar = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -7)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val startDateMillis = calendar.timeInMillis
    
    Log.d(TAG, "📊 Obteniendo estadísticas desde: ${calendar.time}")
    Log.d(TAG, "👤 Filtrado por operador: ${operatorCode ?: "TODOS"}")
    
    val stats = attendanceLogDao.getDailyStats(startDateMillis, operatorCode)
    Log.d(TAG, "📈 Estadísticas obtenidas: ${stats.size} días")
    
    return stats
}
```

**Mejoras**:
- ✅ **Parámetro opcional**: `operatorCode` con default null (todos los operadores)
- ✅ **Fecha normalizada**: Empieza a las 00:00:00 de hace 7 días
- ✅ **Logging detallado**: Muestra fecha de inicio y operador filtrado

### 3. **ViewModel Actualizado** (`SlideshowViewModel.kt`)

```kotlin
fun loadWeeklyStats() {
    viewModelScope.launch {
        try {
            val operatorCode = _currentOperatorCode.value
            Log.d("SlideshowViewModel", "📊 Cargando estadísticas para operador: ${operatorCode ?: "TODOS"}")
            
            val stats = repository.getWeeklyStats(operatorCode)
            _weeklyStats.value = stats
            
            Log.d("SlideshowViewModel", "Estadísticas cargadas: ${stats.size} días")
            stats.forEach { stat ->
                Log.d("SlideshowViewModel", "  - ${stat.date}: ${stat.totalHours} horas")
            }
            
            val total = stats.sumOf { it.totalHours }
            _totalWeeklyHours.value = total
            
            Log.d("SlideshowViewModel", "Total horas semanales: $total")
        } catch (e: Exception) {
            Log.e("SlideshowViewModel", "Error al cargar estadísticas", e)
        }
    }
}
```

**Mejoras**:
- ✅ **Pasa el operador actual**: Respeta `_currentOperatorCode.value`
- ✅ **Logging detallado**: Muestra cada día y sus horas

## Cambios en los Archivos

### `AttendanceLogDao.kt`
```diff
- suspend fun getDailyStats(startDate: Date): List<DailyStats>
+ suspend fun getDailyStats(startDateMillis: Long, operatorCode: String?): List<DailyStats>
```

### `AttendanceRepository.kt`
```diff
- suspend fun getWeeklyStats(): List<DailyStats>
+ suspend fun getWeeklyStats(operatorCode: String? = null): List<DailyStats>
```

### `SlideshowViewModel.kt`
```diff
- val stats = repository.getWeeklyStats()
+ val operatorCode = _currentOperatorCode.value
+ val stats = repository.getWeeklyStats(operatorCode)
```

## Resultado Esperado

### BarChart (Gráfica de Barras)
- ✅ Muestra **últimos 7 días** con horas trabajadas por día
- ✅ Incluye **sesiones activas** (calculando hasta ahora)
- ✅ Filtra por **operador actual**
- ✅ Etiquetas en eje X con fechas (dd/MM)

### PieChart (Gráfica Circular)
- ✅ Muestra **distribución de horas** por día de la semana
- ✅ Porcentajes de cada día sobre el total
- ✅ Colores diferenciados por día

## Logs para Verificación

Abre la app y ve a **Reportes**. Deberías ver en Logcat:

```
AttendanceRepository: 📊 Obteniendo estadísticas desde: Mon Nov 04 00:00:00 GMT-06:00 2025
AttendanceRepository: 👤 Filtrado por operador: 12345
AttendanceRepository: 📈 Estadísticas obtenidas: 5 días

SlideshowViewModel: 📊 Cargando estadísticas para operador: 12345
SlideshowViewModel: Estadísticas cargadas: 5 días
SlideshowViewModel:   - 2025-11-11: 8.5 horas
SlideshowViewModel:   - 2025-11-10: 7.2 horas
SlideshowViewModel:   - 2025-11-09: 9.1 horas
SlideshowViewModel:   - 2025-11-08: 6.8 horas
SlideshowViewModel:   - 2025-11-07: 8.3 horas
SlideshowViewModel: Total horas semanales: 39.9

SlideshowFragment: updateBarChart llamado con 5 estadísticas
SlideshowFragment: updatePieChart llamado con 5 estadísticas
```

## Testing Manual

1. **Abre la app** y autentícate con tu código de operador
2. **Ve a la sección "Reportes"** (Slideshow)
3. **Verifica las gráficas**:
   - BarChart debe mostrar barras con alturas proporcionales a las horas
   - PieChart debe mostrar segmentos con porcentajes
   - Título debe mostrar total de horas semanales
4. **Revisa Logcat** para confirmar que se están cargando datos

## Comandos de Verificación

```bash
# Ver logs filtrados
~/Library/Android/sdk/platform-tools/adb logcat | grep -E "SlideshowViewModel|SlideshowFragment|AttendanceRepository"

# Ver datos en Room (si tienes App Inspection en Android Studio)
# Database Inspector > reportes table > ver últimos 7 días
```

## Notas Técnicas

### Cálculo de Horas para Sesiones Activas
```sql
CAST((strftime('%s', 'now') * 1000 - entrada) AS REAL) / 3600000.0
```
- `strftime('%s', 'now')` - Timestamp actual en segundos
- `* 1000` - Convertir a milisegundos
- `- entrada` - Restar timestamp de entrada (en milisegundos)
- `/ 3600000.0` - Convertir milisegundos a horas

### Filtro Opcional de Operador
```sql
(:operatorCode IS NULL OR operatorCode = :operatorCode)
```
- Si `operatorCode` es NULL → Incluye TODOS los operadores
- Si `operatorCode` tiene valor → Filtra solo ese operador

## Build Info

- ✅ **Compilación**: BUILD SUCCESSFUL in 4s
- ✅ **Instalación**: Installed on SM-X115 (Android 15)
- ⚠️ **Warnings**: 3 deprecation warnings (no afectan funcionalidad)

## Próximos Pasos

1. ✅ **Probar las gráficas** en el dispositivo
2. ⏳ **Verificar datos** con operadores reales
3. ⏳ **Ajustar colores** si es necesario (colors.xml)
4. ⏳ **Agregar refresh manual** (SwipeRefreshLayout)

---

**Fecha**: 11 de noviembre, 2025  
**Dispositivo**: Samsung SM-X115 (Android 15)  
**Build**: Debug APK v1.0
