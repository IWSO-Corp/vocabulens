package com.iwsocorp.vocabnotes.ui.note

import android.graphics.Typeface
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
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
import com.iwsocorp.vocabnotes.core.common.Utils.generateRandomString
import com.iwsocorp.vocabnotes.core.model.Corpus
import com.iwsocorp.vocabnotes.core.model.Note
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
    private val argNoteId: String? by lazy {
        arguments?.getString(ARG_NOTE_ID)
    }
    private val wordAdapter: WordAdapter by lazy {
        WordAdapter(object : WordAdapter.ClickListener {
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

        argNoteId?.let {
            viewModel.updateNoteId(it)
            if (it.isNotEmpty()) {
                viewModel.getNote(it) { note ->
                    Timber.d("note: $note")
                    binding.toolbarNote.title = note.title
                    binding.tvWordLang.text = note.wordLang
                    binding.tvMeaningLang.text = note.meaningLang
                }
            } else {
                binding.toolbarNote.title = "New Note"
            }
        } ?: run {
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

        binding.rvCorpus.adapter = wordAdapter
        binding.btnAdd.setOnClickListener {
            onSubmit()
        }
        binding.toolbarNote.apply {
            setNavigationIcon(R.drawable.baseline_arrow_back_24)
            setNavigationOnClickListener {
                parentFragmentManager.popBackStack()
            }
            inflateMenu(R.menu.menu_note)
            setOnMenuItemClickListener(menuListener)
        }
        binding.iconSwitch.setOnClickListener {
            val worldLang = binding.tvWordLang.text.toString()
            val meaningLang = binding.tvMeaningLang.text.toString()
            binding.tvWordLang.text = meaningLang
            binding.tvMeaningLang.text = worldLang
        }
    }

    private val menuListener = Toolbar.OnMenuItemClickListener { item ->
        when (item?.itemId) {
            R.id.action_delete -> {
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
            val lastVisiblePosition = layoutManager.findLastVisibleItemPosition()
            val data = wordAdapter.snapshot().items

            if (lastVisiblePosition != RecyclerView.NO_POSITION && lastVisiblePosition < data.size) {
                val lastCorpus = data[lastVisiblePosition]
                val firstLetter = lastCorpus.word.first().uppercaseChar()

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
            binding.rvCorpus.scrollToPosition(position)
        }
    }

    private fun onSubmit() {
        val worldLang = binding.tvWordLang.text.toString()
        val meaningLang = binding.tvMeaningLang.text.toString()
        val word = binding.edWord.text.toString().trim()
        val meaning = binding.edMeaning.text.toString().trim()
        if (word.isEmpty() || meaning.isEmpty()) return

        val random = generateRandomString(10)
        val id = "corpus-$random"
        val noteIdNew = "note-$random"

        val corpus = Corpus(
            word = word,
            noteId = viewModel.noteId.value ?: noteIdNew,
            meaning = meaning,
            wordLang = worldLang,
            meaningLang = meaningLang,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        viewModel.insertCorpus(corpus) {
            Toast.makeText(
                requireContext(),
                if (it == -1L) "Duplicate" else "Success",
                Toast.LENGTH_SHORT
            ).show()

            if (it == -1L) return@insertCorpus

            if (viewModel.noteId.value == null) {
                val now = System.currentTimeMillis()
                val note = Note(
                    id = noteIdNew,
                    title = "",
                    wordLang = worldLang,
                    meaningLang = meaningLang,
                    contentSize = 1,
                    createdAt = now,
                    updatedAt = now
                )
                viewModel.createNewNote(note)
                viewModel.updateNoteId(noteIdNew)
            } else {
                viewModel.updateNoteUpdatedAt(viewModel.noteId.value!!, System.currentTimeMillis())
                viewModel.updateNoteContentSize(
                    viewModel.noteId.value!!,
                    wordAdapter.snapshot().size + 1
                )
            }
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