package com.example.alquilervehiculos.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.alquilervehiculos.database.AppDatabase
import com.example.alquilervehiculos.model.VehiculoEntity
import com.example.alquilervehiculos.repository.VehiculoRepository
import kotlinx.coroutines.launch

/**
 * ViewModel para la pantalla de edición de un vehículo (VehiculoActivity en modo edición).
 * Se conecta al Repositorio para obtener y actualizar los datos de un vehículo específico.
 */
class EditVehiculoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: VehiculoRepository

    // LiveData para mantener el vehículo que se está editando.
    private val _vehiculo = MutableLiveData<VehiculoEntity?>()
    val vehiculo: LiveData<VehiculoEntity?> = _vehiculo

    init {
        val vehiculoDao = AppDatabase.getDatabase(application).vehiculoDao()
        repository = VehiculoRepository(vehiculoDao)
    }

    /**
     * Obtiene un vehículo por su placa y lo publica en el LiveData.
     */
    fun getVehiculoByPlaca(placa: String) = viewModelScope.launch {
        _vehiculo.value = repository.getVehiculoByPlaca(placa)
    }

    /**
     * Lanza una corutina para actualizar el vehículo en un hilo de fondo.
     */
    fun update(vehiculo: VehiculoEntity) = viewModelScope.launch {
        repository.update(vehiculo)
    }
}
