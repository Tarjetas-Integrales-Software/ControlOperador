package com.example.controloperador.ui.login

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.controloperador.ControlOperadorApp
import com.example.controloperador.R
import com.example.controloperador.databinding.FragmentLoginBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var loginViewModel: LoginViewModel
    private lateinit var sessionManager: SessionManager
    
    companion object {
        private const val TAG = "LoginFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        loginViewModel = ViewModelProvider(this)[LoginViewModel::class.java]
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        
        sessionManager = SessionManager(requireContext())

        setupUI()
        observeViewModel()
        checkExistingSession()

        return binding.root
    }

    private fun setupUI() {
        // Configurar botón de login
        binding.loginButton.setOnClickListener {
            performLogin()
        }

        // Permitir login con tecla Enter/Done
        binding.operatorCodeInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                performLogin()
                true
            } else {
                false
            }
        }

        // Limpiar error al escribir
        binding.operatorCodeInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.operatorCodeLayout.error = null
                binding.errorMessage.visibility = View.GONE
            }
        }
    }

    private fun observeViewModel() {
        loginViewModel.loginState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is LoginViewModel.LoginState.Idle -> {
                    hideLoading()
                }
                is LoginViewModel.LoginState.Loading -> {
                    showLoading()
                }
                is LoginViewModel.LoginState.Success -> {
                    hideLoading()
                    handleLoginSuccess(state.operatorCode, state.operatorName)
                }
                is LoginViewModel.LoginState.Error -> {
                    hideLoading()
                    showError(state.message)
                }
            }
        }
    }

    private fun performLogin() {
        val code = binding.operatorCodeInput.text?.toString() ?: ""
        hideKeyboard()
        loginViewModel.validateOperatorCode(code)
    }

    private fun handleLoginSuccess(operatorCode: String, operatorName: String = "") {
        Log.d(TAG, "Login exitoso para operador: $operatorCode ($operatorName)")
        
        // Guardar sesión
        sessionManager.saveOperatorSession(operatorCode)
        
        // Registrar entrada en la base de datos local
        registerEntryInDatabase(operatorCode, operatorName)
        
        // Mostrar mensaje de éxito con nombre del operador si está disponible
        val message = if (operatorName.isNotEmpty()) {
            "Bienvenido, $operatorName"
        } else {
            getString(R.string.login_success)
        }
        
        Snackbar.make(
            binding.root,
            message,
            Snackbar.LENGTH_SHORT
        ).show()

        // Navegar al home después de un breve delay para mostrar el mensaje
        binding.root.postDelayed({
            findNavController().navigate(R.id.action_login_to_home)
        }, 500)
    }
    
    /**
     * Registra la entrada del operador en la base de datos local
     * Se ejecuta automáticamente después del login exitoso
     */
    private fun registerEntryInDatabase(operatorCode: String, fullName: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val app = requireActivity().application as ControlOperadorApp
                val repository = app.appContainer.attendanceRepository
                
                // Obtener datos del operador desde LoginResponse
                val loginResponse = loginViewModel.getLastLoginResponse()
                
                val nombre: String
                val apellidoPaterno: String
                val apellidoMaterno: String
                
                if (loginResponse != null) {
                    // Usar datos del backend (campos específicos)
                    nombre = loginResponse.operator.nombre ?: loginResponse.operator.name.split(" ").getOrNull(0) ?: "Operador"
                    apellidoPaterno = loginResponse.operator.apellido_paterno ?: loginResponse.operator.name.split(" ").getOrNull(1) ?: ""
                    apellidoMaterno = loginResponse.operator.apellido_materno ?: loginResponse.operator.name.split(" ").getOrNull(2) ?: ""
                    
                    Log.d(TAG, "Usando datos del backend:")
                    Log.d(TAG, "  - nombre: $nombre")
                    Log.d(TAG, "  - apellido_paterno: $apellidoPaterno")
                    Log.d(TAG, "  - apellido_materno: $apellidoMaterno")
                } else {
                    // Fallback: extraer del nombre completo
                    val nameParts = fullName.trim().split(" ")
                    nombre = nameParts.getOrNull(0) ?: "Operador"
                    apellidoPaterno = nameParts.getOrNull(1) ?: ""
                    apellidoMaterno = nameParts.getOrNull(2) ?: ""
                    
                    Log.d(TAG, "Usando fallback de nombre completo: $fullName")
                }
                
                // Registrar entrada en base de datos
                val logId = repository.registerEntry(
                    operatorCode = operatorCode,
                    nombre = nombre,
                    apellidoPaterno = apellidoPaterno,
                    apellidoMaterno = apellidoMaterno
                )
                
                Log.d(TAG, "✓ Entrada registrada exitosamente")
                Log.d(TAG, "  - ID registro: $logId")
                Log.d(TAG, "  - Operador: $operatorCode")
                Log.d(TAG, "  - Nombre completo: $nombre $apellidoPaterno $apellidoMaterno")
                Log.d(TAG, "  - Hora entrada: ${java.util.Date()}")
                
            } catch (e: Exception) {
                Log.e(TAG, "✗ Error al registrar entrada en base de datos", e)
                // No mostramos error al usuario para no interrumpir el flujo de login
                // pero sí lo registramos en logs para debugging
            }
        }
    }

    private fun checkExistingSession() {
        if (sessionManager.isSessionActive()) {
            // Si ya hay una sesión activa, navegar directamente al home
            findNavController().navigate(R.id.action_login_to_home)
        }
    }

    private fun showError(message: String) {
        binding.operatorCodeLayout.error = message
        binding.errorMessage.text = message
        binding.errorMessage.visibility = View.VISIBLE
        
        // Limpiar el campo y solicitar focus
        binding.operatorCodeInput.text?.clear()
        binding.operatorCodeInput.requestFocus()
    }

    private fun showLoading() {
        binding.loginButton.isEnabled = false
        binding.operatorCodeInput.isEnabled = false
        binding.loginButton.text = "Validando..."
    }

    private fun hideLoading() {
        binding.loginButton.isEnabled = true
        binding.operatorCodeInput.isEnabled = true
        binding.loginButton.text = getString(R.string.login_button)
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
