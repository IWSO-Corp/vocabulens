package com.iwsocorp.vobynotes.ui.slideshow

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.GridLayout
import androidx.fragment.app.viewModels
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.iwsocorp.vobynotes.core.common.BaseFragment
import com.iwsocorp.vobynotes.databinding.FragmentPracticeBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PracticeFragment : BaseFragment<FragmentPracticeBinding>(FragmentPracticeBinding::inflate) {

    private val viewModel: PracticeViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeData()

        viewModel.loadNewQuestion()
    }

    private fun observeData() {
        viewModel.questionSentence.collectLatestLifecycleAware { sentence ->
            binding.tvSentence.text = sentence
        }
        viewModel.options.collectLatestLifecycleAware { options ->
            updateOptions(options)
        }
    }

    private fun updateOptions(options: List<String>) {
        binding.optionsContainer.removeAllViews()

        options.forEach { word ->
            val btn = MaterialButton(requireContext()).apply {
                text = word
                textSize = 16f
                setPadding(12, 12, 12, 12)
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(12, 12, 12, 12)
                }
            }

            btn.setOnClickListener { checkAnswer(word) }
            binding.optionsContainer.addView(btn)
        }
    }

    private fun checkAnswer(selected: String) {
        val correct = viewModel.currentExample.value?.forWord ?: return

        val message = if (selected.equals(correct, ignoreCase = true)) {
            "✅ Benar! Jawabannya: $correct"
        } else {
            "❌ Salah. Jawaban yang benar: $correct"
        }

        Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show()

        // Lanjut ke soal berikutnya setelah 1 detik
        Handler(Looper.getMainLooper()).postDelayed({ viewModel.loadNewQuestion() }, 1000)
    }

}