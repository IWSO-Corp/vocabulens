package com.iwsocorp.vocabnotes.ui.detail

import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.iwsocorp.vocabnotes.R
import com.iwsocorp.vocabnotes.core.common.Utils.isNetworkAvailable
import com.iwsocorp.vocabnotes.core.model.Corpus
import com.iwsocorp.vocabnotes.databinding.FragmentCorpusDetailBinding
import com.iwsocorp.vocabnotes.ui.note.ARG_POSITION
import com.iwsocorp.vocabnotes.ui.note.NoteViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.getValue

const val ARG_CORPUS_WORD = "corpusWordParam"

@AndroidEntryPoint
class CorpusDetailFragment : Fragment() {

    private var _binding: FragmentCorpusDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DetailViewModel by viewModels()
    private val noteViewModel: NoteViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val argWord = arguments?.getString(ARG_CORPUS_WORD)
        val argPosition = arguments?.getInt(ARG_POSITION)

        viewModel.setCorpusWord(argWord ?: "")
        viewModel.setCorpusPosition(argPosition ?: -1)

        viewModel.corpusWord.observe(viewLifecycleOwner) { word ->
            CoroutineScope(Dispatchers.IO).launch {
                viewModel.getCorpusByWord(word) { corpus ->
                    getVocabulary(corpus)
                }
            }
        }
        viewModel.corpusPosition.observe(viewLifecycleOwner) { pos ->
            noteViewModel.corpusList.observe(viewLifecycleOwner) { list ->
                setupNavigation(pos, list)
            }
            setupBack(pos)
        }
    }

    private fun setupNavigation(pos: Int, list: List<Corpus>) {
        val isMin = pos > 0
        val isMax = pos < list.size - 1
        binding.btnPrevious.isVisible = isMin
        binding.btnNext.isVisible = isMax
        if (isMin) {
            val prevWord = list[pos - 1].word
            binding.btnPrevious.apply {
                text = prevWord
                setOnClickListener {
                    viewModel.setCorpusPosition(pos - 1)
                    viewModel.setCorpusWord(prevWord)
                }
            }
        }
        if (isMax) {
            val nextWord = list[pos + 1].word
            binding.btnNext.apply {
                text = nextWord
                setOnClickListener {
                    viewModel.setCorpusPosition(pos + 1)
                    viewModel.setCorpusWord(nextWord)
                }
            }
        }
    }

    private fun setupBack(position: Int) {
        binding.toolbarDetail.apply {
            setNavigationIcon(R.drawable.baseline_arrow_back_24)
            title = (position + 1).toString()
            setNavigationOnClickListener {
                setNoteFragmentResult(position)
                parentFragmentManager.popBackStack()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    setNoteFragmentResult(position)
                    parentFragmentManager.popBackStack()
                }
            }
        )
    }

    private fun setNoteFragmentResult(position: Int) {
        val result = Bundle().apply {
            putInt(ARG_POSITION, position)
        }
        parentFragmentManager.setFragmentResult("requestKey", result)
    }

    private fun getVocabulary(corpus: Corpus) {
        if (corpus.phonetic.isEmpty() && isNetworkAvailable(requireContext())) {
            viewModel.getVocabulary(corpus.word) { vocabulary ->
                Timber.d("vocabulary: $vocabulary")
                val newCorpus = Corpus(
                    id = corpus.id,
                    noteId = corpus.noteId,
                    word = corpus.word,
                    meaning = corpus.meaning,
                    wordLang = corpus.wordLang,
                    meaningLang = corpus.meaningLang,
                    phonetic = vocabulary.phonetic,
                    audio = vocabulary.audio,
                    meanings = vocabulary.meanings,
                    createdAt = corpus.createdAt,
                    updatedAt = System.currentTimeMillis()
                )
                setupUI(newCorpus)
                viewModel.updateCorpus(newCorpus)
            }
        } else if (!isNetworkAvailable(requireContext())) {
            Toast.makeText(requireContext(), "No internet connection", Toast.LENGTH_SHORT).show()
        } else {
            setupUI(corpus)
        }
    }

    private fun setupUI(corpus: Corpus) = with(binding) {
        tvWord.text = corpus.word
        tvMeaning.text = corpus.meaning
        underline.isVisible = corpus.audio.isNotEmpty()
        tvPronun.apply {
            text = corpus.phonetic
            isVisible = corpus.phonetic.isNotEmpty() || corpus.phonetic != "-"
            setOnClickListener {
                if (corpus.audio.isNotEmpty()) playAudio(corpus.audio)
            }
        }
        rvMeanings.adapter = MeaningAdapter(corpus.meanings)
    }

    private var mediaPlayer: MediaPlayer? = null

    private fun playAudio(url: String) {
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
        _binding = FragmentCorpusDetailBinding.inflate(inflater)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        resetMediaPlayer()
    }
}