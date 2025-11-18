package com.example.alquilervehiculos.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load // Importación para cargar imágenes (si es necesaria en el futuro)
import com.example.alquilervehiculos.R // Asumo que necesitas R para placeholders
import com.example.alquilervehiculos.databinding.ItemReservaBinding
import com.example.alquilervehiculos.modelo.Reserva // 🔑 Necesaria para pasar el objeto a la Activity
import com.example.alquilervehiculos.modelo.ReservaViewModel
import java.util.Locale

// ====================================================================
// 1. INTERFAZ PARA MANEJAR ACCIONES DE ACEPTAR/RECHAZAR (LISTENER)
// ====================================================================
/**
 * Interfaz que la Activity (MisVehiculosReservadosActivity) debe implementar
 * para poder gestionar las acciones de la lista.
 */
interface ReservaActionListener {
    fun onAccept(reserva: Reserva)
    fun onReject(reserva: Reserva)
    fun onCancel(reserva: Reserva)
}

class ReservaAdapter(
    private var items: List<ReservaViewModel>,
    private val esVistaArrendador: Boolean, // Booleano para diferenciar la vista
    // 2. RECIBIR EL LISTENER EN EL CONSTRUCTOR
    private val actionListener: ReservaActionListener? = null
) : RecyclerView.Adapter<ReservaAdapter.ReservaViewHolder>() {

    inner class ReservaViewHolder(val binding: ItemReservaBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(reservaVM: ReservaViewModel) {
            val vehiculo = reservaVM.vehiculo
            val cliente = reservaVM.cliente
            val reserva = reservaVM.reserva

            // --- INFO DEL VEHÍCULO ---
            if (vehiculo != null) {
                // Muestra marca, modelo y placa
                binding.textViewReservaPlaca.text =
                    "${vehiculo.marca} ${vehiculo.modelo} (${vehiculo.placa})"
                // Opcional: Cargar imagen
                // binding.imageViewCar.load(vehiculo.imageUrl) {
                //     placeholder(R.drawable.ic_car_placeholder)
                // }
            } else {
                binding.textViewReservaPlaca.text = "Vehículo no disponible"
            }

            // --- INFO DE LA RESERVA (Común) ---
            // Muestra el estado (PENDIENTE, ACEPTADA, RECHAZADA)
            binding.textViewReservaEstado.text =
                "Estado: ${reserva.estado.uppercase(Locale.getDefault())}"
            binding.textViewReservaFechas.text = "Del ${reserva.fechaInicio} al ${reserva.fechaFin}"
            binding.textViewReservaPrecio.text =
                String.format(Locale.US, "Total: $%.2f", reserva.precioTotal)


            // ========================================================================
            // 🔑 LÓGICA CRÍTICA PARA EL ARRENDADOR (MIS VEHÍCULOS RESERVADOS)
            // ========================================================================
            if (esVistaArrendador) {
                // 1. Mostrar el nombre del cliente
                binding.textViewClienteNombre.visibility = View.VISIBLE
                binding.textViewClienteNombre.text =
                    "Cliente: ${cliente?.obtenerNombreMostrable() ?: "No encontrado"}"

                // 2. Mostrar botones SÓLO si el estado es PENDIENTE
                if (reserva.estado.uppercase(Locale.getDefault()) == "PENDIENTE") {
                    binding.layoutActions.visibility =
                        View.VISIBLE // Asumo un contenedor de botones

                    // 3. ATTACH LISTENERS A LOS BOTONES
                    binding.buttonAccept.setOnClickListener {
                        actionListener?.onAccept(reserva)
                    }
                    binding.buttonReject.setOnClickListener {
                        actionListener?.onReject(reserva)
                    }
                } else {
                    // Ocultar acciones si ya está resuelta
                    binding.layoutActions.visibility = View.GONE
                }

            } else {
                // Vista de CLIENTE (Mis Reservas)
                // Si está PENDIENTE, el cliente puede CANCELAR
                if (reserva.estado.uppercase() == "PENDIENTE") {
                    binding.layoutActions.visibility = View.VISIBLE

                    // Ocultamos el botón de aceptar (el cliente no se acepta a sí mismo)
                    binding.buttonAccept.visibility = View.GONE

                    // Reutilizamos el botón de rechazar para Cancelar
                    binding.buttonReject.visibility = View.VISIBLE
                    binding.buttonReject.text = "Cancelar Reserva"

                    binding.buttonReject.setOnClickListener { actionListener?.onCancel(reserva) }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReservaViewHolder {
        val binding = ItemReservaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReservaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReservaViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    /**
     * Función para actualizar la lista de datos desde la Activity.
     */
    fun actualizarLista(nuevaLista: List<ReservaViewModel>) {
        items = nuevaLista
        notifyDataSetChanged()
    }
}