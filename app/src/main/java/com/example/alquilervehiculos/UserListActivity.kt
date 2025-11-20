package com.example.alquilervehiculos

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.alquilervehiculos.adapter.UserAdapter
import com.example.alquilervehiculos.databinding.ActivityUserListBinding
import com.example.alquilervehiculos.repository.UserRepository
import com.example.alquilervehiculos.viewmodel.UserViewModel

class UserListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserListBinding
    private val userViewModel: UserViewModel by viewModels {
        UserViewModelFactory((application as AlquilerVehiculosApplication).userRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = UserAdapter { user, isChecked ->
            userViewModel.updateUserStatus(user, isChecked)
        }

        val recyclerView = findViewById<RecyclerView>(R.id.user_list)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        userViewModel.users.observe(this) { users ->
            users?.let { adapter.submitList(it) }
        }

        val dataSource = intent.getStringExtra("DATA_SOURCE") ?: "FIREBASE"
        userViewModel.loadUsers(dataSource)
    }
}

class UserViewModelFactory(private val repository: UserRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UserViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
