package com.iwsocorp.vocabnotes.ui.note

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.iwsocorp.vocabnotes.core.model.Corpus
import com.iwsocorp.vocabnotes.databinding.ItemWordBinding
import java.util.Locale

class WordAdapter(
    private val listener: ClickListener,
) : ListAdapter<Corpus, WordAdapter.ViewHolder>(DiffCallback()) {

    // Store the current list of items to append new data
    private val currentListData = mutableListOf<Corpus>()

    // Custom method to append new data without replacing existing data and avoid duplicates
    fun appendData(newData: List<Corpus>) {
        // Filter out the new data that is already present in the current list
        val uniqueNewData = newData.filterNot { newItem ->
            currentListData.any { it.id == newItem.id }
        }

        // Add only unique items to the current list
        currentListData.addAll(uniqueNewData)

        // Submit the updated list to the adapter
        submitList(ArrayList(currentListData))
    }

    interface ClickListener {
        fun onClick(corpus: Corpus)
        fun onPlay(url: String)
    }

    inner class ViewHolder(val binding: ItemWordBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(corpus: Corpus, position: Int) = with(binding) {
            tvNumber.text = String.format(Locale.US, "%d", position + 1)
            tvWord.text = corpus.word
            tvMeaning.text = corpus.meaning
            tvPos.text = corpus.meanings.takeIf { it.isNotEmpty() }?.first()?.partOfSpeech
            cardPos.visibility = if (tvPos.text.isEmpty()) ViewGroup.GONE else ViewGroup.VISIBLE
            underline.visibility = if (corpus.audio.isEmpty()) ViewGroup.GONE else ViewGroup.VISIBLE
            tvPronun.apply {
                text = corpus.phonetic
                visibility = if (corpus.phonetic.isEmpty()) ViewGroup.GONE else ViewGroup.VISIBLE
                setOnClickListener {
                    listener.onPlay(corpus.audio)
                }
            }

            itemView.setOnClickListener {
                listener.onClick(corpus)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemWordBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, position)
    }

    class DiffCallback : DiffUtil.ItemCallback<Corpus>() {
        override fun areItemsTheSame(oldItem: Corpus, newItem: Corpus): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Corpus, newItem: Corpus): Boolean = oldItem == newItem
    }

}