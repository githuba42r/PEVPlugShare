# Setup Wizard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a 9-step first-run setup wizard that guides new users through OsmAnd installation, account setup, folder creation, cloud sync, login, folder verification, and optional OpenRouter AI configuration with model selection.

**Architecture:** Single Activity (`SetupWizardActivity`) hosting a `ViewPager2` with 9 Fragment steps. A shared `SetupWizardViewModel` (Hilt-injected, activity-scoped) manages wizard state across all fragments. Steps 6-7 reuse the existing `AuthRepository` for OsmAnd login and category fetching. Step 8 reuses the existing OAuth flow for OpenRouter. The wizard is shown only on first run (SharedPreferences flag) and can be skipped at any point.

**Tech Stack:** Kotlin, ViewPager2, Fragments, Hilt DI, Material Design 3, StateFlow, existing AuthRepository/TokenManager/OsmandIntentHelper

**Spec:** `docs/superpowers/specs/2026-04-03-setup-wizard-design.md`

**Test accounts:**
- Blank (no folders): `osmand-testing@a42r.com` / `G4pKoFg2`
- With "PEV Charges" folder: `osmand-testing2@a42r.com` / `SpjZ17R8`

---

## File Structure

### New Files

| File | Responsibility |
|------|----------------|
| `ui/wizard/SetupWizardActivity.kt` | Host activity: ViewPager2, progress bar, skip button, back navigation |
| `ui/wizard/SetupWizardViewModel.kt` | Shared state: current step, completion flags, login state, categories, selected folder, AI state |
| `ui/wizard/WizardPagerAdapter.kt` | FragmentStateAdapter mapping position → fragment |
| `ui/wizard/steps/WelcomeFragment.kt` | Step 1: intro and Get Started / Skip |
| `ui/wizard/steps/InstallOsmandFragment.kt` | Step 2: OsmAnd detection, Play Store links |
| `ui/wizard/steps/LoginOsmandFragment.kt` | Step 3: instructions for logging into OsmAnd app |
| `ui/wizard/steps/CreateFolderFragment.kt` | Step 4: instructions for creating favourites folder |
| `ui/wizard/steps/SyncCloudFragment.kt` | Step 5: instructions for cloud sync |
| `ui/wizard/steps/ConnectAccountFragment.kt` | Step 6: credential form, login via AuthRepository |
| `ui/wizard/steps/VerifyFolderFragment.kt` | Step 7: fetch categories, folder selection or remediation |
| `ui/wizard/steps/AiFeaturesFragment.kt` | Step 8: AI overview, OAuth, model selection (sub-screens via ViewSwitcher) |
| `ui/wizard/steps/CompletionFragment.kt` | Step 9: summary checklist, launch app |
| `res/layout/activity_setup_wizard.xml` | ViewPager2 + progress bar + skip link |
| `res/layout/fragment_wizard_welcome.xml` | Step 1 layout |
| `res/layout/fragment_wizard_install_osmand.xml` | Step 2 layout |
| `res/layout/fragment_wizard_login_osmand.xml` | Step 3 layout |
| `res/layout/fragment_wizard_create_folder.xml` | Step 4 layout |
| `res/layout/fragment_wizard_sync_cloud.xml` | Step 5 layout |
| `res/layout/fragment_wizard_connect_account.xml` | Step 6 layout |
| `res/layout/fragment_wizard_verify_folder.xml` | Step 7 layout |
| `res/layout/fragment_wizard_ai_features.xml` | Step 8 layout (includes sub-screens) |
| `res/layout/fragment_wizard_completion.xml` | Step 9 layout |

### Modified Files

| File | Change |
|------|--------|
| `app/build.gradle.kts` | Add ViewPager2 + fragment-ktx dependencies |
| `ui/auth/SplashActivity.kt` | Add first-run check, route to SetupWizardActivity |
| `AndroidManifest.xml` | Register SetupWizardActivity |
| `res/values/strings.xml` | Add wizard string resources |

---

### Task 1: Add Dependencies

**Files:**
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Add ViewPager2 and fragment-ktx dependencies**

In `app/build.gradle.kts`, add after the `androidx.activity.ktx` line (around line 179):

```kotlin
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation("androidx.fragment:fragment-ktx:1.8.5")
```

- [ ] **Step 2: Sync gradle**

Run: `cd /home/philg/src/AndroidStudioProjects/PEVPlugShare && ./gradlew dependencies --configuration debugRuntimeClasspath | head -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/build.gradle.kts
git commit -m "chore: add ViewPager2 and fragment-ktx dependencies for setup wizard"
```

---

### Task 2: SetupWizardViewModel

**Files:**
- Create: `app/src/main/java/com/a42r/pevplugshare/ui/wizard/SetupWizardViewModel.kt`

- [ ] **Step 1: Create the shared ViewModel**

```kotlin
package com.a42r.pevplugshare.ui.wizard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a42r.pevplugshare.data.remote.TokenManager
import com.a42r.pevplugshare.domain.repository.AuthRepository
import com.a42r.pevplugshare.ui.settings.model.AiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class WizardLoginState {
    object Idle : WizardLoginState()
    object Loading : WizardLoginState()
    object Success : WizardLoginState()
    data class Error(val message: String) : WizardLoginState()
}

sealed class FolderVerificationState {
    object Idle : FolderVerificationState()
    object Loading : FolderVerificationState()
    data class Found(val categories: List<String>) : FolderVerificationState()
    object NotFound : FolderVerificationState()
    data class Error(val message: String) : FolderVerificationState()
}

@HiltViewModel
class SetupWizardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    companion object {
        const val TOTAL_STEPS = 9
    }

    private val _currentStep = MutableStateFlow(1)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    private val _stepCompleted = MutableStateFlow(mapOf<Int, Boolean>())
    val stepCompleted: StateFlow<Map<Int, Boolean>> = _stepCompleted.asStateFlow()

    private val _osmandInstalled = MutableStateFlow(false)
    val osmandInstalled: StateFlow<Boolean> = _osmandInstalled.asStateFlow()

    private val _loginState = MutableStateFlow<WizardLoginState>(WizardLoginState.Idle)
    val loginState: StateFlow<WizardLoginState> = _loginState.asStateFlow()

    private val _folderState = MutableStateFlow<FolderVerificationState>(FolderVerificationState.Idle)
    val folderState: StateFlow<FolderVerificationState> = _folderState.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _openRouterConnected = MutableStateFlow(false)
    val openRouterConnected: StateFlow<Boolean> = _openRouterConnected.asStateFlow()

    private val _selectedModel = MutableStateFlow<AiModel?>(null)
    val selectedModel: StateFlow<AiModel?> = _selectedModel.asStateFlow()

    var skippedAi: Boolean = false
        private set

    fun goToStep(step: Int) {
        if (step in 1..TOTAL_STEPS) {
            _currentStep.value = step
        }
    }

    fun nextStep() {
        goToStep(_currentStep.value + 1)
    }

    fun previousStep() {
        goToStep(_currentStep.value - 1)
    }

    fun markStepCompleted(step: Int, completed: Boolean = true) {
        _stepCompleted.value = _stepCompleted.value.toMutableMap().apply {
            put(step, completed)
        }
    }

    fun setOsmandInstalled(installed: Boolean) {
        _osmandInstalled.value = installed
        if (installed) markStepCompleted(2)
    }

    fun loginToOsmand(email: String, password: String, rememberMe: Boolean) {
        _loginState.value = WizardLoginState.Loading
        viewModelScope.launch {
            try {
                val result = authRepository.loginWithWebApi(email, password, rememberMe)
                result.onSuccess {
                    tokenManager.setOsmandConnected(true, email)
                    _loginState.value = WizardLoginState.Success
                    markStepCompleted(6)
                }.onFailure { e ->
                    _loginState.value = WizardLoginState.Error(
                        e.message ?: "Login failed"
                    )
                }
            } catch (e: Exception) {
                _loginState.value = WizardLoginState.Error(
                    if (e.message?.contains("Unable to resolve host") == true ||
                        e.message?.contains("timeout") == true)
                        "Connection failed — could not reach the OsmAnd Cloud server. Please check your internet connection."
                    else
                        "Login failed — the email or password is incorrect. Make sure you're using the same credentials from Step 3."
                )
            }
        }
    }

    fun resetLoginState() {
        _loginState.value = WizardLoginState.Idle
    }

    fun verifyFolders() {
        _folderState.value = FolderVerificationState.Loading
        viewModelScope.launch {
            try {
                val result = authRepository.getFavoritesCategories()
                result.onSuccess { categories ->
                    val filtered = categories.filter {
                        !it.equals("favorites", ignoreCase = true) &&
                        !it.equals("favourites", ignoreCase = true)
                    }
                    if (filtered.isNotEmpty()) {
                        _folderState.value = FolderVerificationState.Found(filtered)
                    } else {
                        _folderState.value = FolderVerificationState.NotFound
                    }
                }.onFailure { e ->
                    _folderState.value = FolderVerificationState.Error(
                        e.message ?: "Failed to fetch favourites"
                    )
                }
            } catch (e: Exception) {
                _folderState.value = FolderVerificationState.Error(
                    e.message ?: "Failed to fetch favourites"
                )
            }
        }
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
        markStepCompleted(7)
    }

    fun setOpenRouterConnected(connected: Boolean) {
        _openRouterConnected.value = connected
        if (connected) markStepCompleted(8)
    }

    fun setSelectedModel(model: AiModel) {
        _selectedModel.value = model
        tokenManager.saveSelectedAiModel(model.id)
    }

    fun skipAi() {
        skippedAi = true
        markStepCompleted(8)
    }
}
```

