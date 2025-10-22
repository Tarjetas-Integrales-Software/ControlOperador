package com.example.controloperador.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class LoginViewModel : ViewModel() {

    private val _loginState = MutableLiveData<LoginState>()
    val loginState: LiveData<LoginState> = _loginState

    private val _operatorCode = MutableLiveData<String>()
    val operatorCode: LiveData<String> = _operatorCode

    // TODO: En producción, estas claves deberían venir de un servidor o base de datos encriptada
    // Esta es una lista temporal de claves válidas de operadores
    private val validOperatorCodes = setOf(
        "54321",
        "00001"
    )

    /**
     * Valida la clave de operador ingresada
     * @param code Clave numérica de 5 dígitos
     */
    fun validateOperatorCode(code: String) {
        when {
            code.isBlank() -> {
                _loginState.value = LoginState.Error("Por favor ingrese su clave de operador")
            }
            code.length != 5 -> {
                _loginState.value = LoginState.Error("La clave debe tener exactamente 5 dígitos")
            }
            !code.all { it.isDigit() } -> {
                _loginState.value = LoginState.Error("La clave debe contener solo números")
            }
            !validOperatorCodes.contains(code) -> {
                _loginState.value = LoginState.Error("Clave de operador incorrecta")
            }
            else -> {
                _operatorCode.value = code
                _loginState.value = LoginState.Success(code)
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
        _operatorCode.value = null
        _loginState.value = LoginState.Idle
    }

    /**
     * Estados posibles del proceso de login
     */
    sealed class LoginState {
        object Idle : LoginState()
        object Loading : LoginState()
        data class Success(val operatorCode: String) : LoginState()
        data class Error(val message: String) : LoginState()
    }
}
