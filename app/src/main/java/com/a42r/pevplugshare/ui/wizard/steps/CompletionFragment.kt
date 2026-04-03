package com.a42r.pevplugshare.ui.wizard.steps

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.a42r.pevplugshare.R
import com.a42r.pevplugshare.ui.wizard.SetupWizardActivity
import com.a42r.pevplugshare.ui.wizard.SetupWizardViewModel

class CompletionFragment : Fragment() {

    private val viewModel: SetupWizardViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_wizard_completion, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val checklistContainer = view.findViewById<LinearLayout>(R.id.checklistContainer)
        val startButton = view.findViewById<Button>(R.id.startButton)

        buildChecklist(checklistContainer)

        startButton.setOnClickListener {
            (activity as? SetupWizardActivity)?.completeWizard()
        }
    }

    private fun buildChecklist(container: LinearLayout) {
        container.removeAllViews()

        val selectedCategory = viewModel.selectedCategory.value ?: "(none selected)"
        val alwaysItems = listOf(
            "OsmAnd installed",
            "Logged in to OsmAnd",
            "Folder created in OsmAnd",
            "Synced to OsmAnd Cloud",
            "Account connected",
            "Folder verified: $selectedCategory"
        )

        alwaysItems.forEach { label ->
            container.addView(createChecklistRow(label, checkmark = true))
        }

        val skippedAi = viewModel.skippedAi.value
        if (skippedAi) {
            container.addView(createChecklistRow("OpenRouter AI — skipped", checkmark = false))
        } else {
            container.addView(createChecklistRow("OpenRouter AI connected", checkmark = true))
            val model = viewModel.selectedModel.value
            if (model != null) {
                container.addView(createChecklistRow("Model: ${model.name}", checkmark = true))
            }
        }
    }

    private fun createChecklistRow(label: String, checkmark: Boolean): LinearLayout {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.bottomMargin = dpToPx(8)
            layoutParams = params
        }

        val icon = TextView(requireContext()).apply {
            val params = LinearLayout.LayoutParams(dpToPx(28), LinearLayout.LayoutParams.WRAP_CONTENT)
            layoutParams = params
            text = if (checkmark) "✓" else "○"
            textSize = 16f
            setTextColor(
                if (checkmark) Color.parseColor("#4ade80")
                else Color.parseColor("#fbbf24")
            )
        }

        val text = TextView(requireContext()).apply {
            val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            layoutParams = params
            this.text = label
            textSize = 14f
            setLineSpacing(0f, 1.3f)
        }

        row.addView(icon)
        row.addView(text)
        return row
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
