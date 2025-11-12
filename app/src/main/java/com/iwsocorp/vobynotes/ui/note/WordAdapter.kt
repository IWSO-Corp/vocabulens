package com.iwsocorp.vobynotes.ui.note

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import com.iwsocorp.vobynotes.core.model.Corpus
import com.iwsocorp.vobynotes.databinding.ItemWordBinding

class WordAdapter(
    private val isNote: Boolean,
    private val listener: ClickListener,
) : PagingDataAdapter<Corpus, WordViewHolder>(DIFF_CALLBACK) {

    interface ClickListener {
        fun onClick(corpus: Corpus)
        fun onPlay(url: String)
        fun onSelectionChanged(size: Int)
        fun onMark(corpus: Corpus)
        fun onEdit(corpus: Corpus)
    }

    private val selectedIds = mutableSetOf<String>()
    private var isSelectionMode = false

    @SuppressLint("NotifyDataSetChanged")
    private fun toggleSelection(id: String) {
        if (selectedIds.contains(id)) {
            selectedIds.remove(id)
        } else {
            selectedIds.add(id)
        }
        if (selectedIds.isEmpty()) {
            isSelectionMode = false
        }
        listener.onSelectionChanged(selectedIds.size)
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun clearSelection() {
        selectedIds.clear()
        isSelectionMode = false
        notifyDataSetChanged()
        listener.onSelectionChanged(0)
    }

    fun getSelectedItems(): List<String> = selectedIds.toList()

    fun getSelectedItemLang(): String = getItem(0)?.wordLang ?: "en"

    fun getSelectedItemMeaningLang(): String = getItem(0)?.meaningLang ?: "en"

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WordViewHolder {
        val binding = ItemWordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WordViewHolder(
            binding = binding,
            isNote = isNote,
            listener = listener,
            selectedIds = selectedIds,
            isSelectionMode = { isSelectionMode },
            setSelectionMode = { isSelectionMode = it },
            toggleSelection = { toggleSelection(it) }
        )
    }

    override fun onBindViewHolder(holder: WordViewHolder, position: Int) {
        getItem(position)?.let {
            holder.bind(it)
        }
    }

    override fun onViewRecycled(holder: WordViewHolder) {
        super.onViewRecycled(holder)
        holder.binding.swipeLayout.close()
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Corpus>() {
            override fun areItemsTheSame(oldItem: Corpus, newItem: Corpus): Boolean =
                oldItem.word == newItem.word

            override fun areContentsTheSame(oldItem: Corpus, newItem: Corpus): Boolean =
                oldItem == newItem
        }
    }

}