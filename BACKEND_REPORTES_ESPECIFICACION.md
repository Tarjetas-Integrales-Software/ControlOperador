# 📋 Especificación Backend: Sistema de Reportes de Asistencia

**Proyecto:** ControlOperador - Sistema de Reportes  
**Fecha:** 30 de Octubre, 2025  
**Backend:** Laravel 7.x  
**Base de Datos:** MySQL/SQL Server (Windows Server)

---

## 📌 Resumen Ejecutivo

La aplicación Android **ControlOperador** necesita sincronizar registros de entrada/salida (reportes de asistencia) de operadores de camiones desde la base de datos local (Room/SQLite) hacia el servidor central.

**Flujo:**
```
App Android (SQLite Local) 
    ↓
POST /api/v1/secomsa/reportes
    ↓
Backend Laravel (SQL Server)
    ↓
Tabla: reportes_operadores
```

---

## 🗄️ Estructura de Base de Datos

### Tabla: `reportes_operadores`

```sql
CREATE TABLE reportes_operadores (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    
    -- Identificación del operador
    operator_code VARCHAR(5) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    apellido_paterno VARCHAR(100) NOT NULL,
    apellido_materno VARCHAR(100) NOT NULL,
    
    -- Timestamps de entrada/salida
    entrada DATETIME NOT NULL,
    salida DATETIME NULL,
    
    -- Tiempo trabajado (calculado en horas)
    tiempo_operando DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    
    -- Control de sincronización
    id_app_local BIGINT NULL,  -- ID del registro en la app (para referencia)
    sincronizado_en DATETIME DEFAULT GETDATE(),
    
    -- Timestamps Laravel
    created_at DATETIME DEFAULT GETDATE(),
    updated_at DATETIME DEFAULT GETDATE(),
    
    -- Índices para búsquedas rápidas
    INDEX idx_operator_code (operator_code),
    INDEX idx_entrada (entrada),
    INDEX idx_sincronizado (sincronizado_en)
);
```

#### Descripción de Campos:

| Campo | Tipo | Obligatorio | Descripción |
|-------|------|-------------|-------------|
| `id` | BIGINT | ✅ | ID único del registro (auto-increment) |
| `operator_code` | VARCHAR(5) | ✅ | Código del operador (5 dígitos, ej: "12345") |
| `nombre` | VARCHAR(100) | ✅ | Nombre del operador |
| `apellido_paterno` | VARCHAR(100) | ✅ | Apellido paterno |
| `apellido_materno` | VARCHAR(100) | ✅ | Apellido materno |
| `entrada` | DATETIME | ✅ | Fecha/hora de inicio de turno (UTC) |
| `salida` | DATETIME | ❌ | Fecha/hora de fin de turno (UTC), puede ser NULL si aún está activo |
| `tiempo_operando` | DECIMAL(10,2) | ✅ | Horas trabajadas (calculado: salida - entrada) |
| `id_app_local` | BIGINT | ❌ | ID del registro en la app Android (referencia) |
| `sincronizado_en` | DATETIME | ✅ | Timestamp de cuándo se recibió en el servidor |
| `created_at` | DATETIME | ✅ | Timestamp de creación Laravel |
| `updated_at` | DATETIME | ✅ | Timestamp de última actualización Laravel |

---

## 🌐 Endpoint API

### **POST** `/api/v1/secomsa/reportes`

**Descripción:** Recibe uno o múltiples reportes de asistencia desde la app Android y los almacena en la base de datos.

### Headers Requeridos:
```http
Content-Type: application/json
Accept: application/json
```

### Request Body:

```json
{
  "reportes": [
    {
      "id": 123,
      "operator_code": "12345",
      "nombre": "Juan",
      "apellido_paterno": "Pérez",
      "apellido_materno": "García",
      "entrada": "2025-10-29T08:30:00Z",
      "salida": "2025-10-29T17:45:00Z",
      "tiempo_operando": 9.25
    },
    {
      "id": 124,
      "operator_code": "12345",
      "nombre": "Juan",
      "apellido_paterno": "Pérez",
      "apellido_materno": "García",
      "entrada": "2025-10-30T07:15:00Z",
      "salida": null,
      "tiempo_operando": 0.0
    }
  ]
}
```

