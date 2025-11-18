package com.example.alquilervehiculos.dao

import androidx.room.*
import com.example.alquilervehiculos.model.UsuarioEntity

@Dao
interface UsuarioDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(usuario: UsuarioEntity) // Función de Registro

    @Query("SELECT COUNT(email) FROM usuario_table WHERE email = :email")
    suspend fun countUsersByEmail(email: String): Int // Función de Verificación de unicidad

    @Query("SELECT * FROM usuario_table")
    suspend fun getAllUsers(): List<UsuarioEntity> // Función de Listado (para la prueba)

    // Función de LOGIN: Busca por email y el hash de la contraseña
    //@Query("SELECT * FROM usuario_table WHERE email = :email AND passwordHash = :passwordHash LIMIT 1")
    //suspend fun getUserByEmailAndHash(email: String, passwordHash: String): UsuarioEntity?

    // Función auxiliar (ya no la usaremos, pero es útil)
    @Query("SELECT * FROM usuario_table WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UsuarioEntity?

    @Query("SELECT * FROM usuario_table WHERE uid = :uid LIMIT 1")
    fun getUserByUid(uid: String): UsuarioEntity?
}