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
import androidx.lifecycle.lifecycleScope
import com.iwsocorp.vocabnotes.core.common.Utils.isNetworkAvailable
import com.iwsocorp.vocabnotes.core.model.Corpus
import com.iwsocorp.vocabnotes.databinding.FragmentCorpusDetailBinding
import com.iwsocorp.vocabnotes.ui.note.ARG_POSITION
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.getValue
import com.iwsocorp.vocabnotes.R

const val ARG_CORPUS_WORD = "corpusWordParam"

@AndroidEntryPoint
class CorpusDetailFragment : Fragment() {

    private var _binding: FragmentCorpusDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DetailViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val argWord = arguments?.getString(ARG_CORPUS_WORD)
        val argPosition = arguments?.getInt(ARG_POSITION)

        with(viewModel) {
            setCorpusWord(argWord ?: "")
            setCorpusPosition(argPosition ?: -1)

            corpusWord.observe(viewLifecycleOwner) {
                updateCorpusDetail(it, isNetworkAvailable(requireContext())) {
                    Toast.makeText(requireContext(), "No internet connection", Toast.LENGTH_SHORT)
                        .show()
                }
            }
            updatedCorpus.observe(viewLifecycleOwner) {
                it?.let { corpus ->
                    setupUI(corpus)
                }
            }
            posAndWords.observe(viewLifecycleOwner) { (pos, list) ->
                setupNavigation(pos, list)
                setupToolbar(pos)
            }
        }
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
            setNavigationIcon(R.drawable.baseline_arrow_back_24)
            title = (position + 1).toString()
            setNavigationOnClickListener {
                onBackPressed(position)
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner,
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

    private fun setupUI(corpus: Corpus) = with(binding) {
        tvWord.text = corpus.word
        tvMeaning.text = corpus.meaning
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
        rvMeanings.adapter = MeaningAdapter(corpus.meanings)
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
        viewModel.resetUpdatedCorpus()
    }
}