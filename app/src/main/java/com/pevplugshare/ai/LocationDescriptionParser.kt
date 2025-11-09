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
- "dual" = 2, "triple" = 3, "multiple" without number = 4
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
- If NO hours mentioned AND no restrictions → return null (let system default to 24/7)
- If restrictions mentioned → estimate reasonable hours
- Examples:
  * "not suitable for night" → "9AM-5PM (Weekdays)"
  * "may be turned off during day" → "9AM-5PM (Weekdays)" (conservative)
  * "24/7" → "24/7"
  * No mention at all - 24/7

ACCESS TYPE:
- "food court", "public area" → PUBLIC
- "customers only", "members only", "patrons only" → PRIVATE (false)
- In shopping centers, food courts, public spaces → PUBLIC
- Behind buildings, restricted areas → PRIVATE
- If unclear → PUBLIC

COST:
- Assume FREE unless price/fee explicitly mentioned
- "$", "paid", "charge", "fee" → NOT FREE (isFree: false)
- "free", "no charge", "complimentary" → FREE (isFree: true)
- No mention → FREE (isFree: true)

CAPACITY EXTRACTION:
- Extract power rating with units: "10A", "15A", "3.6kW", "2.4kW"
- Look for: "10A", "15A each", "3.6kW", etc.
- If multiple capacities, list them: "10A/15A"

CRITICAL INSTRUCTION - NO MARKDOWN:
Return ONLY the JSON object with NO markdown code blocks, NO backticks, NO explanatory text.
IMPORTANT: Do NOT wrap the JSON in ```json ``` or any markdown formatting.
IMPORTANT: Do NOT add any text before or after the JSON.
IMPORTANT: The response MUST start with { and end with } - nothing else.
IMPORTANT: This is critical for parsing - any markdown will break the system.