- [ ] **Step 2: Verify it compiles**

Run: `cd /home/philg/src/AndroidStudioProjects/PEVPlugShare && ./gradlew compileDebugKotlin 2>&1 | tail -3`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/a42r/pevplugshare/ui/wizard/SetupWizardViewModel.kt
git commit -m "feat: add SetupWizardViewModel with login, folder verification, and AI state"
```

---

### Task 3: Activity Layout and Host Activity

**Files:**
- Create: `app/src/main/res/layout/activity_setup_wizard.xml`
- Create: `app/src/main/java/com/a42r/pevplugshare/ui/wizard/WizardPagerAdapter.kt`
- Create: `app/src/main/java/com/a42r/pevplugshare/ui/wizard/SetupWizardActivity.kt`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Create activity layout**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:fitsSystemWindows="true">

    <!-- Progress bar area -->
    <LinearLayout
        android:id="@+id/progressContainer"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:paddingHorizontal="24dp"
        android:paddingTop="16dp"
        android:paddingBottom="8dp">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical">

            <TextView
                android:id="@+id/stepLabel"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:textSize="13sp"
                android:textStyle="bold" />

            <TextView
                android:id="@+id/skipButton"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Skip Wizard"
                android:textSize="12sp"
                android:padding="8dp"
                android:textColor="?android:attr/textColorSecondary"
                android:background="?attr/selectableItemBackgroundBorderless" />
        </LinearLayout>

        <LinearLayout
            android:id="@+id/segmentContainer"
            android:layout_width="match_parent"
            android:layout_height="4dp"
            android:layout_marginTop="8dp"
            android:orientation="horizontal" />

        <TextView
            android:id="@+id/stepName"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="4dp"
            android:textSize="11sp"
            android:textColor="?android:attr/textColorSecondary" />
    </LinearLayout>

    <androidx.viewpager2.widget.ViewPager2
        android:id="@+id/viewPager"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1" />

</LinearLayout>
```

- [ ] **Step 2: Create WizardPagerAdapter**

```kotlin
package com.a42r.pevplugshare.ui.wizard

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.a42r.pevplugshare.ui.wizard.steps.*

class WizardPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = SetupWizardViewModel.TOTAL_STEPS

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> WelcomeFragment()
            1 -> InstallOsmandFragment()
            2 -> LoginOsmandFragment()
            3 -> CreateFolderFragment()
            4 -> SyncCloudFragment()
            5 -> ConnectAccountFragment()
            6 -> VerifyFolderFragment()
            7 -> AiFeaturesFragment()
            8 -> CompletionFragment()
            else -> throw IllegalArgumentException("Invalid wizard step: $position")
        }
    }
}
```

- [ ] **Step 3: Create SetupWizardActivity**

```kotlin
package com.a42r.pevplugshare.ui.wizard

import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import com.a42r.pevplugshare.R
import com.a42r.pevplugshare.ui.ai.AiWelcomeActivity
import com.a42r.pevplugshare.ui.auth.LoginActivity
import com.a42r.pevplugshare.ui.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SetupWizardActivity : AppCompatActivity() {

    private val viewModel: SetupWizardViewModel by viewModels()

    private lateinit var viewPager: ViewPager2
    private lateinit var stepLabel: TextView
    private lateinit var stepName: TextView
    private lateinit var segmentContainer: LinearLayout
    private lateinit var skipButton: TextView

    private val stepNames = listOf(
        "Welcome",
        "Install OsmAnd",
        "Log into OsmAnd",
        "Create Favourites Folder",
        "Sync to Cloud",
        "Connect Account",
        "Verify Folder",
        "AI Features (Optional)",
        "Complete"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup_wizard)

        viewPager = findViewById(R.id.viewPager)
        stepLabel = findViewById(R.id.stepLabel)
        stepName = findViewById(R.id.stepName)
        segmentContainer = findViewById(R.id.segmentContainer)
        skipButton = findViewById(R.id.skipButton)

        viewPager.adapter = WizardPagerAdapter(this)
        viewPager.isUserInputEnabled = false

        skipButton.setOnClickListener { skipWizard() }

        setupProgressSegments()
        observeStep()
        setupBackNavigation()
    }

    private fun setupProgressSegments() {
        segmentContainer.removeAllViews()
        val gap = resources.getDimensionPixelSize(R.dimen.wizard_segment_gap)
        for (i in 0 until SetupWizardViewModel.TOTAL_STEPS) {
            val segment = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
                    if (i < SetupWizardViewModel.TOTAL_STEPS - 1) marginEnd = gap
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 4f
                    setColor(getColor(R.color.wizard_segment_remaining))
                }
            }
            segmentContainer.addView(segment)
        }
    }

    private fun observeStep() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.currentStep.collect { step ->
                    viewPager.setCurrentItem(step - 1, true)
                    updateProgress(step)
                }
            }
        }
    }

    fun updateProgress(step: Int) {
        stepLabel.text = "Step $step of ${SetupWizardViewModel.TOTAL_STEPS}"
        stepName.text = stepNames.getOrElse(step - 1) { "" }

        // Hide progress on welcome, show on all other steps
        val progressContainer = findViewById<View>(R.id.progressContainer)
        progressContainer.visibility = if (step == 1) View.GONE else View.VISIBLE

        for (i in 0 until segmentContainer.childCount) {
            val segment = segmentContainer.getChildAt(i)
            val bg = segment.background as GradientDrawable
            when {
                i < step - 1 -> bg.setColor(getColor(R.color.wizard_segment_completed))
                i == step - 1 -> bg.setColor(getColor(R.color.wizard_segment_current))
                else -> bg.setColor(getColor(R.color.wizard_segment_remaining))
            }
        }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (viewModel.currentStep.value > 1) {
                    viewModel.previousStep()
                } else {
                    finish()
                }
            }
        })
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

    private fun markWizardCompleted() {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_WIZARD_COMPLETED, true)
            .apply()
    }

    private fun markAiWelcomeAsSeen() {
        getSharedPreferences("ai_welcome_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("has_seen_ai_welcome", true)
            .apply()
    }

    companion object {
        const val PREFS_NAME = "setup_wizard_prefs"
        const val KEY_WIZARD_COMPLETED = "wizard_completed"

        fun isFirstRun(context: Context): Boolean {
            return !context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_WIZARD_COMPLETED, false)
        }
    }
}
```

- [ ] **Step 4: Add dimension resource for segment gap**

