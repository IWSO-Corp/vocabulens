package com.iwsocorp.vocabnotes.ui.note

import android.graphics.Typeface
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.iwsocorp.vocabnotes.R
import com.iwsocorp.vocabnotes.core.model.Corpus
import com.iwsocorp.vocabnotes.databinding.FragmentNoteBinding
import com.iwsocorp.vocabnotes.ui.detail.ARG_CORPUS_WORD
import com.iwsocorp.vocabnotes.ui.detail.DetailViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

const val ARG_NOTE_ID = "noteIdParam"
const val ARG_POSITION = "positionRecyclerView"

@AndroidEntryPoint
class NoteFragment() : Fragment() {

    private var mediaPlayer: MediaPlayer? = null
    private var _binding: FragmentNoteBinding? = null
    private val binding get() = _binding!!
    private val viewModel: NoteViewModel by viewModels()
    private val detailViewModel: DetailViewModel by activityViewModels()
    private val wordAdapter: WordAdapter by lazy {
        WordAdapter(true, object : WordAdapter.ClickListener {
            override fun onClick(corpus: Corpus) {
                findNavController().navigate(
                    R.id.action_noteFragment_to_corpusDetailFragment,
                    Bundle().apply {
                        putString(ARG_CORPUS_WORD, corpus.word)
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
        argNoteId?.let {
            viewModel.updateNoteId(it)
            if (it.isNotEmpty()) {
                viewModel.getNote(it)
                viewModel.note.observe(viewLifecycleOwner) { note ->
                    Timber.d("note: $note")
                    binding.tvToolbarTitle.text = note.title.ifEmpty { "Untitled" }
                    binding.tvWordLang.text = note.wordLang
                    binding.tvMeaningLang.text = note.meaningLang
                    viewModel.updateNoteTitle(note.title)
                }
            } else {
                binding.tvToolbarTitle.text = "All Words"
            }
        } ?: run {
            binding.tvToolbarTitle.text = "Untitled"
            binding.tvEmpty.visibility = View.VISIBLE
        }

        viewModel.noteId.observe(viewLifecycleOwner) {
            it?.let { noteId ->
                lifecycleScope.launch(Dispatchers.IO) {
                    if (noteId.isNotEmpty()) {
                        viewModel.getCorpusPagingDataFlow(noteId)
                            .collectLatest { corpusPagingData ->
                                withContext(Dispatchers.Main) {
                                    wordAdapter.submitData(corpusPagingData)
                                }
                            }
                    } else {
                        viewModel.getAllCorpus()
                            .collectLatest { corpusPagingData ->
                                withContext(Dispatchers.Main) {
                                    wordAdapter.submitData(corpusPagingData)
                                }
                            }
                    }
                }
            }
        }

        lifecycleScope.launch {
            wordAdapter.loadStateFlow.collectLatest { loadStates ->
                val alphabetSet = extractAvailableLettersFromLoadedPages()
                populateAlphabetSidebar(alphabetSet)
                updateUI()
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (wordAdapter.getSelectedItems().isNotEmpty()) {
                wordAdapter.clearSelection()
            } else {
                isEnabled = false
                requireActivity().onBackPressed()
            }
        }

        setupUI(argNoteId)
    }

    private fun setupUI(argNoteId: String?) = with(binding) {
        setNormalToolbar()

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
    }

    private fun setNormalToolbar() = with(binding) {
        tvToolbarTitle.text = viewModel.noteTitle.value
            ?: (if (viewModel.noteId.value == null) "Untitled" else "All Vocabulary")
        tvToolbarTitle.setOnClickListener {
            tvToolbarTitle.visibility = View.GONE
            etToolbarTitle.visibility = View.VISIBLE
            etToolbarTitle.setText(tvToolbarTitle.text)
        }
        toolbarNote.apply {
            setNavigationIcon(R.drawable.baseline_arrow_back_24)
            setNavigationOnClickListener {
                parentFragmentManager.popBackStack()
            }
            menu.clear()
            inflateMenu(R.menu.menu_note)
            setOnMenuItemClickListener(menuListener)
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
            setOnMenuItemClickListener(menuListener)
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
        when (item?.itemId) {
            R.id.action_move -> {
                Toast.makeText(requireContext(), "Move", Toast.LENGTH_SHORT).show()
            }

            R.id.action_mark -> {
                Toast.makeText(requireContext(), "Mark", Toast.LENGTH_SHORT).show()
            }

            R.id.action_share -> {
                Toast.makeText(requireContext(), "Share", Toast.LENGTH_SHORT).show()
            }

            R.id.action_delete -> {
                Toast.makeText(requireContext(), "Delete", Toast.LENGTH_SHORT).show()
            }

            R.id.action_delete_note -> {
                AlertDialog.Builder(requireContext())
                    .setTitle("Delete Note")
                    .setMessage("Are you sure you want to delete this note and all its contents?")
                    .setPositiveButton("Delete") { _, _ ->
                        viewModel.noteId.observe(viewLifecycleOwner) {
                            it?.let { noteId -> viewModel.deleteNote(noteId) }
                        }
                        parentFragmentManager.popBackStack()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
        true
    }

    private fun updateUI() {
        wordAdapter.addLoadStateListener {
            val isLoading = it.source.refresh is LoadState.Loading
            _binding?.let {
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                binding.tvEmpty.isVisible = !isLoading && wordAdapter.snapshot().isEmpty()
                binding.rvCorpus.addOnScrollListener(scrollListener)
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
        detailViewModel.setCorpusList(currentList.map { it.word })
        Timber.d("currentList size: ${currentList.size}")
        return currentList.map {
            it.word.first().uppercaseChar()
        }.distinct().sorted()
    }

    // Populate the sidebar dynamically with the available letters
    private fun populateAlphabetSidebar(alphabetSet: List<Char>) {
        _binding?.let {
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
        val word = binding.edWord.text.toString().trim()
        val meaning = binding.edMeaning.text.toString().trim()
        if (word.isEmpty() || meaning.isEmpty()) return

        val corpus = Corpus(
            word = word,
            noteId = viewModel.noteId.value ?: "",
            meaning = meaning,
            wordLang = worldLang,
            meaningLang = meaningLang,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentNoteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        resetMediaPlayer()
    }
}