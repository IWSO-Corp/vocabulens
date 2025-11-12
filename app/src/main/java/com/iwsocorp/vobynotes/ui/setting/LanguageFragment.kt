package com.iwsocorp.vobynotes.ui.setting

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.iwsocorp.vobynotes.R
import com.iwsocorp.vobynotes.core.common.BaseFragment
import com.iwsocorp.vobynotes.core.common.Utils.langName
import com.iwsocorp.vobynotes.databinding.FragmentLanguageBinding
import com.iwsocorp.vobynotes.ui.note.LangBottomSheet
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LanguageFragment : BaseFragment<FragmentLanguageBinding>(FragmentLanguageBinding::inflate) {

    private val viewModel: LanguageViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        observe()
    }

    private fun observe() {
        viewModel.appLanguage.collectOnStarted {
            binding.tvLanguage.text = getString(R.string.english)
        }
        viewModel.translationLanguage.collectOnStarted {
            binding.tvTranslation.text = it?.langName(requireContext())
        }

        val adapter = LanguageAdapter() { lang, onDelete ->
            val langName = lang.langName(requireContext())
            AlertDialog.Builder(requireContext())
                .setTitle("Delete model")
                .setMessage("Are you sure want to delete $langName model?")
                .setPositiveButton("Delete") { _, _ ->
                    viewModel.deleteModel(lang) { success ->
                        onDelete(success)
                        Toast.makeText(
                            requireContext(),
                            if (success) "Model $langName deleted" else "Error deleting model $langName",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
        binding.rvLanguage.adapter = adapter

        viewModel.checkDownloadedModels { languages ->
            adapter.submitList(languages.sortedBy { it.langName(requireContext()) })
        }
    }

    private fun setupUI() = with(binding) {
        toolbarLang.apply {
            title = getString(R.string.language_translations)
            setNavigationIcon(R.drawable.baseline_arrow_back_24)
            setNavigationOnClickListener {
                findNavController().navigateUp()
            }
        }
        btnLanguage.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "Coming soon",
                Toast.LENGTH_SHORT
            ).show()
//            LangBottomSheet("App language") {
//                viewModel.setAppLanguage(it.code)
//            }.show(childFragmentManager, null)
        }
        btnTranslations.setOnClickListener {
            LangBottomSheet("Translation language") {
                viewModel.setTranslationLanguage(it.code)
                viewModel.setInfoShowed(null)
                Toast.makeText(
                    requireContext(),
                    "Word will be translated to ${it.name}",
                    Toast.LENGTH_SHORT
                ).show()
            }.show(childFragmentManager, null)
        }
    }

}