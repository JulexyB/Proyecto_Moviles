package com.example.alquilervehiculos.modelo

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Data Class para Firestore (POJO).
 * Se ha añadido el campo 'estado' para que la lógica de MisVehiculosReservadosActivity funcione.
 */
data class Reserva(
    @DocumentId
    var id: String = "",
    val vehiculoId: String = "",
    val clienteId: String = "",
    val arrendadorId: String = "",
    val fechaInicio: Date = Date(),
    val fechaFin: Date = Date(),
    val precioTotal: Double = 0.0,
    var estado: String = "PENDIENTE",


    // Opcional: Para que Firebase ponga la fecha de creación automáticamente
    @ServerTimestamp
    val fechaCreacion: Date? = null
)