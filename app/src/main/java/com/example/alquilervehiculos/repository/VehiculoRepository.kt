package com.example.alquilervehiculos.repository

import android.net.Uri
import com.example.alquilervehiculos.dao.VehiculoDao
import com.example.alquilervehiculos.model.VehiculoEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class VehiculoRepository(private val vehiculoDao: VehiculoDao) {

    private val firestoreDb = FirebaseFirestore.getInstance().collection("vehiculos")
    private val storage = FirebaseStorage.getInstance().reference.child("vehiculos_images")

    val vehiculosFromRoom: Flow<List<VehiculoEntity>> = vehiculoDao.getAll()

    suspend fun getVehiculoByPlaca(placa: String): VehiculoEntity? {
        return vehiculoDao.getVehiculoByPlaca(placa)
    }

    suspend fun update(vehiculo: VehiculoEntity) {
        firestoreDb.document(vehiculo.placa).set(vehiculo).await()
        vehiculoDao.insert(vehiculo)
    }

    suspend fun delete(vehiculo: VehiculoEntity) {
        firestoreDb.document(vehiculo.placa).delete().await()
        vehiculoDao.delete(vehiculo)
    }

    suspend fun refreshVehiculosFromFirebase() {
        try {
            val snapshot = firestoreDb.get().await()
            val vehiculos = snapshot.toObjects(VehiculoEntity::class.java)
            vehiculoDao.insertAll(vehiculos)
        } catch (e: Exception) {
            // Manejar error de conexión, etc.
        }
    }

    suspend fun registrarVehiculo(vehiculo: VehiculoEntity, imageUri: Uri) {
        if (vehiculo.id.isBlank()) {
            vehiculo.id = UUID.randomUUID().toString()
        }

        val imageRef = storage.child("${vehiculo.id}.jpg")
        val uploadTask = imageRef.putFile(imageUri).await()
        val downloadUrl = uploadTask.storage.downloadUrl.await()

        vehiculo.imageUrl = downloadUrl.toString()

        firestoreDb.document(vehiculo.id).set(vehiculo).await()

        vehiculoDao.insert(vehiculo)
    }
}
