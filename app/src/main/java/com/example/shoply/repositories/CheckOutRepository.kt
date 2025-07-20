package com.example.shoply.repositories

import android.util.Log
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import com.example.shoply.database.AppDatabase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID

// Order entity
@Entity(tableName = "orders")
data class Order(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val items: List<CartItem>,
    val address: String,
    val city: String,
    val postalCode: String,
    val total: Int,
    val timestamp: Long
)

// Order DAO
@Dao
interface OrderDao {
    // Insert a new order into the local database
    @Insert
    suspend fun insert(order: Order)

    // Retrieve all orders from the local database
    @Query("SELECT * FROM orders")
    suspend fun getAll(): List<Order>
}

class CheckoutRepository(
    private val db: AppDatabase,
    firestore: FirebaseFirestore
) {
    private val ordersCollection = firestore.collection("orders")

    // Place a new order, saving to both Firestore and Room
    suspend fun placeOrder(order: Order): Result<Unit> {
        return try {
            // Validate order data
            if (order.items.isEmpty() || order.address.isBlank() || order.city.isBlank() || order.postalCode.isBlank()) {
                Log.e("CheckoutRepository", "Order placement failed: Incomplete order data")
                return Result.failure(IllegalArgumentException("Order data is incomplete"))
            }

            // Save to Firestore
            ordersCollection.document(order.id)
                .set(order)
                .await()
            Log.d("CheckoutRepository", "Order successfully saved to Firestore: ${order.id}")

            // Save to Room
            db.orderDao().insert(order)
            Log.d("CheckoutRepository", "Order successfully saved to Room: ${order.id}")

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("CheckoutRepository", "Failed to place order: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Retrieve all orders from the local database
    suspend fun getOrders(): Result<List<Order>> {
        return try {
            val orders = db.orderDao().getAll()
            Log.d("CheckoutRepository", "Successfully retrieved ${orders.size} orders from Room")
            Result.success(orders)
        } catch (e: Exception) {
            Log.e("CheckoutRepository", "Failed to retrieve orders: ${e.message}", e)
            Result.failure(e)
        }
    }
}