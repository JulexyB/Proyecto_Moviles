package com.example.alquilervehiculos

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.alquilervehiculos.adapter.ReservaAdapter
import com.example.alquilervehiculos.adapter.ReservaActionListener
import com.example.alquilervehiculos.databinding.ActivityMisVehiculosReservadosBinding
import com.example.alquilervehiculos.modelo.Reserva
import com.example.alquilervehiculos.modelo.ReservaViewModel
import com.example.alquilervehiculos.model.UsuarioEntity
import com.example.alquilervehiculos.model.VehiculoEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MisVehiculosReservadosActivity : AppCompatActivity(), ReservaActionListener {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val TAG = "ReservasArrendador"

    private lateinit var binding: ActivityMisVehiculosReservadosBinding
    private lateinit var reservaAdapter: ReservaAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMisVehiculosReservadosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()

        lifecycleScope.launch {
            cargarDatosCompletos()
        }
    }

    private fun setupRecyclerView() {
        // 1. Configurar el adaptador
        reservaAdapter = ReservaAdapter(emptyList(), esVistaArrendador = true, this)

        // 2. Configurar el RecyclerView con el ID correcto
        binding.recyclerViewReservados.apply {
            layoutManager = LinearLayoutManager(this@MisVehiculosReservadosActivity)
            adapter = reservaAdapter
        }
    }

    // ... (Tu función cargarDatosCompletos se mantiene igual) ...
    private suspend fun cargarDatosCompletos() {
        val ownerUid = auth.currentUser?.uid ?: return
        withContext(Dispatchers.Main) {
            binding.progressBar.visibility = View.VISIBLE
            binding.layoutNoData.visibility = View.GONE
        }
        try {
            val result = db.collection("reservas").whereEqualTo("arrendadorId", ownerUid).get().await()
            val listaReservas = result.documents.mapNotNull { it.toObject<Reserva>()?.apply { id = it.id } }

            if (listaReservas.isEmpty()) {
                withContext(Dispatchers.Main) {
                    binding.textViewNoData.text = "No tienes reservas pendientes."
                    binding.textViewNoData.visibility = View.VISIBLE
                    binding.progressBar.visibility = View.GONE
                    reservaAdapter.actualizarLista(emptyList())
                }
                return
            }
            val listaDeViewModels = coroutineScope {
                listaReservas.map { reserva ->
                    async(Dispatchers.IO) {
                        val cliente = try { db.collection("usuarios").document(reserva.clienteId).get().await().toObject<UsuarioEntity>() } catch (e: Exception) { null }
                        val vehiculo = try { db.collection("vehiculos").document(reserva.vehiculoId).get().await().toObject<VehiculoEntity>() } catch (e: Exception) { null }
                        ReservaViewModel(reserva, cliente, vehiculo)
                    }
                }
            }.awaitAll()

            withContext(Dispatchers.Main) {
                reservaAdapter.actualizarLista(listaDeViewModels)
                binding.progressBar.visibility = View.GONE
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar reservas", e)
            withContext(Dispatchers.Main) { binding.progressBar.visibility = View.GONE }
        }
    }

    // --- IMPLEMENTACIÓN DE LA INTERFAZ ---

    override fun onAccept(reserva: Reserva) {
        actualizarEstadoReserva(reserva.id, "CONFIRMADA", vehiculoPlaca = null)
    }

    override fun onReject(reserva: Reserva) {
        actualizarEstadoReserva(reserva.id, "RECHAZADA", reserva.vehiculoId, liberarVehiculo = true)
    }

    // 🔑 ¡ESTE MÉTODO ES OBLIGATORIO PORQUE ESTÁ EN LA INTERFAZ!
    override fun onCancel(reserva: Reserva) {
        // El arrendador no cancela, rechaza. Dejamos esto vacío.
    }

    private fun actualizarEstadoReserva(reservaId: String, nuevoEstado: String, vehiculoPlaca: String?, liberarVehiculo: Boolean = false) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                db.collection("reservas").document(reservaId).update("estado", nuevoEstado).await()
                if (liberarVehiculo && vehiculoPlaca != null) {
                    db.collection("vehiculos").document(vehiculoPlaca).update("disponible", true).await()
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MisVehiculosReservadosActivity, "Reserva $nuevoEstado", Toast.LENGTH_SHORT).show()
                    cargarDatosCompletos()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error", e)
            }
        }
    }
}