Return only valid JSON:
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
                Log.d(TAG, "Description to parse: $description")
                Log.d(TAG, "Using model: $modelId")

                val response = okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string()
                    Log.e(TAG, "API request failed: ${response.code} - $errorBody")
                    return@withContext Result.failure(Exception("API request failed: ${response.code}"))
                }

                val responseBody = response.body?.string()
                    ?: return@withContext Result.failure(Exception("Empty response"))

                Log.d(TAG, "========== RAW API RESPONSE ==========")
                Log.d(TAG, "Response length: ${responseBody.length} characters")
                Log.d(TAG, "Full raw response:\n$responseBody")
                Log.d(TAG, "======================================")

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
            put("max_tokens", 1000) // Increased from 500 to ensure complete response
        }

        return json.toString().toRequestBody("application/json".toMediaType())
    }

    private fun parseAiResponse(responseBody: String): ParsedLocationData {
        try {
            Log.d(TAG, "")
            Log.d(TAG, "╔════════════════════════════════════════════════════════════════════════════════╗")
            Log.d(TAG, "║                      COMPLETE API RESPONSE STRUCTURE                          ║")
            Log.d(TAG, "╚════════════════════════════════════════════════════════════════════════════════╝")
            Log.d(TAG, "Raw response body:")
            Log.d(TAG, responseBody)
            Log.d(TAG, "")
            Log.d(TAG, "Parsing structure...")
            
            val json = JSONObject(responseBody)
            
            Log.d(TAG, "")
            Log.d(TAG, "Top-level keys in response: ${json.keys().asSequence().toList()}")
            
            val choices = json.getJSONArray("choices")
            Log.d(TAG, "Number of choices: ${choices.length()}")
            
            val firstChoice = choices.getJSONObject(0)
            Log.d(TAG, "First choice keys: ${firstChoice.keys().asSequence().toList()}")
            Log.d(TAG, "First choice content (pretty-printed):")
            Log.d(TAG, firstChoice.toString(2))
            
            val message = firstChoice.getJSONObject("message")
            Log.d(TAG, "")
            Log.d(TAG, "Message object keys: ${message.keys().asSequence().toList()}")
            Log.d(TAG, "Message content (pretty-printed):")
            Log.d(TAG, message.toString(2))
            
            val content = message.getString("content")
            
            Log.d(TAG, "")
            Log.d(TAG, "╔════════════════════════════════════════════════════════════════════════════════╗")
            Log.d(TAG, "║                    RAW AI MODEL RESPONSE CONTENT (from message.content)        ║")
            Log.d(TAG, "╚════════════════════════════════════════════════════════════════════════════════╝")
            Log.d(TAG, "Content length: ${content.length} characters")
            Log.d(TAG, "---")
            Log.d(TAG, content)
            Log.d(TAG, "---")
            Log.d(TAG, "First 100 chars: ${content.take(100)}")
            Log.d(TAG, "Last 100 chars: ${content.takeLast(100)}")
            Log.d(TAG, "╚════════════════════════════════════════════════════════════════════════════════╝")
            Log.d(TAG, "")

            // Extract JSON from response (might be wrapped in markdown code blocks)
            val jsonContent = extractJson(content)
            Log.d(TAG, "")
            Log.d(TAG, "╔════════════════════════════════════════════════════════════════════════════════╗")
            Log.d(TAG, "║                 EXTRACTED JSON AFTER MARKDOWN REMOVAL                         ║")
            Log.d(TAG, "╚════════════════════════════════════════════════════════════════════════════════╝")
            Log.d(TAG, "Extracted length: ${jsonContent.length} characters")
            Log.d(TAG, "---")
            Log.d(TAG, jsonContent)
            Log.d(TAG, "---")
            Log.d(TAG, "Formatted JSON:")
            try {
                val jsonFormatted = JSONObject(jsonContent).toString(2)
                Log.d(TAG, jsonFormatted)
            } catch (e: Exception) {
                Log.d(TAG, "Could not format as JSON: ${e.message}")
            }
            Log.d(TAG, "╚════════════════════════════════════════════════════════════════════════════════╝")
            Log.d(TAG, "")

            val data = JSONObject(jsonContent)
            Log.d(TAG, "✓ Successfully parsed JSON")
            Log.d(TAG, "Parsed JSON keys: ${data.keys().asSequence().toList()}")
            Log.d(TAG, "Parsed data:")
            Log.d(TAG, "  - openingHours: ${data.optString("openingHours")}")
            Log.d(TAG, "  - chargingPoints: ${data.optString("chargingPoints")}")
            Log.d(TAG, "  - capacity: ${data.optString("capacity")}")
            Log.d(TAG, "  - isFree: ${data.optBoolean("isFree")}")
            Log.d(TAG, "  - isPublic: ${data.optString("isPublic")}")

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
        } catch (e: org.json.JSONException) {
            Log.e(TAG, "JSON parsing error: ${e.message}")
            Log.e(TAG, "Error occurred at: ${e.stackTrace.firstOrNull()}")
            throw Exception("Invalid response format from AI: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing AI response", e)
            throw Exception("Failed to parse AI response: ${e.message}")
        }
    }

    private fun extractJson(content: String): String {
        // Remove markdown code blocks if present and extract JSON
        val trimmed = content.trim()
        
        Log.d(TAG, "Markdown extraction: checking pattern...")

        // Try multiple markdown patterns
        val extracted = when {
            trimmed.startsWith("```json") -> {
                Log.d(TAG, "  ✓ Detected: ```json markdown block")
                trimmed.substringAfter("```json").substringBefore("```").trim()
            }
            trimmed.startsWith("```") -> {
                Log.d(TAG, "  ✓ Detected: ``` markdown block")
                trimmed.substringAfter("```").substringBefore("```").trim()
            }
            trimmed.startsWith("{") -> {
                Log.d(TAG, "  ✓ Detected: Direct JSON (no markdown)")
                // Already valid JSON, find the complete object
                var braceCount = 0
                var endIndex = -1
                for (i in trimmed.indices) {
                    when (trimmed[i]) {
                        '{' -> braceCount++
                        '}' -> {
                            braceCount--
                            if (braceCount == 0) {
                                endIndex = i + 1
                                break
                            }
                        }
                    }
                }
                if (endIndex > 0) trimmed.substring(0, endIndex) else trimmed
            }
            else -> {
                Log.d(TAG, "  ⚠ Unknown format: assuming as-is")
                trimmed
            }
        }

        Log.d(TAG, "  Extracted ${extracted.length} characters")
        
        // Check if JSON appears incomplete
        if (extracted.endsWith("\"") && !extracted.endsWith("}")) {
            Log.w(TAG, "  ⚠ WARNING: JSON appears to be incomplete (ends with unterminated string)")
            Log.w(TAG, "  Last 50 chars: ${extracted.takeLast(50)}")
        }
        
        return extracted
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
