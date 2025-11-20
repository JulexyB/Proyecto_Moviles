package com.example.alquilervehiculos.modelo

import com.example.alquilervehiculos.model.UsuarioEntity
import com.example.alquilervehiculos.model.VehiculoEntity

/**
 * Esta clase especial contiene TODA la información que una fila de la
 * lista de reservas necesita para mostrarse, sin necesidad de hacer más consultas.
 * Es la clave para solucionar el problema.
 */
data class  ReservaViewModel(
    val reserva: Reserva,
    val cliente: UsuarioEntity?,   // Puede ser nulo si el cliente fue borrado
    val vehiculo: VehiculoEntity?  // Puede ser nulo si el vehículo fue borrado
)
