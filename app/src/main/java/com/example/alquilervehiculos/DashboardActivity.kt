package com.example.alquilervehiculos

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.alquilervehiculos.databinding.ActivityDashboardBinding
import com.example.alquilervehiculos.viewmodel.DashboardViewModel

/**
 * Activity para mostrar el dashboard de estadísticas.
 */
class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private val viewModel: DashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Observar el LiveData del total de vehículos para actualizar la UI.
        viewModel.totalVehicles.observe(this) { total ->
            binding.tvTotalVehicles.text = total
        }

        // 🔑 SOLUCIÓN: Se observa el LiveData del total de usuarios para actualizar la UI.
        viewModel.totalUsers.observe(this) { total ->
            binding.tvTotalUsers.text = total
        }
        
        // TODO: En el futuro, se puede observar el LiveData de ingresos.
        // viewModel.recentIncome.observe(this) { binding.tvRecentIncome.text = it }
    }
}
