package com.a42r.pevplugshare.ai

import android.util.Log
import com.a42r.pevplugshare.data.model.Location
import com.a42r.pevplugshare.data.model.LocationAccessType
import com.a42r.pevplugshare.data.remote.TokenManager
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

[Brief contextual description of the location - 1-2 sentences. If a Description is provided, use it as the base and enhance it with charging details. If no Description, create one from the provided details.]

Details:
* Public Access: [allowed/restricted]
* Sockets: [number and type of sockets, OR "at least 1 charging point is available" if number is 0 or unknown]
* Available: [opening hours]
* Cost: [Free/Paid]
* Additional Information: [any additional details about the location]
* Type: PEV Charging Location

IMPORTANT RULES:
1. Start with a brief, contextual description (1-2 sentences) describing the physical location or key features. If a Description is provided in the input, use it as the foundation and integrate the charging details naturally.
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
* Additional Information: Located in public park area with nearby amenities
* Type: PEV Charging Location"

Another example:
"Multiple charging points in visitor parking area. Undercover parking available with good lighting for evening use.

Details:
* Public Access: allowed
* Sockets: 4 Type 2 EV chargers (7kW each)
* Available: 9am-6pm Mon-Fri
* Cost: Paid (app required)
* Additional Information: Requires parking permit during business hours
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
Description: %s

