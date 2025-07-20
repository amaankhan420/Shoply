package com.example.shoply.viewmodels

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.shoply.repositories.AuthRepository

class ProfileViewModel(private val authRepository: AuthRepository) : ViewModel() {
    var user by mutableStateOf(authRepository.getCurrentUser())
        private set

    // Refresh user data from repository
    fun refreshUser() {
        try {
            Log.d("ProfileViewModel", "Refreshing user data")
            user = authRepository.getCurrentUser()
            Log.d("ProfileViewModel", "User data refreshed: ${user?.displayName ?: "No user"}")
        } catch (e: Exception) {
            Log.e("ProfileViewModel", "Error refreshing user data: ${e.message}", e)
            user = null
        }
    }

    // Update user's display name
    suspend fun updateDisplayName(newName: String): Result<String> {
        return try {
            Log.d("ProfileViewModel", "Attempting to update display name to: $newName")
            val result = authRepository.updateDisplayName(newName)
            result.onSuccess {
                user = user?.copy(displayName = newName)
                refreshUser()
                Log.d("ProfileViewModel", "Display name updated successfully")
            }.onFailure { exception ->
                Log.e("ProfileViewModel", "Failed to update display name: ${exception.message}", exception)
            }
            result
        } catch (e: Exception) {
            Log.e("ProfileViewModel", "Unexpected error updating display name: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Update user's password
    suspend fun updatePassword(newPassword: String): Result<String> {
        return try {
            Log.d("ProfileViewModel", "Attempting to update password")
            val result = authRepository.updatePassword(newPassword)
            result.onSuccess {
                Log.d("ProfileViewModel", "Password updated successfully")
            }.onFailure { exception ->
                Log.e("ProfileViewModel", "Failed to update password: ${exception.message}", exception)
            }
            result
        } catch (e: Exception) {
            Log.e("ProfileViewModel", "Unexpected error updating password: ${e.message}", e)
            Result.failure(e)
        }
    }
}