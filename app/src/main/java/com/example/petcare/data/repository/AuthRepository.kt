package com.example.petcare.data.repository

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

    suspend fun updateEmail(
        currentPassword: String,
        newEmail: String
    ): Result<Unit> = runCatching {

        val user = auth.currentUser
            ?: error("The user is not authenticated")

        val isGoogleUser = user.providerData
            .any { it.providerId == GoogleAuthProvider.PROVIDER_ID }

        if (isGoogleUser) {
            error("Google accounts cannot change their email here. Please update it from your Google account settings.")
        }

        val currentEmail = user.email
            ?: error("The user doesn't have an email associated")

        user.getIdToken(true).await()
        val credential = EmailAuthProvider.getCredential(currentEmail, currentPassword)
        user.reauthenticate(credential).await()

        user.verifyBeforeUpdateEmail(newEmail).await()
        newEmail
    }

    fun logout() = auth.signOut()
}