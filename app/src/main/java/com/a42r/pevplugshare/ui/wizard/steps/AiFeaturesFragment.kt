package com.a42r.pevplugshare.ui.wizard.steps

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import android.widget.ViewSwitcher
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.a42r.pevplugshare.R
import com.a42r.pevplugshare.data.remote.TokenManager
import com.a42r.pevplugshare.ui.oauth.OAuthActivity
import com.a42r.pevplugshare.ui.settings.model.AiModel
import com.a42r.pevplugshare.ui.wizard.SetupWizardViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AiFeaturesFragment : Fragment() {

    private val viewModel: SetupWizardViewModel by activityViewModels()

    @Inject
    lateinit var tokenManager: TokenManager

    private val modelMap: Map<Int, AiModel> by lazy {
        mapOf(
            R.id.modelGeminiFlash to AiModel(
                id = "google/gemini-2.0-flash-exp",
                name = "Gemini 2.0 Flash",
                provider = "Google",
                cost = "Free"
            ),
            R.id.modelLlama to AiModel(
                id = "meta-llama/llama-4-scout:free",
                name = "Llama 4 Scout",
                provider = "Meta",
                cost = "Free"
            ),
            R.id.modelQwen to AiModel(
                id = "qwen/qwen2.5-vl-72b-instruct:free",
                name = "Qwen 2.5 VL 72B",
                provider = "Qwen",
                cost = "Free"
            ),
            R.id.modelClaudeSonnet to AiModel(
                id = "anthropic/claude-sonnet-4",
                name = "Claude Sonnet 4",
                provider = "Anthropic",
                cost = "Paid"
            ),
            R.id.modelGpt4o to AiModel(
                id = "openai/gpt-4o",
                name = "GPT-4o",
                provider = "OpenAI",
                cost = "Paid"
            ),
            R.id.modelGeminiPro to AiModel(
                id = "google/gemini-2.5-pro-preview",
                name = "Gemini 2.5 Pro",
                provider = "Google",
                cost = "Paid"
            )
        )
    }

    private val oauthLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        // OAuthActivity saves the API key to TokenManager directly; check if it was saved
        if (tokenManager.hasAiApiKeyConfigured()) {
            viewModel.setOpenRouterConnected(true)
            view?.findViewById<ViewSwitcher>(R.id.viewSwitcher)?.displayedChild = 1
        } else if (result.resultCode != android.app.Activity.RESULT_CANCELED) {
            Toast.makeText(
                requireContext(),
                "OpenRouter connection failed. You can try again or skip.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_wizard_ai_features, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val connectOpenRouterButton = view.findViewById<Button>(R.id.connectOpenRouterButton)
        val skipAiButton = view.findViewById<Button>(R.id.skipAiButton)
        val modelRadioGroup = view.findViewById<RadioGroup>(R.id.modelRadioGroup)
        val paidModelRadioGroup = view.findViewById<RadioGroup>(R.id.paidModelRadioGroup)
        val modelContinueButton = view.findViewById<Button>(R.id.modelContinueButton)

        connectOpenRouterButton.setOnClickListener {
            val intent = Intent(requireContext(), OAuthActivity::class.java).apply {
                putExtra("from_first_launch", true)
            }
            oauthLauncher.launch(intent)
        }

        skipAiButton.setOnClickListener {
            viewModel.skipAi()
            viewModel.markStepCompleted(7)
            viewModel.nextStep()
        }

        // Mutual exclusion between free and paid radio groups
        modelRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId != -1) {
                paidModelRadioGroup.clearCheck()
            }
        }

        paidModelRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId != -1) {
                modelRadioGroup.clearCheck()
            }
        }

        modelContinueButton.setOnClickListener {
            val selectedId = when {
                modelRadioGroup.checkedRadioButtonId != -1 -> modelRadioGroup.checkedRadioButtonId
                paidModelRadioGroup.checkedRadioButtonId != -1 -> paidModelRadioGroup.checkedRadioButtonId
                else -> -1
            }

            if (selectedId != -1) {
                val model = modelMap[selectedId]
                if (model != null) {
                    viewModel.setSelectedModel(model)
                    tokenManager.saveSelectedAiModel(model.id)
                }
                viewModel.setOpenRouterConnected(true)
                viewModel.markStepCompleted(7)
                viewModel.nextStep()
            }
        }
    }
}
