package com.example.alquilervehiculos

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView

class GestionVehiculosActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gestion_vehiculos)

        val backArrow = findViewById<ImageView>(R.id.back_arrow)
        val cardEditar = findViewById<MaterialCardView>(R.id.cardEditarVehiculo)
        val cardEliminar = findViewById<MaterialCardView>(R.id.cardEliminarVehiculo)
        val cardVerLista = findViewById<MaterialCardView>(R.id.cardVerListaVehiculos)
        val cardActualizarEstado = findViewById<MaterialCardView>(R.id.cardActualizarEstado)

        backArrow.setOnClickListener { finish() }

        cardEditar.setOnClickListener { showSourceSelectionDialog("EDITAR") }
        cardEliminar.setOnClickListener { showSourceSelectionDialog("ELIMINAR") }
        cardVerLista.setOnClickListener { showSourceSelectionDialog("VER") }
        cardActualizarEstado.setOnClickListener { showSourceSelectionDialog("ACTUALIZAR_ESTADO") }
    }

    private fun showSourceSelectionDialog(actionType: String) {
        val options = arrayOf("Room", "Firebase")
        AlertDialog.Builder(this)
            .setTitle("Seleccionar fuente de datos")
            .setItems(options) { _, which ->
                val dataSource = if (which == 0) "ROOM" else "FIREBASE"
                launchVehiculosListActivity(actionType, dataSource)
            }
            .show()
    }

    private fun launchVehiculosListActivity(actionType: String, dataSource: String) {
        val intent = Intent(this, VehiculosListActivity::class.java).apply {
            putExtra("ACTION_TYPE", actionType)
            putExtra("DATA_SOURCE", dataSource)
        }
        startActivity(intent)
    }
}
