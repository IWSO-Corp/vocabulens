package com.iwsocorp.vobynotes.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.iwsocorp.vobynotes.R
import com.iwsocorp.vobynotes.core.common.Utils.asString
import com.iwsocorp.vobynotes.core.data.repository.NoteWithCorpus
import com.iwsocorp.vobynotes.core.model.Corpus
import com.iwsocorp.vobynotes.databinding.ItemNoteBinding
import com.iwsocorp.vobynotes.databinding.ItemWordPreviewBinding

class NoteAdapter(
    private val noteWithCorpus: List<NoteWithCorpus>,
    private val listener: ClickListener,
) : RecyclerView.Adapter<NoteAdapter.ViewHolder>() {

    interface ClickListener {
        fun onClick(pos: Int, noteId: String)
        fun getAllCorpusSize(callback: (Int) -> Unit)
    }

    inner class ViewHolder(val binding: ItemNoteBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(noteWithCorpus: NoteWithCorpus) = with(binding) {
            val note = noteWithCorpus.note

            tvTitle.apply {
                text = note.title
                visibility = if (note.title.isEmpty()) View.GONE else View.VISIBLE
            }
            tvDate.visibility = if (note.updatedAt == 0L) View.GONE else View.VISIBLE
            tvDate.text = note.updatedAt.asString()

            if (note.id.isNotEmpty()) {
                tvWordCount.visibility = if (note.contentSize == 0) View.GONE else View.VISIBLE
                tvWordCount.text =
                    itemView.context.getString(R.string.word_amount, note.contentSize)

                if (note.contentSize > 0) setupMark(
                    noteWithCorpus.familiarCount,
                    noteWithCorpus.unfamiliarCount
                )
            } else listener.getAllCorpusSize() {
                tvWordCount.isVisible = it != 0
                tvWordCount.text = itemView.context.getString(R.string.word_amount, it)
            }

            itemView.setOnClickListener {
                listener.onClick(absoluteAdapterPosition, note.id)
            }

            rvPreview.visibility = if (note.contentSize == 0) View.GONE else View.VISIBLE
            val previewAdapter = PreviewAdapter(
                corpusList = noteWithCorpus.corpus,
                noteId = note.id,
                onClick = { listener.onClick(absoluteAdapterPosition, it) },
            )
            rvPreview.adapter = previewAdapter
        }

        private fun ItemNoteBinding.setupMark(
            familiarCount: Int,
            unfamiliarCount: Int
        ) {
            val iconFam = ContextCompat.getDrawable(itemView.context, R.drawable.baseline_star_24)
            iconFam?.setBounds(0, 0, 48, 48) // width x height dalam px
            iconFam?.setTint(
                itemView.context.resources.getColor(
                    R.color.blue,
                    itemView.context.theme
                )
            )
            val iconUnfam = ContextCompat.getDrawable(itemView.context, R.drawable.baseline_star_24)
            iconUnfam?.setBounds(0, 0, 48, 48) // width x height dalam px
            iconUnfam?.setTint(
                itemView.context.resources.getColor(
                    R.color.red,
                    itemView.context.theme
                )
            )

            tvFamiliar.setCompoundDrawables(iconFam, null, null, null)
            tvUnfamiliar.setCompoundDrawables(iconUnfam, null, null, null)

            tvFamiliar.text = familiarCount.toString()
            tvFamiliar.setTextColor(
                itemView.context.resources.getColor(
                    R.color.blue,
                    itemView.context.theme
                )
            )

            tvUnfamiliar.text = unfamiliarCount.toString()
            tvUnfamiliar.setTextColor(
                itemView.context.resources.getColor(
                    R.color.red,
                    itemView.context.theme
                )
            )

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
        holder.bind(noteWithCorpus[position])
    }

    override fun getItemCount(): Int {
        return noteWithCorpus.size
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