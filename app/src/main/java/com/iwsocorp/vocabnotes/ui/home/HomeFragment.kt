package com.iwsocorp.vocabnotes.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.iwsocorp.vocabnotes.R
import com.iwsocorp.vocabnotes.core.model.Note
import com.iwsocorp.vocabnotes.databinding.FragmentHomeBinding
import com.iwsocorp.vocabnotes.ui.note.ARG_NOTE_ID
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.notes.observe(viewLifecycleOwner) {
            Timber.d("notes: $it")
            setupRecyclerView(it)

            binding.tvEmpty.visibility = if (it.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun setupRecyclerView(notes: List<Note>) {
        val listener = object : NoteAdapter.ClickListener {
            override fun onClick(noteId: String) {
                navigate(noteId)
            }
        }
        binding.rvNote.adapter = NoteAdapter(notes.sortedByDescending { it.updatedAt }, listener)
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