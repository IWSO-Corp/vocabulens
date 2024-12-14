package com.iwsocorp.vocabnotes.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
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
    private val viewModel: HomeViewModel,
    private val listener: ClickListener,
) : ListAdapter<Note, NoteAdapter.ViewHolder>(DiffCallback()) {

    interface ClickListener {
        fun onClick(noteId: String)
    }

    inner class ViewHolder(val binding: ItemNoteBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(note: Note) = with(binding) {
            tvTitle.apply {
                text = note.title
                visibility = if (note.title.isEmpty()) View.GONE else View.VISIBLE
            }
            tvDate.visibility = if (note.updatedAt == 0L) View.GONE else View.VISIBLE
            tvDate.text = note.updatedAt.asString()
            tvWordCount.visibility = if (note.contentSize == 0) View.GONE else View.VISIBLE
            tvWordCount.text = itemView.context.getString(R.string.word_amount, note.contentSize)

            itemView.setOnClickListener {
                listener.onClick(note.id)
            }

            rvPreview.visibility = if (note.contentSize == 0) View.GONE else View.VISIBLE
            val previewAdapter = PreviewAdapter(note.id) { listener.onClick(it) }
            rvPreview.adapter = previewAdapter
            viewModel.getCorpusByNoteId(note.id) {
                previewAdapter.submitList(it)
            }
        }

        private fun Long.asString(): String {
            val date = Date(this)
            val format = SimpleDateFormat("MMM dd", Locale.US)
            return format.format(date)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemNoteBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    class DiffCallback : DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean = oldItem == newItem
    }

}

class PreviewAdapter(
    private val noteId: String,
    private val onClick: (noteId: String) -> Unit,
) : ListAdapter<Corpus, PreviewAdapter.ViewHolder>(DiffCallback()) {

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
        val item = getItem(position)
        holder.bind(item)
    }

    class DiffCallback : DiffUtil.ItemCallback<Corpus>() {
        override fun areItemsTheSame(oldItem: Corpus, newItem: Corpus): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Corpus, newItem: Corpus): Boolean =
            oldItem == newItem
    }

}