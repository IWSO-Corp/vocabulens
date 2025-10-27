package com.iwsocorp.vobynotes.ui.home

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.iwsocorp.vobynotes.MainActivity
import com.iwsocorp.vobynotes.R
import com.iwsocorp.vobynotes.core.common.BaseFragment
import com.iwsocorp.vobynotes.core.common.Utils.showAlertDialog
import com.iwsocorp.vobynotes.core.model.Corpus
import com.iwsocorp.vobynotes.databinding.FragmentHomeBinding
import com.iwsocorp.vobynotes.ui.note.ARG_NOTE_ID
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding>(FragmentHomeBinding::inflate) {

    private val viewModel: HomeViewModel by activityViewModels()
    private val adapter: NoteAdapter by lazy {
        NoteAdapter(
            object : NoteAdapter.ClickListener {
                override fun onClick(pos: Int, noteId: String) = findNavController().navigate(
                    R.id.action_nav_home_to_noteFragment,
                    Bundle().apply { putString(ARG_NOTE_ID, noteId) }
                )

                override fun getAllCorpusSize(callback: (Int) -> Unit) =
                    viewModel.allCorpus.collectOnStarted {
                        callback(it.size)
                    }

                override fun onSelectionChanged(size: Int) {
                    if (size > 0) {
                        setSelectionToolbar(size)
                    } else {
                        setNormalToolbar()
                    }
                }

            }
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setNormalToolbar()
        setOnBack()
        observeState()

        binding.toolbarHome.apply {
            setOnMenuItemClickListener(menuListener)
            overflowIcon?.setTint(ContextCompat.getColor(requireContext(), R.color.black))
        }

        viewModel.deletedNoteId.collectOnStarted { ids ->
            Snackbar.make(
                requireView(),
                "Moved ${ids.size} notes to trash",
                Snackbar.LENGTH_SHORT
            ).setAction("Undo") {
                viewModel.restoreNotes(ids)
            }.setAnchorView(
                (requireActivity() as MainActivity).binding.appBarMain.fab
            ).show()
        }
    }

    private fun observeState() = viewModel.uiState.collectOnStarted {
        binding.progressBar.isVisible = it is UiState.Loading

        if (it is UiState.Loaded) with(binding) {
            adapter.submitList(it.notes)
            rvNote.adapter = adapter
            tvEmpty.isVisible = it.notes.isEmpty()
        }
    }

    private fun setOnBack() = requireActivity().onBackPressedDispatcher.addCallback(
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (adapter.getSelectedItems().isNotEmpty()) {
                    adapter.clearSelection()
                } else {
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }

        }
    )

    private fun setNormalToolbar() = binding.toolbarHome.apply {
        title = "Home"
        setNavigationIcon(R.drawable.baseline_menu_24)
        setNavigationOnClickListener {
            (requireActivity() as MainActivity).drawerLayout.openDrawer(GravityCompat.START)
        }
        menu.clear()
        inflateMenu(R.menu.main)
    }

    private fun setSelectionToolbar(size: Int) = binding.toolbarHome.apply {
        title = "$size selected"
        setNavigationIcon(R.drawable.baseline_close_24)
        setNavigationOnClickListener {
            adapter.clearSelection()
            setNormalToolbar()
        }
        menu.clear()
        inflateMenu(R.menu.home_selection)
        if (size > 1) menu.apply {
            removeItem(R.id.action_share)
            removeItem(R.id.action_export)
        }
    }

    private val menuListener = Toolbar.OnMenuItemClickListener {
        when (it.itemId) {
            R.id.action_search -> {
                findNavController().navigate(R.id.action_nav_home_to_searchFragment)
            }

            R.id.action_import -> {
                pickExcelFile()
            }

            R.id.action_share -> {}
            R.id.action_export -> {}
            R.id.action_delete -> {
                val items = adapter.getSelectedItems()

                showAlertDialog(
                    requireContext(),
                    "Move ${items.size} Notes to Trash",
                    null,
                    "Move to Trash",
                    "Cancel"
                ) {
                    viewModel.moveNotesToTrash(items)
                    adapter.clearSelection()
                }
            }
        }
        true
    }

    private val openDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { result ->
        result?.let { uri ->
            val inputStream = requireActivity().contentResolver.openInputStream(uri)
            inputStream?.let { stream ->
                val data: List<Corpus> = viewModel.readExcelFile(stream).distinctBy { it.word }
                Timber.d("data size: ${data.size}")

                if (data.isEmpty()) {
                    Toast.makeText(requireContext(), "Invalid file data", Toast.LENGTH_SHORT).show()
                    return@let
                }

                viewModel.importCorpusBatch(
                    fileName = viewModel.getFileName(
                        requireActivity().contentResolver,
                        uri
                    ),
                    corpusBatch = data,
                    existingCount = {
                        Toast.makeText(requireContext(), "Existing $it", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Toast.makeText(
                        requireContext(),
                        "Imported ${it.successCount} items, duplicate ${it.failedCount}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun pickExcelFile() {
        openDocumentLauncher.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
    }

}