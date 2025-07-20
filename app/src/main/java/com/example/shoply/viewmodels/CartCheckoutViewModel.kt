package com.example.shoply.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shoply.models.Product
import com.example.shoply.repositories.CartItem
import com.example.shoply.repositories.CartRepository
import com.example.shoply.repositories.CheckoutRepository
import com.example.shoply.repositories.Order
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class CartCheckoutUiState(
    val cartItems: List<CartItem> = emptyList(),
    val address: String = "",
    val city: String = "",
    val postalCode: String = "",
    val total: Int = 0,
    val isFormValid: Boolean = false,
    val isLoading: Boolean = false
)

class CartCheckoutViewModel(
    private val cartRepository: CartRepository,
    private val checkoutRepository: CheckoutRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CartCheckoutUiState())
    val uiState: StateFlow<CartCheckoutUiState> = _uiState

    init {
        // Initialize by refreshing cart items
        refreshCartItems()
    }

    // Refresh cart items from repository
    fun refreshCartItems() {
        viewModelScope.launch {
            try {
                val result = cartRepository.getCartItems()
                result.onSuccess { items ->
                    Log.d("CartCheckoutViewModel", "Successfully refreshed cart items: ${items.size} items")
                    _uiState.value = _uiState.value.copy(
                        cartItems = items,
                        total = items.sumOf { it.price * it.quantity }
                    )
                }.onFailure { exception ->
                    Log.e("CartCheckoutViewModel", "Failed to refresh cart: ${exception.message}", exception)
                    _uiState.value = _uiState.value.copy(cartItems = emptyList(), total = 0)
                }
            } catch (e: Exception) {
                Log.e("CartCheckoutViewModel", "Unexpected error refreshing cart: ${e.message}", e)
                _uiState.value = _uiState.value.copy(cartItems = emptyList(), total = 0)
            }
        }
    }

    // Add a product to the cart
    suspend fun addToCart(product: Product): Boolean {
        return try {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                Log.e("CartCheckoutViewModel", "Cannot add to cart: No user signed in")
                return false
            }
            val result = cartRepository.addToCart(product)
            result.onSuccess {
                Log.d("CartCheckoutViewModel", "Successfully added product to cart: ${product.name}")
                refreshCartItems()
            }.onFailure { exception ->
                Log.e("CartCheckoutViewModel", "Failed to add product to cart: ${exception.message}", exception)
            }
            result.isSuccess
        } catch (e: Exception) {
            Log.e("CartCheckoutViewModel", "Unexpected error adding to cart: ${e.message}", e)
            false
        }
    }

    // Delete an item from the cart
    suspend fun deleteFromCart(id: String): Boolean {
        return try {
            val result = cartRepository.deleteFromCart(id)
            result.onSuccess {
                Log.d("CartCheckoutViewModel", "Successfully deleted item $id from cart")
                refreshCartItems()
            }.onFailure { exception ->
                Log.e("CartCheckoutViewModel", "Failed to delete item $id from cart: ${exception.message}", exception)
            }
            result.isSuccess
        } catch (e: Exception) {
            Log.e("CartCheckoutViewModel", "Unexpected error deleting from cart: ${e.message}", e)
            false
        }
    }

    // Update the quantity of an item in the cart
    suspend fun updateQuantity(id: String, increment: Boolean): Boolean {
        return try {
            val result = cartRepository.updateQuantity(id, increment)
            result.onSuccess {
                Log.d("CartCheckoutViewModel", "Successfully updated quantity for item $id")
                refreshCartItems()
            }.onFailure { exception ->
                Log.e("CartCheckoutViewModel", "Failed to update quantity for item $id: ${exception.message}", exception)
            }
            result.isSuccess
        } catch (e: Exception) {
            Log.e("CartCheckoutViewModel", "Unexpected error updating quantity: ${e.message}", e)
            false
        }
    }

    // Clear all items from the cart
    suspend fun clearCart(): Boolean {
        return try {
            val result = cartRepository.clearCart()
            result.onSuccess {
                Log.d("CartCheckoutViewModel", "Successfully cleared cart")
                refreshCartItems()
            }.onFailure { exception ->
                Log.e("CartCheckoutViewModel", "Failed to clear cart: ${exception.message}", exception)
            }
            result.isSuccess
        } catch (e: Exception) {
            Log.e("CartCheckoutViewModel", "Unexpected error clearing cart: ${e.message}", e)
            false
        }
    }

    // Update shipping address
    fun updateAddress(address: String) {
        try {
            _uiState.value = _uiState.value.copy(address = address)
            validateForm()
            Log.d("CartCheckoutViewModel", "Updated address to: $address")
        } catch (e: Exception) {
            Log.e("CartCheckoutViewModel", "Error updating address: ${e.message}", e)
        }
    }

    // Update city
    fun updateCity(city: String) {
        try {
            _uiState.value = _uiState.value.copy(city = city)
            validateForm()
            Log.d("CartCheckoutViewModel", "Updated city to: $city")
        } catch (e: Exception) {
            Log.e("CartCheckoutViewModel", "Error updating city: ${e.message}", e)
        }
    }

    // Update postal code with length validation
    fun updatePostalCode(postalCode: String) {
        try {
            if (postalCode.length <= 6) {
                _uiState.value = _uiState.value.copy(postalCode = postalCode)
                validateForm()
                Log.d("CartCheckoutViewModel", "Updated postal code to: $postalCode")
            } else {
                Log.w("CartCheckoutViewModel", "Postal code exceeds 6 characters, ignoring update")
            }
        } catch (e: Exception) {
            Log.e("CartCheckoutViewModel", "Error updating postal code: ${e.message}", e)
        }
    }

    // Validate checkout form fields
    private fun validateForm() {
        try {
            val state = _uiState.value
            _uiState.value = state.copy(
                isFormValid = state.address.isNotBlank() &&
                        state.city.isNotBlank() &&
                        state.postalCode.isNotBlank() &&
                        state.postalCode.length == 6
            )
            Log.d("CartCheckoutViewModel", "Form validation updated: isValid=${_uiState.value.isFormValid}")
        } catch (e: Exception) {
            Log.e("CartCheckoutViewModel", "Error validating form: ${e.message}", e)
            _uiState.value = _uiState.value.copy(isFormValid = false)
        }
    }

    // Reset UI state to initial values
    private fun resetUIState() {
        try {
            _uiState.value = CartCheckoutUiState()
            Log.d("CartCheckoutViewModel", "UI state reset successfully")
        } catch (e: Exception) {
            Log.e("CartCheckoutViewModel", "Error resetting UI state: ${e.message}", e)
        }
    }

    // Place an order with current cart and shipping details
    suspend fun placeOrder(): Boolean {
        return try {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                Log.e("CartCheckoutViewModel", "Cannot place order: No user signed in")
                return false
            }
            val order = Order(
                userId = currentUser.uid,
                items = _uiState.value.cartItems,
                address = _uiState.value.address,
                city = _uiState.value.city,
                postalCode = _uiState.value.postalCode,
                total = _uiState.value.total,
                timestamp = System.currentTimeMillis()
            )
            val result = checkoutRepository.placeOrder(order)
            result.onSuccess {
                Log.d("CartCheckoutViewModel", "Order placed successfully")
                clearCart()
                resetUIState()
            }.onFailure { exception ->
                Log.e("CartCheckoutViewModel", "Failed to place order: ${exception.message}", exception)
            }
            _uiState.value = _uiState.value.copy(isLoading = false)
            result.isSuccess
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(isLoading = false)
            Log.e("CartCheckoutViewModel", "Unexpected error placing order: ${e.message}", e)
            false
        }
    }
}