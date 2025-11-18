package com.example.alquilervehiculos

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.alquilervehiculos.cloud.StorageService
import com.example.alquilervehiculos.cloud.VehiculoService
import com.example.alquilervehiculos.database.AppDatabase
import com.example.alquilervehiculos.databinding.ActivityVehiculoBinding
import com.example.alquilervehiculos.model.UsuarioEntity
import com.example.alquilervehiculos.model.VehiculoEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Calendar

class VehiculoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVehiculoBinding
    private val auth = FirebaseAuth.getInstance()
    private val firestore = Firebase.firestore
    private lateinit var db: AppDatabase

    // Variables de Estado para Edición
    private var imageUri: Uri? = null
    private var isEditMode: Boolean = false
    private var vehiclePlacaToEdit: String? = null // Placa si estamos en modo Edición
    private var originalImageUrl: String? = null // URL de la imagen existente

    private val tiposVehiculo = arrayOf("Sedan", "SUV", "Camioneta", "Deportivo", "Motocicleta")
    private val ciudadesDisponibles = arrayOf("Quito", "Guayaquil", "Cuenca", "Manta", "Loja", "Ambato")

    // ActivityResultLauncher para seleccionar imagen de la galería
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                imageUri = uri
                binding.imageViewVehicleImage.setImageURI(uri)
                binding.textViewSelectImage.visibility = View.GONE
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVehiculoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = AppDatabase.getDatabase(this)

        // 1. OBTENER ESTADO Y VALIDAR SESIÓN
        vehiclePlacaToEdit = intent.getStringExtra("VEHICULO_PLACA_EDITAR")
        isEditMode = vehiclePlacaToEdit != null

        if (auth.currentUser == null) {
            Toast.makeText(this, "Error: Sesión inválida.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupSpinners()
        setupListeners()

        // 2. CONFIGURACIÓN DE MODO EDICIÓN/CREACIÓN
        if (isEditMode) {
            binding.textViewVehicleFormTitle.text = "Editar Vehículo"
            binding.buttonSaveVehicle.text = "Guardar Cambios"
            binding.editTextVehiclePlaca.isEnabled = false // Bloquea la placa

            // Carga los datos del vehículo existente
            loadVehicleDataForEdit(vehiclePlacaToEdit!!)
        } else {
            binding.textViewVehicleFormTitle.text = "Registrar Nuevo Vehículo"
        }
    }

    private fun setupSpinners() {
        // Adaptador para Tipo de Vehículo
        ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            tiposVehiculo
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerTipo.adapter = adapter
        }

        // Adaptador para Ciudad
        ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            ciudadesDisponibles
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerCiudad.adapter = adapter
        }
    }

    private fun setupListeners() {
        binding.imageViewVehicleImage.setOnClickListener { openGallery() }
        binding.textViewSelectImage.setOnClickListener { openGallery() }
        binding.buttonSaveVehicle.setOnClickListener { saveVehicle() }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
        galleryLauncher.launch(intent)
    }

    // -----------------------------------------------------------------------------------
    // 🔑 FUNCIÓN PARA CARGAR DATOS EN MODO EDICIÓN
    // -----------------------------------------------------------------------------------
    private fun loadVehicleDataForEdit(placa: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val vehicleDoc = firestore.collection("vehiculos").document(placa).get().await()
                val vehicle = vehicleDoc.toObject(VehiculoEntity::class.java)

                withContext(Dispatchers.Main) {
                    if (vehicle != null) {
                        // 1. Pre-llenar campos
                        binding.editTextVehicleMarca.setText(vehicle.marca)
                        binding.editTextVehicleModelo.setText(vehicle.modelo)
                        binding.editTextVehicleAnio.setText(vehicle.anio.toString())
                        binding.editTextVehicleColor.setText(vehicle.color)
                        binding.editTextVehiclePrecioDia.setText(vehicle.precioDia.toString())
                        binding.editTextVehiclePlaca.setText(vehicle.placa)

                        // 2. Seleccionar valores en Spinners (usa la posición del array)
                        binding.spinnerTipo.setSelection(tiposVehiculo.indexOf(vehicle.tipo))
                        binding.spinnerCiudad.setSelection(ciudadesDisponibles.indexOf(vehicle.ciudad))

                        // 3. Cargar imagen existente (requiere Glide/Coil, si lo tienes)
                        originalImageUrl = vehicle.imageUrl
                        // Si usas Glide/Coil: Glide.with(this).load(vehicle.imageUrl).into(binding.imageViewVehicleImage)

                        binding.textViewSelectImage.visibility = View.GONE
                    } else {
                        Toast.makeText(this@VehiculoActivity, "Error: Vehículo no encontrado.", Toast.LENGTH_LONG).show()
                        finish()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@VehiculoActivity, "Error al cargar datos: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // -----------------------------------------------------------------------------------
    // 🔑 FUNCIÓN PARA GUARDAR O ACTUALIZAR EL VEHÍCULO
    // -----------------------------------------------------------------------------------
    private fun saveVehicle() {
        binding.buttonSaveVehicle.isEnabled = false
        binding.buttonSaveVehicle.text = if (isEditMode) "Guardando Cambios..." else "Subiendo..."

        // Captura de Campos
        val marca = binding.editTextVehicleMarca.text.toString().trim()
        val modelo = binding.editTextVehicleModelo.text.toString().trim()
        val anioStr = binding.editTextVehicleAnio.text.toString().trim()
        val color = binding.editTextVehicleColor.text.toString().trim()
        val precioDiaStr = binding.editTextVehiclePrecioDia.text.toString().trim()
        val tipo = binding.spinnerTipo.selectedItem.toString()
        val ciudad = binding.spinnerCiudad.selectedItem.toString()

        // Determinar la placa correcta
        val placaToSave = vehiclePlacaToEdit ?: binding.editTextVehiclePlaca.text.toString().trim().uppercase()

        // Conversión y Validación
        val anio = anioStr.toIntOrNull()
        val precioDia = precioDiaStr.toDoubleOrNull()

        if (marca.isBlank() || anio == null || precioDia == null || precioDia <= 0) {
            Toast.makeText(this, "Verifica los campos obligatorios o numéricos.", Toast.LENGTH_LONG).show()
            // ... (restaurar botón y retornar)
            return
        }

        val userUid = auth.currentUser?.uid
        // Si no hay UID o no hay imagen (y no es modo edición o no hay imagen original)
        if (userUid == null || (imageUri == null && originalImageUrl == null && !isEditMode)) {
            Toast.makeText(this, "Error de sesión o falta seleccionar foto.", Toast.LENGTH_LONG).show()
            // ... (restaurar botón y retornar)
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // A. Obtener Teléfono del Propietario
                var telefonoPropietario: String? = db.usuarioDao().getUserByUid(userUid)?.phone
                if (telefonoPropietario.isNullOrBlank()) {
                    val userFromFirestore = firestore.collection("usuarios").document(userUid).get().await().toObject<UsuarioEntity>()
                    telefonoPropietario = userFromFirestore?.phone
                }
                if (telefonoPropietario.isNullOrBlank()) {
                    throw IllegalStateException("Tu perfil no tiene un número de teléfono...")
                }

                // B. Lógica de Imagen: Subir solo si hay nueva URI
                val finalImageUrl = when {
                    imageUri != null -> StorageService.uploadVehicleImage(imageUri!!, userUid, placaToSave)
                    isEditMode && originalImageUrl != null -> originalImageUrl
                    else -> throw IllegalStateException("Fallo al obtener la URL de la foto.")
                } ?: throw IllegalStateException("Fallo al obtener la URL de la foto.")

                // 3. Crear la entidad (USANDO !! EN LOS VALORES VALIDADOS)
                val nuevoVehiculo = VehiculoEntity(
                    placa = placaToSave, // Clave Primaria para Sobrescribir
                    ownerId = userUid,
                    marca = marca,
                    modelo = modelo,
                    anio = anio!!,
                    color = color,
                    precioDia = precioDia!!,
                    tipo = tipo,
                    ciudad = ciudad,
                    imageUrl = finalImageUrl,
                    disponible = true,
                    telefonoPropietario = telefonoPropietario
                )

                // 4. Guardado Dual: Firestore y Room
                VehiculoService.guardarVehiculo(nuevoVehiculo)
                db.vehiculoDao().insertVehiculo(nuevoVehiculo)

                withContext(Dispatchers.Main) {
                    val message = if (isEditMode) "Vehículo actualizado con éxito!" else "¡Vehículo registrado con éxito!"
                    Toast.makeText(this@VehiculoActivity, message, Toast.LENGTH_LONG).show()
                    finish()
                }

            } catch (e: Exception) {
                // Manejo de errores
                Log.e("VehiculoActivity", "Error al guardar vehículo", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@VehiculoActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    binding.buttonSaveVehicle.isEnabled = true
                    binding.buttonSaveVehicle.text = if (isEditMode) "Guardar Cambios" else "Subir Vehículo"
                }
            }
        }
    }
}
