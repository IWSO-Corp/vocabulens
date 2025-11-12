package com.iwsocorp.vobynotes.ui.setting

import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.iwsocorp.vobynotes.core.common.Utils.langName
import com.iwsocorp.vobynotes.databinding.ItemLanguageBinding

class LanguageAdapter(
    private val onDelete: (String, (Boolean) -> Unit) -> Unit
) : ListAdapter<String, LanguageAdapter.LanguageViewHolder>(DIFF_CALLBACK) {

    inner class LanguageViewHolder(private val binding: ItemLanguageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(language: String, position: Int) = with(binding) {
            tvLanguage.text = language.langName(root.context)
            icon.setOnClickListener {
                onDelete(language) {
                    if (it) notifyItemRemoved(position)
                }
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): LanguageViewHolder {
        return LanguageViewHolder(
            ItemLanguageBinding.inflate(
                android.view.LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(
        holder: LanguageViewHolder,
        position: Int
    ) {
        holder.bind(getItem(position), position)
    }

    companion object {
        private val DIFF_CALLBACK = object : androidx.recyclerview.widget.DiffUtil.ItemCallback<String>() {
            override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
                return oldItem == newItem
            }
            override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
                return oldItem == newItem
            }
        }
    }

}