### Estructura del Objeto `reporte`:

| Campo | Tipo | Obligatorio | Descripción | Ejemplo |
|-------|------|-------------|-------------|---------|
| `id` | integer | ✅ | ID local del registro en la app | `123` |
| `operator_code` | string(5) | ✅ | Código del operador | `"12345"` |
| `nombre` | string | ✅ | Nombre | `"Juan"` |
| `apellido_paterno` | string | ✅ | Apellido paterno | `"Pérez"` |
| `apellido_materno` | string | ✅ | Apellido materno | `"García"` |
| `entrada` | datetime (ISO 8601) | ✅ | Fecha/hora entrada en UTC | `"2025-10-29T08:30:00Z"` |
| `salida` | datetime (ISO 8601) o null | ❌ | Fecha/hora salida en UTC | `"2025-10-29T17:45:00Z"` o `null` |
| `tiempo_operando` | decimal | ✅ | Horas trabajadas | `9.25` |

### Response - Éxito (200 OK):

```json
{
  "success": true,
  "message": "Reportes procesados correctamente",
  "data": {
    "processed": 2,
    "failed": 0,
    "errors": []
  }
}
```

### Response - Éxito Parcial (200 OK):

```json
{
  "success": true,
  "message": "Algunos reportes fallaron",
  "data": {
    "processed": 1,
    "failed": 1,
    "errors": [
      {
        "id": 124,
        "message": "Operador no encontrado"
      }
    ]
  }
}
```

### Response - Error Validación (422 Unprocessable Entity):

```json
{
  "success": false,
  "message": "Errores de validación",
  "errors": {
    "reportes.0.operator_code": [
      "El campo operator_code es obligatorio."
    ],
    "reportes.1.entrada": [
      "El formato de fecha entrada es inválido."
    ]
  }
}
```

### Response - Error Servidor (500 Internal Server Error):

```json
{
  "success": false,
  "message": "Error al procesar reportes",
  "errors": {
    "exception": "Database connection error"
  }
}
```

---

## 🏗️ Implementación Laravel

### 1️⃣ **Migración de Base de Datos**

**Archivo:** `database/migrations/YYYY_MM_DD_create_reportes_operadores_table.php`

```php
<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateReportesOperadoresTable extends Migration
{
    /**
     * Run the migrations.
     *
     * @return void
     */
    public function up()
    {
        Schema::create('reportes_operadores', function (Blueprint $table) {
            $table->id();
            
            // Identificación del operador
            $table->string('operator_code', 5);
            $table->string('nombre', 100);
            $table->string('apellido_paterno', 100);
            $table->string('apellido_materno', 100);
            
            // Timestamps de entrada/salida
            $table->dateTime('entrada');
            $table->dateTime('salida')->nullable();
            
            // Tiempo trabajado (en horas)
            $table->decimal('tiempo_operando', 10, 2)->default(0.00);
            
            // Control de sincronización
            $table->bigInteger('id_app_local')->nullable();
            $table->dateTime('sincronizado_en')->default(DB::raw('GETDATE()'));
            
            // Timestamps Laravel
            $table->timestamps();
            
            // Índices
            $table->index('operator_code');
            $table->index('entrada');
            $table->index('sincronizado_en');
        });
    }

    /**
     * Reverse the migrations.
     *
     * @return void
     */
    public function down()
    {
        Schema::dropIfExists('reportes_operadores');
    }
}
```

**Ejecutar:**
```bash
php artisan migrate
```

---

### 2️⃣ **Modelo Eloquent**

**Archivo:** `app/Models/ReporteOperador.php`

