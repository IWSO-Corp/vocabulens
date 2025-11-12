package com.iwsocorp.vobynotes.ui.search

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.text.InputFilter
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.paging.PagingData
import com.iwsocorp.vobynotes.R
import com.iwsocorp.vobynotes.core.common.BaseFragment
import com.iwsocorp.vobynotes.core.common.TextViewGestureHelper
import com.iwsocorp.vobynotes.core.common.Utils.alertInputDialog
import com.iwsocorp.vobynotes.core.model.Corpus
import com.iwsocorp.vobynotes.core.model.Mark
import com.iwsocorp.vobynotes.core.model.Note
import com.iwsocorp.vobynotes.databinding.FragmentSearchBinding
import com.iwsocorp.vobynotes.ui.detail.ARG_CORPUS_ID
import com.iwsocorp.vobynotes.ui.detail.ARG_FROM
import com.iwsocorp.vobynotes.ui.detail.DetailViewModel
import com.iwsocorp.vobynotes.ui.detail.MeaningAdapter
import com.iwsocorp.vobynotes.ui.note.ARG_POSITION
import com.iwsocorp.vobynotes.ui.note.NoteBottomSheet
import com.iwsocorp.vobynotes.ui.note.NoteViewModel
import com.iwsocorp.vobynotes.ui.note.WordAdapter
import com.iwsocorp.vobynotes.ui.scan.ScanViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

const val ARG_SEARCH_WORD = "searchWordParam"

@AndroidEntryPoint
class SearchFragment : BaseFragment<FragmentSearchBinding>(FragmentSearchBinding::inflate) {

