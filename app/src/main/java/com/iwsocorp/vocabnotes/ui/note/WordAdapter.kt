package com.iwsocorp.vocabnotes.ui.note

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.iwsocorp.vocabnotes.core.model.Corpus
import com.iwsocorp.vocabnotes.databinding.ItemWordBinding
import java.util.Locale

class WordAdapter(
    val corpusList: List<Corpus>,
    val listener: ClickListener,
) : RecyclerView.Adapter<WordAdapter.ViewHolder>() {

    interface ClickListener {
        fun onClick(corpus: Corpus)
    }

    inner class ViewHolder(val binding: ItemWordBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(corpus: Corpus, position: Int) {
            binding.tvNumber.text = String.format(Locale.US, "%d", position + 1)
            binding.tvWord.text = corpus.word
            binding.tvMeaning.text = corpus.meaning
            binding.tvPos.text = corpus.meanings.takeIf { it.isNotEmpty() }?.first()?.partOfSpeech

            itemView.setOnClickListener {
                listener.onClick(corpus)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
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