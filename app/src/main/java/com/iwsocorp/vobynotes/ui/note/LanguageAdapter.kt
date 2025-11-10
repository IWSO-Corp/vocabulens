package com.iwsocorp.vobynotes.ui.note

import android.R
import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.iwsocorp.vobynotes.core.model.Language

class LanguageAdapter(
    private val allItems: List<Language>,
    private val onItemClick: (Language) -> Unit
) : RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {

    private var filteredItems = allItems.toMutableList()
    private var searchQuery: String = ""

    inner class LanguageViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        private val lang: TextView = view.findViewById(R.id.text1)

        fun bind(language: Language) {
            lang.text = highlightText(language.name, searchQuery)

            itemView.setOnClickListener {
                onItemClick(language)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.simple_dropdown_item_1line, parent, false)
        return LanguageViewHolder(view)
    }

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        holder.bind(filteredItems[position])
    }

    override fun getItemCount() = filteredItems.size

    @SuppressLint("NotifyDataSetChanged")
    fun filter(query: String) {
        searchQuery = query
        filteredItems = if (query.isBlank()) {
            allItems.toMutableList()
        } else {
            allItems.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.code.contains(query, ignoreCase = true)
            }.toMutableList()
        }
        notifyDataSetChanged()
    }

    private fun highlightText(text: String, query: String): SpannableString {
        val spannable = SpannableString(text)
        if (query.isBlank()) return spannable

        val start = text.lowercase().indexOf(query.lowercase())
        if (start >= 0) {
            spannable.setSpan(
                ForegroundColorSpan(Color.BLUE),
                start,
                start + query.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannable.setSpan(
                StyleSpan(Typeface.BOLD),
                start,
                start + query.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return spannable
    }
}