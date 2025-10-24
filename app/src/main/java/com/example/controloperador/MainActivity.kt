package com.example.controloperador

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.TextView
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import com.example.controloperador.databinding.ActivityMainBinding
import com.example.controloperador.ui.login.SessionManager
import com.example.controloperador.data.OperatorRepository
import com.example.controloperador.data.OperatorInfo
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
        navView.menu.findItem(R.id.nav_logout)?.setOnMenuItemClickListener {
            showLogoutDialog()
            true
        }
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
        sessionManager.clearSession()
        
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        navController.navigate(R.id.nav_login)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
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
     * Habilita el modo kiosko (pantalla completa inmersiva)
     * Oculta la barra de estado y la barra de navegación
     */
    private fun enableKioskMode() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+ (API 30+)
                window.setDecorFitsSystemWindows(false)
                window.insetsController?.let { controller ->
                    controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                    controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {
                // Android 10 y anteriores
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                )
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
}