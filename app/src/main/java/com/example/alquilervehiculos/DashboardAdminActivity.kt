package com.example.alquilervehiculos

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class DashboardAdminActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard_admin)

        val backArrow = findViewById<ImageView>(R.id.back_arrow_dashboard)
        backArrow.setOnClickListener { finish() }

        // Por ahora, solo mostramos datos de ejemplo.
        // Más adelante, aquí iría la lógica para cargar los datos reales.
        findViewById<TextView>(R.id.tvTotalVehiculos).text = "-"
        findViewById<TextView>(R.id.tvVehiculosDisponibles).text = "-"
        findViewById<TextView>(R.id.tvReservasActivas).text = "-"
        findViewById<TextView>(R.id.tvReservasPendientes).text = "-"

        Toast.makeText(this, "Cargando datos del dashboard...", Toast.LENGTH_SHORT).show()
    }
}
