package com.example.alquilervehiculos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.alquilervehiculos.R
import com.example.alquilervehiculos.model.VehiculoEntity
import com.google.android.material.switchmaterial.SwitchMaterial

// 🔑 CORRECCIÓN: El adaptador ahora usa VehiculoEntity y está limpio.
class VehiculoAdapter(
    private var vehiculos: List<VehiculoEntity>,
    private val actionType: String
) : RecyclerView.Adapter<VehiculoAdapter.VehiculoViewHolder>() {

    // Listeners para las diferentes acciones
    var onItemClickListener: ((VehiculoEntity) -> Unit)? = null
    var onEditClickListener: ((VehiculoEntity) -> Unit)? = null
    var onDeleteClickListener: ((VehiculoEntity) -> Unit)? = null
    var onStatusChangeListener: ((VehiculoEntity, Boolean) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VehiculoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_vehiculo_opciones, parent, false)
        return VehiculoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VehiculoViewHolder, position: Int) {
        val vehiculo = vehiculos[position]
        holder.bind(vehiculo)
    }

    override fun getItemCount() = vehiculos.size

    inner class VehiculoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageVehiculo: ImageView = itemView.findViewById(R.id.image_vehiculo_item) // Asumiendo que existe un ImageView
        private val textMarcaModelo: TextView = itemView.findViewById(R.id.text_marca_modelo)
        private val textPlaca: TextView = itemView.findViewById(R.id.text_placa)
        private val btnEditar: Button = itemView.findViewById(R.id.btn_editar_item)
        private val iconDelete: ImageView = itemView.findViewById(R.id.icon_delete_item)
        private val switchEstado: SwitchMaterial = itemView.findViewById(R.id.switch_estado_item)

        fun bind(vehiculo: VehiculoEntity) {
            textMarcaModelo.text = "${vehiculo.marca} ${vehiculo.modelo}"
            textPlaca.text = vehiculo.placa

            // Cargar imagen con Coil, con placeholder y error
            imageVehiculo.load(vehiculo.imageUrl) {
                placeholder(R.drawable.ic_car_placeholder) // Un placeholder genérico
                error(R.drawable.ic_car_placeholder) // Imagen en caso de error
            }

            // Configurar visibilidad según el tipo de acción
            btnEditar.visibility = if (actionType == "EDITAR") View.VISIBLE else View.GONE
            iconDelete.visibility = if (actionType == "ELIMINAR") View.VISIBLE else View.GONE
            switchEstado.visibility = if (actionType == "ACTUALIZAR_ESTADO") View.VISIBLE else View.GONE

            if (actionType == "ACTUALIZAR_ESTADO") {
                switchEstado.isChecked = vehiculo.estado == "Activo"
                switchEstado.text = if (switchEstado.isChecked) "Activo" else "Inactivo"
            }

            // Asignar listeners
            itemView.setOnClickListener {
                if (actionType == "VER") { // Solo para la acción por defecto
                    onItemClickListener?.invoke(vehiculo)
                }
            }
            btnEditar.setOnClickListener { onEditClickListener?.invoke(vehiculo) }
            iconDelete.setOnClickListener { onDeleteClickListener?.invoke(vehiculo) }
            switchEstado.setOnCheckedChangeListener { _, isChecked ->
                onStatusChangeListener?.invoke(vehiculo, isChecked)
            }
        }
    }

    fun updateData(newVehiculos: List<VehiculoEntity>) {
        this.vehiculos = newVehiculos
        notifyDataSetChanged()
    }
}
