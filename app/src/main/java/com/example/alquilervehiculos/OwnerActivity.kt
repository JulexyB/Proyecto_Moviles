package com.example.alquilervehiculos

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.alquilervehiculos.databinding.ActivityOwnerBinding
import com.google.firebase.auth.FirebaseAuth

class OwnerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOwnerBinding
    private lateinit var ownerEmail: String
    private lateinit var ownerUid: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOwnerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val currentUser = FirebaseAuth.getInstance().currentUser

        // 1. Obtener datos del Intent (Pasados desde Login/Home Router)
        ownerEmail = intent.getStringExtra("USER_EMAIL") ?: ""
        ownerUid = intent.getStringExtra("USER_UID") ?: "" // Usamos UID como clave de filtro

        if (currentUser == null || ownerEmail.isBlank()) {
            Toast.makeText(this, "Error: Sesión de Arrendador no válida.", Toast.LENGTH_LONG).show()
            goToLoginActivity()
            return
        }

        binding.textViewOwnerTitle.text = "Panel de Arrendador"
        binding.textViewOwnerWelcome.text = "¡Hola, ${currentUser.email ?: "Arrendador"}!"

        setupListeners()
    }

    private fun setupListeners() {
        val currentUser = FirebaseAuth.getInstance().currentUser // Obtenemos la referencia

        // Aseguramos que el usuario esté activo antes de configurar los listeners críticos
        if (currentUser == null) return

        // A. Botón: Subir Nuevo Vehículo
        binding.buttonAddVehicle.setOnClickListener {
            val intent = Intent(this, VehiculoActivity::class.java).apply {
                // Pasamos el UID como clave de dueño para el nuevo vehículo
                putExtra("OWNER_UID", currentUser.uid)
            }
            startActivity(intent)
        }

        // B. Botón: Ver Mis Vehículos (GESTIÓN) - 🔑 CLAVE DEL FILTRADO
        binding.buttonViewMyVehicles.setOnClickListener {
            val intent = Intent(this, CatalogActivity::class.java).apply {
                // 🔑 CLAVE: Usamos el UID para filtrar y activar el MODO GESTIÓN
                putExtra("FILTER_BY_OWNER_UID", currentUser.uid)
            }
            startActivity(intent)
        }

        // C. Botón: Ver Reservas de mis Vehículos
        binding.buttonViewMyVehicleReservations.setOnClickListener {
            // MisVehiculosReservadosActivity obtendrá el UID directamente de FirebaseAuth.currentUser
            startActivity(Intent(this, MisVehiculosReservadosActivity::class.java))
        }

        // D. Botón: Perfil
        binding.buttonOwnerProfile.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java).apply {
                // Pasamos el UID para que ProfileActivity cargue el perfil correcto
                putExtra("USER_UID", currentUser.uid)
            }
            startActivity(intent)
        }

        // E. Botón: Cerrar Sesión
        binding.buttonLogoutOwner.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            Toast.makeText(this, "Sesión cerrada.", Toast.LENGTH_SHORT).show()
            goToLoginActivity()
        }
    }

    /**
     * Función reutilizable para navegar a LoginActivity y limpiar la pila de actividades.
     */
    private fun goToLoginActivity() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish() // Cierra OwnerActivity para que no se pueda volver atrás
    }
}