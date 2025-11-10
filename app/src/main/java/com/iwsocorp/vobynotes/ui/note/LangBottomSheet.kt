package com.iwsocorp.vobynotes.ui.note

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.iwsocorp.vobynotes.core.common.Utils.loadLanguages
import com.iwsocorp.vobynotes.core.model.Language
import com.iwsocorp.vobynotes.databinding.FragmentLangBottomSheetBinding

class LangBottomSheet(
    private val title: String,
    private val onLanguageSelected: (Language) -> Unit
) : BottomSheetDialogFragment() {

    private lateinit var adapter: LanguageAdapter
    private lateinit var binding: FragmentLangBottomSheetBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLangBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val languages = loadLanguages(requireContext())
        adapter = LanguageAdapter(languages) {
            onLanguageSelected(it)
            dismiss()
        }

        binding.recyclerViewLanguages.adapter = adapter
        binding.searchEditText.addTextChangedListener { query ->
            adapter.filter(query.toString())
        }
        binding.title.text = title
        binding.iconClose.setOnClickListener {
            dismiss()
        }
    }

}