package com.iwsocorp.vocabnotes.core.common

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.view.GestureDetector
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.iwsocorp.vocabnotes.R
import com.iwsocorp.vocabnotes.databinding.PopupWordBinding

class TextViewGestureHelper(
    private val context: Context,
    private val currentWord: String,
    private val onClickListener: (String) -> Unit,
) {

    private var currentTextView: TextView? = null
    private val gestureDetector: GestureDetector =
        GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                onClick(e)
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                super.onLongPress(e)
                onClick(e)
            }
        })

    @SuppressLint("ClickableViewAccessibility")
    fun attachTo(textView: TextView) {
        textView.setOnTouchListener { v, event ->
            currentTextView = textView
            if (gestureDetector.onTouchEvent(event)) {
                true
            } else {
                if (event.action == MotionEvent.ACTION_UP) v.performClick()
                false
            }
        }
    }

    private fun onClick(e: MotionEvent) = currentTextView?.let { textView ->
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
    private fun showWordPopup(view: View, word: String, rect: Rect) {
        val popupView = LayoutInflater.from(view.context).inflate(R.layout.popup_word, null)
        val binding = PopupWordBinding.bind(popupView)
        binding.tvPopupWord.text = word

        val popupWindow = PopupWindow(
            popupView,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        )

        popupWindow.elevation = 10f
        popupWindow.isOutsideTouchable = true

        // Tampilkan tepat di atas kata
        popupWindow.showAtLocation(
            view,
            Gravity.NO_GRAVITY,
            rect.left,
            rect.top - popupView.measuredHeight - 100 // sedikit spasi
        )

        // Bisa kasih aksi tambahan
        binding.tvPopupWord.setOnClickListener {
            onClickListener(word)
            popupWindow.dismiss()
        }

        val drawable = ContextCompat.getDrawable(view.context, R.drawable.baseline_search_24)
        drawable?.setBounds(0, 0, 48, 48) // width x height dalam px
        binding.tvPopupWord.setCompoundDrawables(drawable, null, null, null)
    }
}