Follow the EXACT format from the system prompt with:
1. Brief contextual description (1-2 sentences). If a Description is provided, use it as the foundation for the opening description and integrate charging details naturally.
2. Blank line
3. "Details:" header
4. Bullet points for all charging details including Additional Information
5. If charging points is 0, use "at least 1 [socket type] is available"
6. Include Additional Information in the Details section with the label "Additional Information:"

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
        description: String? = null,
        conversationHistory: List<Pair<String, String>> = emptyList()
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val apiKey = tokenManager.getOpenRouterApiKey()
                if (apiKey.isNullOrEmpty()) {
                    Log.w(TAG, "No OpenRouter API key configured, falling back to programmatic generation")
                    return@withContext Result.success(generateProgrammaticDescription(
                        locationName, accessType, hasFee, chargingPoints, socketTypes,
                        openingHours, capacity, additionalInfo, description
                    ))
                }

                val modelId = tokenManager.getSelectedAiModel()
                if (modelId.isNullOrEmpty()) {
                    Log.w(TAG, "No AI model selected, falling back to programmatic generation")
                    return@withContext Result.success(generateProgrammaticDescription(
                        locationName, accessType, hasFee, chargingPoints, socketTypes,
                        openingHours, capacity, additionalInfo, description
                    ))
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
                    description,
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

                Log.d(TAG, "")
                Log.d(TAG, "╔════════════════════════════════════════════════════════════════════════════════╗")
                Log.d(TAG, "║                    GENERATING LOCATION DESCRIPTION                          ║")
                Log.d(TAG, "╚════════════════════════════════════════════════════════════════════════════════╝")
                Log.d(TAG, "Generating description for location: $locationName")
                Log.d(TAG, "Using model: $modelId")
                Log.d(TAG, "")
                Log.d(TAG, "INPUT PARAMETERS:")
                Log.d(TAG, "  - Location Name: $locationName")
                Log.d(TAG, "  - Access Type: $accessType")
                Log.d(TAG, "  - Cost: ${if (hasFee) "Paid" else "Free"}")
                Log.d(TAG, "  - Charging Points: $chargingPoints")
                Log.d(TAG, "  - Socket Types: $socketTypes")
                Log.d(TAG, "  - Opening Hours: $openingHours")
                Log.d(TAG, "  - Capacity: $capacity")
                Log.d(TAG, "  - Additional Info: $additionalInfo")
                Log.d(TAG, "")

                val response = okHttpClient.newCall(request).execute()
                Log.d(TAG, "Response Code: ${response.code}")
                Log.d(TAG, "Response Success: ${response.isSuccessful}")

                if (!response.isSuccessful) {
                    val errorBody = response.body?.string()
                    Log.w(TAG, "AI API request failed: ${response.code} - $errorBody")
                    Log.w(TAG, "Falling back to programmatic description generation")
                    return@withContext Result.success(generateProgrammaticDescription(
                        locationName, accessType, hasFee, chargingPoints, socketTypes,
                        openingHours, capacity, additionalInfo, description
                    ))
                }

                val responseBody = response.body?.string()
                    ?: run {
                        Log.w(TAG, "Empty API response, falling back to programmatic description generation")
                        return@withContext Result.success(generateProgrammaticDescription(
                            locationName, accessType, hasFee, chargingPoints, socketTypes,
                            openingHours, capacity, additionalInfo, description
                        ))
                    }

                Log.d(TAG, "")
                Log.d(TAG, "╔════════════════════════════════════════════════════════════════════════════════╗")
                Log.d(TAG, "║                         RAW API RESPONSE                                     ║")
                Log.d(TAG, "╚════════════════════════════════════════════════════════════════════════════════╝")
                Log.d(TAG, responseBody)
                Log.d(TAG, "╚════════════════════════════════════════════════════════════════════════════════╝")
                Log.d(TAG, "")

                val generatedDescription = parseAiResponse(responseBody)

                Log.d(TAG, "")
                Log.d(TAG, "╔════════════════════════════════════════════════════════════════════════════════╗")
                Log.d(TAG, "║                    GENERATED DESCRIPTION RESULT                              ║")
                Log.d(TAG, "╚════════════════════════════════════════════════════════════════════════════════╝")
                Log.d(TAG, generatedDescription)
                Log.d(TAG, "╚════════════════════════════════════════════════════════════════════════════════╝")
                Log.d(TAG, "")

                Result.success(generatedDescription)
            } catch (e: Exception) {
                Log.w(TAG, "Error generating description with AI: ${e.message}, falling back to programmatic generation", e)
                return@withContext Result.success(generateProgrammaticDescription(
                    locationName, accessType, hasFee, chargingPoints, socketTypes,
                    openingHours, capacity, additionalInfo, description
                ))
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
        description: String?,
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
        val descriptionStr = description ?: "None"

        val userPrompt = String.format(
            USER_PROMPT_TEMPLATE,
            locationName,
            accessTypeStr,
            costStr,
            chargingPoints,
            socketTypesStr,
            hoursStr,
            capacityStr,
            additionalStr,
            descriptionStr
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

        Log.d(TAG, "")
        Log.d(TAG, "╔════════════════════════════════════════════════════════════════════════════════╗")
        Log.d(TAG, "║                       REQUEST BODY FOR DESCRIPTION                           ║")
        Log.d(TAG, "╚════════════════════════════════════════════════════════════════════════════════╝")
        Log.d(TAG, jsonString)
        Log.d(TAG, "╚════════════════════════════════════════════════════════════════════════════════╝")
        Log.d(TAG, "")

        return jsonString.toRequestBody("application/json".toMediaType())
    }

    private fun parseAiResponse(responseBody: String): String {
        Log.d(TAG, "")
        Log.d(TAG, "╔════════════════════════════════════════════════════════════════════════════════╗")
        Log.d(TAG, "║                    PARSING AI RESPONSE                                        ║")
        Log.d(TAG, "╚════════════════════════════════════════════════════════════════════════════════╝")

        val json = JSONObject(responseBody)

        Log.d(TAG, "Response structure keys: ${json.keys().asSequence().toList()}")

        val choices = json.getJSONArray("choices")
        Log.d(TAG, "Number of choices: ${choices.length()}")

        val firstChoice = choices.getJSONObject(0)
        val message = firstChoice.getJSONObject("message")
        val content = message.getString("content").trim()

        Log.d(TAG, "")
        Log.d(TAG, "Raw AI generated content (length: ${content.length} characters):")
        Log.d(TAG, content)
        Log.d(TAG, "")
        Log.d(TAG, "╔════════════════════════════════════════════════════════════════════════════════╗")
        Log.d(TAG, "║                      FINAL GENERATED DESCRIPTION                              ║")
        Log.d(TAG, "╚════════════════════════════════════════════════════════════════════════════════╝")
        Log.d(TAG, content)
        Log.d(TAG, "╚════════════════════════════════════════════════════════════════════════════════╝")
        Log.d(TAG, "")

        return content
    }

    private fun generateProgrammaticDescription(
        locationName: String,
        accessType: LocationAccessType,
        hasFee: Boolean,
        chargingPoints: Int,
        socketTypes: List<String>,
        openingHours: String?,
        capacity: String?,
        additionalInfo: String?,
        description: String?
    ): String {
        Log.d(TAG, "")
        Log.d(TAG, "╔════════════════════════════════════════════════════════════════════════════════╗")
        Log.d(TAG, "║                GENERATING PROGRAMMATIC DESCRIPTION (AI Fallback)             ║")
        Log.d(TAG, "╚════════════════════════════════════════════════════════════════════════════════╝")

        val accessStr = when (accessType) {
            LocationAccessType.PUBLIC -> "allowed"
            LocationAccessType.PRIVATE -> "restricted"
        }

        val costStr = if (hasFee) "Paid" else "Free"

        val socketsStr = when {
            chargingPoints == 0 || socketTypes.isEmpty() -> "at least 1 charging point is available"
            chargingPoints == 1 -> "1 ${socketTypes.joinToString(", ")}"
            else -> "$chargingPoints ${socketTypes.joinToString(", ")}"
        }

        val hoursStr = openingHours?.takeIf { it.isNotBlank() } ?: "24x7"
        val capacityStr = capacity?.takeIf { it.isNotBlank() } ?: "Standard capacity"

        val briefDescription = description?.takeIf { it.isNotBlank() }
            ?: "PEV charging location at $locationName."

        val additionalInfoStr = additionalInfo?.takeIf { it.isNotBlank() }
            ?.let { "* Additional Information: $it" }
            ?: ""

        val descriptionBuilder = StringBuilder()
        descriptionBuilder.append(briefDescription)
        descriptionBuilder.append("\n\nDetails:\n")
        descriptionBuilder.append("* Public Access: $accessStr\n")
        descriptionBuilder.append("* Sockets: $socketsStr\n")
        descriptionBuilder.append("* Available: $hoursStr\n")
        descriptionBuilder.append("* Cost: $costStr\n")

        if (additionalInfoStr.isNotEmpty()) {
            descriptionBuilder.append("$additionalInfoStr\n")
        }

        descriptionBuilder.append("* Type: PEV Charging Location")

        val result = descriptionBuilder.toString()

        Log.d(TAG, "")
        Log.d(TAG, "Programmatic Description Generated (length: ${result.length} characters):")
        Log.d(TAG, result)
        Log.d(TAG, "╚════════════════════════════════════════════════════════════════════════════════╝")
        Log.d(TAG, "")

        return result
    }
}
