package com.iwsocorp.vobynotes.ui.trash

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.iwsocorp.vobynotes.R
import com.iwsocorp.vobynotes.core.common.AlphabetSidebarHelper
import com.iwsocorp.vobynotes.core.common.BaseFragment
import com.iwsocorp.vobynotes.core.common.Utils.showAlertDialog
import com.iwsocorp.vobynotes.core.model.Corpus
import com.iwsocorp.vobynotes.databinding.FragmentTrashNoteBinding
import com.iwsocorp.vobynotes.ui.note.ARG_NOTE_ID
import com.iwsocorp.vobynotes.ui.note.WordAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job

@AndroidEntryPoint
class TrashNoteFragment :
    BaseFragment<FragmentTrashNoteBinding>(FragmentTrashNoteBinding::inflate) {

    private val viewModel: TrashNoteViewModel by viewModels()
    private val adapter: WordAdapter by lazy {
        WordAdapter(false, object : WordAdapter.ClickListener {
            override fun onClick(corpus: Corpus) {}

            override fun onPlay(url: String) {}

            override fun onSelectionChanged(size: Int) {}

            override fun onMark(corpus: Corpus) {}

            override fun onEdit(corpus: Corpus) {}

        })
    }
    private val sidebar: AlphabetSidebarHelper by lazy {
        AlphabetSidebarHelper(
            requireContext(),
            binding.alphabetSidebar,
            binding.rvCorpus,
            adapter
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.getString(ARG_NOTE_ID)?.let {
            setupUI(it)
        }

        sidebar.observePagesUpdates()

        binding.toolbarTrashNote.apply {
            setNavigationOnClickListener {
                findNavController().navigateUp()
            }
            menu.clear()
            inflateMenu(R.menu.menu_trash_note)
        }
        binding.rvCorpus.adapter = adapter
        binding.rvCorpus.addOnScrollListener(sidebar.scrollListener)
    }

    private fun setupUI(string: String): Job = viewModel.getNote(string) { note ->
        binding.toolbarTrashNote.apply {
            title = note.title
            setOnMenuItemClickListener(menuListener(note.id))
        }
        viewModel.getPagedCorpus(note.id).collectOnStarted {
            adapter.submitData(it)
        }
    }

    private fun menuListener(noteId: String) = Toolbar.OnMenuItemClickListener { menuItem ->
        when (menuItem.itemId) {
            R.id.action_restore -> {
                viewModel.restoreNotes(listOf(noteId))
                findNavController().navigateUp()
                true
            }

            R.id.action_delete_forever -> {
                showAlertDialog(
                    requireContext(),
                    "Delete Note Forever",
                    "Delete note and their content forever?",
                    "Delete Forever",
                    "Cancel",
                ) {
                    viewModel.deleteNotes(listOf(noteId))
                    findNavController().navigateUp()
                }
                true
            }

            else -> false
        }
    }

}