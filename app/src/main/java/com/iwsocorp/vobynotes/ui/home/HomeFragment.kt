package com.iwsocorp.vobynotes.ui.home

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.iwsocorp.vobynotes.MainActivity
import com.iwsocorp.vobynotes.R
import com.iwsocorp.vobynotes.core.common.BaseFragment
import com.iwsocorp.vobynotes.core.common.CorpusFileManager
import com.iwsocorp.vobynotes.core.common.Utils.sharePublicNoteLink
import com.iwsocorp.vobynotes.core.common.Utils.showAlertDialog
import com.iwsocorp.vobynotes.core.model.Corpus
import com.iwsocorp.vobynotes.core.model.asSharedNote
import com.iwsocorp.vobynotes.databinding.FragmentHomeBinding
import com.iwsocorp.vobynotes.databinding.ImportBottomSheetBinding
import com.iwsocorp.vobynotes.ui.note.ARG_NOTE_ID
import com.iwsocorp.vobynotes.ui.setting.ARG_TITLE
import com.iwsocorp.vobynotes.ui.setting.ImportViewModel
import com.iwsocorp.vobynotes.ui.share.ShareState
import com.iwsocorp.vobynotes.ui.share.ShareViewModel
import com.iwsocorp.vobynotes.ui.share.shareLink
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setNormalToolbar()
        setOnBack()
        observeState()

        binding.toolbarHome.apply {
            setOnMenuItemClickListener(menuListener)
            overflowIcon?.setTint(ContextCompat.getColor(requireContext(), R.color.black))
        }
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

        if (uiState is UiState.Loaded) with(binding) {
            adapter.submitData(viewLifecycleOwner.lifecycle, uiState.notesPaging)
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
                showImportBottomSheet()
            }

            R.id.action_share -> {
                onShare()
            }

            R.id.action_export -> {
                val items = adapter.getSelectedItems()

                AlertDialog.Builder(requireContext())
                    .setTitle("Export ${items.first().title}")
                    .setPositiveButton("Export") { _, _ ->
                        exportExcel(items.first().title)
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
                val note = adapter.snapshot().items.find { it.note.id == id }!!.note
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

    private val import = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult

        val fileName = fileManager.getFileName(uri)
        val inputStream = requireContext().contentResolver.openInputStream(uri)
        if (inputStream == null) return@registerForActivityResult

        val data = fileManager.readFile(
            inputStream,
            fileName ?: ""
        ) {
            if (!it) {
                Toast.makeText(requireContext(), "Invalid file format", Toast.LENGTH_SHORT)
                    .show()
                return@readFile
            }
        }

        if (data.isEmpty()) {
            Toast.makeText(requireContext(), "File is empty", Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }

        importViewModel.setImportedData(data)

        findNavController().navigate(
            R.id.action_nav_home_to_importFragment,
            bundleOf(
                ARG_TITLE to fileName
            )
        )
    }

    private fun pickExcelFile() = import.launch(
        arrayOf(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-excel",
            "text/csv"
        )
    )

    private val export = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
        if (uri == null) return@registerForActivityResult

        val outputStream = requireContext().contentResolver.openOutputStream(uri)
        if (outputStream == null) return@registerForActivityResult

        val note = adapter.getSelectedItems().first()
        viewModel.getCorpusByNoteId(note.id) {
            fileManager.exportCorpusList(outputStream, it)
            adapter.clearSelection()
            Toast.makeText(requireContext(), "${note.title} exported", Toast.LENGTH_SHORT).show()
        }
    }

    fun exportExcel(noteTitle: String) {
        fileManager.setExportFormat(CorpusFileManager.ExportFormat.XLSX)
        export.launch("$noteTitle.xlsx")
    }

    @SuppressLint("InflateParams")
    private fun showImportBottomSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.import_bottom_sheet, null)
        val binding = ImportBottomSheetBinding.bind(view)
        dialog.setContentView(view)

        binding.cardSaved.setOnClickListener {
            fileManager.setImportFormat(CorpusFileManager.ImportFormat.SAVED)
            dialog.dismiss()
            pickExcelFile()
        }
        binding.cardCustom.setOnClickListener {
            fileManager.setImportFormat(CorpusFileManager.ImportFormat.CUSTOM)
            dialog.dismiss()
            pickExcelFile()
        }
        binding.iconClose.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        sharedViewModel.setIdle()
    }

}