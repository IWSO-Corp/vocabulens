package com.iwsocorp.vobynotes.ui.practice

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.GridLayout
import androidx.activity.OnBackPressedCallback
import androidx.core.text.HtmlCompat
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
import com.iwsocorp.vobynotes.ui.home.HomeViewModel
import timber.log.Timber

class QuizFragment : BaseFragment<FragmentQuizBinding>(FragmentQuizBinding::inflate) {

    private val viewModel: QuizViewModel by viewModels()
    private val practiceViewModel: PracticeViewModel by activityViewModels()
    private val homeViewModel: HomeViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val amount = practiceViewModel.amount.value
        practiceViewModel.quizList.collectOnStarted {
            Timber.d("Quiz List ${it.size}: ${it.map { example -> example.sentence }}")
            observe(it.take(
                if (amount == 1) it.size else amount
            ))
        }
    }

    private fun observe(examples: List<Example>) = viewModel.state.collectOnStarted { state ->
        Timber.d("State: $state")
        binding.toolbarQuiz.isVisible = state !is QuizState.Finished
        binding.tvSentence.isVisible = state !is QuizState.Finished
        binding.optionsGrid.isVisible = state !is QuizState.Finished
        binding.nextButton.isClickable = state !is QuizState.ShowQuestion
        binding.nextButton.backgroundTintList = ColorStateList.valueOf(
            resources.getColor(
                if (state is QuizState.ShowQuestion) R.color.light_grey else R.color.blue, null
            )
        )
        binding.resultContainer.isVisible = state is QuizState.Finished

        when (state) {
            is QuizState.Idle -> {
                binding.optionsGrid.removeAllViews()
                viewModel.startQuiz(examples)
            }

            is QuizState.ShowQuestion -> {
                showQuestionUI(state.example, examples, state.position)
            }

            is QuizState.ShowResult -> {
                showResultUI(state, examples)
            }

            is QuizState.Finished -> {
                showFinishedUI(state.result)
            }
        }
    }

    private val optionButtons = mutableListOf<MaterialButton>()

    private fun showQuestionUI(example: Example, examples: List<Example>, position: Int) {
        setupToolbar(position, examples.size)

        val blankSentence = example.sentence.replace(
            example.forWord,
            "<u>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</u>",
            ignoreCase = true
        )
        binding.tvSentence.text =
            HtmlCompat.fromHtml(blankSentence, HtmlCompat.FROM_HTML_MODE_LEGACY)
        binding.optionsGrid.removeAllViews()
        optionButtons.clear()

        val corpus = homeViewModel.allCorpus.value
        val wrongOptions = corpus.map { it.word }.distinct().filterNot { it == example.forWord }
            .shuffled().take(3)
        val options = (wrongOptions + example.forWord).shuffled()

        options.forEach { word ->
            val button = MaterialButton(requireContext()).apply {
                text = word
                textSize = 16f
                setPadding(24, 12, 24, 12)
                backgroundTintList = ColorStateList.valueOf(
                    resources.getColor(R.color.white, null)
                )
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
            button.backgroundTintList = ColorStateList.valueOf(
                resources.getColor(
                    when (button.text) {
                        state.selectedAnswer -> if (state.isCorrect) R.color.green else R.color.red
                        else -> R.color.white
                    }, null
                )
            )
            button.isClickable = false
        }

        val styledSentence = state.sentence.replace(
            state.correctAnswer,
            "<b><u>${state.correctAnswer}</u></b>",
            ignoreCase = true
        )

        viewModel.saveResult(styledSentence, state.isCorrect, state.selectedAnswer)

        binding.tvSentence.text =
            HtmlCompat.fromHtml(styledSentence, HtmlCompat.FROM_HTML_MODE_LEGACY)
        binding.nextButton.setOnClickListener { viewModel.nextQuestion(examples) }
    }

    private fun showFinishedUI(result: List<Triple<String, Boolean, String>>) {
        binding.optionsGrid.removeAllViews()
        optionButtons.clear()
        binding.nextButton.apply {
            text = "Close"
            setOnClickListener {
                findNavController().navigateUp()
            }
        }

        val correct = result.count { it.second }
        val wrong = result.count { !it.second }
        binding.tvCorrect.text = getString(R.string.correct_d, correct)
        binding.tvWrong.text = getString(R.string.wrong_d, wrong)

        val adapter = ResultAdapter(result)
        binding.rvResult.adapter = adapter
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