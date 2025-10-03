package com.iwsocorp.vobynotes.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.iwsocorp.vobynotes.core.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
) : ViewModel() {

    private val _authState = MutableStateFlow(Result.success(repository.getCurrentUser()))
    val authState: StateFlow<Result<FirebaseUser?>> = _authState.asStateFlow()

    fun signInWithGoogle(context: Context, webClientId: String, nonce: String? = null) =
        viewModelScope.launch {
            _authState.value = repository.signInWithGoogle(context, webClientId, nonce)
        }

    fun signInWithEmail(email: String, password: String) = viewModelScope.launch {
        _authState.value = repository.signInWithEmail(email, password)
    }

    fun signOut() = viewModelScope.launch {
        repository.signOut()
        _authState.value = Result.success(null)
    }

}