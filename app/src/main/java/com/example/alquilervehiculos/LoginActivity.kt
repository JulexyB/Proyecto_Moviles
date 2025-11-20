package com.example.alquilervehiculos

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.alquilervehiculos.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth

        // El chequeo de sesión activa se mantiene para comodidad
        if (auth.currentUser != null) {
            lifecycleScope.launch {
                // Se añade la comprobación del correo admin aquí también
                if (auth.currentUser?.email == "julioadmin@gmail.com") {
                    val intent = Intent(this@LoginActivity, HomeAdminActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    redirectUser(auth.currentUser!!.uid)
                }
            }
            return
        }

        binding.buttonLogin.setOnClickListener {
            val email = binding.editTextEmail.text.toString().trim()
            val password = binding.editTextPassword.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    val authResult = auth.signInWithEmailAndPassword(email, password).await()
                    val user = authResult.user
                    if (user != null) {
                        // --- SOLUCIÓN TEMPORAL ---
                        // Si el email es el del admin, se salta la comprobación de rol
                        if (email == "julioadmin@gmail.com") {
                            Log.d("LOGIN_REDIRECT", "Usuario admin por correo, redirigiendo a HomeAdminActivity")
                            val intent = Intent(this@LoginActivity, HomeAdminActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        } else {
                            // Para todos los demás usuarios, se sigue el flujo normal
                            redirectUser(user.uid)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("LOGIN_FAILURE", "Error de autenticación: ${e.message}", e)
                    Toast.makeText(this@LoginActivity, "Error de autenticación: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }

        binding.textViewRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private suspend fun redirectUser(uid: String) {
        try {
            val userDocument = Firebase.firestore.collection("usuarios").document(uid).get().await()
            if (userDocument != null && userDocument.exists()) {
                val userRole = userDocument.getString("rol")
                val intent = when (userRole) {
                    "Admin" -> Intent(this, HomeAdminActivity::class.java)
                    "Arrendador" -> Intent(this, CatalogActivity::class.java)
                    else -> Intent(this, HomeActivity::class.java)
                }
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } else {
                Log.e("LOGIN_REDIRECT", "El perfil del usuario no existe en Firestore para el UID: $uid")
                Toast.makeText(this, "Error: El perfil de este usuario está incompleto.", Toast.LENGTH_LONG).show()
                auth.signOut()
            }
        } catch (e: Exception) {
            Log.e("LOGIN_REDIRECT", "Error al obtener perfil: ${e.message}", e)
            Toast.makeText(this, "Error al obtener perfil. Verifica tu conexión.", Toast.LENGTH_LONG).show()
            auth.signOut()
        }
    }
}
