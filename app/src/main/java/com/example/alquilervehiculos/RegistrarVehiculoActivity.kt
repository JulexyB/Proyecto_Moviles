package com.example.alquilervehiculos

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.alquilervehiculos.model.VehiculoEntity
import com.example.alquilervehiculos.viewmodel.VehiculoViewModel
import com.example.alquilervehiculos.viewmodel.VehiculoViewModelFactory
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

class RegistrarVehiculoActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private var imageUri: Uri? = null
    private var cameraImageUri: Uri? = null

    // 🔑 CORRECCIÓN: Se especifica el 'vehiculoRepository' correcto.
    private val vehiculoViewModel: VehiculoViewModel by viewModels {
        VehiculoViewModelFactory((application as AlquilerVehiculosApplication).vehiculoRepository)
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) showImageSourceDialog() else Toast.makeText(this, "El permiso de la cámara es necesario", Toast.LENGTH_LONG).show()
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            imageUri = cameraImageUri
            imageView.setImageURI(imageUri)
        }
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            it.data?.data?.let {
                imageUri = it
                imageView.setImageURI(imageUri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registrar_vehiculo)

        setupViews()
        setupListeners()
    }

    private fun setupViews() {
        imageView = findViewById(R.id.imageViewVehiculo)
    }

    private fun setupListeners() {
        val backArrow = findViewById<ImageView>(R.id.back_arrow_registrar)
        val btnRegistrar = findViewById<Button>(R.id.btnRegistrarVehiculo)

        backArrow.setOnClickListener { finish() }
        imageView.setOnClickListener { checkCameraPermissionAndShowDialog() }
        btnRegistrar.setOnClickListener { registrarVehiculo() }
    }

    private fun registrarVehiculo() {
        val ownerId = FirebaseAuth.getInstance().currentUser?.uid
        if (ownerId == null) {
            Toast.makeText(this, "Error: Debes estar autenticado.", Toast.LENGTH_LONG).show()
            return
        }

        val marca = findViewById<TextInputEditText>(R.id.editTextMarca).text.toString().trim()
        val modelo = findViewById<TextInputEditText>(R.id.editTextModelo).text.toString().trim()
        val placa = findViewById<TextInputEditText>(R.id.editTextPlaca).text.toString().trim()

        if (marca.isEmpty() || modelo.isEmpty() || placa.isEmpty()) {
            Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        val currentImageUri = imageUri
        if (currentImageUri == null) {
            Toast.makeText(this, "Por favor, selecciona una imagen", Toast.LENGTH_SHORT).show()
            return
        }

        val vehiculo = VehiculoEntity(
            placa = placa,
            marca = marca,
            modelo = modelo,
            ownerId = ownerId,
            estado = "Disponible"
            // Los demás campos usarán sus valores por defecto
        )

        // Usamos el ViewModel para registrar el vehículo
        lifecycleScope.launch {
            try {
                vehiculoViewModel.registrarVehiculo(vehiculo, currentImageUri)
                Toast.makeText(this@RegistrarVehiculoActivity, "Vehículo registrado con éxito", Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                Log.e("RegistroVehiculo", "Error en el registro", e)
                Toast.makeText(this@RegistrarVehiculoActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun checkCameraPermissionAndShowDialog() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> showImageSourceDialog()
            else -> requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun showImageSourceDialog() {
        AlertDialog.Builder(this)
            .setTitle("Seleccionar imagen")
            .setItems(arrayOf("Tomar foto", "Elegir de la galería")) { _, which ->
                when (which) {
                    0 -> openCamera()
                    1 -> openGallery()
                }
            }.show()
    }

    private fun openCamera() {
        createImageFile()?.let { file ->
            cameraImageUri = FileProvider.getUriForFile(this, "${applicationContext.packageName}.provider", file)
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri)
            cameraLauncher.launch(intent)
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    @Throws(IOException::class)
    private fun createImageFile(): File? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir = getExternalFilesDir(null)
            File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
        } catch (e: IOException) {
            Toast.makeText(this, "No se pudo crear el archivo de imagen.", Toast.LENGTH_LONG).show()
            null
        }
    }
}
