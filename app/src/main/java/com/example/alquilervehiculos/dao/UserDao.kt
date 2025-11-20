package com.example.alquilervehiculos.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.alquilervehiculos.model.UsuarioEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) para la entidad Usuario.
 * 🔑 SOLUCIÓN: Se añade el método 'insertAll' que faltaba.
 */
@Dao
interface UserDao {

    /**
     * Obtiene todos los usuarios y emite una nueva lista cada vez que los datos cambian.
     */
    @Query("SELECT * FROM usuario_table ORDER BY firstName ASC")
    fun getAllUsersFlow(): Flow<List<UsuarioEntity>>

    /**
     * Obtiene un solo usuario por su UID de Firebase.
     */
    @Query("SELECT * FROM usuario_table WHERE uid = :uid LIMIT 1")
    suspend fun getUserByUid(uid: String): UsuarioEntity?

    /**
     * Inserta un usuario. Si ya existe, lo reemplaza.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(usuario: UsuarioEntity)

    /**
     * 🔑 SOLUCIÓN: Inserta una lista de usuarios. Si ya existen, los reemplaza.
     * Necesario para la sincronización desde Firebase.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(users: List<UsuarioEntity>)

    /**
     * Actualiza un usuario existente.
     */
    @Update
    suspend fun update(usuario: UsuarioEntity)

    /**
     * Elimina un usuario.
     */
    @Delete
    suspend fun delete(usuario: UsuarioEntity)
}
