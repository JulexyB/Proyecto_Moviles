package com.example.alquilervehiculos.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.util.Date

// TypeConverter para que Room pueda manejar el tipo Date
class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}

@Entity(tableName = "reservas")
@TypeConverters(Converters::class)
data class ReservaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val vehiculoPlaca: String,
    val usuarioId: String,
    val arrendadorId: String,
    val fechaInicio: Date = Date(),
    val fechaFin: Date = Date(),
    val precioTotal: Double = 0.0,
    val estado: String
)
