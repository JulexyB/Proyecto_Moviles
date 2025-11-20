package com.example.alquilervehiculos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.alquilervehiculos.repository.VehiculoRepository

class VehiculoViewModelFactory(private val repository: VehiculoRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VehiculoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VehiculoViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
