package com.iwsocorp.vobynotes.ui.note

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.iwsocorp.vobynotes.R
import com.iwsocorp.vobynotes.core.common.Utils.asString
import com.iwsocorp.vobynotes.core.common.Utils.setupMark
import com.iwsocorp.vobynotes.core.data.repository.NoteWithCorpus
import com.iwsocorp.vobynotes.core.model.Note
import com.iwsocorp.vobynotes.databinding.ItemNoteBinding

class BottomSheetAdapter(
    private val notes: List<NoteWithCorpus>,
    private val onClick: (Note) -> Unit,
) : RecyclerView.Adapter<BottomSheetAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val binding = ItemNoteBinding.bind(view)

        fun bind(item: NoteWithCorpus) = with(binding) {
            val note = item.note
            tvTitle.text = note.title
            tvDate.text = note.createdAt.asString()
            tvWordCount.text = itemView.context.getString(R.string.word_amount, item.corpusCount)

            itemView.setOnClickListener {
                onClick(note)
            }

            if (item.corpusCount > 0) setupMark(
                itemView.context,
                item.familiarCount,
                item.unfamiliarCount
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_note, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(notes[position])
    }

    override fun getItemCount() = notes.size
}