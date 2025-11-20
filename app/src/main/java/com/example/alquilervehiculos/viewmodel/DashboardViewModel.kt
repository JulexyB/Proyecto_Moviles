package com.example.alquilervehiculos.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * ViewModel para el Dashboard. Se encarga de obtener las estadísticas
 * desde las fuentes de datos (Firebase, en este caso).
 */
class DashboardViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    // LiveData para el total de vehículos
    private val _totalVehicles = MutableLiveData<String>()
    val totalVehicles: LiveData<String> = _totalVehicles

    // 🔑 SOLUCIÓN: Se añade el LiveData para el total de usuarios.
    private val _totalUsers = MutableLiveData<String>()
    val totalUsers: LiveData<String> = _totalUsers

    init {
        // Cargar todos los datos cuando el ViewModel se inicializa
        fetchDashboardData()
    }

    private fun fetchDashboardData() {
        // Contar vehículos
        viewModelScope.launch {
            try {
                val vehiclesSnapshot = db.collection("vehiculos").get().await()
                _totalVehicles.value = vehiclesSnapshot.size().toString()
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Error al contar vehículos", e)
                _totalVehicles.value = "Error"
            }
        }

        // 🔑 SOLUCIÓN: Contar usuarios
        viewModelScope.launch {
            try {
                // Nota: Usamos la colección "usuarios" que es la que parece correcta en el proyecto.
                val usersSnapshot = db.collection("usuarios").get().await()
                _totalUsers.value = usersSnapshot.size().toString()
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Error al contar usuarios", e)
                _totalUsers.value = "Error"
            }
        }
    }
}
