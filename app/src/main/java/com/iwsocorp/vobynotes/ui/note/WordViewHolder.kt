package com.iwsocorp.vobynotes.ui.note

import android.graphics.Color
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.daimajia.swipe.SwipeLayout
import com.iwsocorp.vobynotes.R
import com.iwsocorp.vobynotes.core.model.Corpus
import com.iwsocorp.vobynotes.core.model.Mark
import com.iwsocorp.vobynotes.databinding.ItemWordBinding
import java.util.Locale

class WordViewHolder(
    val binding: ItemWordBinding,
    private val isNote: Boolean,
    private val listener: WordAdapter.ClickListener,
    private val selectedIds: MutableSet<String>,
    private var isSelectionMode: () -> Boolean,
    private val setSelectionMode: (Boolean) -> Unit,
    private val toggleSelection: (String) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    private var isDragging = false
    private var openedSwipe: SwipeLayout? = null

    fun bind(corpus: Corpus) = with(binding) {
        tvNumber.text = String.format(Locale.US, "%d", corpus.indexNumber)
        tvWord.text = corpus.word
        tvMeaning.text = corpus.meaning.ifEmpty { "-" }
        tvPos.text = corpus.meanings.takeIf { it.isNotEmpty() }?.first()?.partOfSpeech
        cardPos.isVisible = tvPos.text.isNotEmpty()
        underline.isVisible = corpus.audio.isNotEmpty()

        tvPronun.apply {
            text = corpus.phonetic
            isVisible = corpus.phonetic.isNotEmpty()
            setOnClickListener { listener.onPlay(corpus.audio) }
        }

        val rtlLang = listOf("ar", "fa", "ur")
        tvMeaning.textDirection =
            if (rtlLang.contains(corpus.meaningLang)) View.TEXT_DIRECTION_RTL else View.TEXT_DIRECTION_LTR

        // mark icon setup
        iconMark.isVisible = isNote
        iconMark.setOnClickListener { listener.onMark(corpus) }

        when (corpus.mark) {
            Mark.FAMILIAR -> setMarkIcon(R.drawable.baseline_star_24, R.color.blue)
            Mark.UNFAMILIAR -> setMarkIcon(R.drawable.baseline_star_24, R.color.red)
            Mark.UNMARKED -> setMarkIcon(R.drawable.outline_star_border_24, R.color.grey)
        }

        val isSelected = selectedIds.contains(corpus.id)
        itemView.setBackgroundColor(
            if (isSelected)
                itemView.context.getColor(R.color.bg_lang)
            else
                Color.TRANSPARENT
        )

        // handle click
        swipeLayout.surfaceView.setOnClickListener {
            if (!isDragging) {
                if (isSelectionMode()) {
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
                if (!isSelectionMode()) setSelectionMode(true)
                toggleSelection(corpus.id)
                true
            } else false
        }

        btnEdit.setOnClickListener {
            listener.onEdit(corpus)
            swipeLayout.close()
        }

        // swipe configuration
        swipeLayout.isSwipeEnabled = !isSelectionMode()
        swipeLayout.showMode = SwipeLayout.ShowMode.PullOut
        swipeLayout.addDrag(SwipeLayout.DragEdge.Right, layoutHidden)
        swipeLayout.addDrag(SwipeLayout.DragEdge.Left, layoutHidden)

        swipeLayout.addSwipeListener(object : SwipeLayout.SwipeListener {
            override fun onStartOpen(layout: SwipeLayout) {
                isDragging = true
                if (openedSwipe != null && openedSwipe != layout) openedSwipe?.close()
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
                if (openedSwipe == layout) openedSwipe = null
            }

            override fun onUpdate(layout: SwipeLayout, leftOffset: Int, topOffset: Int) {}
            override fun onHandRelease(layout: SwipeLayout, xvel: Float, yvel: Float) {}
        })
    }

    private fun setMarkIcon(drawableRes: Int, colorRes: Int) = with(binding.iconMark) {
        setImageDrawable(
            ResourcesCompat.getDrawable(
                itemView.context.resources,
                drawableRes,
                itemView.context.theme
            )
        )
        setColorFilter(itemView.context.getColor(colorRes))
    }
}