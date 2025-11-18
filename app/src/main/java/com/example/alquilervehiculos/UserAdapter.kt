package com.example.alquilervehiculos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.alquilervehiculos.R // Asegúrate de tener esta importación
import com.example.alquilervehiculos.model.UsuarioEntity

class UserAdapter(private var userList: List<UsuarioEntity>) :
    RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Asegúrate de que estos IDs coincidan con los de tu layout item_user.xml
        val nameTextView: TextView = itemView.findViewById(R.id.textViewUserName)
        val emailTextView: TextView = itemView.findViewById(R.id.textViewUserEmail)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]
        holder.nameTextView.text = "Nombre: ${user.firstName} ${user.lastName}"
        holder.emailTextView.text = "Email: ${user.email}"
    }

    override fun getItemCount(): Int = userList.size

    /**
     * Función crucial para actualizar la lista de datos desde la Activity.
     * Esto funciona tanto para la lista de ROOM como para la de Firestore.
     */
    fun updateData(newUsers: List<UsuarioEntity>) {
        // Asigna la nueva lista.
        // Nota: Al usar 'var userList' en la declaración de la clase, esto es válido.
        userList = newUsers

        // Notifica al RecyclerView que los datos han cambiado y debe redibujar.
        notifyDataSetChanged()
    }
}