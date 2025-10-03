package com.iwsocorp.vobynotes.ui.detail

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.iwsocorp.vobynotes.R
import com.iwsocorp.vobynotes.core.common.TextViewGestureHelper
import com.iwsocorp.vobynotes.databinding.ItemExampleBinding

class ExampleAdapter(
    private val corpusWord: String,
    private val gestureHelper: TextViewGestureHelper
) : ListAdapter<String, ExampleAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_example, parent, false)
        )
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        holder.bind(getItem(position), position)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val binding = ItemExampleBinding.bind(itemView)

        fun bind(example: String, position: Int) = with(binding) {
            tvNumber.text = (position + 1).toString()
            tvExample.text = boldWordInSentence(example, corpusWord)

            gestureHelper.attachTo(tvExample)
        }

        fun boldWordInSentence(sentence: String, word: String): SpannableString {
            val spannable = SpannableString(sentence)
            val start = sentence.indexOf(word, ignoreCase = true)

            if (start >= 0) {
                val end = start + word.length
                spannable.setSpan(
                    StyleSpan(Typeface.BOLD),
                    start,
                    end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            return spannable
        }
    }

    fun addItems(newItems: List<String>) {
        val currentList = ArrayList(currentList) // copy dari ListAdapter
        currentList.addAll(newItems)
        submitList(currentList)
    }


    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<String>() {
            override fun areItemsTheSame(
                oldItem: String,
                newItem: String,
            ): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(
                oldItem: String,
                newItem: String,
            ): Boolean {
                return oldItem == newItem
            }

        }
    }
}