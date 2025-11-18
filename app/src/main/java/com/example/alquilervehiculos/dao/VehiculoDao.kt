package com.example.alquilervehiculos.dao

import androidx.room.*
import com.example.alquilervehiculos.model.VehiculoEntity

@Dao
interface VehiculoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehiculo(vehiculo: VehiculoEntity)

    // --- MEJORA: Añadimos un insertAll para eficiencia ---
    // Esta función insertará una lista de vehículos. Si un vehículo ya existe
    // (misma Primary Key), lo reemplazará.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vehiculos: List<VehiculoEntity>)

    @Update
    suspend fun updateVehiculo(vehiculo: VehiculoEntity)

    @Delete
    suspend fun deleteVehiculo(vehiculo: VehiculoEntity)

    @Query("SELECT * FROM vehiculo_table")
    fun getAllVehiculos(): List<VehiculoEntity>

    @Query("SELECT * FROM vehiculo_table WHERE disponible = 1")
    fun getAvailableVehiculos(): List<VehiculoEntity>

    @Query("SELECT * FROM vehiculo_table WHERE id = :vehiculoId LIMIT 1")
    fun getVehiculoById(vehiculoId: Int): VehiculoEntity?

    // --- CORRECCIÓN: El parámetro ahora es un UID, no un email ---
    @Query("SELECT * FROM vehiculo_table WHERE ownerId = :ownerId")
    fun getVehiculosByOwner(ownerId: String): List<VehiculoEntity>

}
