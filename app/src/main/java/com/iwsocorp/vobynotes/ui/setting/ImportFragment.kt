package com.iwsocorp.vobynotes.ui.setting

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.iwsocorp.vobynotes.R
import com.iwsocorp.vobynotes.core.common.BaseFragment
import com.iwsocorp.vobynotes.core.common.Utils.langCode
import com.iwsocorp.vobynotes.core.common.Utils.langName
import com.iwsocorp.vobynotes.core.common.Utils.showAlertDialog
import com.iwsocorp.vobynotes.core.model.Corpus
import com.iwsocorp.vobynotes.databinding.FragmentImportBinding
import com.iwsocorp.vobynotes.ui.home.HomeViewModel
import com.iwsocorp.vobynotes.ui.note.LangBottomSheet
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

const val ARG_ID = "noteId"
const val ARG_TITLE = "title"
const val ARG_WORD_LANG = "wordLang"
const val ARG_MEANING_LANG = "meaningLang"

@AndroidEntryPoint
class ImportFragment : BaseFragment<FragmentImportBinding>(FragmentImportBinding::inflate) {

    private val viewModel: ImportViewModel by activityViewModels()
    private val homeViewModel: HomeViewModel by activityViewModels()
    private val languageViewModel: LanguageViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.getString(ARG_TITLE)?.let {
            setupUI(it)
        }
        arguments?.getString(ARG_ID)?.let {
            binding.tvWordLang.text =
                arguments?.getString(ARG_WORD_LANG)?.langName(requireContext())
            binding.tvMeaningLang.text =
                arguments?.getString(ARG_MEANING_LANG)?.langName(requireContext())
            binding.toolbarImport.apply {
                title = "Edit"
                menu.findItem(R.id.action_create).title = "Apply"
            }
        } ?: run {
            binding.toolbarImport.title = "Import"
            languageViewModel.sourceLanguage.collectOnStarted {
                binding.tvWordLang.text = it?.langName(requireContext())
            }
            languageViewModel.translationLanguage.collectOnStarted {
                binding.tvMeaningLang.text = it?.langName(requireContext())
            }
        }

    }

    private fun setupUI(noteTitle: String) = with(binding) {
        toolbarImport.apply {
            setNavigationIcon(R.drawable.baseline_arrow_back_24)
            setNavigationOnClickListener { onBack() }
            menu.clear()
            inflateMenu(R.menu.menu_import)
        }
        etTitle.setText(noteTitle)

        viewModel.importedCorpus.collectOnStarted { list ->
            Timber.d("Imported corpus: $list")
            toolbarImport.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_create -> {
                        if (list.isNotEmpty()) onCreate(list) else onEdit()
                        true
                    }

                    else -> false
                }
            }
        }

        tvWordLang.setOnClickListener {
            LangBottomSheet("Word Language") {
                tvWordLang.text = it.name
            }.show(parentFragmentManager, null)
        }
        tvMeaningLang.setOnClickListener {
            LangBottomSheet("Meaning Language") {
                tvMeaningLang.text = it.name
            }.show(parentFragmentManager, null)
        }
        iconSwitch.setOnClickListener {
            val temp = tvWordLang.text.toString()
            tvWordLang.text = tvMeaningLang.text
            tvMeaningLang.text = temp
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    onBack()
                }
            }
        )
    }

    private fun onEdit() = arguments?.getString(ARG_ID)?.let { id ->
        val newTitle = binding.etTitle.text.toString()
        val wordLang = binding.tvWordLang.text.toString().langCode(requireContext())
        val meaningLang = binding.tvMeaningLang.text.toString().langCode(requireContext())

        viewModel.updateNote(
            noteId = id,
            title = newTitle,
            wordLang = wordLang,
            meaningLang = meaningLang
        ) {
            Toast.makeText(
                requireContext(),
                if (it > 0) "Note updated" else "Failed to update",
                Toast.LENGTH_SHORT
            ).show()
            if (it > 0) findNavController().navigateUp()
        }
    }

    fun onCreate(data: List<Corpus>) {
        val noteTitle = binding.etTitle.text.toString()
        val wordLang = binding.tvWordLang.text.toString().langCode(requireContext())
        val meaningLang = binding.tvMeaningLang.text.toString().langCode(requireContext())
        val list = data.map {
            it.copy(
                wordLang = wordLang,
                meaningLang = meaningLang
            )
        }

        homeViewModel.importCorpusBatch(
            noteTitle,
            list,
            wordLang,
            meaningLang
        ) { result ->
            Toast.makeText(
                requireContext(),
                "Imported ${result.successCount} items, ${result.failedCount} duplicates skipped",
                Toast.LENGTH_LONG
            ).show()

            findNavController().navigateUp()
        }
    }

    private fun onBack() = showAlertDialog(
        requireContext(),
        "Discard Import",
        "Are you sure want to discard?",
        "Discard",
        "Continue",
    ) {
        findNavController().navigateUp()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.setImportedData(emptyList())
    }

}