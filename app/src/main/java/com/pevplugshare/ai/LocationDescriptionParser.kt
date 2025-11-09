package com.pevplugshare.ai

import android.util.Log
import com.pevplugshare.data.remote.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

data class ParsedLocationData(
    val openingHours: String?,
    val chargingPoints: Int?,
    val capacity: String?,
    val isFree: Boolean,
    val hasFee: Boolean,
    val isPublic: Boolean?
)

@Singleton
class LocationDescriptionParser @Inject constructor(
    private val tokenManager: TokenManager
) {
    private val okHttpClient = OkHttpClient()
    
    companion object {
        private const val TAG = "LocationDescriptionParser"
        
        private const val SYSTEM_PROMPT = """You are a specialized AI assistant that extracts structured information from charging location descriptions for electric vehicles and personal electric vehicles (PEVs).

Extract the following information from the description:
1. Opening Hours - time/availability information
2. Number of Charging Points/Sockets - count of available power outlets
3. Capacity - power rating (Amps, Watts, or kW)
4. Cost - whether free or paid
5. Access Type - public or private/restricted

CRITICAL PARSING RULES:

SOCKET COUNTING:
- Look for EXACT numbers: "2x10A", "3 GPO", "dual GPO" (=2), "multiple" (extract range if given)
- Patterns like "2x10A GPO" means 2 sockets at 10A each
- "6-12 in total" means uncertain count, use the LOW number (6)
- "dual" = 2, "triple" = 3, "multiple" without number = null
- DO NOT confuse capacity (10A) with socket count!
- Examples:
  * "2x10A GPO" → chargingPoints: 2, capacity: "10A"
  * "3 GPO 15A each" → chargingPoints: 3, capacity: "15A"
  * "dual GPO" → chargingPoints: 2
  * "6-12 in total" → chargingPoints: 6
  * "multiple GPO" with no number → chargingPoints: null

OPENING HOURS - BE CONSERVATIVE:
- ONLY extract if EXPLICITLY stated
- If description mentions "not suitable for night" or "turned off" → LIMITED HOURS, not 24/7
- "not suitable for night" → "Daylight Hours" or "9AM-5PM (Weekdays)"
- "may be turned off" → indicates unreliable, still extract best guess
- "Tree lights get plugged in" → indicates night usage restricted
- If NO hours mentioned AND no restrictions → return null (let system default to 24/7)
- If restrictions mentioned → estimate reasonable hours
- Examples:
  * "not suitable for night" → "9AM-5PM (Weekdays)"
  * "may be turned off during day" → "9AM-5PM (Weekdays)" (conservative)
  * "24/7" → "24/7"
  * No mention at all → null

ACCESS TYPE:
- "food court", "public area" → PUBLIC
- "customers only", "members only", "patrons only" → PRIVATE (false)
- In shopping centers, food courts, public spaces → PUBLIC
- Behind buildings, restricted areas → PRIVATE
- If unclear → null

COST:
- Assume FREE unless price/fee explicitly mentioned
- "$", "paid", "charge", "fee" → NOT FREE (isFree: false)
- "free", "no charge", "complimentary" → FREE (isFree: true)
- No mention → FREE (isFree: true)

CAPACITY EXTRACTION:
- Extract power rating with units: "10A", "15A", "3.6kW", "2.4kW"
- Look for: "10A", "15A each", "3.6kW", etc.
- If multiple capacities, list them: "10A/15A"

Return ONLY valid JSON:
{
  "openingHours": "string or null",
  "chargingPoints": number or null,
  "capacity": "string or null",
  "isFree": boolean,
  "isPublic": boolean or null
}

EXAMPLES:

Input: "In food court multiple dual GPO 6-12 in total multiple circuits, if some are not working try others"
Response: {"openingHours": null, "chargingPoints": 6, "capacity": null, "isFree": true, "isPublic": true}

Input: "2x10A GPO on post next to first tree on river bank. Tree lights get plugged into it so not suitable for night. may be turned off during the day. Other power points in the area"
Response: {"openingHours": "9AM-5PM (Weekdays)", "chargingPoints": 2, "capacity": "10A", "isFree": true, "isPublic": true}

Input: "Single 15A outlet behind building for customer use only. \$5 per hour."
Response: {"openingHours": null, "chargingPoints": 1, "capacity": "15A", "isFree": false, "isPublic": false}

Input: "3 GPO available 24/7 in parking area"
Response: {"openingHours": "24/7", "chargingPoints": 3, "capacity": null, "isFree": true, "isPublic": true}"""

        private const val USER_PROMPT_TEMPLATE = """Extract charging location information from this description:

"%s"

Return only the JSON response, no additional text."""
    }
    
    suspend fun parseDescription(description: String): Result<ParsedLocationData> {
        return withContext(Dispatchers.IO) {
            try {
                val apiKey = tokenManager.getOpenRouterApiKey()
                if (apiKey.isNullOrEmpty()) {
                    return@withContext Result.failure(Exception("No OpenRouter API key configured"))
                }
                
                val modelId = tokenManager.getSelectedAiModel()
                if (modelId.isNullOrEmpty()) {
                    return@withContext Result.failure(Exception("No AI model selected"))
                }
                
                val requestBody = buildRequestBody(modelId, description)
                val request = Request.Builder()
                    .url("https://openrouter.ai/api/v1/chat/completions")
                    .header("Authorization", "Bearer $apiKey")
                    .header("Content-Type", "application/json")
                    .post(requestBody)
                    .build()
                
                Log.d(TAG, "Sending request to OpenRouter AI for description parsing")
                
                val response = okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string()
                    Log.e(TAG, "API request failed: ${response.code} - $errorBody")
                    return@withContext Result.failure(Exception("API request failed: ${response.code}"))
                }
                
                val responseBody = response.body?.string()
                    ?: return@withContext Result.failure(Exception("Empty response"))
                
                Log.d(TAG, "Received AI response: $responseBody")
                
                val parsedData = parseAiResponse(responseBody)
                Result.success(parsedData)
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing description with AI", e)
                Result.failure(e)
            }
        }
    }
    
    private fun buildRequestBody(modelId: String, description: String): okhttp3.RequestBody {
        val json = JSONObject().apply {
            put("model", modelId)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", SYSTEM_PROMPT)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", String.format(USER_PROMPT_TEMPLATE, description))
                })
            })
            put("temperature", 0.1) // Low temperature for consistent extraction
            put("max_tokens", 500) // Small response
        }
        
        return json.toString().toRequestBody("application/json".toMediaType())
    }
    
    private fun parseAiResponse(responseBody: String): ParsedLocationData {
        val json = JSONObject(responseBody)
        val choices = json.getJSONArray("choices")
        val firstChoice = choices.getJSONObject(0)
        val message = firstChoice.getJSONObject("message")
        val content = message.getString("content")
        
        Log.d(TAG, "AI content: $content")
        
        // Extract JSON from response (might be wrapped in markdown code blocks)
        val jsonContent = extractJson(content)
        val data = JSONObject(jsonContent)
        
        return ParsedLocationData(
            openingHours = data.optString("openingHours").takeIf { it.isNotEmpty() && it != "null" },
            chargingPoints = if (data.has("chargingPoints") && !data.isNull("chargingPoints")) 
                data.getInt("chargingPoints") else null,
            capacity = data.optString("capacity").takeIf { it.isNotEmpty() && it != "null" },
            isFree = data.optBoolean("isFree", true),
            hasFee = !data.optBoolean("isFree", true),
            isPublic = if (data.has("isPublic") && !data.isNull("isPublic")) 
                data.getBoolean("isPublic") else null
        )
    }
    
    private fun extractJson(content: String): String {
        // Remove markdown code blocks if present
        val trimmed = content.trim()
        return when {
            trimmed.startsWith("```json") -> {
                trimmed.substringAfter("```json").substringBefore("```").trim()
            }
            trimmed.startsWith("```") -> {
                trimmed.substringAfter("```").substringBefore("```").trim()
            }
            else -> trimmed
        }
    }
    
    fun calculateDescriptionHash(description: String?): String {
        if (description.isNullOrEmpty()) return ""
        
        val bytes = MessageDigest.getInstance("SHA-1")
            .digest(description.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
    
    fun hasDescriptionChanged(oldHash: String?, newDescription: String?): Boolean {
        val newHash = calculateDescriptionHash(newDescription)
        return oldHash != newHash
    }
}
