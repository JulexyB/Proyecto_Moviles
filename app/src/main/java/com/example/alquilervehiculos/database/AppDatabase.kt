package com.example.alquilervehiculos.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.Room
import android.content.Context
import androidx.room.TypeConverters // IMPORTAR ESTA ANOTACIÓN
import com.example.alquilervehiculos.dao.UsuarioDao
import com.example.alquilervehiculos.dao.VehiculoDao
import com.example.alquilervehiculos.dao.ReservaDao
import com.example.alquilervehiculos.model.UsuarioEntity
import com.example.alquilervehiculos.model.VehiculoEntity
import com.example.alquilervehiculos.model.ReservaEntity
import com.example.alquilervehiculos.model.Converters // <<-- IMPORTAR TU CLASE CONVERTERS

// 🔑 CORRECCIÓN: Añadir la anotación @TypeConverters
@TypeConverters(Converters::class)
@Database(entities = [UsuarioEntity::class, VehiculoEntity::class, ReservaEntity::class], version = 6, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao
    abstract fun vehiculoDao(): VehiculoDao
    abstract fun reservaDao(): ReservaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "alquilervehiculos_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}