package com.example.controloperador.ui.home

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.controloperador.R
import com.example.controloperador.databinding.FragmentHomeBinding
import com.example.controloperador.ui.login.SessionManager
import com.example.controloperador.ui.chat.ChatViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var sessionManager: SessionManager
    
    // ViewModel compartido con ChatFragment para sincronizar conversaciones
    private val chatViewModel: ChatViewModel by activityViewModels()
    
    // Adaptador para el chat integrado (solo en landscape)
    private var chatAdapter: com.example.controloperador.ui.chat.ChatAdapter? = null
    
    // Handler para actualizar el timer en tiempo real
    private val handler = Handler(Looper.getMainLooper())
    private val dateTimeFormat = SimpleDateFormat("HH:mm:ss - dd/MM/yy", Locale.getDefault())
    private val timerRunnable = object : Runnable {
        override fun run() {
            // Usar findViewById para null-safety (el timer puede no existir en portrait)
            binding.root.findViewById<TextView>(R.id.textViewDateTime)?.text = dateTimeFormat.format(Date())
            handler.postDelayed(this, 1000) // Actualizar cada segundo
        }
    }
    
    // Handler para sincronizar chat cada 30 segundos (solo cuando está visible)
    private val chatSyncHandler = Handler(Looper.getMainLooper())
    private val chatSyncRunnable = object : Runnable {
        override fun run() {
            android.util.Log.d("HomeFragment", "⏰ Auto-sync chat triggered (30s interval)")
            chatViewModel.syncMessagesNow() // Sincronizar mensajes
            chatSyncHandler.postDelayed(this, 30_000) // Repetir cada 30 segundos
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root
        
        sessionManager = SessionManager(requireContext())
        
        setupWelcomeMessage()
        startRealtimeTimer() // Iniciar timer en tiempo real
        setupIntegratedChat() // Configurar chat integrado en landscape
        observeChatViewModel() // Observar mensajes compartidos con ChatFragment
        loadMessagesSummary()
        setupClickListeners()

        return root
    }
    
    private fun setupWelcomeMessage() {
        val operatorCode = sessionManager.getOperatorCode()
        binding.welcomeText.text = "Bienvenido, Operador $operatorCode"
    }
    
    /**
     * Inicia el timer que actualiza la fecha/hora en tiempo real cada segundo
     */
    private fun startRealtimeTimer() {
        handler.post(timerRunnable)
    }
    
    /**
     * Observa los cambios en el ViewModel compartido con ChatFragment
     * Mantiene sincronizada la conversación entre ambas pantallas
     */
    private fun observeChatViewModel() {
        val operatorCode = sessionManager.getOperatorCode() ?: return
        
        // Inicializar chat con el código del operador
        chatViewModel.initializeChat(operatorCode)
        
        // Observar contador de mensajes no leídos
        chatViewModel.unreadCount.observe(viewLifecycleOwner) { count ->
            updateUnreadBadge(count)
        }
        
        // Observar respuestas predefinidas dinámicas
        chatViewModel.predefinedResponses.observe(viewLifecycleOwner) { responses ->
            // Actualizar bottom sheet con respuestas del servidor
            // (implementación en setupResponseButton)
        }
        
        // Nota: La observación de mensajes se hace en setupIntegratedChat() para landscape
    }
    
    /**
     * Configura el chat integrado en la card (solo en landscape)
     */
    private fun setupIntegratedChat() {
        // Verificar si existe el RecyclerView (solo en landscape)
        val messagesRecyclerView = binding.root.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.messagesRecyclerView)
        val responseButton = binding.root.findViewById<com.google.android.material.button.MaterialButton>(R.id.responseButton)
        
        if (messagesRecyclerView != null && responseButton != null) {
            // Estamos en landscape, configurar chat integrado
            val operatorCode = sessionManager.getOperatorCode() ?: return
            chatAdapter = com.example.controloperador.ui.chat.ChatAdapter(operatorCode)
            
            // Configurar LinearLayoutManager para chat convencional
            val layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = false  // Los mensajes llenan desde arriba
                reverseLayout = false // Orden normal: antiguos arriba, nuevos abajo
            }
            
            messagesRecyclerView.apply {
                this.layoutManager = layoutManager
                adapter = chatAdapter
            }
            
            // Auto-scroll al último mensaje cuando se actualizan (solo últimos 10)
            chatViewModel.todayMessages.observe(viewLifecycleOwner) { messages ->
                val lastMessages = messages.takeLast(10)
                chatAdapter?.submitList(lastMessages) {
                    // Callback después de que DiffUtil actualiza la lista
                    if (lastMessages.isNotEmpty()) {
                        messagesRecyclerView.post {
                            // Scroll suave al último mensaje visible
                            messagesRecyclerView.smoothScrollToPosition(lastMessages.size - 1)
                        }
                    }
                }
            }
            
            // Configurar botón de respuestas predeterminadas
            responseButton.setOnClickListener {
                showPredefinedResponsesDialog()
            }
        }
    }
    
    /**
     * Muestra el Bottom Sheet con respuestas predefinidas (Material Design 3)
     * Las opciones se cargan dinámicamente desde el backend
     */
    private fun showPredefinedResponsesDialog() {
        // Cargar respuestas desde servidor
        chatViewModel.loadPredefinedResponses()
        
        // Obtener respuestas actuales
        val responses = chatViewModel.predefinedResponses.value
        if (responses.isNullOrEmpty()) {
            Toast.makeText(
                requireContext(),
                "Cargando respuestas predeterminadas...",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        
        // Crear Bottom Sheet Dialog (Material Design 3)
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
        
        // Crear botones dinámicamente según las respuestas del backend
        responses.forEach { response ->
            val button = MaterialButton(
                requireContext(),
                null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle
            ).apply {
                id = View.generateViewId()
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = resources.getDimensionPixelSize(R.dimen.button_margin)
                }
                text = response.mensaje // Texto de la respuesta
                textAlignment = View.TEXT_ALIGNMENT_TEXT_START
                textSize = 13f
                setPadding(
                    resources.getDimensionPixelSize(R.dimen.button_padding_horizontal),
                    paddingTop,
                    resources.getDimensionPixelSize(R.dimen.button_padding_horizontal),
                    paddingBottom
                )
                cornerRadius = resources.getDimensionPixelSize(R.dimen.button_corner_radius)
                
                setOnClickListener {
                    sendPredefinedResponse(response.mensaje)
                    bottomSheetDialog.dismiss()
                }
            }
            
            container.addView(button)
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
     * Envía una respuesta predeterminada
     * Agrega el mensaje a la conversación compartida entre HomeFragment y ChatFragment
     */
    private fun sendPredefinedResponse(response: String) {
        // Enviar mensaje usando ChatViewModel (automáticamente sincroniza con backend)
        chatViewModel.sendMessage(response)
        
        // Mostrar feedback
        Toast.makeText(
            requireContext(),
            getString(R.string.messages_sent),
            Toast.LENGTH_SHORT
        ).show()
        
        // El auto-scroll se maneja en el observer de todayMessages en setupIntegratedChat()
    }
    
    /**
     * Actualiza el badge de mensajes no leídos
     */
    private fun updateUnreadBadge(count: Int) {
        if (count > 0) {
            binding.unreadTextBadge.visibility = View.VISIBLE
            binding.unreadTextBadge.text = "$count sin leer"
        } else {
            binding.unreadTextBadge.visibility = View.GONE
        }
    }
    
    private fun loadMessagesSummary() {
        // TODO: Implementar resumen de mensajes con nuevo sistema
        
        // El badge de mensajes se actualiza en observeChatViewModel() con unreadCount
    }
    
    private fun displayRecentTextMessages() {
        // TODO: Implementar con nuevo sistema de chat
        // Por ahora, simplemente mostrar mensaje vacío
        val textMessagesContainer = binding.root.findViewById<android.widget.LinearLayout>(R.id.textMessagesContainer)
        textMessagesContainer?.let { container ->
            container.removeAllViews()
            
            val emptyView = TextView(requireContext()).apply {
                text = "Use el chat completo para ver mensajes"
                textSize = 14f
                setTextColor(resources.getColor(R.color.text_secondary, null))
                setPadding(0, 16, 0, 16)
            }
            container.addView(emptyView)
        }
    }
    
    private fun displayRecentVoiceMessages() {
        // TODO: Implementar con nuevo sistema de mensajes de voz
        val voiceMessagesContainer = binding.root.findViewById<android.widget.LinearLayout>(R.id.voiceMessagesContainer)
        voiceMessagesContainer?.let { container ->
            container.removeAllViews()
        
            val emptyView = TextView(requireContext()).apply {
                text = "No hay mensajes de voz"
                textSize = 14f
                setTextColor(resources.getColor(R.color.text_secondary, null))
                setPadding(0, 16, 0, 16)
            }
            container.addView(emptyView)
        }
    }
    
    private fun setupClickListeners() {
        // Los botones solo existen en layout portrait
        binding.root.findViewById<View>(R.id.viewAllTextButton)?.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_chat)
        }
        
        binding.root.findViewById<View>(R.id.viewAllVoiceButton)?.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_voice)
        }
    }
    
    private fun getRelativeTime(date: Date): String {
        val now = Date()
        val diff = now.time - date.time
        
        return when {
            diff < TimeUnit.MINUTES.toMillis(1) -> getString(R.string.time_just_now)
            diff < TimeUnit.HOURS.toMillis(1) -> {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
                getString(R.string.time_minutes_ago, minutes)
            }
            diff < TimeUnit.DAYS.toMillis(1) -> {
                val hours = TimeUnit.MILLISECONDS.toHours(diff)
                getString(R.string.time_hours_ago, hours)
            }
            else -> {
                val days = TimeUnit.MILLISECONDS.toDays(diff)
                getString(R.string.time_days_ago, days)
            }
        }
    }
    
    private fun formatDuration(seconds: Int): String {
        val minutes = seconds / 60
        val secs = seconds % 60
        return getString(R.string.voice_duration, minutes, secs)
    }

    override fun onResume() {
        super.onResume()
        android.util.Log.d("HomeFragment", "🟢 Fragment resumed - Starting auto-sync")
        
        // Sincronizar inmediatamente al abrir
        chatViewModel.syncMessagesNow()
        
        // Iniciar polling automático cada 30 segundos
        chatSyncHandler.post(chatSyncRunnable)
    }
    
    override fun onPause() {
        super.onPause()
        android.util.Log.d("HomeFragment", "🔴 Fragment paused - Stopping auto-sync")
        
        // Detener polling cuando el fragment no está visible
        chatSyncHandler.removeCallbacks(chatSyncRunnable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(timerRunnable) // Detener el timer
        chatSyncHandler.removeCallbacks(chatSyncRunnable) // Detener sync de chat
        _binding = null
    }
}