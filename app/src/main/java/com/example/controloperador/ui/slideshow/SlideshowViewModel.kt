package com.example.controloperador.ui.slideshow

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.example.controloperador.ControlOperadorApp
import com.example.controloperador.data.database.AttendanceLog
import com.example.controloperador.data.database.AttendanceRepository
import com.example.controloperador.data.database.DailyStats
import com.example.controloperador.ui.login.SessionManager
import kotlinx.coroutines.launch

/**
 * ViewModel para la pantalla de reportes (Slideshow)
 * Maneja la lógica de obtención y sincronización de reportes de asistencia
 */
class SlideshowViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AttendanceRepository = 
        (application as ControlOperadorApp).appContainer.attendanceRepository
    
    private val sessionManager = SessionManager(application)

    // Código del operador actual
    private val _currentOperatorCode = MutableLiveData<String?>()
    
    // LiveData de reportes filtrados por operador actual
    val allReportes: LiveData<List<AttendanceLog>> = _currentOperatorCode.switchMap { operatorCode ->
        if (operatorCode != null) {
            Log.d("SlideshowViewModel", "Filtrando reportes para operador: $operatorCode")
            repository.getLogsByOperator(operatorCode)
        } else {
            Log.w("SlideshowViewModel", "No hay operador activo")
            MutableLiveData(emptyList())
        }
    }

    // Estado de sincronización
    private val _syncState = MutableLiveData<SyncState>(SyncState.Idle)
    val syncState: LiveData<SyncState> = _syncState

    // Estadísticas semanales
    private val _weeklyStats = MutableLiveData<List<DailyStats>>()
    val weeklyStats: LiveData<List<DailyStats>> = _weeklyStats

    // Total de horas semanales
    private val _totalWeeklyHours = MutableLiveData<Double>(0.0)
    val totalWeeklyHours: LiveData<Double> = _totalWeeklyHours

    init {
        // Obtener código del operador actual
        val operatorCode = sessionManager.getOperatorCode()
        _currentOperatorCode.value = operatorCode
        Log.d("SlideshowViewModel", "Inicializado con operador: $operatorCode")
        
        loadWeeklyStats()
    }
    
    /**
     * Actualiza el operador actual (útil si cambia de sesión)
     */
    fun setOperatorCode(code: String?) {
        _currentOperatorCode.value = code
        loadWeeklyStats()
    }

    /**
     * Carga las estadísticas de la última semana
     */
    fun loadWeeklyStats() {
        viewModelScope.launch {
            try {
                val stats = repository.getWeeklyStats()
                _weeklyStats.value = stats
                
                Log.d("SlideshowViewModel", "Estadísticas cargadas: ${stats.size} días")
                stats.forEach { stat ->
                    Log.d("SlideshowViewModel", "  - ${stat.date}: ${stat.totalHours} horas")
                }
                
                // Calcular total
                val total = stats.sumOf { it.totalHours }
                _totalWeeklyHours.value = total
                
                Log.d("SlideshowViewModel", "Total horas semanales: $total")
            } catch (e: Exception) {
                Log.e("SlideshowViewModel", "Error al cargar estadísticas", e)
            }
        }
    }

    /**
     * Sincroniza todos los reportes no enviados
     */
    fun syncUnsentReports() {
        viewModelScope.launch {
            _syncState.value = SyncState.Loading
            
            try {
                val (successful, failed) = repository.syncUnsentReports()
                
                if (failed > 0) {
                    _syncState.value = SyncState.PartialSuccess(successful, failed)
                } else if (successful > 0) {
                    _syncState.value = SyncState.Success(successful)
                } else {
                    _syncState.value = SyncState.NoData
                }
                
                // Recargar estadísticas
                loadWeeklyStats()
                
            } catch (e: Exception) {
                _syncState.value = SyncState.Error(e.message ?: "Error al sincronizar")
            }
        }
    }

    /**
     * Resetea el estado de sincronización
     */
    fun resetSyncState() {
        _syncState.value = SyncState.Idle
    }

    /**
     * Estados de sincronización
     */
    sealed class SyncState {
        object Idle : SyncState()
        object Loading : SyncState()
        object NoData : SyncState()
        data class Success(val count: Int) : SyncState()
        data class PartialSuccess(val successful: Int, val failed: Int) : SyncState()
        data class Error(val message: String) : SyncState()
    }
}