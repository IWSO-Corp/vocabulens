package com.iwsocorp.vobynotes.ui.anki

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.iwsocorp.vobynotes.R
import com.iwsocorp.vobynotes.core.database.dao.NoteExportStat
import com.iwsocorp.vobynotes.databinding.ItemDropdownNoteBinding

class NoteDropdownAdapter(
    context: Context,
    private val items: List<NoteExportStat>
) : ArrayAdapter<NoteExportStat>(context, R.layout.item_dropdown_note, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createView(position, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createView(position, parent)
    }

    private fun createView(position: Int, parent: ViewGroup): View {
        val binding = ItemDropdownNoteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        val item = items[position]
        val meaningless = item.unexportedWithoutMeaning
        val meaningful = item.unexportedWithMeaning
        val updated = item.unexportedWithAnkiNoteId
        val total = meaningless + meaningful + updated

        binding.tvTitle.text =
            context.getString(R.string.dropdown_title, item.title, item.wordLang, item.meaningLang)
        binding.tvSubtitle.text = context.getString(
            R.string.dropdown_info,
            total,
            meaningless,
            meaningful,
            updated
        )

        return binding.root
    }
}