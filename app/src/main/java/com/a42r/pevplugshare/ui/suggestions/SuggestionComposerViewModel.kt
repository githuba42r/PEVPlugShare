package com.a42r.pevplugshare.ui.suggestions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a42r.pevplugshare.data.api.ServerChangeSuggestion
import com.a42r.pevplugshare.data.api.ServerLocation
import com.a42r.pevplugshare.data.api.SubmitSuggestionBody
import com.a42r.pevplugshare.data.remote.ServerApiClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class SuggestionComposerViewModel @Inject constructor(
    private val serverApiClient: ServerApiClient
) : ViewModel() {

    private val _location = MutableStateFlow<ServerLocation?>(null)
    val location: StateFlow<ServerLocation?> = _location.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _submitted = MutableStateFlow<ServerChangeSuggestion?>(null)
    val submitted: StateFlow<ServerChangeSuggestion?> = _submitted.asStateFlow()

    fun loadLocation(locationId: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val service = serverApiClient.getService()
                val resp = service.getLocation(locationId)
                if (resp.isSuccessful) {
                    _location.value = resp.body()
                } else {
                    _error.value = "Failed to load location: ${resp.code()}"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load location"
            } finally {
                _loading.value = false
            }
        }
    }

    fun submitSuggestion(
        locationId: String,
        newName: String,
        newDescription: String,
        newOpeningHours: String,
        newSocketTypes: String,
        newHasFee: String,
        newChargingCosts: String,
        newPublicPrivate: String,
        message: String
    ) {
        val currentLocation = _location.value ?: return

        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val changes = JSONObject()

                if (newName.isNotBlank() && newName != currentLocation.name) {
                    changes.put("name", newName)
                }
                if (newDescription.isNotBlank() && newDescription != (currentLocation.description ?: "")) {
                    changes.put("description", newDescription)
                }
                if (newOpeningHours.isNotBlank() && newOpeningHours != (currentLocation.openingHours ?: "")) {
                    changes.put("openingHours", newOpeningHours)
                }
                if (newSocketTypes.isNotBlank() && newSocketTypes != (currentLocation.socketTypes ?: "")) {
                    changes.put("socketTypes", newSocketTypes)
                }
                if (newHasFee.isNotBlank()) {
                    val hasFee = newHasFee.lowercase() == "true" || newHasFee == "1"
                    if (hasFee != (currentLocation.hasFee ?: false)) {
                        changes.put("hasFee", hasFee)
                    }
                }
                if (newChargingCosts.isNotBlank() && newChargingCosts != (currentLocation.chargingCosts ?: "")) {
                    changes.put("chargingCosts", newChargingCosts)
                }
                if (newPublicPrivate.isNotBlank() && newPublicPrivate != (currentLocation.publicPrivate ?: "")) {
                    changes.put("publicPrivate", newPublicPrivate)
                }

                if (changes.length() == 0) {
                    _error.value = "No changes to suggest"
                    _loading.value = false
                    return@launch
                }

                val service = serverApiClient.getService()
                val body = SubmitSuggestionBody(
                    changesJson = changes.toString(),
                    message = message.takeIf { it.isNotBlank() }
                )
                val resp = service.submitSuggestion(locationId, body)
                if (resp.isSuccessful) {
                    _submitted.value = resp.body()
                } else {
                    _error.value = "Failed to submit suggestion: ${resp.code()}"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to submit suggestion"
            } finally {
                _loading.value = false
            }
        }
    }

    fun clearSubmitted() {
        _submitted.value = null
    }
}
