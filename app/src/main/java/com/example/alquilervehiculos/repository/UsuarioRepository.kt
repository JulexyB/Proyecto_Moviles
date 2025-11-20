package com.example.alquilervehiculos.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.example.alquilervehiculos.dao.UserDao
import com.example.alquilervehiculos.model.UsuarioEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repositorio para gestionar los datos de los usuarios.
 */
class UsuarioRepository(private val userDao: UserDao) {

    // 🔑 SOLUCIÓN: Se corrige la llamada para usar el método 'getAllUsersFlow()', que es el que existe en el DAO.
    val allUsers: LiveData<List<UsuarioEntity>> = userDao.getAllUsersFlow().asLiveData()

    /**
     * Inserta un nuevo usuario en un hilo de fondo.
     */
    suspend fun insert(usuario: UsuarioEntity) {
        withContext(Dispatchers.IO) {
            userDao.insert(usuario)
        }
    }

    /**
     * Actualiza un usuario existente en un hilo de fondo.
     */
    suspend fun update(usuario: UsuarioEntity) {
        withContext(Dispatchers.IO) {
            userDao.update(usuario)
        }
    }

    /**
     * Elimina un usuario existente en un hilo de fondo.
     */
    suspend fun delete(usuario: UsuarioEntity) {
        withContext(Dispatchers.IO) {
            userDao.delete(usuario)
        }
    }

    /**
     * Obtiene un usuario por su UID.
     */
    suspend fun getUserByUid(uid: String): UsuarioEntity? {
        return withContext(Dispatchers.IO) {
            userDao.getUserByUid(uid)
        }
    }
}
