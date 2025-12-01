package com.iwsocorp.vobynotes.ui.setting

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.paging.PagingData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.iwsocorp.vobynotes.R
import com.iwsocorp.vobynotes.core.common.BaseFragment
import com.iwsocorp.vobynotes.databinding.FragmentAccountBinding
import com.iwsocorp.vobynotes.ui.share.ShareDetailFragment
import com.iwsocorp.vobynotes.ui.share.ShareNoteAdapter
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class AccountFragment : BaseFragment<FragmentAccountBinding>(FragmentAccountBinding::inflate) {

    private val viewModel: AccountViewModel by viewModels()
    private val user: FirebaseUser by lazy {
        FirebaseAuth.getInstance().currentUser!!
    }
    private val adapter: ShareNoteAdapter by lazy {
        ShareNoteAdapter { sharedNote, _ ->
            findNavController().navigate(
                R.id.action_accountFragment_to_shareDetailFragment,
                bundleOf(ShareDetailFragment.NOTE_ID to sharedNote.id)
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()

        viewModel.getSharedNotes {
            adapter.submitData(viewLifecycleOwner.lifecycle, PagingData.from(it))
            Timber.d("Shared notes: ${it.size}")
        }
        adapter.addOnPagesUpdatedListener {
            binding.tvEmpty.isVisible = adapter.itemCount == 0
        }
    }

    private fun setupUI() = with(binding) {
        tvName.text = user.displayName
        tvName.setOnClickListener {
            tvName.visibility = View.INVISIBLE
            ilName.visibility = View.VISIBLE
            etName.visibility = View.VISIBLE
            etName.setSelection(etName.text?.length ?: 0)
            etName.requestFocus()
            btnSaveName.visibility = View.VISIBLE
        }
        etName.addTextChangedListener {
            btnSaveName.text = if (etName.text?.toString() != user.displayName) "Update" else "Cancel"
        }
        etName.setText(user.displayName)
        tvEmail.text = user.email
        btnSaveName.setOnClickListener {
            if (btnSaveName.text == "Update") changeName(etName.text.toString().trim()) else {
                tvName.visibility = View.VISIBLE
                ilName.visibility = View.INVISIBLE
                etName.visibility = View.INVISIBLE
                btnSaveName.visibility = View.INVISIBLE
            }
        }
        tvDeleteAccount.setOnClickListener {
            deleteAccount()
        }
        toolbarAccount.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        rvSharedNotes.adapter = adapter
    }

    private fun changeName(name: String) = viewModel.changeName(name) {
        if (it) binding.btnSaveName.visibility = View.INVISIBLE

        Toast.makeText(requireContext(), if (it) "Success" else "Failed", Toast.LENGTH_SHORT)
            .show()
    }

    private fun deleteAccount() {
        val editText = EditText(requireContext()).apply {
            hint = "Type email to confirm"
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setPadding(32, 24, 32, 24)
        }

        AlertDialog.Builder(requireContext())
            .setView(editText)
            .setTitle("Delete Account")
            .setMessage(getString(R.string.delete_account))
            .setPositiveButton("Confirm") { dialog, _ ->
                val input = editText.text.toString().trim()
                if (input == user.email) {
                    viewModel.deleteAccountData {
                        Toast.makeText(
                            requireContext(),
                            if (it) "Account deleted" else "Failed to delete account",
                            Toast.LENGTH_SHORT
                        ).show()
                        if (it) findNavController().navigateUp()
                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Email does not match",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

}