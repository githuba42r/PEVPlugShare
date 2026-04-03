package com.a42r.pevplugshare.ui.wizard.steps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.a42r.pevplugshare.R
import com.a42r.pevplugshare.ui.wizard.SetupWizardActivity
import com.a42r.pevplugshare.ui.wizard.SetupWizardViewModel

class WelcomeFragment : Fragment() {

    private val viewModel: SetupWizardViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_wizard_welcome, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.getStartedButton).setOnClickListener {
            viewModel.nextStep()
        }

        view.findViewById<Button>(R.id.skipButton).setOnClickListener {
            (activity as? SetupWizardActivity)?.skipWizard()
        }
    }
}
