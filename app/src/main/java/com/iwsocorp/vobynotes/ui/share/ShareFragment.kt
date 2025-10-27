package com.iwsocorp.vobynotes.ui.share

import android.os.Bundle
import android.view.View
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
        observeState()
        setupRecyclerview()
    }

    private fun setupRecyclerview() {
        binding.rvSharedNote.adapter = adapter
        viewModel.getSharedNotes().collectOnStarted {
            adapter.submitData(it)
        }
        adapter.addLoadStateListener { loadState ->
            val isEmpty = adapter.itemCount == 0 && loadState.refresh is LoadState.NotLoading
            binding.tvEmpty.isVisible = isEmpty
            binding.rvSharedNote.isVisible = !isEmpty
        }
    }

    private fun observeState() = viewModel.shareState.collectOnStarted { state ->
        binding.progressBar.isVisible = state is ShareState.Loading
        Timber.d("State: $state")

        when (state) {
            is ShareState.Shared -> {
                Timber.d("Shared: ${state.link}")
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

            R.id.action_filter -> {}
            R.id.action_sort -> {}
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
        } else noteViewModel.notes.observe(viewLifecycleOwner) { notes ->
            NoteBottomSheet(notes) { note ->
                showAlertDialog(
                    requireContext(),
                    "Share ${note.title}",
                    null,
                    "Share",
                    "Cancel",
                ) {
                    val sharedNote = note.asSharedNote(
                        ownerId = user.uid,
                        ownerAvatar = user.photoUrl.toString(),
                        ownerName = user.displayName,
                        content = emptyList()
                    )
                    viewModel.shareNote(sharedNote)
                }
            }.show(childFragmentManager, null)
        }
    }

}