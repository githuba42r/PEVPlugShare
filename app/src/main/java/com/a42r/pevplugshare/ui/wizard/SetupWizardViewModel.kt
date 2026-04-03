package com.a42r.pevplugshare.ui.wizard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a42r.pevplugshare.data.remote.TokenManager
import com.a42r.pevplugshare.domain.repository.AuthRepository
import com.a42r.pevplugshare.ui.settings.model.AiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    object Success : FolderVerificationState()
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

    private val _currentStep = MutableStateFlow(0)
    val currentStep: StateFlow<Int> = _currentStep

    private val _stepCompleted = MutableStateFlow(BooleanArray(TOTAL_STEPS))
    val stepCompleted: StateFlow<BooleanArray> = _stepCompleted

    private val _osmandInstalled = MutableStateFlow(false)
    val osmandInstalled: StateFlow<Boolean> = _osmandInstalled

    private val _loginState = MutableStateFlow<WizardLoginState>(WizardLoginState.Idle)
    val loginState: StateFlow<WizardLoginState> = _loginState

    private val _folderState = MutableStateFlow<FolderVerificationState>(FolderVerificationState.Idle)
    val folderState: StateFlow<FolderVerificationState> = _folderState

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory

    private val _openRouterConnected = MutableStateFlow(false)
    val openRouterConnected: StateFlow<Boolean> = _openRouterConnected

    private val _selectedModel = MutableStateFlow<AiModel?>(null)
    val selectedModel: StateFlow<AiModel?> = _selectedModel

    private val _skippedAi = MutableStateFlow(false)
    val skippedAi: StateFlow<Boolean> = _skippedAi

    fun goToStep(step: Int) {
        if (step in 0 until TOTAL_STEPS) {
            _currentStep.value = step
        }
    }

    fun nextStep() {
        val next = _currentStep.value + 1
        if (next < TOTAL_STEPS) {
            _currentStep.value = next
        }
    }

    fun previousStep() {
        val prev = _currentStep.value - 1
        if (prev >= 0) {
            _currentStep.value = prev
        }
    }

    fun markStepCompleted(step: Int, completed: Boolean = true) {
        val arr = _stepCompleted.value.copyOf()
        if (step in arr.indices) {
            arr[step] = completed
            _stepCompleted.value = arr
        }
    }

    fun setOsmandInstalled(installed: Boolean) {
        _osmandInstalled.value = installed
    }

    fun loginToOsmand(username: String, password: String) {
        viewModelScope.launch {
            _loginState.value = WizardLoginState.Loading
            try {
                val result = authRepository.loginWithWebApi(username, password, rememberMe = true)
                result.onSuccess { userId ->
                    val verifyResult = authRepository.getFavoritesCategories()
                    if (verifyResult.isSuccess) {
                        val name = tokenManager.getUsername() ?: userId
                        tokenManager.setOsmandConnected(true, name)
                        _loginState.value = WizardLoginState.Success
                    } else {
                        _loginState.value = WizardLoginState.Error("Failed to verify session")
                    }
                }.onFailure { error ->
                    _loginState.value = WizardLoginState.Error(error.message ?: "Login failed")
                }
            } catch (e: Exception) {
                _loginState.value = WizardLoginState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun resetLoginState() {
        _loginState.value = WizardLoginState.Idle
    }

    fun verifyFolders() {
        viewModelScope.launch {
            _folderState.value = FolderVerificationState.Loading
            try {
                val result = authRepository.getFavoritesCategories()
                result.onSuccess { categories ->
                    if (categories.isNotEmpty()) {
                        _folderState.value = FolderVerificationState.Success
                    } else {
                        _folderState.value = FolderVerificationState.Error("No folders found. Please create a folder in OsmAnd first.")
                    }
                }.onFailure { error ->
                    _folderState.value = FolderVerificationState.Error(error.message ?: "Verification failed")
                }
            } catch (e: Exception) {
                _folderState.value = FolderVerificationState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
        tokenManager.saveSelectedCategory(category)
    }

    fun setOpenRouterConnected(connected: Boolean) {
        _openRouterConnected.value = connected
    }

    fun setSelectedModel(model: AiModel?) {
        _selectedModel.value = model
    }

    fun skipAi() {
        _skippedAi.value = true
    }
}
