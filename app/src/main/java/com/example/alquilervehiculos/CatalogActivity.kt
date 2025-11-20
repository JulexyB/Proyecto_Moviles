package com.example.alquilervehiculos

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.alquilervehiculos.adapter.VehiculoAdapter
import com.example.alquilervehiculos.adapter.VehiculoActionListener
import com.example.alquilervehiculos.databinding.ActivityCatalogBinding
import com.example.alquilervehiculos.model.VehiculoEntity
import com.example.alquilervehiculos.viewmodel.VerListaVehiculosViewModel
import com.google.firebase.auth.FirebaseAuth

class CatalogActivity : AppCompatActivity(), VehiculoActionListener {

    private lateinit var binding: ActivityCatalogBinding
    private lateinit var adapter: VehiculoAdapter
    private val auth = FirebaseAuth.getInstance()

    private val viewModel: VerListaVehiculosViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCatalogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupObservers()

        binding.fabAddVehicle.setOnClickListener {
            startActivity(Intent(this, VehiculoActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        // La llamada a fetchAllVehiculos() se elimina porque el ViewModel ahora es reactivo.
        // Los datos se observan y la UI se actualiza automáticamente.
    }

    private fun setupRecyclerView() {
        val actionListener: VehiculoActionListener = this
        
        // Se añade el parámetro 'listener' que faltaba en el constructor.
        adapter = VehiculoAdapter(
            vehiculos = emptyList(), 
            actionType = "EDITAR",
            listener = actionListener
        )
        binding.recyclerViewVehiculos.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewVehiculos.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.allVehiculos.observe(this) { vehiculos ->
            val ownerId = auth.currentUser?.uid
            val ownerVehicles = vehiculos.filter { it.ownerId == ownerId }
            if (ownerVehicles.isEmpty()) {
                binding.textViewNoVehicles.visibility = View.VISIBLE
                binding.recyclerViewVehiculos.visibility = View.GONE
            } else {
                binding.textViewNoVehicles.visibility = View.GONE
                binding.recyclerViewVehiculos.visibility = View.VISIBLE
                adapter.updateData(ownerVehicles)
            }
        }
    }

    override fun onEdit(vehiculo: VehiculoEntity) {
        val intent = Intent(this, VehiculoActivity::class.java).apply {
            putExtra("VEHICULO_PLACA_EDITAR", vehiculo.placa)
        }
        startActivity(intent)
    }

    override fun onDelete(vehiculo: VehiculoEntity) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar Eliminación")
            .setMessage("¿Estás seguro de que deseas eliminar el vehículo ${vehiculo.marca} ${vehiculo.modelo}?")
            .setPositiveButton("Eliminar") { _, _ -> viewModel.delete(vehiculo) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onItemClick(vehiculo: VehiculoEntity) {
        // No es necesario implementar nada aquí para la vista del propietario.
    }
}
