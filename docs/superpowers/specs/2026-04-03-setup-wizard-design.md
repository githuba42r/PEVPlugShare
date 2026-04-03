# PEVPlugShare Setup Wizard — Design Spec

**Date:** 2026-04-03
**Status:** Approved
**Branch:** fix/new-user-setup-erfactor

## Overview

A 9-step wizard that guides first-time users through the complete setup of PEVPlugShare, covering OsmAnd installation, account creation, favourites folder setup, cloud sync, account connection, folder verification, and optional OpenRouter AI configuration with model selection.

PEV = **Personal Electric Vehicle**, also known as a PMD (Personal Mobility Device) — e-scooters, e-bikes, electric skateboards, and other light electric vehicles.

## Design Decisions

- **Single Activity with ViewPager2** — swipeable steps with a segmented progress bar
- **First-run only** — tracked via SharedPreferences; wizard never shown again after completion or skip
- **Skip at any time** — falls through to existing flow: Login → AiWelcome → Main
- **Wizard completion** — goes directly to Main (bypasses LoginActivity and AiWelcomeActivity since the wizard handles both)
- **Folder name is user's choice** — the wizard suggests example names but does not enforce a specific name
- **Reuses existing infrastructure** — LoginViewModel for OsmAnd auth, OAuthActivity/OpenRouterOAuthViewModel for AI setup, OsmandIntentHelper for install detection, ModelSelectionAdapter for model picking

## Navigation Flow

```
Splash → First run? → YES → Setup Wizard (9 steps) → Main
Splash → First run? → NO  → Login → Main
Wizard → SKIP             → Login → AiWelcome → Main
```

## Step-by-Step Specification

### Progress Indicator (all steps)

Every step displays at the top:
- Segmented progress bar: 9 segments (green = completed, blue = current, grey = remaining)
- Label: "Step X of 9 — Step Name"
- "Skip Wizard" link (right-aligned)
- Progress bar and label are shown on ALL screens including error/loading states

### Step 1: Welcome

**Purpose:** Introduce the app and outline the setup process.

**Content:**
- App description: PEVPlugShare helps find and share charging locations for Personal Electric Vehicles (PEVs) — also known as Personal Mobility Devices (PMDs) — such as e-scooters, e-bikes, electric skateboards, and other light electric vehicles
- Explains that the app uses OsmAnd for location storage and OpenRouter for optional AI features
- Numbered list of the setup process (Install OsmAnd, Create account & log in, Create favourites folder, Sync to cloud, Connect account, Verify folder, AI features & model selection, Review & finish)
- Note: "Already have OsmAnd set up? Skip straight to the login screen."

**Actions:**
- "Get Started" button → advances to Step 2
- "Skip Wizard" button → navigates to LoginActivity (existing flow)

### Step 2: Install OsmAnd

**Purpose:** Verify OsmAnd is installed on the device.

**Behaviour:**
- On entry, auto-detect OsmAnd using `OsmandIntentHelper.isOsmandInstalled()` (checks `net.osmand.plus` and `net.osmand` packages, then intent resolution fallbacks)
- Explains what OsmAnd is and why PEVPlugShare needs it

**States:**

| State | UI | Actions |
|-------|-----|---------|
| Detected | Green success box: "OsmAnd+ detected on this device". Checkbox auto-ticked. | "Next" button enabled |
| Not found | Red warning box: "OsmAnd is not installed". Explains both OsmAnd (free) and OsmAnd+ (paid) options. | "Open Play Store — OsmAnd" button (launches Play Store intent for `net.osmand`), "Open Play Store — OsmAnd+" button (launches for `net.osmand.plus`), "Re-check Installation" button (re-runs detection) |

**Checkbox:** "OsmAnd is installed" — auto-ticked when detected, manual tick disabled until detection succeeds.

### Step 3: Log into OsmAnd

**Purpose:** Guide user to create/log into an OsmAnd Cloud account within the OsmAnd app.

**Content:**
- Explains why a cloud account is needed (sync between OsmAnd and PEVPlugShare)
- Step-by-step instructions:
  1. Open OsmAnd on your device
  2. Tap the menu button
  3. Tap Settings
  4. Tap OsmAnd Cloud
  5. Sign in or Create account
- Info tip: "Remember the email and password you use here — you'll need the same credentials in Step 6 to connect PEVPlugShare."

