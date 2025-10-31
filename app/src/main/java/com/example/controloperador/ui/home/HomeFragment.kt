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
import com.example.controloperador.data.MessageRepository
import com.example.controloperador.data.model.TextMessage
import com.example.controloperador.data.model.VoiceMessage
import com.example.controloperador.databinding.FragmentHomeBinding
import com.example.controloperador.ui.login.SessionManager
import com.example.controloperador.ui.chat.ChatAdapter
import com.example.controloperador.ui.chat.ChatViewModel
import com.example.controloperador.ui.chat.MessagesState
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
    private val messageRepository = MessageRepository.getInstance() // Singleton compartido
    
    // ViewModel compartido con ChatFragment para sincronizar conversaciones
    private val chatViewModel: ChatViewModel by activityViewModels()
    
    // Adaptador para el chat integrado (solo en landscape)
    private var chatAdapter: ChatAdapter? = null
    
    // Lista de mensajes predeterminados cargados desde backend
    private var predefinedMessages: List<com.example.controloperador.data.api.model.TextMessage> = emptyList()
    
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
        startRealtimeTimer() // Nuevo: iniciar timer en tiempo real
        setupIntegratedChat() // Nuevo: configurar chat integrado en landscape
        observeChatViewModel() // Observar mensajes compartidos con ChatFragment
        loadPredefinedMessages() // Cargar mensajes predeterminados del backend
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
        // Observar estado de carga
        chatViewModel.messagesState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is MessagesState.Loading -> {
                    // Mostrar indicador de carga si es necesario
                }
                is MessagesState.Success -> {
                    // Mensajes cargados exitosamente
                }
                is MessagesState.Error -> {
                    // Mostrar mensaje de error (los mensajes locales ya están como fallback)
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
                is MessagesState.Idle -> {
                    // Estado inicial
                }
            }
        }
        
        // Observar mensajes de texto predeterminados
        chatViewModel.textMessages.observe(viewLifecycleOwner) { messages ->
            predefinedMessages = messages
        }
        
        // Observar nombre del corredor (opcional)
        chatViewModel.corridorName.observe(viewLifecycleOwner) { corridorName ->
            // Podrías mostrar el nombre del corredor en algún lugar de la UI
        }
    }
    
    /**
     * Carga los mensajes predeterminados desde el backend
     */
    private fun loadPredefinedMessages() {
        val operatorCode = sessionManager.getOperatorCode()
        
        if (operatorCode == "54321") {
            // Usuario de prueba offline: usar mensajes locales
            chatViewModel.loadPredefinedMessages(operatorCode, useLocal = true)
        } else {
            // Usuario normal: intentar cargar desde backend
            chatViewModel.loadPredefinedMessages(operatorCode ?: "")
        }
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
            val summary = messageRepository.getMessagesSummary()
            val messages = summary.recentTextMessages
            chatAdapter = ChatAdapter(messages)
            
            // Configurar LinearLayoutManager para chat convencional
            // - stackFromEnd = false: items se apilan desde arriba (orden normal)
            // - reverseLayout = false: no invertir el orden de los items
            val layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = false  // Los mensajes llenan desde arriba
                reverseLayout = false // Orden normal: antiguos arriba, nuevos abajo
            }
            
            messagesRecyclerView.apply {
                this.layoutManager = layoutManager
                adapter = chatAdapter
            }
            
            // Scroll al último mensaje (más reciente)
            if (messages.isNotEmpty()) {
                messagesRecyclerView.scrollToPosition(messages.size - 1)
            }
            
            // Configurar botón de respuestas predeterminadas
            responseButton.setOnClickListener {
                showPredefinedResponsesDialog()
            }
        }
    }
    
    /**
     * Muestra el Bottom Sheet con respuestas predeterminadas (Material Design 3)
     * Las opciones se cargan dinámicamente desde el backend
     */
    private fun showPredefinedResponsesDialog() {
        // Verificar que hay mensajes disponibles
        if (predefinedMessages.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "Cargando mensajes predeterminados...",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        
        // Crear Bottom Sheet Dialog (Material Design 3)
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_predefined_responses, null)
        bottomSheetDialog.setContentView(sheetView)
        
        // Obtener el contenedor de botones en el bottom sheet
        val container = sheetView.findViewById<LinearLayout>(R.id.responsesContainer)
        container?.removeAllViews() // Limpiar botones anteriores
        
        // Crear botones dinámicamente según los mensajes del backend
        predefinedMessages.forEach { message ->
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
                text = message.nombre // Título del mensaje
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
                    sendPredefinedResponse(message.mensaje)
                    bottomSheetDialog.dismiss()
                }
            }
            
            container.addView(button)
        }
        
        // Botón de cancelar
        sheetView.findViewById<View>(R.id.cancelButton)?.setOnClickListener {
            bottomSheetDialog.dismiss()
        }
        
        bottomSheetDialog.show()
    }
    
    /**
     * Envía una respuesta predeterminada
     * Agrega el mensaje a la conversación compartida entre HomeFragment y ChatFragment
     */
    private fun sendPredefinedResponse(response: String) {
        // Agregar mensaje al repositorio compartido (será visible en ambos fragmentos)
        val newMessage = messageRepository.sendTextMessage(response)
        
        // Actualizar el chat integrado en landscape si existe
        chatAdapter?.let { adapter ->
            val allMessages = messageRepository.getAllTextMessages()
            adapter.updateMessages(allMessages)
            
            // Scroll al último mensaje
            binding.root.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.messagesRecyclerView)?.let {
                it.scrollToPosition(allMessages.size - 1)
            }
        }
        
        // TODO: Enviar al backend
        // messagesRepository.sendMessage(operatorCode, response)
        
        // Mostrar feedback
        Toast.makeText(
            requireContext(),
            getString(R.string.messages_sent),
            Toast.LENGTH_SHORT
        ).show()
    }
    
    private fun loadMessagesSummary() {
        val summary = messageRepository.getMessagesSummary()
        
        // Actualizar badge de mensajes de texto
        if (summary.unreadTextMessages > 0) {
            binding.unreadTextBadge.visibility = View.VISIBLE
            binding.unreadTextBadge.text = "${summary.unreadTextMessages} sin leer"
        } else {
            binding.unreadTextBadge.visibility = View.GONE
        }
        
        // Actualizar badge de mensajes de voz
        if (summary.unplayedVoiceMessages > 0) {
            binding.unplayedVoiceBadge.visibility = View.VISIBLE
            binding.unplayedVoiceBadge.text = "${summary.unplayedVoiceMessages} sin reproducir"
        } else {
            binding.unplayedVoiceBadge.visibility = View.GONE
        }
        
        // Mostrar últimos mensajes de texto
        displayRecentTextMessages(summary.recentTextMessages)
        
        // Mostrar últimos mensajes de voz
        displayRecentVoiceMessages(summary.recentVoiceMessages)
    }
    
    private fun displayRecentTextMessages(messages: List<TextMessage>) {
        // Solo en portrait - en landscape se usa el RecyclerView del chat
        val textMessagesContainer = binding.root.findViewById<android.widget.LinearLayout>(R.id.textMessagesContainer)
        textMessagesContainer?.let { container ->
            container.removeAllViews()
            
            if (messages.isEmpty()) {
                val emptyView = TextView(requireContext()).apply {
                    text = getString(R.string.messages_no_messages)
                    textSize = 14f
                    setTextColor(resources.getColor(R.color.text_secondary, null))
                    setPadding(0, 16, 0, 16)
                }
                container.addView(emptyView)
                return
            }
            
            messages.forEach { message ->
                val itemView = layoutInflater.inflate(
                    R.layout.item_text_message_summary,
                    container,
                    false
                )
                
                itemView.findViewById<TextView>(R.id.messageSender).text = message.senderName
                itemView.findViewById<TextView>(R.id.messageContent).text = message.content
                itemView.findViewById<TextView>(R.id.messageTime).text = getRelativeTime(message.timestamp)
                
                itemView.setOnClickListener {
                    findNavController().navigate(R.id.action_home_to_chat)
                }
                
                container.addView(itemView)
            }
        }
    }
    
    private fun displayRecentVoiceMessages(messages: List<VoiceMessage>) {
        // Busca el contenedor - solo existe en layout portrait
        val voiceMessagesContainer = binding.root.findViewById<android.widget.LinearLayout>(R.id.voiceMessagesContainer)
        voiceMessagesContainer?.let { container ->
            container.removeAllViews()
        
            if (messages.isEmpty()) {
                val emptyView = TextView(requireContext()).apply {
                    text = getString(R.string.messages_no_voice)
                    textSize = 14f
                    setTextColor(resources.getColor(R.color.text_secondary, null))
                    setPadding(0, 16, 0, 16)
                }
                container.addView(emptyView)
                return@let
            }
        
            messages.forEach { message ->
                val itemView = layoutInflater.inflate(
                    R.layout.item_voice_message_summary,
                    container,
                    false
                )
            
                itemView.findViewById<TextView>(R.id.voiceSender).text = message.senderName
                itemView.findViewById<TextView>(R.id.voiceDuration).text = formatDuration(message.duration)
                itemView.findViewById<TextView>(R.id.voiceTime).text = getRelativeTime(message.timestamp)
            
                itemView.setOnClickListener {
                    findNavController().navigate(R.id.action_home_to_voice)
                }
            
                container.addView(itemView)
            }
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

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(timerRunnable) // Detener el timer
        _binding = null
    }
}