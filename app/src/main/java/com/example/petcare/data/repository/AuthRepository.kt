package com.example.petcare.data.repository

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
            val profileUpdates = userProfileChangeRequest {
                displayName = fullName
            }
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
        val user = auth.currentUser ?: error("Not Authenticated")
        return user.getIdToken(false).await().token ?: error("Token is null")
    }

    fun logout() = auth.signOut()
}