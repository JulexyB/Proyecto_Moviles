package com.example.alquilervehiculos.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.alquilervehiculos.database.AppDatabase
import com.example.alquilervehiculos.model.UsuarioEntity
import com.example.alquilervehiculos.repository.UsuarioRepository
import kotlinx.coroutines.launch

/**
 * ViewModel para la pantalla de lista de usuarios (UserListActivity).
 * Se conecta al Repositorio para obtener y actualizar los datos de los usuarios.
 */
class UserListViewModel(application: Application) : AndroidViewModel(application) {

    // 🔑 SOLUCIÓN: Se inicializan el repositorio y la lista de usuarios directamente,
    // eliminando la necesidad de un bloque 'init'. Esto hace el código más conciso.
    private val repository = UsuarioRepository(AppDatabase.getDatabase(application).userDao())
    val allUsers: LiveData<List<UsuarioEntity>> = repository.allUsers

    /**
     * Lanza una corutina para actualizar un usuario en un hilo de fondo.
     */
    fun update(usuario: UsuarioEntity) = viewModelScope.launch {
        repository.update(usuario)
    }

    /**
     * 🔑 MEJORA: Se añade una función para eliminar un usuario.
     * Lanza una corutina para eliminar un usuario en un hilo de fondo.
     */
    fun delete(usuario: UsuarioEntity) = viewModelScope.launch {
        repository.delete(usuario)
    }
}
