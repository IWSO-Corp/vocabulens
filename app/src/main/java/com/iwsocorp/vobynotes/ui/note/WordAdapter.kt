package com.iwsocorp.vobynotes.ui.note

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.daimajia.swipe.SwipeLayout
import com.iwsocorp.vobynotes.R
import com.iwsocorp.vobynotes.core.model.Corpus
import com.iwsocorp.vobynotes.core.model.Mark
import com.iwsocorp.vobynotes.databinding.ItemWordBinding
import java.util.Locale

class WordAdapter(
    private val isNote: Boolean,
    private val listener: ClickListener,
) : PagingDataAdapter<Corpus, WordAdapter.ViewHolder>(DIFF_CALLBACK) {

    interface ClickListener {
        fun onClick(corpus: Corpus)
        fun onPlay(url: String)
        fun onSelectionChanged(size: Int)
        fun onMark(corpus: Corpus)
        fun onEdit(corpus: Corpus)
    }

    private val selectedIds = mutableSetOf<String>()
    private var isSelectionMode = false

    inner class ViewHolder(val binding: ItemWordBinding) : RecyclerView.ViewHolder(binding.root) {

        private var isDragging = false
        private var openedSwipe: SwipeLayout? = null

        fun bind(corpus: Corpus, position: Int) = with(binding) {
            tvNumber.text = String.format(Locale.US, "%d", corpus.indexNumber)
            tvWord.text = corpus.word
            tvMeaning.text = corpus.meaning.ifEmpty { "-" }
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
            iconMark.isVisible = isNote
            iconMark.setOnClickListener {
                listener.onMark(corpus)
            }

            when (corpus.mark) {
                Mark.FAMILIAR -> {
                    iconMark.setImageDrawable(
                        ResourcesCompat.getDrawable(
                            itemView.context.resources,
                            R.drawable.baseline_star_24,
                            itemView.context.theme
                        )
                    )
                    iconMark.setColorFilter(
                        itemView.context.resources.getColor(
                            R.color.blue,
                            itemView.context.theme
                        )
                    )
                }

                Mark.UNFAMILIAR,
                    -> {
                    iconMark.setImageDrawable(
                        ResourcesCompat.getDrawable(
                            itemView.context.resources,
                            R.drawable.baseline_star_24,
                            itemView.context.theme
                        )
                    )
                    iconMark.setColorFilter(
                        itemView.context.resources.getColor(
                            R.color.red,
                            itemView.context.theme
                        )
                    )
                }

                Mark.UNMARKED -> {
                    iconMark.setImageDrawable(
                        ResourcesCompat.getDrawable(
                            itemView.context.resources,
                            R.drawable.outline_star_border_24,
                            itemView.context.theme
                        )
                    )
                    iconMark.setColorFilter(
                        itemView.context.resources.getColor(
                            R.color.grey,
                            itemView.context.theme
                        )
                    )
                }
            }

            val isSelected = selectedIds.contains(corpus.id)

            itemView.setBackgroundColor(
                if (isSelected) itemView.context.resources.getColor(
                    R.color.light_grey,
                    itemView.context.theme
                ) else Color.TRANSPARENT
            )

            swipeLayout.surfaceView.setOnClickListener {
                if (!isDragging) {
                    if (isSelectionMode) {
                        toggleSelection(corpus.id)
                    } else {
                        listener.onClick(corpus)
                    }
                } else {
                    swipeLayout.close()
                }
            }
            swipeLayout.surfaceView.setOnLongClickListener {
                if (!isDragging && isNote) {
                    if (!isSelectionMode) isSelectionMode = true
                    toggleSelection(corpus.id)
                    true
                } else {
                    false
                }
            }

            btnEdit.setOnClickListener {
                listener.onEdit(corpus)
                swipeLayout.close()
            }

            swipeLayout.isSwipeEnabled = !isSelectionMode
            swipeLayout.showMode = SwipeLayout.ShowMode.PullOut
            swipeLayout.addDrag(
                SwipeLayout.DragEdge.Right,
                layoutHidden
            )
            swipeLayout.addDrag(
                SwipeLayout.DragEdge.Left,
                layoutHidden
            )

            swipeLayout.addSwipeListener(object : SwipeLayout.SwipeListener {
                override fun onStartOpen(layout: SwipeLayout) {
                    isDragging = true
                    // Tutup swipe sebelumnya kalau ada
                    if (openedSwipe != null && openedSwipe != layout) {
                        openedSwipe?.close()
                    }
                    openedSwipe = layout
                }

                override fun onOpen(layout: SwipeLayout) {
                    isDragging = true
                    openedSwipe = layout
                }

                override fun onStartClose(layout: SwipeLayout) {
                    isDragging = true
                }

                override fun onClose(layout: SwipeLayout) {
                    isDragging = false
                    if (openedSwipe == layout) {
                        openedSwipe = null
                    }
                }

                override fun onUpdate(layout: SwipeLayout, leftOffset: Int, topOffset: Int) {}
                override fun onHandRelease(layout: SwipeLayout, xvel: Float, yvel: Float) {}
            })
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun toggleSelection(id: String) {
        if (selectedIds.contains(id)) {
            selectedIds.remove(id)
        } else {
            selectedIds.add(id)
        }
        if (selectedIds.isEmpty()) {
            isSelectionMode = false
        }
        listener.onSelectionChanged(selectedIds.size)
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun clearSelection() {
        selectedIds.clear()
        isSelectionMode = false
        notifyDataSetChanged()
        listener.onSelectionChanged(0)
    }

    fun getSelectedItems(): List<String> = selectedIds.toList()

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
        getItem(position)?.let {
            holder.bind(it, position)
        }
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.binding.swipeLayout.close()
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