```php
<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Carbon\Carbon;

class ReporteOperador extends Model
{
    /**
     * Nombre de la tabla
     */
    protected $table = 'reportes_operadores';

    /**
     * Campos asignables masivamente
     */
    protected $fillable = [
        'operator_code',
        'nombre',
        'apellido_paterno',
        'apellido_materno',
        'entrada',
        'salida',
        'tiempo_operando',
        'id_app_local',
        'sincronizado_en',
    ];

    /**
     * Campos que deben ser tratados como fechas
     */
    protected $dates = [
        'entrada',
        'salida',
        'sincronizado_en',
        'created_at',
        'updated_at',
    ];

    /**
     * Casts de atributos
     */
    protected $casts = [
        'tiempo_operando' => 'decimal:2',
        'id_app_local' => 'integer',
    ];

    /**
     * Obtener el nombre completo del operador
     */
    public function getNombreCompletoAttribute()
    {
        return trim("{$this->nombre} {$this->apellido_paterno} {$this->apellido_materno}");
    }

    /**
     * Verificar si el turno está cerrado
     */
    public function isTurnoCerrado()
    {
        return !is_null($this->salida);
    }

    /**
     * Calcular tiempo operando (si no se proporcionó)
     */
    public function calcularTiempoOperando()
    {
        if ($this->isTurnoCerrado()) {
            $entrada = Carbon::parse($this->entrada);
            $salida = Carbon::parse($this->salida);
            return round($entrada->diffInMinutes($salida) / 60, 2);
        }
        return 0.00;
    }

    /**
     * Scope: Reportes de un operador específico
     */
    public function scopeDelOperador($query, $operatorCode)
    {
        return $query->where('operator_code', $operatorCode);
    }

    /**
     * Scope: Reportes en un rango de fechas
     */
    public function scopeEntreFechas($query, $fechaInicio, $fechaFin)
    {
        return $query->whereBetween('entrada', [$fechaInicio, $fechaFin]);
    }

    /**
     * Scope: Turnos cerrados
     */
    public function scopeTurnosCerrados($query)
    {
        return $query->whereNotNull('salida');
    }

    /**
     * Scope: Turnos abiertos
     */
    public function scopeTurnosAbiertos($query)
    {
        return $query->whereNull('salida');
    }
}
```

---

### 3️⃣ **Request de Validación**

**Archivo:** `app/Http/Requests/StoreReportesRequest.php`

```php
<?php

namespace App\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

class StoreReportesRequest extends FormRequest
{
    /**
     * Determina si el usuario está autorizado para hacer esta petición
     */
    public function authorize()
    {
        return true; // Ajustar según tu sistema de autenticación
    }

    /**
     * Reglas de validación
     */
    public function rules()
    {
        return [
            'reportes' => 'required|array|min:1',
            'reportes.*.id' => 'required|integer',
            'reportes.*.operator_code' => 'required|string|size:5',
            'reportes.*.nombre' => 'required|string|max:100',
            'reportes.*.apellido_paterno' => 'required|string|max:100',
            'reportes.*.apellido_materno' => 'required|string|max:100',
            'reportes.*.entrada' => 'required|date|date_format:Y-m-d\TH:i:s\Z',
            'reportes.*.salida' => 'nullable|date|date_format:Y-m-d\TH:i:s\Z|after:reportes.*.entrada',
            'reportes.*.tiempo_operando' => 'required|numeric|min:0',
        ];
    }

    /**
     * Mensajes de error personalizados
     */
    public function messages()
    {
        return [
            'reportes.required' => 'Debe enviar al menos un reporte',
            'reportes.*.operator_code.required' => 'El código de operador es obligatorio',
            'reportes.*.operator_code.size' => 'El código de operador debe tener 5 dígitos',
            'reportes.*.entrada.required' => 'La fecha de entrada es obligatoria',
            'reportes.*.entrada.date_format' => 'El formato de fecha entrada debe ser ISO 8601 (Y-m-d\TH:i:s\Z)',
            'reportes.*.salida.date_format' => 'El formato de fecha salida debe ser ISO 8601 (Y-m-d\TH:i:s\Z)',
            'reportes.*.salida.after' => 'La fecha de salida debe ser posterior a la entrada',
        ];
    }
}
```

---

### 4️⃣ **Controlador**

**Archivo:** `app/Http/Controllers/Api/ReportesController.php`

