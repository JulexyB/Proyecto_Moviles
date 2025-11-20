package com.example.alquilervehiculos.model

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "usuario_table")
data class UsuarioEntity(
    @PrimaryKey
    var uid: String = "",
    var id: Long = 0L,
    var firstName: String? = null,
    var lastName: String? = null,
    var email: String? = null,
    var phone: String? = null,
    var birthDate: String? = null,
    var profilePhotoUrl: String? = null,
    var rol: String? = "Cliente",
    var estado: String = "Activo",
) {
    // 🔑 SOLUCIÓN: Se añade una propiedad calculada para obtener el nombre completo.
    // Room ignorará esta propiedad gracias a @Ignore.
    @get:Ignore
    val nombreMostrable: String
        get() {
            val fName = firstName?.trim() ?: ""
            val lName = lastName?.trim() ?: ""

            return when {
                fName.isNotEmpty() && lName.isNotEmpty() -> "$fName $lName"
                fName.isNotEmpty() -> fName
                lName.isNotEmpty() -> lName
                else -> "Nombre no disponible"
            }
        }
}
