package com.iwsocorp.vobynotes.ui.detail

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
import com.iwsocorp.vobynotes.R
import com.iwsocorp.vobynotes.core.common.Utils.showAlertDialog
import com.iwsocorp.vobynotes.core.model.Corpus
import com.iwsocorp.vobynotes.databinding.FragmentEditDetailBinding
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class EditDetailFragment : Fragment() {

    private var _binding: FragmentEditDetailBinding? = null
    private val binding get() = _binding!!
    private val detailViewModel: DetailViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.getString(ARG_CORPUS_ID)?.let {
            detailViewModel.getCorpusById(it)
        }

        detailViewModel.corpus.observe(viewLifecycleOwner) { corpus ->
            Timber.d("Corpus: $corpus")
            corpus?.let {
                binding.ilWord.hint = it.wordLang
                binding.ilMeaning.hint = it.meaningLang
                binding.etWord.setText(it.word)
                binding.etMeaning.setText(it.meaning)
                binding.toolbarEdit.setNavigationOnClickListener {
                    onBack(corpus)
                }
                requireActivity().onBackPressedDispatcher.addCallback(
                    viewLifecycleOwner,
                    object : OnBackPressedCallback(true) {
                        override fun handleOnBackPressed() {
                            onBack(corpus)
                        }
                    }
                )
            }
        }

        binding.toolbarEdit.apply {
            title = "Edit"
            menu.clear()
            inflateMenu(R.menu.menu_edit)
            setOnMenuItemClickListener(menuListener)
            setNavigationIcon(R.drawable.baseline_arrow_back_24)
        }
    }

    private fun onBack(corpus: Corpus) {
        if (corpus.isDataChanged()) {
            showAlertDialog(
                requireContext(),
                title = "Discard changes?",
                description = "Are you sure you want to discard changes?",
                positiveButton = "Discard",
            ) {
                findNavController().navigateUp()
            }
        } else {
            findNavController().navigateUp()
        }
    }

    private fun Corpus.isDataChanged(): Boolean {
        val word = binding.etWord.text.toString().lowercase().trim()
        val meaning = binding.etMeaning.text.toString().lowercase().trim()
        return this.word != word || this.meaning != meaning
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
        detailViewModel.updateCorpus(corpus.word, updatedCorpus) {
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