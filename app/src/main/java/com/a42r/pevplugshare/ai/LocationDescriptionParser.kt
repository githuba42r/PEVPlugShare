package com.a42r.pevplugshare.ai

import android.util.Log
import com.a42r.pevplugshare.data.remote.TokenManager
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
    val isPublic: Boolean?,
    val additionalInfo: String? = null
)

@Singleton
class LocationDescriptionParser @Inject constructor(
    private val tokenManager: TokenManager
) {
    private val okHttpClient = OkHttpClient()

    companion object {
        private const val TAG = "LocationDescriptionParser"

        private const val SYSTEM_PROMPT = """Extract charging location data from descriptions. Return ONLY valid JSON with no markdown, no backticks, no extra text. Response MUST start with { and end with }.

JSON: {"openingHours": "string or null", "chargingPoints": "number or null", "capacity": "string or null", "isFree": "boolean", "isPublic": "boolean or null", "additionalInfo": "string or null"}

SOCKET COUNTING: "2x10A"=2 sockets, "3 GPO"=3, "dual"=2, "6-12 total"=6, "multiple"=null. Don't confuse capacity (10A) with count.

OPENING HOURS: Extract if stated. "not suitable for night"→"9AM-5PM (Weekdays)". No mention + no restrictions→null.

ACCESS: "food court", "public"→true. "customers only", "members only"→false. Unclear→null.

COST: No mention="free" (isFree:true). "$", "paid", "fee"→isFree:false.

ADDITIONAL INFO: Extract any other relevant information not covered by above fields. Examples: location details (e.g., "near parking area", "behind building"), conditions (e.g., "may be turned off", "sometimes unavailable"), access instructions (e.g., "ask staff", "key required"), or other useful details for users. Keep to 1-2 sentences max.

EXAMPLES:
"2x10A GPO behind building, may be turned off, ask staff" → {"openingHours": null, "chargingPoints": 2, "capacity": "10A", "isFree": true, "isPublic": false, "additionalInfo": "May be turned off during day, ask staff for access"}
"3 GPO in food court near parking, 24/7" → {"openingHours": "24/7", "chargingPoints": 3, "capacity": null, "isFree": true, "isPublic": true, "additionalInfo": "Located in food court near parking area"}
"Single 15A outlet, customers only, $5/hr" → {"openingHours": null, "chargingPoints": 1, "capacity": "15A", "isFree": false, "isPublic": false, "additionalInfo": null}"""

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
            put("max_tokens", 2000) // Increased from 500 to ensure complete response
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
            Log.d(TAG, "  - additionalInfo: ${data.optString("additionalInfo")}")

            return ParsedLocationData(
                openingHours = data.optString("openingHours").takeIf { it.isNotEmpty() && it != "null" },
                chargingPoints = if (data.has("chargingPoints") && !data.isNull("chargingPoints"))
                    data.getInt("chargingPoints") else null,
                capacity = data.optString("capacity").takeIf { it.isNotEmpty() && it != "null" },
                isFree = data.optBoolean("isFree", true),
                hasFee = !data.optBoolean("isFree", true),
                isPublic = if (data.has("isPublic") && !data.isNull("isPublic"))
                    data.getBoolean("isPublic") else null,
                additionalInfo = data.optString("additionalInfo").takeIf { it.isNotEmpty() && it != "null" }
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