Add to `res/values/dimens.xml` (create if it doesn't exist):

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <dimen name="wizard_segment_gap">3dp</dimen>
</resources>
```

- [ ] **Step 5: Add color resources for progress segments**

Add to `res/values/colors.xml`:

```xml
    <color name="wizard_segment_completed">#4ade80</color>
    <color name="wizard_segment_current">#4a9eff</color>
    <color name="wizard_segment_remaining">#333333</color>
```

- [ ] **Step 6: Register activity in AndroidManifest.xml**

Add after the AiWelcomeActivity declaration:

```xml
        <activity android:name=".ui.wizard.SetupWizardActivity"
            android:exported="false" />
```

- [ ] **Step 7: Verify it compiles**

Run: `cd /home/philg/src/AndroidStudioProjects/PEVPlugShare && ./gradlew compileDebugKotlin 2>&1 | tail -3`
Expected: BUILD SUCCESSFUL (will have warnings about missing fragment classes — that's fine, we create them next)

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/a42r/pevplugshare/ui/wizard/SetupWizardActivity.kt \
       app/src/main/java/com/a42r/pevplugshare/ui/wizard/WizardPagerAdapter.kt \
       app/src/main/res/layout/activity_setup_wizard.xml \
       app/src/main/res/values/dimens.xml \
       app/src/main/res/values/colors.xml \
       app/src/main/AndroidManifest.xml
git commit -m "feat: add SetupWizardActivity with ViewPager2 host and progress bar"
```

---

### Task 4: Steps 1-5 (Instructional Fragments)

These are the simpler steps that display instructions and checkboxes. Steps 1-5 don't perform any API calls.

**Files:**
- Create: `app/src/main/res/layout/fragment_wizard_welcome.xml`
- Create: `app/src/main/res/layout/fragment_wizard_install_osmand.xml`
- Create: `app/src/main/res/layout/fragment_wizard_login_osmand.xml`
- Create: `app/src/main/res/layout/fragment_wizard_create_folder.xml`
- Create: `app/src/main/res/layout/fragment_wizard_sync_cloud.xml`
- Create: `app/src/main/java/com/a42r/pevplugshare/ui/wizard/steps/WelcomeFragment.kt`
- Create: `app/src/main/java/com/a42r/pevplugshare/ui/wizard/steps/InstallOsmandFragment.kt`
- Create: `app/src/main/java/com/a42r/pevplugshare/ui/wizard/steps/LoginOsmandFragment.kt`
- Create: `app/src/main/java/com/a42r/pevplugshare/ui/wizard/steps/CreateFolderFragment.kt`
- Create: `app/src/main/java/com/a42r/pevplugshare/ui/wizard/steps/SyncCloudFragment.kt`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Add string resources**

Add to `res/values/strings.xml`:

```xml
    <!-- Setup Wizard -->
    <string name="wizard_welcome_title">Welcome to PEVPlugShare</string>
    <string name="wizard_welcome_description">PEVPlugShare helps you find and share charging locations for Personal Electric Vehicles (PEVs) — also known as Personal Mobility Devices (PMDs) — such as e-scooters, e-bikes, electric skateboards, and other light electric vehicles.</string>
    <string name="wizard_welcome_uses">The app uses OsmAnd to store and sync your charging locations, and OpenRouter for optional AI-powered features.</string>
    <string name="wizard_welcome_guide">This wizard will guide you through 9 steps to get everything set up:</string>
    <string name="wizard_welcome_steps">1. Install OsmAnd (map &amp; navigation app)\n2. Create an OsmAnd Cloud account &amp; log in\n3. Create a favourites folder for your charging locations\n4. Sync your folder to OsmAnd Cloud\n5. Connect your OsmAnd account to this app\n6. Verify your folder is available\n7. Set up AI features &amp; choose a model (optional)\n8. Review &amp; finish</string>
    <string name="wizard_welcome_skip_hint">Already have OsmAnd set up? Skip straight to the login screen.</string>
    <string name="wizard_get_started">Get Started</string>
    <string name="wizard_skip">Skip Wizard</string>
    <string name="wizard_next">Next</string>
    <string name="wizard_back">Back</string>

    <string name="wizard_install_title">Install OsmAnd</string>
    <string name="wizard_install_description">OsmAnd is an open-source map and navigation app. PEVPlugShare uses OsmAnd Cloud to store and sync your PEV charging locations across devices.</string>
    <string name="wizard_install_detected">OsmAnd detected on this device</string>
    <string name="wizard_install_detected_detail">You\'re good to go — OsmAnd is already installed.</string>
    <string name="wizard_install_not_found">OsmAnd is not installed</string>
    <string name="wizard_install_not_found_detail">PEVPlugShare requires OsmAnd to store and sync your charging locations. Install either OsmAnd (free) or OsmAnd+ (paid) from the Play Store.</string>
    <string name="wizard_install_play_store_free">Open Play Store — OsmAnd</string>
    <string name="wizard_install_play_store_plus">Open Play Store — OsmAnd+</string>
    <string name="wizard_install_recheck">Re-check Installation</string>
    <string name="wizard_install_checkbox">OsmAnd is installed</string>

    <string name="wizard_login_osmand_title">Log into OsmAnd</string>
    <string name="wizard_login_osmand_description">You need an OsmAnd Cloud account so your PEV charging locations can sync between OsmAnd and PEVPlugShare. If you don\'t have one yet, you can create a free account inside the OsmAnd app.</string>
    <string name="wizard_login_osmand_instructions">In the OsmAnd app:\n\n1. Open OsmAnd on your device\n2. Tap the menu (☰) button\n3. Tap Settings\n4. Tap OsmAnd Cloud\n5. Sign in or tap Create account</string>
    <string name="wizard_login_osmand_tip">Tip: Remember the email and password you use here — you\'ll need the same credentials in Step 6 to connect PEVPlugShare.</string>
    <string name="wizard_login_osmand_checkbox">I have logged into OsmAnd Cloud</string>

    <string name="wizard_create_folder_title">Create a Favourites Folder</string>
    <string name="wizard_create_folder_description">PEVPlugShare needs a dedicated favourites folder in OsmAnd to store the PEV charging locations you discover and share. This must be a separate folder from the default \"Favourites\" — it keeps your charging location data organised and prevents the app from modifying your personal OsmAnd favourites.</string>
    <string name="wizard_create_folder_instructions">In the OsmAnd app:\n\n1. Tap My Places (★ star icon)\n2. Go to the Favourites tab\n3. Tap + to add a new favourite point\n4. In the folder/group field, tap to create a new folder\n5. Give it any name you\'ll recognise — e.g. \"PEV Charging\", \"My Chargers\", \"PMD Charging Spots\"\n6. Save the temporary favourite point (you can delete it afterward)</string>
    <string name="wizard_create_folder_why">Why a separate folder? PEVPlugShare reads and writes to this folder via the OsmAnd Cloud API. Using a dedicated folder ensures your personal OsmAnd favourites are never modified. You can name it whatever you like — you\'ll select it from a list after connecting your account.</string>
    <string name="wizard_create_folder_checkbox">I have created a dedicated favourites folder</string>

    <string name="wizard_sync_title">Sync to OsmAnd Cloud</string>
    <string name="wizard_sync_description">Your new favourites folder currently only exists locally on your device. You need to sync it to OsmAnd Cloud so that PEVPlugShare can access it via the API.</string>
    <string name="wizard_sync_instructions">In the OsmAnd app:\n\n1. Tap the menu (☰) button\n2. Tap Settings\n3. Tap OsmAnd Cloud\n4. Tap Sync now\n5. Wait for the sync to complete — look for the green checkmark</string>
    <string name="wizard_sync_why">Why sync now? PEVPlugShare connects to OsmAnd Cloud (not the local app) to read your folders. If the folder hasn\'t been synced, it won\'t appear when you connect your account.</string>
    <string name="wizard_sync_checkbox">I have synced my favourites to OsmAnd Cloud</string>

    <string name="wizard_connect_title">Connect Your OsmAnd Account</string>
    <string name="wizard_connect_description">Enter the same email and password you used to log into OsmAnd Cloud. PEVPlugShare will verify your account and check for your favourites folders.</string>
    <string name="wizard_connect_button">Connect &amp; Verify</string>
    <string name="wizard_connect_loading">Connecting to OsmAnd Cloud…</string>

    <string name="wizard_verify_title">Select Your PEV Folder</string>
    <string name="wizard_verify_description">We found the following favourites folders in your OsmAnd Cloud account. Select the one you\'d like to use for your PEV charging locations:</string>
    <string name="wizard_verify_excluded">(The default \"Favourites\" folder is excluded)</string>
    <string name="wizard_verify_loading">Checking Your Folders…</string>
    <string name="wizard_verify_not_found_title">No Favourites Folder Found</string>
    <string name="wizard_verify_not_found_detail">We connected to your OsmAnd Cloud account successfully, but couldn\'t find any favourites folders other than the default \"Favourites\".</string>
    <string name="wizard_verify_not_found_hint">This usually means one of two things:</string>
    <string name="wizard_verify_create_heading">A) You haven\'t created the folder yet</string>
    <string name="wizard_verify_create_steps">1. Open OsmAnd on your device\n2. Tap My Places (★ star icon)\n3. Go to the Favourites tab\n4. Tap + to add a new favourite point\n5. Create a new folder with any name you like (e.g. \"PEV Charging\", \"My Chargers\")\n6. Save the point, then sync to cloud (see option B below)</string>
    <string name="wizard_verify_sync_heading">B) You created the folder but haven\'t synced yet</string>
    <string name="wizard_verify_sync_steps">1. Open OsmAnd Settings → OsmAnd Cloud\n2. Tap Sync now\n3. Wait for the sync to complete (green checkmark)</string>
    <string name="wizard_verify_delay_warning">Sync delay: After syncing in OsmAnd, there can be a delay of 1–2 minutes before the folder appears in the OsmAnd Cloud API. If you\'ve already created the folder and synced, please wait a moment and then tap \"Retry\" below.</string>
    <string name="wizard_verify_retry">Retry — Check for Folders</string>
    <string name="wizard_verify_continue">Continue with Selected Folder</string>

    <string name="wizard_ai_title">AI-Assisted Location Creation (Optional)</string>
    <string name="wizard_ai_not_required">AI is not required. You can always create and manage PEV charging locations manually — entering all details yourself. The app works fully without AI features.</string>
    <string name="wizard_ai_description">When enabled, AI makes the process quicker by automatically generating location data from your photos:</string>
    <string name="wizard_ai_features">• Charger type identification — detects make and model\n• Socket/plug detection — identifies connector types\n• Operating hours — reads hours from signage\n• Location details — extracts access info, fees, etc.</string>
    <string name="wizard_ai_review_note">You always review and refine the AI-generated information before submitting. Nothing is saved without your confirmation.</string>
    <string name="wizard_ai_openrouter_note">Requires a free OpenRouter.ai account. You can set this up later from Settings at any time.</string>
    <string name="wizard_ai_connect">Connect OpenRouter</string>
    <string name="wizard_ai_skip">Skip — I\'ll Add Manually</string>
    <string name="wizard_ai_model_title">Choose an AI Model</string>
    <string name="wizard_ai_model_description">PEVPlugShare uses a vision-capable AI model to analyse your charger photos and generate location details. Different models offer different trade-offs between cost and quality.</string>
    <string name="wizard_ai_model_why">The model determines how accurately photos are analysed, how detailed the generated descriptions are, and how well socket types and operating hours are identified. Better models produce more accurate and complete results with fewer corrections needed.</string>
    <string name="wizard_ai_model_continue">Continue with Selected Model</string>
    <string name="wizard_ai_connected">OpenRouter connected successfully</string>
    <string name="wizard_ai_connected_detail">AI features are now available.</string>

    <string name="wizard_complete_title">You\'re All Set!</string>
    <string name="wizard_complete_description">Setup complete. Here\'s your configuration:</string>
    <string name="wizard_complete_start">Start Using PEVPlugShare</string>
```

- [ ] **Step 2: Create fragment_wizard_welcome.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:fillViewport="true">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="32dp"
        android:gravity="center_horizontal">

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="⚡"
            android:textSize="48sp"
            android:layout_marginBottom="16dp" />

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/wizard_welcome_title"
            android:textSize="24sp"
            android:textStyle="bold"
            android:textAlignment="center"
            android:layout_marginBottom="16dp" />

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/wizard_welcome_description"
            android:textSize="14sp"
            android:lineSpacingMultiplier="1.3"
            android:layout_marginBottom="12dp" />

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/wizard_welcome_uses"
            android:textSize="14sp"
            android:lineSpacingMultiplier="1.3"
            android:layout_marginBottom="16dp" />

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/wizard_welcome_guide"
            android:textSize="14sp"
            android:layout_marginBottom="8dp" />

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/wizard_welcome_steps"
            android:textSize="13sp"
            android:lineSpacingMultiplier="1.4"
            android:paddingStart="8dp"
            android:layout_marginBottom="16dp" />

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/wizard_welcome_skip_hint"
            android:textSize="12sp"
            android:alpha="0.7"
            android:layout_marginBottom="24dp" />

        <View
            android:layout_width="match_parent"
            android:layout_height="0dp"
            android:layout_weight="1"
            android:minHeight="16dp" />

        <Button
            android:id="@+id/getStartedButton"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/wizard_get_started"
            android:textSize="16sp"
            android:layout_marginBottom="8dp" />

        <Button
            android:id="@+id/skipWizardButton"
            style="@style/Widget.Material3.Button.TextButton"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/wizard_skip"
            android:textSize="14sp" />

    </LinearLayout>
</ScrollView>
```

- [ ] **Step 3: Create WelcomeFragment.kt**

```kotlin
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_wizard_welcome, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<Button>(R.id.getStartedButton).setOnClickListener {
            viewModel.nextStep()
        }
        view.findViewById<Button>(R.id.skipWizardButton).setOnClickListener {
            (activity as? SetupWizardActivity)?.skipWizard()
        }
    }
}
```

- [ ] **Step 4: Create fragment_wizard_install_osmand.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:fillViewport="true">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="32dp">

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/wizard_install_title"
            android:textSize="20sp"
            android:textStyle="bold"
            android:layout_marginBottom="12dp" />

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/wizard_install_description"
            android:textSize="14sp"
            android:lineSpacingMultiplier="1.3"
            android:layout_marginBottom="16dp" />

        <!-- Detected state -->
        <LinearLayout
            android:id="@+id/detectedContainer"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:background="@drawable/wizard_success_bg"
            android:padding="14dp"
            android:layout_marginBottom="12dp"
            android:visibility="gone">

            <TextView
                android:id="@+id/detectedText"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="@string/wizard_install_detected"
                android:textSize="14sp"
                android:textStyle="bold"
                android:textColor="#4ade80" />

            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="@string/wizard_install_detected_detail"
                android:textSize="12sp"
                android:layout_marginTop="4dp"
                android:textColor="#86efac" />
        </LinearLayout>

        <!-- Not found state -->
        <LinearLayout
            android:id="@+id/notFoundContainer"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:background="@drawable/wizard_error_bg"
            android:padding="14dp"
            android:layout_marginBottom="12dp"
            android:visibility="gone">

            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="@string/wizard_install_not_found"
                android:textSize="14sp"
                android:textStyle="bold"
                android:textColor="#fca5a5" />

            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="@string/wizard_install_not_found_detail"
                android:textSize="12sp"
                android:layout_marginTop="6dp" />

            <Button
                android:id="@+id/playStoreFreeButton"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="10dp"
                android:text="@string/wizard_install_play_store_free" />

            <Button
                android:id="@+id/playStorePlusButton"
                style="@style/Widget.Material3.Button.OutlinedButton"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="6dp"
                android:text="@string/wizard_install_play_store_plus" />
        </LinearLayout>

        <Button
            android:id="@+id/recheckButton"
            style="@style/Widget.Material3.Button.OutlinedButton"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/wizard_install_recheck"
            android:visibility="gone"
            android:layout_marginBottom="16dp" />

        <CheckBox
            android:id="@+id/installedCheckbox"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/wizard_install_checkbox"
            android:enabled="false"
            android:layout_marginBottom="16dp" />

        <View
            android:layout_width="match_parent"
            android:layout_height="0dp"
            android:layout_weight="1"
            android:minHeight="16dp" />

        <Button
            android:id="@+id/nextButton"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/wizard_next"
            android:enabled="false" />

    </LinearLayout>
</ScrollView>
```

