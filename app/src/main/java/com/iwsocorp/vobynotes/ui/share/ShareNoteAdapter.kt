package com.iwsocorp.vobynotes.ui.share

import android.icu.text.SimpleDateFormat
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.Timestamp
import com.iwsocorp.vobynotes.R
import com.iwsocorp.vobynotes.core.common.Utils.langName
import com.iwsocorp.vobynotes.core.model.SharedNote
import com.iwsocorp.vobynotes.databinding.ItemSharedNoteBinding
import timber.log.Timber
import java.util.Locale

class ShareNoteAdapter(
    private val onClick: (SharedNote, View) -> Unit
) : PagingDataAdapter<SharedNote, ShareNoteAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(private val binding: ItemSharedNoteBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(sharedNote: SharedNote) = with(binding) {
            Timber.d("sharedNote: $sharedNote")
            Glide.with(imgAvatar).load(
                if (sharedNote.ownerAvatar.isNullOrEmpty()) R.drawable.baseline_person_24 else sharedNote.ownerAvatar
            ).into(imgAvatar)
            tvOwner.text =
                if (sharedNote.ownerName.isNullOrEmpty()) "Anonymous" else sharedNote.ownerName
            tvDate.text = sharedNote.uploadedAt.asDateString()
            if (sharedNote.updatedAt.asDateString() != sharedNote.uploadedAt.asDateString()) tvUpdate.text =
                itemView.context.getString(
                    R.string.shared_note_updated_at,
                    sharedNote.updatedAt.asDateString()
                )
            tvTitle.text = sharedNote.title
            tvWordCount.text = itemView.context.getString(
                R.string.word_amount,
                sharedNote.content.size
            )
            tvExampleCount.text = itemView.context.getString(
                R.string.example_amount,
                sharedNote.content.sumOf { it.examples.size }
            )
            tvLang.text = itemView.context.getString(
                R.string.note_lang,
                sharedNote.wordLang.langName(itemView.context),
                sharedNote.meaningLang.langName(itemView.context)
            )
            tvSaveCount.text = sharedNote.savedCount.toString()

            val icon =
                ContextCompat.getDrawable(itemView.context, R.drawable.baseline_file_download_24)
            icon?.setBounds(0, 0, 48, 48)
            tvSaveCount.setCompoundDrawables(icon, null, null, null)

            card.setOnClickListener { onClick(sharedNote, itemView) }
        }

        private fun Timestamp.asDateString(): String =
            SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(this.toDate())
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        return ViewHolder(
            ItemSharedNoteBinding.inflate(
                android.view.LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        getItem(position)?.let { holder.bind(it) }
    }

    object DiffCallback : DiffUtil.ItemCallback<SharedNote>() {
        override fun areItemsTheSame(oldItem: SharedNote, newItem: SharedNote) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: SharedNote, newItem: SharedNote) =
            oldItem == newItem
    }
}