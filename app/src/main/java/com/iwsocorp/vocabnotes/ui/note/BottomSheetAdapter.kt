package com.iwsocorp.vocabnotes.ui.note

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.iwsocorp.vocabnotes.R
import com.iwsocorp.vocabnotes.core.common.Utils.asString
import com.iwsocorp.vocabnotes.core.model.Note
import com.iwsocorp.vocabnotes.databinding.ItemNoteBinding

class BottomSheetAdapter(
    private val notes: List<Note>,
    private val onClick: (Note) -> Unit,
) : RecyclerView.Adapter<BottomSheetAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val binding = ItemNoteBinding.bind(view)

        fun bind(note: Note) = with(binding) {
            tvTitle.text = note.title
            tvDate.text = note.createdAt.asString()
            tvWordCount.text = itemView.context.getString(R.string.word_amount, note.contentSize)

            itemView.setOnClickListener {
                onClick(note)
            }
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