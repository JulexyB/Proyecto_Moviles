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
import coil.load
import com.example.alquilervehiculos.cloud.StorageService
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

class VehiculoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVehiculoBinding
    private val auth = FirebaseAuth.getInstance()
    private val firestore = Firebase.firestore
    private lateinit var db: AppDatabase

    private var imageUri: Uri? = null
    private var isEditMode: Boolean = false
    private var vehiclePlacaToEdit: String? = null
    private var originalImageUrl: String? = null

    private val tiposVehiculo = arrayOf("Sedan", "SUV", "Camioneta", "Deportivo", "Motocicleta")
    private val ciudadesDisponibles = arrayOf("Quito", "Guayaquil", "Cuenca", "Manta", "Loja", "Ambato")

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let {
                imageUri = it
                binding.imageViewVehicleImage.setImageURI(it)
                binding.textViewSelectImage.visibility = View.GONE
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVehiculoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = AppDatabase.getDatabase(this)

        vehiclePlacaToEdit = intent.getStringExtra("VEHICULO_PLACA_EDITAR")
        isEditMode = vehiclePlacaToEdit != null

        if (auth.currentUser == null) {
            Toast.makeText(this, "Error: Sesión inválida.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupSpinners()
        setupListeners()

        if (isEditMode) {
            binding.textViewVehicleFormTitle.text = "Editar Vehículo"
            binding.buttonSaveVehicle.text = "Guardar Cambios"
            binding.editTextVehiclePlaca.isEnabled = false
            loadVehicleDataForEdit(vehiclePlacaToEdit!!)
        } else {
            binding.textViewVehicleFormTitle.text = "Registrar Nuevo Vehículo"
        }
    }

    private fun setupSpinners() {
        val commonAdapter = { data: Array<String> ->
            ArrayAdapter(this, android.R.layout.simple_spinner_item, data).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
        }
        binding.spinnerTipo.adapter = commonAdapter(tiposVehiculo)
        binding.spinnerCiudad.adapter = commonAdapter(ciudadesDisponibles)
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

    private fun loadVehicleDataForEdit(placa: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val vehicle = db.vehiculoDao().getVehiculoByPlaca(placa)

                withContext(Dispatchers.Main) {
                    if (vehicle != null) {
                        binding.editTextVehicleMarca.setText(vehicle.marca)
                        binding.editTextVehicleModelo.setText(vehicle.modelo)
                        binding.editTextVehicleAnio.setText(vehicle.ano.toString())
                        binding.editTextVehicleColor.setText(vehicle.color)
                        binding.editTextVehiclePrecioDia.setText(vehicle.precio.toString())
                        binding.editTextVehiclePlaca.setText(vehicle.placa)

                        binding.spinnerTipo.setSelection(tiposVehiculo.indexOf(vehicle.tipo))
                        binding.spinnerCiudad.setSelection(ciudadesDisponibles.indexOf(vehicle.ciudad))

                        originalImageUrl = vehicle.imageUrl
                        if (!originalImageUrl.isNullOrEmpty()) {
                            binding.imageViewVehicleImage.load(originalImageUrl)
                        }

                        binding.textViewSelectImage.visibility = View.GONE
                    } else {
                        Toast.makeText(this@VehiculoActivity, "Error: Vehículo no encontrado.", Toast.LENGTH_LONG).show()
                        finish()
                    }
                }
            } catch (e: Exception) {
                Log.e("VehiculoActivity", "Error al cargar datos", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@VehiculoActivity, "Error al cargar datos: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun saveVehicle() {
        setLoadingState(true)

        val marca = binding.editTextVehicleMarca.text.toString().trim()
        val modelo = binding.editTextVehicleModelo.text.toString().trim()
        val anoStr = binding.editTextVehicleAnio.text.toString().trim()
        val color = binding.editTextVehicleColor.text.toString().trim()
        val precioStr = binding.editTextVehiclePrecioDia.text.toString().trim()
        val tipo = binding.spinnerTipo.selectedItem.toString()
        val ciudad = binding.spinnerCiudad.selectedItem.toString()
        val placaToSave = vehiclePlacaToEdit ?: binding.editTextVehiclePlaca.text.toString().trim().uppercase()

        val ano = anoStr.toIntOrNull()
        val precio = precioStr.toDoubleOrNull()

        if (marca.isBlank() || modelo.isBlank() || placaToSave.isBlank() || ano == null || ano <= 1900 || precio == null || precio <= 0) {
            Toast.makeText(this, "Verifica todos los campos. El año y precio deben ser números válidos.", Toast.LENGTH_LONG).show()
            setLoadingState(false)
            return
        }

        val userUid = auth.currentUser?.uid
        if (userUid == null) {
            Toast.makeText(this, "Error de sesión. Vuelve a iniciar sesión.", Toast.LENGTH_LONG).show()
            setLoadingState(false)
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Obtener teléfono del perfil de usuario
                val userDoc = firestore.collection("usuarios").document(userUid).get().await()
                val telefono = userDoc.getString("phone")
                if (telefono.isNullOrBlank()) throw IllegalStateException("Tu perfil no tiene un número de teléfono.")

                // Subir imagen solo si se seleccionó una nueva
                val finalImageUrl = if (imageUri != null) {
                    StorageService.uploadVehicleImage(imageUri!!, userUid, placaToSave)
                } else {
                    originalImageUrl
                } ?: throw IllegalStateException("No se pudo determinar la imagen del vehículo.")

                // Crear la entidad con los campos corregidos
                val vehiculo = VehiculoEntity(
                    placa = placaToSave,
                    ownerId = userUid,
                    marca = marca,
                    modelo = modelo,
                    ano = ano,
                    color = color,
                    precio = precio,
                    tipo = tipo,
                    ciudad = ciudad,
                    imageUrl = finalImageUrl,
                    estado = "Disponible",
                    telefonoPropietario = telefono
                )

                firestore.collection("vehiculos").document(vehiculo.placa).set(vehiculo).await()
                db.vehiculoDao().insert(vehiculo)

                withContext(Dispatchers.Main) {
                    val message = if (isEditMode) "Vehículo actualizado con éxito" else "Vehículo registrado con éxito"
                    Toast.makeText(this@VehiculoActivity, message, Toast.LENGTH_LONG).show()
                    finish()
                }

            } catch (e: Exception) {
                Log.e("VehiculoActivity", "Error al guardar vehículo", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@VehiculoActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    setLoadingState(false)
                }
            }
        }
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.buttonSaveVehicle.isEnabled = !isLoading
        binding.buttonSaveVehicle.text = if (isLoading) {
            if (isEditMode) "Guardando..." else "Registrando..."
        } else {
            if (isEditMode) "Guardar Cambios" else "Registrar Vehículo"
        }
    }
}
