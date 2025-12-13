package com.iwsocorp.vobynotes.ui.anki

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.iwsocorp.vobynotes.core.common.BaseFragment
import com.iwsocorp.vobynotes.core.model.Note
import com.iwsocorp.vobynotes.databinding.FragmentAnkiBinding
import com.iwsocorp.vobynotes.ui.home.HomeViewModel
import com.iwsocorp.vobynotes.ui.note.NoteViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class AnkiFragment : BaseFragment<FragmentAnkiBinding>(FragmentAnkiBinding::inflate) {

    private val viewModel: AnkiViewModel by viewModels()
    private val noteViewModel: NoteViewModel by activityViewModels()
    private val homeViewModel: HomeViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        noteViewModel.notes.observe(viewLifecycleOwner) {
            setupUI(it)
            Timber.d("Notes: $it")
        }

        observe()
    }

    private fun setupUI(notes: List<Note>) = with(binding) {
        noteDropdown.setAdapter(
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                notes.map { it.title.ifEmpty { "Untitled" } }
            )
        )
        noteDropdown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                p0: AdapterView<*>?,
                p1: View?,
                p2: Int,
                p3: Long,
            ) {
                homeViewModel.getCorpusByNoteId(notes[p2].id) { list ->
                    viewModel.preview(notes[p2], list.first())
                    btnSend.setOnClickListener {
                        viewModel.exportNoteToAnki(requireActivity(), notes[p2], list)
                    }

                    Timber.d("Corpus: $list")
                }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {}

        }
    }

    private fun observe() = with(binding) {
        viewModel.previewState.collectOnStarted {
            when {
                it.loading -> {}
                it.cards != null -> {
                    val previewUI = viewModel.mapPreviewToUi(it.cards)
                    frontWebView.loadDataWithBaseURL(
                        null,
                        previewUI.first().frontHtml,
                        "text/html",
                        "utf-8",
                        null
                    )
                }

                it.error != null -> {}
            }

            Timber.d("PreviewState: $it")
        }
        viewModel.uiState.collectOnStarted {
            progressBar.isVisible = it is UiState.Loading

            when (it) {
                is UiState.Success -> {
                    Toast.makeText(
                        requireContext(),
                        "Successfully export ${it.noteTitle}, ${it.totalAdded} cards added to Anki",
                        Toast.LENGTH_LONG
                    ).show()
                }

                is UiState.Error -> {
                    Toast.makeText(
                        requireContext(),
                        "Error: ${it.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    Timber.e("Error: ${it.message}")
                }

                is UiState.AnkiNotInstalled -> {
                    Toast.makeText(
                        requireContext(),
                        "Anki is not installed",
                        Toast.LENGTH_LONG
                    ).show()
                }

                is UiState.PermissionRequired -> {
                    Toast.makeText(
                        requireContext(),
                        "Permission to access Anki is required",
                        Toast.LENGTH_LONG
                    ).show()
                }

                else -> {}
            }

            Timber.d("UiState: ${it::class.java.simpleName}")
        }
    }

}