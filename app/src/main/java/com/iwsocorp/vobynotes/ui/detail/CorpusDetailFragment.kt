package com.iwsocorp.vobynotes.ui.detail

import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.iwsocorp.vobynotes.R
import com.iwsocorp.vobynotes.core.common.BaseFragment
import com.iwsocorp.vobynotes.core.common.TextViewGestureHelper
import com.iwsocorp.vobynotes.core.common.Utils.isNetworkAvailable
import com.iwsocorp.vobynotes.core.common.Utils.setIconColor
import com.iwsocorp.vobynotes.core.common.Utils.showAlertDialog
import com.iwsocorp.vobynotes.core.model.Corpus
import com.iwsocorp.vobynotes.core.model.Example
import com.iwsocorp.vobynotes.core.model.Mark
import com.iwsocorp.vobynotes.databinding.FragmentCorpusDetailBinding
import com.iwsocorp.vobynotes.ui.note.ARG_POSITION
import com.iwsocorp.vobynotes.ui.scan.ScanViewModel
import com.iwsocorp.vobynotes.ui.search.ARG_SEARCH_WORD
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

const val ARG_CORPUS_ID = "corpusIdParam"
const val ARG_FROM = "fromParam"

@AndroidEntryPoint
class CorpusDetailFragment : BaseFragment<FragmentCorpusDetailBinding>(
    FragmentCorpusDetailBinding::inflate
) {

    private val viewModel: DetailViewModel by activityViewModels()
    private val scanViewModel: ScanViewModel by viewModels()
    private val corpusId: String by lazy {
        arguments?.getString(ARG_CORPUS_ID)!!
    }
    private lateinit var adapter: ExampleAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.setCorpusId(corpusId)
        viewModel.setCorpusPosition(arguments?.getInt(ARG_POSITION)!!)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val searchArgs = arguments?.getString(ARG_FROM)

        viewModel.corpusId.observe(viewLifecycleOwner) { id ->
            viewModel.getCorpusById(id)

            Timber.d("Corpus ID: $id")
        }

        viewModel.corpus.observe(viewLifecycleOwner) { data ->
            data?.let { corpus ->
                if (corpus.phonetic.isEmpty() && isNetworkAvailable(requireContext())) {
                    viewModel.updateCorpusDetail(corpus.word)
                } else if (corpus.phonetic.isEmpty() && !isNetworkAvailable(requireContext())) {
                    Snackbar.make(
                        binding.btnNext,
                        "No internet connection",
                        Snackbar.LENGTH_INDEFINITE
                    )
                        .setAction("Close") {}.show()
                }
                setupUI(corpus)
            }

            Timber.d("Corpus data: $data")
        }

        viewModel.posAndCorpusList.observe(viewLifecycleOwner) { (pos, list) ->
            if (searchArgs.isNullOrBlank()) {
                setupNavigation(pos, list)
                setupToolbar(pos)
            } else {
                setupNavigation(0, emptyList())
                setupToolbar(0)
            }
        }

        binding.itemDetail.iconAddExample.setOnClickListener {
            ExampleBottomSheet {
                val corpus = viewModel.corpus.value!!
                viewModel.insertExampleSentence(
                    Example(corpus.id, corpus.word, it)
                )
                Toast.makeText(requireContext(), "Example added", Toast.LENGTH_SHORT).show()
            }.show(childFragmentManager, null)
        }
    }

    private fun setupNavigation(pos: Int, list: List<Corpus>) {
        val isMin = pos > 0
        val isMax = pos < list.size - 1
        binding.btnPrevious.isVisible = isMin
        binding.btnNext.isVisible = isMax && pos != -1
        if (isMin) {
            val prevCorpus = list[pos - 1]
            binding.btnPrevious.apply {
                text = prevCorpus.word
                setOnClickListener {
                    viewModel.setCorpusPosition(pos - 1)
                    viewModel.setCorpusId(prevCorpus.id)
                }
            }
        }
        if (isMax) {
            val nextCorpus = list[pos + 1]
            binding.btnNext.apply {
                text = nextCorpus.word
                setOnClickListener {
                    viewModel.setCorpusPosition(pos + 1)
                    viewModel.setCorpusId(nextCorpus.id)
                }
            }
        }
    }

    private fun setupToolbar(position: Int) {
        binding.toolbarDetail.apply {
            title = (position + 1).toString()
            setNavigationIcon(R.drawable.baseline_arrow_back_24)
            setNavigationOnClickListener { onBackPressed(position) }
            menu.clear()
            inflateMenu(R.menu.menu_detail)
            setIconColor(requireContext())
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
        parentFragmentManager.apply {
            setFragmentResult("requestKey", bundleOf(ARG_POSITION to position))
            popBackStack()
        }
        viewModel.resetCorpus()
    }

    private var examplesJob: Job? = null

    private fun setupUI(corpus: Corpus) {
        Timber.d("Setup ui with: $corpus")

        with(binding.itemDetail) {
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

            val gestureHelper = TextViewGestureHelper(requireContext(), corpus.word, {
                lifecycleScope.launch {
                    findNavController().navigate(
                        R.id.action_corpusDetailFragment_to_searchFragment,
                        Bundle().apply {
                            putString(ARG_SEARCH_WORD, it)
                        }
                    )
                }
            }) { word ->
                scanViewModel.translate(word) {
                    Snackbar.make(
                        requireView(),
                        it,
                        Snackbar.LENGTH_INDEFINITE,
                    ).setAction("OK") {}.show()
                }
            }
            rvMeanings.adapter = MeaningAdapter(corpus.meanings, gestureHelper)
            rvMeanings.setHasFixedSize(true)

            adapter = ExampleAdapter(corpus.word, gestureHelper)
            rvExample.adapter = adapter
        }

        examplesJob?.cancel()

        examplesJob = viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getExamplesByWord(corpus.word)
                .collectLatest { examples ->
                    adapter.submitList(examples)
                    Timber.d("Examples by word: ${corpus.word}")
                    Timber.d("Examples data: ${examples.map { it.sentence }}")
                }
        }

        val menuMark = binding.toolbarDetail.menu.findItem(R.id.action_mark)
        menuMark?.setIcon(
            when (corpus.mark) {
                Mark.FAMILIAR,
                Mark.UNFAMILIAR,
                    -> R.drawable.baseline_star_24

                Mark.UNMARKED -> R.drawable.outline_star_border_24
            }
        )
        menuMark?.icon?.let {
            DrawableCompat.setTint(
                it,
                ContextCompat.getColor(
                    requireContext(),
                    when (corpus.mark) {
                        Mark.FAMILIAR -> R.color.blue
                        Mark.UNFAMILIAR -> R.color.red
                        else -> R.color.black
                    }
                )
            )
        }
    }

    private val menuListener = Toolbar.OnMenuItemClickListener { item ->
        val corpus = viewModel.corpus.value!!
        when (item.itemId) {
            R.id.action_mark -> {
                viewModel.updateCorpusMark(
                    listOf(corpus.id),
                    when (corpus.mark) {
                        Mark.UNMARKED -> Mark.FAMILIAR
                        Mark.FAMILIAR -> Mark.UNFAMILIAR
                        Mark.UNFAMILIAR -> Mark.UNMARKED
                    }
                )
                Timber.d("Mark updated")

            }

            R.id.action_edit -> {
                findNavController().navigate(
                    R.id.action_corpusDetailFragment_to_editDetailFragment,
                    Bundle().apply {
                        putString(ARG_CORPUS_ID, corpus.id)
                    }
                )
            }

            R.id.action_delete_word -> {
                showAlertDialog(
                    requireContext(),
                    "Delete Word",
                    "Are you sure you want to delete this word?",
                    "Delete",
                    "Cancel"
                ) {
                    viewModel.deleteCorpus(listOf(corpus.id))
                    findNavController().popBackStack()
                }
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

}