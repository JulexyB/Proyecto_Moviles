package com.example.alquilervehiculos.repository

import android.util.Log
import com.example.alquilervehiculos.dao.UserDao
import com.example.alquilervehiculos.model.UsuarioEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

/**
 * Repositorio para gestionar los datos de los usuarios.
 */
class UserRepository(private val userDao: UserDao) {

    val usersFromRoom: Flow<List<UsuarioEntity>> = userDao.getAllUsersFlow()

    /**
     * Refresca la base de datos local con los datos más recientes de Firebase.
     */
    suspend fun refreshUsersFromFirebase() {
        val firestore = FirebaseFirestore.getInstance()
        try {
            val snapshot = firestore.collection("users").get().await()
            val users = snapshot.toObjects(UsuarioEntity::class.java)
            userDao.insertAll(users)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error al refrescar usuarios desde Firebase", e)
        }
    }

    /**
     * 🔑 SOLUCIÓN: Actualiza el estado de un usuario en Firebase y luego en Room.
     */
    suspend fun updateUserStatus(user: UsuarioEntity, newStatus: String) {
        val firestore = FirebaseFirestore.getInstance()
        try {
            // Paso 1: Actualizar el estado en Firebase.
            firestore.collection("users").document(user.uid).update("estado", newStatus).await()

            // Paso 2: Actualizar el estado en la base de datos local (Room).
            val updatedUser = user.copy(estado = newStatus)
            userDao.update(updatedUser) // Asume que el DAO tiene un método @Update
        } catch (e: Exception) {
            Log.e("UserRepository", "Error al actualizar el estado del usuario", e)
        }
    }
}
