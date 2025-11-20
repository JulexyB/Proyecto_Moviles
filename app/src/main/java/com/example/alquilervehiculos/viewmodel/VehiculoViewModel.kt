package com.example.alquilervehiculos.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.alquilervehiculos.model.VehiculoEntity
import com.example.alquilervehiculos.repository.VehiculoRepository
import kotlinx.coroutines.launch

class VehiculoViewModel(private val repository: VehiculoRepository) : ViewModel() {

    val vehiculos = repository.vehiculosFromRoom.asLiveData()

    fun loadVehiculos(dataSource: String) {
        if (dataSource.equals("FIREBASE", ignoreCase = true)) {
            viewModelScope.launch {
                repository.refreshVehiculosFromFirebase()
            }
        }
    }

    suspend fun registrarVehiculo(vehiculo: VehiculoEntity, imageUri: Uri) {
        repository.registrarVehiculo(vehiculo, imageUri)
    }

    /**
     * Lanza una corutina para eliminar un vehículo a través del repositorio.
     */
    fun delete(vehiculo: VehiculoEntity) = viewModelScope.launch {
        repository.delete(vehiculo)
    }

    /**
     * Lanza una corutina para actualizar un vehículo a través del repositorio.
     */
    fun update(vehiculo: VehiculoEntity) = viewModelScope.launch {
        repository.update(vehiculo)
    }
}
