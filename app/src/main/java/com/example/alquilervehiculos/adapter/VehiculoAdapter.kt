package com.example.alquilervehiculos.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.alquilervehiculos.R
import com.example.alquilervehiculos.model.VehiculoEntity

interface VehiculoActionListener {
    fun onEdit(vehiculo: VehiculoEntity)
    fun onDelete(vehiculo: VehiculoEntity)
    fun onItemClick(vehiculo: VehiculoEntity)
}

// 🔑 CIRUGÍA FINAL: Se invierte el orden de los parámetros para forzar al compilador a re-evaluar.
class VehiculoAdapter(
    private var vehiculos: List<VehiculoEntity>,
    private val actionType: String, // Parámetro 1
    private val listener: VehiculoActionListener // Parámetro 2
) : RecyclerView.Adapter<VehiculoAdapter.VehiculoViewHolder>() {

    private val isOwnerView: Boolean = (actionType == "EDITAR")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VehiculoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_vehiculo, parent, false)
        return VehiculoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VehiculoViewHolder, position: Int) {
        holder.bind(vehiculos[position])
    }

    override fun getItemCount() = vehiculos.size

    fun updateData(newVehiculos: List<VehiculoEntity>) {
        this.vehiculos = newVehiculos
        notifyDataSetChanged()
    }

    inner class VehiculoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.imageViewVehicle)
        private val marcaModeloTextView: TextView = itemView.findViewById(R.id.textViewMarcaModelo)
        private val ciudadTextView: TextView = itemView.findViewById(R.id.textViewVehiculoCiudad)
        private val tipoTextView: TextView = itemView.findViewById(R.id.textViewTipo)
        private val precioTextView: TextView = itemView.findViewById(R.id.textViewPrecio)
        private val ownerActionsLayout: LinearLayout = itemView.findViewById(R.id.layoutOwnerActions)
        private val btnEdit: Button = itemView.findViewById(R.id.buttonEditVehicle)
        private val btnDelete: Button = itemView.findViewById(R.id.buttonDeleteVehicle)

        fun bind(vehiculo: VehiculoEntity) {
            imageView.load(vehiculo.imageUrl) {
                placeholder(R.drawable.ic_car_placeholder)
                error(R.drawable.ic_car_placeholder)
            }
            marcaModeloTextView.text = "${vehiculo.marca} ${vehiculo.modelo} (${vehiculo.ano})"
            ciudadTextView.text = vehiculo.ciudad
            tipoTextView.text = "Tipo: ${vehiculo.tipo} - Color: ${vehiculo.color}"
            precioTextView.text = String.format("$%.2f / día", vehiculo.precio)

            if (isOwnerView) {
                ownerActionsLayout.visibility = View.VISIBLE
                btnEdit.setOnClickListener { listener.onEdit(vehiculo) }
                btnDelete.setOnClickListener { listener.onDelete(vehiculo) }
                itemView.setOnClickListener(null)
            } else {
                ownerActionsLayout.visibility = View.GONE
                itemView.setOnClickListener { listener.onItemClick(vehiculo) }
            }
        }
    }
}
