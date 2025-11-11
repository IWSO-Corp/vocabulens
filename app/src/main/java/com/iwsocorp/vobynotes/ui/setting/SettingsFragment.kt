package com.iwsocorp.vobynotes.ui.setting

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseUser
import com.iwsocorp.vobynotes.R
import com.iwsocorp.vobynotes.core.common.BaseFragment
import com.iwsocorp.vobynotes.core.common.Utils.showAlertDialog
import com.iwsocorp.vobynotes.databinding.BottomSheetBackupBinding
import com.iwsocorp.vobynotes.databinding.FragmentSettingsBinding
import com.iwsocorp.vobynotes.ui.auth.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class SettingsFragment : BaseFragment<FragmentSettingsBinding>(FragmentSettingsBinding::inflate) {

    private val viewModel: SettingsViewModel by activityViewModels()
    private val authViewModel: AuthViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeState()
    }

    private fun observeState() {
        authViewModel.authState.collectOnStarted { result ->
            result.onSuccess { user ->
                setupUI(user)
                Timber.d("Firebase user: ${user?.uid}")

                user?.let {
                    val backupData = viewModel.backupData.value
                    if (backupData == null) viewModel.getBackupData(it.uid)
                }
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
            onSignIn(user)
        }

        csAccount.setOnClickListener {
            user?.let {

            }
        }
        btnLanguageTranslations.setOnClickListener {
            findNavController().navigate(R.id.action_nav_settings_to_languageFragment)
        }
        btnBackupRestore.setOnClickListener {
            showBackupDialog(user)
        }
        btnExportImport.setOnClickListener {
            showExportDialog(user)
        }
        btnImportFileFormat.setOnClickListener {

        }
        btnVersion.setOnClickListener {

        }
    }

    private fun showBackupDialog(user: FirebaseUser?) {
        val binding = BottomSheetBackupBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(binding.root)

        binding.cardBackup.setOnClickListener {
            onBackup(user)
            dialog.dismiss()
        }
        binding.cardRestore.setOnClickListener {
            onRestore(user)
            dialog.dismiss()
        }
        binding.iconClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showExportDialog(user: FirebaseUser?) {
        val binding = BottomSheetBackupBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(binding.root)

        with(binding) {
            iconBackup.setImageResource(R.drawable.baseline_upload_24)
            iconRestore.setImageResource(R.drawable.baseline_file_download_24)
            tvBackup.text = "Export"
            tvRestore.text = "Import"

            cardBackup.setOnClickListener {

                dialog.dismiss()
            }
            cardRestore.setOnClickListener {

                dialog.dismiss()
            }
            iconClose.setOnClickListener {
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun onSignIn(user: FirebaseUser?) = user?.let {
        showAlertDialog(
            requireContext(),
            "Sign Out",
            "Are you sure you want to sign out?",
            "Sign Out",
            "Cancel",
        ) {
            authViewModel.signOut()
            viewModel.resetBackupData()
            Toast.makeText(requireContext(), "Signed out", Toast.LENGTH_SHORT).show()
        }
    } ?: run {
        findNavController().navigate(R.id.action_nav_settings_to_authFragment)
    }

    private fun onRestore(user: FirebaseUser?) = user?.let {
        val backupData = viewModel.backupData.value
        backupData?.let {
            AlertDialog.Builder(requireContext())
                .setTitle("Restore Data")
                .setMessage("Are you sure want to restore data?")
                .setPositiveButton("Restore") { _, _ ->
                    viewModel.insertToDatabase(backupData)
                    Toast.makeText(requireContext(), "Syncing...", Toast.LENGTH_SHORT).show()
                    viewModel.getLocalData()
                }
                .setNegativeButton("Cancel", null)
                .show()
        } ?: run {
            Toast.makeText(requireContext(), "No backup data", Toast.LENGTH_SHORT).show()
        }
        Timber.d("Restore data: notes=${backupData?.notes?.size}, corpus=${backupData?.corpus?.size}, examples=${backupData?.examples}")
    } ?: run {
        signInFirst()
    }

    private fun onBackup(user: FirebaseUser?) = user?.let {
        AlertDialog.Builder(requireContext())
            .setTitle("Backup Data")
            .setMessage("This will overwrite your current data. Are you sure want to backup?")
            .setPositiveButton("Backup") { _, _ ->
                viewModel.backupToFirestore(user.uid)
                Toast.makeText(requireContext(), "Syncing...", Toast.LENGTH_SHORT)
                    .show()
                viewModel.getBackupData(user.uid)
            }
            .setNegativeButton("Cancel", null)
            .show()
    } ?: run {
        signInFirst()
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

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.setIdle()
    }

}