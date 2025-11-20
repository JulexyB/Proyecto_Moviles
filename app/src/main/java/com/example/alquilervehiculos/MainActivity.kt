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
import com.example.alquilervehiculos.HomeAdminActivity
import com.example.alquilervehiculos.CatalogActivity
import com.example.alquilervehiculos.HomeActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        firebaseAuth = FirebaseAuth.getInstance()
        db = AppDatabase.getDatabase(this)

        if (firebaseAuth.currentUser != null) {
            lifecycleScope.launch {
                redirectToCorrectActivity(firebaseAuth.currentUser!!.uid)
            }
            return
        }

        emailEditText = findViewById(R.id.editTextEmail)
        passwordEditText = findViewById(R.id.editTextPassword)
        val loginButton = findViewById<Button>(R.id.buttonLogin)
        val registerLink = findViewById<TextView>(R.id.textViewRegister)

        // Se ha eliminado toda la lógica del botón de administrador de esta pantalla

        loginButton.setOnClickListener { signInUser() }

        registerLink.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun signInUser() {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor, completa ambos campos", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
                val firebaseUser = authResult.user
                if (firebaseUser != null) {
                    redirectToCorrectActivity(firebaseUser.uid)
                }
            } catch (e: Exception) {
                Log.w("LOGIN_FIREBASE", "Fallo al iniciar sesión.", e)
                showLoginError("Credenciales inválidas o error de red.")
            }
        }
    }

    private suspend fun redirectToCorrectActivity(userId: String) {
        try {
            val userDocument = withContext(Dispatchers.IO) {
                Firebase.firestore.collection("usuarios").document(userId).get().await()
            }

            if (userDocument == null || !userDocument.exists()) {
                showLoginError("Error: El perfil del usuario no fue encontrado.")
                firebaseAuth.signOut()
                return
            }

            val rawRole = userDocument.getString("rol")
            val userRole = rawRole?.trim()?.uppercase() ?: "ARRENDATARIO"
            
            val targetActivity = when (userRole) {
                "ARRENDADOR" -> CatalogActivity::class.java
                else -> HomeActivity::class.java // Arrendatario y cualquier otro caso
            }

            Toast.makeText(this, "Sesión iniciada como $userRole", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, targetActivity)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()

        } catch (e: Exception) {
            Log.e("REDIRECT_ERROR", "Fallo al obtener el rol del usuario.", e)
            showLoginError("Error de red al verificar el perfil.")
            firebaseAuth.signOut()
        }
    }

    private fun showLoginError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
