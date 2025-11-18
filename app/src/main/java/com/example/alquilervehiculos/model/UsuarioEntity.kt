package com.example.alquilervehiculos.model

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "usuario_table")
data class UsuarioEntity(
    @PrimaryKey
    var uid: String = "", // El UID de Firebase es la clave primaria
    var id: Long = 0L, // ID autogenerado para Room (lo mantenemos por si lo usas)
    var firstName: String? = null,
    var lastName: String? = null,
    var email: String? = null,
    var phone: String? = null,
    var birthDate: String? = null,
    var profilePhotoUrl: String? = null,
    var rol: String? = "Cliente",
    // ¡¡ESTE CAMPO YA NO LO USAREMOS DIRECTAMENTE DE FIRESTORE!!
    // Lo marcamos con @Ignore para que Room no intente guardarlo en una columna
    @Ignore
    var nombreCompleto: String? = null
) {
    // ESTA ES LA FUNCIÓN MÁGICA Y LA CLAVE DE LA SOLUCIÓN
    // Es un getter personalizado que construye el nombre al momento.
    fun obtenerNombreMostrable(): String {
        val fName = firstName?.trim() ?: ""
        val lName = lastName?.trim() ?: ""

        return when {
            // Si tenemos ambos, los unimos
            fName.isNotEmpty() && lName.isNotEmpty() -> "$fName $lName"
            // Si solo tenemos el primero
            fName.isNotEmpty() -> fName
            // Si solo tenemos el segundo
            lName.isNotEmpty() -> lName
            // Si no hay nada, devolvemos un texto por defecto
            else -> "Nombre no disponible"
        }
    }
}
