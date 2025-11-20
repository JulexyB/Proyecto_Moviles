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
            result.data?.data?.let {
                imageUri = it
                Glide.with(this).load(it).circleCrop().into(binding.imageViewProfilePicture)
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

    private fun loadUserProfile(uid: String) {
        lifecycleScope.launch {
            try {
                // 1. Obtenemos datos de Firestore
                val userFromFirestore = firestore.collection("usuarios").document(uid).get().await()
                    .toObject<UsuarioEntity>()
                    ?: throw IllegalStateException("No se encontró el perfil en la nube.")

                // 2. 🔑 SOLUCIÓN: Usar el método correcto 'getUserByUid()'
                val localId = withContext(Dispatchers.IO) { db.userDao().getUserByUid(uid)?.id ?: 0L }

                // 3. Combinamos datos de Firestore con el ID local
                val userToSync = userFromFirestore.copy(id = localId)

                // 4. Sincronizamos con la base de datos local
                withContext(Dispatchers.IO) {
                    db.userDao().insert(userToSync)
                }

                // 5. Actualizamos el estado y la UI
                currentUsuario = userToSync
                populateUI(currentUsuario!!)

            } catch (e: Exception) {
                Log.w("ProfileActivity", "Fallo al cargar de Firestore, intentando desde Room...", e)
                // 🔑 SOLUCIÓN: Usar el método correcto 'getUserByUid()' en el bloque catch también
                val userFromRoom = withContext(Dispatchers.IO) { db.userDao().getUserByUid(uid) }
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

        if (!user.profilePhotoUrl.isNullOrEmpty()) {
            Glide.with(this).load(user.profilePhotoUrl).circleCrop().placeholder(R.drawable.ic_profile).into(binding.imageViewProfilePicture)
        } else {
            Glide.with(this).load(R.drawable.ic_profile).circleCrop().into(binding.imageViewProfilePicture)
        }
    }

    private fun saveUserProfile(uid: String) {
        if (binding.editTextProfileFirstName.text.toString().isBlank() || binding.editTextProfilePhone.text.toString().isBlank()) {
            Toast.makeText(this, "El nombre y el teléfono son obligatorios.", Toast.LENGTH_SHORT).show()
            return
        }

        setLoadingState(true)

        lifecycleScope.launch {
            try {
                var newPhotoUrl = currentUsuario?.profilePhotoUrl
                if (imageUri != null) {
                    newPhotoUrl = StorageService.uploadProfileImage(imageUri!!, uid)
                        ?: throw Exception("Fallo al subir la nueva foto.")
                }

                val updatedData = mapOf(
                    "firstName" to binding.editTextProfileFirstName.text.toString().trim(),
                    "lastName" to binding.editTextProfileLastName.text.toString().trim(),
                    "birthDate" to binding.editTextProfileBirthDate.text.toString().trim(),
                    "phone" to binding.editTextProfilePhone.text.toString().trim(),
                    "profilePhotoUrl" to newPhotoUrl
                )

                firestore.collection("usuarios").document(uid).set(updatedData, SetOptions.merge()).await()

                val updatedUserEntity = currentUsuario!!.copy(
                    firstName = updatedData["firstName"] as String?,
                    lastName = updatedData["lastName"] as String?,
                    birthDate = updatedData["birthDate"] as String?,
                    phone = updatedData["phone"] as String?,
                    profilePhotoUrl = updatedData["profilePhotoUrl"] as String?
                )

                // Sincronizamos el usuario actualizado con la base de datos local
                withContext(Dispatchers.IO) {
                    db.userDao().insert(updatedUserEntity)
                }

                withContext(Dispatchers.Main) {
                    currentUsuario = updatedUserEntity
                    imageUri = null
                    setEditMode(false)
                    populateUI(currentUsuario!!)
                    Toast.makeText(this@ProfileActivity, "Perfil actualizado con éxito.", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ProfileActivity, "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    setLoadingState(false)
                }
            }
        }
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.buttonEditSaveProfile.isEnabled = !isLoading
        binding.buttonEditSaveProfile.text = if (isLoading) "Guardando..." else "Guardar Cambios"
    }
}
