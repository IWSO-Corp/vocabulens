package com.iwsocorp.vobynotes.ui.note

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.iwsocorp.vobynotes.R
import com.iwsocorp.vobynotes.core.model.Note
import com.iwsocorp.vobynotes.databinding.BottomSheetListBinding

class NoteBottomSheet(
    private val notes: List<Note>,
    private val onNewNote: (() -> Unit)? = null,
    private val onItemClick: (Note) -> Unit,
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return inflater.inflate(R.layout.bottom_sheet_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = BottomSheetListBinding.bind(view)

        binding.cardNewNote.isVisible = onNewNote != null
        binding.cardNewNote.setOnClickListener {
            onNewNote?.invoke()
            dismiss()
        }
        binding.rvBottomSheet.adapter = BottomSheetAdapter(notes) { note ->
            onItemClick(note)
            dismiss()
        }
        binding.iconClose.setOnClickListener {
            dismiss()
        }
    }

}
