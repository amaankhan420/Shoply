package com.example.shoply.repositories

import android.util.Log
import com.example.shoply.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val googleSignInClient: GoogleSignInClient?
) {
    // Sign in with email and password
    suspend fun signInWithEmail(email: String, password: String): Result<User> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user
            if (firebaseUser != null) {
                val user = User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    displayName = firebaseUser.displayName
                        ?: firebaseUser.email?.substringBefore("@") ?: ""
                )
                saveUserToFirestore(user)
                Result.success(user)
            } else {
                Log.e("AuthRepository", "Email sign-in failed: No user returned")
                Result.failure(Exception("Sign-in failed"))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Email sign-in error: ${e.message}")
            Result.failure(e)
        }
    }

    // Sign up with email, password, and display name
    suspend fun signUpWithEmail(
        email: String,
        password: String,
        displayName: String
    ): Result<User> {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user
            if (firebaseUser != null) {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .build()
                firebaseUser.updateProfile(profileUpdates).await()
                val user = User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    displayName = displayName
                )
                saveUserToFirestore(user)
                Result.success(user)
            } else {
                Log.e("AuthRepository", "Email sign-up failed: No user returned")
                Result.failure(Exception("Sign-up failed"))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Email sign-up error: ${e.message}")
            Result.failure(e)
        }
    }

    // Sign in with Google account
    suspend fun signInWithGoogle(account: GoogleSignInAccount): Result<User> {
        return try {
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            val result = firebaseAuth.signInWithCredential(credential).await()
            val firebaseUser = result.user
            if (firebaseUser != null) {
                val user = User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    displayName = firebaseUser.displayName
                        ?: firebaseUser.email?.substringBefore("@") ?: ""
                )
                saveUserToFirestore(user)
                Result.success(user)
            } else {
                Log.e("AuthRepository", "Google sign-in failed: No user returned")
                Result.failure(Exception("Google sign-in failed"))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Google sign-in error: ${e.message}")
            Result.failure(e)
        }
    }

    // Save user data to Firestore
    suspend fun saveUserToFirestore(user: User) {
        try {
            firestore.collection("users").document(user.uid).set(user).await()
            Log.d("AuthRepository", "User data saved to Firestore successfully")
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to save user to Firestore: ${e.message}")
        }
    }

    // Sign out the current user
    fun signOut() {
        try {
            firebaseAuth.signOut()
            googleSignInClient?.signOut()
            Log.d("AuthRepository", "User signed out successfully")
        } catch (e: Exception) {
            Log.e("AuthRepository", "Sign-out error: ${e.message}")
        }
    }

    // Get the current authenticated user
    fun getCurrentUser(): User? {
        return try {
            val firebaseUser = firebaseAuth.currentUser
            firebaseUser?.let {
                User(
                    uid = it.uid,
                    email = it.email ?: "",
                    displayName = it.displayName ?: it.email?.substringBefore("@") ?: ""
                )
            }.also {
                Log.d("AuthRepository", "Current user fetched successfully")
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error fetching current user: ${e.message}")
            null
        }
    }

    // Update user's display name
    suspend fun updateDisplayName(newName: String): Result<String> {
        return try {
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser != null) {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(newName)
                    .build()
                firebaseUser.updateProfile(profileUpdates).await()
                val updatedUser = firebaseUser.let {
                    User(
                        uid = it.uid,
                        email = it.email ?: "",
                        displayName = newName
                    )
                }
                saveUserToFirestore(updatedUser)
                Log.d("AuthRepository", "Display name updated successfully")
                Result.success("Display name updated successfully!")
            } else {
                Log.e("AuthRepository", "No user signed in for display name update")
                Result.failure(Exception("No user signed in"))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Display name update error: ${e.message}")
            Result.failure(when (e) {
                is IllegalArgumentException -> Exception("Invalid display name")
                else -> Exception("Failed to update display name: ${e.message}")
            })
        }
    }

    // Update user's password
    suspend fun updatePassword(newPassword: String): Result<String> {
        return try {
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser?.providerData?.any { it.providerId == "password" } == true) {
                if (newPassword.length < 6) {
                    Log.e("AuthRepository", "Password too short: must be at least 6 characters")
                    return Result.failure(Exception("Password must be at least 6 characters"))
                }
                firebaseUser.updatePassword(newPassword).await()
                Log.d("AuthRepository", "Password updated successfully")
                Result.success("Password updated successfully!")
            } else {
                Log.e("AuthRepository", "Password update not allowed for non-email account")
                Result.failure(Exception("Password update not allowed for this account"))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Password update error: ${e.message}")
            Result.failure(when (e) {
                is IllegalArgumentException -> Exception("Invalid password")
                else -> Exception("Failed to update password: ${e.message}")
            })
        }
    }
}