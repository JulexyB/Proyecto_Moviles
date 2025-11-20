package com.example.alquilervehiculos

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.alquilervehiculos.database.AppDatabase
import com.example.alquilervehiculos.databinding.ActivityRegisterBinding
import com.example.alquilervehiculos.model.UsuarioEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

class RegisterActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = Firebase.auth
        db = AppDatabase.getDatabase(this)

        setupRoleSpinner()

        binding.editTextBirthDate.setOnClickListener {
            showDatePickerDialog()
        }

        binding.buttonRegister.setOnClickListener {
            lifecycleScope.launch {
                registerUser()
            }
        }
    }

    private fun setupRoleSpinner() {
        ArrayAdapter.createFromResource(
            this,
            R.array.user_roles_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerUserRole.adapter = adapter
        }
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear)
                binding.editTextBirthDate.setText(selectedDate)
            },
            year, month, day
        ).apply {
            datePicker.maxDate = System.currentTimeMillis()
            show()
        }
    }

    private suspend fun registerUser() {
        val firstName = binding.editTextFirstName.text.toString().trim()
        val lastName = binding.editTextLastName.text.toString().trim()
        val email = binding.editTextEmailRegister.text.toString().trim()
        val birthDate = binding.editTextBirthDate.text.toString().trim()
        val phone = binding.editTextPhone.text.toString().trim()
        val password = binding.editTextPasswordRegister.text.toString()
        
        // <<-- SOLUCIÓN DEFINITIVA: Estandarizar el rol antes de guardarlo -->>
        val selectedRoleText = binding.spinnerUserRole.selectedItem.toString()
        val finalRole = when (selectedRoleText) {
            "Administrador" -> "ADMINISTRADOR"
            "Arrendador" -> "ARRENDADOR"
            else -> "ARRENDATARIO"
        }

        // Validaciones (sin cambios)
        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty() || birthDate.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Por favor, completa todos los campos.", Toast.LENGTH_SHORT).show()
            return
        }
        val nameRegex = Regex("^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$")
        if (!firstName.matches(nameRegex) || !lastName.matches(nameRegex)) {
            Toast.makeText(this, "El nombre y apellido solo pueden contener letras.", Toast.LENGTH_SHORT).show()
            return
        }
        if (phone.length != 10 || !phone.all { it.isDigit() }) {
            Toast.makeText(this, "El teléfono debe ser de 10 números válidos.", Toast.LENGTH_SHORT).show()
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches() || !email.endsWith("@gmail.com")) {
            Toast.makeText(this, "Ingresa un correo Gmail válido.", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val dob = Calendar.getInstance().apply { time = sdf.parse(birthDate)!! }
            val today = Calendar.getInstance()
            var age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR)
            if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) age--
            if (age < 18) {
                Toast.makeText(this, "Debes tener al menos 18 años para registrarte.", Toast.LENGTH_LONG).show()
                return
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Formato de fecha incorrecto.", Toast.LENGTH_SHORT).show()
            return
        }
        if (password.length < 6) {
            Toast.makeText(this, "La contraseña es muy corta (mínimo 6 caracteres).", Toast.LENGTH_SHORT).show()
            return
        }

        // Proceso de Registro
        try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw IllegalStateException("Error: Usuario nulo tras registro.")
            val userUid = firebaseUser.uid

            val userData = hashMapOf(
                "uid" to userUid,
                "firstName" to firstName,
                "lastName" to lastName,
                "email" to email,
                "birthDate" to birthDate,
                "phone" to phone,
                "rol" to finalRole, // Se guarda el rol estandarizado "ADMINISTRADOR"
                "estado" to "Activo",
                "profilePhotoUrl" to ""
            )

            Firebase.firestore.collection("usuarios").document(userUid).set(userData).await()

            val localUser = UsuarioEntity(
                uid = userUid,
                id = 0,
                firstName = firstName,
                lastName = lastName,
                email = email,
                phone = phone,
                birthDate = birthDate,
                rol = finalRole, // Se guarda el rol estandarizado
                estado = "Activo",
                profilePhotoUrl = ""
            )

            withContext(Dispatchers.IO) {
                // 🔑 CORRECCIÓN: Se usa el nombre correcto del método: 'userDao' en lugar de 'usuarioDao'.
                db.userDao().insert(localUser)
            }

            firebaseAuth.signOut()
            Toast.makeText(this, "¡Registro exitoso! Inicia sesión.", Toast.LENGTH_LONG).show()

            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()

        } catch (e: Exception) {
            val msg = when (e) {
                is FirebaseAuthUserCollisionException -> "Este correo ya está registrado."
                is FirebaseAuthWeakPasswordException -> "La contraseña es muy débil."
                else -> "Error al registrarse: ${e.message}"
            }
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
        }
    }
}
