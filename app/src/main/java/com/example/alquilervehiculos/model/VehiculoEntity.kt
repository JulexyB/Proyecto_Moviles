package com.example.alquilervehiculos.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * Modelo de datos ÚNICO Y DEFINITIVO para un Vehículo.
 * Se usa para Room (base de datos local) y Firebase (nube).
 */
@IgnoreExtraProperties
@Entity(tableName = "vehiculos")
data class VehiculoEntity(
    @PrimaryKey
    var id: String = "", // Clave primaria para Room y Document ID para Firebase
    var placa: String = "",
    var marca: String = "",
    var modelo: String = "",
    var ano: Int = 0,
    var color: String = "",
    var ownerId: String = "",
    var estado: String = "Disponible",
    var imageUrl: String? = null,
    var precio: Double = 0.0,
    var ciudad: String = "",
    var tipo: String = "",
    var telefonoPropietario: String? = null
)
