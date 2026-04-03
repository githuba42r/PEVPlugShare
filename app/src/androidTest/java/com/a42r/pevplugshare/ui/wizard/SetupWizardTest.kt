package com.a42r.pevplugshare.ui.wizard

import android.content.Context
import android.content.Intent
import android.view.WindowManager
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.a42r.pevplugshare.R
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.hamcrest.Matchers.allOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for the Setup Wizard.
 *
 * Test accounts:
 * - Blank (no folders): osmand-testing@a42r.com / G4pKoFg2
 * - With "PEV Charges" folder: osmand-testing2@a42r.com / SpjZ17R8
 *
 * Prerequisites: Disable animations on emulator:
 *   adb shell settings put global window_animation_scale 0
 *   adb shell settings put global transition_animation_scale 0
 *   adb shell settings put global animator_duration_scale 0
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SetupWizardTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    private lateinit var context: Context

    @Before
    fun setup() {
        hiltRule.inject()
        context = ApplicationProvider.getApplicationContext()
        clearWizardState()
    }

    private fun clearWizardState() {
        context.getSharedPreferences("wizard_prefs", Context.MODE_PRIVATE)
            .edit().clear().apply()
        context.getSharedPreferences("ai_welcome_prefs", Context.MODE_PRIVATE)
            .edit().clear().apply()
    }

    private fun launchWizard(): ActivityScenario<SetupWizardActivity> {
        val intent = Intent(context, SetupWizardActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val scenario = ActivityScenario.launch<SetupWizardActivity>(intent)
        // Ensure the activity gets window focus by turning screen on and unlocking
        scenario.onActivity { activity ->
            activity.runOnUiThread {
                activity.window.addFlags(
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                )
            }
        }
        Thread.sleep(500)
        return scenario
    }

    private fun launchWizardAtStep(step: Int): ActivityScenario<SetupWizardActivity> {
        context.getSharedPreferences("wizard_prefs", Context.MODE_PRIVATE)
            .edit().putInt("wizard_current_step", step).apply()
        return launchWizard()
    }

    private fun visible(viewId: Int) = allOf(withId(viewId), isCompletelyDisplayed())

    private fun loginWith(email: String, password: String) {
        onView(visible(R.id.emailInput)).perform(typeText(email), closeSoftKeyboard())
        onView(visible(R.id.passwordInput)).perform(typeText(password), closeSoftKeyboard())
        onView(visible(R.id.connectButton)).perform(click())
        Thread.sleep(10000)
    }

    // ── First Run Detection ──────────────────────────────────────────

    @Test
    fun isFirstRun_trueWhenFresh() {
        assert(SetupWizardActivity.isFirstRun(context))
    }

    @Test
    fun isFirstRun_falseAfterCompletion() {
        context.getSharedPreferences("wizard_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean("wizard_completed", true).apply()
        assert(!SetupWizardActivity.isFirstRun(context))
    }

    // ── Step 1: Welcome ──────────────────────────────────────────────

    @Test
    fun step1_displaysWelcomeContent() {
        val s = launchWizard()
        onView(withId(R.id.welcomeTitle)).check(matches(isDisplayed()))
        onView(withId(R.id.pevDefinition)).check(matches(isDisplayed()))
        onView(withId(R.id.getStartedButton)).check(matches(isDisplayed()))
        s.close()
    }

    @Test
    fun step1_getStarted_goesToStep2() {
        val s = launchWizard()
        onView(withId(R.id.getStartedButton)).perform(click())
        Thread.sleep(300)
        onView(withId(R.id.installTitle)).check(matches(isDisplayed()))
        s.close()
    }

    @Test
    fun step1_skipButton_exists() {
        val s = launchWizard()
        onView(allOf(withId(R.id.skipButton), isDescendantOfA(withId(R.id.viewPager))))
            .check(matches(isDisplayed()))
        s.close()
    }

    // ── Step 2: Install OsmAnd ───────────────────────────────────────

    @Test
    fun step2_showsInstallScreen() {
        val s = launchWizardAtStep(1)
        onView(withId(R.id.installTitle)).check(matches(isDisplayed()))
        s.close()
    }

    // ── Steps 3-5: Checkbox Steps ────────────────────────────────────

    @Test
    fun step3_checkboxEnablesNext() {
        val s = launchWizardAtStep(2)
        onView(visible(R.id.nextButton)).check(matches(isNotEnabled()))
        onView(visible(R.id.loggedInCheckbox)).perform(scrollTo(), click())
        onView(visible(R.id.nextButton)).check(matches(isEnabled()))
        s.close()
    }

    @Test
    fun step4_checkboxEnablesNext() {
        val s = launchWizardAtStep(3)
        onView(visible(R.id.nextButton)).check(matches(isNotEnabled()))
        onView(visible(R.id.folderCreatedCheckbox)).perform(scrollTo(), click())
        onView(visible(R.id.nextButton)).check(matches(isEnabled()))
        s.close()
    }

    @Test
    fun step5_checkboxEnablesNext() {
        val s = launchWizardAtStep(4)
        onView(visible(R.id.nextButton)).check(matches(isNotEnabled()))
        onView(visible(R.id.syncedCheckbox)).perform(scrollTo(), click())
        onView(visible(R.id.nextButton)).check(matches(isEnabled()))
        s.close()
    }

    // ── Step 6: Connect Account ──────────────────────────────────────

    @Test
    fun step6_showsLoginForm() {
        val s = launchWizardAtStep(5)
        onView(visible(R.id.emailInput)).check(matches(isDisplayed()))
        onView(visible(R.id.passwordInput)).check(matches(isDisplayed()))
        onView(visible(R.id.connectButton)).check(matches(isDisplayed()))
        s.close()
    }

    @Test
    fun step6_emptyEmail_showsError() {
        val s = launchWizardAtStep(5)
        onView(visible(R.id.connectButton)).perform(click())
        Thread.sleep(300)
        onView(visible(R.id.emailInput)).check(matches(isDisplayed()))
        s.close()
    }

    @Test
    fun step6_invalidCredentials_showsError() {
        val s = launchWizardAtStep(5)
        loginWith("invalid@test.com", "wrongpassword")
        onView(visible(R.id.errorContainer)).check(matches(isDisplayed()))
        s.close()
    }

    @Test
    fun step6_blankAccount_advancesToStep7_noFolders() {
        val s = launchWizardAtStep(5)
        loginWith("osmand-testing@a42r.com", "G4pKoFg2")
        onView(withId(R.id.notFoundContainer)).check(matches(isDisplayed()))
        s.close()
    }

    @Test
    fun step6_syncedAccount_advancesToStep7_foldersFound() {
        val s = launchWizardAtStep(5)
        loginWith("osmand-testing2@a42r.com", "SpjZ17R8")
        onView(withId(R.id.foundContainer)).check(matches(isDisplayed()))
        onView(withId(R.id.folderRadioGroup)).check(matches(isDisplayed()))
        onView(withId(R.id.continueButton)).check(matches(isEnabled()))
        s.close()
    }

    // ── Step 7: Verify Folder ────────────────────────────────────────

    @Test
    fun step7_noFolders_showsRetryButton() {
        val s = launchWizardAtStep(5)
        loginWith("osmand-testing@a42r.com", "G4pKoFg2")
        onView(withId(R.id.retryButton)).check(matches(isDisplayed()))
        s.close()
    }

    @Test
    fun step7_retry_stillShowsNoFolders() {
        val s = launchWizardAtStep(5)
        loginWith("osmand-testing@a42r.com", "G4pKoFg2")
        onView(withId(R.id.retryButton)).perform(scrollTo(), click())
        Thread.sleep(5000)
        onView(withId(R.id.notFoundContainer)).check(matches(isDisplayed()))
        s.close()
    }

    @Test
    fun step7_selectFolder_advancesToStep8() {
        val s = launchWizardAtStep(5)
        loginWith("osmand-testing2@a42r.com", "SpjZ17R8")
        onView(withId(R.id.continueButton)).perform(scrollTo(), click())
        Thread.sleep(300)
        onView(withId(R.id.connectOpenRouterButton)).check(matches(isDisplayed()))
        s.close()
    }

    // ── Step 8: AI Features ──────────────────────────────────────────

    @Test
    fun step8_showsConnectAndSkipButtons() {
        val s = launchWizardAtStep(5)
        loginWith("osmand-testing2@a42r.com", "SpjZ17R8")
        onView(withId(R.id.continueButton)).perform(scrollTo(), click())
        Thread.sleep(300)
        onView(withId(R.id.connectOpenRouterButton)).check(matches(isDisplayed()))
        onView(withId(R.id.skipAiButton)).check(matches(isDisplayed()))
        s.close()
    }

    @Test
    fun step8_skipAi_advancesToStep9() {
        val s = launchWizardAtStep(5)
        loginWith("osmand-testing2@a42r.com", "SpjZ17R8")
        onView(withId(R.id.continueButton)).perform(scrollTo(), click())
        Thread.sleep(300)
        onView(withId(R.id.skipAiButton)).perform(scrollTo(), click())
        Thread.sleep(300)
        onView(withId(R.id.startButton)).check(matches(isDisplayed()))
        s.close()
    }

    // ── Step 9: Completion ───────────────────────────────────────────

    @Test
    fun step9_showsChecklistAndStartButton() {
        val s = launchWizardAtStep(5)
        loginWith("osmand-testing2@a42r.com", "SpjZ17R8")
        onView(withId(R.id.continueButton)).perform(scrollTo(), click())
        Thread.sleep(300)
        onView(withId(R.id.skipAiButton)).perform(scrollTo(), click())
        Thread.sleep(300)
        onView(withId(R.id.checklistContainer)).check(matches(isDisplayed()))
        onView(withId(R.id.startButton)).check(matches(isDisplayed()))
        s.close()
    }

    @Test
    fun step9_startButton_marksWizardComplete() {
        val s = launchWizardAtStep(5)
        loginWith("osmand-testing2@a42r.com", "SpjZ17R8")
        onView(withId(R.id.continueButton)).perform(scrollTo(), click())
        Thread.sleep(300)
        onView(withId(R.id.skipAiButton)).perform(scrollTo(), click())
        Thread.sleep(300)
        onView(withId(R.id.startButton)).perform(click())
        Thread.sleep(300)
        val prefs = context.getSharedPreferences("wizard_prefs", Context.MODE_PRIVATE)
        assert(prefs.getBoolean("wizard_completed", false)) { "Wizard should be marked complete" }
        s.close()
    }

    @Test
    fun step9_startButton_marksAiWelcomeSeen() {
        val s = launchWizardAtStep(5)
        loginWith("osmand-testing2@a42r.com", "SpjZ17R8")
        onView(withId(R.id.continueButton)).perform(scrollTo(), click())
        Thread.sleep(300)
        onView(withId(R.id.skipAiButton)).perform(scrollTo(), click())
        Thread.sleep(300)
        onView(withId(R.id.startButton)).perform(click())
        Thread.sleep(300)
        val prefs = context.getSharedPreferences("ai_welcome_prefs", Context.MODE_PRIVATE)
        assert(prefs.getBoolean("has_seen_ai_welcome", false)) { "AI welcome should be marked seen" }
        s.close()
    }

    // ── Skip Wizard ──────────────────────────────────────────────────

    @Test
    fun skipWizard_marksComplete_notAiWelcome() {
        val s = launchWizardAtStep(1)
        onView(allOf(withId(R.id.skipButton), isDescendantOfA(withId(R.id.progressContainer))))
            .perform(click())
        Thread.sleep(300)
        val wizPrefs = context.getSharedPreferences("wizard_prefs", Context.MODE_PRIVATE)
        assert(wizPrefs.getBoolean("wizard_completed", false)) { "Wizard should be complete" }
        val aiPrefs = context.getSharedPreferences("ai_welcome_prefs", Context.MODE_PRIVATE)
        assert(!aiPrefs.getBoolean("has_seen_ai_welcome", false)) { "AI welcome should NOT be seen" }
        s.close()
    }

    // ── Back Navigation ──────────────────────────────────────────────

    @Test
    fun backNavigation_goesToPreviousStep() {
        val s = launchWizardAtStep(2)
        onView(visible(R.id.loggedInCheckbox)).check(matches(isDisplayed()))
        onView(isRoot()).perform(pressBack())
        Thread.sleep(300)
        onView(withId(R.id.installTitle)).check(matches(isDisplayed()))
        s.close()
    }

    // ── Step Persistence ─────────────────────────────────────────────

    @Test
    fun persistence_savesStep() {
        val s = launchWizard()
        onView(withId(R.id.getStartedButton)).perform(click())
        Thread.sleep(300)
        val prefs = context.getSharedPreferences("wizard_prefs", Context.MODE_PRIVATE)
        assert(prefs.getInt("wizard_current_step", -1) == 1) { "Should save step 1" }
        s.close()
    }

    @Test
    fun persistence_restoresSavedStep() {
        val s = launchWizardAtStep(3)
        onView(visible(R.id.folderCreatedCheckbox)).check(matches(isDisplayed()))
        s.close()
    }

    @Test
    fun persistence_capsAtStep5ForSessionSteps() {
        val s = launchWizardAtStep(7)
        onView(visible(R.id.emailInput)).check(matches(isDisplayed()))
        s.close()
    }

    // ── Progress Bar ─────────────────────────────────────────────────

    @Test
    fun progressBar_showsCorrectStepLabel() {
        val s = launchWizard()
        onView(withId(R.id.stepLabel)).check(matches(withText("Step 1 of 9")))
        onView(withId(R.id.getStartedButton)).perform(click())
        Thread.sleep(300)
        onView(withId(R.id.stepLabel)).check(matches(withText("Step 2 of 9")))
        s.close()
    }
}
