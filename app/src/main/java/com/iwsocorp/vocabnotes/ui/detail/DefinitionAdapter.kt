package com.iwsocorp.vocabnotes.ui.detail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.iwsocorp.vocabnotes.core.model.Definition
import com.iwsocorp.vocabnotes.databinding.ItemWordDefinitionBinding
import java.util.Locale

class DefinitionAdapter(
    private val definitions: List<Definition>,
) : RecyclerView.Adapter<DefinitionAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemWordDefinitionBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(definition: Definition) = with(binding) {
            tvNumber.text = String.format(Locale.getDefault(), "%d", adapterPosition + 1)
            tvDefinition.text = definition.definition
            tvExample.apply {
                text = String.format(Locale.getDefault(), "\"%s\"", definition.example)
                visibility = if (definition.example.isNullOrEmpty()) View.GONE else View.VISIBLE
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
        return ViewHolder(
            ItemWordDefinitionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(definitions[position])
    }

    override fun getItemCount(): Int {
        return definitions.size
    }
}