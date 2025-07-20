package com.example.shoply.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shoply.repositories.AuthRepository
import com.example.shoply.models.User
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.util.Log

sealed class AuthUiEvent {
    object SignupSuccess : AuthUiEvent()
    object None : AuthUiEvent()
}

class AuthViewModel(
    private val repository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<User?>(null)
    val authState: StateFlow<User?> = _authState
    private val _loginErrorMessage = MutableStateFlow<String?>(null)
    val loginErrorMessage: StateFlow<String?> = _loginErrorMessage
    private val _signupErrorMessage = MutableStateFlow<String?>(null)
    val signupErrorMessage: StateFlow<String?> = _signupErrorMessage
    private val _uiEvent = MutableStateFlow<AuthUiEvent>(AuthUiEvent.None)
    val uiEvent: StateFlow<AuthUiEvent> = _uiEvent

    // Initialize the current user state
    fun initializeAuthState() {
        try {
            _authState.value = repository.getCurrentUser()
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Failed to initialize auth state: ${e.message}")
            _authState.value = null
        }
    }

    // Sign in with email and password
    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            try {
                val result = repository.signInWithEmail(email, password)
                handleAuthResult(result, isSignup = false)
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Email sign-in failed: ${e.message}")
                _loginErrorMessage.value = "Unexpected error during sign-in. Please try again."
            }
        }
    }

    // Sign up with email, password, and display name
    fun signUpWithEmail(email: String, password: String, displayName: String) {
        viewModelScope.launch {
            try {
                val result = repository.signUpWithEmail(email, password, displayName)
                if (result.isSuccess) {
                    _signupErrorMessage.value = null
                    _uiEvent.value = AuthUiEvent.SignupSuccess
                } else {
                    handleAuthResult(result, isSignup = true)
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Email sign-up failed: ${e.message}")
                _signupErrorMessage.value = "Unexpected error during sign-up. Please try again."
            }
        }
    }

    // Sign in with Google account
    fun signInWithGoogle(account: GoogleSignInAccount) {
        viewModelScope.launch {
            try {
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                FirebaseAuth.getInstance().signInWithCredential(credential).await()
                val userResult = repository.signInWithGoogle(account)
                handleAuthResult(userResult, isSignup = false)
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Google sign-in failed: ${e.message}")
                _loginErrorMessage.value = "Google sign-in failed. Please try again."
            }
        }
    }

    // Sign out the current user
    fun signOut() {
        viewModelScope.launch {
            try {
                repository.signOut()
                _authState.value = null
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Sign-out failed: ${e.message}")
                _loginErrorMessage.value = "Failed to sign out. Please try again."
            }
        }
    }

    // Reset UI event to none
    fun resetUiEvent() {
        try {
            _uiEvent.value = AuthUiEvent.None
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Failed to reset UI event: ${e.message}")
        }
    }

    // Handle authentication result and set appropriate error messages
    fun handleAuthResult(result: Result<User>, isSignup: Boolean) {
        try {
            if (result.isSuccess) {
                if (!isSignup) {
                    _authState.value = result.getOrNull()
                }
                if (isSignup) {
                    _uiEvent.value = AuthUiEvent.SignupSuccess
                }
                _loginErrorMessage.value = null
                _signupErrorMessage.value = null
            } else {
                val errorMessage = when (result.exceptionOrNull()) {
                    is FirebaseAuthInvalidCredentialsException -> "Invalid email or password."
                    is FirebaseAuthInvalidUserException -> "No account found with this email."
                    is FirebaseAuthUserCollisionException -> "An account with this email already exists."
                    else -> "Authentication failed. Please try again."
                }
                if (isSignup) {
                    _signupErrorMessage.value = errorMessage
                } else {
                    _loginErrorMessage.value = errorMessage
                }
            }
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Error handling auth result: ${e.message}")
            if (isSignup) {
                _signupErrorMessage.value = "Unexpected error during authentication. Please try again."
            } else {
                _loginErrorMessage.value = "Unexpected error during authentication. Please try again."
            }
        }
    }
}