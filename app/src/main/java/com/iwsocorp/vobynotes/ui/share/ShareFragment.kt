package com.iwsocorp.vobynotes.ui.share

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import com.google.firebase.auth.FirebaseAuth
import com.iwsocorp.vobynotes.MainActivity
import com.iwsocorp.vobynotes.R
import com.iwsocorp.vobynotes.core.common.BaseFragment
import com.iwsocorp.vobynotes.core.common.Utils.showAlertDialog
import com.iwsocorp.vobynotes.core.model.asSharedNote
import com.iwsocorp.vobynotes.databinding.FragmentShareBinding
import com.iwsocorp.vobynotes.ui.note.NoteBottomSheet
import com.iwsocorp.vobynotes.ui.note.NoteViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class ShareFragment : BaseFragment<FragmentShareBinding>(FragmentShareBinding::inflate) {

    private val viewModel: ShareViewModel by activityViewModels()
    private val noteViewModel: NoteViewModel by activityViewModels()
    private val adapter: ShareNoteAdapter by lazy {
        ShareNoteAdapter { sharedNote ->
            Timber.d("Clicked: $sharedNote")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupDropdowns()
        setupUI()
        observeState()
        onSearch()
    }

    private fun setupUI() = with(binding) {
        rvSharedNote.adapter = adapter
        floatingActionButton.setOnClickListener {
            onSearch()
            adapter.refresh()
        }

        viewModel.sharedNotes.collectOnStarted {
            Timber.d("Paging data: $it")
            adapter.submitData(viewLifecycleOwner.lifecycle, it)
        }
        adapter.addLoadStateListener { loadState ->
            Timber.d("Item count: ${adapter.itemCount}")
            val isEmpty = adapter.itemCount == 0 && loadState.refresh is LoadState.NotLoading
            tvEmpty.isVisible = isEmpty
            rvSharedNote.isVisible = !isEmpty
        }
    }

    private fun observeState() = viewModel.shareState.collectOnStarted { state ->
        binding.progressBar.isVisible = state is ShareState.Loading
        Timber.d("State: $state")

        when (state) {
            is ShareState.Shared -> {
                Timber.d("Shared: ${state.link}")
                Toast.makeText(requireContext(), state.link, Toast.LENGTH_SHORT).show()
            }

            is ShareState.Error -> {
                Timber.e(state.message)
                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
            }

            else -> {}
        }
    }

    private fun setupToolbar() = binding.toolbarShare.apply {
        title = getString(R.string.shared_notes)
        setNavigationIcon(R.drawable.baseline_menu_24)
        setNavigationOnClickListener {
            (requireActivity() as MainActivity).drawerLayout.openDrawer(GravityCompat.START)
        }
        menu.clear()
        inflateMenu(R.menu.menu_share)
        setOnMenuItemClickListener(menuListener)
    }

    private val menuListener = Toolbar.OnMenuItemClickListener {
        when (it.itemId) {
            R.id.action_share -> {
                onShare()
            }
        }
        true
    }

    private fun onShare() {
        val user = FirebaseAuth.getInstance().currentUser

        if (user == null) showAlertDialog(
            requireContext(),
            "You are not signed in",
            "Please sign in first",
            "Sign in",
            "Cancel",
        ) {
            findNavController().navigate(R.id.action_nav_share_to_authFragment)
        } else {
            var isBottomSheetShown = false

            noteViewModel.notes.observe(viewLifecycleOwner) { notes ->
                if (!isBottomSheetShown) {
                    isBottomSheetShown = true
                    NoteBottomSheet(notes) { note ->
                        showAlertDialog(
                            requireContext(),
                            (if (note.shared) "Update Shared " else "Share ") + note.title,
                            null,
                            "Share",
                            "Cancel",
                        ) {
                            if (note.shared) {
                                viewModel.updateSharedNote(
                                    noteId = note.id,
                                    title = note.title,
                                    wordLang = note.wordLang,
                                    meaningLang = note.meaningLang,
                                )
                            } else {
                                val sharedNote = note.asSharedNote(
                                    ownerId = user.uid,
                                    ownerAvatar = user.photoUrl.toString(),
                                    ownerName = user.displayName,
                                    content = emptyList()
                                )
                                viewModel.shareNote(sharedNote)
                            }
                            isBottomSheetShown = false
                            adapter.refresh()
                        }
                    }.show(childFragmentManager, null)
                }
            }
        }

    }

    private fun setupDropdowns() = with(binding) {
        val filterOptions = listOf("All", "English", "Indonesian", "Japanese")
        val sortOptions = listOf("Latest", "Popular")

        filterDropdown.setAdapter(
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                filterOptions
            )
        )
        sortDropdown.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, sortOptions)
        )

        filterDropdown.setText(viewModel.filter.ifEmpty { filterOptions.first() }, false)
        sortDropdown.setText(viewModel.sort.ifEmpty { sortOptions.first() }, false)
    }

    private fun onSearch() {
        val filter = binding.filterDropdown.text.toString()
        val sort = binding.sortDropdown.text.toString()

        val filterWordLang = when (filter) {
            "All" -> null
            "English" -> "EN"
            "Indonesian" -> "ID"
            "Japanese" -> "JP"
            else -> null
        }
        val sortBy = when (sort) {
            "Latest" -> "updatedAt"
            "Popular" -> "savedCount"
            else -> "updatedAt"
        }

        viewModel.getSharedNotes(
            filterWordLang = filterWordLang,
            sortBy = sortBy,
        )
        viewModel.updateFilter(filter)
        viewModel.updateSort(sort)
    }

    override fun onResume() {
        super.onResume()
        setupDropdowns()
    }

}