**Checkbox:** "I have logged into OsmAnd Cloud" — user self-confirms.

**Actions:** "Next" button (enabled when checkbox ticked).

### Step 4: Create Favourites Folder

**Purpose:** Guide user to create a dedicated favourites folder in OsmAnd for PEV charging locations.

**Content:**
- Explains that PEVPlugShare needs a separate folder from the default "Favourites" to store charging locations without modifying personal favourites
- Step-by-step instructions:
  1. Tap My Places (star icon)
  2. Go to the Favourites tab
  3. Tap + to add a new favourite point
  4. In the folder/group field, tap to create a new folder
  5. Give it any name you'll recognise — e.g. "PEV Charging", "My Chargers", "PMD Charging Spots"
  6. Save the temporary favourite point (can delete it afterward)
- Info box: "Why a separate folder? PEVPlugShare reads and writes to this folder via the OsmAnd Cloud API. Using a dedicated folder ensures your personal OsmAnd favourites are never modified. You can name it whatever you like — you'll select it from a list after connecting your account."

**Checkbox:** "I have created a dedicated favourites folder" — user self-confirms.

**Actions:** "Next" button (enabled when checkbox ticked).

### Step 5: Sync to Cloud

**Purpose:** Guide user to sync their new folder to OsmAnd Cloud so PEVPlugShare can access it via the API.

**Content:**
- Explains that the folder only exists locally until synced
- Step-by-step instructions:
  1. Tap the menu button
  2. Tap Settings
  3. Tap OsmAnd Cloud
  4. Tap Sync now
  5. Wait for the green checkmark
- Info box: "Why sync now? PEVPlugShare connects to OsmAnd Cloud (not the local app) to read your folders. If the folder hasn't been synced, it won't appear when you connect your account."

**Checkbox:** "I have synced my favourites to OsmAnd Cloud" — user self-confirms.

**Actions:** "Next" button (enabled when checkbox ticked).

### Step 6: Connect OsmAnd Account

**Purpose:** Authenticate with OsmAnd Cloud using the user's credentials. This is the actual login step that replaces LoginActivity for wizard users.

**Content:**
- Explains to use the same credentials from Step 3
- Email input field
- Password input field
- "Remember me" checkbox (with same security warning dialog as existing LoginActivity — AES-256 encryption notice)

**Behaviour:**
- Reuses existing `LoginViewModel.loginWithCredentials()` for authentication
- On success: auto-advances to Step 7 (folder verification)
- On failure: shows error inline, does not navigate away

**States:**

| State | UI |
|-------|-----|
| Form | Email/password inputs, "Connect & Verify" button |
| Loading | Spinner: "Connecting to OsmAnd Cloud..." |
| Invalid credentials | Red error box: "Login failed — email or password is incorrect. Make sure you're using the same credentials from Step 3." Form re-shown with "Try Again" button |
| Network error | Red error box: "Connection failed — could not reach the OsmAnd Cloud server. Please check your internet connection." "Retry Connection" button |

### Step 7: Verify Favourites Folder

**Purpose:** Fetch the user's favourites categories from OsmAnd Cloud, exclude the default "Favourites" folder, and let the user select which folder to use for PEV charging locations.

**Behaviour:**
- Auto-triggered after successful login in Step 6
- Calls `LoginViewModel.getFavoritesCategories()` (existing method that hits the `list-files` endpoint and parses FAVOURITES files)
- Filters out the default "Favourites" category
- If one or more non-default folders exist: show selection list
- If no non-default folders exist: show remediation instructions

**States:**

| State | UI |
|-------|-----|
| Loading | Spinner: "Checking Your Folders..." |
| Folders found | Radio-select list of non-default folder names. Pre-select first folder. "Continue with Selected Folder" button |
| No folders found | Two instruction panels (A: create folder, B: sync to cloud) + yellow delay warning + "Retry — Check for Folders" button |
| API error | Red error box: "Failed to fetch favourites" + "Retry" button |

**No folders found — detail:**

Panel A: "You haven't created the folder yet"
- Repeats the folder creation instructions from Step 4
- Example names: "PEV Charging", "My Chargers"

Panel B: "You created the folder but haven't synced yet"
- Repeats the sync instructions from Step 5

Yellow warning box: "Sync delay: After syncing in OsmAnd, there can be a delay of 1–2 minutes before the folder appears in the OsmAnd Cloud API. If you've already created the folder and synced, please wait a moment and then tap 'Retry' below."

