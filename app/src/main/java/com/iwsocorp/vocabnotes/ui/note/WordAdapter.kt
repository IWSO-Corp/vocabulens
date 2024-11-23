package com.iwsocorp.vocabnotes.ui.note

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.iwsocorp.vocabnotes.core.model.Corpus
import com.iwsocorp.vocabnotes.databinding.ItemWordBinding
import java.util.Locale

class WordAdapter(
    private val corpusList: List<Corpus>,
    private val listener: ClickListener,
) : RecyclerView.Adapter<WordAdapter.ViewHolder>() {

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
        holder.bind(corpusList[position], position)
    }

    override fun getItemCount(): Int {
        return corpusList.size
    }
}