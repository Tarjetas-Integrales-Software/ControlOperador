# Publicar Release v1.0.6 en GitHub

## 📦 Archivos Preparados

- ✅ APK firmada: `~/Desktop/ControlOperador-v1.0.6-release.apk` (14 MB)
- ✅ Tag v1.0.6 creado y subido a GitHub
- ✅ Commit con cambios subido a rama `operadorDan`

## 🚀 Pasos para Publicar el Release

### 1. Ir a GitHub Releases
Abre esta URL en tu navegador:
```
https://github.com/Tarjetas-Integrales-Software/ControlOperador/releases/new?tag=v1.0.6
```

### 2. Configurar el Release

**Release title:**
```
Control Operador v1.0.6 - Sincronización Pre-Actualización
```

**Description:**
```markdown
## 🆕 Novedades v1.0.6

### 🔄 Sincronización Automática de Datos
- **Sincronización antes de actualizar**: Ahora la app sincroniza automáticamente todos los mensajes pendientes antes de instalar una actualización
- **Diálogo de progreso**: Muestra el estado de sincronización en tiempo real
- **Manejo inteligente de errores**: Si la sincronización falla, el usuario puede decidir si continuar o cancelar
- **Protección de datos**: Reduce el riesgo de pérdida de mensajes durante actualizaciones

### 🎨 Mejoras de UI
- **Botón de logout rediseñado**: Ahora aparece como un botón rojo distintivo con texto blanco
- **Separador en menú**: Nueva línea divisoria entre opciones de navegación e información
- **Mejor estructura del drawer**: Menú más limpio sin títulos de sección redundantes

### 🔧 Mejoras Técnicas
- Función `syncPendingData()` para sincronización automática
- Función `syncAndInstallUpdate()` con manejo de errores robusto
- Logs detallados para debugging del proceso de actualización
- Integración con ChatRepository para sincronizar mensajes

### 📱 Flujo de Actualización Mejorado
1. Usuario presiona "Instalar Actualización"
2. Se muestra aviso: "Se sincronizarán datos pendientes"
3. La app sincroniza mensajes pendientes automáticamente
4. Se muestra resumen de sincronización
5. Usuario confirma instalación
6. Se instala la nueva versión

### ⚠️ Nota Importante
- Esta versión requiere los mismos permisos que la v1.0.5
- Compatible con actualizaciones desde v1.0.5 (mismo keystore)
- **No compatible** con v1.0.3 o anteriores (diferente firma)

---

## 📋 Requisitos

- Android 10.0 (API 29) o superior
- Permiso "Instalar apps desconocidas" (solo para auto-actualización)
- Conexión a internet para sincronización de datos

## 🔐 Información de Firma

- **Keystore**: controloperador-new.jks
- **Firma**: Compatible con v1.0.5
- **Algoritmo**: SHA-256

---

## 🐛 Correcciones de Bugs

- Corregido: Import incorrecto de ChatDatabase (ahora usa AppDatabase)
- Mejorado: Manejo de errores en sincronización de mensajes

## 🔄 Cambios desde v1.0.5

- Nueva funcionalidad de sincronización pre-actualización
- Rediseño del botón de logout
- Mejoras en la estructura del menú lateral
- Logs más detallados para debugging

---

**Instalación Manual:**
1. Descarga el archivo APK
2. Si tienes v1.0.5, simplemente instala sobre ella
3. Si tienes v1.0.3 o anterior, desinstala primero (perderás datos locales)
4. Permite la instalación de apps desconocidas si es necesario

**Auto-Actualización:**
Si tienes v1.0.5 con auto-update activado, la app detectará automáticamente esta nueva versión en aproximadamente 5-15 minutos (según optimizaciones del sistema).
```

### 3. Subir el APK

1. En la sección "Attach binaries", arrastra el archivo:
   ```
   ~/Desktop/ControlOperador-v1.0.6-release.apk
   ```

2. Espera a que se suba completamente (14 MB)

### 4. Configurar Opciones

- ✅ Marca: **"Set as the latest release"**
- ❌ **NO** marcar "Set as a pre-release"

### 5. Publicar

Clic en el botón verde **"Publish release"**

---

## ✅ Verificación Post-Publicación

Después de publicar, verifica:

1. **Release visible**: https://github.com/Tarjetas-Integrales-Software/ControlOperador/releases
2. **APK descargable**: Prueba descargar el APK desde el release
3. **Tag correcto**: v1.0.6 debe aparecer en la lista de tags
4. **Latest release badge**: Debe mostrar v1.0.6

---

## 🧪 Pruebas Recomendadas

Después de publicar, prueba:

1. **Auto-update desde v1.0.5**:
   - Instala v1.0.5 en un dispositivo
   - Espera 5-15 minutos (según optimizaciones del sistema)
   - Verifica que detecte v1.0.6
   - Prueba el flujo completo de actualización

2. **Sincronización de datos**:
   - Deja mensajes pendientes en el chat
   - Inicia actualización a v1.0.6
   - Verifica que muestre "X mensajes sincronizados"
   - Confirma que los mensajes llegaron al servidor

3. **Botón de logout**:
   - Verifica que aparece en rojo con texto blanco
   - Confirma que el diálogo de logout funciona

---

## 📊 Changelog Técnico

### Archivos Modificados

- `MainActivity.kt`:
  - Nueva función `syncPendingData()`
  - Nueva función `syncAndInstallUpdate()`
  - Nueva función `proceedWithInstallation()`
  - Imports actualizados (AppDatabase, ChatRepository)

- `activity_main_drawer.xml`:
  - Segundo grupo agregado para línea divisoria
  - Items de versión y logout en grupo separado

- `menu_item_logout.xml`:
  - Layout personalizado para logout
  - Background rojo con texto blanco

- `bg_logout_button.xml`:
  - Drawable con fondo rojo sólido
  - Esquinas redondeadas (12dp)

### Dependencias

No hay cambios en dependencias para esta versión.

---

## 🔗 Enlaces Útiles

- **Repositorio**: https://github.com/Tarjetas-Integrales-Software/ControlOperador
- **Releases**: https://github.com/Tarjetas-Integrales-Software/ControlOperador/releases
- **Issues**: https://github.com/Tarjetas-Integrales-Software/ControlOperador/issues
- **Tag v1.0.6**: https://github.com/Tarjetas-Integrales-Software/ControlOperador/releases/tag/v1.0.6

---

## 📝 Notas Finales

- Esta versión mejora significativamente la experiencia de actualización
- La sincronización automática protege los datos del usuario
- El nuevo diseño del botón de logout es más intuitivo
- Compatible con el flujo de auto-actualización establecido

**Próximas mejoras sugeridas:**
- Sincronizar también reportes pendientes
- Sincronizar asistencias pendientes
- Agregar opción para forzar sincronización manual
- Mostrar indicador de datos pendientes en la UI
