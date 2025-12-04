package com.iwsocorp.vobynotes.ui.note

import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import com.google.firebase.auth.FirebaseAuth
import com.iwsocorp.vobynotes.R
import com.iwsocorp.vobynotes.core.common.AlphabetSidebarHelper
import com.iwsocorp.vobynotes.core.common.BaseFragment
import com.iwsocorp.vobynotes.core.common.Utils.alertInputDialog
import com.iwsocorp.vobynotes.core.common.Utils.langCode
import com.iwsocorp.vobynotes.core.common.Utils.langName
import com.iwsocorp.vobynotes.core.common.Utils.sharePublicNoteLink
import com.iwsocorp.vobynotes.core.common.Utils.showAlertDialog
import com.iwsocorp.vobynotes.core.common.Utils.showPopupMenu
import com.iwsocorp.vobynotes.core.model.Corpus
import com.iwsocorp.vobynotes.core.model.Mark
import com.iwsocorp.vobynotes.core.model.Note
import com.iwsocorp.vobynotes.core.model.SortBy
import com.iwsocorp.vobynotes.core.model.SortOrder
import com.iwsocorp.vobynotes.core.model.asSharedNote
import com.iwsocorp.vobynotes.databinding.FragmentNoteBinding
import com.iwsocorp.vobynotes.ui.detail.ARG_CORPUS_ID
import com.iwsocorp.vobynotes.ui.detail.DetailViewModel
import com.iwsocorp.vobynotes.ui.home.HomeViewModel
import com.iwsocorp.vobynotes.ui.setting.ARG_ID
import com.iwsocorp.vobynotes.ui.setting.ARG_MEANING_LANG
import com.iwsocorp.vobynotes.ui.setting.ARG_TITLE
import com.iwsocorp.vobynotes.ui.setting.ARG_WORD_LANG
import com.iwsocorp.vobynotes.ui.setting.LanguageViewModel
import com.iwsocorp.vobynotes.ui.setting.SettingsViewModel
import com.iwsocorp.vobynotes.ui.share.ShareState
import com.iwsocorp.vobynotes.ui.share.ShareViewModel
import com.iwsocorp.vobynotes.ui.share.shareLink
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

const val ARG_NOTE_ID = "noteIdParam"
const val ARG_POSITION = "positionRecyclerView"

