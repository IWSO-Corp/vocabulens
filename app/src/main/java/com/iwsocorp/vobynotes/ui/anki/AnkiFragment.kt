package com.iwsocorp.vobynotes.ui.anki

import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.iwsocorp.vobynotes.R
import com.iwsocorp.vobynotes.core.common.BaseFragment
import com.iwsocorp.vobynotes.core.data.anki.showExportFailed
import com.iwsocorp.vobynotes.core.data.anki.showExportFinished
import com.iwsocorp.vobynotes.core.data.anki.updateProgress
import com.iwsocorp.vobynotes.core.model.Corpus
import com.iwsocorp.vobynotes.core.model.Note
import com.iwsocorp.vobynotes.databinding.FragmentAnkiBinding
import com.iwsocorp.vobynotes.ui.note.NoteViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class AnkiFragment : BaseFragment<FragmentAnkiBinding>(FragmentAnkiBinding::inflate) {

    private val viewModel: AnkiViewModel by viewModels()
    private val noteViewModel: NoteViewModel by activityViewModels()
    private val notificationManager by lazy {
        requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        observePreview()
        observeUI()
    }

    private fun setupUI() = noteViewModel.notes.observe(viewLifecycleOwner) { notes ->
        binding.noteDropdown.setAdapter(
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                listOf("Select Note") + notes.map {
                    val title = it.title.ifEmpty { "Untitled" }
                    val lang = "${it.wordLang} - ${it.meaningLang}"
                    "$title ($lang)"
                }
            )
        )
        binding.noteDropdown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                p0: AdapterView<*>?,
                p1: View?,
                p2: Int,
                p3: Long,
            ) {
                if (p2 == 0) {
                    resetPreview()
                    return
                }

                val note = notes[p2 - 1]

                viewLifecycleOwner.lifecycleScope.launch {
                    viewModel.handleBeforeSelect(note)
                    onSelect(note)
                }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {}

        }
    }

    private fun onSelect(note: Note) = viewModel.getNotExportedByNote(note.id) { list ->
        Timber.d("Corpus: ${list.size}")

        if (list.isEmpty()) {
            viewModel.setPreviewState(PreviewState(cards = null))
            binding.btnSend.setOnClickListener(null)

            Toast.makeText(
                requireContext(),
                "List of words is empty or already exported",
                Toast.LENGTH_SHORT
            ).show()
            return@getNotExportedByNote
        }

        viewModel.preview(note, list.first())
        binding.btnSend.setOnClickListener {
            onSend(note, list)
        }
    }

    private fun onSend(note: Note, list: List<Corpus>) = AlertDialog.Builder(requireContext())
        .setTitle("Send to Anki")
        .setMessage(
            getString(
                R.string.send_info,
                list.size,
                list.filter { it.meanings.isEmpty() }.size,
                list.filter { it.meanings.isNotEmpty() }.size
            )
        )
        .setPositiveButton("Yes") { _, _ ->
            viewModel.exportNoteToAnki(requireActivity(), note, list) { current, total ->
                val progress = (current * 100) / total

                notificationManager.updateProgress(
                    requireContext(),
                    title = "Send to Anki",
                    message = "Sending $current from $total",
                    progress = progress
                )

                if (progress == 100) notificationManager.showExportFinished(requireContext())
            }
        }
        .setNegativeButton("No") { _, _ -> }
        .show()

    private fun observePreview() = viewModel.previewState.collectOnStarted {
        Timber.d("PreviewState: $it")

        binding.progressBar.isVisible = it.loading

        if (it.cards != null) {
            val previewUI = viewModel.mapPreviewToUi(it.cards)
            binding.frontWebView.loadDataWithBaseURL(
                null,
                previewUI.first().frontHtml,
                "text/html",
                "utf-8",
                null
            )
            binding.backWebView.loadDataWithBaseURL(
                null,
                previewUI.first().backHtml,
                "text/html",
                "utf-8",
                null
            )
        } else {
            resetPreview()
        }

        if (it.error != null) {
            Toast.makeText(
                requireContext(),
                "Error: ${it.error}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun resetPreview() {
        binding.frontWebView.loadUrl("about:blank")
        binding.backWebView.loadUrl("about:blank")
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
                    "Successfully export ${it.noteTitle}, ${it.totalAdded} cards added to Anki",
                    Toast.LENGTH_SHORT
                ).show()

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

            is UiState.AnkiNotInstalled -> {
                Toast.makeText(
                    requireContext(),
                    "Anki is not installed",
                    Toast.LENGTH_SHORT
                ).show()

                viewModel.resetState()
            }

            is UiState.PermissionRequired -> {
                Toast.makeText(
                    requireContext(),
                    "Permission to access Anki is required",
                    Toast.LENGTH_SHORT
                ).show()

                viewModel.resetState()
            }

            else -> {}
        }
    }
}