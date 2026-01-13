package com.example.controloperador.ui.chat

import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.controloperador.R
import com.example.controloperador.data.api.model.chat.PredefinedResponse
import com.example.controloperador.databinding.FragmentChatBinding
import com.example.controloperador.ui.login.SessionManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton

/**
 * Fragment para chat en tiempo real entre operador y analistas
 * - Muestra solo mensajes del día actual
 * - Sincroniza automáticamente cada 30 segundos (Polling manual con Handler)
 * - Estados: Enviando → Enviado → Leído
 */
class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var sessionManager: SessionManager
    private val viewModel: ChatViewModel by viewModels()
    
    private var predefinedResponses: List<PredefinedResponse> = emptyList()
    private var operatorCode: String = ""
    
    // Handler para polling automático cada 30 segundos
    private val syncHandler = Handler(Looper.getMainLooper())
    private val syncRunnable = object : Runnable {
        override fun run() {
            Log.d("ChatFragment", "⏰ Auto-sync triggered (30s interval)")
            viewModel.syncMessagesNow() // Sincronizar mensajes
            syncHandler.postDelayed(this, 30_000) // Repetir cada 30 segundos
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        
        sessionManager = SessionManager(requireContext())
        operatorCode = sessionManager.getOperatorCode() ?: ""
        
        setupRecyclerView()
        setupObservers()
        setupListeners()
        
        // Inicializar chat
        viewModel.initializeChat(operatorCode)
        
        return binding.root
    }
    
    override fun onResume() {
        super.onResume()
        Log.d("ChatFragment", "🟢 Fragment resumed - Starting auto-sync")
        
        // Forzar sincronización inmediata de mensajes nuevos
        viewModel.syncMessagesNow()
        
        // Marcar mensajes como leídos al abrir el chat
        viewModel.markAllMessagesAsRead()
        
        // Iniciar polling automático cada 30 segundos
        syncHandler.post(syncRunnable)
    }
    
    override fun onPause() {
        super.onPause()
        Log.d("ChatFragment", "🔴 Fragment paused - Stopping auto-sync")
        
        // Detener polling cuando el fragment no está visible
        syncHandler.removeCallbacks(syncRunnable)
    }
    
    /**
     * Configura el RecyclerView con el adaptador
     */
    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(operatorCode)
        
        val layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = false  // Los mensajes llenan desde arriba
            reverseLayout = false // Orden normal: antiguos arriba, nuevos abajo
        }
        
        binding.messagesRecyclerView.apply {
            this.layoutManager = layoutManager
            adapter = chatAdapter
        }
    }
    
    /**
     * Configura los listeners de los botones
     */
    private fun setupListeners() {
        // Botón de respuesta predeterminada
        val responseButton = binding.root.findViewById<View>(R.id.responseButton)
        responseButton?.setOnClickListener {
            showPredefinedResponsesBottomSheet()
        }
        
        // Si estamos en landscape, configurar botones directos
        setupLandscapeResponseButtons()
    }
    
    /**
     * Configura los observadores del ViewModel
     */
    private fun setupObservers() {
        // Observar mensajes del día actual
        viewModel.todayMessages.observe(viewLifecycleOwner) { messages ->
            Log.d("ChatFragmentNew", "Received ${messages.size} messages")
            chatAdapter.submitList(messages) {
                // Callback ejecutado después de que DiffUtil termina de actualizar la lista
                // Hacer scroll al último mensaje de forma suave
                if (messages.isNotEmpty()) {
                    binding.messagesRecyclerView.post {
                        binding.messagesRecyclerView.smoothScrollToPosition(messages.size - 1)
                    }
                }
            }
        }
        
        // Observar respuestas predefinidas
        viewModel.predefinedResponses.observe(viewLifecycleOwner) { responses ->
            predefinedResponses = responses
            setupLandscapeResponseButtons() // Re-configurar botones
        }
        
        // Observar estado de envío de mensaje
        viewModel.sendMessageState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is SendMessageState.Sending -> {
                    // Mostrar indicador de envío (opcional)
                    Log.d("ChatFragmentNew", "Sending message...")
                }
                
                is SendMessageState.Success -> {
                    // Mensaje enviado exitosamente
                    Log.d("ChatFragmentNew", "Message sent successfully")
                }
                
                is SendMessageState.Error -> {
                    // Mostrar error
                    Toast.makeText(
                        requireContext(),
                        "Error: ${state.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                
                is SendMessageState.Idle -> {
                    // Estado idle
                }
            }
        }
        
        // Observar estado de respuestas predefinidas
        viewModel.responsesState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ResponsesState.Loading -> {
                    Log.d("ChatFragmentNew", "Loading predefined responses...")
                }
                
                is ResponsesState.Success -> {
                    Log.d("ChatFragmentNew", "Predefined responses loaded")
                }
                
                is ResponsesState.Error -> {
                    Log.e("ChatFragmentNew", "Error loading responses: ${state.message}")
                    // Intentar cargar respuestas locales como fallback si es necesario
                }
                
                is ResponsesState.Idle -> {}
            }
        }
        
        // Observar conteo de no leídos (opcional)
        viewModel.unreadCount.observe(viewLifecycleOwner) { count ->
            Log.d("ChatFragmentNew", "Unread messages: $count")
            // Podrías actualizar un badge en la UI si lo necesitas
        }
    }
    
    /**
     * Configura botones de respuestas predeterminadas en landscape
     */
    private fun setupLandscapeResponseButtons() {
        val container = binding.root.findViewById<LinearLayout>(R.id.responsesContainer)
        
        // Solo aplica en landscape
        if (container == null) return
        
        container.removeAllViews() // Limpiar botones anteriores
        
        // Si no hay respuestas, no mostrar nada
        if (predefinedResponses.isEmpty()) return
        
        // Crear botones dinámicamente según las respuestas del backend
        predefinedResponses.forEach { response ->
            val button = MaterialButton(
                requireContext(),
                null,
                com.google.android.material.R.attr.materialButtonStyle
            ).apply {
                text = response.mensaje
                textSize = 14f
                isAllCaps = false
                setOnClickListener {
                    sendPredefinedResponse(response)
                }
                
                // Estilo moderno con fondo blanco
                backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.white)
                )
                setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.primary_dark)
                )
                iconTint = ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.accent_gold)
                )
                
                // Agregar ícono de envío
                icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_send)
                iconGravity = MaterialButton.ICON_GRAVITY_END
                iconSize = resources.getDimensionPixelSize(R.dimen.icon_size_small)
                
                // Elevación y corners
                elevation = 4f
                cornerRadius = resources.getDimensionPixelSize(R.dimen.corner_radius_medium)
                
                // Estilo del botón
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 0, 0, 12)
                layoutParams = params
            }
            
            container.addView(button)
        }
    }
    
    /**
     * Muestra el bottom sheet con respuestas predeterminadas (portrait)
     */
    private fun showPredefinedResponsesBottomSheet() {
        if (predefinedResponses.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "Cargando respuestas predeterminadas...",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_predefined_responses, null)
        bottomSheetDialog.setContentView(sheetView)
        
        // ✨ Expandir el BottomSheet a pantalla completa
        bottomSheetDialog.behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
        bottomSheetDialog.behavior.skipCollapsed = true
        
        // Configurar altura para ocupar toda la pantalla
        sheetView.layoutParams = android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT
        )
        
        // Obtener el contenedor de botones en el bottom sheet
        val container = sheetView.findViewById<LinearLayout>(R.id.responsesContainer)
        container?.removeAllViews() // Limpiar botones anteriores
        
        // Crear botones dinámicamente según los mensajes del backend
        predefinedResponses.forEach { response ->
            val button = MaterialButton(
                requireContext(),
                null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle
            ).apply {
                text = response.mensaje
                textSize = 14f
                isAllCaps = false
                setOnClickListener {
                    sendPredefinedResponse(response)
                    bottomSheetDialog.dismiss()
                }
                
                // Estilo del botón
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 0, 0, 16)
                layoutParams = params
            }
            
            container?.addView(button)
        }
        
        // Botón de cancelar
        sheetView.findViewById<View>(R.id.cancelButton)?.setOnClickListener {
            bottomSheetDialog.dismiss()
        }
        
        // Botón de cerrar (X) en el header
        sheetView.findViewById<View>(R.id.closeButton)?.setOnClickListener {
            bottomSheetDialog.dismiss()
        }
        
        bottomSheetDialog.show()
    }
    
    /**
     * Envía una respuesta predefinida
     */
    private fun sendPredefinedResponse(response: PredefinedResponse) {
        viewModel.sendPredefinedResponse(response)
        
        // Mostrar confirmación
        Toast.makeText(
            requireContext(),
            "Enviando: ${response.mensaje}",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Detener polling si el fragment se destruye
        syncHandler.removeCallbacks(syncRunnable)
        _binding = null
    }
}
