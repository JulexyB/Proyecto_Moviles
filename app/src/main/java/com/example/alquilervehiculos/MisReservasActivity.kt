package com.example.alquilervehiculos

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.alquilervehiculos.adapter.ReservaActionListener
import com.example.alquilervehiculos.adapter.ReservaAdapter
import com.example.alquilervehiculos.databinding.ActivityMisReservasBinding
import com.example.alquilervehiculos.modelo.Reserva
import com.example.alquilervehiculos.modelo.ReservaViewModel
import com.example.alquilervehiculos.model.VehiculoEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class MisReservasActivity : AppCompatActivity(), ReservaActionListener {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val TAG = "MisReservasCliente"

    private lateinit var binding: ActivityMisReservasBinding
    private lateinit var reservaAdapter: ReservaAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMisReservasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        lifecycleScope.launch {
            cargarDatosCompletos()
        }
    }

    private fun setupRecyclerView() {
        // 2. Pasamos 'this' como listener
        reservaAdapter = ReservaAdapter(emptyList(), esVistaArrendador = false, actionListener = this)
        binding.recyclerViewReservas.apply {
            layoutManager = LinearLayoutManager(this@MisReservasActivity)
            adapter = reservaAdapter
        }
    }

    private suspend fun cargarDatosCompletos() {
        val clienteId = auth.currentUser?.uid
        if (clienteId == null) {
            Toast.makeText(this, "Error de sesión", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.layoutNoData.visibility = View.GONE

        try {
            // 1. Obtener las reservas del cliente
            val reservasSnapshot = db.collection("reservas")
                .whereEqualTo("clienteId", clienteId)
                .get().await()
            val listaReservas = reservasSnapshot.toObjects(Reserva::class.java)

            if (listaReservas.isEmpty()) {
                binding.textViewNoData.text = "Aún no has realizado ninguna reserva."
                binding.textViewNoData.visibility = View.VISIBLE
                binding.progressBar.visibility = View.GONE
                return
            }

            // 2. Para cada reserva, buscar la info del vehículo
            val listaDeViewModels = coroutineScope { // <--- SE AÑADE ESTA LÍNEA
                listaReservas.map { reserva ->
                    async {
                        val vehiculo = try {
                            db.collection("vehiculos").document(reserva.vehiculoId).get().await().toObject<VehiculoEntity>()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error al buscar vehículo ${reserva.vehiculoId}", e)
                            null
                        }
                        ReservaViewModel(reserva, cliente = null, vehiculo)
                    }
                }
            }.awaitAll()

            // 3. Actualizar el adaptador
            reservaAdapter.actualizarLista(listaDeViewModels)
            binding.progressBar.visibility = View.GONE

        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar las reservas del cliente.", e)
            binding.progressBar.visibility = View.GONE
            binding.textViewNoData.text = "Error al cargar tus reservas."
            binding.textViewNoData.visibility = View.VISIBLE
            Toast.makeText(this, "Error de red: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    // ===================================================================
    // 3. LÓGICA DE CANCELACIÓN (Implementación de la Interfaz)
    // ===================================================================

    override fun onCancel(reserva: Reserva) {
        // Lógica para que el CLIENTE cancele su propia reserva
        cancelarReserva(reserva)
    }

    // Estos métodos no se usan en la vista de cliente (botones ocultos), pero deben implementarse
    override fun onAccept(reserva: Reserva) {}
    override fun onReject(reserva: Reserva) {}

    private fun cancelarReserva(reserva: Reserva) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. Cambiar estado a CANCELADA
                db.collection("reservas").document(reserva.id)
                    .update("estado", "CANCELADA")
                    .await()

                // 2. Liberar el vehículo (disponible = true)
                // Esto es importante porque al reservar se puso en false.
                db.collection("vehiculos").document(reserva.vehiculoId)
                    .update("disponible", true)
                    .await()

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MisReservasActivity, "Reserva cancelada correctamente.", Toast.LENGTH_SHORT).show()
                    // Recargar lista
                    cargarDatosCompletos()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error al cancelar reserva", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MisReservasActivity, "Error al cancelar: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}

