package com.iwsocorp.vobynotes.core.data.repository

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

interface AuthRepository {
    suspend fun signInWithGoogle(
        context: Context,
        webClientId: String,
        nonce: String? = null,
    ): Result<FirebaseUser?>

    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser?>
    fun getCurrentUser(): FirebaseUser?
    suspend fun signOut()
    suspend fun resetPassword(email: String): Result<Void?>
}

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val credentialManager: CredentialManager,
) : AuthRepository {

    override suspend fun signInWithGoogle(
        context: Context,
        webClientId: String,
        nonce: String?,
    ): Result<FirebaseUser?> {
        return try {
            val option = GetSignInWithGoogleOption.Builder(webClientId)
                .setNonce(nonce ?: "")
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(option)
                .build()

            val response = credentialManager.getCredential(context, request)
            val credential = response.credential

            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleCred = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleCred.idToken

                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                val user = firebaseAuth.signInWithCredential(firebaseCredential).await().user
                Result.success(user)
            } else {
                Result.failure(Exception("Credential bukan Google"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser?> {
        return try {
            val user = firebaseAuth.signInWithEmailAndPassword(email, password).await().user
            Result.success(user)
        } catch (e: Exception) {
            when (e) {
                is FirebaseAuthInvalidUserException,
                is FirebaseAuthInvalidCredentialsException -> {
                    try {
                        val newUser = firebaseAuth.createUserWithEmailAndPassword(email, password)
                            .await().user
                        Result.success(newUser)
                    } catch (signupError: Exception) {
                        Result.failure(signupError)
                    }
                }

                else -> Result.failure(e)
            }
        }
    }

    override fun getCurrentUser(): FirebaseUser? = firebaseAuth.currentUser

    override suspend fun signOut() {
        firebaseAuth.signOut()
        try {
            val request = ClearCredentialStateRequest()
            credentialManager.clearCredentialState(request)
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    override suspend fun resetPassword(email: String): Result<Void?> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email, null).await()
            Result.success(null)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}