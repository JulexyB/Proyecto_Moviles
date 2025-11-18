package com.example.alquilervehiculos

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView // Importación necesaria
import com.example.alquilervehiculos.UserAdapter // <<-- RUTA CORREGIDA
//import com.example.alquilervehiculos.adapter.UserAdapter // Asume esta ruta
import com.example.alquilervehiculos.database.AppDatabase
import com.example.alquilervehiculos.cloud.FirebaseService
import com.example.alquilervehiculos.model.UsuarioEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UserListActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var buttonListarRoom: Button
    private lateinit var buttonListarFirestore: Button

    // NUEVAS REFERENCIAS PARA MOSTRAR DATOS
    private lateinit var recyclerViewUsers: RecyclerView
    private lateinit var userAdapter: UserAdapter // Tu adaptador

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_list)

        db = AppDatabase.getDatabase(this)

        // 1. Obtener referencias y configurar el RecyclerView
        buttonListarRoom = findViewById(R.id.buttonListarRoom)
        buttonListarFirestore = findViewById(R.id.buttonListarFirestore)

        recyclerViewUsers = findViewById(R.id.recyclerViewUsers)
        userAdapter = UserAdapter(mutableListOf()) // Inicializa el adaptador con una lista vacía

        recyclerViewUsers.layoutManager = LinearLayoutManager(this)
        recyclerViewUsers.adapter = userAdapter

        // 2. Lógica para listar ROOM
        buttonListarRoom.setOnClickListener {
            listarUsuariosRoom()
        }

        // 3. Lógica para listar FIRESTORE
        buttonListarFirestore.setOnClickListener {
            listarUsuariosFirestore()
        }
    }

    // =======================================================================
    // Funciones de Listado
    // =======================================================================

    private fun listarUsuariosRoom() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val usuariosRoom = db.usuarioDao().getAllUsers()

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@UserListActivity, "ROOM: ${usuariosRoom.size} usuarios.", Toast.LENGTH_SHORT).show()

                    // PASO CLAVE: ACTUALIZAR EL ADAPTADOR
                    actualizarLista(usuariosRoom)
                }
            } catch (e: Exception) {
                Log.e("DIAGNOSIS_ROOM", "Error al leer la BD de ROOM: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@UserListActivity, "ERROR ROOM: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun listarUsuariosFirestore() {
        // La función de Firebase usa callbacks
        FirebaseService.obtenerUsuarios { usuariosFirestore ->
            lifecycleScope.launch(Dispatchers.Main) {
                try {
                    Toast.makeText(this@UserListActivity, "FIRESTORE: ${usuariosFirestore.size} perfiles.", Toast.LENGTH_SHORT).show()

                    // PASO CLAVE: ACTUALIZAR EL ADAPTADOR
                    actualizarLista(usuariosFirestore)

                } catch (e: Exception) {
                    Log.e("DIAGNOSIS_FIRESTORE", "Error al procesar la lista de Firestore: ${e.message}", e)
                    Toast.makeText(this@UserListActivity, "ERROR FIRESTORE: Fallo al procesar datos.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Función central para pasar la lista al adaptador
    private fun actualizarLista(usuarios: List<UsuarioEntity>) {
        // Asumiendo que tu adaptador tiene un método para actualizar la lista (ej: updateData o submitList)
        userAdapter.updateData(usuarios)
        // También puedes usar: userAdapter.notifyDataSetChanged() si tu adaptador no tiene un método dedicado.
    }
}