package com.iwsocorp.vocabnotes.ui.detail

import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.iwsocorp.vocabnotes.core.common.Utils.isNetworkAvailable
import com.iwsocorp.vocabnotes.core.model.Corpus
import com.iwsocorp.vocabnotes.databinding.FragmentCorpusDetailBinding
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import kotlin.getValue

const val ARG_CORPUS_WORD = "corpusWordParam"

@AndroidEntryPoint
class CorpusDetailFragment : Fragment() {

    private var _binding: FragmentCorpusDetailBinding? = null
    private val binding get() = _binding!!
    private val corpusWord: String? by lazy {
        arguments?.getString(ARG_CORPUS_WORD)
    }
    private val viewModel: DetailViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        corpusWord?.let {
            viewModel.getCorpusByWord(it) { corpus ->
                Timber.d("corpus: $corpus")

                if (corpus.phonetic.isEmpty() && isNetworkAvailable(requireContext())) {
                    viewModel.getVocabulary(it) { vocabulary ->
                        Timber.d("vocabulary: $vocabulary")

                        viewModel.insertWordMeanings(it, vocabulary.meanings)

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
                        viewModel.updateCorpus(newCorpus)
                        setupUI(newCorpus)
                    }
                } else {
                    setupUI(corpus)
                }
            }
        }
    }

    private fun setupUI(corpus: Corpus) = with(binding) {
        tvWord.text = corpus.word
        tvMeaning.text = corpus.meaning
        tvPronun.apply {
            text = corpus.phonetic
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
    }

}