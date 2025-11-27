package com.iwsocorp.vobynotes.ui.home

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.Toolbar
import androidx.core.os.bundleOf
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.iwsocorp.vobynotes.MainActivity
import com.iwsocorp.vobynotes.R
import com.iwsocorp.vobynotes.core.common.BaseFragment
import com.iwsocorp.vobynotes.core.common.CorpusFileManager
import com.iwsocorp.vobynotes.core.common.FilePickerManager
import com.iwsocorp.vobynotes.core.common.Utils.sharePublicNoteLink
import com.iwsocorp.vobynotes.core.common.Utils.showAlertDialog
import com.iwsocorp.vobynotes.core.model.Corpus
import com.iwsocorp.vobynotes.core.model.asSharedNote
import com.iwsocorp.vobynotes.databinding.FragmentHomeBinding
import com.iwsocorp.vobynotes.ui.note.ARG_NOTE_ID
import com.iwsocorp.vobynotes.ui.setting.ARG_TITLE
import com.iwsocorp.vobynotes.ui.setting.ImportViewModel
import com.iwsocorp.vobynotes.ui.share.ShareState
import com.iwsocorp.vobynotes.ui.share.ShareViewModel
import com.iwsocorp.vobynotes.ui.share.shareLink
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding>(FragmentHomeBinding::inflate) {

    private val viewModel: HomeViewModel by activityViewModels()
    private val sharedViewModel: ShareViewModel by activityViewModels()
    private val importViewModel: ImportViewModel by activityViewModels()

    private val adapter: NoteAdapter by lazy {
        NoteAdapter(
            object : NoteAdapter.ClickListener {
                override fun onClick(pos: Int, noteId: String) = findNavController().navigate(
                    R.id.action_nav_home_to_noteFragment,
                    Bundle().apply { putString(ARG_NOTE_ID, noteId) }
                )

                override fun getAllCorpusSize(callback: (Int) -> Unit) =
                    viewModel.allCorpus.collectOnStarted {
                        callback(it.size)
                    }

                override fun onSelectionChanged(size: Int) {
                    if (size > 0) {
                        setSelectionToolbar(size)
                    } else {
                        setNormalToolbar()
                    }
                }

                override fun getLastFiveCorpus(
                    noteId: String,
                    callback: (List<Corpus>) -> Unit
                ) {
                    viewModel.getLastFiveCorpus(noteId, callback)
                }

            }
        )
    }

    @Inject
    lateinit var fileManager: CorpusFileManager
    private lateinit var pickerManager: FilePickerManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pickerManager = FilePickerManager(
            caller = this,
            context = requireContext(),
            fileManager = fileManager,
            onImportComplete = { data, fileName ->
                importViewModel.setImportedData(data)
                findNavController().navigate(
                    R.id.action_nav_home_to_importFragment,
                    bundleOf(ARG_TITLE to fileName)
                )
            },
            onExportComplete = { stream, name ->
                val note = adapter.getSelectedItems().firstOrNull() ?: return@FilePickerManager
                viewModel.getCorpusByNoteId(note.id) {
                    fileManager.exportCorpusList(stream, it)
                    adapter.clearSelection()
                }
            }
        )

        setNormalToolbar()
        setOnBack()
        observeState()

        binding.toolbarHome.apply {
            setOnMenuItemClickListener(menuListener)
        }
        binding.rvNote.itemAnimator = null
        binding.rvNote.adapter = adapter
        adapter.loadStateFlow.collectOnStarted {
            binding.tvEmpty.isVisible = adapter.itemCount == 0
        }

        fileManager.progress.collectOnStarted { progress ->
            Timber.d("File progress: $progress%")
        }
        viewModel.deletedNoteId.collectOnStarted { ids ->
            Snackbar.make(
                requireView(),
                "Moved ${ids.size} notes to trash",
                Snackbar.LENGTH_SHORT
            ).setAction("Undo") {
                viewModel.restoreNotes(ids)
            }.setAnchorView(
                (requireActivity() as MainActivity).binding.appBarMain.fab
            ).show()
        }
        sharedViewModel.setIdle()
    }

    private fun observeState() = combine(
        viewModel.uiState,
        sharedViewModel.shareState
    ) { uiState, shareState ->
        uiState to shareState
    }.collectOnStarted { (uiState, shareState) ->
        binding.progressBar.isVisible =
            uiState is UiState.Loading || shareState is ShareState.Loading

        if (uiState is UiState.Loaded) withContext(Dispatchers.Main) {
            adapter.submitData(uiState.notesPaging)
        }

        if (shareState is ShareState.Shared) requireContext().sharePublicNoteLink(
            shareState.noteTitle,
            shareState.link
        )

        Timber.d(
            "UiState: ${
                when (uiState) {
                    is UiState.Idle -> "Idle"
                    is UiState.Loading -> "Loading"
                    is UiState.Loaded -> "Loaded"
                }
            }"
        )
        Timber.d(
            "ShareState: ${
                when (shareState) {
                    is ShareState.Idle -> "Idle"
                    is ShareState.Loading -> "Loading"
                    is ShareState.Shared -> "Shared"
                    is ShareState.Error -> "Error"
                    is ShareState.Loaded -> "Loaded"
                }
            }"
        )
    }

    private fun setOnBack() = requireActivity().onBackPressedDispatcher.addCallback(
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (adapter.getSelectedItems().isNotEmpty()) {
                    adapter.clearSelection()
                } else {
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }

        }
    )

    private fun setNormalToolbar() = binding.toolbarHome.apply {
        title = "Home"
        setNavigationIcon(R.drawable.baseline_menu_24)
        setNavigationOnClickListener {
            (requireActivity() as MainActivity).drawerLayout.openDrawer(GravityCompat.START)
        }
        menu.clear()
        inflateMenu(R.menu.main)
    }

    private fun setSelectionToolbar(size: Int) = binding.toolbarHome.apply {
        title = "$size selected"
        setNavigationIcon(R.drawable.baseline_close_24)
        setNavigationOnClickListener {
            adapter.clearSelection()
            setNormalToolbar()
        }
        menu.clear()
        inflateMenu(R.menu.home_selection)
        if (size > 1) menu.apply {
            removeItem(R.id.action_share)
            removeItem(R.id.action_export)
        }
    }

    private val menuListener = Toolbar.OnMenuItemClickListener { menuItem ->
        when (menuItem.itemId) {
            R.id.action_search -> {
                findNavController().navigate(R.id.action_nav_home_to_searchFragment)
            }

            R.id.action_import -> {
                pickerManager.showImportBottomSheet()
            }

            R.id.action_share -> {
                onShare()
            }

            R.id.action_export -> {
                val items = adapter.getSelectedItems()

                AlertDialog.Builder(requireContext())
                    .setTitle("Export ${items.first().title}")
                    .setPositiveButton("Export") { _, _ ->
                        pickerManager.exportExcel(items.first().title)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }

            R.id.action_delete -> {
                val items = adapter.getSelectedItems().map { it.id }

                showAlertDialog(
                    requireContext(),
                    "Move ${items.size} Notes to Trash",
                    null,
                    "Move to Trash",
                    "Cancel"
                ) {
                    viewModel.moveNotesToTrash(items)
                    adapter.clearSelection()
                }
            }
        }
        true
    }

    private fun onShare() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) showAlertDialog(
            requireContext(),
            "You are not logged in",
            "You must be logged in to share note",
            "Login",
            "Cancel"
        ) {
            adapter.clearSelection()
            findNavController().navigate(R.id.action_nav_home_to_authFragment)
        } else {
            val noteId = adapter.getSelectedItems().map { it.id }.firstOrNull()
            noteId?.let { id ->
                val note = adapter.snapshot().items.firstOrNull { it.note.id == id }?.note ?: return
                if (note.shared) requireContext().sharePublicNoteLink(
                    note.title,
                    shareLink + note.id
                ) else showAlertDialog(
                    requireContext(),
                    "This note is not shared",
                    "Share this note to public?",
                    "Share",
                    "Cancel"
                ) {
                    sharedViewModel.shareNote(
                        note.asSharedNote(
                            user.uid,
                            if (user.photoUrl != null) user.photoUrl.toString() else null,
                            user.displayName,
                            emptyList()
                        )
                    )
                    adapter.clearSelection()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        sharedViewModel.setIdle()
    }

}