package com.example.controloperador.ui.login

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.controloperador.R
import com.example.controloperador.databinding.FragmentLoginBinding
import com.google.android.material.snackbar.Snackbar

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var loginViewModel: LoginViewModel
    private lateinit var sessionManager: SessionManager

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
        // Guardar sesión
        sessionManager.saveOperatorSession(operatorCode)
        
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
