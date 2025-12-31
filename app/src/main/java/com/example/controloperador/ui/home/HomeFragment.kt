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
import com.example.controloperador.ui.voice.VoiceMessagesViewModel
import com.example.controloperador.data.api.model.VoiceMessageDetail
import com.example.controloperador.utils.AudioPlayerHelper
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
    
    // ViewModel para mensajes de voz
    private lateinit var voiceViewModel: VoiceMessagesViewModel
    
    // AudioPlayer para reproducir audios en el home
    private var audioPlayer: AudioPlayerHelper? = null
    private var currentPlayingAudioId: String? = null
    
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
    
    // Handler para sincronizar mensajes de voz cada 30 segundos
    private val voiceSyncHandler = Handler(Looper.getMainLooper())
    private val voiceSyncRunnable = object : Runnable {
        override fun run() {
            android.util.Log.d("HomeFragment", "🎙️ Auto-sync voice messages triggered (30s interval)")
            loadVoiceMessages() // Recargar mensajes de voz
            voiceSyncHandler.postDelayed(this, 30_000) // Repetir cada 30 segundos
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        voiceViewModel = ViewModelProvider(this)[VoiceMessagesViewModel::class.java]

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root
        
        sessionManager = SessionManager(requireContext())
        
        // Inicializar AudioPlayer
        audioPlayer = AudioPlayerHelper(requireContext())
        setupAudioPlayer()
        
        setupWelcomeMessage()
        startRealtimeTimer() // Iniciar timer en tiempo real
        setupIntegratedChat() // Configurar chat integrado en landscape
        observeChatViewModel() // Observar mensajes compartidos con ChatFragment
        observeVoiceViewModel() // Observar mensajes de voz
        loadMessagesSummary()
        loadVoiceMessages() // Cargar audios recientes
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
    
    /**
     * Configura los listeners del AudioPlayer
     */
    private fun setupAudioPlayer() {
        audioPlayer?.apply {
            setOnCompletionListener { audioId ->
                android.util.Log.d("HomeFragment", "Audio completed: $audioId")
                currentPlayingAudioId = null
                // Actualizar UI del botón que terminó de reproducir
                updatePlayButtonsState()
            }
            
            setOnErrorListener { audioId, error ->
                android.util.Log.e("HomeFragment", "Audio error: $audioId - $error")
                Toast.makeText(requireContext(), "Error al reproducir audio", Toast.LENGTH_SHORT).show()
                currentPlayingAudioId = null
                updatePlayButtonsState()
            }
            
            setOnPreparedListener { audioId, duration ->
                android.util.Log.d("HomeFragment", "Audio prepared: $audioId (${duration}s)")
            }
        }
    }
    
    /**
     * Observa cambios en VoiceMessagesViewModel
     */
    private fun observeVoiceViewModel() {
        // Observar mensajes de voz
        voiceViewModel.messages.observe(viewLifecycleOwner) { messages ->
            displayRecentVoiceMessages(messages)
        }
        
        // Observar contador de mensajes sin reproducir
        voiceViewModel.unreadCount.observe(viewLifecycleOwner) { count ->
            updateVoiceUnreadBadge(count)
        }
        
        // Observar errores
        voiceViewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                android.util.Log.e("HomeFragment", "Voice error: $it")
            }
        }
    }
    
    /**
     * Carga los mensajes de voz del operador
     */
    private fun loadVoiceMessages() {
        val operatorCode = sessionManager.getOperatorCode() ?: return
        voiceViewModel.loadConversations(operatorCode)
    }
    
    /**
     * Muestra los últimos 5 mensajes de voz recibidos
     */
    private fun displayRecentVoiceMessages(allMessages: List<VoiceMessageDetail> = emptyList()) {
        val voiceMessagesContainer = binding.root.findViewById<LinearLayout>(R.id.voiceMessagesContainer)
        voiceMessagesContainer?.let { container ->
            container.removeAllViews()
            
            if (allMessages.isEmpty()) {
                val emptyView = TextView(requireContext()).apply {
                    text = "No hay mensajes de voz"
                    textSize = 14f
                    setTextColor(resources.getColor(R.color.text_secondary, null))
                    setPadding(0, 16, 0, 16)
                }
                container.addView(emptyView)
                return
            }
            
            // Tomar los últimos 5 audios (más recientes primero)
            val recentMessages = allMessages
                .sortedByDescending { it.createdAt } // Más recientes primero
                .take(5)
            
            recentMessages.forEach { message ->
                val itemView = layoutInflater.inflate(R.layout.item_voice_message_home, container, false)
                
                // Configurar datos del audio
                itemView.findViewById<TextView>(R.id.audioDate)?.text = getRelativeTimeFromString(message.createdAt)
                itemView.findViewById<TextView>(R.id.audioDuration)?.text = message.formattedDuration
                
                // Mostrar indicador de no leído
                val unreadIndicator = itemView.findViewById<View>(R.id.unreadIndicator)
                unreadIndicator?.visibility = if (!message.isRead) View.VISIBLE else View.GONE
                
                // Configurar botón de reproducción
                val playButton = itemView.findViewById<MaterialButton>(R.id.playButton)
                playButton?.tag = message.id.toString() // Guardar ID en el tag
                playButton?.setOnClickListener {
                    handleAudioPlayPause(message, playButton)
                }
                
                // Actualizar icono inicial
                updatePlayButtonIcon(playButton, message.id.toString())
                
                container.addView(itemView)
            }
        }
    }
    
    /**
     * Maneja play/pause de un audio
     */
    private fun handleAudioPlayPause(message: VoiceMessageDetail, button: MaterialButton) {
        val player = audioPlayer ?: return
        val messageId = message.id.toString()
        
        if (currentPlayingAudioId == messageId && player.isPlaying()) {
            // Pausar audio actual
            player.pause()
            updatePlayButtonIcon(button, messageId)
        } else if (currentPlayingAudioId == messageId && !player.isPlaying()) {
            // Reanudar audio pausado
            player.resume()
            updatePlayButtonIcon(button, messageId)
        } else {
            // Reproducir nuevo audio
            player.stop() // Detener cualquier audio anterior
            currentPlayingAudioId = messageId
            updatePlayButtonsState() // Actualizar todos los botones
            
            val started = player.playAudioFromPath(message.audioUrl, messageId)
            if (!started) {
                Toast.makeText(
                    requireContext(),
                    "No se pudo reproducir el audio",
                    Toast.LENGTH_SHORT
                ).show()
                currentPlayingAudioId = null
                updatePlayButtonIcon(button, messageId)
            }
        }
    }
    
    /**
     * Actualiza el icono de un botón de reproducción
     */
    private fun updatePlayButtonIcon(button: MaterialButton, audioId: String) {
        val isPlaying = currentPlayingAudioId == audioId && audioPlayer?.isPlaying() == true
        button.setIconResource(
            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        )
    }
    
    /**
     * Actualiza el estado de todos los botones de reproducción
     */
    private fun updatePlayButtonsState() {
        val container = binding.root.findViewById<LinearLayout>(R.id.voiceMessagesContainer)
        container?.let {
            for (i in 0 until it.childCount) {
                val itemView = it.getChildAt(i)
                val button = itemView.findViewById<MaterialButton>(R.id.playButton)
                val audioId = button?.tag as? String
                if (audioId != null) {
                    updatePlayButtonIcon(button, audioId)
                }
            }
        }
    }
    
    /**
     * Actualiza el badge de mensajes de voz sin reproducir
     */
    private fun updateVoiceUnreadBadge(count: Int) {
        val badge = binding.root.findViewById<TextView>(R.id.unplayedVoiceBadge)
        badge?.let {
            if (count > 0) {
                it.visibility = View.VISIBLE
                it.text = "$count sin reproducir"
            } else {
                it.visibility = View.GONE
            }
        }
    }
    
    /**
     * Convierte fecha ISO string a tiempo relativo
     */
    private fun getRelativeTimeFromString(dateString: String): String {
        return try {
            // Parsear fecha ISO 8601 del backend
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val date = format.parse(dateString.split(".")[0]) // Remover milisegundos
            if (date != null) {
                getRelativeTime(date)
            } else {
                "Reciente"
            }
        } catch (e: Exception) {
            android.util.Log.e("HomeFragment", "Error parsing date: $dateString", e)
            "Reciente"
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
        
        // Sincronizar chat inmediatamente al abrir
        chatViewModel.syncMessagesNow()
        
        // Sincronizar mensajes de voz inmediatamente al abrir
        loadVoiceMessages()
        
        // Iniciar polling automático para chat cada 30 segundos
        chatSyncHandler.post(chatSyncRunnable)
        
        // Iniciar polling automático para mensajes de voz cada 30 segundos
        voiceSyncHandler.post(voiceSyncRunnable)
    }
    
    override fun onPause() {
        super.onPause()
        android.util.Log.d("HomeFragment", "🔴 Fragment paused - Stopping auto-sync")
        
        // Detener polling de chat cuando el fragment no está visible
        chatSyncHandler.removeCallbacks(chatSyncRunnable)
        
        // Detener polling de mensajes de voz cuando el fragment no está visible
        voiceSyncHandler.removeCallbacks(voiceSyncRunnable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(timerRunnable) // Detener el timer
        chatSyncHandler.removeCallbacks(chatSyncRunnable) // Detener sync de chat
        voiceSyncHandler.removeCallbacks(voiceSyncRunnable) // Detener sync de mensajes de voz
        audioPlayer?.release() // Liberar MediaPlayer
        audioPlayer = null
        _binding = null
    }
}