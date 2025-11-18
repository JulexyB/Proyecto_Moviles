package com.example.alquilervehiculos

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.alquilervehiculos.database.AppDatabase
import com.example.alquilervehiculos.model.ReservaEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

class VehiculoDetailActivity : AppCompatActivity() {

    private lateinit var placa: String
    private var precioPorDia: Double = 0.0
    private var arrendadorId: String? = null
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var roomDb: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vehiculo_detail)

        roomDb = AppDatabase.getDatabase(this)

        val imageView: ImageView = findViewById(R.id.imageViewDetail)
        val marcaModeloView: TextView = findViewById(R.id.textViewMarcaModeloDetail)
        val anioColorView: TextView = findViewById(R.id.textViewAnioColorDetail)
        val precioView: TextView = findViewById(R.id.textViewPrecioDetail)
        val placaView: TextView = findViewById(R.id.textViewPlacaDetail)
        val estadoView: TextView = findViewById(R.id.textViewEstadoDetail)
        val reservarButton: Button = findViewById(R.id.buttonReservar)
        val telefonoView: TextView = findViewById(R.id.textViewContacto)

        val marca = intent.getStringExtra("MARCA") ?: "N/A"
        val modelo = intent.getStringExtra("MODELO") ?: "N/A"
        val anio = intent.getIntExtra("ANIO", 0)
        val color = intent.getStringExtra("COLOR") ?: "N/A"
        val imageUrl = intent.getStringExtra("IMAGE_URL") ?: ""
        placa = intent.getStringExtra("PLACA") ?: return
        arrendadorId = intent.getStringExtra("OWNER_ID")
        precioPorDia = intent.getDoubleExtra("PRECIO", 0.0)
        val disponible = intent.getBooleanExtra("DISPONIBLE", false)
        val telefono = intent.getStringExtra("TELEFONO_PROPIETARIO") ?: "No disponible"

        marcaModeloView.text = "$marca $modelo"
        anioColorView.text = "$anio - $color"
        precioView.text = String.format("$%.2f/día", precioPorDia)
        placaView.text = placa
        telefonoView.text = "Contacto: $telefono"

        imageView.load(imageUrl) {
            crossfade(true)
            placeholder(R.drawable.ic_car_placeholder)
            error(R.drawable.ic_car_placeholder)
        }

        if (disponible) {
            estadoView.text = "Disponible"
            estadoView.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
            reservarButton.isEnabled = true
        } else {
            estadoView.text = "No Disponible"
            estadoView.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
            reservarButton.isEnabled = false
        }

        reservarButton.setOnClickListener {
            if (arrendadorId.isNullOrEmpty()) {
                Toast.makeText(this, "Error: No se pudo identificar al propietario.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            mostrarSelectorDeFechas()
        }
    }

    private fun mostrarSelectorDeFechas() {
        val calendario = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, anio, mes, dia ->
                val fechaInicioSeleccionada = Calendar.getInstance().apply { set(anio, mes, dia) }
                DatePickerDialog(
                    this,
                    { _, anioFin, mesFin, diaFin ->
                        val fechaFinSeleccionada = Calendar.getInstance().apply { set(anioFin, mesFin, diaFin) }
                        crearReserva(fechaInicioSeleccionada.time, fechaFinSeleccionada.time)
                    }, anio, mes, dia
                ).apply {
                    datePicker.minDate = fechaInicioSeleccionada.timeInMillis
                    setTitle("Selecciona Fecha de Devolución")
                    show()
                }
            },
            calendario.get(Calendar.YEAR), calendario.get(Calendar.MONTH), calendario.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = System.currentTimeMillis() - 1000
            setTitle("Selecciona Fecha de Recogida")
            show()
        }
    }

    private fun crearReserva(fechaInicio: Date, fechaFin: Date) {
        val usuarioActual = auth.currentUser ?: run {
            Toast.makeText(this, "Error: Debes iniciar sesión para reservar.", Toast.LENGTH_LONG).show()
            return
        }

        val diferenciaMillis = fechaFin.time - fechaInicio.time
        val dias = TimeUnit.MILLISECONDS.toDays(diferenciaMillis).toInt().coerceAtLeast(1)
        val precioTotal = dias * precioPorDia

        // =======================================================================
        // 🔑 CORRECCIÓN CRÍTICA PARA FIRESTORE
        // =======================================================================
        val reservaFirestore = hashMapOf(
            "vehiculoId" to placa,
            "clienteId" to usuarioActual.uid,
            "arrendadorId" to arrendadorId,
            "fechaInicio" to fechaInicio,
            "fechaFin" to fechaFin,
            "precioTotal" to precioTotal,

            // ¡ESTE CAMPO FALTABA Y ES LA CAUSA DE TU ERROR!
            "estado" to "PENDIENTE"
        )
        // =======================================================================

        db.collection("reservas")
            .add(reservaFirestore)
            .addOnSuccessListener {
                Toast.makeText(this, "¡Reserva exitosa!", Toast.LENGTH_SHORT).show()
                Log.d("RESERVA_FIRESTORE", "Reserva creada con éxito en Firestore.")

                // Guardado en Room (esto ya lo tenías corregido)
                lifecycleScope.launch {
                    val nuevaReservaEntity = ReservaEntity(
                        vehiculoPlaca = placa,
                        usuarioId = usuarioActual.uid,
                        arrendadorId = arrendadorId!!,
                        fechaInicio = fechaInicio,
                        fechaFin = fechaFin,
                        precioTotal = precioTotal,
                        estado = "PENDIENTE" // Correcto en Room
                    )
                    roomDb.reservaDao().insertarReserva(nuevaReservaEntity)
                }

                actualizarEstadoVehiculo()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al crear la reserva: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("RESERVA_FIRESTORE", "Error en Firestore", e)
            }
    }

    private fun actualizarEstadoVehiculo() {
        db.collection("vehiculos").document(placa)
            .update("disponible", false)
            .addOnSuccessListener {
                val intent = Intent(this, CatalogActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                Log.e("VEHICULO_UPDATE", "Error al actualizar la disponibilidad del vehículo", e)
            }
    }
}