package com.example.alquilervehiculos.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.alquilervehiculos.model.ReservaEntity

@Dao
interface ReservaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarReserva(reserva: ReservaEntity)

    @Query("SELECT * FROM reservas WHERE usuarioId = :usuarioId ORDER BY fechaInicio DESC")
    suspend fun getReservasPorUsuario(usuarioId: String): List<ReservaEntity>
}
