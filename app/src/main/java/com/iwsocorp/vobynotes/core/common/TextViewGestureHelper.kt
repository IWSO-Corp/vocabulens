package com.iwsocorp.vobynotes.core.common

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.view.GestureDetector
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.text.clearSpans
import com.iwsocorp.vobynotes.R
import com.iwsocorp.vobynotes.databinding.PopupWordBinding

class TextViewGestureHelper(
    context: Context,
    private val currentWord: String,
    private val onClickListener: (String) -> Unit,
) {

    private var currentTextView: TextView? = null
    private val gestureDetector: GestureDetector =
        GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                onSelect(e)
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                super.onLongPress(e)
                onSelect(e)
            }
        })

    @SuppressLint("ClickableViewAccessibility")
    fun attachTo(textView: TextView) {
        textView.setTextIsSelectable(true)
        textView.setOnTouchListener { v, event ->
            currentTextView = textView
            gestureDetector.onTouchEvent(event)
            true
        }
    }

    private fun onSelect(e: MotionEvent) = currentTextView?.let { textView ->
        val result = getWordAndPosition(textView, e)
        result?.let { (word, rect) ->
            if (currentWord != word) showWordPopup(textView, word, rect)
        }
    }

    private fun getWordAndPosition(textView: TextView, event: MotionEvent): Pair<String, Rect>? {
        val x = event.x.toInt()
        val y = event.y.toInt()
        val layout = textView.layout ?: return null

        val line = layout.getLineForVertical(y)
        val offset = layout.getOffsetForHorizontal(line, x.toFloat())

        val text = textView.text.toString()
        if (offset < 0 || offset >= text.length) return null

        // Cari awal kata
        var start = offset
        while (start > 0 && !text[start - 1].isWhitespace()) start--

        // Cari akhir kata
        var end = offset
        while (end < text.length && !text[end].isWhitespace()) end++

        val word = text.substring(start, end).replace(Regex("[^A-Za-z0-9]"), "")

        // Hitung koordinat kata di layar
        val startX = layout.getPrimaryHorizontal(start).toInt()
        val endX = layout.getPrimaryHorizontal(end).toInt()
        val baseline = layout.getLineBaseline(line)
        val ascent = layout.getLineAscent(line)

        val rect = Rect(
            startX,
            baseline + ascent,
            endX,
            baseline
        )
        // Offset relatif ke layar
        val location = IntArray(2)
        textView.getLocationOnScreen(location)
        rect.offset(location[0], location[1])

        return word to rect
    }

    @SuppressLint("InflateParams")
    private fun showWordPopup(textView: TextView, word: String, rect: Rect) {
        val popupView = LayoutInflater.from(textView.context).inflate(R.layout.popup_word, null)
        val binding = PopupWordBinding.bind(popupView)
        binding.tvSearch.text = word.uppercase()

        val popupWindow = PopupWindow(
            popupView,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        )
        popupWindow.animationStyle = android.R.style.Animation_Dialog

        popupWindow.elevation = 10f
        popupWindow.isOutsideTouchable = true
        popupWindow.showAtLocation(
            textView,
            Gravity.NO_GRAVITY,
            rect.left,
            rect.top - 200
        )

        val span = highlightWord(textView, word)
        popupWindow.setOnDismissListener {
            span.clearSpans()
            textView.text = span
        }

        // Bisa kasih aksi tambahan
        binding.tvSearch.setOnClickListener {
            onClickListener(word)
            popupWindow.dismiss()
        }
        binding.tvTranslate.setOnClickListener {
            Toast.makeText(textView.context, "Coming soon :)", Toast.LENGTH_SHORT).show()
            popupWindow.dismiss()
        }

        val iconSearch = ContextCompat.getDrawable(textView.context, R.drawable.baseline_search_24)
        iconSearch?.setBounds(0, 0, 48, 48)
        binding.tvSearch.setCompoundDrawables(iconSearch, null, null, null)
        val iconTranslate = ContextCompat.getDrawable(textView.context, R.drawable.baseline_translate_24)
        iconTranslate?.setBounds(0, 0, 48, 48)
        binding.tvTranslate.setCompoundDrawables(iconTranslate, null, null, null)
    }

    private fun highlightWord(textView: TextView, word: String): SpannableString {
        val fullText = textView.text.toString()
        val spannable = SpannableString(fullText)

        val start = fullText.indexOf(word)
        if (start >= 0) {
            val end = start + word.length
            spannable.setSpan(
                BackgroundColorSpan(Color.YELLOW), // warna highlight custom
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        textView.text = spannable
        return spannable
    }
}