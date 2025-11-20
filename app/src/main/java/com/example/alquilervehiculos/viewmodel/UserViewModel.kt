package com.example.alquilervehiculos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.alquilervehiculos.model.UsuarioEntity
import com.example.alquilervehiculos.repository.UserRepository
import kotlinx.coroutines.launch

/**
 * ViewModel para la gestión de datos de usuarios.
 */
class UserViewModel(private val repository: UserRepository) : ViewModel() {

    val users = repository.usersFromRoom.asLiveData()

    fun loadUsers(dataSource: String) {
        if (dataSource.equals("FIREBASE", ignoreCase = true)) {
            viewModelScope.launch {
                repository.refreshUsersFromFirebase()
            }
        }
    }

    /**
     * 🔑 SOLUCIÓN: Llama al repositorio para actualizar el estado de un usuario.
     * Se ejecuta en una corutina para no bloquear el hilo principal.
     */
    fun updateUserStatus(user: UsuarioEntity, isActive: Boolean) {
        viewModelScope.launch {
            val newStatus = if (isActive) "Activo" else "Inactivo"
            repository.updateUserStatus(user, newStatus)
        }
    }
}
