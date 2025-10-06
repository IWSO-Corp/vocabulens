package com.iwsocorp.vobynotes.ui.practice

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.iwsocorp.vobynotes.R
import com.iwsocorp.vobynotes.core.common.BaseFragment
import com.iwsocorp.vobynotes.core.model.Mark
import com.iwsocorp.vobynotes.databinding.FragmentPracticeBinding
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class PracticeFragment : BaseFragment<FragmentPracticeBinding>(FragmentPracticeBinding::inflate) {

    private val viewModel: PracticeViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupQuizPreferences()
    }

    private fun setupQuizPreferences() {
        val noteOptions = listOf("All Notes", "Note A", "Note B", "Note C")
        val typeOptions =
            listOf("All Words", Mark.FAMILIAR.name, Mark.UNFAMILIAR.name, Mark.UNMARKED.name)
        val limitOptions = listOf(10, 20, 30, 40, 50)

        binding.noteDropdown.setAdapter(
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                noteOptions
            )
        )
        binding.typeDropdown.setAdapter(
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                typeOptions
            )
        )
        binding.limitDropdown.setAdapter(
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                limitOptions
            )
        )
        binding.noteDropdown.onItemSelectedListener = listener {
            viewModel.setNoteId(
                if (it != 0) noteOptions[it] else null
            )
        }
        binding.typeDropdown.onItemSelectedListener = listener {
            viewModel.setMark(
                if (it != 0) Mark.valueOf(typeOptions[it]) else null
            )
        }
        binding.limitDropdown.onItemSelectedListener = listener {
            viewModel.setLimit(limitOptions[it])
        }

        binding.btnStart.setOnClickListener {
            val note = viewModel.noteId.value
            val type = viewModel.mark.value
            val limit = viewModel.limit.value

            Timber.d("Note: $note, Type: $type, Limit: $limit")

            viewModel.getExamplesForQuiz(
                noteId = note,
                mark = type,
                limit = limit
            )
        }

        viewModel.isExampleEnough.collectOnStarted {
            if (it) {
                findNavController().navigate(R.id.action_nav_practice_to_quizFragment)
            } else {
                Snackbar.make(
                    requireView(),
                    "Not enough examples to start quiz, please add more examples or lower the limit",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun listener(action: (position: Int) -> Unit) =
        object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                p0: AdapterView<*>?,
                p1: View?,
                p2: Int,
                p3: Long,
            ) {
                action(p2)
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {}

        }

}