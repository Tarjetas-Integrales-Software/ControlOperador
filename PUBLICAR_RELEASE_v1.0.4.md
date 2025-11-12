# 📦 Instrucciones para Publicar Release v1.0.4

## ✅ Estado Actual

- **Versión**: 1.0.4 (versionCode: 4)
- **APK Firmado**: `~/Desktop/ControlOperador-v1.0.4-release.apk` (14 MB)
- **Tag Git**: `v1.0.4` ✅ Ya subido a GitHub
- **Commit**: `1ac0220` ✅ Ya subido a GitHub
- **Tablet**: Actualmente tiene v1.0.3 instalada

## 🎯 Nuevas Funcionalidades en v1.0.4

1. **Visualización dinámica de versión en menú lateral**
   - Ahora el drawer muestra la versión actual de la app
   - Se lee automáticamente de `BuildConfig.VERSION_NAME`
   - Ubicación: Sección "Información" debajo de "Cerrar Sesión"

2. **Mejoras en el sistema de auto-actualización**
   - Botón manual de instalación en toolbar
   - Manejo mejorado de actualizaciones descargadas

## 📝 Pasos para Publicar en GitHub

### 1. Ir a la página de releases
```
https://github.com/Tarjetas-Integrales-Software/ControlOperador/releases/new
```

### 2. Configurar el Release

**Choose a tag**: `v1.0.4` (seleccionar el tag existente)

**Release title**: 
```
Control Operador v1.0.4
```

**Description**:
```markdown
## 🚀 Novedades

### ✨ Nueva Funcionalidad
- **Visualización de versión en menú lateral**: Ahora puedes ver la versión actual de la app en el drawer, en la sección "Información"
- La versión se actualiza automáticamente cuando se instala una nueva actualización

### 🔧 Mejoras
- Mejoras en el sistema de auto-actualización
- Botón manual para instalar actualizaciones descargadas
- Optimizaciones de interfaz de usuario

### 📱 Instalación
1. Descarga el archivo `ControlOperador-v1.0.4-release.apk`
2. Instala el APK en tu dispositivo
3. Si tienes v1.0.3, la app detectará automáticamente esta actualización

### 🔄 Auto-Update
Esta versión incluye el sistema de auto-actualización que verifica nuevas versiones cada 15 minutos cuando hay conexión a internet.

---

**Versión anterior**: v1.0.3  
**Fecha de lanzamiento**: 11 de noviembre de 2025
```

### 3. Adjuntar el APK

- Click en el área de "Attach binaries..."
- Seleccionar el archivo: `~/Desktop/ControlOperador-v1.0.4-release.apk`
- Esperar a que se suba completamente (14 MB)

### 4. Marcar como Latest Release

- ✅ Marcar la casilla "Set as the latest release"
- ⚠️ NO marcar "Set as a pre-release" (dejar sin marcar)

### 5. Publicar

- Click en el botón verde **"Publish release"**

## 🧪 Pruebas Post-Publicación

### Verificar que el auto-update funcione:

1. La tablet tiene v1.0.3 instalada
2. En máximo 15 minutos, UpdateCheckWorker detectará v1.0.4
3. Descargará automáticamente el APK (14 MB)
4. Aparecerá el ícono de descarga en el toolbar
5. Al hacer clic, mostrará diálogo para instalar

### Comandos para monitorear logs:

```bash
# Ver logs del sistema de auto-update
adb logcat | grep -E "UpdateCheckWorker|UpdateRepository|ApkInstaller"

# Ver logs con emojis para identificar fácilmente
adb logcat | grep "🔄\|📦\|🆕\|⬇️\|✅"
```

## 📊 Flujo de Actualización Esperado

```
v1.0.3 (Tablet) 
    ↓
UpdateCheckWorker detecta v1.0.4 en GitHub
    ↓
Descarga automática del APK
    ↓
Usuario ve ícono de descarga en toolbar
    ↓
Click en ícono → Diálogo "Actualización Disponible"
    ↓
"Instalar" → Android Package Installer
    ↓
v1.0.4 instalada → Drawer muestra "Versión 1.0.4"
```

## 🔍 Verificación en la Tablet

Después de instalar v1.0.4:

1. Abrir el menú lateral (drawer)
2. Ir a la sección "Información"
3. Verificar que muestre: **"Versión 1.0.4"**

## 📁 Archivos Generados

- ✅ `~/Desktop/ControlOperador-v1.0.4-release.apk` (14 MB, firmado)
- ✅ Tag `v1.0.4` en GitHub
- ✅ Commit `1ac0220` en branch `operadorDan`

## 🎉 Checklist de Publicación

- [ ] Publicar release en GitHub con APK adjunto
- [ ] Marcar como "Latest release"
- [ ] Esperar 15 minutos para que la tablet detecte la actualización
- [ ] Verificar logs de UpdateCheckWorker
- [ ] Instalar actualización usando el botón manual
- [ ] Confirmar que drawer muestre "Versión 1.0.4"

---

**Notas importantes**:
- La tablet NO se actualiza automáticamente hasta que el usuario confirme la instalación
- El sistema solo descarga el APK automáticamente
- El usuario debe hacer clic en el ícono de descarga y confirmar la instalación
- Si las notificaciones están bloqueadas, se debe usar el botón manual del toolbar
