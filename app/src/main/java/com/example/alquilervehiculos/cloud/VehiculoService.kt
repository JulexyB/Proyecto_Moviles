package com.example.alquilervehiculos.cloud

import android.util.Log
import com.example.alquilervehiculos.model.VehiculoEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage // 🔑 IMPORTANTE: Para la eliminación de Storage
import kotlinx.coroutines.tasks.await // 🔑 Necesario para usar await()

object VehiculoService {

    private val db = FirebaseFirestore.getInstance()
    private const val COLLECTION_NAME = "vehiculos"

    /**
     * Guarda o actualiza un vehículo en Firestore.
     */
    fun guardarVehiculo(vehiculo: VehiculoEntity) {
        if (vehiculo.placa.isBlank()) {
            Log.e("VehiculoService", "No se puede guardar un vehículo sin placa.")
            return
        }

        val data = hashMapOf(
            "ownerId" to vehiculo.ownerId,
            "marca" to vehiculo.marca,
            "modelo" to vehiculo.modelo,
            "anio" to vehiculo.anio,
            "color" to vehiculo.color,
            "precioDia" to vehiculo.precioDia,
            "tipo" to vehiculo.tipo,
            "disponible" to vehiculo.disponible,
            "imageUrl" to vehiculo.imageUrl,
            "ciudad" to vehiculo.ciudad,
            "telefonoPropietario" to vehiculo.telefonoPropietario
        )

        db.collection(COLLECTION_NAME).document(vehiculo.placa)
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                Log.d("VehiculoService", "Vehículo ${vehiculo.placa} guardado/actualizado.")
            }
            .addOnFailureListener { e ->
                Log.e("VehiculoService", "Error al guardar vehículo ${vehiculo.placa}", e)
            }
    }

    /**
     * Obtiene todos los vehículos disponibles de Firestore.
     */
    fun obtenerVehiculos(callback: (List<VehiculoEntity>) -> Unit) {
        db.collection(COLLECTION_NAME)
            .whereEqualTo("disponible", true)
            .get()
            .addOnSuccessListener { result ->
                val lista = result.documents.mapNotNull { doc ->
                    val placa = doc.id
                    VehiculoEntity(
                        id = 0,
                        ownerId = doc.getString("ownerId") ?: "",
                        marca = doc.getString("marca") ?: "",
                        modelo = doc.getString("modelo") ?: "",
                        anio = doc.getLong("anio")?.toInt() ?: 0,
                        color = doc.getString("color") ?: "",
                        placa = placa,
                        precioDia = doc.getDouble("precioDia") ?: 0.0,
                        tipo = doc.getString("tipo") ?: "",
                        disponible = doc.getBoolean("disponible") ?: true,
                        imageUrl = doc.getString("imageUrl") ?: "",
                        ciudad = doc.getString("ciudad") ?: "No especificada",
                        telefonoPropietario = doc.getString("telefonoPropietario") ?: "No disponible"
                    )
                }
                callback(lista)
            }
            .addOnFailureListener { e ->
                Log.e("VehiculoService", "Error al obtener vehículos de Firestore", e)
                callback(emptyList())
            }
    }

    /**
     * Obtiene los vehículos de un propietario específico.
     */
    fun obtenerVehiculosPorOwnerId(ownerId: String, callback: (List<VehiculoEntity>) -> Unit) {
        db.collection(COLLECTION_NAME)
            .whereEqualTo("ownerId", ownerId)
            .get()
            .addOnSuccessListener { result ->
                val lista = result.documents.mapNotNull { doc ->
                    val placa = doc.id
                    VehiculoEntity(
                        id = 0,
                        ownerId = doc.getString("ownerId") ?: "",
                        marca = doc.getString("marca") ?: "",
                        modelo = doc.getString("modelo") ?: "",
                        anio = doc.getLong("anio")?.toInt() ?: 0,
                        color = doc.getString("color") ?: "",
                        placa = placa,
                        precioDia = doc.getDouble("precioDia") ?: 0.0,
                        tipo = doc.getString("tipo") ?: "",
                        disponible = doc.getBoolean("disponible") ?: true,
                        imageUrl = doc.getString("imageUrl") ?: "",
                        ciudad = doc.getString("ciudad") ?: "No especificada",
                        telefonoPropietario = doc.getString("telefonoPropietario") ?: "No disponible"
                    )
                }
                callback(lista)
            }
            .addOnFailureListener { e ->
                Log.e("VehiculoService", "Error al obtener vehículos del owner $ownerId de Firestore", e)
                callback(emptyList())
            }
    }

    /**
     * 🔑 FUNCIÓN CRÍTICA: Elimina un vehículo de Firestore y su imagen de Storage.
     */
    suspend fun eliminarVehiculo(placa: String, ownerId: String) {
        // La función DEBE ser 'suspend' para usar await()

        if (placa.isBlank() || ownerId.isBlank()) {
            Log.e("VehiculoService", "Error: No se puede eliminar sin Placa o OwnerId.")
            throw IllegalArgumentException("Faltan parámetros críticos para la eliminación.")
        }

        // 1. Eliminar documento de Firestore
        try {
            db.collection(COLLECTION_NAME).document(placa).delete().await()
            Log.d("VehiculoService", "Documento de vehículo $placa eliminado de Firestore.")
        } catch (e: Exception) {
            Log.e("VehiculoService", "Error crítico al eliminar documento de Firestore: ${e.message}")
            // Relanza el error para que la Activity lo capture
            throw e
        }

        // 2. Eliminar la imagen de Storage
        val imageRef = FirebaseStorage.getInstance().reference
            .child("vehiculos/$ownerId/$placa.jpg")

        try {
            imageRef.delete().await()
            Log.d("VehiculoService", "Imagen del vehículo $placa eliminada de Storage.")
        } catch (e: Exception) {
            // Manejamos el error de forma suave aquí, ya que el archivo puede que no exista.
            Log.w("VehiculoService", "Advertencia: No se pudo eliminar la imagen de Storage (puede que no existiera): ${e.message}")
        }
    }
}