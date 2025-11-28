package com.iwsocorp.vobynotes.ui.setting

import android.app.Activity.RESULT_OK
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.auth.FirebaseUser
import com.iwsocorp.vobynotes.R
import com.iwsocorp.vobynotes.core.common.BaseFragment
import com.iwsocorp.vobynotes.core.common.CorpusFileManager
import com.iwsocorp.vobynotes.core.common.FilePickerManager
import com.iwsocorp.vobynotes.core.common.Utils.showAlertDialog
import com.iwsocorp.vobynotes.core.common.Utils.showToast
import com.iwsocorp.vobynotes.core.model.Note
import com.iwsocorp.vobynotes.databinding.BottomSheetBackupBinding
import com.iwsocorp.vobynotes.databinding.FragmentSettingsBinding
import com.iwsocorp.vobynotes.ui.auth.AuthViewModel
import com.iwsocorp.vobynotes.ui.home.HomeViewModel
import com.iwsocorp.vobynotes.ui.note.NoteBottomSheet
import com.iwsocorp.vobynotes.ui.widget.SearchWidget
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : BaseFragment<FragmentSettingsBinding>(FragmentSettingsBinding::inflate) {

    private val viewModel: SettingsViewModel by activityViewModels()
    private val authViewModel: AuthViewModel by activityViewModels()
    private val homeViewModel: HomeViewModel by activityViewModels()
    private val importViewModel: ImportViewModel by activityViewModels()

    @Inject
    lateinit var fileManager: CorpusFileManager
    private lateinit var appUpdateManager: AppUpdateManager
    private lateinit var pickerManager: FilePickerManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        appUpdateManager = AppUpdateManagerFactory.create(requireContext())
        pickerManager = FilePickerManager(
            caller = this,
            context = requireContext(),
            fileManager = fileManager,
            onImportComplete = { data, fileName ->
                importViewModel.setImportedData(data)
                findNavController().navigate(
                    R.id.action_nav_settings_to_importFragment,
                    bundleOf(ARG_TITLE to fileName)
                )
            },
            onExportComplete = { stream, name ->
                note.observe(viewLifecycleOwner) { note ->
                    homeViewModel.getCorpusByNoteId(note.id) {
                        fileManager.exportCorpusList(stream, it)
                    }
                }
            }
        )

        appUpdateManager.appUpdateInfo.addOnSuccessListener {
            val isUpdate = it.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
            binding.redDot.isVisible = isUpdate
            Timber.d("Update available: $isUpdate")
        }

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

        val versionName = requireContext().packageManager.getPackageInfo(
            requireContext().packageName,
            0
        ).versionName
        tvVersion.text = "v$versionName"

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
            showExportDialog()
        }
        btnFeedback.setOnClickListener {
            if (user != null) findNavController().navigate(R.id.action_nav_settings_to_feedbackFragment)
            else signInFirst()
        }
        btnAddWidget.setOnClickListener {
            requestAddWidgetToHomeScreen()
        }
        btnVersion.setOnClickListener {
            checkForAppUpdates()
        }
    }

    private fun requestAddWidgetToHomeScreen() {
        val appWidgetManager = AppWidgetManager.getInstance(requireContext())
        val provider = ComponentName(requireContext(), SearchWidget::class.java)

        if (appWidgetManager.isRequestPinAppWidgetSupported) {
            val intent = Intent(requireContext(), SearchWidget::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                requireContext(),
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE
            )

            appWidgetManager.requestPinAppWidget(provider, null, pendingIntent)
        } else {
            Toast.makeText(
                requireContext(),
                "The device does not support adding widgets directly",
                Toast.LENGTH_SHORT
            ).show()
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

    private fun showExportDialog() {
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

                NoteBottomSheet(viewModel.notes.value!!) {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Export Note")
                        .setMessage("Are you sure want to export ${it.title}?")
                        .setPositiveButton("Export") { _, _ ->
                            note.value = it
                            pickerManager.exportExcel(it.title)
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }.show(parentFragmentManager, null)
            }
            cardRestore.setOnClickListener {
                dialog.dismiss()

                pickerManager.showImportBottomSheet()
            }
            iconClose.setOnClickListener {
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private val note = MutableLiveData<Note>()

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
                    viewModel.insertToDatabase(backupData) {
                        Toast.makeText(
                            requireContext(),
                            "Restore ${it.successCount} success, ${it.failedCount} failed",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    Toast.makeText(requireContext(), "Syncing...", Toast.LENGTH_SHORT).show()
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

    private fun checkForAppUpdates() {

        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            val isUpdate = appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
            showToast(
                requireContext(),
                if (isUpdate) "Update available" else "The app is up to date"
            )

            // Check if an update is available
            if (isUpdate && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            ) {
                showToast(requireContext(), getString(R.string.update_available))
                monitorUpdates(appUpdateInfo)
            }
        }.addOnFailureListener { exception ->
            // Handle the error if the task fails
            showToast(requireContext(), getString(R.string.failed_to_check_for_updates))
        }
    }

    private val listener = InstallStateUpdatedListener { state ->
        when (state.installStatus()) {
            InstallStatus.DOWNLOADING -> {
                // Get progress percentage and show it to the user
                val bytesDownloaded = state.bytesDownloaded()
                val totalBytesToDownload = state.totalBytesToDownload()
                if (totalBytesToDownload > 0) {
                    val progress = (bytesDownloaded * 100 / totalBytesToDownload).toInt()
                    // Show progress in a UI component, such as a progress bar
                    showToast(requireContext(), "Download progress: $progress%")
                }
            }

            InstallStatus.DOWNLOADED -> {
                // Update has been downloaded, prompt user to restart the app
                showRestartSnackbar()
                showToast(requireContext(), "Update downloaded.")
            }

            InstallStatus.FAILED -> {
                // Handle update failure
                showToast(requireContext(), "Update failed.")
            }

            InstallStatus.INSTALLED -> {
                showToast(requireContext(), "Update installed.")
            }

            else -> {
                // Handle other cases if necessary
                showToast(requireContext(), "Update status: ${state.installStatus()}")
            }
        }
    }

    private val activityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        // handle callback
        if (result.resultCode != RESULT_OK) {
            showToast(requireContext(), "Update flow failed! Result code: ${result.resultCode}");
            // If the update is canceled or fails,
            // you can request to start the update again.
        }
    }

    private fun monitorUpdates(appUpdateInfo: AppUpdateInfo) {
        // Register the listener
        appUpdateManager.registerListener(listener)

        // Start the update
        appUpdateManager.startUpdateFlowForResult(
            // Pass the intent that is returned by 'getAppUpdateInfo()'.
            appUpdateInfo,
            // an activity result launcher registered via registerForActivityResult
            activityResultLauncher,
            // Or pass 'AppUpdateType.FLEXIBLE' to newBuilder() for
            // flexible updates.
            AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
        )

        // When status updates are no longer needed, unregister the listener.
        appUpdateManager.unregisterListener(listener)
    }

    private fun showRestartSnackbar() = Snackbar.make(
        requireActivity().findViewById(android.R.id.content),
        getString(R.string.update_downloaded),
        Snackbar.LENGTH_INDEFINITE
    ).setAction("Restart") {
        appUpdateManager.completeUpdate()  // This will trigger the app restart
    }.show()

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.setIdle()
    }

}