package com.example.alquilervehiculos

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.alquilervehiculos.adapter.VehiculoAdapter
import com.example.alquilervehiculos.adapter.VehiculoActionListener // ⬅️ Interfaz para Editar/Eliminar
import com.example.alquilervehiculos.cloud.VehiculoService
import com.example.alquilervehiculos.database.AppDatabase
import com.example.alquilervehiculos.databinding.ActivityCatalogBinding
import com.example.alquilervehiculos.model.VehiculoEntity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


// 1. Implementar la interfaz VehiculoActionListener
class CatalogActivity : AppCompatActivity(), VehiculoActionListener {

    private lateinit var db: AppDatabase
    private lateinit var adapter: VehiculoAdapter
    private lateinit var binding: ActivityCatalogBinding
    private val auth = FirebaseAuth.getInstance()

    // Variables de estado para los filtros
    private var filterOwnerUid: String? = null //
    private var isOwnerView: Boolean = false // Flag para el adaptador
    private var filterSearchQuery: String? = null
    private var filterTipo: String? = null

    // Lista de vehículos original (sin filtrar)
    private var listaCompletaVehiculos = listOf<VehiculoEntity>()
    private val tiposVehiculo = listOf("Todos", "Sedan", "SUV", "Camioneta", "Deportivo", "Motocicleta")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCatalogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this)

        // 1. OBTENER PARÁMETRO DE GESTIÓN
        filterOwnerUid = intent.getStringExtra("FILTER_BY_OWNER_UID")
        isOwnerView = filterOwnerUid != null // TRUE si se lanzó desde OwnerActivity

        // 2. AJUSTAR VISTAS PARA EL MODO GESTIÓN
        binding.textViewCatalogTitle.text = if (isOwnerView) "Gestión de Mis Vehículos" else "Catálogo General"
        toggleFilterVisibility(!isOwnerView) // Ocultar filtros si es vista de dueño

        setupRecyclerView()
        setupListeners()
        loadVehicles()
    }

    private fun setupRecyclerView() {
        // El adaptador se inicializa con el flag de modo dueño y el listener
        adapter = VehiculoAdapter(emptyList(), isOwnerView, this)
        binding.recyclerViewCatalog.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewCatalog.adapter = adapter
    }

    private fun setupListeners() {
        // Configurar el Spinner de Tipo de Vehículo
        ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            tiposVehiculo
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerFilterTipo.adapter = adapter
        }

        binding.spinnerFilterTipo.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                filterTipo = if (position > 0) tiposVehiculo[position] else null
                aplicarFiltros()
            }
            override fun onNothingSelected(parent: AdapterView<*>) { /* No se usa */ }
        }

        // Configurar el SearchView
        binding.searchViewCatalog.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterSearchQuery = query?.trim()?.lowercase()
                aplicarFiltros()
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                filterSearchQuery = newText?.trim()?.lowercase()
                aplicarFiltros()
                return true
            }
        })
    }

    private fun toggleFilterVisibility(show: Boolean) {
        // Ocultamos la barra de búsqueda y el spinner si estamos en modo gestión
        binding.searchViewCatalog.visibility = if (show) View.VISIBLE else View.GONE
        binding.spinnerFilterTipo.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun aplicarFiltros() {
        var listaFiltrada = listaCompletaVehiculos

        // 1. Filtrar por Tipo de Vehículo
        if (filterTipo != null) {
            listaFiltrada = listaFiltrada.filter { it.tipo.equals(filterTipo, ignoreCase = true) }
        }

        // 2. Filtrar por Búsqueda (Marca, Modelo o Ciudad)
        if (!filterSearchQuery.isNullOrBlank()) {
            val query = filterSearchQuery!!
            listaFiltrada = listaFiltrada.filter {
                it.marca.lowercase().contains(query) ||
                        it.modelo.lowercase().contains(query) ||
                        it.ciudad.lowercase().contains(query)
            }
        }

        adapter.updateData(listaFiltrada)
        updateEmptyView(listaFiltrada)
    }

    /**
     * Carga vehículos de Room y los sincroniza con Firestore.
     */
    private fun loadVehicles() {
        // Define la función para cargar datos de Room (dependiendo del modo)
        val loadFunction: suspend () -> List<VehiculoEntity> = if (isOwnerView) {
            { db.vehiculoDao().getVehiculosByOwner(filterOwnerUid!!) } // Filtrar por dueño
        } else {
            { db.vehiculoDao().getAvailableVehiculos() } // Solo disponibles
        }

        lifecycleScope.launch(Dispatchers.IO) {
            // 1. Cargar datos iniciales desde Room y mostrarlos
            val localVehicles = loadFunction()
            withContext(Dispatchers.Main) {
                listaCompletaVehiculos = localVehicles
                aplicarFiltros()
            }

            // 2. Sincronizar con Firestore
            val fetchVehiclesFunction: ((List<VehiculoEntity>) -> Unit) -> Unit = if (isOwnerView) {
                { callback -> VehiculoService.obtenerVehiculosPorOwnerId(filterOwnerUid!!, callback) }
            } else {
                { callback -> VehiculoService.obtenerVehiculos(callback) }
            }

            fetchVehiclesFunction { freshVehicles ->
                lifecycleScope.launch(Dispatchers.IO) {
                    // Actualizar Room con los datos frescos
                    db.vehiculoDao().insertAll(freshVehicles)

                    // Volver a cargar la lista completa y actualizar la UI
                    val updatedLocalVehicles = loadFunction()
                    withContext(Dispatchers.Main) {
                        listaCompletaVehiculos = updatedLocalVehicles
                        aplicarFiltros()
                        Toast.makeText(this@CatalogActivity, "Catálogo sincronizado.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun updateEmptyView(vehicles: List<VehiculoEntity>) {
        if (vehicles.isEmpty()) {
            binding.recyclerViewCatalog.visibility = View.GONE
            binding.textViewEmptyCatalog.visibility = View.VISIBLE
            binding.textViewEmptyCatalog.text = if (isOwnerView) {
                "Aún no tienes vehículos registrados."
            } else {
                "No se encontraron vehículos con estos filtros."
            }
        } else {
            binding.recyclerViewCatalog.visibility = View.VISIBLE
            binding.textViewEmptyCatalog.visibility = View.GONE
        }
    }

    // =========================================================================
    // 4. IMPLEMENTACIÓN DE VEHICULO ACTION LISTENER (Gestión de Inventario)
    // =========================================================================

    // 🔑 FUNCIONALIDAD EDITAR
    override fun onEdit(vehiculo: VehiculoEntity) {
        val intent = Intent(this, VehiculoActivity::class.java).apply {
            // Pasamos la PLACA para que VehiculoActivity sepa qué vehículo cargar
            putExtra("VEHICULO_PLACA_EDITAR", vehiculo.placa)
        }
        startActivity(intent)
    }

    // 🔑 FUNCIONALIDAD ELIMINAR
    override fun onDelete(vehiculo: VehiculoEntity) {
        // Pedir confirmación antes de eliminar
        AlertDialog.Builder(this)
            .setTitle("Confirmar Eliminación")
            .setMessage("¿Estás seguro de que quieres eliminar el vehículo ${vehiculo.marca} ${vehiculo.modelo} (${vehiculo.placa})? Esta acción es irreversible.")
            .setPositiveButton("Eliminar") { dialog, which ->
                deleteVehicleConfirmed(vehiculo)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteVehicleConfirmed(vehiculo: VehiculoEntity) {
        // La lógica de eliminación DEBE estar aquí para que el botón funcione.
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Llamar al servicio que elimina de Firestore y Storage
                VehiculoService.eliminarVehiculo(vehiculo.placa, vehiculo.ownerId)

                // Eliminar de ROOM
                db.vehiculoDao().deleteVehiculo(vehiculo)

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CatalogActivity, "Vehículo eliminado con éxito.", Toast.LENGTH_SHORT).show()
                    // Recargar la lista después de la eliminación
                    loadVehicles()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CatalogActivity, "Error al eliminar: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}