```php
<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Http\Requests\StoreReportesRequest;
use App\Models\ReporteOperador;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Log;
use Exception;

class ReportesController extends Controller
{
    /**
     * Almacena reportes de asistencia desde la app Android
     * 
     * POST /api/v1/secomsa/reportes
     */
    public function store(StoreReportesRequest $request)
    {
        $reportes = $request->input('reportes');
        $processed = 0;
        $failed = 0;
        $errors = [];

        DB::beginTransaction();

        try {
            foreach ($reportes as $reporteData) {
                try {
                    // Validar que el operador existe (opcional)
                    // Puedes agregar validación contra la tabla de operadores aquí
                    // $operatorExists = Operator::where('operator_code', $reporteData['operator_code'])->exists();
                    // if (!$operatorExists) {
                    //     throw new Exception("Operador {$reporteData['operator_code']} no encontrado");
                    // }

                    // Verificar si ya existe un reporte con el mismo id_app_local
                    $existente = ReporteOperador::where('id_app_local', $reporteData['id'])
                        ->where('operator_code', $reporteData['operator_code'])
                        ->first();

                    if ($existente) {
                        // Actualizar reporte existente (por si cambió la hora de salida)
                        $existente->update([
                            'salida' => $reporteData['salida'] ?? null,
                            'tiempo_operando' => $reporteData['tiempo_operando'],
                        ]);
                        
                        Log::info("Reporte actualizado", [
                            'id_app_local' => $reporteData['id'],
                            'operator_code' => $reporteData['operator_code']
                        ]);
                    } else {
                        // Crear nuevo reporte
                        ReporteOperador::create([
                            'operator_code' => $reporteData['operator_code'],
                            'nombre' => $reporteData['nombre'],
                            'apellido_paterno' => $reporteData['apellido_paterno'],
                            'apellido_materno' => $reporteData['apellido_materno'],
                            'entrada' => $reporteData['entrada'],
                            'salida' => $reporteData['salida'] ?? null,
                            'tiempo_operando' => $reporteData['tiempo_operando'],
                            'id_app_local' => $reporteData['id'],
                            'sincronizado_en' => now(),
                        ]);
                        
                        Log::info("Reporte creado", [
                            'id_app_local' => $reporteData['id'],
                            'operator_code' => $reporteData['operator_code']
                        ]);
                    }

                    $processed++;

                } catch (Exception $e) {
                    $failed++;
                    $errors[] = [
                        'id' => $reporteData['id'],
                        'message' => $e->getMessage()
                    ];
                    
                    Log::error("Error procesando reporte", [
                        'id_app_local' => $reporteData['id'],
                        'error' => $e->getMessage()
                    ]);
                }
            }

            DB::commit();

            return response()->json([
                'success' => true,
                'message' => $failed > 0 
                    ? 'Algunos reportes fallaron' 
                    : 'Reportes procesados correctamente',
                'data' => [
                    'processed' => $processed,
                    'failed' => $failed,
                    'errors' => $errors
                ]
            ], 200);

        } catch (Exception $e) {
            DB::rollBack();
            
            Log::error("Error general al procesar reportes", [
                'error' => $e->getMessage(),
                'trace' => $e->getTraceAsString()
            ]);

            return response()->json([
                'success' => false,
                'message' => 'Error al procesar reportes',
                'errors' => [
                    'exception' => $e->getMessage()
                ]
            ], 500);
        }
    }

    /**
     * Obtiene reportes de un operador específico
     * 
     * GET /api/v1/secomsa/reportes/{operator_code}
     */
    public function show($operatorCode)
    {
        try {
            $reportes = ReporteOperador::delOperador($operatorCode)
                ->orderBy('entrada', 'desc')
                ->get();

            return response()->json([
                'success' => true,
                'message' => 'Reportes obtenidos correctamente',
                'data' => [
                    'reportes' => $reportes,
                    'total' => $reportes->count()
                ]
            ], 200);

        } catch (Exception $e) {
            return response()->json([
                'success' => false,
                'message' => 'Error al obtener reportes',
                'errors' => [
                    'exception' => $e->getMessage()
                ]
            ], 500);
        }
    }

    /**
     * Obtiene estadísticas de reportes
     * 
     * GET /api/v1/secomsa/reportes/estadisticas/{operator_code}
     */
    public function estadisticas($operatorCode)
    {
        try {
            $reportes = ReporteOperador::delOperador($operatorCode)
                ->turnosCerrados()
                ->get();

            $totalHoras = $reportes->sum('tiempo_operando');
            $promedioHorasDiarias = $reportes->count() > 0 
                ? round($totalHoras / $reportes->count(), 2) 
                : 0;

            return response()->json([
                'success' => true,
                'message' => 'Estadísticas obtenidas correctamente',
                'data' => [
                    'total_turnos' => $reportes->count(),
                    'total_horas' => round($totalHoras, 2),
                    'promedio_horas_diarias' => $promedioHorasDiarias,
                    'ultimo_turno' => $reportes->first(),
                ]
            ], 200);

        } catch (Exception $e) {
            return response()->json([
                'success' => false,
                'message' => 'Error al obtener estadísticas',
                'errors' => [
                    'exception' => $e->getMessage()
                ]
            ], 500);
        }
    }
}
```

