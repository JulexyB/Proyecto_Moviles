package com.example.alquilervehiculos

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.alquilervehiculos.databinding.ActivityHomeBinding
import com.google.firebase.auth.FirebaseAuth

class HomeActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            goToLoginActivity()
            return
        }

        binding.textViewUserEmail.text = currentUser.email

        setupListeners()
        setupOnBackPressed()
    }

    private fun setupListeners() {
        binding.cardViewProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        binding.cardViewCatalog.setOnClickListener {
            startActivity(Intent(this, CatalogActivity::class.java))
        }

        binding.cardViewReservas.setOnClickListener {
            startActivity(Intent(this, MisReservasActivity::class.java))
        }

        binding.buttonLogout.setOnClickListener {
            firebaseAuth.signOut()
            Toast.makeText(this, "Sesión cerrada.", Toast.LENGTH_SHORT).show()
            goToLoginActivity()
        }

        // <<-- LÓGICA DEL BOTÓN DE PRUEBA AÑADIDA -->>
        binding.buttonAdminTest.setOnClickListener {
            Toast.makeText(this, "Abriendo panel de administrador...", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, HomeAdminActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupOnBackPressed() {
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Toast.makeText(this@HomeActivity, "Usa el botón 'Cerrar Sesión'", Toast.LENGTH_SHORT).show()
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    private fun goToLoginActivity() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
