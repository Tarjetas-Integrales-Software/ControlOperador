package com.example.controloperador.ui.login

import android.content.Context
import android.content.SharedPreferences

/**
 * Gestor de sesión de operador usando SharedPreferences
 * Maneja la persistencia de la sesión del operador
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    companion object {
        private const val PREFS_NAME = "ControlOperadorPrefs"
        private const val KEY_IS_LOGGED_IN = "isLoggedIn"
        private const val KEY_OPERATOR_CODE = "operatorCode"
        private const val KEY_LOGIN_TIME = "loginTime"
        private const val SESSION_TIMEOUT = 8 * 60 * 60 * 1000L // 8 horas en milisegundos
    }

    /**
     * Guarda la sesión del operador
     */
    fun saveOperatorSession(operatorCode: String) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_OPERATOR_CODE, operatorCode)
            putLong(KEY_LOGIN_TIME, System.currentTimeMillis())
            apply()
        }
    }

    /**
     * Verifica si hay una sesión activa y válida
     */
    fun isSessionActive(): Boolean {
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        if (!isLoggedIn) return false

        // Verificar si la sesión ha expirado
        val loginTime = prefs.getLong(KEY_LOGIN_TIME, 0L)
        val currentTime = System.currentTimeMillis()
        val sessionDuration = currentTime - loginTime

        return sessionDuration < SESSION_TIMEOUT
    }

    /**
     * Obtiene el código del operador de la sesión actual
     */
    fun getOperatorCode(): String? {
        return if (isSessionActive()) {
            prefs.getString(KEY_OPERATOR_CODE, null)
        } else {
            null
        }
    }

    /**
     * Cierra la sesión del operador
     */
    fun clearSession() {
        prefs.edit().apply {
            clear()
            apply()
        }
    }

    /**
     * Renueva el tiempo de la sesión (para mantener la sesión activa)
     */
    fun renewSession() {
        if (isSessionActive()) {
            prefs.edit().apply {
                putLong(KEY_LOGIN_TIME, System.currentTimeMillis())
                apply()
            }
        }
    }
}