---

### 5️⃣ **Rutas API**

**Archivo:** `routes/api.php`

```php
<?php

use Illuminate\Support\Facades\Route;
use App\Http\Controllers\Api\ReportesController;

/*
|--------------------------------------------------------------------------
| API Routes - Reportes de Asistencia
|--------------------------------------------------------------------------
*/

Route::prefix('v1/secomsa')->group(function () {
    
    // Enviar reportes desde la app Android
    Route::post('reportes', [ReportesController::class, 'store']);
    
    // Obtener reportes de un operador
    Route::get('reportes/{operator_code}', [ReportesController::class, 'show']);
    
    // Obtener estadísticas de un operador
    Route::get('reportes/estadisticas/{operator_code}', [ReportesController::class, 'estadisticas']);
});
```

**URL completa:** `http://tu-servidor.com/api/v1/secomsa/reportes`

---

## 🧪 Testing

### Probar con cURL:

```bash
curl -X POST http://localhost:8000/api/v1/secomsa/reportes \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "reportes": [
      {
        "id": 1,
        "operator_code": "12345",
        "nombre": "Juan",
        "apellido_paterno": "Pérez",
        "apellido_materno": "García",
        "entrada": "2025-10-30T08:30:00Z",
        "salida": "2025-10-30T17:45:00Z",
        "tiempo_operando": 9.25
      }
    ]
  }'
```

### Probar con Postman:

1. **Method:** POST
2. **URL:** `http://localhost:8000/api/v1/secomsa/reportes`
3. **Headers:**
   - `Content-Type: application/json`
   - `Accept: application/json`
4. **Body (raw JSON):**
```json
{
  "reportes": [
    {
      "id": 1,
      "operator_code": "12345",
      "nombre": "Juan",
      "apellido_paterno": "Pérez",
      "apellido_materno": "García",
      "entrada": "2025-10-30T08:30:00Z",
      "salida": "2025-10-30T17:45:00Z",
      "tiempo_operando": 9.25
    }
  ]
}
```

---

## 🔒 Seguridad y Validaciones

### Recomendaciones:

1. **Autenticación:**
   ```php
   // Agregar middleware de autenticación
   Route::post('reportes', [ReportesController::class, 'store'])
       ->middleware('auth:sanctum'); // O el middleware que uses
   ```

2. **Rate Limiting:**
   ```php
   Route::middleware('throttle:60,1')->group(function () {
       Route::post('reportes', [ReportesController::class, 'store']);
   });
   ```

3. **Validación de Operador Existente:**
   ```php
   // En el controlador, antes de guardar:
   $operator = Operator::where('operator_code', $reporteData['operator_code'])->first();
   if (!$operator) {
       throw new Exception("Operador no encontrado");
   }
   ```

4. **Evitar Duplicados:**
   - La lógica actual verifica `id_app_local` + `operator_code`
   - Si existe, actualiza; si no, crea nuevo

5. **Logs Detallados:**
   - Todos los reportes procesados se registran en Laravel logs
   - Revisar `storage/logs/laravel.log`

