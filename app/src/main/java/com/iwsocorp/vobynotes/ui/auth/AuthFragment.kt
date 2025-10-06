package com.iwsocorp.vobynotes.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.iwsocorp.vobynotes.BuildConfig
import com.iwsocorp.vobynotes.core.common.BaseFragment
import com.iwsocorp.vobynotes.databinding.FragmentAuthBinding
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class AuthFragment : BaseFragment<FragmentAuthBinding>(FragmentAuthBinding::inflate) {

    private val viewModel: AuthViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.authState.collectOnStarted {
            it.onSuccess { user ->
                user?.let {
                    Toast.makeText(requireContext(), "Signed in", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
            }.onFailure { error ->
                Timber.e(error)
                Toast.makeText(requireContext(), error.message, Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnSignin.setOnClickListener { onSignIn() }
        binding.btnGoogle.setOnClickListener { onGoogleSignIn() }
    }

    private fun onSignIn() {
        val email = binding.etEmail.text.toString()
        val password = binding.etPassword.text.toString()
        viewModel.signInWithEmail(email, password)
    }

    private fun onGoogleSignIn() = viewModel.signInWithGoogle(
        context = requireContext(),
        webClientId = BuildConfig.CLIENT_ID,
        nonce = null
    )

}