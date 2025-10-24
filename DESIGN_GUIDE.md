# Sistema de Login - Guía Visual

## 🎨 Diseño de Pantalla de Login

### Elementos de la Interfaz

```
┌────────────────────────────────────────┐
│                                        │
│  ┌──────────────────────────────────┐ │
│  │  ╔════════════════════════════╗  │ │
│  │  ║    🚛                      ║  │ │
│  │  ║  CONTROL OPERADOR          ║  │ │  <- Card Header
│  │  ║  Sistema de Gestión...     ║  │ │     (Fondo azul oscuro #1A2332)
│  │  ╚════════════════════════════╝  │ │
│  └──────────────────────────────────┘ │
│                                        │
│  ┌──────────────────────────────────┐ │
│  │  Acceso de Operador              │ │
│  │  Ingrese su clave de...          │ │
│  │                                  │ │
│  │  ┌────────────────────────────┐ │ │
│  │  │ 👤 [  12345  ]         🔒  │ │ │  <- Campo de clave
│  │  │    5 dígitos numéricos     │ │ │     (5 caracteres max)
│  │  └────────────────────────────┘ │ │
│  │                                  │ │
│  │  ┌────────────────────────────┐ │ │
│  │  │  ➡️  INGRESAR AL SISTEMA   │ │ │  <- Botón dorado
│  │  └────────────────────────────┘ │ │     (#F39C12)
│  │                                  │ │
│  └──────────────────────────────────┘ │
│                                        │
│  Control Operador v1.0                │
│                                        │
└────────────────────────────────────────┘
```

## 🎯 Claves de Prueba

Para desarrollo y testing, usar cualquiera de estas claves:

| Clave  | Estado |
|--------|--------|
| 12345  | ✅ Válida |
| 54321  | ✅ Válida |
| 11111  | ✅ Válida |
| 99999  | ✅ Válida |
| 00001  | ✅ Válida |

## 🔄 Flujo de Usuario

### Caso Exitoso
```
Usuario abre app
    ↓
Pantalla de Login (sin drawer/toolbar)
    ↓
Ingresa clave: "12345"
    ↓
Presiona "INGRESAR AL SISTEMA"
    ↓
✅ "Acceso autorizado. Bienvenido!"
    ↓
Navegación automática → Home
    ↓
Drawer y toolbar ahora visibles
    ↓
Header muestra "Operador: 12345"
```

### Caso con Error
```
Usuario ingresa clave: "99998"
    ↓
Presiona "INGRESAR AL SISTEMA"
    ↓
❌ "Clave de operador incorrecta"
    ↓
Campo se limpia automáticamente
    ↓
Focus vuelve al campo de entrada
```

## 🎨 Paleta de Colores Aplicada

### Colores Principales
```css
/* Primario - Profesional y ejecutivo */
primary_dark: #1A2332      ████████
primary_medium: #2C3E50    ████████
primary_light: #34495E     ████████

/* Acentos - Identidad de transporte */
accent_gold: #F39C12       ████████ (Botones, iconos)
accent_blue: #3498DB       ████████ (Enlaces, acciones)

/* Fondos */
login_background: #ECF0F1  ████████ (Fondo general)
card_background: #FFFFFF   ████████ (Cards)

/* Estados */
success_green: #27AE60     ████████
error_red: #E74C3C         ████████
```

## 📱 Responsive Behavior

### Estados del Campo de Clave

1. **Normal** (sin interacción)
   - Borde gris claro
   - Hint visible: "Clave de Operador"
   - Helper text: "5 dígitos numéricos"

2. **Focus** (usuario escribiendo)
   - Borde azul oscuro (#1A2332)
   - Hint se mueve arriba
   - Teclado numérico activo

3. **Error** (validación fallida)
   - Borde rojo (#E74C3C)
   - Mensaje de error visible debajo
   - Texto del error en rojo

4. **Success** (validación exitosa)
   - Animación de éxito
   - Snackbar verde con mensaje
   - Navegación después de 500ms

## 🔐 Drawer Navigation (Post-Login)

```
╔══════════════════════════════════╗
║  🚛 [Logo Camión Dorado]         ║
║                                  ║
║  CONTROL OPERADOR                ║
║  Operador: 12345                 ║ <- Código dinámico
╠══════════════════════════════════╣
║                                  ║
║  🏠  Inicio                      ║
║  🚛  Unidades                    ║
║  📊  Reportes                    ║
║                                  ║
║  ─────── Sesión ────────         ║
║  🚪  Cerrar Sesión               ║
║                                  ║
╚══════════════════════════════════╝
```

## 🔒 Seguridad Implementada

### Validaciones en LoginViewModel
```kotlin
✅ Clave no puede estar vacía
✅ Debe tener exactamente 5 caracteres
✅ Solo acepta dígitos numéricos (0-9)
✅ Verifica contra lista de códigos válidos
✅ Mensajes de error específicos por tipo
```

### Gestión de Sesión
```kotlin
✅ Persiste en SharedPreferences
✅ Timeout de 8 horas
✅ Auto-renovación en onResume()
✅ Limpieza completa al cerrar sesión
✅ Verificación en cada inicio de app
```

### Navegación Protegida
```kotlin
✅ Login es startDestination
✅ popUpTo impide volver a login desde home
✅ Drawer bloqueado en login screen
✅ Toolbar oculto en login screen
✅ Verificación de sesión al navegar
```

## 📊 Estados de la Aplicación

### Matriz de Estados

| Pantalla | Drawer | Toolbar | FAB | Sesión Requerida |
|----------|--------|---------|-----|------------------|
| Login    | ❌ Oculto | ❌ Oculto | ❌ Oculto | ❌ No |
| Home     | ✅ Visible | ✅ Visible | ✅ Visible | ✅ Sí |
| Unidades | ✅ Visible | ✅ Visible | ✅ Visible | ✅ Sí |
| Reportes | ✅ Visible | ✅ Visible | ✅ Visible | ✅ Sí |

## 🎭 Animaciones y Transiciones

### Login Flow
- Entrada de texto con ripple effect
- Botón con animación al presionar
- Snackbar desliza desde abajo
- Transición suave a Home (500ms)

### Error Handling
- Shake animation en campo de error
- Color transition rojo
- Auto-clear del campo
- Re-focus automático

## 🧪 Testing Checklist

- [ ] Login con clave válida
- [ ] Login con clave inválida
- [ ] Campo vacío al presionar login
- [ ] Clave con menos de 5 dígitos
- [ ] Clave con más de 5 dígitos
- [ ] Caracteres no numéricos
- [ ] Presionar Enter/Done en teclado
- [ ] Persistencia de sesión (reiniciar app)
- [ ] Timeout de sesión (esperar 8 horas)
- [ ] Cerrar sesión desde drawer
- [ ] Navegación bloqueada sin sesión
- [ ] Renovación de sesión en onResume
- [ ] Drawer oculto en login
- [ ] Header actualizado con código de operador
