package com.example.shoply.viewmodels.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.shoply.repositories.AuthRepository
import com.example.shoply.repositories.CartRepository
import com.example.shoply.repositories.CheckoutRepository
import com.example.shoply.repositories.HomeRepository
import com.example.shoply.viewmodels.AuthViewModel
import com.example.shoply.viewmodels.CartCheckoutViewModel
import com.example.shoply.viewmodels.HomeViewModel
import com.example.shoply.viewmodels.ProfileViewModel
import com.example.shoply.viewmodels.OrderHistoryViewModel

class AppViewModelFactory(
    private val authRepository: AuthRepository? = null,
    private val homeRepository: HomeRepository? = null,
    private val cartRepository: CartRepository? = null,
    private val checkoutRepository: CheckoutRepository? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AuthViewModel::class.java) -> {
                requireNotNull(authRepository)
                AuthViewModel(authRepository) as T
            }
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> {
                requireNotNull(homeRepository)
                requireNotNull(cartRepository)
                HomeViewModel(homeRepository, cartRepository) as T
            }
            modelClass.isAssignableFrom(CartCheckoutViewModel::class.java) -> {
                requireNotNull(cartRepository)
                requireNotNull(checkoutRepository)
                CartCheckoutViewModel(cartRepository, checkoutRepository) as T
            }
            modelClass.isAssignableFrom(ProfileViewModel::class.java) -> {
                requireNotNull(authRepository)
                ProfileViewModel(authRepository) as T
            }
            modelClass.isAssignableFrom(OrderHistoryViewModel::class.java) -> {
                requireNotNull(checkoutRepository)
                OrderHistoryViewModel(checkoutRepository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
