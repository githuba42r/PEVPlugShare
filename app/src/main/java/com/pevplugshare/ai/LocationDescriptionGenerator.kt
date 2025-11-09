package com.pevplugshare.ai

import android.util.Log
import com.pevplugshare.data.model.Location
import com.pevplugshare.data.model.LocationAccessType
import com.pevplugshare.data.remote.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationDescriptionGenerator @Inject constructor(
    private val tokenManager: TokenManager
) {
    private val okHttpClient = OkHttpClient()
    
    companion object {
        private const val TAG = "LocationDescriptionGenerator"
        
        private const val SYSTEM_PROMPT = """You are a specialized AI assistant that generates standardized, informative descriptions for PEV (Personal Electric Vehicle) charging locations.

Your task is to create a clear, readable description that includes all available charging details in a consistent format.

The description MUST follow this EXACT format:

[Brief contextual description of the location - 1-2 sentences]

Details:
* Public Access: [allowed/restricted]
* Sockets: [number and type of sockets, OR "at least 1 charging point is available" if number is 0 or unknown]
* Available: [opening hours]
* Cost: [Free/Paid]
* Type: PEV Charging Location

IMPORTANT RULES:
1. Start with a brief, contextual description (1-2 sentences) describing the physical location or key features
2. Follow with "Details:" on a new line with a blank line before it
3. Use bullet points (asterisk + space) for each detail
4. If the number of charging points is 0 or unknown, use "at least 1 [socket type] is available"
5. Keep it informative but concise
6. Include any additional context in the opening description

Example output:
"Single GPO on Light Pole on corner of grassed area near The Wheel of Brisbane.

Details:
* Public Access: allowed
* Sockets: at least 1 AU GPO
* Available: 24x7
* Cost: Free
* Type: PEV Charging Location"

Another example:
"Multiple charging points in visitor parking area. Undercover parking available with good lighting for evening use.

Details:
* Public Access: allowed
* Sockets: 4 Type 2 EV chargers (7kW each)
* Available: 9am-6pm Mon-Fri
* Cost: Paid (app required)
* Type: PEV Charging Location"

Generate a description following this EXACT format."""

        private const val USER_PROMPT_TEMPLATE = """Generate a standardized charging location description with the following details:

Location Name: %s
Access Type: %s
Cost: %s
Number of Charging Points: %d
Socket Types: %s
Opening Hours: %s
Power Capacity: %s
Additional Information: %s

Follow the EXACT format from the system prompt with:
1. Brief contextual description (1-2 sentences)
2. Blank line
3. "Details:" header
4. Bullet points for all charging details
5. If charging points is 0, use "at least 1 [socket type] is available"

Return ONLY the formatted description, no additional commentary."""
    }
    
    suspend fun generateDescription(
        locationName: String,
        accessType: LocationAccessType,
        hasFee: Boolean,
        chargingPoints: Int,
        socketTypes: List<String>,
        openingHours: String?,
        capacity: String?,
        additionalInfo: String?,
        conversationHistory: List<Pair<String, String>> = emptyList()
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val apiKey = tokenManager.getOpenRouterApiKey()
                if (apiKey.isNullOrEmpty()) {
                    Log.e(TAG, "No OpenRouter API key configured")
                    return@withContext Result.failure(Exception("No OpenRouter API key configured. Please add an API key in Settings."))
                }
                
                val modelId = tokenManager.getSelectedAiModel()
                if (modelId.isNullOrEmpty()) {
                    Log.e(TAG, "No AI model selected")
                    return@withContext Result.failure(Exception("No AI model selected. Please select a model in Settings."))
                }
                
                Log.d(TAG, "Using model: $modelId")
                
                val requestBody = buildRequestBody(
                    modelId,
                    locationName,
                    accessType,
                    hasFee,
                    chargingPoints,
                    socketTypes,
                    openingHours,
                    capacity,
                    additionalInfo,
                    conversationHistory
                )
                
                val request = Request.Builder()
                    .url("https://openrouter.ai/api/v1/chat/completions")
                    .header("Authorization", "Bearer $apiKey")
                    .header("Content-Type", "application/json")
                    .header("HTTP-Referer", "https://pevplugshare.app")
                    .header("X-Title", "PEV PlugShare")
                    .post(requestBody)
                    .build()
                
                Log.d(TAG, "Generating description with OpenRouter AI for location: $locationName")
                Log.d(TAG, "API URL: https://openrouter.ai/api/v1/chat/completions")
                Log.d(TAG, "API Key (first 10 chars): ${apiKey.take(10)}...")
                
                val response = okHttpClient.newCall(request).execute()
                Log.d(TAG, "Response Code: ${response.code}")
                Log.d(TAG, "Response Success: ${response.isSuccessful}")
                
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string()
                    Log.e(TAG, "API request failed: ${response.code} - $errorBody")
                    val errorMessage = when (response.code) {
                        401 -> "Invalid API key. Please check your OpenRouter API key in Settings."
                        404 -> {
                            if (errorBody?.contains("data policy") == true || errorBody?.contains("Free model") == true) {
                                "Free model access not enabled. Please enable 'Allow free model publication' at https://openrouter.ai/settings/privacy"
                            } else {
                                "Model not found. The selected model may no longer be available. Please select a different model in Settings."
                            }
                        }
                        429 -> "Rate limit exceeded. Please try again later."
                        else -> "API request failed with code ${response.code}"
                    }
                    return@withContext Result.failure(Exception(errorMessage))
                }
                
                val responseBody = response.body?.string()
                    ?: return@withContext Result.failure(Exception("Empty response"))
                
                Log.d(TAG, "Received AI response")
                
                val generatedDescription = parseAiResponse(responseBody)
                Result.success(generatedDescription)
            } catch (e: Exception) {
                Log.e(TAG, "Error generating description with AI", e)
                Result.failure(e)
            }
        }
    }
    
    private fun buildRequestBody(
        modelId: String,
        locationName: String,
        accessType: LocationAccessType,
        hasFee: Boolean,
        chargingPoints: Int,
        socketTypes: List<String>,
        openingHours: String?,
        capacity: String?,
        additionalInfo: String?,
        conversationHistory: List<Pair<String, String>>
    ): okhttp3.RequestBody {
        val accessTypeStr = when (accessType) {
            LocationAccessType.PUBLIC -> "Public access"
            LocationAccessType.PRIVATE -> "Private access"
        }
        
        val costStr = if (hasFee) "Paid charging" else "Free charging"
        val socketTypesStr = if (socketTypes.isNotEmpty()) socketTypes.joinToString(", ") else "Standard outlets"
        val hoursStr = openingHours ?: "Hours not specified"
        val capacityStr = capacity ?: "Not specified"
        val additionalStr = additionalInfo ?: "None"
        
        val userPrompt = String.format(
            USER_PROMPT_TEMPLATE,
            locationName,
            accessTypeStr,
            costStr,
            chargingPoints,
            socketTypesStr,
            hoursStr,
            capacityStr,
            additionalStr
        )
        
        val json = JSONObject().apply {
            put("model", modelId)
            put("messages", JSONArray().apply {
                // System prompt
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", SYSTEM_PROMPT)
                })
                
                // Initial user request
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userPrompt)
                })
                
                // Add conversation history for iterative refinement
                conversationHistory.forEach { (userMsg, assistantMsg) ->
                    put(JSONObject().apply {
                        put("role", "assistant")
                        put("content", assistantMsg)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", userMsg)
                    })
                }
            })
            put("temperature", 0.7) // Balanced for readability and consistency
            put("max_tokens", 500)
        }
        
        val jsonString = json.toString()
        Log.d(TAG, "--- REQUEST BODY ---")
        Log.d(TAG, jsonString)
        Log.d(TAG, "--- END REQUEST BODY ---")
        
        return jsonString.toRequestBody("application/json".toMediaType())
    }
    
    private fun parseAiResponse(responseBody: String): String {
        Log.d(TAG, "--- AI RESPONSE ---")
        Log.d(TAG, responseBody)
        Log.d(TAG, "--- END AI RESPONSE ---")
        
        val json = JSONObject(responseBody)
        val choices = json.getJSONArray("choices")
        val firstChoice = choices.getJSONObject(0)
        val message = firstChoice.getJSONObject("message")
        val content = message.getString("content").trim()
        
        Log.d(TAG, "--- PARSED AI CONTENT ---")
        Log.d(TAG, content)
        Log.d(TAG, "--- END PARSED AI CONTENT ---")
        Log.d(TAG, "=== END AI DESCRIPTION GENERATION DEBUG ===")
        
        return content
    }
}
