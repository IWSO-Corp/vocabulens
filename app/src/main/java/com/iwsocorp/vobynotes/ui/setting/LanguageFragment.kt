package com.iwsocorp.vobynotes.ui.setting

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.iwsocorp.vobynotes.R
import com.iwsocorp.vobynotes.core.common.BaseFragment
import com.iwsocorp.vobynotes.core.common.Utils.langName
import com.iwsocorp.vobynotes.core.common.Utils.loadLanguages
import com.iwsocorp.vobynotes.core.common.Utils.normalizeLanguageCode
import com.iwsocorp.vobynotes.databinding.FragmentLanguageBinding
import com.iwsocorp.vobynotes.ui.note.LangBottomSheet
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.util.Locale

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
//            if (it == null) viewModel.setAppLanguage(getLocalLang())

            binding.tvLanguage.text = getString(R.string.english)
        }
        viewModel.translationLanguage.collectOnStarted {
            if (it == null) viewModel.setTranslationLanguage(getLocalLang())

            binding.tvTranslation.text = it?.langName(requireContext())
        }
    }

    private fun getLocalLang(): String {
        val languages = loadLanguages(requireContext())
        val localeCode = Locale.getDefault().language
        return languages.find {
            it.code == localeCode.normalizeLanguageCode()
        }?.code ?: "en"
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
                Toast.makeText(
                    requireContext(),
                    "Word will be translated to ${it.name}",
                    Toast.LENGTH_SHORT
                ).show()
            }.show(childFragmentManager, null)
        }
    }

}