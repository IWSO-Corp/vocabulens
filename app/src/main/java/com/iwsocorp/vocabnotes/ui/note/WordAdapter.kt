package com.iwsocorp.vocabnotes.ui.note

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.iwsocorp.vocabnotes.core.model.Corpus
import com.iwsocorp.vocabnotes.databinding.ItemWordBinding
import java.util.Locale

class WordAdapter(
    private val listener: ClickListener,
) : PagingDataAdapter<Corpus, WordAdapter.ViewHolder>(DIFF_CALLBACK) {

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
            cardPos.isVisible = tvPos.text.isNotEmpty()
            underline.isVisible = corpus.audio.isNotEmpty()
            tvPronun.apply {
                text = corpus.phonetic
                isVisible = corpus.phonetic.isNotEmpty()
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
        item?.let {
            holder.bind(item, position)
        }
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