- [ ] **Step 5: Create drawable backgrounds for status boxes**

Create `res/drawable/wizard_success_bg.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="#1a3a1a" />
    <stroke android:width="1dp" android:color="#2d5a2d" />
    <corners android:radius="8dp" />
</shape>
```

Create `res/drawable/wizard_error_bg.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="#3a1a1a" />
    <stroke android:width="1dp" android:color="#5a2d2d" />
    <corners android:radius="8dp" />
</shape>
```

Create `res/drawable/wizard_info_bg.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="#1a2a3a" />
    <stroke android:width="1dp" android:color="#2a4a6a" />
    <corners android:radius="8dp" />
</shape>
```

Create `res/drawable/wizard_warning_bg.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="#2a2a1a" />
    <stroke android:width="1dp" android:color="#5a5a2d" />
    <corners android:radius="8dp" />
</shape>
```

- [ ] **Step 6: Create InstallOsmandFragment.kt**

```kotlin
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
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.a42r.pevplugshare.R
import com.a42r.pevplugshare.ui.wizard.SetupWizardViewModel
import com.a42r.pevplugshare.util.OsmandIntentHelper

class InstallOsmandFragment : Fragment() {

    private val viewModel: SetupWizardViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_wizard_install_osmand, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val detectedContainer = view.findViewById<LinearLayout>(R.id.detectedContainer)
        val detectedText = view.findViewById<TextView>(R.id.detectedText)
        val notFoundContainer = view.findViewById<LinearLayout>(R.id.notFoundContainer)
        val recheckButton = view.findViewById<Button>(R.id.recheckButton)
        val checkbox = view.findViewById<CheckBox>(R.id.installedCheckbox)
        val nextButton = view.findViewById<Button>(R.id.nextButton)

        fun checkInstallation() {
            val installed = OsmandIntentHelper.isOsmandInstalled(requireContext())
            viewModel.setOsmandInstalled(installed)

            if (installed) {
                val pkg = OsmandIntentHelper.getOsmandPackage(requireContext())
                val name = if (pkg == OsmandIntentHelper.OSMAND_PACKAGE) "OsmAnd+" else "OsmAnd"
                detectedText.text = "✓ $name detected on this device"
                detectedContainer.visibility = View.VISIBLE
                notFoundContainer.visibility = View.GONE
                recheckButton.visibility = View.GONE
                checkbox.isChecked = true
                nextButton.isEnabled = true
            } else {
                detectedContainer.visibility = View.GONE
                notFoundContainer.visibility = View.VISIBLE
                recheckButton.visibility = View.VISIBLE
                checkbox.isChecked = false
                nextButton.isEnabled = false
            }
        }

        checkInstallation()

        view.findViewById<Button>(R.id.playStoreFreeButton).setOnClickListener {
            openPlayStore("net.osmand")
        }
        view.findViewById<Button>(R.id.playStorePlusButton).setOnClickListener {
            openPlayStore("net.osmand.plus")
        }
        recheckButton.setOnClickListener { checkInstallation() }
        nextButton.setOnClickListener { viewModel.nextStep() }
    }

    override fun onResume() {
        super.onResume()
        // Re-check when returning from Play Store
        view?.let {
            val installed = OsmandIntentHelper.isOsmandInstalled(requireContext())
            if (installed && !viewModel.osmandInstalled.value) {
                it.findViewById<Button>(R.id.recheckButton)?.performClick()
            }
        }
    }

    private fun openPlayStore(packageName: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
        } catch (e: Exception) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
        }
    }
}
```

