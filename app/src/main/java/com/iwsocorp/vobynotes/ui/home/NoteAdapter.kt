package com.iwsocorp.vobynotes.ui.home

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.iwsocorp.vobynotes.R
import com.iwsocorp.vobynotes.core.common.Utils.asString
import com.iwsocorp.vobynotes.core.common.Utils.langName
import com.iwsocorp.vobynotes.core.common.Utils.setupMark
import com.iwsocorp.vobynotes.core.data.repository.NoteWithCorpus
import com.iwsocorp.vobynotes.core.model.Corpus
import com.iwsocorp.vobynotes.core.model.Note
import com.iwsocorp.vobynotes.databinding.ItemNoteBinding
import com.iwsocorp.vobynotes.databinding.ItemWordPreviewBinding

class NoteAdapter(
    private val listener: ClickListener,
    private val isTrash: Boolean = false,
) : PagingDataAdapter<NoteWithCorpus, NoteAdapter.ViewHolder>(DIFF_CALLBACK) {

    interface ClickListener {
        fun onClick(pos: Int, noteId: String)
        fun getAllCorpusSize(callback: (Int) -> Unit)
        fun onSelectionChanged(size: Int)
        fun getLastFiveCorpus(noteId: String, callback: (List<Corpus>) -> Unit)
    }

    private var isSelectionMode = false
    private val selectedNotes = mutableSetOf<Note>()
    private val corpusCache = mutableMapOf<String, List<Corpus>>()

    inner class ViewHolder(val binding: ItemNoteBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(noteWithCorpus: NoteWithCorpus, pos: Int) = with(binding) {
            val note = noteWithCorpus.note

            tvLang.isVisible = pos != 0
            tvDate.isVisible = pos != 0
            tvFamiliar.isVisible = pos != 0
            tvUnfamiliar.isVisible = pos != 0
            tvTitle.apply {
                text = note.title
                visibility = if (note.title.isEmpty()) View.GONE else View.VISIBLE
            }
            tvDate.text = note.updatedAt.asString()
            tvLang.text =
                itemView.context.getString(
                    R.string.note_lang,
                    note.wordLang.langName(itemView.context),
                    note.meaningLang.langName(itemView.context)
                )

            if (note.id.isNotEmpty()) {
                tvWordCount.text =
                    itemView.context.getString(R.string.word_amount, noteWithCorpus.corpusCount)

                tvFamiliar.isVisible = noteWithCorpus.corpusCount > 0
                tvUnfamiliar.isVisible = noteWithCorpus.corpusCount > 0

                if (noteWithCorpus.corpusCount > 0) setupMark(
                    itemView.context,
                    noteWithCorpus.familiarCount,
                    noteWithCorpus.unfamiliarCount
                )
            } else listener.getAllCorpusSize() {
                tvWordCount.text = itemView.context.getString(R.string.word_amount, it)
            }

            itemView.setOnClickListener {
                if (!isSelectionMode) listener.onClick(
                    absoluteAdapterPosition,
                    note.id
                ) else {
                    if (pos != 0) toggleSelection(note)
                }
            }
            if (pos != 0 || isTrash) itemView.setOnLongClickListener {
                if (!isSelectionMode) isSelectionMode = true
                toggleSelection(note)
                true
            }
            else itemView.setOnLongClickListener(null)

            val isSelected = selectedNotes.contains(note)

            cardNote.backgroundTintList = itemView.context.resources.getColorStateList(
                if (isSelected) R.color.bg_lang else R.color.bg_card,
                itemView.context.theme
            )

            rvPreview.visibility = if (note.contentSize == 0) View.GONE else View.VISIBLE

            val adapter = PreviewAdapter(
                noteId = note.id,
                onClick = {
                    if (!isSelectionMode) listener.onClick(
                        absoluteAdapterPosition,
                        note.id
                    ) else toggleSelection(note)
                },
            ) {
                if (!isSelectionMode) isSelectionMode = true
                toggleSelection(note)
            }

            listener.getLastFiveCorpus(note.id) { newList ->
                val cachedList = corpusCache[note.id]
                if (cachedList != newList) {
                    corpusCache[note.id] = newList
                    adapter.submitList(newList)
                } else {
                    // gunakan cache tanpa animasi / submit ulang
                    adapter.submitList(cachedList)
                }
            }

            rvPreview.adapter = adapter
        }

    }

    @SuppressLint("NotifyDataSetChanged")
    private fun toggleSelection(note: Note) {
        val currentListSnapshot = snapshot()
        val pos = currentListSnapshot.indexOfFirst { it?.note == note }
        if (pos == -1) return

        if (selectedNotes.contains(note)) {
            selectedNotes.remove(note)
        } else {
            selectedNotes.add(note)
        }

        if (selectedNotes.isEmpty()) {
            isSelectionMode = false
        }

        listener.onSelectionChanged(selectedNotes.size)
        notifyItemChanged(pos)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun clearSelection() {
        selectedNotes.clear()
        isSelectionMode = false
        notifyDataSetChanged()
        listener.onSelectionChanged(0)
    }

    fun getSelectedItems(): List<Note> = selectedNotes.toList()

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
        getItem(position)?.let { holder.bind(it, position) }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<NoteWithCorpus>() {
            override fun areItemsTheSame(
                oldItem: NoteWithCorpus,
                newItem: NoteWithCorpus
            ): Boolean =
                oldItem.note.id == newItem.note.id

            override fun areContentsTheSame(
                oldItem: NoteWithCorpus,
                newItem: NoteWithCorpus
            ): Boolean =
                oldItem.note == newItem.note
        }
    }

}

class PreviewAdapter(
    private val noteId: String,
    private val onClick: (noteId: String) -> Unit,
    private val onLongClick: (noteId: String) -> Unit,
) : ListAdapter<Corpus, PreviewAdapter.ViewHolder>(DIFF_CALLBACK) {

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

            val rtlLang = listOf("ar", "fa", "ur")
            tvMeaning.textDirection =
                if (rtlLang.contains(corpus.meaningLang)) View.TEXT_DIRECTION_RTL else View.TEXT_DIRECTION_LTR

            itemView.setOnClickListener {
                onClick(noteId)
            }
            itemView.setOnLongClickListener {
                onLongClick(noteId)
                true
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
        holder.bind(getItem(position))
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Corpus>() {
            override fun areItemsTheSame(oldItem: Corpus, newItem: Corpus): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Corpus, newItem: Corpus): Boolean =
                oldItem == newItem
        }
    }

}