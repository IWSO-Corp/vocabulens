package com.iwsocorp.vobynotes.ui.setting

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.iwsocorp.vobynotes.core.common.BaseFragment
import com.iwsocorp.vobynotes.databinding.FragmentFeedbackBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FeedbackFragment : BaseFragment<FragmentFeedbackBinding>(FragmentFeedbackBinding::inflate) {

    private val viewModel: FeedbackViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbarFeedback.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        binding.btnSend.setOnClickListener {
            val text = binding.etFeedback.text.toString()
            if (text.isEmpty()) return@setOnClickListener

            viewModel.sendFeedback(text) {
                requireActivity().runOnUiThread {
                    if (it) binding.etFeedback.text?.clear()

                    Toast.makeText(
                        requireContext(),
                        if (it) "Thank you for your feedback" else "Failed to send feedback",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

}