- [ ] **Step 7: Create fragment layouts for Steps 3-5**

Create `res/layout/fragment_wizard_login_osmand.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:fillViewport="true">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="32dp">

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/wizard_login_osmand_title"
            android:textSize="20sp"
            android:textStyle="bold"
            android:layout_marginBottom="12dp" />

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/wizard_login_osmand_description"
            android:textSize="14sp"
            android:lineSpacingMultiplier="1.3"
            android:layout_marginBottom="16dp" />

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/wizard_login_osmand_instructions"
            android:textSize="14sp"
            android:lineSpacingMultiplier="1.5"
            android:layout_marginBottom="16dp" />

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/wizard_login_osmand_tip"
            android:textSize="12sp"
            android:background="@drawable/wizard_info_bg"
            android:padding="12dp"
            android:textColor="#7dd3fc"
            android:layout_marginBottom="16dp" />

        <CheckBox
            android:id="@+id/loggedInCheckbox"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/wizard_login_osmand_checkbox"
            android:layout_marginBottom="16dp" />

        <View
            android:layout_width="match_parent"
            android:layout_height="0dp"
            android:layout_weight="1"
            android:minHeight="16dp" />

        <Button
            android:id="@+id/nextButton"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/wizard_next"
            android:enabled="false" />

    </LinearLayout>
</ScrollView>
```

Create `res/layout/fragment_wizard_create_folder.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:fillViewport="true">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="32dp">

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/wizard_create_folder_title"
            android:textSize="20sp"
            android:textStyle="bold"
            android:layout_marginBottom="12dp" />

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/wizard_create_folder_description"
            android:textSize="14sp"
            android:lineSpacingMultiplier="1.3"
            android:layout_marginBottom="16dp" />

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/wizard_create_folder_instructions"
            android:textSize="14sp"
            android:lineSpacingMultiplier="1.5"
            android:layout_marginBottom="16dp" />

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/wizard_create_folder_why"
            android:textSize="12sp"
            android:background="@drawable/wizard_info_bg"
            android:padding="12dp"
            android:textColor="#7dd3fc"
            android:layout_marginBottom="16dp" />

        <CheckBox
            android:id="@+id/folderCreatedCheckbox"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/wizard_create_folder_checkbox"
            android:layout_marginBottom="16dp" />

        <View
            android:layout_width="match_parent"
            android:layout_height="0dp"
            android:layout_weight="1"
            android:minHeight="16dp" />

        <Button
            android:id="@+id/nextButton"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/wizard_next"
            android:enabled="false" />

    </LinearLayout>
</ScrollView>
```

Create `res/layout/fragment_wizard_sync_cloud.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:fillViewport="true">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="32dp">

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/wizard_sync_title"
            android:textSize="20sp"
            android:textStyle="bold"
            android:layout_marginBottom="12dp" />

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/wizard_sync_description"
            android:textSize="14sp"
            android:lineSpacingMultiplier="1.3"
            android:layout_marginBottom="16dp" />

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/wizard_sync_instructions"
            android:textSize="14sp"
            android:lineSpacingMultiplier="1.5"
            android:layout_marginBottom="16dp" />

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/wizard_sync_why"
            android:textSize="12sp"
            android:background="@drawable/wizard_info_bg"
            android:padding="12dp"
            android:textColor="#7dd3fc"
            android:layout_marginBottom="16dp" />

        <CheckBox
            android:id="@+id/syncedCheckbox"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/wizard_sync_checkbox"
            android:layout_marginBottom="16dp" />

        <View
            android:layout_width="match_parent"
            android:layout_height="0dp"
            android:layout_weight="1"
            android:minHeight="16dp" />

        <Button
            android:id="@+id/nextButton"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/wizard_next"
            android:enabled="false" />

    </LinearLayout>
</ScrollView>
```

- [ ] **Step 8: Create fragment classes for Steps 3-5**

These three fragments follow the same checkbox-gated pattern.

Create `LoginOsmandFragment.kt`:

```kotlin
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

class LoginOsmandFragment : Fragment() {

    private val viewModel: SetupWizardViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_wizard_login_osmand, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val checkbox = view.findViewById<CheckBox>(R.id.loggedInCheckbox)
        val nextButton = view.findViewById<Button>(R.id.nextButton)

        checkbox.setOnCheckedChangeListener { _, isChecked ->
            nextButton.isEnabled = isChecked
            viewModel.markStepCompleted(3, isChecked)
        }
        nextButton.setOnClickListener { viewModel.nextStep() }
    }
}
```

Create `CreateFolderFragment.kt`:

```kotlin
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_wizard_create_folder, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val checkbox = view.findViewById<CheckBox>(R.id.folderCreatedCheckbox)
        val nextButton = view.findViewById<Button>(R.id.nextButton)

        checkbox.setOnCheckedChangeListener { _, isChecked ->
            nextButton.isEnabled = isChecked
            viewModel.markStepCompleted(4, isChecked)
        }
        nextButton.setOnClickListener { viewModel.nextStep() }
    }
}
```

Create `SyncCloudFragment.kt`:

```kotlin
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

class SyncCloudFragment : Fragment() {

    private val viewModel: SetupWizardViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_wizard_sync_cloud, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val checkbox = view.findViewById<CheckBox>(R.id.syncedCheckbox)
        val nextButton = view.findViewById<Button>(R.id.nextButton)

        checkbox.setOnCheckedChangeListener { _, isChecked ->
            nextButton.isEnabled = isChecked
            viewModel.markStepCompleted(5, isChecked)
        }
        nextButton.setOnClickListener { viewModel.nextStep() }
    }
}
```

- [ ] **Step 9: Verify it compiles**

Run: `cd /home/philg/src/AndroidStudioProjects/PEVPlugShare && ./gradlew compileDebugKotlin 2>&1 | tail -3`
Expected: BUILD SUCCESSFUL

- [ ] **Step 10: Commit**

```bash
git add app/src/main/java/com/a42r/pevplugshare/ui/wizard/steps/ \
       app/src/main/res/layout/fragment_wizard_welcome.xml \
       app/src/main/res/layout/fragment_wizard_install_osmand.xml \
       app/src/main/res/layout/fragment_wizard_login_osmand.xml \
       app/src/main/res/layout/fragment_wizard_create_folder.xml \
       app/src/main/res/layout/fragment_wizard_sync_cloud.xml \
       app/src/main/res/drawable/wizard_success_bg.xml \
       app/src/main/res/drawable/wizard_error_bg.xml \
       app/src/main/res/drawable/wizard_info_bg.xml \
       app/src/main/res/drawable/wizard_warning_bg.xml \
       app/src/main/res/values/strings.xml
git commit -m "feat: add wizard Steps 1-5 fragments with layouts and string resources"
```

---

### Task 5: Step 6 — Connect Account

**Files:**
- Create: `app/src/main/res/layout/fragment_wizard_connect_account.xml`
- Create: `app/src/main/java/com/a42r/pevplugshare/ui/wizard/steps/ConnectAccountFragment.kt`

- [ ] **Step 1: Create layout**

```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:fillViewport="true">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="32dp">

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/wizard_connect_title"
            android:textSize="20sp"
            android:textStyle="bold"
            android:layout_marginBottom="12dp" />

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/wizard_connect_description"
            android:textSize="14sp"
            android:lineSpacingMultiplier="1.3"
            android:layout_marginBottom="16dp" />

        <!-- Error container -->
        <LinearLayout
            android:id="@+id/errorContainer"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:background="@drawable/wizard_error_bg"
            android:padding="14dp"
            android:layout_marginBottom="16dp"
            android:visibility="gone">

            <TextView
                android:id="@+id/errorTitle"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:textSize="13sp"
                android:textStyle="bold"
                android:textColor="#fca5a5" />

            <TextView
                android:id="@+id/errorDetail"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:textSize="12sp"
                android:layout_marginTop="6dp" />
        </LinearLayout>

        <com.google.android.material.textfield.TextInputLayout
            android:id="@+id/emailLayout"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:hint="Email">

            <com.google.android.material.textfield.TextInputEditText
                android:id="@+id/emailInput"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:inputType="textEmailAddress"
                android:maxLines="1" />
        </com.google.android.material.textfield.TextInputLayout>

        <com.google.android.material.textfield.TextInputLayout
            android:id="@+id/passwordLayout"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="12dp"
            android:hint="Password">

            <com.google.android.material.textfield.TextInputEditText
                android:id="@+id/passwordInput"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:inputType="textPassword"
                android:maxLines="1" />
        </com.google.android.material.textfield.TextInputLayout>

        <CheckBox
            android:id="@+id/rememberMeCheckbox"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="12dp"
            android:text="Remember me" />

        <!-- Loading indicator -->
        <LinearLayout
            android:id="@+id/loadingContainer"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:gravity="center"
            android:padding="32dp"
            android:visibility="gone">

            <ProgressBar
                android:layout_width="wrap_content"
                android:layout_height="wrap_content" />

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="16dp"
                android:text="@string/wizard_connect_loading"
                android:textSize="14sp" />
        </LinearLayout>

        <View
            android:layout_width="match_parent"
            android:layout_height="0dp"
            android:layout_weight="1"
            android:minHeight="16dp" />

        <Button
            android:id="@+id/connectButton"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/wizard_connect_button"
            android:textSize="16sp" />

    </LinearLayout>
</ScrollView>
```

