package com.example.alquilervehiculos.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.alquilervehiculos.R
import com.example.alquilervehiculos.databinding.UserListItemBinding
import com.example.alquilervehiculos.model.UsuarioEntity

/**
 * Adaptador para el RecyclerView que muestra la lista de usuarios.
 * 🔑 SOLUCIÓN: Se añade un callback para notificar cuando el estado de un usuario cambia.
 */
class UserAdapter(
    private val onStatusChanged: (UsuarioEntity, Boolean) -> Unit
) : ListAdapter<UsuarioEntity, UserAdapter.UserViewHolder>(UserDiffCallback()) {

    /**
     * ViewHolder que contiene la vista de un solo item de la lista.
     */
    inner class UserViewHolder(private val binding: UserListItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: UsuarioEntity) {
            binding.tvUserName.text = user.nombreMostrable
            binding.tvUserEmail.text = user.email

            // Configurar el switch
            binding.switchUserStatus.setOnCheckedChangeListener(null) // Evitar que el listener se dispare solo
            binding.switchUserStatus.isChecked = user.estado.equals("Activo", ignoreCase = true)
            binding.switchUserStatus.setOnCheckedChangeListener { _, isChecked ->
                onStatusChanged(user, isChecked)
            }

            // Cargar la imagen de perfil con Glide
            Glide.with(binding.root.context)
                .load(user.profilePhotoUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .into(binding.ivUserPhoto)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = UserListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class UserDiffCallback : DiffUtil.ItemCallback<UsuarioEntity>() {
    override fun areItemsTheSame(oldItem: UsuarioEntity, newItem: UsuarioEntity): Boolean {
        return oldItem.uid == newItem.uid
    }

    override fun areContentsTheSame(oldItem: UsuarioEntity, newItem: UsuarioEntity): Boolean {
        return oldItem == newItem
    }
}
