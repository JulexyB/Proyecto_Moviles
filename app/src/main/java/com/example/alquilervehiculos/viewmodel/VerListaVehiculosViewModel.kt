package com.example.alquilervehiculos.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.alquilervehiculos.database.AppDatabase
import com.example.alquilervehiculos.model.VehiculoEntity
import com.example.alquilervehiculos.repository.VehiculoRepository
import kotlinx.coroutines.launch

/**
 * ViewModel para la pantalla que muestra la lista de vehículos (VerListaVehiculosActivity).
 * Se conecta al Repositorio para obtener y manipular los datos de los vehículos de forma reactiva.
 */
class VerListaVehiculosViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: VehiculoRepository

    // Expone la lista de vehículos desde la base de datos local como LiveData.
    // Se actualizará automáticamente cuando los datos en Room cambien.
    val allVehiculos: LiveData<List<VehiculoEntity>>

    init {
        val vehiculoDao = AppDatabase.getDatabase(application).vehiculoDao()
        repository = VehiculoRepository(vehiculoDao)
        allVehiculos = repository.vehiculosFromRoom.asLiveData()

        // Refresca los datos desde Firebase al iniciar el ViewModel.
        refreshVehiculosFromServer()
    }

    /**
     * Lanza una corutina para eliminar un vehículo. La UI se actualizará automáticamente.
     */
    fun delete(vehiculo: VehiculoEntity) = viewModelScope.launch {
        repository.delete(vehiculo)
    }

    /**
     * Lanza una corutina para actualizar un vehículo. La UI se actualizará automáticamente.
     */
    fun update(vehiculo: VehiculoEntity) = viewModelScope.launch {
        repository.update(vehiculo)
    }

    /**
     * Fuerza la sincronización de los datos desde Firebase al repositorio local.
     */
    fun refreshVehiculosFromServer() = viewModelScope.launch {
        repository.refreshVehiculosFromFirebase()
    }
}