**Data flow:** The selected folder name is stored for use as the active favourites category throughout the app.

### Step 8: AI Features & Model Selection (Optional)

**Purpose:** Explain AI features, clarify they are optional, connect OpenRouter, and select an AI model.

**Sub-screens (sequential flow within this step):**

#### 8a: AI Overview

**Content:**
- Blue info box: "AI is not required. You can always create and manage PEV charging locations manually — entering all details yourself. The app works fully without AI features."
- Bullet list of AI capabilities: charger type identification, socket/plug detection, operating hours extraction, location detail extraction
- Green reassurance box: "You always review and refine the AI-generated information before submitting. Nothing is saved without your confirmation."
- Note: "Requires a free OpenRouter.ai account. You can set this up later from Settings at any time."

**Actions:**
- "Connect OpenRouter" → launches OAuth flow (reuses existing `OAuthActivity` / `OpenRouterOAuthViewModel`)
- "Skip — I'll Add Manually" → advances to Step 9 with AI marked as skipped

#### 8b: Model Selection (shown after successful OAuth)

**Content:**
- Explains why the model matters: accuracy of photo analysis, description quality, socket/signage detection
- **Free Models** section (green header):
  - Description: "Good for trying out AI features at no cost. May occasionally miss details or produce less polished descriptions."
  - Meta Llama 4 Scout — FREE — "Vision-capable, 512K context. Good baseline for photo analysis."
  - Google Gemini 2.0 Flash — FREE — "Fast responses, good vision. Recommended free option." (tagged: Recommended)
  - Qwen2.5 VL 72B — FREE — "Strong vision model, good detail extraction."
- **Paid Models** section (purple header):
  - Description: "More accurate analysis, richer descriptions, and better socket/signage detection. Typically a few cents per location created."
  - Anthropic Claude Sonnet 4 — ~$0.01/location — "Excellent vision and description quality. Best balance of cost and accuracy." (tagged: Best Value)
  - OpenAI GPT-4o — ~$0.01/location — "Strong all-round vision model with reliable analysis."
  - Google Gemini 2.5 Pro — ~$0.02/location — "Top-tier accuracy, especially for signage and text extraction."
- Note: "You can change your model at any time in Settings. Costs are approximate and depend on photo complexity."
- Radio-select list for model choice

**Actions:** "Continue with Selected Model" → advances to 8c

**Implementation:** Reuses existing `ModelSelectionAdapter` and `AiModel` data class. The recommended models are presented as a curated subset; the full model catalog remains accessible in Settings.

#### 8c: Connection Confirmed

**Content:**
- Green success box: "OpenRouter connected successfully"
- Summary: selected model name, approximate cost, masked API key
- Note: "You can change your model or disconnect OpenRouter at any time in Settings."

**Actions:** "Next" → advances to Step 9

#### 8d: OAuth Failed (error state)

**Content:**
- Red error box: "OpenRouter connection failed — the OAuth flow did not complete. This could be because the browser was closed or the authorisation was cancelled."

**Actions:**
- "Try Again" → re-launches OAuth
- "Skip for Now" → advances to Step 9 with AI marked as skipped

### Step 9: All Done

**Purpose:** Summary of completed setup with checklist.

**Content:**
- Celebration header: "You're All Set!"
- Checklist with green checkmarks for each completed item:
  - OsmAnd installed
  - Logged into OsmAnd Cloud
  - Favourites folder created
  - Synced to OsmAnd Cloud
  - OsmAnd account connected
  - Folder verified: "[selected folder name]"
  - OpenRouter AI connected / Model: [selected model] (or yellow circle: "skipped — set up later in Settings")

**Actions:** "Start Using PEVPlugShare" → sets first-run flag to false, navigates to MainActivity, finishes wizard activity.

## Architecture

### New Files

