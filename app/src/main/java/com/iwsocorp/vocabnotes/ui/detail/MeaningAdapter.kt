package com.iwsocorp.vocabnotes.ui.detail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.iwsocorp.vocabnotes.core.model.Meaning
import com.iwsocorp.vocabnotes.databinding.ItemDefinitionBinding

class MeaningAdapter(
    private val meanings: List<Meaning>,
) : RecyclerView.Adapter<MeaningAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemDefinitionBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(meaning: Meaning) = with(binding) {
            tvPartOfSpeech.text = meaning.partOfSpeech
            rvDefinitions.adapter = DefinitionAdapter(meaning.definitions)
            tvSynonyms.text = meaning.synonyms.joinToString()
            tvAntonyms.text = meaning.antonyms.joinToString()
            llSynonyms.visibility = if (meaning.synonyms.isEmpty()) View.GONE else View.VISIBLE
            llAntonyms.visibility = if (meaning.antonyms.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
        return ViewHolder(
            ItemDefinitionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(meanings[position])
    }

    override fun getItemCount(): Int {
        return meanings.size
    }

}