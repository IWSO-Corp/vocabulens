package com.iwsocorp.vobynotes.ui.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.iwsocorp.vobynotes.core.network.retrofit.GoogleFormRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class FeedbackViewModel @Inject constructor(
    private val googleFormRepository: GoogleFormRepository
) : ViewModel() {

    private val email = FirebaseAuth.getInstance().currentUser?.email ?: "vocabulens@gmail.com"

    fun sendFeedback(
        text: String,
        onDone: (Boolean) -> Unit,
    ) = viewModelScope.launch(Dispatchers.IO) {
        try {
            val response = googleFormRepository.postFeedback(email, text)
            Timber.d("Response: $response")
            onDone(response.isSuccessful)
        } catch (e: Exception) {
            e.printStackTrace()
            onDone(false)
        }
    }

}