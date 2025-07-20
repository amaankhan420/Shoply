package com.example.shoply.repositories

import android.util.Log
import androidx.room.*
import com.example.shoply.database.AppDatabase
import com.example.shoply.models.Product
import com.google.firebase.auth.FirebaseAuth

// Cart Entity
@Entity(tableName = "cart_items")
data class CartItem(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val price: Int,
    val image: String = "",
    val quantity: Int = 1
)

// Cart DAO
@Dao
interface CartDao {
    // Insert a new cart item, ignore if it already exists
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(cartItem: CartItem)

    // Increment quantity of an existing cart item
    @Query("UPDATE cart_items SET quantity = quantity + 1 WHERE id = :id AND userId = :userId")
    suspend fun incrementQuantity(id: String, userId: String)

    // Decrement quantity of an existing cart item
    @Query("UPDATE cart_items SET quantity = quantity - 1 WHERE id = :id AND userId = :userId")
    suspend fun decrementQuantity(id: String, userId: String)

    // Delete a specific cart item
    @Query("DELETE FROM cart_items WHERE id = :id AND userId = :userId")
    suspend fun delete(id: String, userId: String)

    // Get all cart items for a user
    @Query("SELECT * FROM cart_items WHERE userId = :userId")
    suspend fun getAll(userId: String): List<CartItem>

    // Clear all cart items for a user
    @Query("DELETE FROM cart_items WHERE userId = :userId")
    suspend fun clearAll(userId: String)
}

class CartRepository(private val db: AppDatabase) {
    private val currentUserId: String?
        get() = try {
            FirebaseAuth.getInstance().currentUser?.uid.also {
                Log.d("CartRepository", "Got user ID: $it")
            }
        } catch (e: Exception) {
            Log.e("CartRepository", "Failed to get user ID: ${e.message}")
            null
        }

    // Add a product to the cart
    suspend fun addToCart(product: Product): Result<Unit> {
        return try {
            val userId: String =
                currentUserId ?: return Result.failure<Unit>(Exception("No user signed in")).also {
                    Log.e("CartRepository", "Add to cart failed: No user")
                }
            val cartItem = CartItem(
                id = product.id,
                userId = userId,
                name = product.name,
                price = product.price,
                image = product.image
            )
            Log.d("CartRepository", "Adding item: ${cartItem.name}")
            val existingItems: List<CartItem> = db.cartDao().getAll(userId)
            val existingItem: CartItem? = existingItems.find { it.id == product.id }
            if (existingItem != null) {
                db.cartDao().incrementQuantity(product.id, userId)
                Log.d("CartRepository", "Increased quantity: ${existingItem.name}")
            } else {
                db.cartDao().insert(cartItem)
                Log.d("CartRepository", "Added new item: ${cartItem.name}")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("CartRepository", "Failed to add item")
            Result.failure(e)
        }
    }

    // Delete an item from the cart
    suspend fun deleteFromCart(id: String): Result<Unit> {
        return try {
            val userId: String =
                currentUserId ?: return Result.failure<Unit>(Exception("No user signed in")).also {
                    Log.e("CartRepository", "Delete failed: No user")
                }
            db.cartDao().delete(id, userId)
            Log.d("CartRepository", "Deleted item: $id")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("CartRepository", "Failed to delete item")
            Result.failure(e)
        }
    }

    // Update the quantity of a cart item
    suspend fun updateQuantity(id: String, increment: Boolean): Result<Unit> {
        return try {
            val userId: String =
                currentUserId ?: return Result.failure<Unit>(Exception("No user signed in")).also {
                    Log.e("CartRepository", "Update quantity failed: No user")
                }
            if (increment) {
                db.cartDao().incrementQuantity(id, userId)
                Log.d("CartRepository", "Increased quantity: $id")
            } else {
                db.cartDao().decrementQuantity(id, userId)
                val items: List<CartItem> = db.cartDao().getAll(userId)
                val item: CartItem? = items.find { it.id == id }
                if (item?.quantity == 0) {
                    db.cartDao().delete(id, userId)
                    Log.d("CartRepository", "Removed item $id: Zero quantity")
                } else {
                    Log.d("CartRepository", "Decreased quantity: $id")
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("CartRepository", "Failed to update quantity")
            Result.failure(e)
        }
    }

    // Retrieve all cart items for the current user
    suspend fun getCartItems(): Result<List<CartItem>> {
        return try {
            val userId: String = currentUserId
                ?: return Result.failure<List<CartItem>>(Exception("No user signed in")).also {
                    Log.e("CartRepository", "Get items failed: No user")
                }
            val items: List<CartItem> = db.cartDao().getAll(userId)
            Log.d("CartRepository", "Got ${items.size} items")
            Result.success(items)
        } catch (e: Exception) {
            Log.e("CartRepository", "Failed to get items")
            Result.failure(e)
        }
    }

    // Clear all cart items for the current user
    suspend fun clearCart(): Result<Unit> {
        return try {
            val userId: String =
                currentUserId ?: return Result.failure<Unit>(Exception("No user signed in")).also {
                    Log.e("CartRepository", "Clear cart failed: No user")
                }
            db.cartDao().clearAll(userId)
            Log.d("CartRepository", "Cleared cart")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("CartRepository", "Failed to clear cart")
            Result.failure(e)
        }
    }
}