- [ ] **Step 2: Create ConnectAccountFragment.kt**

```kotlin
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.a42r.pevplugshare.R
import com.a42r.pevplugshare.ui.wizard.SetupWizardViewModel
import com.a42r.pevplugshare.ui.wizard.WizardLoginState
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class ConnectAccountFragment : Fragment() {

    private val viewModel: SetupWizardViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_wizard_connect_account, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val emailInput = view.findViewById<TextInputEditText>(R.id.emailInput)
        val passwordInput = view.findViewById<TextInputEditText>(R.id.passwordInput)
        val rememberMe = view.findViewById<CheckBox>(R.id.rememberMeCheckbox)
        val connectButton = view.findViewById<Button>(R.id.connectButton)
        val loadingContainer = view.findViewById<LinearLayout>(R.id.loadingContainer)
        val errorContainer = view.findViewById<LinearLayout>(R.id.errorContainer)
        val errorTitle = view.findViewById<TextView>(R.id.errorTitle)
        val errorDetail = view.findViewById<TextView>(R.id.errorDetail)
        val emailLayout = view.findViewById<TextInputLayout>(R.id.emailLayout)
        val passwordLayout = view.findViewById<TextInputLayout>(R.id.passwordLayout)

        connectButton.setOnClickListener {
            val email = emailInput.text?.toString()?.trim() ?: ""
            val password = passwordInput.text?.toString() ?: ""

            if (email.isEmpty()) {
                emailLayout.error = "Email is required"
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                passwordLayout.error = "Password is required"
                return@setOnClickListener
            }

            emailLayout.error = null
            passwordLayout.error = null
            viewModel.loginToOsmand(email, password, rememberMe.isChecked)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
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
                            // Auto-advance to folder verification
                            viewModel.nextStep()
                            viewModel.verifyFolders()
                        }
                        is WizardLoginState.Error -> {
                            loadingContainer.visibility = View.GONE
                            errorContainer.visibility = View.VISIBLE
                            errorTitle.text = "✗ Login failed"
                            errorDetail.text = state.message
                            connectButton.isEnabled = true
                            connectButton.text = "Try Again"
                        }
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 3: Verify it compiles**

Run: `cd /home/philg/src/AndroidStudioProjects/PEVPlugShare && ./gradlew compileDebugKotlin 2>&1 | tail -3`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/a42r/pevplugshare/ui/wizard/steps/ConnectAccountFragment.kt \
       app/src/main/res/layout/fragment_wizard_connect_account.xml
git commit -m "feat: add wizard Step 6 — OsmAnd account connection with login form"
```

---

### Task 6: Step 7 — Verify Folder

**Files:**
- Create: `app/src/main/res/layout/fragment_wizard_verify_folder.xml`
- Create: `app/src/main/java/com/a42r/pevplugshare/ui/wizard/steps/VerifyFolderFragment.kt`

- [ ] **Step 1: Create layout**

```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:fillViewport="true">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="32dp">

        <!-- Loading state -->
        <LinearLayout
            android:id="@+id/loadingContainer"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:gravity="center"
            android:padding="48dp"
            android:visibility="gone">

            <ProgressBar
                android:layout_width="wrap_content"
                android:layout_height="wrap_content" />

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="16dp"
                android:text="@string/wizard_verify_loading"
                android:textSize="16sp"
                android:textStyle="bold" />
        </LinearLayout>

        <!-- Found state -->
        <LinearLayout
            android:id="@+id/foundContainer"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:visibility="gone">

            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="@string/wizard_verify_title"
                android:textSize="20sp"
                android:textStyle="bold"
                android:layout_marginBottom="12dp" />

            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="@string/wizard_verify_description"
                android:textSize="14sp"
                android:lineSpacingMultiplier="1.3"
                android:layout_marginBottom="8dp" />

            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="@string/wizard_verify_excluded"
                android:textSize="11sp"
                android:alpha="0.7"
                android:layout_marginBottom="16dp" />

            <RadioGroup
                android:id="@+id/folderRadioGroup"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginBottom="16dp" />

            <Button
                android:id="@+id/continueButton"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="@string/wizard_verify_continue"
                android:enabled="false" />
        </LinearLayout>

        <!-- Not found state -->
        <LinearLayout
            android:id="@+id/notFoundContainer"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:visibility="gone">

            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="@string/wizard_verify_not_found_title"
                android:textSize="20sp"
                android:textStyle="bold"
                android:layout_marginBottom="12dp" />

            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="@string/wizard_verify_not_found_detail"
                android:textSize="14sp"
                android:lineSpacingMultiplier="1.3"
                android:layout_marginBottom="8dp" />

            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="@string/wizard_verify_not_found_hint"
                android:textSize="14sp"
                android:layout_marginBottom="12dp" />

            <!-- Panel A: Create folder -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:background="@drawable/wizard_info_bg"
                android:padding="14dp"
                android:layout_marginBottom="12dp">

                <TextView
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:text="@string/wizard_verify_create_heading"
                    android:textSize="13sp"
                    android:textStyle="bold"
                    android:textColor="#7dd3fc"
                    android:layout_marginBottom="8dp" />

                <TextView
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:text="@string/wizard_verify_create_steps"
                    android:textSize="12sp"
                    android:lineSpacingMultiplier="1.4" />
            </LinearLayout>

            <!-- Panel B: Sync -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:background="@drawable/wizard_info_bg"
                android:padding="14dp"
                android:layout_marginBottom="12dp">

                <TextView
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:text="@string/wizard_verify_sync_heading"
                    android:textSize="13sp"
                    android:textStyle="bold"
                    android:textColor="#7dd3fc"
                    android:layout_marginBottom="8dp" />

                <TextView
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:text="@string/wizard_verify_sync_steps"
                    android:textSize="12sp"
                    android:lineSpacingMultiplier="1.4" />
            </LinearLayout>

            <!-- Delay warning -->
            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="@string/wizard_verify_delay_warning"
                android:textSize="12sp"
                android:background="@drawable/wizard_warning_bg"
                android:padding="12dp"
                android:textColor="#fde68a"
                android:layout_marginBottom="16dp" />

            <Button
                android:id="@+id/retryButton"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="@string/wizard_verify_retry" />
        </LinearLayout>

        <!-- Error state -->
        <LinearLayout
            android:id="@+id/errorContainer"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:visibility="gone">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:background="@drawable/wizard_error_bg"
                android:padding="14dp"
                android:layout_marginBottom="16dp">

                <TextView
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:text="✗ Failed to fetch favourites"
                    android:textSize="13sp"
                    android:textStyle="bold"
                    android:textColor="#fca5a5" />

                <TextView
                    android:id="@+id/errorDetail"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:textSize="12sp"
                    android:layout_marginTop="6dp" />
            </LinearLayout>

            <Button
                android:id="@+id/errorRetryButton"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="Retry" />
        </LinearLayout>

    </LinearLayout>
</ScrollView>
```

- [ ] **Step 2: Create VerifyFolderFragment.kt**

