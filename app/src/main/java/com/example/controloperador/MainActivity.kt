package com.example.controloperador

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import com.google.android.material.snackbar.Snackbar
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
        
        sessionManager = SessionManager(this)

        setSupportActionBar(binding.appBarMain.toolbar)

        binding.appBarMain.fab.setOnClickListener { view ->
            Snackbar.make(view, "Acción rápida de operador", Snackbar.LENGTH_LONG)
                .setAction("OK", null)
                .setAnchorView(R.id.fab).show()
        }
        
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        
        // Configurar destinos de nivel superior
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_login, R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow
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
                    binding.appBarMain.fab.visibility = View.GONE
                }
                else -> {
                    // Mostrar drawer y toolbar en otras pantallas
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                    supportActionBar?.show()
                    binding.appBarMain.fab.visibility = View.VISIBLE
                    
                    // Actualizar header con código de operador
                    updateNavHeader()
                }
            }
        }
        
        // Configurar logout desde el menú del drawer
        setupLogoutMenuItem(navView)
        
        // Inicializar referencias a las vistas del header
        initializeHeaderViews()
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
        
        Snackbar.make(
            binding.root,
            getString(R.string.logout_success),
            Snackbar.LENGTH_SHORT
        ).show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                Snackbar.make(binding.root, "Configuración próximamente", Snackbar.LENGTH_SHORT).show()
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
}