package com.iwsocorp.vobynotes.ui.scan

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.iwsocorp.vobynotes.R
import com.iwsocorp.vobynotes.databinding.ItemScanBinding

class ScanAdapter(
    private val onSelectionChanged: (Int) -> Unit,
) : ListAdapter<WordResult, ScanAdapter.ViewHolder>(DIFF_CALLBACK) {

    private val selectedItem = mutableSetOf<WordResult>()
    private var isSelectionMode = false

    @SuppressLint("NotifyDataSetChanged")
    fun toggleSelection(item: WordResult) {
        if (selectedItem.contains(item)) {
            selectedItem.remove(item)
        } else {
            selectedItem.add(item)
        }
        if (selectedItem.isEmpty()) {
            isSelectionMode = false
        }
        onSelectionChanged(selectedItem.size)
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun clearSelection() {
        selectedItem.clear()
        isSelectionMode = false
        onSelectionChanged(0)
        notifyDataSetChanged()
    }

    fun getSelectedItems(): List<WordResult> = selectedItem.toList()

    @SuppressLint("NotifyDataSetChanged")
    fun clearItems() {
        submitList(emptyList())
        notifyDataSetChanged()
    }

    inner class ViewHolder(
        private val binding: ItemScanBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(wordResult: WordResult) = with(binding) {
            tvWord.text = itemView.context.getString(
                R.string.scan_word,
                wordResult.word,
                wordResult.language
            )
            tvTranslate.text = wordResult.translation

            itemView.setOnClickListener {
                if (isSelectionMode) toggleSelection(wordResult)
            }
            itemView.setOnLongClickListener {
                if (!isSelectionMode) isSelectionMode = true
                toggleSelection(wordResult)
                true
            }
            itemView.setBackgroundColor(
                if (selectedItem.contains(wordResult)) itemView.context.resources.getColor(
                    R.color.light_grey,
                    itemView.context.theme
                ) else android.graphics.Color.TRANSPARENT
            )
        }
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<WordResult>() {
            override fun areItemsTheSame(oldItem: WordResult, newItem: WordResult): Boolean {
                return oldItem.word == newItem.word
            }

            override fun areContentsTheSame(oldItem: WordResult, newItem: WordResult): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemScanBinding.inflate(
                android.view.LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

}