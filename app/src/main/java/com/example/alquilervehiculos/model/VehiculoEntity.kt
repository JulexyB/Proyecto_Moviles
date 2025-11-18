package com.example.alquilervehiculos.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vehiculo_table")
data class VehiculoEntity(
    val id: Int = 0,
    @PrimaryKey
    val placa: String = "",

    val ownerId: String = "",
    val marca: String = "",
    val modelo: String = "",
    val anio: Int = 0, // Año de fabricación
    val color: String = "",
    val precioDia: Double = 0.0, // Precio del alquiler por día
    val tipo: String = "", // Sedan, SUV, Camioneta, etc.
    val disponible: Boolean = true,
    val imageUrl: String = "",
    val ciudad: String = "",
    val telefonoPropietario: String = "" // ¡NUEVO CAMPO!
)