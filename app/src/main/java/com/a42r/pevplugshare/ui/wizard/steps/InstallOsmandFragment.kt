package com.a42r.pevplugshare.ui.wizard.steps

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.a42r.pevplugshare.R
import com.a42r.pevplugshare.ui.wizard.SetupWizardViewModel
import com.a42r.pevplugshare.util.OsmandIntentHelper

class InstallOsmandFragment : Fragment() {

    private val viewModel: SetupWizardViewModel by activityViewModels()

    private var detectedContainer: LinearLayout? = null
    private var notFoundContainer: LinearLayout? = null
    private var installedCheckbox: CheckBox? = null
    private var nextButton: Button? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_wizard_install_osmand, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        detectedContainer = view.findViewById(R.id.detectedContainer)
        notFoundContainer = view.findViewById(R.id.notFoundContainer)
        installedCheckbox = view.findViewById(R.id.installedCheckbox)
        nextButton = view.findViewById(R.id.nextButton)

        view.findViewById<Button>(R.id.recheckButton).setOnClickListener {
            checkOsmandInstallation()
        }

        view.findViewById<Button>(R.id.playStorePlusButton).setOnClickListener {
            openPlayStore(OsmandIntentHelper.OSMAND_PACKAGE)
        }

        view.findViewById<Button>(R.id.playStoreFreeButton).setOnClickListener {
            openPlayStore(OsmandIntentHelper.OSMAND_FREE_PACKAGE)
        }

        installedCheckbox?.setOnCheckedChangeListener { _, isChecked ->
            nextButton?.isEnabled = isChecked
            if (isChecked) {
                viewModel.markStepCompleted(1)
            }
        }

        nextButton?.setOnClickListener {
            viewModel.nextStep()
        }

        checkOsmandInstallation()
    }

    override fun onResume() {
        super.onResume()
        checkOsmandInstallation()
    }

    private fun checkOsmandInstallation() {
        val context = requireContext()
        val installed = OsmandIntentHelper.isOsmandInstalled(context)
        viewModel.setOsmandInstalled(installed)

        if (installed) {
            detectedContainer?.visibility = View.VISIBLE
            notFoundContainer?.visibility = View.GONE
            // Auto-tick the checkbox when OsmAnd is detected
            installedCheckbox?.isChecked = true
        } else {
            detectedContainer?.visibility = View.GONE
            notFoundContainer?.visibility = View.VISIBLE
        }
    }

    private fun openPlayStore(packageName: String) {
        val playStoreUri = Uri.parse("market://details?id=$packageName")
        val playStoreIntent = Intent(Intent.ACTION_VIEW, playStoreUri)
        try {
            startActivity(playStoreIntent)
        } catch (e: Exception) {
            val browserUri = Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
            startActivity(Intent(Intent.ACTION_VIEW, browserUri))
        }
    }
}
