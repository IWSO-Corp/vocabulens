package com.iwsocorp.vocabnotes.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.iwsocorp.vocabnotes.R
import com.iwsocorp.vocabnotes.databinding.BottomSheetExampleBinding

class ExampleBottomSheet(
    private val onSubmit: (String) -> Unit,
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_example, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = BottomSheetExampleBinding.bind(view)
        binding.btnAdd.setOnClickListener {
            val sentence = binding.edExample.text.toString().trim()
            if (sentence.isEmpty()) return@setOnClickListener
            onSubmit(sentence)
            dismiss()
        }
    }
}