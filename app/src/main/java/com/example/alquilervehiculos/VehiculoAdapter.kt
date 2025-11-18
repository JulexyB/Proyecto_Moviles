package com.example.alquilervehiculos.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load // Usado para la carga eficiente de imágenes
import com.example.alquilervehiculos.R
import com.example.alquilervehiculos.VehiculoDetailActivity
import com.example.alquilervehiculos.databinding.ItemVehiculoBinding // ViewBinding
import com.example.alquilervehiculos.model.VehiculoEntity
import java.util.Locale

// =========================================================================
// 1. DEFINICIÓN DE LA INTERFAZ PARA MANEJAR ACCIONES DE GESTIÓN
// =========================================================================
interface VehiculoActionListener {

    fun onEdit(vehiculo: VehiculoEntity)
    fun onDelete(vehiculo: VehiculoEntity)
}

class VehiculoAdapter(
    private var vehiculos: List<VehiculoEntity>,
    // 2. FLAG para saber si es la vista del dueño (modo gestión)
    private val isOwnerView: Boolean = false,
    // 3. Listener para las acciones de Editar/Eliminar
    private val actionListener: VehiculoActionListener? = null
) : RecyclerView.Adapter<VehiculoAdapter.VehiculoViewHolder>() {

    // El ViewHolder ahora usa el objeto de ViewBinding
    inner class VehiculoViewHolder(val binding: ItemVehiculoBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VehiculoViewHolder {
        // Inflamos el layout usando el ViewBinding generado
        val binding = ItemVehiculoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VehiculoViewHolder(binding)
    }

    override fun getItemCount(): Int = vehiculos.size

    override fun onBindViewHolder(holder: VehiculoViewHolder, position: Int) {
        val vehiculo = vehiculos[position]
        val binding = holder.binding

        // 4. Enlace de Datos
        binding.textViewMarcaModelo.text = "${vehiculo.marca} ${vehiculo.modelo} (${vehiculo.anio})"
        binding.textViewTipo.text = "Tipo: ${vehiculo.tipo} - Color: ${vehiculo.color}"
        binding.textViewPrecio.text = String.format(Locale.US, "$%.2f / día", vehiculo.precioDia)
        binding.textViewVehiculoCiudad.text = vehiculo.ciudad // Mostrar ciudad

        // Carga de Imagen con Coil
        binding.imageViewVehicle.load(vehiculo.imageUrl) {
            crossfade(true)
            placeholder(R.drawable.ic_car_placeholder)
            error(R.drawable.ic_car_placeholder)
        }

        // 5. Lógica de Gestión (Editar/Eliminar)
        if (isOwnerView) {
            // Mostrar el contenedor de botones (asumo el ID layoutOwnerActions existe en item_vehiculo.xml)
            binding.layoutOwnerActions.visibility = View.VISIBLE

            // Adjuntar los listeners a los botones
            binding.buttonEditVehicle.setOnClickListener {
                actionListener?.onEdit(vehiculo)
            }
            binding.buttonDeleteVehicle.setOnClickListener {
                actionListener?.onDelete(vehiculo)
            }

            // En modo Owner, el click principal no va a Detail, solo los botones funcionan
            holder.itemView.setOnClickListener(null)

        } else {
            // Modo Arrendatario (Cliente): Ocultar botones de gestión
            binding.layoutOwnerActions.visibility = View.GONE

            // Lógica de click normal (Ir a VehiculoDetailActivity)
            holder.itemView.setOnClickListener {
                val context = holder.itemView.context
                val intent = Intent(context, VehiculoDetailActivity::class.java).apply {
                    // Pasar datos del vehículo a la pantalla de detalle
                    putExtra("MARCA", vehiculo.marca)
                    putExtra("MODELO", vehiculo.modelo)
                    putExtra("ANIO", vehiculo.anio)
                    putExtra("PLACA", vehiculo.placa)
                    putExtra("IMAGE_URL", vehiculo.imageUrl)
                    putExtra("DISPONIBLE", vehiculo.disponible)
                    putExtra("OWNER_ID", vehiculo.ownerId)
                    putExtra("PRECIO", vehiculo.precioDia)
                    // ... (demás campos como COLOR, CIUDAD, TELEFONO) ...
                }
                context.startActivity(intent)
            }
        }
    }

    /**
     * Función para actualizar la lista de vehículos desde la Activity.
     */
    fun updateData(newVehiculos: List<VehiculoEntity>) {
        vehiculos = newVehiculos
        notifyDataSetChanged()
    }
}