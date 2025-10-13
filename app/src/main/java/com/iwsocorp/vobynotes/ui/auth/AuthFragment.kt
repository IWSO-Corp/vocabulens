package com.iwsocorp.vobynotes.ui.auth

import android.animation.ValueAnimator
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout
import com.iwsocorp.vobynotes.BuildConfig
import com.iwsocorp.vobynotes.R
import com.iwsocorp.vobynotes.core.common.BaseFragment
import com.iwsocorp.vobynotes.databinding.FragmentAuthBinding
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class AuthFragment : BaseFragment<FragmentAuthBinding>(FragmentAuthBinding::inflate) {

    private val viewModel: AuthViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        observeState()
    }

    private fun observeState() = viewModel.authState.collectOnStarted {
        it.onSuccess { user ->
            user?.let {
                Toast.makeText(requireContext(), "Signed in", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        }.onFailure { error ->
            Timber.e(error)
            Toast.makeText(
                requireContext(),
                error.message,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun setupUI() = with(binding) {
        btnSignin.setOnClickListener {
            if (tilEmail.error != null || tilPassword.error != null) return@setOnClickListener

            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            if (email.isEmpty() || password.isEmpty()) return@setOnClickListener

            viewModel.signInWithEmail(email, password)
        }
        btnGoogle.setOnClickListener {
            viewModel.signInWithGoogle(
                context = requireContext(),
                webClientId = BuildConfig.CLIENT_ID,
                nonce = null
            )
        }
        tvForgot.setOnClickListener {
            findNavController().navigate(R.id.action_authFragment_to_resetPassFragment)
        }

        etEmail.addTextChangedListener(inputWatcher(tilEmail, ::validateEmail))
        etPassword.addTextChangedListener(inputWatcher(tilPassword, ::validatePassword))

        btnSignin.isEnabled = false
        btnSignin.alpha = 0.5f
    }

    companion object {

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
                } else with(layout) {
                    if (
                        this.editText?.inputType?.and(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS) != 0
                    ) endIconDrawable = null
                    else endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
                }

                val tilEmail = layout.rootView.findViewById<TextInputLayout>(R.id.til_email)
                val etEmail = tilEmail.editText
                val tilPassword = layout.rootView.findViewById<TextInputLayout>(R.id.til_password)
                val etPassword = tilPassword.editText
                val btnSignin = layout.rootView.findViewById<MaterialButton>(R.id.btn_signin)

                val isValid = tilEmail.error == null && tilPassword.error == null
                if (etEmail?.text.toString().isNotEmpty() && etPassword?.text.toString()
                        .isNotEmpty()
                ) {
                    btnSignin.isEnabled = isValid
                    btnSignin.alpha = if (isValid) 1f else 0.5f
                }
            }

            override fun afterTextChanged(s: Editable?) {}

        }

        fun TextInputLayout.fadeIcon() = this.endIconDrawable?.mutate()?.let {
            ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 200
                addUpdateListener { animation ->
                    it.alpha = (animation.animatedValue as Float * 255).toInt()
                }
            }.start()
        }

        fun validateEmail(email: String): String? = when {
            email.isEmpty() -> "Email cannot be empty"
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Email invalid"
            else -> null
        }

        fun validatePassword(password: String): String? = when {
            password.isEmpty() -> "Password cannot be empty"
            password.length < 6 -> "Password should be at least 6 characters"
            password.startsWith(" ") -> "Password cannot start with spaces"
            password.endsWith(" ") -> "Password cannot end with spaces"
            else -> null
        }
    }

}