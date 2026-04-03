package com.a42r.pevplugshare.ui.wizard.steps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.a42r.pevplugshare.R
import com.a42r.pevplugshare.ui.wizard.SetupWizardViewModel

class CreateFolderFragment : Fragment() {

    private val viewModel: SetupWizardViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_wizard_create_folder, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val folderCreatedCheckbox = view.findViewById<CheckBox>(R.id.folderCreatedCheckbox)
        val nextButton = view.findViewById<Button>(R.id.nextButton)

        folderCreatedCheckbox.setOnCheckedChangeListener { _, isChecked ->
            nextButton.isEnabled = isChecked
            if (isChecked) {
                viewModel.markStepCompleted(3)
            }
        }

        nextButton.setOnClickListener {
            viewModel.nextStep()
        }
    }
}
