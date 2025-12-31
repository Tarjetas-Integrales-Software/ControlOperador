package com.example.controloperador.ui.slideshow

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.controloperador.R
import com.example.controloperador.data.database.AttendanceLog
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter para mostrar reportes de asistencia en un RecyclerView
 */
class ReportesAdapter : ListAdapter<AttendanceLog, ReportesAdapter.ReporteViewHolder>(ReporteDiffCallback()) {

    companion object {
        private val dateFormatter = SimpleDateFormat("dd/MM/yyyy\nHH:mm", Locale.getDefault())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReporteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reporte, parent, false)
        return ReporteViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReporteViewHolder, position: Int) {
        val reporte = getItem(position)
        holder.bind(reporte)
    }

    class ReporteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNombreCompleto: TextView = itemView.findViewById(R.id.tvNombreCompleto)
        private val tvFechaEntrada: TextView = itemView.findViewById(R.id.tvFechaEntrada)
        private val tvFechaSalida: TextView = itemView.findViewById(R.id.tvFechaSalida)
        private val tvTiempoOperando: TextView = itemView.findViewById(R.id.tvTiempoOperando)
        private val ivEstadoSync: ImageView = itemView.findViewById(R.id.ivEstadoSync)

        fun bind(reporte: AttendanceLog) {
            // Nombre completo
            tvNombreCompleto.text = reporte.getFullName()

            // Fecha de entrada
            tvFechaEntrada.text = dateFormatter.format(reporte.entrada)

            // Fecha de salida (o "En curso" si aún no ha salido)
            if (reporte.salida != null) {
                tvFechaSalida.text = dateFormatter.format(reporte.salida!!)
                // Convertir horas decimales a formato HH:MM
                tvTiempoOperando.text = formatHoursToHHMM(reporte.tiempoOperando)
            } else {
                tvFechaSalida.text = "En curso..."
                tvTiempoOperando.text = "-"
            }

            // Estado de sincronización
            if (reporte.isEnviado()) {
                // Sincronizado - check verde
                ivEstadoSync.setImageResource(android.R.drawable.ic_menu_upload_you_tube)
                ivEstadoSync.setColorFilter(itemView.context.getColor(R.color.accent_blue))
            } else {
                // Pendiente de sincronizar - upload naranja
                ivEstadoSync.setImageResource(android.R.drawable.ic_menu_upload)
                ivEstadoSync.setColorFilter(itemView.context.getColor(R.color.accent_gold))
            }
        }
        
        /**
         * Convierte horas decimales a formato HH:MM
         * Ejemplos:
         * - 2.98h → 02:59
         * - 25.73h → 25:44
         * - 66.11h → 66:07
         */
        private fun formatHoursToHHMM(horasDecimales: Double): String {
            val horas = horasDecimales.toInt()
            val minutos = ((horasDecimales - horas) * 60).toInt()
            return String.format("%02d:%02d", horas, minutos)
        }
    }

    class ReporteDiffCallback : DiffUtil.ItemCallback<AttendanceLog>() {
        override fun areItemsTheSame(oldItem: AttendanceLog, newItem: AttendanceLog): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: AttendanceLog, newItem: AttendanceLog): Boolean {
            return oldItem == newItem
        }
    }
}