```kotlin
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.a42r.pevplugshare.R
import com.a42r.pevplugshare.ui.wizard.FolderVerificationState
import com.a42r.pevplugshare.ui.wizard.SetupWizardViewModel
import kotlinx.coroutines.launch

class VerifyFolderFragment : Fragment() {

    private val viewModel: SetupWizardViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_wizard_verify_folder, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val loadingContainer = view.findViewById<LinearLayout>(R.id.loadingContainer)
        val foundContainer = view.findViewById<LinearLayout>(R.id.foundContainer)
        val notFoundContainer = view.findViewById<LinearLayout>(R.id.notFoundContainer)
        val errorContainer = view.findViewById<LinearLayout>(R.id.errorContainer)
        val radioGroup = view.findViewById<RadioGroup>(R.id.folderRadioGroup)
        val continueButton = view.findViewById<Button>(R.id.continueButton)
        val retryButton = view.findViewById<Button>(R.id.retryButton)
        val errorRetryButton = view.findViewById<Button>(R.id.errorRetryButton)
        val errorDetail = view.findViewById<TextView>(R.id.errorDetail)

        retryButton.setOnClickListener { viewModel.verifyFolders() }
        errorRetryButton.setOnClickListener { viewModel.verifyFolders() }

        continueButton.setOnClickListener {
            val selectedId = radioGroup.checkedRadioButtonId
            if (selectedId != -1) {
                val selectedButton = view.findViewById<RadioButton>(selectedId)
                viewModel.selectCategory(selectedButton.text.toString())
                viewModel.nextStep()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.folderState.collect { state ->
                    loadingContainer.visibility = View.GONE
                    foundContainer.visibility = View.GONE
                    notFoundContainer.visibility = View.GONE
                    errorContainer.visibility = View.GONE

                    when (state) {
                        is FolderVerificationState.Idle -> {
                            // Trigger verification if not already running
                            viewModel.verifyFolders()
                        }
                        is FolderVerificationState.Loading -> {
                            loadingContainer.visibility = View.VISIBLE
                        }
                        is FolderVerificationState.Found -> {
                            foundContainer.visibility = View.VISIBLE
                            radioGroup.removeAllViews()
                            state.categories.forEachIndexed { index, category ->
                                val radio = RadioButton(requireContext()).apply {
                                    text = category
                                    id = View.generateViewId()
                                    textSize = 14f
                                    setPadding(16, 16, 16, 16)
                                }
                                radioGroup.addView(radio)
                                if (index == 0) {
                                    radio.isChecked = true
                                    continueButton.isEnabled = true
                                }
                            }
                            radioGroup.setOnCheckedChangeListener { _, _ ->
                                continueButton.isEnabled = true
                            }
                        }
                        is FolderVerificationState.NotFound -> {
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
    }
}
```

- [ ] **Step 3: Verify it compiles**

Run: `cd /home/philg/src/AndroidStudioProjects/PEVPlugShare && ./gradlew compileDebugKotlin 2>&1 | tail -3`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/a42r/pevplugshare/ui/wizard/steps/VerifyFolderFragment.kt \
       app/src/main/res/layout/fragment_wizard_verify_folder.xml
git commit -m "feat: add wizard Step 7 — folder verification with retry and remediation"
```

---

### Task 7: Step 8 — AI Features & Model Selection

**Files:**
- Create: `app/src/main/res/layout/fragment_wizard_ai_features.xml`
- Create: `app/src/main/java/com/a42r/pevplugshare/ui/wizard/steps/AiFeaturesFragment.kt`

- [ ] **Step 1: Create layout with ViewSwitcher for sub-screens**

```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:fillViewport="true">

    <ViewSwitcher
        android:id="@+id/viewSwitcher"
        android:layout_width="match_parent"
        android:layout_height="wrap_content">

        <!-- Sub-screen 0: AI Overview -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="32dp">

            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="@string/wizard_ai_title"
                android:textSize="20sp"
                android:textStyle="bold"
                android:layout_marginBottom="12dp" />

            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="@string/wizard_ai_not_required"
                android:textSize="13sp"
                android:background="@drawable/wizard_info_bg"
                android:padding="14dp"
                android:textColor="#7dd3fc"
                android:layout_marginBottom="16dp" />

            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="@string/wizard_ai_description"
                android:textSize="14sp"
                android:layout_marginBottom="8dp" />

            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="@string/wizard_ai_features"
                android:textSize="13sp"
                android:lineSpacingMultiplier="1.4"
                android:layout_marginBottom="16dp" />

            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="@string/wizard_ai_review_note"
                android:textSize="13sp"
                android:background="@drawable/wizard_success_bg"
                android:padding="14dp"
                android:textColor="#86efac"
                android:layout_marginBottom="16dp" />

            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="@string/wizard_ai_openrouter_note"
                android:textSize="13sp"
                android:layout_marginBottom="24dp" />

            <Button
                android:id="@+id/connectOpenRouterButton"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="@string/wizard_ai_connect"
                android:layout_marginBottom="8dp" />

            <Button
                android:id="@+id/skipAiButton"
                style="@style/Widget.Material3.Button.TextButton"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="@string/wizard_ai_skip" />
        </LinearLayout>

        <!-- Sub-screen 1: Model Selection -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="32dp">

            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="@string/wizard_ai_model_title"
                android:textSize="20sp"
                android:textStyle="bold"
                android:layout_marginBottom="12dp" />

            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="@string/wizard_ai_model_description"
                android:textSize="14sp"
                android:lineSpacingMultiplier="1.3"
                android:layout_marginBottom="12dp" />

            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="@string/wizard_ai_model_why"
                android:textSize="12sp"
                android:background="@drawable/wizard_info_bg"
                android:padding="14dp"
                android:textColor="#7dd3fc"
                android:layout_marginBottom="16dp" />

            <!-- Free models -->
            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="Free Models"
                android:textSize="14sp"
                android:textStyle="bold"
                android:textColor="#4ade80"
                android:layout_marginBottom="4dp" />

            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="Good for trying out AI features at no cost. May occasionally miss details."
                android:textSize="12sp"
                android:layout_marginBottom="8dp" />

            <RadioGroup
                android:id="@+id/modelRadioGroup"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginBottom="16dp">

                <RadioButton
                    android:id="@+id/modelGeminiFlash"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:text="Google Gemini 2.0 Flash (FREE) — Recommended"
                    android:textSize="13sp"
                    android:padding="12dp"
                    android:checked="true" />

                <RadioButton
                    android:id="@+id/modelLlama"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:text="Meta Llama 4 Scout (FREE)"
                    android:textSize="13sp"
                    android:padding="12dp" />

                <RadioButton
                    android:id="@+id/modelQwen"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:text="Qwen2.5 VL 72B (FREE)"
                    android:textSize="13sp"
                    android:padding="12dp" />

            <!-- Paid models header -->
            </RadioGroup>

            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="Paid Models"
                android:textSize="14sp"
                android:textStyle="bold"
                android:textColor="#c084fc"
                android:layout_marginBottom="4dp" />

            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="More accurate analysis and richer descriptions. Typically a few cents per location."
                android:textSize="12sp"
                android:layout_marginBottom="8dp" />

            <RadioGroup
                android:id="@+id/paidModelRadioGroup"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginBottom="16dp">

                <RadioButton
                    android:id="@+id/modelClaudeSonnet"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:text="Anthropic Claude Sonnet 4 (~$0.01/loc) — Best Value"
                    android:textSize="13sp"
                    android:padding="12dp" />

                <RadioButton
                    android:id="@+id/modelGpt4o"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:text="OpenAI GPT-4o (~$0.01/loc)"
                    android:textSize="13sp"
                    android:padding="12dp" />

                <RadioButton
                    android:id="@+id/modelGeminiPro"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:text="Google Gemini 2.5 Pro (~$0.02/loc)"
                    android:textSize="13sp"
                    android:padding="12dp" />
            </RadioGroup>

            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="You can change your model at any time in Settings."
                android:textSize="11sp"
                android:alpha="0.7"
                android:layout_marginBottom="16dp" />

            <Button
                android:id="@+id/modelContinueButton"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="@string/wizard_ai_model_continue" />
        </LinearLayout>

    </ViewSwitcher>
</ScrollView>
```

- [ ] **Step 2: Create AiFeaturesFragment.kt**

```kotlin
package com.a42r.pevplugshare.ui.wizard.steps

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ViewSwitcher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.a42r.pevplugshare.R
import com.a42r.pevplugshare.ui.oauth.OAuthActivity
import com.a42r.pevplugshare.ui.settings.model.AiModel
import com.a42r.pevplugshare.ui.wizard.SetupWizardViewModel

class AiFeaturesFragment : Fragment() {

    private val viewModel: SetupWizardViewModel by activityViewModels()

    private val modelMap = mapOf(
        R.id.modelGeminiFlash to AiModel(
            id = "google/gemini-2.0-flash-exp",
            name = "Google Gemini 2.0 Flash",
            provider = "Google",
            cost = "Free"
        ),
        R.id.modelLlama to AiModel(
            id = "meta-llama/llama-4-scout:free",
            name = "Meta Llama 4 Scout",
            provider = "Meta",
            cost = "Free"
        ),
        R.id.modelQwen to AiModel(
            id = "qwen/qwen2.5-vl-72b-instruct:free",
            name = "Qwen2.5 VL 72B",
            provider = "Qwen",
            cost = "Free"
        ),
        R.id.modelClaudeSonnet to AiModel(
            id = "anthropic/claude-sonnet-4",
            name = "Anthropic Claude Sonnet 4",
            provider = "Anthropic",
            cost = "~$0.01/location"
        ),
        R.id.modelGpt4o to AiModel(
            id = "openai/gpt-4o",
            name = "OpenAI GPT-4o",
            provider = "OpenAI",
            cost = "~$0.01/location"
        ),
        R.id.modelGeminiPro to AiModel(
            id = "google/gemini-2.5-pro-preview",
            name = "Google Gemini 2.5 Pro",
            provider = "Google",
            cost = "~$0.02/location"
        )
    )