@AndroidEntryPoint
class NoteFragment() : BaseFragment<FragmentNoteBinding>(
    FragmentNoteBinding::inflate
) {

    private var mediaPlayer: MediaPlayer? = null
    private val viewModel: NoteViewModel by viewModels()
    private val homeViewModel: HomeViewModel by activityViewModels()
    private val detailViewModel: DetailViewModel by activityViewModels()
    private val sharedViewModel: ShareViewModel by activityViewModels()
    private val languageViewModel: LanguageViewModel by activityViewModels()
    private val settingsViewModel: SettingsViewModel by activityViewModels()

    private lateinit var wordAdapter: WordAdapter
    private lateinit var alphabetSidebarHelper: AlphabetSidebarHelper
    private val argNoteId: String? by lazy {
        arguments?.getString(ARG_NOTE_ID)
    }

    @OptIn(FlowPreview::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        wordAdapter = WordAdapter(true, object : WordAdapter.ClickListener {
            override fun onClick(corpus: Corpus) {
                findNavController().navigate(
                    R.id.action_noteFragment_to_corpusDetailFragment,
                    Bundle().apply {
                        putString(ARG_CORPUS_ID, corpus.id)
                        putInt(ARG_POSITION, wordAdapter.snapshot().items.indexOf(corpus))
                    }
                )
            }

            override fun onPlay(url: String) {
                lifecycleScope.launch(Dispatchers.IO) {
                    if (url.isNotEmpty()) playAudio(url)
                }
            }

            override fun onSelectionChanged(size: Int) {
                if (size > 0) {
                    setSelectionToolbar(size)
                } else {
                    setNormalToolbar()
                }
            }

            override fun onMark(corpus: Corpus) {
                viewModel.updateCorpusMark(
                    listOf(corpus.id),
                    when (corpus.mark) {
                        Mark.UNMARKED -> Mark.FAMILIAR
                        Mark.FAMILIAR -> Mark.UNFAMILIAR
                        Mark.UNFAMILIAR -> Mark.UNMARKED
                    }
                )
            }

            override fun onEdit(corpus: Corpus) {
                findNavController().navigate(
                    R.id.action_noteFragment_to_editDetailFragment,
                    Bundle().apply {
                        putString(ARG_CORPUS_ID, corpus.id)
                    }
                )
            }
        })
        alphabetSidebarHelper = AlphabetSidebarHelper(
            requireContext(),
            binding.alphabetSidebar,
            binding.rvCorpus,
            wordAdapter
        )

        setFragmentResultListener("requestKey") { _, bundle ->
            val position = bundle.getInt(ARG_POSITION)
            binding.rvCorpus.scrollToPosition(
                if (position <= 4) position else position - 4
            )
        }

        Timber.d("argNoteId: $argNoteId")
        argNoteId?.let {
            viewModel.updateNoteId(it)
            if (it.isNotEmpty()) {
                viewModel.getNote(it)
            } else {
                binding.tvToolbarTitle.apply {
                    text = "All Vocabulary"
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                }
            }
        } ?: run {
            binding.tvToolbarTitle.apply {
                text = "Untitled"
                setTextColor(ContextCompat.getColor(requireContext(), R.color.grey))
            }
            binding.tvEmpty.visibility = View.VISIBLE
        }

        setupUI(argNoteId)

        viewModel.noteId.observe(viewLifecycleOwner) { id ->
            id?.let { noteId ->
                viewModel.getPagedCorpus(noteId).collectOnStarted { corpusPagingData ->
                    Timber.d("corpusPagingData: $corpusPagingData")
                    withContext(Dispatchers.Main) {
                        wordAdapter.submitData(corpusPagingData)
                    }
                }
                binding.tvWordLang.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                binding.tvMeaningLang.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                binding.tvWordLang.setOnClickListener(null)
                binding.tvMeaningLang.setOnClickListener(null)
            } ?: run {
                binding.tvWordLang.setCompoundDrawablesWithIntrinsicBounds(
                    0,
                    0,
                    R.drawable.baseline_arrow_drop_down_24,
                    0
                )
                binding.tvMeaningLang.setCompoundDrawablesWithIntrinsicBounds(
                    0,
                    0,
                    R.drawable.baseline_arrow_drop_down_24,
                    0
                )
            }
            binding.iconSwitch.isVisible = id == null
        }
        viewModel.note.observe(viewLifecycleOwner) { note ->
            Timber.d("note: $note")
            binding.tvToolbarTitle.text = note.title
            binding.tvWordLang.text = note.wordLang.langName(requireContext())
            binding.tvMeaningLang.text = note.meaningLang.langName(requireContext())
            viewModel.updateNoteTitle(note.title)
        }

        alphabetSidebarHelper.observePagesUpdates()
        wordAdapter.loadStateFlow.collectOnStarted {
            val currentList = wordAdapter.snapshot().items
            detailViewModel.setCorpusList(currentList)
            updateUI(it)
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (wordAdapter.getSelectedItems().isNotEmpty()) {
                wordAdapter.clearSelection()
            } else {
                isEnabled = false
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }

        sharedViewModel.setIdle()
        sharedViewModel.shareState.collectOnStarted { state ->
            binding.progressBar.isVisible = state is ShareState.Loading

            if (state is ShareState.Shared) requireContext().sharePublicNoteLink(
                state.noteTitle,
                state.link
            )
        }
    }

    private fun setupUI(argNoteId: String?) = with(binding) {
        setNormalToolbar()

        toolbarNote.setOnMenuItemClickListener(menuListener)
        etToolbarTitle.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                saveToolbarTitle()
                true
            } else {
                false
            }
        }
        etToolbarTitle.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) saveToolbarTitle()
        }
        csAdd.isVisible = (viewModel.noteId.value == null) || (viewModel.noteId.value != "")
        rvCorpus.adapter = wordAdapter
        btnAdd.setOnClickListener {
            onSubmit()
        }
        iconSwitch.isVisible = argNoteId == null
        iconSwitch.setOnClickListener {
            languageViewModel.switchLanguage()
        }
        btnScrollToTop.setOnClickListener {
            rvCorpus.scrollToPosition(0)
        }

        languageViewModel.sourceLanguage.collectOnStarted {
            tvWordLang.text = it?.langName(requireContext())
        }
        languageViewModel.translationLanguage.collectOnStarted {
            tvMeaningLang.text = it?.langName(requireContext())
        }

        tvWordLang.setOnClickListener {
            LangBottomSheet("Word Language") {
                languageViewModel.setSourceLanguage(it.code)
            }.show(parentFragmentManager, null)
        }
        tvMeaningLang.setOnClickListener {
            LangBottomSheet("Meaning Language") {
                languageViewModel.setTranslationLanguage(it.code)
            }.show(parentFragmentManager, null)
        }
    }

    private fun setNormalToolbar() = with(binding) {
        viewModel.note.observe(viewLifecycleOwner) {
            tvToolbarTitle.apply {
                text = it.title
            }
            toolbarNote.apply {
                menu.clear()
                inflateMenu(R.menu.menu_note)
            }
        }
        if (argNoteId?.isNotEmpty() == true || argNoteId == null) tvToolbarTitle.setOnClickListener {
            tvToolbarTitle.visibility = View.GONE
            etToolbarTitle.visibility = View.VISIBLE
            etToolbarTitle.setText(tvToolbarTitle.text)
            etToolbarTitle.requestFocus()
        }
        toolbarNote.apply {
            setNavigationIcon(R.drawable.baseline_arrow_back_24)
            setNavigationOnClickListener {
                parentFragmentManager.popBackStack()
            }
            menu.clear()
            argNoteId?.let {
                inflateMenu(R.menu.menu_note)
                if (it.isEmpty()) {
                    menu.removeItem(R.id.action_share)
                    menu.removeItem(R.id.action_delete_note)
                }
            }
        }
    }

    private fun setSelectionToolbar(size: Int) = with(binding) {
        tvToolbarTitle.apply {
            text = "$size selected"
            setOnClickListener(null)
        }
        toolbarNote.apply {
            setNavigationIcon(R.drawable.baseline_close_24)
            setNavigationOnClickListener {
                wordAdapter.clearSelection()
                setNormalToolbar()
            }
            menu.clear()
            inflateMenu(R.menu.menu_note_selection)
        }
    }

    private fun saveToolbarTitle() = with(binding) {
        tvToolbarTitle.text = etToolbarTitle.text.toString().ifEmpty { "Untitled" }
        etToolbarTitle.visibility = View.GONE
        tvToolbarTitle.visibility = View.VISIBLE
        val noteTitle = tvToolbarTitle.text.toString()
        if (viewModel.note.value != null && viewModel.note.value!!.title != noteTitle) {
            viewModel.updateNote(viewModel.note.value!!.copy(title = noteTitle))
        }
    }

    private val menuListener = Toolbar.OnMenuItemClickListener { item ->
        val selectedItemIds = wordAdapter.getSelectedItems()

        when (item.itemId) {
            R.id.action_filter -> {
                fun onFilter(mark: Mark?) = viewModel.setFilter(mark)

                showPopupMenu(
                    requireContext(),
                    binding.toolbarNote.findViewById(R.id.action_filter),
                    listOf(
                        "Familiar" to { onFilter(Mark.FAMILIAR) },
                        "Unfamiliar" to { onFilter(Mark.UNFAMILIAR) },
                        "Unmarked" to { onFilter(Mark.UNMARKED) },
                        "Clear Filter" to { onFilter(null) }
                    )
                )
            }

            R.id.action_sort -> {
                fun onSort(sortBy: SortBy) = viewModel.setSort(
                    sortBy,
                    if (sortBy == SortBy.WORD) SortOrder.ASC else SortOrder.DESC
                )

                showPopupMenu(
                    requireContext(),
                    binding.toolbarNote.findViewById(R.id.action_sort),
                    listOf(
                        "Sort Alphabetically" to { onSort(SortBy.WORD) },
                        "Sort By Time" to { onSort(SortBy.UPDATED_AT) }
                    )
                )
            }

            R.id.action_move -> {
                NoteBottomSheet(settingsViewModel.notes.value!!, {
                    val note = Note(
                        title = "New Note",
                        wordLang = wordAdapter.getSelectedItemLang(),
                        meaningLang = wordAdapter.getSelectedItemMeaningLang(),
                        contentSize = 0
                    )
                    requireContext().alertInputDialog(note.title) {
                        val newNote = if (note.title == it) note else note.copy(title = it)
                        viewModel.createNote(newNote)
                        viewModel.moveCorpusToNote(selectedItemIds, newNote.id)
                        wordAdapter.clearSelection()
                    }
                }) { note ->
                    showAlertDialog(
                        requireContext(),
                        "Move ${selectedItemIds.size} Words to ${note.title}",
                        null,
                        "Move",
                        "Cancel"
                    ) {
                        viewModel.moveCorpusToNote(selectedItemIds, note.id)
                        wordAdapter.clearSelection()

                        Toast.makeText(
                            requireContext(),
                            "${selectedItemIds.size} words moved to ${note.title}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }.show(childFragmentManager, null)
            }

            R.id.action_mark -> {
                fun markWords(mark: Mark) = showAlertDialog(
                    requireContext(),
                    "Mark ${selectedItemIds.size} Words as $mark",
                    null,
                    "Mark",
                    "Cancel"
                ) {
                    viewModel.updateCorpusMark(selectedItemIds, mark)
                    setNormalToolbar()
                }

                showPopupMenu(
                    requireContext(),
                    binding.toolbarNote.findViewById(R.id.action_mark),
                    listOf(
                        "Familiar" to {
                            markWords(Mark.FAMILIAR)
                            wordAdapter.clearSelection()
                        },
                        "Unfamiliar" to {
                            markWords(Mark.UNFAMILIAR)
                            wordAdapter.clearSelection()
                        },
                        "Unmark" to {
                            markWords(Mark.UNMARKED)
                            wordAdapter.clearSelection()
                        }
                    )
                )
            }

            R.id.action_delete -> {
                showAlertDialog(
                    requireContext(),
                    "Delete ${selectedItemIds.size} Words",
                    "This action cannot be undone",
                    "Delete",
                    "Cancel"
                ) {
                    viewModel.deleteCorpusBatch(selectedItemIds)
                    wordAdapter.clearSelection()
                }
            }

            R.id.action_edit -> {
                val note = viewModel.note.value
                findNavController().navigate(
                    R.id.action_noteFragment_to_importFragment,
                    bundleOf(
                        ARG_ID to note?.id,
                        ARG_TITLE to note?.title,
                        ARG_WORD_LANG to note?.wordLang,
                        ARG_MEANING_LANG to note?.meaningLang
                    )
                )
            }

            R.id.action_share -> {
                val user = FirebaseAuth.getInstance().currentUser
                if (user == null) showAlertDialog(
                    requireContext(),
                    "You are not logged in",
                    "You must be logged in to share note",
                    "Login",
                    "Cancel"
                ) {
                    findNavController().navigate(R.id.action_noteFragment_to_authFragment)
                } else viewModel.note.observe(viewLifecycleOwner) { note ->
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
                        ) {}
                    }
                }
            }

            R.id.action_delete_note -> {
                showAlertDialog(
                    requireContext(),
                    "Delete Note",
                    "Are you sure you want to delete this note and all its contents?",
                    "Move to Trash",
                    "Cancel"
                ) {
                    val id = viewModel.noteId.value
                    id?.let { noteId ->
                        homeViewModel.moveNotesToTrash(listOf(noteId))
                        findNavController().popBackStack()
                    }
                }
            }
        }

        true
    }

    private fun updateUI(loadStates: CombinedLoadStates) = with(binding) {
        val isLoading = loadStates.refresh is LoadState.Loading
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        tvEmpty.isVisible = !isLoading && wordAdapter.snapshot().isEmpty()
        rvCorpus.addOnScrollListener(alphabetSidebarHelper.scrollListener)

        viewModel.queryState.collectOnStarted { state ->
            Timber.d("queryState: $state")
            svAlphabet.isVisible = state.sortBy == SortBy.WORD
            btnScrollToTop.isVisible = state.sortBy == SortBy.UPDATED_AT

            val menuFilter = toolbarNote.menu.findItem(R.id.action_filter)
            menuFilter?.setIcon(
                if (state.mark != null) {
                    R.drawable.baseline_filter_alt_24
                } else {
                    R.drawable.outline_filter_alt_24
                }
            )
            val drawable = menuFilter?.icon
            drawable?.let {
                DrawableCompat.setTint(
                    it,
                    ContextCompat.getColor(
                        requireContext(),
                        when (state.mark) {
                            Mark.FAMILIAR -> R.color.blue
                            Mark.UNFAMILIAR -> R.color.red
                            else -> R.color.onPrimary
                        }
                    )
                )
            }
        }
    }

    private fun onSubmit() {
        val wordLang = binding.tvWordLang.text.toString()
        val meaningLang = binding.tvMeaningLang.text.toString()
        val word = binding.edWord.text.toString().lowercase().trim()
        val meaning = binding.edMeaning.text.toString().lowercase().trim()
        if (word.isEmpty() || meaning.isEmpty()) return

        viewModel.updateNoteTitle(binding.tvToolbarTitle.text.toString())

        val corpus = Corpus(
            noteId = viewModel.noteId.value ?: "",
            word = word,
            meaning = meaning,
            wordLang = wordLang.langCode(requireContext()),
            meaningLang = meaningLang.langCode(requireContext()),
        )

        viewModel.insertCorpus(corpus) {
            Toast.makeText(
                requireContext(),
                if (it == -1L) "The word already exists with same language" else "Added",
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.edWord.text?.clear()
        binding.edMeaning.text?.clear()
    }

    private fun playAudio(url: String) {
        mediaPlayer?.release()
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(url)
                prepareAsync()
                setOnPreparedListener {
                    start()
                    Timber.d("Audio started playing")
                }
                setOnCompletionListener {
                    resetMediaPlayer()
                    Timber.d("Audio finished playing")
                }
                setOnErrorListener { _, what, extra ->
                    resetMediaPlayer()
                    Timber.e("Error occurred while playing audio: what=$what, extra=$extra")
                    true
                }
            }
        } else {
            mediaPlayer?.start()
        }
    }

    private fun resetMediaPlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        resetMediaPlayer()
        sharedViewModel.setIdle()
    }
}