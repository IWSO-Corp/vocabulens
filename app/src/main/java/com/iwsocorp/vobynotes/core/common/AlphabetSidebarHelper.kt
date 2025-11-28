package com.iwsocorp.vobynotes.core.common

import android.content.Context
import android.graphics.Typeface
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.iwsocorp.vobynotes.R
import com.iwsocorp.vobynotes.core.model.Corpus
import com.iwsocorp.vobynotes.ui.note.WordAdapter

class AlphabetSidebarHelper(
    private val context: Context,
    private val sidebarContainer: LinearLayout,
    private val recyclerView: RecyclerView,
    private val adapter: WordAdapter
) {

    fun observePagesUpdates() = adapter.addOnPagesUpdatedListener {
        val currentList = adapter.snapshot().items
        val letters = extractAvailableLettersFromLoadedPages(currentList)
        populateAlphabetSidebar(currentList, letters)
    }

    val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)

            val layoutManager = recyclerView.layoutManager as LinearLayoutManager
            val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
            val data = adapter.snapshot().items

            if (firstVisiblePosition != RecyclerView.NO_POSITION && firstVisiblePosition < data.size) {
                val firstCorpus = data[firstVisiblePosition]
                if (firstCorpus.word.isEmpty()) return

                val firstLetter = firstCorpus.word.first().uppercaseChar()

                highlightCurrentLetter(firstLetter)
            }
        }
    }

    private fun highlightCurrentLetter(currentLetter: Char) {
        for (i in 0 until sidebarContainer.childCount) {
            val textView = sidebarContainer.getChildAt(i) as TextView
            textView.apply {
                setTextColor(ContextCompat.getColor(context, R.color.grey))
                setTypeface(null, Typeface.NORMAL)
            }
        }

        for (i in 0 until sidebarContainer.childCount) {
            val textView = sidebarContainer.getChildAt(i) as TextView
            if (textView.text.toString() == currentLetter.toString()) {
                textView.apply {
                    textSize = 20f
                    setTextColor(ContextCompat.getColor(context, R.color.onPrimary))
                    setTypeface(null, Typeface.BOLD)
                }
                break
            }
        }
    }

    private fun extractAvailableLettersFromLoadedPages(currentList: List<Corpus>): List<Char> {
        val letters = currentList.mapNotNull {
            if (it.word.isEmpty()) return emptyList()
            it.word.firstOrNull()?.uppercaseChar()
        }.distinct().sorted()
        return letters
    }

    private fun populateAlphabetSidebar(currentList: List<Corpus>, alphabetSet: List<Char>) {
        sidebarContainer.removeAllViews()
        alphabetSet.forEach { letter ->
            val textView = TextView(context).apply {
                text = letter.toString()
                textSize = 20f
                setOnClickListener { scrollToLetter(currentList, letter) }
            }
            sidebarContainer.addView(textView)
        }
    }

    private fun scrollToLetter(currentList: List<Corpus>, letter: Char) {
        val position = currentList.indexOfFirst {
            it.word.firstOrNull()?.uppercaseChar() == letter
        }
        if (position != -1) {
            val layoutManager = recyclerView.layoutManager as? LinearLayoutManager
            layoutManager?.scrollToPositionWithOffset(position, 0)
        }
    }
}