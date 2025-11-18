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
import com.example.alquilervehiculos.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Locale
import com.example.alquilervehiculos.model.UsuarioEntity
import com.example.alquilervehiculos.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RegisterActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var db: AppDatabase

    // Roles disponibles
    private val roles = arrayOf("Arrendatario", "Arrendador")

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
            // Ejecutamos el registro dentro de una corrutina
            lifecycleScope.launch {
                registerUser()
            }
        }
    }

    private fun setupRoleSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, roles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerUserRole.adapter = adapter
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                // Formato DD/MM/YYYY
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
        // 1. Capturar valores
        val firstName = binding.editTextFirstName.text.toString().trim()
        val lastName = binding.editTextLastName.text.toString().trim()
        val email = binding.editTextEmailRegister.text.toString().trim()
        val birthDate = binding.editTextBirthDate.text.toString().trim()
        val phone = binding.editTextPhone.text.toString().trim()
        val password = binding.editTextPasswordRegister.text.toString()

        val selectedRole = binding.spinnerUserRole.selectedItem.toString().uppercase()

        // =======================================================================
        // 🛡️ BLOQUE DE VALIDACIONES
        // =======================================================================

        // A. Campos Vacíos
        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty() || birthDate.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Por favor, completa todos los campos.", Toast.LENGTH_SHORT).show()
            return
        }

        // B. Validar Nombre y Apellido (Solo letras y espacios)
        val nameRegex = Regex("^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$")

        if (!firstName.matches(nameRegex)) {
            binding.editTextFirstName.error = "Solo letras"
            Toast.makeText(this, "El nombre solo puede contener letras.", Toast.LENGTH_SHORT).show()
            return
        }

        if (!lastName.matches(nameRegex)) {
            binding.editTextLastName.error = "Solo letras"
            Toast.makeText(this, "El apellido solo puede contener letras.", Toast.LENGTH_SHORT).show()
            return
        }

        // C. Validar Teléfono (10 dígitos numéricos)
        if (phone.length != 10 || !phone.all { it.isDigit() }) {
            binding.editTextPhone.error = "Debe tener 10 dígitos"
            Toast.makeText(this, "El teléfono debe ser de 10 números válidos.", Toast.LENGTH_SHORT).show()
            return
        }

        // D. Validar Email (Formato correcto y solo Gmail)
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.editTextEmailRegister.error = "Email inválido"
            Toast.makeText(this, "Ingresa un correo electrónico válido.", Toast.LENGTH_SHORT).show()
            return
        }

        if (!email.endsWith("@gmail.com")) {
            binding.editTextEmailRegister.error = "Solo Gmail"
            Toast.makeText(this, "El correo debe terminar en @gmail.com", Toast.LENGTH_SHORT).show()
            return
        }

        // E. Validar Edad (Mínimo 18 años)
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val birthDateDate = try {
            sdf.parse(birthDate)
        } catch (e: Exception) {
            null
        }

        if (birthDateDate != null) {
            val dobCalendar = Calendar.getInstance().apply { time = birthDateDate }
            val today = Calendar.getInstance()

            var age = today.get(Calendar.YEAR) - dobCalendar.get(Calendar.YEAR)

            if (today.get(Calendar.DAY_OF_YEAR) < dobCalendar.get(Calendar.DAY_OF_YEAR)) {
                age--
            }

            if (age < 18) {
                binding.editTextBirthDate.error = "Menor de edad"
                Toast.makeText(this, "Debes tener al menos 18 años para registrarte.", Toast.LENGTH_LONG).show()
                return
            } else {
                binding.editTextBirthDate.error = null
            }
        } else {
            binding.editTextBirthDate.error = "Fecha inválida"
            Toast.makeText(this, "Formato de fecha incorrecto.", Toast.LENGTH_SHORT).show()
            return
        }

        // F. Validar Contraseña (Longitud mínima)
        if (password.length < 6) {
            binding.editTextPasswordRegister.error = "Mínimo 6 caracteres"
            Toast.makeText(this, "La contraseña es muy corta.", Toast.LENGTH_SHORT).show()
            return
        }

        // =======================================================================
        // 🚀 PROCESO DE REGISTRO
        // =======================================================================
        try {
            // 1. Crear usuario en Firebase Auth
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
                ?: throw IllegalStateException("Error: Usuario nulo tras registro.")

            val userUid = firebaseUser.uid

            // 2. Preparar datos para Firestore
            val userData = hashMapOf(
                "uid" to userUid,
                "firstName" to firstName,
                "lastName" to lastName,
                "nombreCompleto" to "$firstName $lastName",
                "email" to email,
                "birthDate" to birthDate,
                "phone" to phone,
                "rol" to selectedRole,
                "profilePhotoUrl" to ""
            )

            // 3. Guardar en Firestore
            Firebase.firestore.collection("usuarios").document(userUid)
                .set(userData)
                .await()

            // =======================================================================
            // D. GUARDAR EN ROOM (LOCAL) - ¡ESTA ES LA PARTE QUE FALTABA!
            // =======================================================================
            val localUser = UsuarioEntity(
                uid = userUid,
                id = 0, // Autogenerado
                firstName = firstName,
                lastName = lastName,
                email = email,
                phone = phone,
                birthDate = birthDate,
                rol = selectedRole,
                profilePhotoUrl = "",
                //passwordHash = "" // No guardamos pass en local si usamos Auth
            )
            // Ejecutar inserción en hilo de fondo (IO)
            withContext(Dispatchers.IO) {
                db.usuarioDao().insertUser(localUser)
                Log.d("RegisterActivity", "Usuario guardado localmente en ROOM: $userUid")
            }

            // 4. Cerrar sesión para obligar login
            firebaseAuth.signOut()

            Toast.makeText(this, "¡Registro exitoso! Inicia sesión.", Toast.LENGTH_LONG).show()

            // 5. Ir al Login
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()

        } catch (e: Exception) {
            Log.e("REGISTRO", "Error: ${e.message}", e)
            val msg = when (e) {
                is FirebaseAuthUserCollisionException -> "Este correo ya está registrado."
                is FirebaseAuthWeakPasswordException -> "La contraseña es muy débil."
                else -> "Error al registrarse: ${e.message}"
            }
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
        }
    }
}