package com.example.alquilervehiculos.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.alquilervehiculos.model.VehiculoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VehiculoDao {
    @Query("SELECT * FROM vehiculos")
    fun getAll(): Flow<List<VehiculoEntity>>

    @Query("SELECT * FROM vehiculos WHERE placa = :placa")
    suspend fun getVehiculoByPlaca(placa: String): VehiculoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vehiculo: VehiculoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vehiculos: List<VehiculoEntity>)

    @Delete
    suspend fun delete(vehiculo: VehiculoEntity)

    @Query("DELETE FROM vehiculos")
    suspend fun deleteAll()
}