---

## 📊 Consultas SQL Útiles

### Ver todos los reportes:
```sql
SELECT 
    operator_code,
    nombre,
    apellido_paterno,
    entrada,
    salida,
    tiempo_operando,
    sincronizado_en
FROM reportes_operadores
ORDER BY entrada DESC;
```

### Reportes de un operador específico:
```sql
SELECT * FROM reportes_operadores
WHERE operator_code = '12345'
ORDER BY entrada DESC;
```

### Estadísticas por operador:
```sql
SELECT 
    operator_code,
    COUNT(*) as total_turnos,
    SUM(tiempo_operando) as total_horas,
    AVG(tiempo_operando) as promedio_horas,
    MAX(entrada) as ultimo_turno
FROM reportes_operadores
WHERE salida IS NOT NULL
GROUP BY operator_code;
```

### Reportes del último mes:
```sql
SELECT * FROM reportes_operadores
WHERE entrada >= DATEADD(MONTH, -1, GETDATE())
ORDER BY entrada DESC;
```

### Turnos abiertos (sin salida):
```sql
SELECT * FROM reportes_operadores
WHERE salida IS NULL
ORDER BY entrada DESC;
```

---

## 🚀 Pasos de Implementación

### Checklist:

- [ ] 1. Crear migración de base de datos
- [ ] 2. Ejecutar `php artisan migrate`
- [ ] 3. Verificar que la tabla existe en SQL Server
- [ ] 4. Crear modelo `ReporteOperador.php`
- [ ] 5. Crear request `StoreReportesRequest.php`
- [ ] 6. Crear controlador `ReportesController.php`
- [ ] 7. Agregar rutas en `routes/api.php`
- [ ] 8. Probar endpoint con Postman/cURL
- [ ] 9. Verificar logs en `storage/logs/laravel.log`
- [ ] 10. Probar desde la app Android
- [ ] 11. Agregar autenticación (si es necesario)
- [ ] 12. Documentar en Swagger/Postman Collection

---

## 📝 Notas Adicionales

### Formato de Fechas:
- La app envía fechas en formato **ISO 8601 UTC**: `2025-10-30T08:30:00Z`
- Laravel las convierte automáticamente a `DATETIME` de SQL Server
- **Importante:** Todas las fechas están en UTC, ajustar para zona horaria local si es necesario

### Manejo de Salida NULL:
- Si `salida` es `null`, significa que el turno aún está **abierto** (operador trabajando)
- La app enviará actualización cuando el operador haga logout
- El backend debe permitir `salida` como NULL en la validación

### Duplicados:
- Se usa `id_app_local` + `operator_code` como clave única lógica
- Si el mismo reporte se envía dos veces, se **actualiza** en lugar de duplicar

### Logs:
Todos los eventos importantes se registran:
```
[2025-10-30 10:30:00] Reporte creado {"id_app_local":123,"operator_code":"12345"}
[2025-10-30 17:45:00] Reporte actualizado {"id_app_local":123,"operator_code":"12345"}
[2025-10-30 18:00:00] Error procesando reporte {"id_app_local":124,"error":"..."}
```

---

## 📞 Contacto

Si hay dudas o problemas durante la implementación:
- Revisar logs de Laravel: `storage/logs/laravel.log`
- Revisar logs de la app Android: Filtrar por `AttendanceRepository` y `MainActivity`
- Verificar conectividad: La app usa `RetrofitClient.BASE_URL`

---

## ✅ Checklist Final

Antes de marcar como completo:

- [ ] Tabla `reportes_operadores` creada en SQL Server
- [ ] Modelo Laravel funcional
- [ ] Endpoint responde correctamente a POST
- [ ] Validaciones funcionando
- [ ] Errores devuelven mensajes claros
- [ ] Logs registrando actividad
- [ ] Probado con datos reales desde Android
- [ ] Sincronización exitosa (enviado=1 en app)
- [ ] Documentación actualizada

---

**Fecha de última actualización:** 30 de Octubre, 2025  
**Versión:** 1.0  
**Autor:** Equipo ControlOperador
