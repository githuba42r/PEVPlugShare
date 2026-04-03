package com.a42r.pevplugshare.ui.suggestions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a42r.pevplugshare.data.api.ServerChangeSuggestion
import com.a42r.pevplugshare.data.api.SuggestionResponseBody
import com.a42r.pevplugshare.data.remote.ServerApiClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IncomingSuggestionsViewModel @Inject constructor(
    private val serverApiClient: ServerApiClient
) : ViewModel() {

    private val _suggestions = MutableStateFlow<List<ServerChangeSuggestion>>(emptyList())
    val suggestions: StateFlow<List<ServerChangeSuggestion>> = _suggestions.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val service = serverApiClient.getService()
                val resp = service.getIncomingSuggestions()
                if (resp.isSuccessful) {
                    _suggestions.value = resp.body()?.data ?: emptyList()
                } else {
                    _error.value = "Failed to load: ${resp.code()}"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load suggestions"
            } finally {
                _loading.value = false
            }
        }
    }

    fun accept(id: String, responseMsg: String? = null) {
        viewModelScope.launch {
            try {
                val service = serverApiClient.getService()
                val resp = service.acceptSuggestion(id, SuggestionResponseBody(responseMsg = responseMsg))
                if (resp.isSuccessful) {
                    load()
                } else {
                    _error.value = "Failed to accept: ${resp.code()}"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to accept suggestion"
            }
        }
    }

    fun reject(id: String, responseMsg: String? = null) {
        viewModelScope.launch {
            try {
                val service = serverApiClient.getService()
                val resp = service.rejectSuggestion(id, SuggestionResponseBody(responseMsg = responseMsg))
                if (resp.isSuccessful) {
                    load()
                } else {
                    _error.value = "Failed to reject: ${resp.code()}"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to reject suggestion"
            }
        }
    }
}
