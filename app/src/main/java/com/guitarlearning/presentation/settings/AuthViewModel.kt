package com.guitarlearning.presentation.settings

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class AuthUiState(
    val user: FirebaseUser? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState(user = auth.currentUser))
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val currentUser: FirebaseUser? get() = auth.currentUser

    /** Credential Manager Google Sign-In (modern, works on MIUI) */
    fun signInWithGoogleCredentialManager(context: Context, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val webClientId = context.getString(com.guitarlearning.R.string.default_web_client_id)
                Log.d("AuthViewModel", "CM: using webClientId=${webClientId.take(30)}…")

                val credentialManager = CredentialManager.create(context)

                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)   // show ALL accounts, not just previously used
                    .setServerClientId(webClientId)
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(context = context, request = request)
                val credential = result.credential

                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    Log.d("AuthViewModel", "CM: got idToken=${googleIdToken.take(20)}…")
                    val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    val authResult = auth.signInWithCredential(firebaseCredential).await()
                    Log.d("AuthViewModel", "CM: Firebase sign-in success: ${authResult.user?.email}")
                    _uiState.value = _uiState.value.copy(user = authResult.user, isLoading = false)
                    onSuccess()
                } else {
                    Log.e("AuthViewModel", "CM: unexpected credential type: ${credential.type}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Неочікуваний тип облікових даних"
                    )
                }
            } catch (e: GetCredentialCancellationException) {
                Log.d("AuthViewModel", "CM: cancelled by user")
                _uiState.value = _uiState.value.copy(isLoading = false, error = null)
            } catch (e: GetCredentialException) {
                Log.e("AuthViewModel", "CM: GetCredentialException: ${e.type} - ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Google Sign-In: ${e.message}"
                )
            } catch (e: Exception) {
                Log.e("AuthViewModel", "CM: unexpected error", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Помилка: ${e.message}"
                )
            }
        }
    }

    fun signInWithEmail(email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                _uiState.value = _uiState.value.copy(user = result.user, isLoading = false)
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = mapFirebaseError(e.message)
                )
            }
        }
    }

    fun signUpWithEmail(email: String, password: String, displayName: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                    this.displayName = displayName.trim().ifEmpty { email.substringBefore("@") }
                }
                result.user?.updateProfile(profileUpdates)?.await()
                _uiState.value = _uiState.value.copy(user = auth.currentUser, isLoading = false)
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = mapFirebaseError(e.message)
                )
            }
        }
    }

    fun signOut(context: Context? = null, onSuccess: () -> Unit = {}) {
        auth.signOut()
        _uiState.value = _uiState.value.copy(user = null)
        onSuccess()
    }

    fun setLoading(loading: Boolean) {
        _uiState.value = _uiState.value.copy(isLoading = loading)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun mapFirebaseError(msg: String?): String = when {
        msg == null -> "Невідома помилка"
        "no user record" in msg || "password is invalid" in msg -> "Невірний email або пароль"
        "email address is already in use" in msg -> "Цей email вже зареєстрований"
        "badly formatted" in msg -> "Невірний формат email"
        "weak password" in msg -> "Пароль занадто слабкий (мінімум 6 символів)"
        "network" in msg.lowercase() -> "Немає з'єднання з мережею"
        else -> msg
    }
}
