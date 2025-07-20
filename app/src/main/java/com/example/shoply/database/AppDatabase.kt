package com.example.shoply.database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.shoply.repositories.CartDao
import com.example.shoply.repositories.CartItem
import com.example.shoply.repositories.CartItemListConverter
import com.example.shoply.repositories.Order
import com.example.shoply.repositories.OrderDao

// Room database configuration for CartItem and Order entities
@Database(entities = [CartItem::class, Order::class], version = 1)
@TypeConverters(CartItemListConverter::class)
abstract class AppDatabase : RoomDatabase() {
    // Provide access to Cart DAO
    abstract fun cartDao(): CartDao

    // Provide access to Order DAO
    abstract fun orderDao(): OrderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Get or create a singleton instance of the database
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                try {
                    Log.d("AppDatabase", "Attempting to create or retrieve database instance")
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "shoply_database"
                    ).build()
                    INSTANCE = instance
                    Log.d("AppDatabase", "Database instance created successfully")
                    instance
                } catch (e: Exception) {
                    Log.e("AppDatabase", "Error creating database instance: ${e.message}", e)
                    throw RuntimeException("Failed to initialize database: ${e.message}", e)
                }
            }
        }
    }
}