package com.example.alquilervehiculos

import android.app.Application
import com.example.alquilervehiculos.database.AppDatabase
import com.example.alquilervehiculos.repository.UserRepository
import com.example.alquilervehiculos.repository.VehiculoRepository

/**
 * Clase Application personalizada para gestionar instancias globales,
 * como los repositorios, que deben ser compartidos por toda la app.
 */
class AlquilerVehiculosApplication : Application() {
    // Se usa 'lazy' para que la base de datos y los repositorios
    // se creen solo cuando se necesiten por primera vez.
    private val database by lazy { AppDatabase.getDatabase(this) }

    val vehiculoRepository by lazy { VehiculoRepository(database.vehiculoDao()) }
    val userRepository by lazy { UserRepository(database.userDao()) }
}
