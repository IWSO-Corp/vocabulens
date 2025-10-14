package com.iwsocorp.vobynotes.ui.home

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.iwsocorp.vobynotes.R
import com.iwsocorp.vobynotes.core.common.BaseFragment
import com.iwsocorp.vobynotes.databinding.FragmentHomeBinding
import com.iwsocorp.vobynotes.ui.note.ARG_NOTE_ID
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding>(FragmentHomeBinding::inflate) {

    private val viewModel: HomeViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.uiState.collectOnStarted {
            binding.progressBar.isVisible = it is UiState.Loading

            if (it is UiState.Loaded) with(binding) {
                rvNote.adapter = NoteAdapter(it.notes, listener)
                tvEmpty.isVisible = it.notes.isEmpty()
            }
        }
    }

    private val listener = object : NoteAdapter.ClickListener {
        override fun onClick(pos: Int, noteId: String) = findNavController().navigate(
            R.id.action_nav_home_to_noteFragment,
            Bundle().apply { putString(ARG_NOTE_ID, noteId) }
        )

        override fun getAllCorpusSize(callback: (Int) -> Unit) =
            viewModel.allCorpus.collectOnStarted {
                callback(it.size)
            }

    }

}