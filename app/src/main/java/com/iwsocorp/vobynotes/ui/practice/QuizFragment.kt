package com.iwsocorp.vobynotes.ui.practice

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.GridLayout
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.iwsocorp.vobynotes.R
import com.iwsocorp.vobynotes.core.common.BaseFragment
import com.iwsocorp.vobynotes.core.common.Utils.showAlertDialog
import com.iwsocorp.vobynotes.core.model.Example
import com.iwsocorp.vobynotes.databinding.FragmentQuizBinding
import timber.log.Timber

class QuizFragment : BaseFragment<FragmentQuizBinding>(FragmentQuizBinding::inflate) {

    private val viewModel: QuizViewModel by viewModels()
    private val practiceViewModel: PracticeViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        practiceViewModel.quizList.collectOnStarted {
            Timber.d("Quiz List ${it.size}: ${it.map { example -> example.sentence }}")
            observe(it)
        }
    }

    private fun observe(examples: List<Example>) = viewModel.state.collectOnStarted { state ->
        Timber.d("State: $state")
        binding.toolbarQuiz.isVisible = state != QuizState.Finished
        when (state) {
            is QuizState.Idle -> {
                binding.tvSentence.text = ""
                binding.optionsGrid.removeAllViews()
                viewModel.startQuiz(examples)
            }

            is QuizState.ShowQuestion -> {
                binding.nextButton.visibility = View.INVISIBLE
                setupToolbar(state.position, state.total)
                showQuestionUI(state.example, examples)
            }

            is QuizState.ShowResult -> {
                showResultUI(state, examples)
            }

            is QuizState.Finished -> {
                showFinishedUI()
            }
        }
    }

    private val optionButtons = mutableListOf<MaterialButton>()

    private fun showQuestionUI(example: Example, examples: List<Example>) {
        val blankSentence = example.sentence.replace(
            example.forWord, "____", ignoreCase = true
        )
        binding.tvSentence.text = blankSentence
        binding.optionsGrid.removeAllViews()
        optionButtons.clear()

        val wrongOptions = examples.map { it.forWord }
            .distinct()
            .filterNot { it == example.forWord }
            .shuffled()
            .take(3)
        val options = (wrongOptions + example.forWord).shuffled()

        options.forEach { word ->
            val button = MaterialButton(requireContext()).apply {
                text = word
                textSize = 16f
                setPadding(24, 12, 24, 12)
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(12, 12, 12, 12)
                }
                setOnClickListener { viewModel.answerQuestion(word, examples) }
            }
            optionButtons.add(button)
            binding.optionsGrid.addView(button)
        }
    }

    private fun showResultUI(state: QuizState.ShowResult, examples: List<Example>) {
        optionButtons.forEach { button ->
            when (button.text) {
                state.correctAnswer -> button.backgroundTintList =
                    ColorStateList.valueOf(Color.GREEN)

                state.selectedAnswer -> if (!state.isCorrect) {
                    button.backgroundTintList = ColorStateList.valueOf(Color.RED)
                }

                else -> button.backgroundTintList = ColorStateList.valueOf(Color.LTGRAY)
            }
            button.isClickable = false
        }

        binding.tvSentence.text = state.sentence
        binding.nextButton.apply {
            visibility = View.VISIBLE
            setOnClickListener { viewModel.nextQuestion(examples) }
        }
    }

    private fun showFinishedUI() {
        binding.tvSentence.text = "Quiz Selesai"
        binding.optionsGrid.removeAllViews()
        optionButtons.clear()
        binding.nextButton.apply {
            text = "Selesai"
            setOnClickListener {
                findNavController().navigateUp()
            }
        }

    }

    private fun setupToolbar(position: Int, total: Int) {
        binding.toolbarQuiz.apply {
            title = "$position/$total"
            setNavigationIcon(R.drawable.baseline_arrow_back_24)
            setNavigationOnClickListener {
                onBack()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    onBack()
                }
            }
        )
    }

    private fun onBack() = showAlertDialog(
        requireContext(),
        "End Quiz",
        "Are you sure you want to exit?",
        "Exit",
        "Continue",
        ) {
        findNavController().navigateUp()
    }

}