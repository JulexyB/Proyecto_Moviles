package com.example.alquilervehiculos.cloud

import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

object StorageService {

    private val storage = FirebaseStorage.getInstance()
    private val storageRef = storage.reference

    /**
     * Sube la imagen de un vehículo a Firebase Storage.
     * --- FUNCIÓN CORREGIDA ---
     * @param fileUri URI local de la imagen.
     * @param ownerId UID del propietario del vehículo.
     * @param placa Placa del vehículo.
     * @return URL de descarga de la imagen o null si falla.
     */
    suspend fun uploadVehicleImage(fileUri: Uri, ownerId: String, placa: String): String? {
        // La ruta ahora es: "vehiculos/{ownerId}/{placa}.jpg"
        // Esto coincide con las reglas de seguridad de Firebase.
        val imageFileName = "$placa.jpg"
        val vehicleImageRef = storageRef.child("vehiculos/$ownerId/$imageFileName")

        return try {
            val uploadTask = vehicleImageRef.putFile(fileUri).await()
            Log.d("StorageService", "Imagen de vehículo subida: ${uploadTask.bytesTransferred} bytes")

            val downloadUrl = vehicleImageRef.downloadUrl.await().toString()
            Log.d("StorageService", "URL de vehículo obtenida: $downloadUrl")
            downloadUrl
        } catch (e: Exception) {
            Log.e("StorageService", "Error al subir la imagen del vehículo: ${e.message}", e)
            null
        }
    }

    /**
     * Sube la imagen de perfil de un usuario a Firebase Storage.
     * --- NUEVA FUNCIÓN ---
     * @param uri URI local de la imagen de perfil.
     * @param userUid UID del usuario.
     * @return URL de descarga de la imagen o null si falla.
     */
    suspend fun uploadProfileImage(uri: Uri, userUid: String): String? {
        return try {
            // La ruta será: "profile_pictures/{userUid}.jpg"
            // Esto coincide con las nuevas reglas de seguridad que definimos.
            val storageRef = storage.reference.child("profile_pictures/$userUid.jpg")
            val uploadTask = storageRef.putFile(uri).await()
            val downloadUrl = uploadTask.storage.downloadUrl.await()
            downloadUrl.toString()
        } catch (e: Exception) {
            Log.e("StorageService", "Error al subir foto de perfil", e)
            null
        }
    }
}
