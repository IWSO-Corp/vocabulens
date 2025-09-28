package com.iwsocorp.vocabnotes.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.iwsocorp.vocabnotes.R
import com.iwsocorp.vocabnotes.core.model.Corpus
import com.iwsocorp.vocabnotes.databinding.FragmentEditDetailBinding
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class EditDetailFragment : Fragment() {

    private var _binding: FragmentEditDetailBinding? = null
    private val binding get() = _binding!!
    private val detailViewModel: DetailViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.getString(ARG_CORPUS_WORD)?.let {
            detailViewModel.getCorpus(it)
        }

        detailViewModel.corpus.observe(viewLifecycleOwner) { corpus ->
            Timber.d("Corpus: $corpus")
            corpus?.let {
                binding.etWord.setText(it.word)
                binding.etMeaning.setText(it.meaning)
            }
        }

        binding.toolbarEdit.apply {
            menu.clear()
            inflateMenu(R.menu.menu_edit)
            setOnMenuItemClickListener(menuListener)
            setNavigationIcon(R.drawable.baseline_arrow_back_24)
            setNavigationOnClickListener {
                findNavController().navigateUp()
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    Toast.makeText(requireContext(), "oi", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun onSave(corpus: Corpus) {
        val word = binding.etWord.text.toString().lowercase().trim()
        val meaning = binding.etMeaning.text.toString().lowercase().trim()
        if (word.isEmpty() || meaning.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }
        if (word == corpus.word && meaning == corpus.meaning) {
            Toast.makeText(requireContext(), "No changes made", Toast.LENGTH_SHORT).show()
            return
        }
        val updatedCorpus = corpus.copy(
            word = word,
            meaning = meaning,
            phonetic = "",
            audio = "",
            meanings = emptyList(),
            updatedAt = System.currentTimeMillis()
        )
        detailViewModel.updateCorpus(updatedCorpus) {
            if (it == -1L) {
                Toast.makeText(requireContext(), "Word already exists", Toast.LENGTH_SHORT).show()
                return@updateCorpus
            } else {
                Toast.makeText(requireContext(), "Saved", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        }
    }

    private val menuListener = Toolbar.OnMenuItemClickListener { item ->
        when (item.itemId) {
            R.id.action_save -> {
                detailViewModel.corpus.observe(viewLifecycleOwner) { corpus ->
                    Timber.d("Corpus: $corpus")
                    corpus?.let {
                        onSave(it)
                    }
                }
            }
        }
        true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentEditDetailBinding.inflate(inflater)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}