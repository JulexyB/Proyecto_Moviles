package com.example.alquilervehiculos

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.alquilervehiculos.viewmodel.VehiculoViewModel

class VehiculosListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: VehiculoAdapter
    private lateinit var actionType: String
    private lateinit var dataSource: String

    // 🔑 CORRECCIÓN: Se obtiene el 'vehiculoRepository' específico desde la clase Application.
    private val vehiculoViewModel: VehiculoViewModel by viewModels {
        VehiculoViewModelFactory((application as AlquilerVehiculosApplication).vehiculoRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vehiculos_list)

        actionType = intent.getStringExtra("ACTION_TYPE") ?: "VER"
        dataSource = intent.getStringExtra("DATA_SOURCE") ?: "ROOM"

        val backArrow = findViewById<ImageView>(R.id.back_arrow_list)
        val title = findViewById<TextView>(R.id.list_title)
        recyclerView = findViewById(R.id.recycler_view_vehiculos)

        backArrow.setOnClickListener { finish() }

        title.text = when (actionType) {
            "EDITAR" -> "Editar Vehículo"
            "ELIMINAR" -> "Eliminar Vehículo"
            "ACTUALIZAR_ESTADO" -> "Actualizar Estado"
            else -> "Lista de Vehículos"
        }

        setupRecyclerView()
        observeVehiculos()

        vehiculoViewModel.loadVehiculos(dataSource)
    }

    private fun setupRecyclerView() {
        adapter = VehiculoAdapter(emptyList(), actionType)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        adapter.onEditClickListener = { vehiculo ->
            val intent = Intent(this, VehiculoActivity::class.java).apply {
                putExtra("VEHICULO_PLACA_EDITAR", vehiculo.placa)
            }
            startActivity(intent)
        }

        adapter.onDeleteClickListener = { vehiculo ->
            AlertDialog.Builder(this)
                .setTitle("Confirmar Eliminación")
                .setMessage("¿Seguro que quieres eliminar el vehículo ${vehiculo.marca} ${vehiculo.modelo}?")
                .setPositiveButton("Eliminar") { _, _ -> vehiculoViewModel.delete(vehiculo) }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        adapter.onStatusChangeListener = { vehiculo, isChecked ->
            val nuevoEstado = if (isChecked) "Activo" else "Inactivo"
            vehiculo.estado = nuevoEstado
            vehiculoViewModel.update(vehiculo)
            Toast.makeText(this, "${vehiculo.placa} ahora está $nuevoEstado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeVehiculos() {
        vehiculoViewModel.vehiculos.observe(this) { vehiculos ->
            if (vehiculos.isEmpty()) {
                Toast.makeText(this, "No hay vehículos para mostrar", Toast.LENGTH_LONG).show()
            } else {
                adapter.updateData(vehiculos)
            }
        }
    }
}

class VehiculoViewModelFactory(private val repository: com.example.alquilervehiculos.repository.VehiculoRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VehiculoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VehiculoViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
