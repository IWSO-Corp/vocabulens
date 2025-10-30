package com.iwsocorp.vobynotes.ui.share

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.paging.PagingData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.iwsocorp.vobynotes.R
import com.iwsocorp.vobynotes.core.common.AlphabetSidebarHelper
import com.iwsocorp.vobynotes.core.common.BaseFragment
import com.iwsocorp.vobynotes.core.common.Utils.showAlertDialog
import com.iwsocorp.vobynotes.core.model.Corpus
import com.iwsocorp.vobynotes.core.model.SharedNote
import com.iwsocorp.vobynotes.core.model.asCorpus
import com.iwsocorp.vobynotes.databinding.FragmentShareDetailBinding
import com.iwsocorp.vobynotes.ui.note.WordAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

@AndroidEntryPoint
class ShareDetailFragment :
    BaseFragment<FragmentShareDetailBinding>(FragmentShareDetailBinding::inflate) {

    private val viewModel: ShareDetailViewModel by viewModels()
    private val firebaseUser = FirebaseAuth.getInstance().currentUser
    private val adapter: WordAdapter by lazy {
        WordAdapter(false, object : WordAdapter.ClickListener {
            override fun onClick(corpus: Corpus) {}

            override fun onPlay(url: String) {}

            override fun onSelectionChanged(size: Int) {}

            override fun onMark(corpus: Corpus) {}

            override fun onEdit(corpus: Corpus) {}

        })
    }
    private lateinit var alphabetSidebarHelper: AlphabetSidebarHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.getString(NOTE_ID)?.let { viewModel.getSharedNote(it) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        alphabetSidebarHelper = AlphabetSidebarHelper(
            requireContext(),
            binding.alphabetSidebar,
            binding.rvCorpus,
        )

        viewModel.sharedNoteState.collectOnStarted { state ->
            binding.progressBar.isVisible = state is SharedNoteState.Loading
            binding.tvEmpty.isVisible = state is SharedNoteState.Error

            when (state) {
                is SharedNoteState.Loaded -> setupUI(state.sharedNote)
                is SharedNoteState.Error -> {
                    binding.tvEmpty.text = "Note is deleted or not found"
                    Toast.makeText(
                        requireContext(),
                        state.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }

                else -> {}
            }
        }
        viewModel.saveState.collectOnStarted {
            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupUI(sharedNote: SharedNote) {
        with(binding.toolbarNote) {
            title = sharedNote.title
            setNavigationIcon(R.drawable.baseline_arrow_back_24)
            setNavigationOnClickListener { findNavController().navigateUp() }
            inflateMenu(R.menu.menu_share_detail)
            setOnMenuItemClickListener(menuListener)
            if (firebaseUser?.uid == sharedNote.ownerId) menu.clear()
        }

        binding.rvCorpus.adapter = adapter
        binding.rvCorpus.addOnScrollListener(scrollListener)

        var counter = 0
        val corpusList = sharedNote.content.map { it.asCorpus() }.map {
            counter++
            it.copy(indexNumber = counter)
        }

        adapter.submitData(viewLifecycleOwner.lifecycle, PagingData.from(corpusList))
        adapter.loadStateFlow.collectOnStarted {
            val items = adapter.snapshot().items
            Timber.d("items: ${items.size}")
            withContext(Dispatchers.Main) {
                alphabetSidebarHelper.updateSidebarFromData(items)
            }
        }
    }

    private val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)

            val layoutManager = recyclerView.layoutManager as LinearLayoutManager
            val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
            val data = adapter.snapshot().items

            if (firstVisiblePosition != RecyclerView.NO_POSITION && firstVisiblePosition < data.size) {
                val firstCorpus = data[firstVisiblePosition]
                if (firstCorpus.word.isEmpty()) return

                val firstLetter = firstCorpus.word.first().uppercaseChar()

                alphabetSidebarHelper.highlightCurrentLetter(firstLetter)
            }
        }
    }

    private val menuListener = Toolbar.OnMenuItemClickListener { menuItem ->
        when (menuItem.itemId) {
            R.id.action_save -> {
                firebaseUser?.let { user ->
                    viewModel.sharedNoteState.collectOnStarted { state ->
                        if (state is SharedNoteState.Loaded) showAlertDialog(
                            requireContext(),
                            "Save Shared Note",
                            "Save ${state.sharedNote.title} note?",
                            "Save",
                        ) {
                            viewModel.saveSharedNote(
                                user.uid,
                                state.sharedNote,
                                {
                                    Toast.makeText(
                                        requireContext(),
                                        "Existing $it",
                                        Toast.LENGTH_SHORT,
                                    ).show()
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
                } ?: run {
                    showAlertDialog(
                        requireContext(),
                        "You are not signed in",
                        "Please sign in first",
                        "Sign in",
                        "Cancel",
                    ) {
                        findNavController().navigate(R.id.action_shareDetailFragment_to_authFragment)
                    }
                }
                true
            }

            else -> false
        }
    }

    companion object {
        const val NOTE_ID = "noteId"
    }
}