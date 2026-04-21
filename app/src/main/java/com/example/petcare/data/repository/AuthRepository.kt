package com.example.petcare.data.repository

import android.util.Log
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()


    val currentUser: FirebaseUser? get() = auth.currentUser

    suspend fun login(email: String, password: String): Result<FirebaseUser> =
        runCatching {
            auth.signInWithEmailAndPassword(email, password).await().user!!
        }

    suspend fun register(email: String, password: String, fullName: String): Result<FirebaseUser> =
        runCatching {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val profileUpdates = userProfileChangeRequest { displayName = fullName }
            result.user!!.updateProfile(profileUpdates).await()
            result.user!!.getIdToken(true).await()
            result.user!!
        }

    suspend fun loginWithGoogle(idToken: String): Result<FirebaseUser> =
        runCatching {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential).await().user!!
        }

    suspend fun getFreshToken(): String {
        val user = auth.currentUser ?: error("Not authenticated")
        return user.getIdToken(false).await().token ?: error("Token is null")
    }

    suspend fun forceRefreshToken(): String {
        val user = auth.currentUser ?: error("Not authenticated — currentUser is null")
        Log.d("AuthRepo", "Reloading user: ${user.uid}")
        user.reload().await()
        return user.getIdToken(true).await().token ?: error("Token is null after force refresh")
    }

    suspend fun updateEmail(
        currentPassword: String,
        newEmail: String
    ): Result<Unit> = runCatching {
        val user = auth.currentUser
            ?: error("The user is not authenticated")

        val isGoogleUser = user.providerData
            .any { it.providerId == GoogleAuthProvider.PROVIDER_ID }

        if (isGoogleUser) {
            error("Google accounts cannot change their email here.")
        }

        val currentEmail = user.email
            ?: error("The user doesn't have an email associated")
        val credential = EmailAuthProvider.getCredential(currentEmail, currentPassword)
        try {
            user.reauthenticate(credential).await()
        } catch (e: Exception) {
            error("Incorrect password")
        }

        try {
            user.verifyBeforeUpdateEmail(newEmail).await()
        } catch (e: Exception) {
            error("Failed to send verification email. Please check the address and try again.")
        }

        Log.d("EMAIL", "Email Sent")
    }

    suspend fun resetPassword(email: String): Result<Unit> = runCatching {
        auth.sendPasswordResetEmail(email).await()
    }

    fun logout() = auth.signOut()

}
