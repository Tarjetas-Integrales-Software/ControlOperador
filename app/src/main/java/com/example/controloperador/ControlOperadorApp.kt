package com.example.controloperador

import android.app.Application
import com.example.controloperador.data.AppContainer

/**
 * Clase Application personalizada para inicializar dependencias globales
 */
class ControlOperadorApp : Application() {
    
    // Contenedor de dependencias accesible desde toda la aplicación
    lateinit var appContainer: AppContainer
        private set
    
    override fun onCreate() {
        super.onCreate()
        
        // Inicializar contenedor de dependencias
        appContainer = AppContainer(this)
    }
}
