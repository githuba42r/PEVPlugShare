package com.pevplugshare.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class PhotoAnalysisResult(
    val socketCount: Int?,
    val socketTypes: List<String>,
    val locationDescription: String?,
    val powerRating: String?,
    val warnings: List<String>
)

@Singleton
class PhotoAnalyzer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tokenManager: com.pevplugshare.data.remote.TokenManager
) {
    private val okHttpClient = OkHttpClient()
    
    companion object {
        private const val TAG = "PhotoAnalyzer"
        private const val API_URL = "https://openrouter.ai/api/v1/chat/completions"
        
        private const val ANALYSIS_PROMPT = """You are an expert at analyzing images of electrical charging locations for electric vehicles and personal electric vehicles (PEVs).

Analyze the provided images and extract the following information:

1. **Socket Count** - Count the TOTAL number of visible power outlets/sockets/GPOs
   - Count each individual socket carefully
   - If you see a dual outlet (duplex), count it as 2 sockets
   - If uncertain, provide your best estimate

2. **Socket Types** - Identify the types of power outlets visible:
   - AU/NZ GPO (AS/NZS 3112) - Standard Australian 3-pin angled outlets
   - Type 2 / Mennekes - EV charging connector (round with multiple pins)
   - CCS2 / Combo - Combined charging system
   - CHAdeMO - Japanese fast charging standard
   - Type 1 / J1772 - North American EV connector
   - Wall Socket / Standard outlet
   - Caravan outlet
   - Industrial outlet
   - Other (describe)

3. **Location Description** - Describe WHERE the sockets are located:
   - "On the wall next to the door"
   - "On a post near the entrance"
   - "Under the shelter on the left side"
   - "Behind the building next to the air conditioning unit"
   - "On the pillar in the car park"
   - Be specific about landmarks, orientation, and identifying features

4. **Power Rating** - If visible on labels, signs, or the outlets themselves:
   - Look for amperage: "10A", "15A", "20A", "32A"
   - Look for voltage: "240V", "415V"
   - Look for wattage/kilowatts: "2.4kW", "7kW", "22kW"
   - Only report if clearly visible in the image

5. **Warnings/Notes** - Important observations:
   - "Outlets appear weathered or damaged"
   - "Located in exposed area, may be wet in rain"
   - "Tree lights are plugged in"
   - "Sign says 'Customers Only'"
   - "Appears to be turned off or covered"
   - "Multiple circuits available"

IMPORTANT COUNTING RULES:
- Count CAREFULLY - this is critical information
- A "dual GPO" or "duplex outlet" = 2 sockets
- A "quad GPO" or "4-gang outlet" = 4 sockets
- If you see multiple outlet panels, count ALL sockets across ALL panels
- If uncertain due to image quality, provide a range (e.g., "4-6 sockets")

IMAGE ANALYSIS TIPS:
- Look at ALL images provided
- Zoom in mentally on outlet panels
- Count individual socket openings
- Check for outlets in different areas of the same location
- Some outlets might be partially visible or at angles

**IMPORTANT: Return ONLY valid JSON, no markdown code blocks, no extra text. Start with { and end with }**

Return your analysis in this JSON format:
{
  "socketCount": number or null,
  "socketTypes": ["type1", "type2", ...],
  "locationDescription": "detailed location description",
  "powerRating": "rating if visible" or null,
  "warnings": ["warning1", "warning2", ...]
}

Examples:

Image showing 2 dual GPO outlets on a wall:
{
  "socketCount": 4,
  "socketTypes": ["AU/NZ GPO (AS/NZS 3112)"],
  "locationDescription": "Two dual outlets mounted on the brick wall next to the entrance door",
  "powerRating": "10A",
  "warnings": ["Outlets appear exposed to weather"]
}

Image showing Type 2 EV charger:
{
  "socketCount": 1,
  "socketTypes": ["Type 2 / Mennekes"],
  "locationDescription": "Wall-mounted EV charger on the left side of the building next to parking bay 5",
  "powerRating": "7kW",
  "warnings": []
}

Image showing multiple outlets in a food court:
{
  "socketCount": 8,
  "socketTypes": ["AU/NZ GPO (AS/NZS 3112)"],
  "locationDescription": "Multiple dual GPO outlets on pillars throughout the food court seating area. Main cluster of 3 dual outlets (6 sockets) on the central pillar, additional outlets on side pillars",
  "powerRating": null,
  "warnings": ["Multiple circuits available", "Some outlets may be in use by food court equipment"]
}"""
    }

    suspend fun analyzePhotos(
        imageFiles: List<File>,
        apiKey: String
    ): Result<PhotoAnalysisResult> = withContext(Dispatchers.IO) {
        try {
            if (imageFiles.isEmpty()) {
                return@withContext Result.failure(Exception("No images to analyze"))
            }
            
            val modelId = tokenManager.getSelectedAiModel() ?: "google/gemini-flash-1.5"

            // Build message content with text and images
            val contentParts = JSONArray()
            
            // Add text prompt
            contentParts.put(JSONObject().apply {
                put("type", "text")
                put("text", ANALYSIS_PROMPT)
            })
            
            // Add images as base64
            for (file in imageFiles) {
                try {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                        ?: throw Exception("Failed to decode image: ${file.name}")
                    
                    // Resize if too large
                    val maxDimension = 768
                    val resizedBitmap = if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
                        val scale = maxDimension.toFloat() / maxOf(bitmap.width, bitmap.height)
                        Bitmap.createScaledBitmap(
                            bitmap,
                            (bitmap.width * scale).toInt(),
                            (bitmap.height * scale).toInt(),
                            true
                        )
                    } else {
                        bitmap
                    }
                    
                    // Convert to base64
                    val outputStream = ByteArrayOutputStream()
                    resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
                    val base64Image = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
                    
                    contentParts.put(JSONObject().apply {
                        put("type", "image_url")
                        put("image_url", JSONObject().apply {
                            put("url", "data:image/jpeg;base64,$base64Image")
                        })
                    })
                    
                    resizedBitmap.recycle()
                    bitmap.recycle()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to process image: ${file.name}", e)
                    throw Exception("Failed to process image ${file.name}: ${e.message}")
                }
            }

            // Build OpenRouter request
            val messages = JSONArray()
            messages.put(JSONObject().apply {
                put("role", "user")
                put("content", contentParts)
            })
            
            val requestBody = JSONObject().apply {
                put("model", modelId)
                put("messages", messages)
            }
            
            Log.d(TAG, "Analyzing ${imageFiles.size} images with model: $modelId")
            Log.d(TAG, "Request body: ${requestBody.toString(2)}")
            
            val request = Request.Builder()
                .url(API_URL)
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .header("HTTP-Referer", "https://pevplugshare.app")
                .header("X-Title", "PEV PlugShare")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw Exception("Empty response")
            
            Log.d(TAG, "API Response Code: ${response.code}")
            Log.d(TAG, "API Response Headers: ${response.headers}")
            Log.d(TAG, "API Response Body: $responseBody")
            
            if (!response.isSuccessful) {
                Log.e(TAG, "API error: ${response.code} - $responseBody")
                val errorMessage = try {
                    val errorJson = JSONObject(responseBody)
                    errorJson.optJSONObject("error")?.optString("message") ?: responseBody
                } catch (e: Exception) {
                    responseBody
                }
                throw Exception("API error ${response.code}: $errorMessage")
            }
            
            val jsonResponse = JSONObject(responseBody)
            val choices = jsonResponse.optJSONArray("choices")
            if (choices == null || choices.length() == 0) {
                Log.e(TAG, "No choices in response: $responseBody")
                throw Exception("No response from AI model")
            }
            
            val message = choices.getJSONObject(0).getJSONObject("message")
            val responseText = message.getString("content")
            
            Log.d(TAG, "AI Response: $responseText")
            
            // Parse JSON response
            val result = parseAnalysisResponse(responseText)
            
            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Photo analysis failed", e)
            Result.failure(e)
        }
    }

    private fun parseAnalysisResponse(jsonText: String): PhotoAnalysisResult {
        try {
            Log.d(TAG, "Raw response text: $jsonText")
            
            var jsonString = jsonText.trim()
            
            // Strip markdown code blocks if present (AI sometimes ignores instructions)
            if (jsonString.startsWith("```")) {
                Log.d(TAG, "Detected markdown code block, stripping...")
                jsonString = jsonString
                    .removePrefix("```json").removePrefix("```")
                    .removeSuffix("```")
                    .trim()
                Log.d(TAG, "After stripping markdown: $jsonString")
            }
            
            val json = org.json.JSONObject(jsonString)
            
            Log.d(TAG, "Successfully parsed JSON")
            
            val socketCount = if (json.has("socketCount") && !json.isNull("socketCount")) {
                json.getInt("socketCount")
            } else null
            
            val socketTypes = mutableListOf<String>()
            if (json.has("socketTypes")) {
                val typesArray = json.getJSONArray("socketTypes")
                for (i in 0 until typesArray.length()) {
                    socketTypes.add(typesArray.getString(i))
                }
            }
            
            val locationDescription = if (json.has("locationDescription") && !json.isNull("locationDescription")) {
                json.getString("locationDescription")
            } else null
            
            val powerRating = if (json.has("powerRating") && !json.isNull("powerRating")) {
                json.getString("powerRating")
            } else null
            
            val warnings = mutableListOf<String>()
            if (json.has("warnings")) {
                val warningsArray = json.getJSONArray("warnings")
                for (i in 0 until warningsArray.length()) {
                    warnings.add(warningsArray.getString(i))
                }
            }
            
            return PhotoAnalysisResult(
                socketCount = socketCount,
                socketTypes = socketTypes,
                locationDescription = locationDescription,
                powerRating = powerRating,
                warnings = warnings
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse analysis response", e)
            throw Exception("Failed to parse AI response: ${e.message}")
        }
    }
}
