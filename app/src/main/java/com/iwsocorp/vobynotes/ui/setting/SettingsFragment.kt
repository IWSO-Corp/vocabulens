package com.iwsocorp.vobynotes.ui.setting

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseUser
import com.iwsocorp.vobynotes.R
import com.iwsocorp.vobynotes.core.common.BaseFragment
import com.iwsocorp.vobynotes.core.common.Utils.showAlertDialog
import com.iwsocorp.vobynotes.databinding.FragmentSettingsBinding
import com.iwsocorp.vobynotes.ui.auth.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class SettingsFragment : BaseFragment<FragmentSettingsBinding>(FragmentSettingsBinding::inflate) {

    private val viewModel: SettingsViewModel by viewModels()
    private val authViewModel: AuthViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeState()
    }

    private fun observeState() {
        authViewModel.authState.collectOnStarted {
            it.onSuccess { user ->
                setupUI(user)
                Timber.d("Firebase user: ${user?.uid}")
            }.onFailure { error ->
                setupUI(null)
                Timber.e(error)
                Toast.makeText(requireContext(), error.message, Toast.LENGTH_SHORT).show()
            }
        }
        viewModel.backupState.collectOnStarted {
            binding.progressLoading.isVisible = it is BackupState.Loading

            when (it) {
                is BackupState.Success -> {
                    Toast.makeText(requireContext(), "Backup success", Toast.LENGTH_SHORT).show()
                }

                is BackupState.Restored -> {
                    Toast.makeText(requireContext(), "Restore success", Toast.LENGTH_SHORT).show()
                }

                is BackupState.Error -> {
                    Toast.makeText(requireContext(), "Error occurred", Toast.LENGTH_SHORT).show()
                    Timber.e(it.message)
                }

                else -> {}
            }
        }
    }

    private fun setupUI(user: FirebaseUser?) = with(binding) {
        toolbarSetting.apply {
            title = "Settings"
            setNavigationIcon(R.drawable.baseline_arrow_back_24)
            setNavigationOnClickListener {
                findNavController().navigateUp()
            }
        }
        tvAccount.text = user?.email
        tvAccount.isVisible = user != null
        tvSignin.text = getString(
            if (user == null) R.string.sign_in else R.string.sign_out
        )
        tvSignin.setTextColor(
            resources.getColor(
                if (user == null) R.color.teal_700 else R.color.red,
                null
            )
        )
        tvSignin.setOnClickListener {
            user?.let {
                showAlertDialog(
                    requireContext(),
                    "Sign Out",
                    "Are you sure you want to sign out?",
                    "Sign Out",
                    "Cancel",
                ) {
                    authViewModel.signOut()
                    Toast.makeText(requireContext(), "Signed out", Toast.LENGTH_SHORT).show()
                }
            } ?: run {
                findNavController().navigate(R.id.action_nav_settings_to_authFragment)
            }
        }
        btnBackup.setOnClickListener {
            user?.let {
                viewModel.backupToFirestore(it.uid)
                Toast.makeText(requireContext(), "Syncing...", Toast.LENGTH_SHORT).show()
            } ?: run {
                signInFirst()
            }
        }
        btnRestore.setOnClickListener {
            user?.let {
                viewModel.restoreFromFirestoreAndInsertToDatabase(it.uid)
                Toast.makeText(requireContext(), "Syncing...", Toast.LENGTH_SHORT).show()
            } ?: run {
                signInFirst()
            }
        }
        btnImport.setOnClickListener {

        }
    }

    private fun signInFirst() = showAlertDialog(
        requireContext(),
        "You are not signed in",
        "Please sign in first",
        "Sign in",
        "Cancel",
    ) {
        findNavController().navigate(R.id.action_nav_settings_to_authFragment)
    }
}