    private val oauthLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // OAuthActivity stores the API key in TokenManager on success
        // Check if we now have an API key
        if (viewModel.openRouterConnected.value) {
            view?.findViewById<ViewSwitcher>(R.id.viewSwitcher)?.showNext()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_wizard_ai_features, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewSwitcher = view.findViewById<ViewSwitcher>(R.id.viewSwitcher)
        val freeRadioGroup = view.findViewById<RadioGroup>(R.id.modelRadioGroup)
        val paidRadioGroup = view.findViewById<RadioGroup>(R.id.paidModelRadioGroup)

        // AI Overview buttons
        view.findViewById<Button>(R.id.connectOpenRouterButton).setOnClickListener {
            val intent = Intent(requireContext(), OAuthActivity::class.java).apply {
                putExtra("from_first_launch", true)
            }
            oauthLauncher.launch(intent)
        }

        view.findViewById<Button>(R.id.skipAiButton).setOnClickListener {
            viewModel.skipAi()
            viewModel.nextStep()
        }

        // Mutual exclusion between free and paid radio groups
        freeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId != -1) paidRadioGroup.clearCheck()
        }
        paidRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId != -1) freeRadioGroup.clearCheck()
        }

        // Model continue button
        view.findViewById<Button>(R.id.modelContinueButton).setOnClickListener {
            val selectedId = freeRadioGroup.checkedRadioButtonId.takeIf { it != -1 }
                ?: paidRadioGroup.checkedRadioButtonId
            val model = modelMap[selectedId]
            if (model != null) {
                viewModel.setSelectedModel(model)
                viewModel.setOpenRouterConnected(true)
                viewModel.nextStep()
            }
        }
    }

}
```

- [ ] **Step 3: Verify it compiles**

Run: `cd /home/philg/src/AndroidStudioProjects/PEVPlugShare && ./gradlew compileDebugKotlin 2>&1 | tail -3`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/a42r/pevplugshare/ui/wizard/steps/AiFeaturesFragment.kt \
       app/src/main/res/layout/fragment_wizard_ai_features.xml
git commit -m "feat: add wizard Step 8 — AI features overview, OAuth launch, and model selection"
```

---

### Task 8: Step 9 — Completion & Summary

**Files:**
- Create: `app/src/main/res/layout/fragment_wizard_completion.xml`
- Create: `app/src/main/java/com/a42r/pevplugshare/ui/wizard/steps/CompletionFragment.kt`

- [ ] **Step 1: Create layout**

```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:fillViewport="true">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="32dp"
        android:gravity="center_horizontal">

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="🎉"
            android:textSize="48sp"
            android:layout_marginBottom="16dp" />

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/wizard_complete_title"
            android:textSize="22sp"
            android:textStyle="bold"
            android:textAlignment="center"
            android:layout_marginBottom="8dp" />

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/wizard_complete_description"
            android:textSize="14sp"
            android:textAlignment="center"
            android:layout_marginBottom="24dp" />

        <LinearLayout
            android:id="@+id/checklistContainer"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:layout_marginBottom="24dp" />

        <View
            android:layout_width="match_parent"
            android:layout_height="0dp"
            android:layout_weight="1"
            android:minHeight="16dp" />

        <Button
            android:id="@+id/startButton"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/wizard_complete_start"
            android:textSize="16sp"
            android:paddingVertical="14dp" />

    </LinearLayout>
</ScrollView>
```

- [ ] **Step 2: Create CompletionFragment.kt**

```kotlin
package com.a42r.pevplugshare.ui.wizard.steps

import android.os.Bundle
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_wizard_completion, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val checklistContainer = view.findViewById<LinearLayout>(R.id.checklistContainer)

        val items = mutableListOf(
            "OsmAnd installed" to true,
            "Logged into OsmAnd Cloud" to true,
            "Favourites folder created" to true,
            "Synced to OsmAnd Cloud" to true,
            "OsmAnd account connected" to true,
            "Folder verified: \"${viewModel.selectedCategory.value ?: "—"}\"" to true
        )

        if (viewModel.skippedAi) {
            items.add("OpenRouter AI — skipped (set up later in Settings)" to false)
        } else {
            items.add("OpenRouter AI connected" to true)
            val model = viewModel.selectedModel.value
            if (model != null) {
                items.add("Model: ${model.name}" to true)
            }
        }

        checklistContainer.removeAllViews()
        for ((text, completed) in items) {
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 8, 0, 8)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val icon = TextView(requireContext()).apply {
                text = if (completed) "✓" else "○"
                setTextColor(if (completed) 0xFF4ade80.toInt() else 0xFFfbbf24.toInt())
                textSize = 16f
                setPadding(0, 0, 24, 0)
            }

            val label = TextView(requireContext()).apply {
                this.text = text
                textSize = 13f
            }

            row.addView(icon)
            row.addView(label)
            checklistContainer.addView(row)
        }

        view.findViewById<Button>(R.id.startButton).setOnClickListener {
            (activity as? SetupWizardActivity)?.completeWizard()
        }
    }
}
```

- [ ] **Step 3: Verify it compiles**

Run: `cd /home/philg/src/AndroidStudioProjects/PEVPlugShare && ./gradlew compileDebugKotlin 2>&1 | tail -3`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/a42r/pevplugshare/ui/wizard/steps/CompletionFragment.kt \
       app/src/main/res/layout/fragment_wizard_completion.xml
git commit -m "feat: add wizard Step 9 — completion summary with checklist"
```

---

### Task 9: Wire Up SplashActivity

**Files:**
- Modify: `app/src/main/java/com/a42r/pevplugshare/ui/auth/SplashActivity.kt`

- [ ] **Step 1: Add first-run check to SplashActivity**

At the top of the `onCreate` method, after the splash delay logic and before the session cookie check, add the first-run routing. The exact insertion point is inside the `lifecycleScope.launch` block, after the 1000ms delay but before `viewModel.hasSessionCookie()`.

Add this import at the top of the file:

```kotlin
import com.a42r.pevplugshare.ui.wizard.SetupWizardActivity
```

Then, after the splash delay (`delay(1000)` or similar) and before the session validation logic, add:

```kotlin
// First-run wizard check
if (SetupWizardActivity.isFirstRun(this@SplashActivity)) {
    startActivity(Intent(this@SplashActivity, SetupWizardActivity::class.java))
    finish()
    return@launch
}
```

This intercepts the flow before any session/credential checks, routing first-time users directly to the wizard.

- [ ] **Step 2: Verify it compiles**

Run: `cd /home/philg/src/AndroidStudioProjects/PEVPlugShare && ./gradlew compileDebugKotlin 2>&1 | tail -3`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/a42r/pevplugshare/ui/auth/SplashActivity.kt
git commit -m "feat: route first-run users to setup wizard from SplashActivity"
```

---

### Task 10: End-to-End Testing

**Files:** None (manual testing)

- [ ] **Step 1: Build and install**

Run: `cd /home/philg/src/AndroidStudioProjects/PEVPlugShare && ./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Test first-run detection**

Clear app data or fresh install → verify wizard appears.
Verify "Skip Wizard" on Step 1 → goes to LoginActivity.

- [ ] **Step 3: Test Steps 1-5 (instructional)**

Walk through all instructional steps. Verify:
- Progress bar updates on each step
- Checkboxes enable "Next" buttons
- Back navigation works
- OsmAnd detection auto-checks on Step 2

- [ ] **Step 4: Test Step 6 with invalid credentials**

Enter wrong credentials → verify error message shown inline.
Enter blank fields → verify field validation.

- [ ] **Step 5: Test Step 6 with blank account**

Use `osmand-testing@a42r.com` / `G4pKoFg2`.
Verify: login succeeds → auto-advances to Step 7 → shows "No Folders Found" with remediation instructions.

- [ ] **Step 6: Test Step 7 with synced account**

Use `osmand-testing2@a42r.com` / `SpjZ17R8`.
Verify: login succeeds → Step 7 shows "PEV Charges" folder → select it → continue.

- [ ] **Step 7: Test Step 8 (skip AI)**

Click "Skip — I'll Add Manually" → verify Step 9 shows AI as skipped (yellow).

- [ ] **Step 8: Test Step 8 (connect OpenRouter)**

Click "Connect OpenRouter" → verify OAuth flow launches.
After OAuth → verify model selection screen shows.
Select a model → verify Step 9 shows AI connected + model name.

- [ ] **Step 9: Test completion**

Click "Start Using PEVPlugShare" → verify lands in MainActivity.
Kill and relaunch app → verify wizard does NOT appear again.

- [ ] **Step 10: Commit final state**

```bash
git add -A
git commit -m "feat: complete setup wizard implementation — 9-step first-run onboarding"
```
