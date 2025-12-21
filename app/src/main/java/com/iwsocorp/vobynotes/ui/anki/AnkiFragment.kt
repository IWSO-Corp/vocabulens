package com.iwsocorp.vobynotes.ui.anki

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.widget.AdapterView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.ichi2.anki.api.AddContentApi
import com.iwsocorp.vobynotes.core.common.BaseFragment
import com.iwsocorp.vobynotes.core.common.Utils.hasPermission
import com.iwsocorp.vobynotes.core.common.Utils.showPermanentlyDeniedDialog
import com.iwsocorp.vobynotes.core.common.Utils.showRationaleDialog
import com.iwsocorp.vobynotes.core.data.anki.showExportFailed
import com.iwsocorp.vobynotes.core.data.anki.showExportFinished
import com.iwsocorp.vobynotes.core.data.anki.updateProgress
import com.iwsocorp.vobynotes.core.model.Corpus
import com.iwsocorp.vobynotes.databinding.FragmentAnkiBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class AnkiFragment : BaseFragment<FragmentAnkiBinding>(FragmentAnkiBinding::inflate) {

    private val viewModel: AnkiViewModel by viewModels()
    private val notificationManager by lazy {
        requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
    private val permissionLauncher: ActivityResultLauncher<Array<String>> by lazy {
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val ankiGranted = permissions[AddContentApi.READ_WRITE_PERMISSION] == true

            when {
                ankiGranted -> {
                    init()
                    Toast.makeText(requireContext(), "Permission Granted", Toast.LENGTH_SHORT)
                        .show()
                }

                else -> {
                    if (shouldShowRequestPermissionRationale(AddContentApi.READ_WRITE_PERMISSION))
                        showRationaleDialog(requireContext()) {
                            checkAnkiPermission()
                        } else showPermanentlyDeniedDialog(requireContext())
                }
            }
        }
    }
    private lateinit var frontWebView: WebView
    private lateinit var backWebView: WebView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (viewModel.isAnkiAvailable) checkAnkiPermission() else Snackbar.make(
            view,
            "Anki is not available on this device",
            Snackbar.LENGTH_INDEFINITE
        ).setAction("OK") {}.show()
    }

    private fun launchRequest() {
        val permissionsToRequest = mutableListOf<String>()

        if (!hasPermission(requireContext(), AddContentApi.READ_WRITE_PERMISSION)) {
            permissionsToRequest.add(AddContentApi.READ_WRITE_PERMISSION)
        }

        if (Build.VERSION.SDK_INT >= 33 &&
            !hasPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
        ) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun checkAnkiPermission() = when {
        hasPermission(requireContext(), AddContentApi.READ_WRITE_PERMISSION) -> init()

        shouldShowRequestPermissionRationale(AddContentApi.READ_WRITE_PERMISSION) -> showRationaleDialog(
            requireContext()
        ) {
            launchRequest()
        }

        else -> launchRequest()
    }

    private fun init() {
        frontWebView = WebView(requireContext())
        backWebView = WebView(requireContext())

        setupUI()
        observePreview()
        observeUI()
    }

    private fun setupUI() = viewModel.getNoteExportStats().collectOnStarted { notes ->
        val adapter = NoteDropdownAdapter(requireContext(), notes)
        binding.noteDropdown.setAdapter(adapter)
        binding.noteDropdown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                p0: AdapterView<*>?,
                p1: View?,
                p2: Int,
                p3: Long,
            ) {
                val note = notes[p2]
                Timber.d("Selected note: $note")

                viewLifecycleOwner.lifecycleScope.launch {
                    viewModel.handleBeforeSelect(
                        note.noteId,
                        note.title,
                        note.wordLang,
                        note.meaningLang
                    )
                    onSelect(note.noteId, note.title, note.wordLang, note.meaningLang)
                }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
    }

    private fun onSelect(noteId: String, title: String, wordLang: String, meaningLang: String) =
        viewModel.getNotExportedByNote(noteId).distinctUntilChanged().collectOnStarted { list ->
            Timber.d("Corpus: ${list.size}")

            binding.btnSend.isEnabled = list.isNotEmpty()

            if (list.isEmpty()) {
                viewModel.setPreviewState(PreviewState(cards = null))
                binding.btnSend.setOnClickListener(null)

                Toast.makeText(
                    requireContext(),
                    "List of words is empty or already exported",
                    Toast.LENGTH_SHORT
                ).show()
                return@collectOnStarted
            }

            viewModel.preview(title, wordLang, meaningLang, list.first())
            binding.btnSend.setOnClickListener {
                onSend(title, wordLang, meaningLang, list)
            }
        }

    private fun onSend(title: String, wordLang: String, meaningLang: String, list: List<Corpus>) =
        AlertDialog.Builder(requireContext())
            .setTitle("Send to Anki")
            .setPositiveButton("Proceed") { _, _ ->
                viewModel.sendListToAnki(title, wordLang, meaningLang, list) { current, total ->
                    val progress = (current * 100) / total

                    notificationManager.updateProgress(
                        requireContext(),
                        title = "Send to Anki",
                        message = "Sending $current from $total",
                        progress = progress
                    )

                    if (progress == 100) notificationManager.showExportFinished(requireContext())
                }

                if (!hasPermission(
                        requireContext(),
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                ) Toast.makeText(
                    requireContext(),
                    "Sending data in background (notification is off)",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("Cancel") { _, _ -> }
            .show()

    private fun observePreview() = viewModel.previewState.distinctUntilChanged().collectOnStarted {
        Timber.d("PreviewState: $it")
        resetPreview()

        binding.progressBar.isVisible = it.loading
        binding.frontPreview.isVisible = it.cards == null
        binding.backPreview.isVisible = it.cards == null

        if (it.cards != null) {
            binding.frontWebView.addView(frontWebView)
            binding.backWebView.addView(backWebView)

            val previewUI = viewModel.mapPreviewToUi(it.cards)

            frontWebView.loadDataWithBaseURL(
                null,
                previewUI.last().frontHtml,
                "text/html",
                "utf-8",
                null
            )
            backWebView.loadDataWithBaseURL(
                null,
                previewUI.last().backHtml,
                "text/html",
                "utf-8",
                null
            )
        }

        if (it.error != null) {
            Toast.makeText(
                requireContext(),
                "Error: ${it.error}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun observeUI() = viewModel.uiState.collectOnStarted {
        Timber.d("UiState: ${it::class.java.simpleName}")

        binding.progressBar.isVisible = it is UiState.Loading

        when (it) {
            is UiState.Success -> {
                binding.noteDropdown.setSelection(0)
                resetPreview()

                Toast.makeText(
                    requireContext(),
                    "Successfully export ${it.noteTitle}, ${it.success} cards added to Anki",
                    Toast.LENGTH_SHORT
                ).show()
                Timber.d("Successfully export ${it.noteTitle}, ${it.success} cards added to Anki")

                viewModel.resetState()
            }

            is UiState.Error -> {
                notificationManager.showExportFailed(requireContext(), it.message)

                Toast.makeText(
                    requireContext(),
                    "Error: ${it.message}",
                    Toast.LENGTH_SHORT
                ).show()
                Timber.e("Error: ${it.message}")

                viewModel.resetState()
            }

            else -> {}
        }
    }

    private fun resetPreview() {
        binding.frontWebView.removeView(frontWebView)
        binding.backWebView.removeView(backWebView)
    }
}