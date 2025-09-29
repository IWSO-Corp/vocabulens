package com.iwsocorp.vocabnotes.ui.search

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.paging.PagingData
import com.iwsocorp.vocabnotes.R
import com.iwsocorp.vocabnotes.core.common.TextViewGestureHelper
import com.iwsocorp.vocabnotes.core.model.Corpus
import com.iwsocorp.vocabnotes.core.model.Mark
import com.iwsocorp.vocabnotes.databinding.FragmentSearchBinding
import com.iwsocorp.vocabnotes.ui.detail.ARG_CORPUS_WORD
import com.iwsocorp.vocabnotes.ui.detail.ARG_FROM
import com.iwsocorp.vocabnotes.ui.detail.DetailViewModel
import com.iwsocorp.vocabnotes.ui.detail.MeaningAdapter
import com.iwsocorp.vocabnotes.ui.note.NoteBottomSheet
import com.iwsocorp.vocabnotes.ui.note.NoteViewModel
import com.iwsocorp.vocabnotes.ui.note.WordAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

const val ARG_SEARCH_WORD = "searchWordParam"

@AndroidEntryPoint
class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SearchViewModel by viewModels()
    private val detailViewModel: DetailViewModel by activityViewModels()
    private val noteViewModel: NoteViewModel by viewModels()
    private val wordAdapter: WordAdapter by lazy {
        WordAdapter(false, object : WordAdapter.ClickListener {
            override fun onClick(corpus: Corpus) {
                findNavController().navigate(
                    R.id.action_searchFragment_to_corpusDetailFragment,
                    Bundle().apply {
                        putString(ARG_CORPUS_WORD, corpus.word)
                        putString(ARG_FROM, "searchFragment")
                    }
                )
            }

            override fun onPlay(url: String) {
                lifecycleScope.launch(Dispatchers.IO) {
                    if (url.isNotEmpty()) playAudio(url)
                }
            }

            override fun onSelectionChanged(size: Int) {}

            override fun onMark(
                word: String,
                mark: Mark,
            ) {
                viewModel.updateCorpusMark(
                    listOf(word),
                    when (mark) {
                        Mark.UNMARKED -> Mark.FAMILIAR
                        Mark.FAMILIAR -> Mark.UNFAMILIAR
                        Mark.UNFAMILIAR -> Mark.UNMARKED
                    }
                )
            }

            override fun onEdit(corpusWord: String) {
                findNavController().navigate(
                    R.id.action_searchFragment_to_editDetailFragment,
                    Bundle().apply {
                        putString(ARG_CORPUS_WORD, corpusWord)
                    }
                )
            }
        })
    }
    private val searchWord = MutableLiveData<String>()
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

        viewModel.searchResults.observe(viewLifecycleOwner) {
            lifecycleScope.launch {
                wordAdapter.submitData(it)
            }
        }
        lifecycleScope.launch {
            wordAdapter.loadStateFlow.collect {
                binding.btnSearch.isVisible =
                    (wordAdapter.itemCount == 0 && searchWord.value?.isNotEmpty() == true)
            }
        }
        searchWord.observe(viewLifecycleOwner) { word ->
            binding.btnSearch.text = word.trim()
            binding.btnSearch.setOnClickListener {
                onSearch(word)
            }
        }
        detailViewModel.corpus.observe(viewLifecycleOwner) {
            Timber.d("Corpus: $it")
            it?.let { corpus ->
                setupUI(corpus)
            }
        }
        wordAdapter.addLoadStateListener {
            if (it.refresh is LoadState.NotLoading) binding.rvSearch.scrollToPosition(0)
        }

        val searchIcon: ImageView =
            binding.searchView.findViewById(androidx.appcompat.R.id.search_mag_icon)
        searchIcon.visibility = View.GONE

        binding.rvSearch.adapter = wordAdapter
        binding.searchView.apply {
            isIconified = false
            requestFocus()
            setOnCloseListener {
                wordAdapter.submitData(lifecycle, PagingData.from(emptyList()))
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
        binding.btnSave.setOnClickListener {
            noteViewModel.notes.observe(viewLifecycleOwner) { list ->
                NoteBottomSheet(list) { note ->
                    noteViewModel.updateNoteId(note.id)
                    noteViewModel.insertCorpus(
                        detailViewModel.corpus.value!!.copy(noteId = note.id)
                    ) {
                        Toast.makeText(
                            requireContext(),
                            "Word saved to ${note.title}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    binding.btnSave.visibility = View.GONE
                    binding.btnSearch.visibility = View.GONE
                }.show(childFragmentManager, null)
            }
        }
    }

    private fun onSearch(word: String) {
        detailViewModel.searchWordDefinition(word.trim())

        binding.btnSearch.visibility = View.GONE
        binding.itemDetail.contentDetail.visibility = View.VISIBLE

        lifecycleScope.launch {
            val existingCorpus = detailViewModel.getCorpusByWord(word)
            binding.btnSave.isVisible = existingCorpus == null
        }
    }

    private fun setupUI(corpus: Corpus) = with(binding.itemDetail) {
        tvWord.text = corpus.word.ifEmpty { args }
        tvMeaning.visibility = View.GONE
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

        val gestureHelper = TextViewGestureHelper(requireContext(), corpus.word) {
            binding.searchView.setQuery("", false)
            onSearch(it)
        }
        rvMeanings.adapter = MeaningAdapter(corpus.meanings, gestureHelper)
    }

    private val queryListener = object : SearchView.OnQueryTextListener {
        override fun onQueryTextSubmit(p0: String?): Boolean {
            return true
        }

        override fun onQueryTextChange(p0: String?): Boolean {
            val endIcon: ImageView =
                binding.searchView.findViewById(androidx.appcompat.R.id.search_close_btn)
            p0?.let {
                endIcon.isVisible = it.isNotEmpty()
                if (it.isNotEmpty()) {
                    viewModel.searchWord(it)
                } else {
                    wordAdapter.submitData(lifecycle, PagingData.from(emptyList()))
                }

                binding.btnSave.visibility = View.GONE
                binding.itemDetail.contentDetail.visibility = View.GONE
                searchWord.value = it
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        detailViewModel.resetCorpus()
    }

}