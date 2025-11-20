package com.example.alquilervehiculos

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class HomeAdminActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_admin)

        val cardManageVehicles = findViewById<CardView>(R.id.card_manage_vehicles)
        val cardManageUsers = findViewById<CardView>(R.id.card_manage_users)
        // 🔑 SOLUCIÓN: Se enlaza la nueva tarjeta del dashboard.
        val cardDashboard = findViewById<CardView>(R.id.card_dashboard)

        cardManageVehicles.setOnClickListener {
            showDataSourceDialog(VehiculosListActivity::class.java)
        }

        cardManageUsers.setOnClickListener {
            showDataSourceDialog(UserListActivity::class.java)
        }

        // 🔑 SOLUCIÓN: Se añade el listener para abrir la DashboardActivity.
        cardDashboard.setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java)
            startActivity(intent)
        }
    }

    /**
     * Muestra un diálogo para que el usuario elija la fuente de datos (Firebase o Room)
     * y luego lanza la actividad correspondiente.
     */
    private fun showDataSourceDialog(activityClass: Class<*>) {
        val options = arrayOf("Firebase (Nube)", "Room (Local)")

        AlertDialog.Builder(this)
            .setTitle("Seleccionar fuente de datos")
            .setItems(options) { dialog, which ->
                val dataSource = if (which == 0) "FIREBASE" else "ROOM"
                val intent = Intent(this, activityClass).apply {
                    putExtra("DATA_SOURCE", dataSource)
                }
                startActivity(intent)
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}
