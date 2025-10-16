package com.iwsocorp.vobynotes.ui.trash

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.iwsocorp.vobynotes.MainActivity
import com.iwsocorp.vobynotes.R
import com.iwsocorp.vobynotes.core.common.BaseFragment
import com.iwsocorp.vobynotes.databinding.FragmentTrashBinding
import com.iwsocorp.vobynotes.ui.home.NoteAdapter
import com.iwsocorp.vobynotes.ui.home.UiState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TrashFragment : BaseFragment<FragmentTrashBinding>(FragmentTrashBinding::inflate) {

    private val viewModel: TrashViewModel by activityViewModels()
    private val adapter: NoteAdapter by lazy {
        NoteAdapter(
            object : NoteAdapter.ClickListener {
                override fun onClick(pos: Int, noteId: String) {
                    findNavController().navigate(
                        R.id.action_nav_trash_to_trashNoteFragment,
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
            }
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
    }

    private fun observeState() = viewModel.uiState.collectOnStarted { state ->
        binding.progressBar.isVisible = state is UiState.Loading

        if (state is UiState.Loaded) with(binding) {
            val isEmpty = state.notes.isEmpty()

            rvNote.isVisible = !isEmpty
            tvEmpty.isVisible = isEmpty

            adapter.submitList(state.notes)
            rvNote.adapter = adapter
        }
    }

    private fun setNormalToolbar() = binding.toolbarTrash.apply {
        title = "Trash"
        setNavigationIcon(R.drawable.baseline_menu_24)
        setNavigationOnClickListener {
            (requireActivity() as MainActivity).drawerLayout.openDrawer(GravityCompat.START)
        }
        setOnMenuItemClickListener(menuListener)
        menu.clear()
        inflateMenu(R.menu.menu_trash)
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

    private val menuListener = Toolbar.OnMenuItemClickListener {
        val items = adapter.getSelectedItems()

        when (it.itemId) {
            R.id.action_restore -> {

            }

            R.id.action_delete_forever -> {

            }

            R.id.action_empty_trash -> {

            }
        }

        true
    }

}