    private val viewModel: SearchViewModel by viewModels()
    private val noteViewModel: NoteViewModel by viewModels()
    private val scanViewModel: ScanViewModel by viewModels()
    private val detailViewModel: DetailViewModel by activityViewModels()
    private val wordAdapter: WordAdapter by lazy {
        WordAdapter(false, object : WordAdapter.ClickListener {
            override fun onClick(corpus: Corpus) {
                detailViewModel.setCorpusList(emptyList())
                findNavController().navigate(
                    R.id.action_searchFragment_to_corpusDetailFragment,
                    bundleOf(
                        ARG_CORPUS_ID to corpus.id,
                        ARG_POSITION to 0,
                        ARG_FROM to "search"
                    )
                )
            }

            override fun onPlay(url: String) {
                lifecycleScope.launch(Dispatchers.IO) {
                    if (url.isNotEmpty()) playAudio(url)
                }
            }

            override fun onSelectionChanged(size: Int) {}

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
                    R.id.action_searchFragment_to_editDetailFragment,
                    Bundle().apply {
                        putString(ARG_CORPUS_ID, corpus.id)
                    }
                )
            }
        })
    }
    private val args by lazy {
        arguments?.getString(ARG_SEARCH_WORD)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        args?.let {
            onSearch(it)
        } ?: run {
            binding.searchView.requestFocus()
        }

        setupUI()
        observeState()
    }

    private fun observeState() = viewModel.searchUiState.collectOnStarted { state ->
        binding.progressBar.isVisible = state is SearchUiState.Loading
        binding.rvSearch.isVisible = state is SearchUiState.LocalLoaded

        when (state) {
            is SearchUiState.Idle -> {}
            is SearchUiState.Loading -> {}
            is SearchUiState.LocalLoaded -> {
                wordAdapter.submitData(viewLifecycleOwner.lifecycle, state.corpusPagingData)
                wordAdapter.addLoadStateListener {
                    if (it.refresh is LoadState.NotLoading) binding.rvSearch.scrollToPosition(0)
                    binding.tvEmpty.isVisible = wordAdapter.itemCount == 0
                }
            }

            is SearchUiState.ApiLoaded -> {
                setupDetailUI(state.corpus)
            }
        }

        Timber.d(
            "State: ${
                when (state) {
                    is SearchUiState.Idle -> "Idle"
                    is SearchUiState.Loading -> "Loading"
                    is SearchUiState.LocalLoaded -> "LocalLoaded: ${state.corpusPagingData}"
                    is SearchUiState.ApiLoaded -> "ApiLoaded: ${state.corpus}"
                }
            }"
        )
    }

    private fun setupUI() {
        val searchIcon: ImageView =
            binding.searchView.findViewById(androidx.appcompat.R.id.search_mag_icon)
        searchIcon.visibility = View.GONE

        binding.rvSearch.adapter = wordAdapter
        binding.searchView.apply {
            isIconified = false
            requestFocus()
            setOnCloseListener {
                wordAdapter.submitData(viewLifecycleOwner.lifecycle, PagingData.from(emptyList()))
                setQuery("", false)

                binding.itemDetail.contentDetail.visibility = View.GONE

                true
            }
            setOnQueryTextListener(queryListener)
        }
        binding.toolbarSearch.apply {
            setNavigationIcon(R.drawable.baseline_arrow_back_24)
            setNavigationOnClickListener {
                parentFragmentManager.popBackStack()
            }
        }
        binding.searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text).apply {
            setHintTextColor(ContextCompat.getColor(requireContext(), R.color.grey))
            setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            filters = arrayOf(
                InputFilter.LengthFilter(30),
                InputFilter { source, _, _, _, _, _ ->
                    if (source != null && source.contains(" ")) "" else source
                }
            )
        }
        binding.searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn).apply {
            setColorFilter(ContextCompat.getColor(requireContext(), R.color.black))
        }
    }

    private fun onSearch(word: String) {
        scanViewModel.translate(word) {
            viewModel.searchWordDefinition(word.trim(), it)
        }

        binding.btnSearch.visibility = View.GONE
        binding.itemDetail.contentDetail.visibility = View.VISIBLE

        lifecycleScope.launch {
            val existingCorpus = viewModel.getCorpusByWord(word)
            binding.btnSave.isVisible = existingCorpus == null
        }
    }

    private fun setupDetailUI(corpus: Corpus) = with(binding.itemDetail) {
        tvWord.text = corpus.word.ifEmpty { args }
        tvMeaning.text = corpus.meaning
        csExample.visibility = View.GONE
        rvExample.visibility = View.GONE
        underline.isVisible = corpus.audio.isNotEmpty()
        tvPronun.apply {
            text = corpus.phonetic
            isVisible = corpus.phonetic.isNotEmpty()
            setOnClickListener {
                lifecycleScope.launch(Dispatchers.IO) {
                    if (corpus.audio.isNotEmpty()) playAudio(corpus.audio)
                }
            }
        }
        tvEmpty.isVisible = corpus.meanings.isEmpty()

        val gestureHelper = TextViewGestureHelper(requireContext(), corpus.word, {
            binding.searchView.setQuery("", false)
            onSearch(it)
        }) { word, result ->
            scanViewModel.translate(word, result)
        }
        rvMeanings.adapter = MeaningAdapter(corpus.meanings, gestureHelper)

        binding.btnSave.setOnClickListener {
            noteViewModel.notes.observe(viewLifecycleOwner) { list ->
                NoteBottomSheet(list, {
                    val note = Note(
                        title = "New Note",
                        wordLang = corpus.wordLang,
                        meaningLang = corpus.meaningLang,
                        contentSize = 1
                    )
                    requireContext().alertInputDialog(note.title) {
                        val newNote = if (note.title == it) note else note.copy(title = it)
                        noteViewModel.updateNoteId(newNote.id)
                        noteViewModel.createNote(newNote)
                        noteViewModel.insertCorpus(
                            corpus.copy(noteId = newNote.id)
                        ) {
                            Toast.makeText(
                                requireContext(),
                                "Word saved to ${newNote.title}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        binding.btnSave.visibility = View.GONE
                        binding.btnSearch.visibility = View.GONE
                        binding.rvSearch.visibility = View.GONE
                    }
                }) { note ->
                    noteViewModel.updateNoteId(note.id)
                    noteViewModel.insertCorpus(
                        corpus.copy(noteId = note.id)
                    ) {
                        Toast.makeText(
                            requireContext(),
                            "Word saved to ${note.title}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    binding.btnSave.visibility = View.GONE
                    binding.btnSearch.visibility = View.GONE
                    binding.rvSearch.visibility = View.GONE
                }.show(childFragmentManager, null)
            }
        }
    }

    private val queryListener = object : SearchView.OnQueryTextListener {
        override fun onQueryTextSubmit(p0: String?): Boolean {
            return true
        }

        override fun onQueryTextChange(p0: String?): Boolean {
            val endIcon: ImageView =
                binding.searchView.findViewById(androidx.appcompat.R.id.search_close_btn)
            p0?.let { q ->
                endIcon.isVisible = q.isNotEmpty()
                if (q.isNotEmpty()) {
                    viewModel.searchWord(q)
                } else {
                    wordAdapter.submitData(
                        viewLifecycleOwner.lifecycle,
                        PagingData.from(emptyList())
                    )
                    viewModel.setIdleState()
                }

                binding.btnSave.visibility = View.GONE
                binding.itemDetail.contentDetail.visibility = View.GONE

                binding.btnSearch.isVisible = q.isNotEmpty()
                binding.btnSearch.text = q.trim()
                binding.btnSearch.setOnClickListener {
                    onSearch(q)
                    wordAdapter.submitData(
                        viewLifecycleOwner.lifecycle,
                        PagingData.from(emptyList())
                    )
                }
            }
            return true
        }

    }

    private var mediaPlayer: MediaPlayer? = null

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

    override fun onResume() {
        super.onResume()

        val imm =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
    }

}