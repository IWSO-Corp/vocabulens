package com.iwsocorp.vocabnotes.ui.search

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.PagingData
import com.iwsocorp.vocabnotes.R
import com.iwsocorp.vocabnotes.core.model.Corpus
import com.iwsocorp.vocabnotes.databinding.FragmentSearchBinding
import com.iwsocorp.vocabnotes.ui.detail.ARG_CORPUS_WORD
import com.iwsocorp.vocabnotes.ui.detail.DetailViewModel
import com.iwsocorp.vocabnotes.ui.note.WordAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.getValue

@AndroidEntryPoint
class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SearchViewModel by viewModels()
    private val detailViewModel: DetailViewModel by activityViewModels()
    private val wordAdapter: WordAdapter by lazy {
        WordAdapter(false, object : WordAdapter.ClickListener {
            override fun onClick(corpus: Corpus) {
                findNavController().navigate(
                    R.id.action_searchFragment_to_corpusDetailFragment,
                    Bundle().apply {
                        putString(ARG_CORPUS_WORD, corpus.word)
                    }
                )
            }

            override fun onPlay(url: String) {
                lifecycleScope.launch(Dispatchers.IO) {
                    if (url.isNotEmpty()) playAudio(url)
                }
            }

            override fun onSelectionChanged(size: Int) {}
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        detailViewModel.setCorpusList(emptyList())

        viewModel.searchResults.observe(viewLifecycleOwner) {
            lifecycleScope.launch {
                wordAdapter.submitData(it)
            }
        }
        lifecycleScope.launch {
            wordAdapter.loadStateFlow.collect {
                binding.tvEmpty.visibility =
                    if (wordAdapter.itemCount == 0) View.VISIBLE else View.GONE
            }
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

        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
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
    }

}