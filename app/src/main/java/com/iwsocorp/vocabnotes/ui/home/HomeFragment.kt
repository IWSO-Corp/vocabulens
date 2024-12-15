package com.iwsocorp.vocabnotes.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import com.iwsocorp.vocabnotes.R
import com.iwsocorp.vocabnotes.databinding.FragmentHomeBinding
import com.iwsocorp.vocabnotes.ui.note.ARG_NOTE_ID
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by activityViewModels()
    private val noteAdapter: NoteAdapter by lazy {
        NoteAdapter(viewModel, object : NoteAdapter.ClickListener {
            override fun onClick(pos: Int, noteId: String) {
                navigate(noteId)
            }

            override fun onLongClick(pos: Int, noteId: String) {
                noteAdapter.toggleSelection(pos)
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            viewModel.getAllNotes().collectLatest {
                withContext(Dispatchers.Main) {
                    noteAdapter.submitData(it)
                }
            }
        }
        lifecycleScope.launch {
            noteAdapter.loadStateFlow.collectLatest {
                noteAdapter.addLoadStateListener {
                    val isLoading = it.source.refresh is LoadState.Loading
                    _binding?.let {
                        binding.tvEmpty.isVisible = !isLoading && noteAdapter.snapshot().isEmpty()
                    }
                }
            }
        }

        binding.rvNote.adapter = noteAdapter
    }

    private fun navigate(noteId: String) {
        findNavController().navigate(
            R.id.action_nav_home_to_noteFragment,
            Bundle().apply { putString(ARG_NOTE_ID, noteId) }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}