package com.a42r.pevplugshare.ui.wizard.steps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.a42r.pevplugshare.R
import com.a42r.pevplugshare.ui.wizard.FolderVerificationState
import com.a42r.pevplugshare.ui.wizard.SetupWizardViewModel
import kotlinx.coroutines.launch

class VerifyFolderFragment : Fragment() {

    private val viewModel: SetupWizardViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_wizard_verify_folder, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val loadingContainer = view.findViewById<LinearLayout>(R.id.loadingContainer)
        val foundContainer = view.findViewById<LinearLayout>(R.id.foundContainer)
        val notFoundContainer = view.findViewById<LinearLayout>(R.id.notFoundContainer)
        val errorContainer = view.findViewById<LinearLayout>(R.id.errorContainer)
        val folderRadioGroup = view.findViewById<RadioGroup>(R.id.folderRadioGroup)
        val continueButton = view.findViewById<Button>(R.id.continueButton)
        val retryButton = view.findViewById<Button>(R.id.retryButton)
        val errorRetryButton = view.findViewById<Button>(R.id.errorRetryButton)
        val errorDetail = view.findViewById<TextView>(R.id.errorDetail)

        retryButton.setOnClickListener { viewModel.verifyFolders() }
        errorRetryButton.setOnClickListener { viewModel.verifyFolders() }

        continueButton.setOnClickListener {
            val selectedId = folderRadioGroup.checkedRadioButtonId
            if (selectedId != -1) {
                val radioButton = view.findViewById<RadioButton>(selectedId)
                viewModel.selectCategory(radioButton.text.toString())
                viewModel.markStepCompleted(6)
                viewModel.nextStep()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.folderState.collect { state ->
                // Hide all containers first
                loadingContainer.visibility = View.GONE
                foundContainer.visibility = View.GONE
                notFoundContainer.visibility = View.GONE
                errorContainer.visibility = View.GONE

                when (state) {
                    is FolderVerificationState.Idle -> {
                        viewModel.verifyFolders()
                    }
                    is FolderVerificationState.Loading -> {
                        loadingContainer.visibility = View.VISIBLE
                    }
                    is FolderVerificationState.Found -> {
                        foundContainer.visibility = View.VISIBLE
                        populateFolderRadioGroup(folderRadioGroup, state.categories)
                        continueButton.isEnabled = folderRadioGroup.checkedRadioButtonId != -1
                    }
                    is FolderVerificationState.NotFound -> {
                        notFoundContainer.visibility = View.VISIBLE
                    }
                    is FolderVerificationState.Success -> {
                        // Legacy success state — treat same as not found since we have no categories
                        notFoundContainer.visibility = View.VISIBLE
                    }
                    is FolderVerificationState.Error -> {
                        errorContainer.visibility = View.VISIBLE
                        errorDetail.text = state.message
                    }
                }
            }
        }
    }

    private fun populateFolderRadioGroup(radioGroup: RadioGroup, categories: List<String>) {
        radioGroup.removeAllViews()
        categories.forEachIndexed { index, category ->
            val radioButton = RadioButton(requireContext()).apply {
                id = View.generateViewId()
                text = category
                isChecked = index == 0
            }
            radioGroup.addView(radioButton)
        }
    }
}