| File | Purpose |
|------|---------|
| `ui/wizard/SetupWizardActivity.kt` | Host activity with ViewPager2 + progress bar |
| `ui/wizard/SetupWizardViewModel.kt` | Shared ViewModel for wizard state across all steps |
| `ui/wizard/WizardStepFragment.kt` | Base fragment class for wizard steps |
| `ui/wizard/steps/WelcomeFragment.kt` | Step 1 |
| `ui/wizard/steps/InstallOsmandFragment.kt` | Step 2 |
| `ui/wizard/steps/LoginOsmandFragment.kt` | Step 3 |
| `ui/wizard/steps/CreateFolderFragment.kt` | Step 4 |
| `ui/wizard/steps/SyncCloudFragment.kt` | Step 5 |
| `ui/wizard/steps/ConnectAccountFragment.kt` | Step 6 |
| `ui/wizard/steps/VerifyFolderFragment.kt` | Step 7 |
| `ui/wizard/steps/AiFeaturesFragment.kt` | Step 8 (manages sub-screens internally) |
| `ui/wizard/steps/CompletionFragment.kt` | Step 9 |
| `res/layout/activity_setup_wizard.xml` | ViewPager2 + progress bar layout |
| `res/layout/fragment_wizard_*.xml` | Layout for each step (9 layouts + sub-screen layouts for step 8) |

### Modified Files

| File | Change |
|------|--------|
| `ui/auth/SplashActivity.kt` | Add first-run check before existing flow; route to SetupWizardActivity |
| `ui/auth/SplashViewModel.kt` | Add `isFirstRun()` method checking SharedPreferences |
| `AndroidManifest.xml` | Register SetupWizardActivity |

### Shared ViewModel State

`SetupWizardViewModel` holds:
- `currentStep: MutableStateFlow<Int>` — current step index (1-9)
- `stepCompletionState: Map<Int, Boolean>` — checkbox/completion state per step
- `osmandInstalled: MutableStateFlow<Boolean>` — auto-detected
- `loginState: MutableStateFlow<LoginState>` — reuses existing sealed class
- `availableCategories: MutableStateFlow<List<String>>` — fetched after login
- `selectedCategory: MutableStateFlow<String?>` — user's folder choice
- `openRouterConnected: MutableStateFlow<Boolean>`
- `selectedModel: MutableStateFlow<AiModel?>`
- `skippedAi: Boolean`

### Dependencies (injected via Hilt)

- `AuthRepository` — for OsmAnd login (existing)
- `TokenManager` — for credential storage (existing)
- `OsmandApiClient` — for fetching categories (existing)
- `OpenRouterOAuthViewModel` logic — for AI setup (existing, extracted as needed)

### First-Run Detection

```kotlin
// In SplashViewModel or shared preferences helper
private val PREFS_NAME = "setup_wizard_prefs"
private val KEY_WIZARD_COMPLETED = "wizard_completed"

fun isFirstRun(): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return !prefs.getBoolean(KEY_WIZARD_COMPLETED, false)
}

fun markWizardCompleted() {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putBoolean(KEY_WIZARD_COMPLETED, true).apply()
}
```

### ViewPager2 Configuration

- Swipe navigation disabled (steps are linear, controlled by Next/Back buttons)
- `setUserInputEnabled(false)` — prevents accidental swipes
- Programmatic page changes via `setCurrentItem(step, true)` with smooth scrolling
- Back button navigates to previous step (except Step 1 where back = exit)

## Edge Cases

| Scenario | Behaviour |
|----------|-----------|
| User kills app mid-wizard | Wizard restarts from Step 1 on next launch (no partial state persisted) |
| OsmAnd installed after Step 2 | "Re-check Installation" button re-runs detection |
| User skips at Step 6 (already logged in) | Skip goes to LoginActivity; existing auto-login handles if credentials stored |
| Cloud sync delay | Step 7 "No folders found" shows 1-2 min delay warning + Retry button |
| Only default Favourites exists | Step 7 treats this as "no folders found" — shows creation + sync instructions |
| OAuth cancelled/failed | Step 8 shows error with "Try Again" / "Skip for Now" options |
| User rotates device | ViewModel survives config changes; ViewPager2 restores position |

## Testing Plan

- Verify first-run detection routes to wizard, subsequent launches skip it
- Verify skip at each step routes to Login → AiWelcome → Main
- Verify OsmAnd detection works for both `net.osmand` and `net.osmand.plus`
- Verify Play Store intents launch correctly for both OsmAnd variants
- Verify login flow handles success, invalid credentials, and network errors
- Verify folder verification excludes default "Favourites" and shows correct remediation
- Verify retry after sync delay finds newly synced folders
- Verify OpenRouter OAuth flow and model selection persist correctly
- Verify completion sets first-run flag and navigates to Main
- Verify progress bar shows correct state on all screens including error states
