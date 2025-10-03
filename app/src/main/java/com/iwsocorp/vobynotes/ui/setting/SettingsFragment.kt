package com.iwsocorp.vobynotes.ui.setting

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseUser
import com.iwsocorp.vobynotes.R
import com.iwsocorp.vobynotes.core.common.BaseFragment
import com.iwsocorp.vobynotes.core.common.Utils.showAlertDialog
import com.iwsocorp.vobynotes.databinding.FragmentSettingsBinding
import com.iwsocorp.vobynotes.ui.auth.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class SettingsFragment : BaseFragment<FragmentSettingsBinding>(FragmentSettingsBinding::inflate) {

    private val authViewModel: AuthViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbarSetting.apply {
            title = "Settings"
            setNavigationIcon(R.drawable.baseline_arrow_back_24)
            setNavigationOnClickListener {
                parentFragmentManager.popBackStack()
            }
        }
        binding.btnBackup.setOnClickListener {

        }

        authViewModel.authState.collectLatestLifecycleAware {
            it.onSuccess { user ->
                setupUI(user)
                Timber.d("Firebase user: ${user.toString()}")
            }
        }
    }

    private fun setupUI(user: FirebaseUser?) = with(binding) {
        tvAccount.text = user?.email
        tvAccount.isVisible = user != null
        tvSignin.text = getString(
            if (user == null) R.string.sign_in else R.string.sign_out
        )
        tvSignin.setTextColor(
            resources.getColor(
                if (user == null) R.color.teal_700 else R.color.red,
                null
            )
        )
        tvSignin.setOnClickListener {
            user?.let {
                showAlertDialog(
                    requireContext(),
                    "Sign Out",
                    "Are you sure you want to sign out?",
                    "Sign Out",
                    "Cancel",
                ) {
                    authViewModel.signOut()
                }
            } ?: run {
                findNavController().navigate(R.id.action_nav_settings_to_authFragment)
            }
        }
    }

}