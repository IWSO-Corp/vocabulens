package com.iwsocorp.vocabnotes.ui.search

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagingData
import com.iwsocorp.vocabnotes.core.model.Corpus
import com.iwsocorp.vocabnotes.databinding.FragmentSearchBinding
import com.iwsocorp.vocabnotes.ui.note.WordAdapter
import com.iwsocorp.vocabnotes.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SearchViewModel by viewModels()
    private val wordAdapter: WordAdapter by lazy {
        WordAdapter(object : WordAdapter.ClickListener {
            override fun onClick(corpus: Corpus) {
            }
            override fun onPlay(url: String) {
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.searchResults.observe(viewLifecycleOwner) {
            wordAdapter.submitData(lifecycle, it)
        }
        lifecycleScope.launch {
            wordAdapter.loadStateFlow.collect {
                binding.tvEmpty.visibility = if (wordAdapter.itemCount == 0) View.VISIBLE else View.GONE
            }
        }

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

        val searchIcon: ImageView = binding.searchView.findViewById(androidx.appcompat.R.id.search_mag_icon)
        searchIcon.visibility = View.GONE
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.searchView, InputMethodManager.SHOW_IMPLICIT)
    }

    private val queryListener = object : SearchView.OnQueryTextListener {
        override fun onQueryTextSubmit(p0: String?): Boolean {
            return true
        }

        override fun onQueryTextChange(p0: String?): Boolean {
            val endIcon: ImageView = binding.searchView.findViewById(androidx.appcompat.R.id.search_close_btn)
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