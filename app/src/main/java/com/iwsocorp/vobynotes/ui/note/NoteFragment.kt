package com.iwsocorp.vobynotes.ui.note

import android.graphics.Typeface
import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.iwsocorp.vobynotes.R
import com.iwsocorp.vobynotes.core.common.BaseFragment
import com.iwsocorp.vobynotes.core.common.Utils.alertInputDialog
import com.iwsocorp.vobynotes.core.common.Utils.setIconColor
import com.iwsocorp.vobynotes.core.common.Utils.showAlertDialog
import com.iwsocorp.vobynotes.core.common.Utils.showPopupMenu
import com.iwsocorp.vobynotes.core.model.Corpus
import com.iwsocorp.vobynotes.core.model.Mark
import com.iwsocorp.vobynotes.core.model.SortBy
import com.iwsocorp.vobynotes.core.model.SortOrder
import com.iwsocorp.vobynotes.databinding.FragmentNoteBinding
import com.iwsocorp.vobynotes.ui.detail.ARG_CORPUS_ID
import com.iwsocorp.vobynotes.ui.detail.DetailViewModel
import com.iwsocorp.vobynotes.ui.home.HomeViewModel
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
    private val wordAdapter: WordAdapter by lazy {
        WordAdapter(true, object : WordAdapter.ClickListener {
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
    }

    @OptIn(FlowPreview::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setFragmentResultListener("requestKey") { _, bundle ->
            val position = bundle.getInt(ARG_POSITION)
            binding.rvCorpus.scrollToPosition(
                if (position <= 4) position else position - 4
            )
        }

        val argNoteId = arguments?.getString(ARG_NOTE_ID)
        Timber.d("argNoteId: $argNoteId")
        argNoteId?.let {
            viewModel.updateNoteId(it)
            if (it.isNotEmpty()) {
                viewModel.getNote(it)
            } else {
                binding.tvToolbarTitle.text = "All Vocabulary"
            }
        } ?: run {
            binding.tvToolbarTitle.text = "Untitled"
            binding.tvToolbarTitle.setTextColor(
                resources.getColor(R.color.grey, null)
            )
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
            }
            binding.iconSwitch.isVisible = id == null
        }
        viewModel.note.observe(viewLifecycleOwner) { note ->
            Timber.d("note: $note")
            binding.tvToolbarTitle.text = note.title
            binding.tvWordLang.text = note.wordLang
            binding.tvMeaningLang.text = note.meaningLang
            viewModel.updateNoteTitle(note.title)
        }

        wordAdapter.loadStateFlow.collectOnStarted {
            val alphabetSet = extractAvailableLettersFromLoadedPages()
            populateAlphabetSidebar(alphabetSet)
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
            val worldLang = tvWordLang.text.toString()
            val meaningLang = tvMeaningLang.text.toString()
            tvWordLang.text = meaningLang
            tvMeaningLang.text = worldLang
        }
        btnScrollToTop.setOnClickListener {
            rvCorpus.scrollToPosition(0)
        }
    }

    private fun setNormalToolbar() = with(binding) {
        tvToolbarTitle.text = viewModel.note.value?.title ?: ""
        tvToolbarTitle.setOnClickListener {
            tvToolbarTitle.visibility = View.GONE
            etToolbarTitle.visibility = View.VISIBLE
            etToolbarTitle.setText(tvToolbarTitle.text)
        }
        toolbarNote.apply {
            setIconColor(requireContext())
            setNavigationIcon(R.drawable.baseline_arrow_back_24)
            setNavigationOnClickListener {
                parentFragmentManager.popBackStack()
            }
            menu.clear()
            inflateMenu(R.menu.menu_note)
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
                val notes = viewModel.notes.value
                notes?.let { list ->
                    NoteBottomSheet(list.filterNot { it.id == viewModel.noteId.value }, { note ->
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
                        }
                    }.show(childFragmentManager, null)
                }
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
                        "Familiar" to { markWords(Mark.FAMILIAR) },
                        "Unfamiliar" to { markWords(Mark.UNFAMILIAR) },
                        "Unmark" to { markWords(Mark.UNMARKED) }
                    )
                )
            }

            R.id.action_delete -> {
                showAlertDialog(
                    requireContext(),
                    "Delete ${selectedItemIds.size} Words",
                    null,
                    "Delete",
                    "Cancel"
                ) {
                    viewModel.deleteCorpusBatch(selectedItemIds)
                    setNormalToolbar()
                }
            }

            R.id.action_share -> {
                Toast.makeText(requireContext(), "Share", Toast.LENGTH_SHORT).show()
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
        rvCorpus.addOnScrollListener(scrollListener)

        viewModel.queryState.collectOnStarted { state ->
            Timber.d("queryState: $state")
            svAlphabet.isVisible = state.sortBy == SortBy.WORD
            btnScrollToTop.isVisible = state.sortBy == SortBy.UPDATED_AT

            val menuFilter = toolbarNote.menu.findItem(R.id.action_filter)
            menuFilter.setIcon(
                if (state.mark != null) {
                    R.drawable.baseline_filter_alt_24
                } else {
                    R.drawable.outline_filter_alt_24
                }
            )
            val drawable = menuFilter.icon
            drawable?.let {
                DrawableCompat.setTint(
                    it,
                    ContextCompat.getColor(
                        requireContext(),
                        when (state.mark) {
                            Mark.FAMILIAR -> R.color.blue
                            Mark.UNFAMILIAR -> R.color.red
                            else -> R.color.black
                        }
                    )
                )
            }
        }
    }

    private val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)

            val layoutManager = recyclerView.layoutManager as LinearLayoutManager
            val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
            val data = wordAdapter.snapshot().items

            if (firstVisiblePosition != RecyclerView.NO_POSITION && firstVisiblePosition < data.size) {
                val firstCorpus = data[firstVisiblePosition]
                if (firstCorpus.word.isEmpty()) return

                val firstLetter = firstCorpus.word.first().uppercaseChar()

                highlightCurrentLetterInSidebar(firstLetter)
            }
        }
    }

    private fun highlightCurrentLetterInSidebar(currentLetter: Char) {
        // Reset all letters to normal
        for (i in 0 until binding.alphabetSidebar.childCount) {
            val textView = binding.alphabetSidebar.getChildAt(i) as TextView
            textView.apply {
                setTextColor(resources.getColor(R.color.grey, null))
                setTypeface(null, Typeface.NORMAL)
            }
        }

        // Find the matching letter in the sidebar and make it bold
        for (i in 0 until binding.alphabetSidebar.childCount) {
            val textView = binding.alphabetSidebar.getChildAt(i) as TextView
            if (textView.text.toString() == currentLetter.toString()) {
                textView.apply {
                    textSize = 20f
                    setTextColor(resources.getColor(R.color.black, null))
                    setTypeface(null, Typeface.BOLD)
                }
                break
            }
        }
    }

    // Function to extract available letters from currently loaded pages
    private fun extractAvailableLettersFromLoadedPages(): List<Char> {
        val currentList = wordAdapter.snapshot().items

        detailViewModel.setCorpusList(currentList)
        Timber.d("currentList size: ${currentList.size}")

        val letters = currentList.map {
            if (it.word.isEmpty()) return emptyList()
            it.word.first().uppercaseChar()
        }.distinct().sorted()

        return letters
    }

    // Populate the sidebar dynamically with the available letters
    private fun populateAlphabetSidebar(alphabetSet: List<Char>) {
        binding.alphabetSidebar.removeAllViews()
        alphabetSet.forEach { letter ->
            val textView = TextView(requireContext()).apply {
                text = letter.toString()
                textSize = 20f
                setOnClickListener {
                    scrollToLetter(letter)
                }
            }
            binding.alphabetSidebar.addView(textView)
        }
    }

    // Scroll to the first item starting with the selected letter
    private fun scrollToLetter(letter: Char) {
        val position = wordAdapter.snapshot().items.indexOfFirst {
            it.word.first().uppercaseChar() == letter
        }
        if (position != -1) {
            val layoutManager = binding.rvCorpus.layoutManager as LinearLayoutManager
            layoutManager.scrollToPositionWithOffset(position, 0)
        }
    }

    private fun onSubmit() {
        viewModel.updateNoteTitle(binding.tvToolbarTitle.text.toString())
        val worldLang = binding.tvWordLang.text.toString()
        val meaningLang = binding.tvMeaningLang.text.toString()
        val word = binding.edWord.text.toString().lowercase().trim()
        val meaning = binding.edMeaning.text.toString().lowercase().trim()
        if (word.isEmpty() || meaning.isEmpty()) return

        val corpus = Corpus(
            noteId = viewModel.noteId.value ?: "",
            word = word,
            meaning = meaning,
            wordLang = worldLang,
            meaningLang = meaningLang,
        )

        viewModel.insertCorpus(corpus) {
            Toast.makeText(
                requireContext(),
                if (it == -1L) "The word already exists in all vocabulary" else "Added",
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
    }
}