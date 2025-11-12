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
import androidx.core.content.ContextCompat
import androidx.core.text.clearSpans
import androidx.core.view.isVisible
import com.iwsocorp.vobynotes.R
import com.iwsocorp.vobynotes.databinding.PopupWordBinding

class TextViewGestureHelper(
    context: Context,
    private val currentWord: String,
    private val onSearch: (String) -> Unit,
    private val onTranslate: (String, (String) -> Unit) -> Unit,
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

        // Ikon Search
        val iconSearch = ContextCompat.getDrawable(textView.context, R.drawable.baseline_search_24)
        iconSearch?.setBounds(0, 0, 48, 48)
        binding.tvSearch.setCompoundDrawables(iconSearch, null, null, null)
        binding.tvSearch.setOnClickListener {
            onSearch(word)
            popupWindow.dismiss()
        }

        // Ikon Translate
        val iconTranslate =
            ContextCompat.getDrawable(textView.context, R.drawable.baseline_translate_24)
        iconTranslate?.setBounds(0, 0, 48, 48)
        binding.tvTranslate.setCompoundDrawables(iconTranslate, null, null, null)
        binding.tvTranslate.setOnClickListener {
            popupWindow.dismiss()

            // Panggil ViewModel untuk translate (async)
            onTranslate(word) { translated ->
                // tampilkan popup hasil translate setelah hasil diterima
                showTranslatePopup(textView, word, translated, rect)
            }
        }
    }

    @SuppressLint("InflateParams")
    private fun showTranslatePopup(
        textView: TextView,
        word: String,
        translated: String,
        rect: Rect
    ) {
        val popupView = LayoutInflater.from(textView.context).inflate(R.layout.popup_word, null)
        val binding = PopupWordBinding.bind(popupView)

        // Gunakan layout yang sama tapi ubah isi untuk mode hasil terjemahan
        binding.tvSearch.isVisible = false
        binding.tvTranslate.text = translated

        // Nonaktifkan klik agar tampil pasif
        binding.tvTranslate.isClickable = false

        // Tambahkan animasi fade-in
        popupView.alpha = 0f
        popupView.animate().alpha(1f).setDuration(250).start()

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
            rect.top - 150
        )

        // Pertahankan highlight selama popup tampil
        val span = highlightWord(textView, word)

        popupWindow.setOnDismissListener {
            // Hapus highlight sepenuhnya saat popup ditutup
            span.clearSpans()
            textView.text = textView.text.toString() // reset teks tanpa span
        }
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