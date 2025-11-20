package com.example.alquilervehiculos.dao

import androidx.room.*
import com.example.alquilervehiculos.model.UsuarioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {

    // Inserta un usuario. Si ya existe, lo reemplaza.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(usuario: UsuarioEntity)

    // Actualiza un usuario existente.
    @Update
    suspend fun update(usuario: UsuarioEntity)

    // Obtiene todos los usuarios y emite una nueva lista cada vez que los datos cambian.
    @Query("SELECT * FROM usuario_table ORDER BY firstName ASC")
    fun getAllUsersFlow(): Flow<List<UsuarioEntity>>

    // Obtiene un usuario por su UID de forma síncrona (usar con precaución fuera del hilo principal).
    @Query("SELECT * FROM usuario_table WHERE uid = :uid LIMIT 1")
    fun getUserByUid(uid: String): UsuarioEntity?

}
