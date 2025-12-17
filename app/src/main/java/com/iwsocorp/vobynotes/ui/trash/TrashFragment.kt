package com.iwsocorp.vobynotes.ui.trash

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
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
import com.iwsocorp.vobynotes.databinding.FragmentTrashBinding
import com.iwsocorp.vobynotes.ui.home.NoteAdapter
import com.iwsocorp.vobynotes.ui.home.UiState
import com.iwsocorp.vobynotes.ui.note.ARG_NOTE_ID
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class TrashFragment : BaseFragment<FragmentTrashBinding>(FragmentTrashBinding::inflate) {

    private val viewModel: TrashViewModel by activityViewModels()
    private lateinit var adapter: NoteAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = NoteAdapter(
            object : NoteAdapter.ClickListener {
                override fun onClick(pos: Int, noteId: String) {
                    findNavController().navigate(
                        R.id.action_nav_trash_to_trashNoteFragment,
                        bundleOf(
                            ARG_NOTE_ID to noteId
                        )
                    )
                }

                override fun getAllCorpusSize(callback: (Int) -> Unit) {}
                override fun onSelectionChanged(size: Int) {
                    if (size > 0) {
                        setSelectionToolbar(size)
                    } else {
                        setNormalToolbar()
                    }
                }

                override fun getLastFiveCorpus(
                    noteId: String,
                    callback: (List<Corpus>) -> Unit
                ) {
                    viewModel.getLastFiveCorpus(noteId, callback)
                }
            },
            true
        )

        setNormalToolbar()
        observeState()

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (adapter.getSelectedItems().isNotEmpty()) {
                adapter.clearSelection()
            } else {
                isEnabled = false
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }

        binding.rvNote.adapter = adapter
        binding.toolbarTrash.apply {
            setOnMenuItemClickListener(menuListener)
            overflowIcon?.setTint(ContextCompat.getColor(requireContext(), R.color.onPrimary))
        }
        adapter.addOnPagesUpdatedListener {
            val isNotEmpty = adapter.itemCount > 0
            binding.tvEmpty.isVisible = !isNotEmpty
            binding.toolbarTrash.apply {
                menu.clear()
                if (isNotEmpty) inflateMenu(R.menu.menu_trash)
            }
        }
    }

    private fun observeState() = viewModel.uiState.collectOnStarted { state ->
        binding.progressBar.isVisible = state is UiState.Loading

        if (state is UiState.Loaded) withContext(Dispatchers.Main) {
            adapter.submitData(state.notesPaging)
        }
    }

    private fun setNormalToolbar() = binding.toolbarTrash.apply {
        title = "Trash"
        setNavigationIcon(R.drawable.baseline_menu_24)
        setNavigationOnClickListener {
            (requireActivity() as MainActivity).drawerLayout.openDrawer(GravityCompat.START)
        }
        menu.clear()
        if (adapter.itemCount > 0) inflateMenu(R.menu.menu_trash)
    }

    private fun setSelectionToolbar(size: Int) = binding.toolbarTrash.apply {
        title = "$size selected"
        setNavigationIcon(R.drawable.baseline_close_24)
        setNavigationOnClickListener {
            adapter.clearSelection()
            setNormalToolbar()
        }
        menu.clear()
        inflateMenu(R.menu.menu_trash_selection)
    }

    private val menuListener = Toolbar.OnMenuItemClickListener { menuItem ->
        val items = adapter.getSelectedItems().map { it.id }

        when (menuItem.itemId) {
            R.id.action_restore -> {
                viewModel.restoreNotes(items)
                adapter.clearSelection()

                Snackbar.make(
                    binding.root,
                    "Restored ${items.size} notes",
                    Snackbar.LENGTH_SHORT
                ).setAction("Undo") {
                    viewModel.moveNotesToTrash(items)
                }.show()
            }

            R.id.action_delete_forever -> {
                showAlertDialog(
                    requireContext(),
                    "Delete ${items.size} Notes Forever",
                    "Delete notes and their content forever?",
                    "Delete Forever",
                    "Cancel",
                ) {
                    viewModel.deleteNotes(items)
                    adapter.clearSelection()
                }
            }

            R.id.action_empty_trash -> {
                showAlertDialog(
                    requireContext(),
                    "Empty Trash",
                    "All notes in trash will be permanently deleted.",
                    "Empty Trash",
                    "Cancel",
                ) {
                    val allTrashIds = adapter.snapshot().items.map { it.note.id }
                    viewModel.deleteNotes(allTrashIds)
                    adapter.clearSelection()
                }
            }
        }

        true
    }

}