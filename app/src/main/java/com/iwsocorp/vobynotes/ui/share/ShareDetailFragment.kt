package com.iwsocorp.vobynotes.ui.share

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.paging.PagingData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.iwsocorp.vobynotes.R
import com.iwsocorp.vobynotes.core.common.AlphabetSidebarHelper
import com.iwsocorp.vobynotes.core.common.BaseFragment
import com.iwsocorp.vobynotes.core.common.Utils.sharePublicNoteLink
import com.iwsocorp.vobynotes.core.common.Utils.showAlertDialog
import com.iwsocorp.vobynotes.core.model.Corpus
import com.iwsocorp.vobynotes.core.model.SharedNote
import com.iwsocorp.vobynotes.core.model.asCorpus
import com.iwsocorp.vobynotes.databinding.FragmentShareDetailBinding
import com.iwsocorp.vobynotes.ui.note.WordAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ShareDetailFragment :
    BaseFragment<FragmentShareDetailBinding>(FragmentShareDetailBinding::inflate) {

    private val viewModel: ShareDetailViewModel by viewModels()
    private var firebaseUser: FirebaseUser? = null
    private val adapter: WordAdapter by lazy {
        WordAdapter(false, object : WordAdapter.ClickListener {
            override fun onClick(corpus: Corpus) {}

            override fun onPlay(url: String) {}

            override fun onSelectionChanged(size: Int) {}

            override fun onMark(corpus: Corpus) {}

            override fun onEdit(corpus: Corpus) {}

        })
    }
    private val alphabetSidebarHelper: AlphabetSidebarHelper by lazy {
        AlphabetSidebarHelper(
            requireContext(),
            binding.alphabetSidebar,
            binding.rvCorpus,
            adapter
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.getString(NOTE_ID)?.let { viewModel.getSharedNote(it) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firebaseUser = FirebaseAuth.getInstance().currentUser

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
        viewModel.isSaved.collectOnStarted {
            if (it) binding.toolbarNote.menu.findItem(R.id.action_save)
                .setIcon(R.drawable.baseline_download_done_24)
        }

        alphabetSidebarHelper.observePagesUpdates()
    }

    private fun setupUI(sharedNote: SharedNote) {
        with(binding.toolbarNote) {
            title = sharedNote.title
            setNavigationIcon(R.drawable.baseline_arrow_back_24)
            setNavigationOnClickListener { findNavController().navigateUp() }
            menu.clear()
            inflateMenu(R.menu.menu_share_detail)
            setOnMenuItemClickListener(menuListener)
            firebaseUser?.let { user ->
                if (user.uid == sharedNote.ownerId) menu.removeItem(R.id.action_save)
            }
        }

        checkNoteSavedStatus(sharedNote)

        binding.rvCorpus.adapter = adapter
        binding.rvCorpus.addOnScrollListener(alphabetSidebarHelper.scrollListener)

        var counter = 0
        val corpusList = sharedNote.content.map { it.asCorpus() }.map {
            counter++
            it.copy(indexNumber = counter)
        }

        adapter.submitData(viewLifecycleOwner.lifecycle, PagingData.from(corpusList))
    }

    private fun checkNoteSavedStatus(sharedNote: SharedNote) = firebaseUser?.let { user ->
        viewModel.checkNoteSavedStatus(user.uid, sharedNote.id) {
            viewModel.setIsSaved(it)
        }
    }

    private val menuListener = Toolbar.OnMenuItemClickListener { menuItem ->
        when (menuItem.itemId) {
            R.id.action_save -> {
                firebaseUser?.let { user ->
                    onSave(user)
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

            R.id.action_share -> {
                viewModel.sharedNoteState.collectOnStarted { state ->
                    if (state is SharedNoteState.Loaded) {
                        requireContext().sharePublicNoteLink(
                            state.sharedNote.title,
                            "${shareLink}${state.sharedNote.id}"
                        )
                    }
                }
                true
            }

            else -> false
        }
    }

    private fun onSave(user: FirebaseUser) = viewModel.sharedNoteState.collectOnStarted { state ->
        val isSaved = viewModel.isSaved.value
        if (state is SharedNoteState.Loaded) {
            if (isSaved) showAlertDialog(
                requireContext(),
                "Update Saved Note",
                "Do you want to update ${state.sharedNote.title}",
                "Update",
                "Cancel",
            ) {
                viewModel.refreshSavedNote(state.sharedNote, {
                    Toast.makeText(
                        requireContext(),
                        "Existing $it",
                        Toast.LENGTH_SHORT,
                    ).show()
                }) {
                    Toast.makeText(
                        requireContext(),
                        "Imported ${it.successCount} items, duplicate $it",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } else showAlertDialog(
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
                viewModel.setIsSaved(true)
            }
        }
    }

    companion object {
        const val NOTE_ID = "noteId"
    }
}