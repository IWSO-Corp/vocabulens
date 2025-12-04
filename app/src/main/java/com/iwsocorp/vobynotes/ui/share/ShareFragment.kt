package com.iwsocorp.vobynotes.ui.share

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.Toolbar
import androidx.core.os.bundleOf
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import com.google.firebase.auth.FirebaseAuth
import com.iwsocorp.vobynotes.MainActivity
import com.iwsocorp.vobynotes.R
import com.iwsocorp.vobynotes.core.common.BaseFragment
import com.iwsocorp.vobynotes.core.common.Utils.langCode
import com.iwsocorp.vobynotes.core.common.Utils.loadLanguages
import com.iwsocorp.vobynotes.core.common.Utils.sharePublicNoteLink
import com.iwsocorp.vobynotes.core.common.Utils.showAlertDialog
import com.iwsocorp.vobynotes.core.model.asSharedNote
import com.iwsocorp.vobynotes.databinding.FragmentShareBinding
import com.iwsocorp.vobynotes.ui.note.NoteBottomSheet
import com.iwsocorp.vobynotes.ui.setting.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

@AndroidEntryPoint
class ShareFragment : BaseFragment<FragmentShareBinding>(FragmentShareBinding::inflate) {

    private val viewModel: ShareViewModel by activityViewModels()
    private val settingsViewModel: SettingsViewModel by activityViewModels()

    private val adapter: ShareNoteAdapter by lazy {
        ShareNoteAdapter { sharedNote, view ->
            viewModel.updateRecyclerPosition(binding.rvSharedNote.getChildAdapterPosition(view))

            findNavController().navigate(
                R.id.action_nav_share_to_shareDetailFragment,
                bundleOf(ShareDetailFragment.NOTE_ID to sharedNote.id)
            )
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
        binding.nestedScroll.post {
            val topView = binding.rvSharedNote.getChildAt(viewModel.recyclerPosition)
            topView?.let {
                binding.nestedScroll.smoothScrollTo(0, it.top)
            }
        }

        floatingActionButton.setOnClickListener {
            onSearch()
            adapter.refresh()
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

        when (state) {
            is ShareState.Loaded -> {
                withContext(Dispatchers.Main) {
                    adapter.submitData(state.notes)
                }
            }

            is ShareState.Shared -> {
                requireContext().sharePublicNoteLink(state.noteTitle, state.link)

                Timber.d("Shared: ${state.link}")
                Toast.makeText(requireContext(), "Note shared", Toast.LENGTH_SHORT).show()
            }

            is ShareState.Error -> {
                Timber.e(state.message)
                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
            }

            else -> {}
        }

        Timber.d(
            "State: ${
                when (state) {
                    is ShareState.Idle -> "Idle"
                    is ShareState.Loading -> "Loading"
                    is ShareState.Loaded -> "Loaded"
                    is ShareState.Shared -> "Shared"
                    is ShareState.Error -> "Error"
                }
            }"
        )
    }

    private fun setupToolbar() {
        binding.toolbarShare.apply {
            title = getString(R.string.shared_notes)
            setNavigationIcon(R.drawable.baseline_menu_24)
            setNavigationOnClickListener {
                viewModel.updateRecyclerPosition(0)
                (requireActivity() as MainActivity).drawerLayout.openDrawer(GravityCompat.START)
            }
            menu.clear()
            inflateMenu(R.menu.menu_share)
            setOnMenuItemClickListener(menuListener)
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    viewModel.updateRecyclerPosition(0)
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        )
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
            NoteBottomSheet(settingsViewModel.notes.value!!) { note ->
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
                            ownerAvatar = if (user.photoUrl != null) user.photoUrl.toString() else null,
                            ownerName = user.displayName,
                            content = emptyList()
                        )
                        viewModel.shareNote(sharedNote) {}
                    }
                    adapter.refresh()
                }
            }.show(childFragmentManager, null)
        }
    }

    private fun setupDropdowns() = with(binding) {
        val filterOptions = listOf("All") + loadLanguages(requireContext()).map { it.name }
        val sortOptions = listOf("Latest", "Popular")

        sortDropdown.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, sortOptions)
        )

        filterDropdown.setText(viewModel.filter.ifEmpty { filterOptions.first() }, false)
        sortDropdown.setText(viewModel.sort.ifEmpty { sortOptions.first() }, false)

        filterDropdown.addTextChangedListener { text ->
            val filtered = filterOptions.filter { it.contains(text.toString(), ignoreCase = true) }
            filterDropdown.setAdapter(
                ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    if (text.toString().trim().isEmpty() || text.toString()
                            .lowercase() == "all"
                    ) filterOptions else filtered
                )
            )
            if (text.toString().trim().isEmpty()) filterDropdown.showDropDown()
        }
    }

    private fun onSearch() {
        val filter = binding.filterDropdown.text.toString()
        val sort = binding.sortDropdown.text.toString()

        val filterWordLang = when (filter) {
            "All" -> null
            else -> filter.langCode(requireContext())
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