package com.iwsocorp.vocabnotes.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.iwsocorp.vocabnotes.R
import com.iwsocorp.vocabnotes.core.model.Corpus
import com.iwsocorp.vocabnotes.core.model.Note
import com.iwsocorp.vocabnotes.databinding.ItemNoteBinding
import com.iwsocorp.vocabnotes.databinding.ItemWordPreviewBinding
import com.iwsocorp.vocabnotes.ui.home.NoteAdapter.ClickListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NoteAdapter(
    private val noteList: List<Note>,
    private val listener: ClickListener,
) : RecyclerView.Adapter<NoteAdapter.ViewHolder>() {

    interface ClickListener {
        fun onClick(note: Note)
    }

    inner class ViewHolder(val binding: ItemNoteBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(note: Note) = with(binding) {
            tvTitle.apply {
                text = note.title
                visibility = if (note.title.isEmpty()) View.GONE else View.VISIBLE
            }
            tvDate.text = note.updatedAt.asString()

            itemView.setOnClickListener {
                listener.onClick(note)
            }
            rvPreview.adapter = PreviewAdapter(note.content.take(5).sortedBy { it.word }, note.id) {
                listener.onClick(note)
            }
        }

        private fun Long.asString(): String {
            val date = Date(this)
            val format = SimpleDateFormat("MMM dd", Locale.US)
            return format.format(date)
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
        return ViewHolder(
            ItemNoteBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        holder.bind(noteList[position])
    }

    override fun getItemCount(): Int {
        return noteList.size
    }

}

class PreviewAdapter(
    private val corpusList: List<Corpus>,
    private val noteId: String,
    private val onClick: (noteId: String) -> Unit,
) : RecyclerView.Adapter<PreviewAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemWordPreviewBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(corpus: Corpus) = with(binding) {
            tvWord.text = corpus.word
            tvMeaning.text = corpus.meaning
            tvPos.text = corpus.meanings.takeIf { it.isNotEmpty() }?.first()?.partOfSpeech
            cardPos.visibility = if (tvPos.text.isEmpty()) View.GONE else View.VISIBLE
            tvPronun.apply {
                text = corpus.phonetic
                visibility = if (corpus.phonetic.isEmpty()) View.GONE else View.VISIBLE
            }
            itemView.setOnClickListener {
                onClick(noteId)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemWordPreviewBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(corpusList[position])
    }

    override fun getItemCount(): Int {
        return corpusList.size
    }
}