package com.example.controloperador.ui.slideshow

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.controloperador.R
import com.example.controloperador.databinding.FragmentSlideshowBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.*

class SlideshowFragment : Fragment() {

    private var _binding: FragmentSlideshowBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: SlideshowViewModel
    private lateinit var reportesAdapter: ReportesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this)[SlideshowViewModel::class.java]
        _binding = FragmentSlideshowBinding.inflate(inflater, container, false)
        
        setupRecyclerView()
        setupCharts()
        observeData()
        
        return binding.root
    }
    
    override fun onResume() {
        super.onResume()
        // Recargar estadísticas cuando el fragment se hace visible
        viewModel.loadWeeklyStats()
    }

    /**
     * Configura el RecyclerView de reportes
     */
    private fun setupRecyclerView() {
        reportesAdapter = ReportesAdapter()
        binding.rvReportes.apply {
            adapter = reportesAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    /**
     * Configura las gráficas (barras y dona)
     */
    private fun setupCharts() {
        // Configurar gráfica de barras
        binding.chartBarras.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            setDrawValueAboveBar(true)
            setPinchZoom(false)
            setScaleEnabled(false)
            
            // Eje X
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
            }
            
            // Eje Y izquierdo
            axisLeft.apply {
                setDrawGridLines(true)
                axisMinimum = 0f
            }
            
            // Eje Y derecho (deshabilitado)
            axisRight.isEnabled = false
            
            // Leyenda
            legend.isEnabled = true
        }
        
        // Configurar gráfica de dona
        binding.chartDona.apply {
            description.isEnabled = false
            setDrawHoleEnabled(true)
            setHoleColor(Color.WHITE)
            setTransparentCircleRadius(58f)
            setDrawEntryLabels(true)
            setCenterTextSize(18f)
            
            // Leyenda
            legend.isEnabled = true
        }
    }

    /**
     * Observa los datos del ViewModel
     */
    private fun observeData() {
        // Observar lista de reportes
        viewModel.allReportes.observe(viewLifecycleOwner) { reportes ->
            if (reportes.isEmpty()) {
                binding.layoutEmptyState.visibility = View.VISIBLE
                binding.rvReportes.visibility = View.GONE
            } else {
                binding.layoutEmptyState.visibility = View.GONE
                binding.rvReportes.visibility = View.VISIBLE
                reportesAdapter.submitList(reportes)
            }
        }
        
        // Observar estadísticas semanales
        viewModel.weeklyStats.observe(viewLifecycleOwner) { stats ->
            android.util.Log.d("SlideshowFragment", "🔔 Observer de weeklyStats recibió ${stats.size} estadísticas")
            updateBarChart(stats)
            updatePieChart(stats)
        }
        
        // Observar total de horas
        viewModel.totalWeeklyHours.observe(viewLifecycleOwner) { total ->
            binding.tvTotalHorasSemana.text = String.format("%.2f hrs", total)
        }
        
        // Observar estado de sincronización
        viewModel.syncState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is SlideshowViewModel.SyncState.Idle -> {
                    binding.layoutSyncStatus.visibility = View.GONE
                }
                is SlideshowViewModel.SyncState.Loading -> {
                    binding.layoutSyncStatus.visibility = View.VISIBLE
                    binding.tvSyncStatus.text = "Sincronizando reportes..."
                }
                is SlideshowViewModel.SyncState.Success -> {
                    binding.layoutSyncStatus.visibility = View.GONE
                    Snackbar.make(binding.root, 
                        "✓ ${state.count} reportes sincronizados", 
                        Snackbar.LENGTH_SHORT).show()
                    viewModel.resetSyncState()
                }
                is SlideshowViewModel.SyncState.PartialSuccess -> {
                    binding.layoutSyncStatus.visibility = View.GONE
                    Snackbar.make(binding.root, 
                        "⚠ ${state.successful} exitosos, ${state.failed} fallidos", 
                        Snackbar.LENGTH_LONG).show()
                    viewModel.resetSyncState()
                }
                is SlideshowViewModel.SyncState.Error -> {
                    binding.layoutSyncStatus.visibility = View.GONE
                    Snackbar.make(binding.root, 
                        "✗ Error: ${state.message}", 
                        Snackbar.LENGTH_LONG).show()
                    viewModel.resetSyncState()
                }
                is SlideshowViewModel.SyncState.NoData -> {
                    binding.layoutSyncStatus.visibility = View.GONE
                    Snackbar.make(binding.root, 
                        "No hay reportes pendientes", 
                        Snackbar.LENGTH_SHORT).show()
                    viewModel.resetSyncState()
                }
            }
        }
    }

    /**
     * Actualiza la gráfica de barras con estadísticas diarias
     */
    private fun updateBarChart(stats: List<com.example.controloperador.data.database.DailyStats>) {
        android.util.Log.d("SlideshowFragment", "updateBarChart llamado con ${stats.size} estadísticas")
        
        if (stats.isEmpty()) {
            android.util.Log.w("SlideshowFragment", "No hay estadísticas, limpiando gráfica de barras")
            binding.chartBarras.clear()
            binding.chartBarras.invalidate()
            return
        }
        
        val entries = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()
        
        // Formateadores de fecha
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("EEE dd", Locale.getDefault())
        
        stats.forEachIndexed { index, stat ->
            entries.add(BarEntry(index.toFloat(), stat.totalHours.toFloat()))
            
            // Convertir String "2025-11-07" a formato legible "Jue 07"
            try {
                val date = inputFormat.parse(stat.date)
                labels.add(if (date != null) outputFormat.format(date) else stat.date.substring(5))
                android.util.Log.d("SlideshowFragment", "  Barra $index: ${labels.last()} = ${stat.totalHours}h")
            } catch (e: Exception) {
                // Si falla el parseo, usar los últimos 5 caracteres (MM-dd)
                labels.add(stat.date.substring(5))
                android.util.Log.w("SlideshowFragment", "Error parseando fecha: ${stat.date}", e)
            }
        }
        
        val dataSet = BarDataSet(entries, "Horas Trabajadas").apply {
            color = ContextCompat.getColor(requireContext(), R.color.accent_gold)
            valueTextSize = 10f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return String.format("%.1fh", value)
                }
            }
        }
        
        binding.chartBarras.apply {
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            xAxis.granularity = 1f
            xAxis.labelCount = labels.size
            data = BarData(dataSet)
            animateY(500)
            invalidate() // Refrescar gráfica
        }
        
        android.util.Log.d("SlideshowFragment", "✓ Gráfica de barras actualizada")
    }

    /**
     * Actualiza la gráfica de dona con distribución semanal
     */
    private fun updatePieChart(stats: List<com.example.controloperador.data.database.DailyStats>) {
        android.util.Log.d("SlideshowFragment", "updatePieChart llamado con ${stats.size} estadísticas")
        
        if (stats.isEmpty()) {
            android.util.Log.w("SlideshowFragment", "No hay estadísticas, limpiando gráfica de dona")
            binding.chartDona.clear()
            binding.chartDona.invalidate()
            return
        }
        
        val entries = mutableListOf<PieEntry>()
        
        // Formateadores de fecha
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("EEE", Locale.getDefault())
        
        stats.forEach { stat ->
            if (stat.totalHours > 0) {
                // Convertir String "2025-11-07" a formato legible "Jue"
                try {
                    val date = inputFormat.parse(stat.date)
                    val label = if (date != null) outputFormat.format(date) else stat.date.substring(8)
                    entries.add(PieEntry(stat.totalHours.toFloat(), label))
                    android.util.Log.d("SlideshowFragment", "  Segmento: $label = ${stat.totalHours}h")
                } catch (e: Exception) {
                    // Si falla el parseo, usar los últimos 2 caracteres (día del mes)
                    entries.add(PieEntry(stat.totalHours.toFloat(), stat.date.substring(8)))
                    android.util.Log.w("SlideshowFragment", "Error parseando fecha: ${stat.date}", e)
                }
            }
        }
        
        if (entries.isEmpty()) {
            android.util.Log.w("SlideshowFragment", "Todas las estadísticas tienen 0 horas, limpiando gráfica")
            binding.chartDona.clear()
            binding.chartDona.invalidate()
            return
        }
        
        val dataSet = PieDataSet(entries, "Distribución Semanal").apply {
            colors = listOf(
                ContextCompat.getColor(requireContext(), R.color.primary_dark),
                ContextCompat.getColor(requireContext(), R.color.accent_gold),
                ContextCompat.getColor(requireContext(), R.color.accent_blue),
                Color.parseColor("#E74C3C"), // Rojo
                Color.parseColor("#9B59B6"), // Púrpura
                Color.parseColor("#27AE60"), // Verde
                Color.parseColor("#F39C12")  // Naranja
            )
            valueTextSize = 12f
            valueTextColor = Color.WHITE
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return String.format("%.1fh", value)
                }
            }
        }
        
        val total = stats.sumOf { it.totalHours }
        android.util.Log.d("SlideshowFragment", "Total de horas: $total")
        
        binding.chartDona.apply {
            data = PieData(dataSet)
            centerText = String.format("Total\n%.1fh", total)
            animateY(500)
            invalidate() // Refrescar gráfica
        }
        
        android.util.Log.d("SlideshowFragment", "✓ Gráfica de dona actualizada")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}