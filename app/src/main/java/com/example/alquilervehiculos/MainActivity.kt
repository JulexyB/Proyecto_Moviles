package com.example.alquilervehiculos

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.alquilervehiculos.database.AppDatabase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.example.alquilervehiculos.model.UsuarioEntity // <-- Usaremos esta entidad
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var db: AppDatabase // Para obtener datos locales si fallan los de Firebase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        firebaseAuth = FirebaseAuth.getInstance()
        db = AppDatabase.getDatabase(this) // Inicializa ROOM

        // --- Verificación de Sesión Activa (Router Inicial) ---
        if (firebaseAuth.currentUser != null) {
            lifecycleScope.launch {
                redirectToCorrectActivity(firebaseAuth.currentUser!!.uid)
            }
            return
        }

        // --- Inicialización de la UI ---
        emailEditText = findViewById(R.id.editTextEmail)
        passwordEditText = findViewById(R.id.editTextPassword)
        val loginButton = findViewById<Button>(R.id.buttonLogin)
        val registerLink = findViewById<TextView>(R.id.textViewRegister)

        // Asumiendo que el botón de prueba para listar usuarios sigue ahí:
        // val viewUsersButton = findViewById<Button>(R.id.buttonViewUsers)

        // --- Lógica del botón de Login ---
        loginButton.setOnClickListener {
            signInUser()
        }

        // --- Redirección a Registro ---
        registerLink.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    // Función de inicio de sesión que llama al router
    private fun signInUser() {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor, completa ambos campos", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                // 1. Autenticar con Firebase Auth
                val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
                val firebaseUser = authResult.user

                if (firebaseUser != null) {
                    // 2. Éxito: Redirigir basado en el rol
                    redirectToCorrectActivity(firebaseUser.uid)
                } else {
                    showLoginError("Error inesperado al iniciar sesión.")
                }

            } catch (e: Exception) {
                Log.w("LOGIN_FIREBASE", "Fallo al iniciar sesión.", e)
                showLoginError("Credenciales inválidas o el usuario no existe.")
            }
        }
    }

    /**
     * Función que obtiene el rol del usuario y decide la Activity de destino.
     */
    private suspend fun redirectToCorrectActivity(userId: String) {
        try {
            // 1. Obtenemos el documento del usuario desde Firestore
            val userDocument = withContext(Dispatchers.IO) {
                Firebase.firestore.collection("usuarios")
                    .document(userId)
                    .get()
                    .await()
            }

            // 2. Mapeamos el rol o usamos "ARRENDATARIO" por defecto/seguridad
            val userRole = userDocument.getString("rol")?.uppercase() ?: "ARRENDATARIO"
            val userEmail = userDocument.getString("email") ?: ""

            // 3. Decidimos a qué actividad ir
            val targetActivity = when (userRole) {
                "ADMINISTRADOR", "ARRENDADOR" -> OwnerActivity::class.java
                else -> HomeActivity::class.java // ARRENDATARIO (Cliente)
            }

            Toast.makeText(this, "Sesión iniciada como $userRole", Toast.LENGTH_SHORT).show()

            // 4. Navegamos y limpiamos la pila
            val intent = Intent(this, targetActivity).apply {
                // Pasamos el email y UID (crucial para la OwnerActivity)
                putExtra("USER_EMAIL", userEmail)
                putExtra("USER_UID", userId)
            }
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()

        } catch (e: Exception) {
            Log.e("REDIRECT_ERROR", "Fallo al obtener el rol del usuario.", e)
            showLoginError("Error: No se pudo verificar el perfil.")
            // Si falla la obtención del rol, cerramos la sesión por seguridad
            firebaseAuth.signOut()
        }
    }

    private fun showLoginError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
