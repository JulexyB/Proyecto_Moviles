package com.example.alquilervehiculos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.alquilervehiculos.R
import com.example.alquilervehiculos.model.UsuarioEntity

class UserAdapter(private var userList: List<UsuarioEntity>) :
    RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.text_user_name)
        val emailTextView: TextView = itemView.findViewById(R.id.text_user_email)
        val roleTextView: TextView = itemView.findViewById(R.id.text_user_role)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]
        // 🔑 SOLUCIÓN: Se usa la propiedad 'nombreMostrable' en lugar de la función inexistente.
        holder.nameTextView.text = user.nombreMostrable
        holder.emailTextView.text = user.email
        holder.roleTextView.text = "Rol: ${user.rol}"
    }

    override fun getItemCount(): Int = userList.size

    fun updateData(newUsers: List<UsuarioEntity>) {
        userList = newUsers
        notifyDataSetChanged()
    }
}
