package com.iwsocorp.vocabnotes.ui.note

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.iwsocorp.vocabnotes.R
import com.iwsocorp.vocabnotes.core.model.Note
import com.iwsocorp.vocabnotes.databinding.BottomSheetListBinding

class NoteBottomSheet(
    private val notes: List<Note>,
    private val onItemClick: (String) -> Unit,
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return inflater.inflate(R.layout.bottom_sheet_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = BottomSheetListBinding.bind(view)
        binding.rvBottomSheet.adapter = BottomSheetAdapter(notes) {
            onItemClick(it)
            dismiss()
        }
    }

}
