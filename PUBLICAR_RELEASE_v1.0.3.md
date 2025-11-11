# 🚀 Publicar Release v1.0.3 en GitHub

## ✅ Estado Actual
- ✅ Código commiteado y pusheado
- ✅ Tag v1.0.3 creado y pusheado
- ✅ APK v1.0.3 firmado y listo: `~/Desktop/ControlOperador-v1.0.3-release.apk`
- ⏳ **FALTA: Crear Release en GitHub**

---

## 📝 Pasos para Crear el Release

### 1. Ir a GitHub Releases
Abre este enlace en tu navegador:
```
https://github.com/Tarjetas-Integrales-Software/ControlOperador/releases/new
```

O manualmente:
1. Ve a: https://github.com/Tarjetas-Integrales-Software/ControlOperador
2. Click en **"Releases"** (barra lateral derecha)
3. Click en **"Draft a new release"**

---

### 2. Configurar el Release

#### **Choose a tag:**
Selecciona: `v1.0.3` (debe aparecer en el dropdown)

#### **Release title:**
```
Control Operador v1.0.3
```

#### **Description:**
Copia y pega esto:

```markdown
## 🧪 Testing Auto-Update System

Esta versión es para **probar el sistema de auto-actualización**.

### 🔄 Qué hace diferente

- Version Code: **3** (anterior era 2)
- Los dispositivos con v1.0.2 detectarán automáticamente esta actualización
- Se descargará en background sin intervención del usuario
- Aparecerá notificación cuando esté lista para instalar

### ✨ Sistema de Auto-Actualización

- ✅ Verificación automática cada 15 minutos
- ✅ Descarga silenciosa en background
- ✅ Notificaciones de actualización disponible
- ✅ Instalación con un solo tap

### 📊 Mejoras Incluidas

- Intervalo de verificación ajustado a 15 minutos (mínimo de Android)
- WorkManager correctamente configurado
- Logs mejorados para debugging
- URL de producción configurada

### 🔧 Información Técnica

- **Version Name:** 1.0.3
- **Version Code:** 3
- **Min SDK:** 29 (Android 10)
- **Target SDK:** 36
- **Firmado con:** controloperador-new.jks

### 📥 Instalación Manual

Si quieres instalar directamente (sin auto-update):
1. Descarga el APK adjunto
2. Permite instalación de fuentes desconocidas
3. Instala el APK

### ⚠️ Para Testing

**Dispositivos con v1.0.2:**
- Abre la app
- Espera máximo 15 minutos
- Verás notificación: "Nueva versión disponible"
- Se descargará automáticamente
- Toca la notificación para instalar

**Logs esperados:**
```
D UpdateCheckWorker: 🔄 Iniciando verificación de actualizaciones...
D UpdateRepository: 🔍 Verificando desde GitHub API...
D UpdateRepository: 📦 Versión actual: 1.0.2 (code: 2)
D UpdateRepository: 🆕 Nueva versión encontrada: v1.0.3
D UpdateCheckWorker: ⬇️ Iniciando descarga automática...
D UpdateRepository: 📥 Descargando: 100%
D UpdateCheckWorker: ✅ Descarga completada
```
```

---

### 3. Subir el APK

En la sección **"Attach binaries by dropping them here or selecting them"**:

1. Arrastra el archivo desde tu escritorio: `ControlOperador-v1.0.3-release.apk`
2. O click en **"choose them"** y selecciona el archivo
3. Espera a que se suba (14 MB aprox.)
4. Verifica que aparezca en la lista con el ícono de APK

**⚠️ IMPORTANTE:** El nombre del archivo **DEBE** terminar en `.apk` para que la app lo detecte.

---

### 4. Marcar como Latest Release

**MUY IMPORTANTE:**
- ✅ Asegúrate de que esté marcado **"Set as the latest release"**
- ❌ NO marques "Set as a pre-release"
- ❌ NO marques "Create a discussion for this release"

**Esto es CRÍTICO** porque la app consulta el endpoint `/releases/latest`

---

### 5. Publicar

Click en **"Publish release"**

---

## ✅ Verificación

Una vez publicado, verifica que está correcto:

### En el navegador:
```
https://github.com/Tarjetas-Integrales-Software/ControlOperador/releases/latest
```

