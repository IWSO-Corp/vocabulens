package com.iwsocorp.vobynotes.ui.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    fun sendFeedback(
        text: String,
        onDone: (Boolean) -> Unit,
    ) = viewModelScope.launch(Dispatchers.IO) {
        try {
            val response = googleFormRepository.postFeedback(text)
            Timber.d("Response: $response")
            if (response.isSuccessful) {
                onDone(true)
            } else {
                onDone(false)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            onDone(false)
        }
    }

}