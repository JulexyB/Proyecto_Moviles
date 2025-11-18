package com.example.alquilervehiculos.cloud

import android.util.Log
import com.example.alquilervehiculos.model.UsuarioEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

object FirebaseService {

    private val db = FirebaseFirestore.getInstance()
    private const val COLLECTION_USERS = "usuarios" // Renombrado para más claridad

    /**
     * Guarda o actualiza los datos de un perfil de usuario en Firestore.
     * --- FUNCIÓN CORREGIDA ---
     * Ahora usa el UID del usuario como ID del documento para una consistencia total.
     */
    fun guardarUsuario(usuario: UsuarioEntity) {
        // El UID es el identificador único y perfecto para el documento.
        if (usuario.uid.isBlank()) {
            Log.e("FirebaseService", "CRÍTICO: No se puede guardar un usuario sin UID.")
            return
        }

        // NO guardamos el hash de la contraseña. FirebaseAuth se encarga de eso.
        // Creamos un mapa sin ese campo.
        val userData = hashMapOf(
            "uid" to usuario.uid,
            "firstName" to usuario.firstName,
            "lastName" to usuario.lastName,
            "email" to usuario.email,
            "birthDate" to usuario.birthDate,
            "phone" to usuario.phone,
            "profilePhotoUrl" to usuario.profilePhotoUrl,
            "rol" to usuario.rol
        )

        // Usamos el UID como ID del documento en Firestore.
        db.collection(COLLECTION_USERS).document(usuario.uid).set(userData, SetOptions.merge())
            .addOnSuccessListener {
                Log.d("FirebaseService", "Usuario '${usuario.uid}' guardado/actualizado en Firestore.")
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseService", "Error al guardar el usuario en Firestore", e)
            }
    }

    /**
     * Obtiene UN perfil de usuario específico de Firestore usando su UID.
     * --- NUEVA FUNCIÓN MEJORADA ---
     */
    fun obtenerUsuario(uid: String, callback: (UsuarioEntity?) -> Unit) {
        db.collection(COLLECTION_USERS).document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // El método toObject() de Firestore mapea automáticamente los campos si los nombres coinciden.
                    val usuario = document.toObject(UsuarioEntity::class.java)
                    callback(usuario)
                } else {
                    Log.d("FirebaseService", "No se encontró ningún usuario con UID: $uid")
                    callback(null)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseService", "Error al obtener el usuario de Firestore", exception)
                callback(null)
            }
    }

    /**
     * Obtiene todos los usuarios de Firestore.
     * --- FUNCIÓN OBTENERUSUARIOS CORREGIDA ---
     */
    fun obtenerUsuarios(callback: (List<UsuarioEntity>) -> Unit) {
        db.collection(COLLECTION_USERS).get()
            .addOnSuccessListener { result ->
                // El método toObjects() hace el mapeo por nosotros. ¡Más limpio!
                val lista = result.toObjects(UsuarioEntity::class.java)
                callback(lista)
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseService", "Error al obtener la lista de usuarios de Firestore", e)
                callback(emptyList())
            }
    }
}
