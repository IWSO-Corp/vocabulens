package com.iwsocorp.vobynotes.ui.auth

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout
import com.iwsocorp.vobynotes.R
import com.iwsocorp.vobynotes.core.common.BaseFragment
import com.iwsocorp.vobynotes.databinding.FragmentResetPassBinding
import com.iwsocorp.vobynotes.ui.auth.AuthFragment.Companion.fadeIcon
import com.iwsocorp.vobynotes.ui.auth.AuthFragment.Companion.validateEmail
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ResetPassFragment :
    BaseFragment<FragmentResetPassBinding>(FragmentResetPassBinding::inflate) {

    private val viewModel: AuthViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        observeState()
    }

    private fun setupUI() = with(binding) {
        toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        etEmail.addTextChangedListener(inputWatcher(tilEmail, ::validateEmail))
        btnResetPassword.isEnabled = tilEmail.error == null
        btnResetPassword.alpha = if (tilEmail.error == null) 1f else 0.5f
        btnResetPassword.setOnClickListener {
            val email = etEmail.text.toString().trim()
            if (tilEmail.error != null || email.isEmpty()) return@setOnClickListener

            viewModel.resetPassword(email)
        }
        btnResetPassword.isEnabled = false
        btnResetPassword.alpha = 0.5f
    }

    private fun observeState() = viewModel.resetPasswordState.collectOnStarted {
        it?.onSuccess {
            Toast.makeText(
                requireContext(),
                "Password reset email sent",
                Toast.LENGTH_SHORT
            ).show()
            findNavController().navigateUp()
        }?.onFailure { error ->
            Toast.makeText(requireContext(), error.message, Toast.LENGTH_LONG).show()
        }
    }

    private fun inputWatcher(
        layout: TextInputLayout,
        validator: (String) -> String?
    ): TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            val input = s.toString()
            val errorMsg = validator(input)
            layout.error = errorMsg

            val icon = when {
                errorMsg != null -> R.drawable.baseline_error_outline_24
                else -> 0
            }

            if (icon != 0) with(layout) {
                endIconDrawable = ContextCompat.getDrawable(layout.context, icon)
                endIconMode = TextInputLayout.END_ICON_CUSTOM
                fadeIcon()
            } else layout.endIconDrawable = null

            val tilEmail = layout.rootView.findViewById<TextInputLayout>(R.id.til_email)
            val btnReset = layout.rootView.findViewById<MaterialButton>(R.id.btnResetPassword)

            val isValid = tilEmail.error == null
            btnReset.isEnabled = isValid
            btnReset.alpha = if (isValid) 1f else 0.5f
        }

        override fun afterTextChanged(s: Editable?) {}

    }
}