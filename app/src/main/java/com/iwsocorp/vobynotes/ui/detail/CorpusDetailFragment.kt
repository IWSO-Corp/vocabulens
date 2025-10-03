package com.iwsocorp.vobynotes.ui.detail

import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.iwsocorp.vobynotes.R
import com.iwsocorp.vobynotes.core.common.TextViewGestureHelper
import com.iwsocorp.vobynotes.core.common.Utils.isNetworkAvailable
import com.iwsocorp.vobynotes.core.common.Utils.setIconColor
import com.iwsocorp.vobynotes.core.model.Corpus
import com.iwsocorp.vobynotes.core.model.Example
import com.iwsocorp.vobynotes.databinding.FragmentCorpusDetailBinding
import com.iwsocorp.vobynotes.ui.home.HomeViewModel
import com.iwsocorp.vobynotes.ui.note.ARG_POSITION
import com.iwsocorp.vobynotes.ui.search.ARG_SEARCH_WORD
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

const val ARG_CORPUS_WORD = "corpusWordParam"
const val ARG_FROM = "fromParam"

@AndroidEntryPoint
class CorpusDetailFragment : Fragment() {

    private var _binding: FragmentCorpusDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DetailViewModel by activityViewModels()
    private val homeViewModel: HomeViewModel by activityViewModels()
    private val corpusWord: String by lazy {
        arguments?.getString(ARG_CORPUS_WORD)!!
    }
    private lateinit var adapter: ExampleAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.setCorpusWord(corpusWord)
        viewModel.setCorpusPosition(arguments?.getInt(ARG_POSITION)!!)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args = arguments?.getString(ARG_FROM)
        if (args != null) setupToolbar(0)

        viewModel.corpusWord.observe(viewLifecycleOwner) { keyword ->
            viewModel.getCorpus(keyword)
            viewModel.getExamplesByWord(keyword) { list ->
                if (::adapter.isInitialized) adapter.addItems(
                    list.map { it.sentence }.shuffled().take(3)
                )
            }

            lifecycleScope.launch(Dispatchers.IO) {
                homeViewModel.allCorpus.collectLatest { list ->
                    val examples = mutableListOf<String>()
                    list.map { corpus ->
                        corpus.meanings.map { meaning ->
                            meaning.definitions.map { definition ->
                                definition.example?.let { sentence ->
                                    if (sentence.isNotEmpty()) examples.add(sentence)
                                }
                            }
                        }
                    }
                    Timber.d("Examples: $examples")
                    val filteredExamples = examples.toList().filter {
                        containsWordRegex(it, keyword)
                    }.shuffled().take(3)
                    withContext(Dispatchers.Main) {
                        if (::adapter.isInitialized) adapter.addItems(filteredExamples)
                    }
                }
            }
        }
        viewModel.corpus.observe(viewLifecycleOwner) { corpus ->
            corpus?.let {
                Timber.d(it.toString())
                if (it.phonetic.isEmpty() && isNetworkAvailable(requireContext())) {
                    viewModel.updateCorpusDetail(it.word)
                } else if (it.phonetic.isEmpty() && !isNetworkAvailable(requireContext())) {
                    Snackbar.make(
                        binding.btnNext,
                        "No internet connection",
                        Snackbar.LENGTH_INDEFINITE
                    )
                        .setAction("Close") {}.show()
                }
                setupUI(it)
            }
        }
        viewModel.posAndWords.observe(viewLifecycleOwner) { (pos, list) ->
            setupNavigation(pos, list)
            setupToolbar(pos)
        }

        binding.itemDetail.iconAddExample.setOnClickListener {
            ExampleBottomSheet {
                viewModel.insertExampleSentence(
                    Example(it)
                )
                Toast.makeText(requireContext(), "Example added", Toast.LENGTH_SHORT).show()
            }.show(childFragmentManager, null)
        }
    }

    fun containsWordRegex(sentence: String, word: String): Boolean {
        val pattern = "\\b${Regex.escape(word)}\\b".toRegex(RegexOption.IGNORE_CASE)
        return pattern.containsMatchIn(sentence)
    }

    private fun setupNavigation(pos: Int, list: List<String>) {
        val isMin = pos > 0
        val isMax = pos < list.size - 1
        binding.btnPrevious.isVisible = isMin
        binding.btnNext.isVisible = isMax && pos != -1
        if (isMin) {
            val prevWord = list[pos - 1]
            binding.btnPrevious.apply {
                text = prevWord
                setOnClickListener {
                    viewModel.setCorpusPosition(pos - 1)
                    viewModel.setCorpusWord(prevWord)
                }
            }
        }
        if (isMax) {
            val nextWord = list[pos + 1]
            binding.btnNext.apply {
                text = nextWord
                setOnClickListener {
                    viewModel.setCorpusPosition(pos + 1)
                    viewModel.setCorpusWord(nextWord)
                }
            }
        }
    }

    private fun setupToolbar(position: Int) {
        binding.toolbarDetail.apply {
            title = (position + 1).toString()
            setNavigationIcon(R.drawable.baseline_arrow_back_24)
            setNavigationOnClickListener {
                onBackPressed(position)
            }
            setIconColor(requireContext())
            menu.clear()
            inflateMenu(R.menu.menu_detail)
            setOnMenuItemClickListener(menuListener)
        }
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    onBackPressed(position)
                }
            }
        )
    }

    private fun onBackPressed(position: Int) {
        val result = Bundle().apply {
            putInt(ARG_POSITION, position)
        }
        parentFragmentManager.apply {
            setFragmentResult("requestKey", result)
            popBackStack()
        }
    }

    private fun setupUI(corpus: Corpus) = with(binding.itemDetail) {
        tvWord.text = corpus.word
        tvMeaning.text = corpus.meaning.ifEmpty { "-" }
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
            viewModel.resetCorpus()
            lifecycleScope.launch {
                findNavController().navigate(
                    R.id.action_corpusDetailFragment_to_searchFragment,
                    Bundle().apply {
                        putString(ARG_SEARCH_WORD, it)
                    }
                )
            }
        }
        rvMeanings.adapter = MeaningAdapter(corpus.meanings, gestureHelper)
        rvMeanings.setHasFixedSize(true)
        adapter = ExampleAdapter(corpus.word, gestureHelper)
        binding.itemDetail.rvExample.adapter = adapter
    }

    private val menuListener = Toolbar.OnMenuItemClickListener { item ->
        when (item.itemId) {
            R.id.action_mark -> {

            }

            R.id.action_edit -> {
                findNavController().navigate(
                    R.id.action_corpusDetailFragment_to_editDetailFragment,
                    Bundle().apply {
                        putString(ARG_CORPUS_WORD, viewModel.corpusWord.value)
                    }
                )
            }

            R.id.action_delete_word -> {

            }
        }
        true
    }

    private var mediaPlayer: MediaPlayer? = null

    private fun playAudio(url: String) {
        resetMediaPlayer()
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
    }

    private fun resetMediaPlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentCorpusDetailBinding.inflate(inflater)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        resetMediaPlayer()
        viewModel.resetCorpus()
    }
}