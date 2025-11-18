package com.example.alquilervehiculos

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.alquilervehiculos.cloud.StorageService
import com.example.alquilervehiculos.database.AppDatabase
import com.example.alquilervehiculos.databinding.ActivityProfileBinding
import com.example.alquilervehiculos.model.UsuarioEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var db: AppDatabase

    private var imageUri: Uri? = null
    private var isEditMode = false
    private var currentUsuario: UsuarioEntity? = null

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                imageUri = uri
                Glide.with(this).load(uri).circleCrop().into(binding.imageViewProfilePicture)
                Toast.makeText(this, "Foto seleccionada. Presiona GUARDAR.", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this)

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Sesión no válida.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        loadUserProfile(currentUser.uid)

        binding.buttonEditSaveProfile.setOnClickListener {
            if (isEditMode) {
                saveUserProfile(currentUser.uid)
            } else {
                setEditMode(true)
            }
        }

        binding.buttonChangePhoto.setOnClickListener {
            if (isEditMode) {
                openGallery()
            } else {
                Toast.makeText(this, "Presiona 'Editar Perfil' primero.", Toast.LENGTH_SHORT).show()
            }
        }
        setEditMode(false)
    }

    private fun setEditMode(enable: Boolean) {
        isEditMode = enable
        binding.editTextProfileFirstName.isEnabled = enable
        binding.editTextProfileLastName.isEnabled = enable
        binding.editTextProfileBirthDate.isEnabled = enable
        binding.editTextProfilePhone.isEnabled = enable
        binding.buttonChangePhoto.visibility = if (enable) View.VISIBLE else View.INVISIBLE
        binding.buttonEditSaveProfile.text = if (enable) "Guardar Cambios" else "Editar Perfil"
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
        galleryLauncher.launch(intent)
    }

    // --- LÓGICA DE CARGA UNIFICADA Y CORREGIDA ---
    private fun loadUserProfile(uid: String) {
        lifecycleScope.launch {
            try {
                // 1. OBTENEMOS LOS DATOS FRESCOS DE FIRESTORE
                val document = firestore.collection("usuarios").document(uid).get().await()
                val userFromFirestore = document.toObject<UsuarioEntity>()
                if (userFromFirestore == null) {
                    Toast.makeText(this@ProfileActivity, "Error: No se encontró el perfil en la nube.", Toast.LENGTH_LONG).show()
                    finish()
                    return@launch
                }

                // 2. OBTENEMOS EL USUARIO LOCAL DE ROOM PARA CONOCER SU 'id'
                var localUser = withContext(Dispatchers.IO) { db.usuarioDao().getUserByUid(uid) }

                if (localUser == null) {
                    // Si no existe en Room, lo insertamos para que obtenga un 'id'
                    withContext(Dispatchers.IO) { db.usuarioDao().insertUser(userFromFirestore) }
                    // Y lo volvemos a leer para tener la versión con el 'id' local
                    localUser = withContext(Dispatchers.IO) { db.usuarioDao().getUserByUid(uid) }!!
                }

                // 3. ACTUALIZAMOS EL OBJETO LOCAL CON LOS DATOS DE FIRESTORE
                currentUsuario = localUser.copy(
                    firstName = userFromFirestore.firstName,
                    lastName = userFromFirestore.lastName,
                    birthDate = userFromFirestore.birthDate,
                    phone = userFromFirestore.phone,
                    rol = userFromFirestore.rol,
                    profilePhotoUrl = userFromFirestore.profilePhotoUrl
                )
                // Calculamos el nombre completo siempre para asegurar consistencia
                currentUsuario!!.nombreCompleto = "${currentUsuario!!.firstName} ${currentUsuario!!.lastName}"


                // 4. GUARDAMOS LA VERSIÓN SINCRONIZADA EN ROOM (ACTUALIZANDO)
                withContext(Dispatchers.IO) {
                    db.usuarioDao().insertUser(currentUsuario!!)
                    Log.d("ProfileActivity", "Usuario sincronizado en Room. ID: ${currentUsuario!!.id}")
                }

                // 5. MOSTRAMOS LOS DATOS EN LA UI
                populateUI(currentUsuario!!)

            } catch (e: Exception) {
                // Si todo lo anterior falla (ej. sin red), intentamos cargar desde el respaldo local
                Log.w("ProfileActivity", "Fallo al cargar de Firestore, intentando desde Room...", e)
                val userFromRoom = withContext(Dispatchers.IO) { db.usuarioDao().getUserByUid(uid) }
                if (userFromRoom != null) {
                    currentUsuario = userFromRoom
                    populateUI(userFromRoom)
                    Toast.makeText(this@ProfileActivity, "Mostrando datos locales (sin conexión)", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@ProfileActivity, "Error: No se pudo cargar el perfil.", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
    }

    private fun populateUI(user: UsuarioEntity) {
        binding.editTextProfileEmail.setText(user.email)
        binding.editTextProfileFirstName.setText(user.firstName)
        binding.editTextProfileLastName.setText(user.lastName)
        binding.editTextProfileBirthDate.setText(user.birthDate)
        binding.editTextProfilePhone.setText(user.phone)

        if (user.profilePhotoUrl.isNullOrEmpty()) {
            Glide.with(this).load(user.profilePhotoUrl).circleCrop().placeholder(R.drawable.ic_profile).into(binding.imageViewProfilePicture)
        }
    }

    // --- LÓGICA HÍBRIDA DE GUARDADO (CORREGIDA) ---
    private fun saveUserProfile(uid: String) {
        if (binding.editTextProfileFirstName.text.toString().isBlank() || binding.editTextProfilePhone.text.toString().isBlank()) {
            Toast.makeText(this, "El nombre y el teléfono son obligatorios.", Toast.LENGTH_SHORT).show()
            return
        }

        binding.buttonEditSaveProfile.isEnabled = false
        binding.buttonEditSaveProfile.text = "Guardando..."

        lifecycleScope.launch {
            try {
                var newPhotoUrl = currentUsuario?.profilePhotoUrl ?: ""
                if (imageUri != null) {
                    val uploadedUrl = StorageService.uploadProfileImage(imageUri!!, uid)
                    if (uploadedUrl != null) {
                        newPhotoUrl = uploadedUrl
                    } else {
                        throw Exception("Fallo al subir la nueva foto.")
                    }
                }

                // --------- ¡¡¡VERSIÓN FINAL Y CORRECTA!!! -----------
                // 1. Creamos la copia actualizando los campos del constructor
                val updatedUser = currentUsuario!!.copy(
                    firstName = binding.editTextProfileFirstName.text.toString().trim(),
                    lastName = binding.editTextProfileLastName.text.toString().trim(),
                    birthDate = binding.editTextProfileBirthDate.text.toString().trim(),
                    phone = binding.editTextProfilePhone.text.toString().trim(),
                    profilePhotoUrl = newPhotoUrl
                )

                // 2. Asignamos 'nombreCompleto' por separado, ya que no está en el constructor copy()
                updatedUser.nombreCompleto = "${updatedUser.firstName} ${updatedUser.lastName}"
                // ----------------------------------------------------

                // Guardamos los datos actualizados en Firestore
                firestore.collection("usuarios").document(uid).set(updatedUser, SetOptions.merge()).await()

                // Guardamos (ACTUALIZAMOS) en Room. Como `updatedUser` tiene el 'id' correcto, Room actualizará la fila.
                withContext(Dispatchers.IO) {
                    db.usuarioDao().insertUser(updatedUser)
                    Log.d("ProfileActivity", "Perfil actualizado en Room para el ID: ${updatedUser.id}")
                }

                withContext(Dispatchers.Main) {
                    currentUsuario = updatedUser
                    imageUri = null
                    setEditMode(false)
                    Toast.makeText(this@ProfileActivity, "Perfil actualizado con éxito.", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ProfileActivity, "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    binding.buttonEditSaveProfile.isEnabled = true
                    binding.buttonEditSaveProfile.text = if (isEditMode) "Guardar Cambios" else "Editar Perfil"
                }
            }
        }
    }
}
