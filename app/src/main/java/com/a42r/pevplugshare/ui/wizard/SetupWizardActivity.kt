package com.a42r.pevplugshare.ui.wizard

import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.a42r.pevplugshare.R
import com.a42r.pevplugshare.ui.auth.LoginActivity
import com.a42r.pevplugshare.ui.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SetupWizardActivity : AppCompatActivity() {

    companion object {
        private const val PREFS_WIZARD = "wizard_prefs"
        private const val KEY_WIZARD_COMPLETED = "wizard_completed"
        private const val KEY_WIZARD_CURRENT_STEP = "wizard_current_step"
        private const val KEY_WIZARD_SELECTED_CATEGORY = "wizard_selected_category"
        private const val KEY_WIZARD_SKIPPED_AI = "wizard_skipped_ai"
        private const val KEY_AI_WELCOME_SEEN = "has_seen_ai_welcome"
        private const val PREFS_AI_WELCOME = "ai_welcome_prefs"

        fun isFirstRun(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_WIZARD, Context.MODE_PRIVATE)
            return !prefs.getBoolean(KEY_WIZARD_COMPLETED, false)
        }

        fun getSavedStep(context: Context): Int {
            val prefs = context.getSharedPreferences(PREFS_WIZARD, Context.MODE_PRIVATE)
            return prefs.getInt(KEY_WIZARD_CURRENT_STEP, 0)
        }

        fun newIntent(context: Context): Intent {
            return Intent(context, SetupWizardActivity::class.java)
        }
    }

    private val viewModel: SetupWizardViewModel by viewModels()

    private lateinit var viewPager: ViewPager2
    private lateinit var segmentContainer: LinearLayout
    private lateinit var stepLabel: TextView
    private lateinit var stepName: TextView
    private lateinit var skipButton: Button

    private val stepNames = arrayOf(
        "Welcome",
        "Install OsmAnd",
        "Login to OsmAnd",
        "Create Folder",
        "Sync to Cloud",
        "Connect Account",
        "Verify Folder",
        "AI Features",
        "Complete"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup_wizard)

        viewPager = findViewById(R.id.viewPager)
        segmentContainer = findViewById(R.id.segmentContainer)
        stepLabel = findViewById(R.id.stepLabel)
        stepName = findViewById(R.id.stepName)
        skipButton = findViewById(R.id.skipButton)

        setupViewPager()
        setupProgressSegments()
        setupSkipButton()

        // Restore saved step BEFORE observing (to avoid the observer
        // overwriting saved state with the initial value of 0)
        val savedStep = getSavedStep(this)
        if (savedStep > 0) {
            val restoredStep = if (savedStep > 5) 5 else savedStep  // 5 = Connect Account (0-indexed)
            viewModel.goToStep(restoredStep)
        }

        observeViewModel()
    }

    private fun setupViewPager() {
        val adapter = WizardPagerAdapter(this)
        viewPager.adapter = adapter
        viewPager.isUserInputEnabled = false // Navigation controlled programmatically
    }

    private fun setupProgressSegments() {
        segmentContainer.removeAllViews()
        val gap = resources.getDimensionPixelSize(R.dimen.wizard_segment_gap)

        for (i in 0 until SetupWizardViewModel.TOTAL_STEPS) {
            val segment = View(this)
            val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            if (i > 0) params.marginStart = gap
            segment.layoutParams = params
            segment.background = createSegmentDrawable(SegmentState.REMAINING)
            segmentContainer.addView(segment)
        }

        updateProgressSegments(0, BooleanArray(SetupWizardViewModel.TOTAL_STEPS))
    }

    private fun updateProgressSegments(currentStep: Int, completed: BooleanArray) {
        for (i in 0 until segmentContainer.childCount) {
            val segment = segmentContainer.getChildAt(i)
            val state = when {
                i < completed.size && completed[i] -> SegmentState.COMPLETED
                i == currentStep -> SegmentState.CURRENT
                else -> SegmentState.REMAINING
            }
            segment.background = createSegmentDrawable(state)
        }
    }

    private enum class SegmentState { COMPLETED, CURRENT, REMAINING }

    private fun createSegmentDrawable(state: SegmentState): GradientDrawable {
        val drawable = GradientDrawable()
        drawable.shape = GradientDrawable.RECTANGLE
        drawable.cornerRadius = 2f
        val color = when (state) {
            SegmentState.COMPLETED -> ContextCompat.getColor(this, R.color.wizard_segment_completed)
            SegmentState.CURRENT -> ContextCompat.getColor(this, R.color.wizard_segment_current)
            SegmentState.REMAINING -> ContextCompat.getColor(this, R.color.wizard_segment_remaining)
        }
        drawable.setColor(color)
        return drawable
    }

    private fun setupSkipButton() {
        skipButton.setOnClickListener {
            skipWizard()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.currentStep.collect { step ->
                viewPager.currentItem = step
                stepLabel.text = "Step ${step + 1} of ${SetupWizardViewModel.TOTAL_STEPS}"
                stepName.text = stepNames.getOrNull(step) ?: ""
                updateProgressSegments(step, viewModel.stepCompleted.value)
                saveCurrentStep(step)
            }
        }

        lifecycleScope.launch {
            viewModel.stepCompleted.collect { completed ->
                updateProgressSegments(viewModel.currentStep.value, completed)
            }
        }
    }

    fun skipWizard() {
        markWizardCompleted()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    fun completeWizard() {
        markWizardCompleted()
        markAiWelcomeAsSeen()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun saveCurrentStep(step: Int) {
        getSharedPreferences(PREFS_WIZARD, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_WIZARD_CURRENT_STEP, step)
            .apply()
    }

    private fun markWizardCompleted() {
        getSharedPreferences(PREFS_WIZARD, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_WIZARD_COMPLETED, true)
            .remove(KEY_WIZARD_CURRENT_STEP)
            .apply()
    }

    private fun markAiWelcomeAsSeen() {
        getSharedPreferences(PREFS_AI_WELCOME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_AI_WELCOME_SEEN, true)
            .apply()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (viewModel.currentStep.value > 0) {
            viewModel.previousStep()
        } else {
            super.onBackPressed()
        }
    }
}