Deberías ver:
- Tag: v1.0.3
- Title: Control Operador v1.0.3
- APK adjunto: ControlOperador-v1.0.3-release.apk
- Badge: "Latest"

### Con curl (desde terminal):
```bash
curl -s https://api.github.com/repos/Tarjetas-Integrales-Software/ControlOperador/releases/latest | grep -A 3 "tag_name"
```

Debería devolver:
```json
"tag_name": "v1.0.3",
"target_commitish": "operadorDan",
"name": "Control Operador v1.0.3",
```

---

## 📱 Probar en la Tablet

Una vez publicado el release:

### Opción A: Esperar (Natural)

1. **NO instales nada manualmente en la tablet**
2. Deja la tablet con conexión a internet
3. Espera máximo 15 minutos
4. La app detectará automáticamente la actualización

### Opción B: Forzar verificación (Con ADB)

```bash
# Conectar tablet por USB
ADB=$(find ~/Library/Android/sdk -name "adb" 2>/dev/null | head -1)

# Forzar ejecución del Worker
$ADB shell cmd jobscheduler run -f com.example.controloperador 1

# Ver logs
$ADB logcat | grep -E "UpdateCheckWorker|UpdateRepository"
```

### Logs esperados (versión 2 detectando versión 3):

```
D UpdateCheckWorker: 🔄 Iniciando verificación de actualizaciones...
D UpdateRepository: 🔍 Verificando desde GitHub API...
D UpdateRepository: 📦 Versión actual: 1.0.2 (code: 2)
D UpdateRepository: 📋 Release encontrado: Control Operador v1.0.3 (v1.0.3)
D UpdateRepository: 📱 APK encontrado: ControlOperador-v1.0.3-release.apk
D UpdateRepository: 🔢 Comparando versiones:
D UpdateRepository:     - Actual: 2
D UpdateRepository:     - Disponible: 3
D UpdateRepository: 🆕 ¡Nueva versión disponible! 2 -> 3
D UpdateCheckWorker: 🆕 Nueva versión encontrada: v1.0.3
D UpdateCheckWorker: ⬇️ Iniciando descarga automática...
D UpdateRepository: 📥 Descarga: 10%
D UpdateRepository: 📥 Descarga: 50%
D UpdateRepository: 📥 Descarga: 100%
D UpdateCheckWorker: ✅ Descarga completada: ControlOperador-v1.0.3.apk
```

### Notificaciones esperadas:

1. **Primera notificación (al detectar):**
   - Título: "Nueva versión disponible"
   - Texto: "ControlOperador 1.0.3"

2. **Durante descarga:**
   - Título: "Descargando actualización"
   - Texto: "Descargando ControlOperador... XX%"
   - Barra de progreso

3. **Al terminar descarga:**
   - Título: "Actualización lista"
   - Texto: "Toca para instalar ControlOperador 1.0.3"

4. **Al tocar la notificación:**
   - Se abre el instalador de Android
   - Muestra: "¿Deseas actualizar esta aplicación?"
   - Confirmas → Se instala v1.0.3

---

## 🎯 Resumen del Flujo

```
Tablet con v1.0.2
      ↓
WorkManager ejecuta cada 15 min
      ↓
Consulta GitHub API: /releases/latest
      ↓
Detecta v1.0.3 (code 3 > code 2)
      ↓
Muestra notificación "Nueva versión"
      ↓
Descarga APK en background
      ↓
Muestra notificación "Lista para instalar"
      ↓
Usuario toca → Instalador Android
      ↓
Usuario confirma → App actualizada a v1.0.3
```

---

## 🐛 Si algo no funciona

### Release no aparece como "latest"
1. Ve a releases
2. Click en el release v1.0.3
3. Click en "Edit"
4. Marca "Set as the latest release"
5. Guarda cambios

### APK no se detecta
Verifica que:
- El archivo se llame `*.apk` (cualquier nombre)
- El archivo esté en la sección "Assets"
- El release sea público (repositorio público)

### App no detecta actualización
```bash
# Verificar API de GitHub
curl https://api.github.com/repos/Tarjetas-Integrales-Software/ControlOperador/releases/latest

# Debe devolver v1.0.3
```

---

¿Listo para publicar el release? Una vez que lo hagas, avísame y probamos el auto-update en tu tablet! 🚀
