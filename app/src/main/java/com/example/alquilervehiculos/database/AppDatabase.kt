package com.example.alquilervehiculos.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.alquilervehiculos.dao.ReservaDao
import com.example.alquilervehiculos.dao.UserDao
import com.example.alquilervehiculos.dao.VehiculoDao
import com.example.alquilervehiculos.model.ReservaEntity
import com.example.alquilervehiculos.model.UsuarioEntity
import com.example.alquilervehiculos.model.VehiculoEntity

// 🔑 SOLUCIÓN: Se elimina la anotación @TypeConverters que hacía referencia a una clase inexistente.
@Database(entities = [UsuarioEntity::class, VehiculoEntity::class, ReservaEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun reservaDao(): ReservaDao
    abstract fun vehiculoDao(): VehiculoDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "alquiler_vehiculos_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
