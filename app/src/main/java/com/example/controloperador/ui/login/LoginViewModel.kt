package com.example.controloperador.ui.login

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.controloperador.data.api.AuthRepository
import com.example.controloperador.data.api.model.LoginResponse
import kotlinx.coroutines.launch

/**
 * ViewModel para la pantalla de Login
 * Maneja la lógica de autenticación con API REST
 */
class LoginViewModel : ViewModel() {

    private val authRepository = AuthRepository()
    
    private val _loginState = MutableLiveData<LoginState>()
    val loginState: LiveData<LoginState> = _loginState

    private val _operatorCode = MutableLiveData<String>()
    val operatorCode: LiveData<String> = _operatorCode
    
    private val _operatorName = MutableLiveData<String>()
    val operatorName: LiveData<String> = _operatorName
    
    // Almacenar respuesta completa del login para acceso a datos del operador
    private var lastLoginResponse: LoginResponse? = null
    
    fun getLastLoginResponse(): LoginResponse? = lastLoginResponse
    
    companion object {
        private const val TAG = "LoginViewModel"
    }

    /**
     * Valida y autentica la clave de operador con el backend
     * @param code Clave numérica de 5 dígitos
     */
    fun validateOperatorCode(code: String) {
        // Validación local primero
        when {
            code.isBlank() -> {
                _loginState.value = LoginState.Error("Por favor ingrese su clave de operador")
                return
            }
            code.length != 5 -> {
                _loginState.value = LoginState.Error("La clave debe tener exactamente 5 dígitos")
                return
            }
            !code.all { it.isDigit() } -> {
                _loginState.value = LoginState.Error("La clave debe contener solo números")
                return
            }
        }
        
        // Usuario de prueba offline: 54321
        if (code == "54321") {
            authenticateTestUser(code)
        } else {
            // Si pasa validación local, autenticar con servidor
            authenticateWithServer(code)
        }
    }
    
    /**
     * Autentica usuario de prueba sin conexión al backend
     * Código de prueba: 54321
     */
    private fun authenticateTestUser(code: String) {
        _loginState.value = LoginState.Loading
        
        // Simular delay de red
        viewModelScope.launch {
            kotlinx.coroutines.delay(500)
            
            _operatorCode.value = code
            _operatorName.value = "Juan Hernández"
            _loginState.value = LoginState.Success(
                operatorCode = code,
                operatorName = "Juan Hernández"
            )
        }
    }
    
    /**
     * Autentica el operador con el servidor backend
     */
    private fun authenticateWithServer(code: String) {
        Log.d(TAG, "Starting authentication for code: $code")
        _loginState.value = LoginState.Loading
        
        viewModelScope.launch {
            when (val result = authRepository.login(code)) {
                is AuthRepository.Result.Success -> {
                    Log.d(TAG, "Authentication successful")
                    val loginResponse = result.data
                    Log.d(TAG, "Operator code: ${loginResponse.operator.operator_code}")
                    Log.d(TAG, "Operator name: ${loginResponse.operator.name}")
                    
                    // Guardar respuesta completa para acceso posterior
                    lastLoginResponse = loginResponse
                    
                    _operatorCode.value = loginResponse.operator.operator_code
                    _operatorName.value = loginResponse.operator.name
                    _loginState.value = LoginState.Success(
                        operatorCode = loginResponse.operator.operator_code,
                        operatorName = loginResponse.operator.name
                    )
                    Log.d(TAG, "LoginState updated to Success")
                }
                is AuthRepository.Result.Error -> {
                    Log.e(TAG, "Authentication error: ${result.message}")
                    _loginState.value = LoginState.Error(result.message)
                }
                is AuthRepository.Result.NetworkError -> {
                    Log.e(TAG, "Network error")
                    _loginState.value = LoginState.Error(
                        "Sin conexión a internet.\nVerifique su conexión e intente nuevamente."
                    )
                }
                is AuthRepository.Result.Timeout -> {
                    Log.e(TAG, "Timeout error")
                    _loginState.value = LoginState.Error(
                        "Tiempo de espera agotado.\nEl servidor no respondió a tiempo."
                    )
                }
            }
        }
    }

    /**
     * Limpia el estado de login
     */
    fun clearLoginState() {
        _loginState.value = LoginState.Idle
    }

    /**
     * Cierra la sesión del operador
     */
    fun logout() {
        val code = _operatorCode.value
        if (code != null) {
            viewModelScope.launch {
                authRepository.logout(code)
            }
        }
        _operatorCode.value = null
        _operatorName.value = null
        _loginState.value = LoginState.Idle
    }
    
    /**
     * Verifica la conectividad con el servidor
     */
    fun checkServerConnection(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val isConnected = authRepository.checkConnection()
            onResult(isConnected)
        }
    }

    /**
     * Estados posibles del proceso de login
     */
    sealed class LoginState {
        object Idle : LoginState()
        object Loading : LoginState()
        data class Success(val operatorCode: String, val operatorName: String) : LoginState()
        data class Error(val message: String) : LoginState()
    }
}
