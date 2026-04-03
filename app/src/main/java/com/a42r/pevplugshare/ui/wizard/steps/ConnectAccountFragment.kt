package com.a42r.pevplugshare.ui.wizard.steps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.a42r.pevplugshare.R
import com.a42r.pevplugshare.ui.wizard.SetupWizardViewModel
import com.a42r.pevplugshare.ui.wizard.WizardLoginState
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class ConnectAccountFragment : Fragment() {

    private val viewModel: SetupWizardViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_wizard_connect_account, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val emailLayout = view.findViewById<TextInputLayout>(R.id.emailLayout)
        val emailInput = view.findViewById<TextInputEditText>(R.id.emailInput)
        val passwordLayout = view.findViewById<TextInputLayout>(R.id.passwordLayout)
        val passwordInput = view.findViewById<TextInputEditText>(R.id.passwordInput)
        val errorContainer = view.findViewById<LinearLayout>(R.id.errorContainer)
        val errorDetail = view.findViewById<TextView>(R.id.errorDetail)
        val loadingContainer = view.findViewById<LinearLayout>(R.id.loadingContainer)
        val connectButton = view.findViewById<Button>(R.id.connectButton)
        val rememberMe = view.findViewById<CheckBox>(R.id.rememberMeCheckbox)

        connectButton.setOnClickListener {
            val email = emailInput.text?.toString()?.trim() ?: ""
            val password = passwordInput.text?.toString() ?: ""

            var valid = true
            if (email.isEmpty()) {
                emailLayout.error = "Email is required"
                valid = false
            } else {
                emailLayout.error = null
            }
            if (password.isEmpty()) {
                passwordLayout.error = "Password is required"
                valid = false
            } else {
                passwordLayout.error = null
            }

            if (valid) {
                viewModel.loginToOsmand(email, password, rememberMe.isChecked)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.loginState.collect { state ->
                when (state) {
                    is WizardLoginState.Idle -> {
                        loadingContainer.visibility = View.GONE
                        errorContainer.visibility = View.GONE
                        connectButton.isEnabled = true
                        connectButton.text = getString(R.string.wizard_connect_button)
                    }
                    is WizardLoginState.Loading -> {
                        loadingContainer.visibility = View.VISIBLE
                        errorContainer.visibility = View.GONE
                        connectButton.isEnabled = false
                    }
                    is WizardLoginState.Success -> {
                        viewModel.markStepCompleted(5)
                        viewModel.nextStep()
                        viewModel.verifyFolders()
                    }
                    is WizardLoginState.Error -> {
                        loadingContainer.visibility = View.GONE
                        errorContainer.visibility = View.VISIBLE
                        errorDetail.text = state.message
                        connectButton.isEnabled = true
                        connectButton.text = "Try Again"
                    }
                }
            }
        }
    }
}
