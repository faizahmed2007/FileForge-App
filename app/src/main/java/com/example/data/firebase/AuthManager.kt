package com.example.data.firebase

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AuthManager(private val context: Context) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val credentialManager: CredentialManager = CredentialManager.create(context)

    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser
        }
    }

    private fun getServerClientId(): String {
        return context.getString(R.string.default_web_client_id_placeholder)
    }

    suspend fun signInWithGoogle(activityContext: Context): Result<FirebaseUser?> = withContext(Dispatchers.IO) {
        _isLoading.value = true
        _errorMessage.value = null

        try {
            val serverClientId = getServerClientId()

            // Verify each GetCredentialRequest carries exactly one credential option:
            // GetSignInWithGoogleOption for interactive sign-in
            val signInOption = GetSignInWithGoogleOption.Builder(serverClientId)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInOption)
                .build()

            val response = credentialManager.getCredential(
                context = activityContext,
                request = request
            )

            val credential = response.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(firebaseCredential).await()
                val user = authResult.user
                _currentUser.value = user
                _isLoading.value = false
                Result.success(user)
            } else {
                _isLoading.value = false
                _errorMessage.value = "Unrecognized credential type"
                Result.failure(IllegalStateException("Unrecognized credential type"))
            }
        } catch (e: GetCredentialCancellationException) {
            // Skill requirement: Catch GetCredentialCancellationException separately,
            // log diagnostic warning, and reset UI state (isLoading = false) for retry.
            Log.w("AuthManager", "Interactive Google Sign-In was cancelled by user: ${e.message}")
            _isLoading.value = false
            Result.failure(e)
        } catch (e: GetCredentialException) {
            Log.e("AuthManager", "Google Credential Manager failure: ${e.message}", e)
            _isLoading.value = false
            _errorMessage.value = "Sign-in failed: ${e.localizedMessage}"
            Result.failure(e)
        } catch (e: Exception) {
            Log.e("AuthManager", "Unexpected sign-in error: ${e.message}", e)
            _isLoading.value = false
            _errorMessage.value = e.localizedMessage ?: "Authentication error"
            Result.failure(e)
        }
    }

    suspend fun trySilentSignIn(): FirebaseUser? = withContext(Dispatchers.IO) {
        if (auth.currentUser != null) {
            return@withContext auth.currentUser
        }
        try {
            val serverClientId = getServerClientId()
            // In a separate request, use GetGoogleIdOption for silent auto sign-in
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(true)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val response = credentialManager.getCredential(context = context, request = request)
            val credential = response.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(firebaseCredential).await()
                _currentUser.value = authResult.user
                authResult.user
            } else {
                null
            }
        } catch (e: Exception) {
            Log.d("AuthManager", "Silent sign-in skipped: ${e.message}")
            null
        }
    }

    fun signOut() {
        auth.signOut()
        _currentUser.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
