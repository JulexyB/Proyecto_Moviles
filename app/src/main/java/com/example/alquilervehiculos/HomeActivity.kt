package com.example.alquilervehiculos

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.alquilervehiculos.databinding.ActivityHomeBinding
import com.google.firebase.auth.FirebaseAuth

/**
 * Esta es la pantalla principal para el rol "arrendatario".
 * Muestra las opciones generales como ver el catálogo, el perfil y sus propias reservas.
 * Ya no contiene lógica para diferenciar roles.
 */
class HomeActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Infla el layout 'activity_home.xml' que ya está limpio.
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializa Firebase
        firebaseAuth = FirebaseAuth.getInstance()

        // Verifica que haya un usuario con sesión activa
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            goToLoginActivity()
            return // Detiene la ejecución si no hay usuario
        }

        // Personaliza la bienvenida con el email del usuario
        binding.textViewWelcome.text = "¡Bienvenido/a!"
        binding.textViewUserEmail.text = currentUser.email

        // Configura los listeners para los botones que SÍ existen en el layout
        setupListeners()

        // Configura el comportamiento del botón de retroceso
        setupOnBackPressed()
    }

    /**
     * Configura los listeners para las vistas de esta actividad.
     * Solo se hace referencia a las vistas que existen en activity_home.xml.
     */
    private fun setupListeners() {
        // A. Listener para la tarjeta de Perfil
        binding.cardViewProfile.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        // B. Listener para la tarjeta de Catálogo
        binding.cardViewCatalog.setOnClickListener {
            val intent = Intent(this, CatalogActivity::class.java)
            startActivity(intent)
        }

        // C. Listener para la tarjeta de "Mis Reservas"
        binding.cardViewReservas.setOnClickListener {
            val intent = Intent(this, MisReservasActivity::class.java)
            startActivity(intent)
        }

        // D. Listener para el botón de Cerrar Sesión
        binding.buttonLogout.setOnClickListener {
            firebaseAuth.signOut()
            Toast.makeText(this, "Sesión cerrada.", Toast.LENGTH_SHORT).show()
            goToLoginActivity()
        }
    }

    /**
     * Evita que el usuario pueda volver a la pantalla de Login con el botón de retroceso.
     */
    private fun setupOnBackPressed() {
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Toast.makeText(this@HomeActivity, "Usa el botón 'Cerrar Sesión'", Toast.LENGTH_SHORT).show()
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    /**
     * Navega a la pantalla de Login y limpia el historial de actividades.
     */
    private fun goToLoginActivity() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish() // Cierra HomeActivity para que no se pueda volver a ella
    }
}
