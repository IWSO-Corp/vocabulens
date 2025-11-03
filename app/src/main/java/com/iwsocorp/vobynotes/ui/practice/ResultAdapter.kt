package com.iwsocorp.vobynotes.ui.practice

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.iwsocorp.vobynotes.R
import com.iwsocorp.vobynotes.databinding.ItemQuizResultBinding

class ResultAdapter(
    private val results: List<Triple<String, String, String>>,
) : RecyclerView.Adapter<ResultAdapter.ResultViewHolder>() {

    inner class ResultViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val binding = ItemQuizResultBinding.bind(itemView)

        fun bind(item: Triple<String, String, String>, position: Int) {
            val isCorrect = item.second == item.third

            binding.tvNumber.text = position.toString()
            binding.tvSentence.text =
                HtmlCompat.fromHtml(item.first, HtmlCompat.FROM_HTML_MODE_LEGACY)

            binding.tvCorrectAnswer.isVisible = !item.first.contains(item.second)
            binding.tvAnswer.isVisible = !isCorrect
            binding.tvCorrectAnswer.text =
                itemView.context.getString(R.string.correct_answer_s, item.second)
            binding.tvAnswer.text = itemView.context.getString(R.string.your_answer_s, item.third)

            binding.icon.setImageResource(if (isCorrect) R.drawable.baseline_check_24 else R.drawable.baseline_close_24)
            binding.icon.setColorFilter(
                itemView.context.getColor(
                    if (isCorrect) R.color.green else R.color.red
                )
            )
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ResultViewHolder {
        return ResultViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_quiz_result, parent, false)
        )
    }

    override fun onBindViewHolder(
        holder: ResultViewHolder,
        position: Int,
    ) {
        holder.bind(results[position], position + 1)
    }

    override fun getItemCount(): Int {
        return results.size
    }

}