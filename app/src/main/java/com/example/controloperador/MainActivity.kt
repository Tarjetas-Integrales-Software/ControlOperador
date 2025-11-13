package com.example.controloperador

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.example.controloperador.databinding.ActivityMainBinding
import com.example.controloperador.ui.login.SessionManager
import com.example.controloperador.data.OperatorRepository
import com.example.controloperador.data.OperatorInfo
import com.example.controloperador.data.repository.UpdateRepository
import com.example.controloperador.data.database.AppDatabase
import com.example.controloperador.data.database.AttendanceRepository
import com.example.controloperador.data.database.chat.ChatRepository
import com.example.controloperador.data.api.RetrofitClient
import com.example.controloperador.utils.ApkInstaller
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var sessionManager: SessionManager
    private val operatorRepository = OperatorRepository()
    
    // Views del header
    private var textViewOperatorCode: TextView? = null
    private var textViewRoute: TextView? = null
    private var textViewUnit: TextView? = null
    private var textViewDateTime: TextView? = null
    
    // Información del operador
    private var operatorInfo: OperatorInfo? = null
    
    // Handler para actualizar fecha y hora
    private val dateTimeHandler = Handler(Looper.getMainLooper())
    private val dateTimeUpdateRunnable = object : Runnable {
        override fun run() {
            updateDateTime()
            dateTimeHandler.postDelayed(this, 1000) // Actualizar cada segundo
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Mostrar Android ID al inicio
        showAndroidIdDialog()
        
        // Habilitar modo kiosko (pantalla completa inmersiva)
        // IMPORTANTE: Debe llamarse DESPUÉS de setContentView()
        enableKioskMode()
        
        sessionManager = SessionManager(this)

        setSupportActionBar(binding.appBarMain.toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        
        // Configurar destinos de nivel superior
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_login, R.id.nav_home, R.id.nav_slideshow
            ), drawerLayout
        )
        
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        
        // Controlar visibilidad del drawer y toolbar según el fragmento actual
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.nav_login -> {
                    // Ocultar drawer y toolbar en login
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                    supportActionBar?.hide()
                    binding.appBarMain.toolbarLogoutButton.visibility = View.GONE
                }
                else -> {
                    // Mostrar drawer y toolbar en otras pantallas
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                    supportActionBar?.show()
                    binding.appBarMain.toolbarLogoutButton.visibility = View.VISIBLE
                    
                    // Actualizar header con código de operador
                    updateNavHeader()
                }
            }
        }
        
        // Configurar logout desde el menú del drawer
        setupLogoutMenuItem(navView)
        
        // Configurar versión de la app en el menú del drawer
        setupVersionMenuItem(navView)
        
        // Inicializar referencias a las vistas del header
        initializeHeaderViews()
        
        // Configurar botón de logout en el toolbar
        setupToolbarLogoutButton()
    }

    private fun setupToolbarLogoutButton() {
        binding.appBarMain.toolbarLogoutButton.setOnClickListener {
            showLogoutDialog()
        }
    }

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
            // Obtener información del operador (de momento desde repositorio con datos fijos)
            // TODO: Cuando la API esté lista, esto hará una llamada real al servidor
            operatorInfo = operatorRepository.getOperatorInfo(operatorCode)
            
            // Actualizar código de operador
            textViewOperatorCode?.text = "Operador: ${operatorInfo?.operatorCode}"
            
            // Actualizar ruta y unidad (actualmente valores fijos, vendrán de API)
            textViewRoute?.text = "RUTA: ${operatorInfo?.route}"
            textViewUnit?.text = "UNIDAD: ${operatorInfo?.unitNumber}"
            
            // Iniciar actualización de fecha y hora
            startDateTimeUpdates()
        }
    }
    
    private fun updateDateTime() {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy - HH:mm:ss", Locale.getDefault())
        val currentDateTime = dateFormat.format(Date())
        textViewDateTime?.text = currentDateTime
    }
    
    private fun startDateTimeUpdates() {
        // Detener cualquier actualización previa
        dateTimeHandler.removeCallbacks(dateTimeUpdateRunnable)
        // Iniciar actualizaciones
        dateTimeHandler.post(dateTimeUpdateRunnable)
    }
    
    private fun stopDateTimeUpdates() {
        dateTimeHandler.removeCallbacks(dateTimeUpdateRunnable)
    }

    private fun setupLogoutMenuItem(navView: NavigationView) {
        val logoutItem = navView.menu.findItem(R.id.nav_logout)
        
        // Inflar la vista personalizada para el logout
        val customView = layoutInflater.inflate(R.layout.menu_item_logout, null)
        logoutItem?.actionView = customView
        
        // Configurar el click en toda la vista
        customView.setOnClickListener {
            showLogoutDialog()
        }
        
        // También mantener el click listener del item por si acaso
        logoutItem?.setOnMenuItemClickListener {
            showLogoutDialog()
            true
        }
    }

    private fun setupVersionMenuItem(navView: NavigationView) {
        val menu = navView.menu
        val versionItem = menu.findItem(R.id.nav_version)
        versionItem?.title = "Versión ${BuildConfig.VERSION_NAME}"
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar Sesión")
            .setMessage(getString(R.string.logout_confirm))
            .setPositiveButton(getString(R.string.button_confirm)) { _, _ ->
                performLogout()
            }
            .setNegativeButton(getString(R.string.button_cancel), null)
            .show()
    }

    private fun performLogout() {
        // Obtener código del operador antes de limpiar la sesión
        val operatorCode = sessionManager.getOperatorCode()
        
        if (operatorCode != null) {
            // Registrar salida y sincronizar reportes
            registerExitAndSync(operatorCode)
        } else {
            // Si no hay código de operador, solo limpiar sesión y navegar
            sessionManager.clearSession()
            val navController = findNavController(R.id.nav_host_fragment_content_main)
            navController.navigate(R.id.nav_login)
        }
    }
    
    /**
     * Registra la salida del operador y sincroniza todos los reportes pendientes
     * Este método se ejecuta cuando el usuario cierra sesión
     */
    private fun registerExitAndSync(operatorCode: String) {
        // Mostrar diálogo de progreso
        val progressDialog = AlertDialog.Builder(this)
            .setTitle("Cerrando sesión")
            .setMessage("Guardando información y sincronizando reportes...")
            .setCancelable(false)
            .create()
        progressDialog.show()
        
        // Ejecutar en coroutine
        lifecycleScope.launch {
            try {
                val app = application as ControlOperadorApp
                val repository = app.appContainer.attendanceRepository
                
                // 1. Registrar salida (actualizar registro actual con hora de salida)
                android.util.Log.d("MainActivity", "Registrando salida para operador: $operatorCode")
                val exitLog = repository.registerExit(operatorCode)
                
                if (exitLog != null) {
                    android.util.Log.d("MainActivity", "✓ Salida registrada - ID: ${exitLog.id}")
                    android.util.Log.d("MainActivity", "  Tiempo operado: ${exitLog.tiempoOperando} horas")
                    
                    // 2. Intentar sincronizar el reporte recién cerrado inmediatamente
                    android.util.Log.d("MainActivity", "Intentando sincronizar reporte recién cerrado...")
                    val syncSuccess = repository.syncSingleReport(exitLog)
                    
                    if (syncSuccess) {
                        android.util.Log.d("MainActivity", "✓ Reporte actual sincronizado exitosamente")
                    } else {
                        android.util.Log.w("MainActivity", "⚠ No se pudo sincronizar reporte actual, se reintentará después")
                    }
                } else {
                    android.util.Log.w("MainActivity", "⚠ No se encontró registro abierto para cerrar")
                }
                
                // 3. Sincronizar TODOS los reportes pendientes (enviado=0)
                android.util.Log.d("MainActivity", "Sincronizando todos los reportes pendientes...")
                val (successful, failed) = repository.syncUnsentReports()
                
                android.util.Log.d("MainActivity", "Resultado de sincronización:")
                android.util.Log.d("MainActivity", "  - Exitosos: $successful")
                android.util.Log.d("MainActivity", "  - Fallidos: $failed")
                
                // Cerrar diálogo
                progressDialog.dismiss()
                
                // Mostrar resultado
                val message = when {
                    successful > 0 && failed == 0 -> 
                        "✓ Sesión cerrada\n$successful reportes sincronizados"
                    successful > 0 && failed > 0 -> 
                        "⚠ Sesión cerrada\n$successful sincronizados, $failed pendientes"
                    failed > 0 -> 
                        "⚠ Sesión cerrada\n$failed reportes pendientes (sin conexión)"
                    else -> 
                        "✓ Sesión cerrada"
                }
                
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "✗ Error en proceso de logout", e)
                progressDialog.dismiss()
                
                Toast.makeText(
                    this@MainActivity,
                    "⚠ Sesión cerrada (error al sincronizar)",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                // Siempre limpiar sesión y navegar al login
                sessionManager.clearSession()
                
                val navController = findNavController(R.id.nav_host_fragment_content_main)
                navController.navigate(R.id.nav_login)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_install_update -> {
                checkAndInstallPendingUpdates()
                true
            }
            R.id.action_settings -> {
                // Configuración próximamente
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
    
    override fun onResume() {
        super.onResume()
        // Renovar sesión cuando la app vuelve a primer plano
        if (sessionManager.isSessionActive()) {
            sessionManager.renewSession()
            startDateTimeUpdates() // Reiniciar actualización de fecha/hora
        }
        // Re-aplicar modo kiosko cuando la app regresa al primer plano
        enableKioskMode()
    }
    
    override fun onPause() {
        super.onPause()
        // Detener actualización de fecha/hora cuando la app está en segundo plano
        stopDateTimeUpdates()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Limpiar recursos
        stopDateTimeUpdates()
    }
    
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            // Re-aplicar modo kiosko si la app recupera el foco
            enableKioskMode()
        }
    }
    
    // ==================== MODO KIOSKO ====================
    
    /**
     * Muestra un diálogo con el Android ID del dispositivo
     * Se muestra durante 50 segundos al iniciar la aplicación
     */
    private fun showAndroidIdDialog() {
        try {
            // Obtener el Android ID del dispositivo
            val androidId = Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ANDROID_ID
            )
            
            // Crear el diálogo
            val dialog = AlertDialog.Builder(this)
                .setTitle("ID del Dispositivo")
                .setMessage("Android ID:\n\n$androidId\n\nEste diálogo se cerrará en 50 segundos.")
                .setCancelable(true)
                .setPositiveButton("Copiar ID") { _, _ ->
                    // Copiar al portapapeles
                    val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("Android ID", androidId)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(this, "ID copiado al portapapeles", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cerrar", null)
                .create()
            
            dialog.show()
            
            // Auto-cerrar después de 50 segundos
            Handler(Looper.getMainLooper()).postDelayed({
                if (dialog.isShowing) {
                    dialog.dismiss()
                }
            }, 50000) // 50 segundos
            
        } catch (e: Exception) {
            // Si falla, mostrar Toast simple
            Toast.makeText(
                this,
                "Error al obtener Android ID: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    /**
     * Habilita el modo kiosko (pantalla completa inmersiva)
     * Oculta completamente la barra de estado y la barra de navegación
     */
    private fun enableKioskMode() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+ (API 30+)
                window.setDecorFitsSystemWindows(false)
                window.insetsController?.let { controller ->
                    // Ocultar barras de estado y navegación
                    controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                    
                    // BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE permite que las barras aparezcan al deslizar
                    // pero se ocultan automáticamente después de unos segundos
                    controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
                
                // Listener para volver a ocultar las barras cuando aparezcan por swipe
                window.decorView.setOnApplyWindowInsetsListener { view, insets ->
                    // Si las barras se vuelven visibles, ocultarlas de nuevo
                    val insetsCompat = insets.getInsets(WindowInsets.Type.systemBars())
                    if (insetsCompat.bottom > 0 || insetsCompat.top > 0) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            window.insetsController?.hide(
                                WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars()
                            )
                        }, 3000) // Ocultar después de 3 segundos
                    }
                    view.onApplyWindowInsets(insets)
                }
            } else {
                // Android 10 y anteriores
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY // Modo pegajoso: las barras se ocultan automáticamente
                    or View.SYSTEM_UI_FLAG_FULLSCREEN    // Ocultar barra de estado
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // Ocultar barra de navegación
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                )
                
                // Listener para restaurar el modo inmersivo cuando las barras aparezcan
                window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
                    if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                        // Las barras están visibles, ocultarlas de nuevo
                        Handler(Looper.getMainLooper()).postDelayed({
                            enableKioskMode()
                        }, 2000)
                    }
                }
            }
            
            // Mantener la pantalla encendida (útil para dispositivos en camiones)
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            
        } catch (e: Exception) {
            // Si falla el modo kiosko, continuar sin él
            e.printStackTrace()
        }
    }
    
    /**
     * Prevenir que el usuario salga de la app con el botón atrás
     * Solo permitir logout a través de la opción del menú
     */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        
        // Si estamos en login, no hacer nada (no se puede salir)
        if (navController.currentDestination?.id == R.id.nav_login) {
            // Mostrar mensaje informativo
            AlertDialog.Builder(this)
                .setTitle("Modo Kiosko")
                .setMessage("Esta aplicación está en modo kiosko. Para salir, cierre sesión primero.")
                .setPositiveButton("Entendido", null)
                .show()
            return
        }
        
        // Si estamos en home, mostrar confirmación
        if (navController.currentDestination?.id == R.id.nav_home) {
            AlertDialog.Builder(this)
                .setTitle("Salir de la pantalla")
                .setMessage("¿Desea regresar a la pantalla de inicio de sesión?")
                .setPositiveButton("Sí") { _, _ ->
                    showLogoutDialog()
                }
                .setNegativeButton("No", null)
                .show()
            return
        }
        
        // Para otras pantallas, navegar normalmente
        @Suppress("DEPRECATION")
        super.onBackPressed()
    }
    
    /**
     * Verifica si hay una actualización descargada pendiente de instalar
     * Se ejecuta al iniciar la app
     */
    private fun checkForPendingUpdate() {
        // Si el intent tiene el extra para instalar actualización
        if (intent.getBooleanExtra("trigger_update_install", false)) {
            lifecycleScope.launch {
                try {
                    val updateRepository = UpdateRepository(this@MainActivity)
                    val updatesDir = updateRepository.getUpdatesDirectory()
                    
                    // Buscar APK descargado
                    val apkFile = updatesDir.listFiles()?.firstOrNull { file ->
                        file.name.endsWith(".apk")
                    }
                    
                    if (apkFile != null && apkFile.exists()) {
                        android.util.Log.d("MainActivity", "📦 APK encontrado: ${apkFile.name}")
                        
                        // Verificar si es válido
                        if (ApkInstaller.isValidApk(this@MainActivity, apkFile)) {
                            // Instalar APK
                            ApkInstaller.installApk(this@MainActivity, apkFile)
                        } else {
                            android.util.Log.e("MainActivity", "❌ APK inválido")
                            Toast.makeText(
                                this@MainActivity,
                                "Error: APK descargado está corrupto",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Error verificando actualización", e)
                }
            }
        }
    }
    
    /**
     * Sincroniza todos los datos pendientes antes de actualizar
     * Envía mensajes pendientes, reportes, asistencias, etc.
     */
    private suspend fun syncPendingData(): com.example.controloperador.data.api.Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val operatorCode = sessionManager.getOperatorCode()
                if (operatorCode == null) {
                    return@withContext com.example.controloperador.data.api.Result.Error("No hay sesión activa")
                }
                
                val results = mutableListOf<String>()
                
                // 1. Sincronizar mensajes pendientes del chat
                try {
                    val database = AppDatabase.getDatabase(this@MainActivity)
                    val chatRepository = ChatRepository(
                        database.conversationDao(),
                        database.chatMessageDao(),
                        RetrofitClient.chatApiService
                    )
                    
                    val conversation = chatRepository.getOrCreateConversation(operatorCode)
                    val syncedMessages = chatRepository.retryPendingMessages(conversation.id, operatorCode)
                    
                    if (syncedMessages > 0) {
                        results.add("✓ $syncedMessages mensajes sincronizados")
                        android.util.Log.d("MainActivity", "✅ Sincronizados $syncedMessages mensajes pendientes")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "❌ Error sincronizando mensajes: ${e.message}")
                    results.add("⚠ Error en mensajes: ${e.message}")
                }
                
                // 2. Sincronizar reportes pendientes (enviado=0)
                try {
                    val database = AppDatabase.getDatabase(this@MainActivity)
                    val attendanceRepository = AttendanceRepository(
                        database.attendanceLogDao(),
                        RetrofitClient.apiService
                    )
                    
                    val (successful, failed) = attendanceRepository.syncUnsentReports()
                    val totalReports = successful.toInt() + failed.toInt()
                    
                    if (totalReports > 0) {
                        if (successful.toInt() > 0) {
                            results.add("✓ $successful reportes sincronizados")
                            android.util.Log.d("MainActivity", "✅ Sincronizados $successful reportes pendientes")
                        }
                        if (failed.toInt() > 0) {
                            results.add("⚠ $failed reportes no se pudieron sincronizar")
                            android.util.Log.w("MainActivity", "⚠️ No se pudieron sincronizar $failed reportes")
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "❌ Error sincronizando reportes: ${e.message}")
                    results.add("⚠ Error en reportes: ${e.message}")
                }
                
                // 3. TODO: Aquí puedes agregar sincronización de otros datos si los hay
                
                val summary = if (results.isEmpty()) {
                    "No hay datos pendientes"
                } else {
                    results.joinToString("\n")
                }
                
                com.example.controloperador.data.api.Result.Success(summary)
                
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "❌ Error en sincronización: ${e.message}")
                com.example.controloperador.data.api.Result.Error("Error al sincronizar: ${e.message}")
            }
        }
    }
    
    /**
     * Sincroniza datos pendientes y luego instala la actualización
     */
    private fun syncAndInstallUpdate(apkFile: File) {
        var progressDialog: AlertDialog? = null
        
        lifecycleScope.launch {
            try {
                // Mostrar diálogo de progreso
                progressDialog = AlertDialog.Builder(this@MainActivity)
                    .setTitle("Preparando Actualización")
                    .setMessage("Sincronizando datos pendientes...\n\nEsto puede tardar unos segundos.")
                    .setCancelable(false)
                    .create()
                progressDialog?.show()
                
                android.util.Log.d("MainActivity", "🔄 Iniciando sincronización pre-actualización")
                
                // Sincronizar datos pendientes
                val syncResult = syncPendingData()
                
                // Cerrar diálogo de progreso
                progressDialog?.dismiss()
                
                when (syncResult) {
                    is com.example.controloperador.data.api.Result.Success -> {
                        android.util.Log.d("MainActivity", "✅ Sincronización completada: ${syncResult.data}")
                        
                        // Mostrar resultado de sincronización
                        val syncMessage = if (syncResult.data == "No hay datos pendientes") {
                            "No había datos pendientes.\n\n¿Proceder con la instalación?"
                        } else {
                            "${syncResult.data}\n\n¿Proceder con la instalación?"
                        }
                        
                        AlertDialog.Builder(this@MainActivity)
                            .setTitle("✓ Sincronización Completa")
                            .setMessage(syncMessage)
                            .setPositiveButton("Instalar Ahora") { _, _ ->
                                proceedWithInstallation(apkFile)
                            }
                            .setNegativeButton("Cancelar", null)
                            .setIcon(android.R.drawable.ic_dialog_info)
                            .show()
                    }
                    
                    is com.example.controloperador.data.api.Result.Error -> {
                        android.util.Log.e("MainActivity", "❌ Error en sincronización: ${syncResult.message}")
                        
                        // Mostrar error y preguntar si continuar de todos modos
                        AlertDialog.Builder(this@MainActivity)
                            .setTitle("⚠ Error de Sincronización")
                            .setMessage("${syncResult.message}\n\n¿Deseas continuar con la actualización de todos modos?\n\nNota: Los datos pendientes podrían perderse.")
                            .setPositiveButton("Continuar") { _, _ ->
                                proceedWithInstallation(apkFile)
                            }
                            .setNegativeButton("Cancelar", null)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show()
                    }
                    
                    is com.example.controloperador.data.api.Result.NetworkError -> {
                        android.util.Log.w("MainActivity", "🌐 Sin conexión durante sincronización")
                        
                        AlertDialog.Builder(this@MainActivity)
                            .setTitle("⚠ Sin Conexión")
                            .setMessage("No hay conexión a internet para sincronizar.\n\n¿Deseas continuar con la actualización?\n\nNota: Los datos pendientes se sincronizarán después.")
                            .setPositiveButton("Continuar") { _, _ ->
                                proceedWithInstallation(apkFile)
                            }
                            .setNegativeButton("Cancelar", null)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show()
                    }
                    
                    is com.example.controloperador.data.api.Result.Timeout -> {
                        android.util.Log.w("MainActivity", "⏱️ Timeout durante sincronización")
                        
                        AlertDialog.Builder(this@MainActivity)
                            .setTitle("⚠ Tiempo Agotado")
                            .setMessage("La sincronización tardó demasiado.\n\n¿Deseas continuar con la actualización?")
                            .setPositiveButton("Continuar") { _, _ ->
                                proceedWithInstallation(apkFile)
                            }
                            .setNegativeButton("Cancelar", null)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show()
                    }
                }
                
            } catch (e: Exception) {
                progressDialog?.dismiss()
                android.util.Log.e("MainActivity", "❌ Error en sincronización e instalación", e)
                
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Error")
                    .setMessage("Error inesperado: ${e.message}\n\n¿Continuar con la instalación?")
                    .setPositiveButton("Continuar") { _, _ ->
                        proceedWithInstallation(apkFile)
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        }
    }
    
    /**
     * Procede con la instalación del APK después de sincronización
     */
    private fun proceedWithInstallation(apkFile: File) {
        if (ApkInstaller.isValidApk(this, apkFile)) {
            // Verificar permiso antes de instalar
            if (ApkInstaller.canInstallPackages(this)) {
                ApkInstaller.installApk(this, apkFile)
            } else {
                // Mostrar diálogo explicativo antes de ir a configuración
                showInstallPermissionDialog()
            }
        } else {
            Toast.makeText(
                this,
                "Error: APK descargado está corrupto",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    /**
     * Verifica si hay actualizaciones descargadas pendientes de instalar
     * Muestra un diálogo al usuario para instalarlas
     */
    private fun checkAndInstallPendingUpdates() {
        lifecycleScope.launch {
            try {
                val updateRepository = UpdateRepository(this@MainActivity)
                val updatesDir = updateRepository.getUpdatesDirectory()
                
                // Buscar APK descargado
                val apkFile = updatesDir.listFiles()?.firstOrNull { file ->
                    file.name.endsWith(".apk")
                }
                
                if (apkFile != null && apkFile.exists()) {
                    android.util.Log.d("MainActivity", "📦 Actualización pendiente encontrada: ${apkFile.name}")
                    
                    // Extraer versión del nombre del archivo
                    val versionName = apkFile.name
                        .removePrefix("ControlOperador-")
                        .removeSuffix(".apk")
                        .removeSuffix("-release")
                    
                    // Mostrar diálogo
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("Actualización Disponible")
                        .setMessage("Se descargó la versión $versionName.\n\n¿Deseas instalarla ahora?\n\n⚠️ Se sincronizarán todos los datos pendientes antes de actualizar.")
                        .setPositiveButton("Instalar") { _, _ ->
                            // Sincronizar datos antes de instalar
                            syncAndInstallUpdate(apkFile)
                        }
                        .setNegativeButton("Más tarde", null)
                        .setIcon(android.R.drawable.stat_sys_download_done)
                        .show()
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "No hay actualizaciones pendientes",
                        Toast.LENGTH_SHORT
                    ).show()
                    android.util.Log.d("MainActivity", "ℹ️ No hay actualizaciones pendientes")
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error verificando actualizaciones pendientes", e)
                Toast.makeText(
                    this@MainActivity,
                    "Error al verificar actualizaciones",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    /**
     * Muestra diálogo explicativo cuando falta permiso para instalar APKs
     */
    private fun showInstallPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permiso Requerido")
            .setMessage(
                "Para instalar actualizaciones automáticamente, necesitas activar " +
                "el permiso 'Instalar apps desconocidas'.\n\n" +
                "Pasos:\n" +
                "1. Se abrirá la configuración de la app\n" +
                "2. Activa 'Permitir instalar apps desconocidas'\n" +
                "3. Regresa e intenta instalar nuevamente"
            )
            .setPositiveButton("Ir a Configuración") { _, _ ->
                ApkInstaller.openInstallPermissionSettings(this)
            }
            .setNegativeButton("Cancelar", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }
}
