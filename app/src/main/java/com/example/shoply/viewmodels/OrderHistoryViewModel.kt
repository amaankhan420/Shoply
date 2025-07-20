package com.example.shoply.viewmodels

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shoply.repositories.CheckoutRepository
import com.example.shoply.repositories.Order
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class OrderHistoryViewModel(private val checkoutRepository: CheckoutRepository) : ViewModel() {
    var orders by mutableStateOf<List<Order>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set

    init {
        // Load orders when ViewModel is initialized
        loadOrders()
    }

    // Refresh the order history
    fun refreshOrders() {
        Log.d("OrderHistoryViewModel", "Refreshing order history")
        loadOrders()
    }

    // Load orders for the current user
    private fun loadOrders() {
        viewModelScope.launch {
            try {
                loading = true
                Log.d("OrderHistoryViewModel", "Starting to load orders")

                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser != null) {
                    val result = checkoutRepository.getOrders()
                    result.onSuccess { orderList ->
                        orders = orderList.filter { it.userId == currentUser.uid }
                        Log.d("OrderHistoryViewModel", "Successfully loaded ${orders.size} orders for user ${currentUser.uid}")
                    }.onFailure { exception ->
                        Log.e("OrderHistoryViewModel", "Failed to load orders: ${exception.message}", exception)
                        orders = emptyList()
                    }
                } else {
                    Log.w("OrderHistoryViewModel", "No user signed in, setting empty order list")
                    orders = emptyList()
                }
            } catch (e: Exception) {
                Log.e("OrderHistoryViewModel", "Unexpected error loading orders: ${e.message}", e)
                orders = emptyList()
            } finally {
                loading = false
                Log.d("OrderHistoryViewModel", "Finished loading orders, loading state set to false")
            }
        }
    }
}