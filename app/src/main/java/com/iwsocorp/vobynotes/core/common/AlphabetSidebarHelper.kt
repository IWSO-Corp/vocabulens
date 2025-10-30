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

class AlphabetSidebarHelper(
    private val context: Context,
    private val sidebarContainer: LinearLayout,
    private val recyclerView: RecyclerView,
) {

    fun updateSidebarFromData(currentList: List<Corpus>) {
        val letters = extractAvailableLettersFromLoadedPages(currentList)
        populateAlphabetSidebar(currentList, letters)
    }

    fun highlightCurrentLetter(currentLetter: Char) {
        for (i in 0 until sidebarContainer.childCount) {
            val textView = sidebarContainer.getChildAt(i) as TextView
            textView.apply {
                setTextColor(ContextCompat.getColor(context, R.color.grey))
                setTypeface(null, Typeface.NORMAL)
                textSize = 16f
            }
        }

        for (i in 0 until sidebarContainer.childCount) {
            val textView = sidebarContainer.getChildAt(i) as TextView
            if (textView.text.toString() == currentLetter.toString()) {
                textView.apply {
                    textSize = 20f
                    setTextColor(ContextCompat.getColor(context, R.color.black))
                    setTypeface(null, Typeface.BOLD)
                }
                break
            }
        }
    }

    private fun extractAvailableLettersFromLoadedPages(currentList: List<Corpus>): List<Char> {
        val letters = currentList.mapNotNull {
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