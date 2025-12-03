package com.iwsocorp.vobynotes.ui.search

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.iwsocorp.vobynotes.databinding.ItemWordBinding
import com.iwsocorp.vobynotes.ui.note.WordAdapter
import com.iwsocorp.vobynotes.ui.note.WordViewHolder

class SearchAdapter(
    private val wordListener: WordAdapter.ClickListener,
    private val onNoteClick: (String) -> Unit
) : PagingDataAdapter<SearchUiModel, RecyclerView.ViewHolder>(diff) {

    inner class HeaderVH(view: View) : RecyclerView.ViewHolder(view) {
        private val title: TextView = view.findViewById(android.R.id.text1)

        fun bind(header: SearchUiModel.Header) {
            title.text = header.noteTitle
            title.setTypeface(title.typeface, Typeface.BOLD)
            itemView.setOnClickListener {
                onNoteClick(header.noteId)
            }
        }
    }

    override fun getItemViewType(position: Int): Int =
        when (peek(position)) {
            is SearchUiModel.Header -> 0
            is SearchUiModel.Item -> 1
            else -> 1
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return if (viewType == 0) {
            HeaderVH(inflater.inflate(android.R.layout.simple_list_item_1, parent, false))
        } else {
            WordViewHolder(
                ItemWordBinding.inflate(inflater, parent, false),
                false,
                wordListener,
                mutableSetOf(),
                { false },
                {},
                {}
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is SearchUiModel.Header -> (holder as HeaderVH).bind(item)
            is SearchUiModel.Item -> (holder as WordViewHolder).bind(item.data.corpus)
            else -> {}
        }
    }
}

val diff = object : DiffUtil.ItemCallback<SearchUiModel>() {
    override fun areItemsTheSame(old: SearchUiModel, new: SearchUiModel): Boolean {
        return when {
            old is SearchUiModel.Header && new is SearchUiModel.Header ->
                old.noteId == new.noteId

            old is SearchUiModel.Item && new is SearchUiModel.Item ->
                old.data.corpus.id == new.data.corpus.id

            else -> false
        }
    }

    override fun areContentsTheSame(old: SearchUiModel, new: SearchUiModel): Boolean {
        return old == new
    }
}
