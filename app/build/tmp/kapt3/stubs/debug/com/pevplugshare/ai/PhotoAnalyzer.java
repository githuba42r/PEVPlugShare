package com.pevplugshare.ai;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000L\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010$\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\u000b\n\u0002\b\u0006\b\u0007\u0018\u0000 \u001d2\u00020\u0001:\u0001\u001dB\u0019\b\u0007\u0012\b\b\u0001\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006JH\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u000b0\n2\f\u0010\f\u001a\b\u0012\u0004\u0012\u00020\u000e0\r2\u0006\u0010\u000f\u001a\u00020\u00102\u0014\b\u0002\u0010\u0011\u001a\u000e\u0012\u0004\u0012\u00020\u0010\u0012\u0004\u0012\u00020\u00130\u0012H\u0086@\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\u0004\b\u0014\u0010\u0015J\u0010\u0010\u0016\u001a\u00020\u00102\u0006\u0010\u0017\u001a\u00020\u0018H\u0002J\u0006\u0010\u0019\u001a\u00020\u0010J\u0006\u0010\u001a\u001a\u00020\u0018J\u0010\u0010\u001b\u001a\u00020\u000b2\u0006\u0010\u001c\u001a\u00020\u0010H\u0002R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u0082\u0002\u000b\n\u0002\b!\n\u0005\b\u00a1\u001e0\u0001\u00a8\u0006\u001e"}, d2 = {"Lcom/pevplugshare/ai/PhotoAnalyzer;", "", "context", "Landroid/content/Context;", "tokenManager", "Lcom/pevplugshare/data/remote/TokenManager;", "(Landroid/content/Context;Lcom/pevplugshare/data/remote/TokenManager;)V", "okHttpClient", "Lokhttp3/OkHttpClient;", "analyzePhotos", "Lkotlin/Result;", "Lcom/pevplugshare/ai/PhotoAnalysisResult;", "imageFiles", "", "Ljava/io/File;", "apiKey", "", "focusAreas", "", "Landroid/graphics/RectF;", "analyzePhotos-BWLJW6A", "(Ljava/util/List;Ljava/lang/String;Ljava/util/Map;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "buildAnalysisPrompt", "hasFocusAreas", "", "getAiConfigurationStatus", "isAiAnalysisAvailable", "parseAnalysisResponse", "jsonText", "Companion", "app_debug"})
public final class PhotoAnalyzer {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pevplugshare.data.remote.TokenManager tokenManager = null;
    @org.jetbrains.annotations.NotNull()
    private final okhttp3.OkHttpClient okHttpClient = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = "PhotoAnalyzer";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String API_URL = "https://openrouter.ai/api/v1/chat/completions";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String ANALYSIS_PROMPT = "You are an expert at analyzing images of electrical charging locations for electric vehicles and personal electric vehicles (PEVs).\n\nAnalyze the provided images and extract the following information:\n\n1. **Socket Count** - Count the TOTAL number of visible power outlets/sockets/GPOs\n   - Count each individual socket carefully\n   - If you see a dual outlet (duplex), count it as 2 sockets\n   - If uncertain, provide your best estimate\n\n2. **Socket Types** - Identify the types of power outlets visible:\n   - AU/NZ GPO (AS/NZS 3112) - Standard Australian 3-pin angled outlets\n   - Type 2 / Mennekes - EV charging connector (round with multiple pins)\n   - CCS2 / Combo - Combined charging system\n   - CHAdeMO - Japanese fast charging standard\n   - Type 1 / J1772 - North American EV connector\n   - Wall Socket / Standard outlet\n   - Caravan outlet\n   - Industrial outlet\n   - Other (describe)\n\n3. **Location Description** - Describe WHERE the sockets are located:\n   - \"On the wall next to the door\"\n   - \"On a post near the entrance\"\n   - \"Under the shelter on the left side\"\n   - \"Behind the building next to the air conditioning unit\"\n   - \"On the pillar in the car park\"\n   - Be specific about landmarks, orientation, and identifying features\n\n4. **Power Rating** - If visible on labels, signs, or the outlets themselves:\n   - Look for amperage: \"10A\", \"15A\", \"20A\", \"32A\"\n   - Look for voltage: \"240V\", \"415V\"\n   - Look for wattage/kilowatts: \"2.4kW\", \"7kW\", \"22kW\"\n   - Only report if clearly visible in the image\n\n5. **Warnings/Notes** - Important observations:\n   - \"Outlets appear weathered or damaged\"\n   - \"Located in exposed area, may be wet in rain\"\n   - \"Tree lights are plugged in\"\n   - \"Sign says \'Customers Only\'\"\n   - \"Appears to be turned off or covered\"\n   - \"Multiple circuits available\"\n\nIMPORTANT COUNTING RULES:\n- Count CAREFULLY - this is critical information\n- A \"dual GPO\" or \"duplex outlet\" = 2 sockets\n- A \"quad GPO\" or \"4-gang outlet\" = 4 sockets\n- If you see multiple outlet panels, count ALL sockets across ALL panels\n- If uncertain due to image quality, provide a range (e.g., \"4-6 sockets\")\n\nIMAGE ANALYSIS TIPS:\n- Look at ALL images provided\n- Zoom in mentally on outlet panels\n- Count individual socket openings\n- Check for outlets in different areas of the same location\n- Some outlets might be partially visible or at angles\n\n**IMPORTANT: Return ONLY valid JSON, no markdown code blocks, no extra text. Start with { and end with }**\n\nReturn your analysis in this JSON format:\n{\n  \"socketCount\": number or null,\n  \"socketTypes\": [\"type1\", \"type2\", ...],\n  \"locationDescription\": \"detailed location description\",\n  \"powerRating\": \"rating if visible\" or null,\n  \"warnings\": [\"warning1\", \"warning2\", ...]\n}\n\nExamples:\n\nImage showing 2 dual GPO outlets on a wall:\n{\n  \"socketCount\": 4,\n  \"socketTypes\": [\"AU/NZ GPO (AS/NZS 3112)\"],\n  \"locationDescription\": \"Two dual outlets mounted on the brick wall next to the entrance door\",\n  \"powerRating\": \"10A\",\n  \"warnings\": [\"Outlets appear exposed to weather\"]\n}\n\nImage showing Type 2 EV charger:\n{\n  \"socketCount\": 1,\n  \"socketTypes\": [\"Type 2 / Mennekes\"],\n  \"locationDescription\": \"Wall-mounted EV charger on the left side of the building next to parking bay 5\",\n  \"powerRating\": \"7kW\",\n  \"warnings\": []\n}\n\nImage showing multiple outlets in a food court:\n{\n  \"socketCount\": 8,\n  \"socketTypes\": [\"AU/NZ GPO (AS/NZS 3112)\"],\n  \"locationDescription\": \"Multiple dual GPO outlets on pillars throughout the food court seating area. Main cluster of 3 dual outlets (6 sockets) on the central pillar, additional outlets on side pillars\",\n  \"powerRating\": null,\n  \"warnings\": [\"Multiple circuits available\", \"Some outlets may be in use by food court equipment\"]\n}";
    @org.jetbrains.annotations.NotNull()
    public static final com.pevplugshare.ai.PhotoAnalyzer.Companion Companion = null;
    
    @javax.inject.Inject()
    public PhotoAnalyzer(@dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.remote.TokenManager tokenManager) {
        super();
    }
    
    private final java.lang.String buildAnalysisPrompt(boolean hasFocusAreas) {
        return null;
    }
    
    private final com.pevplugshare.ai.PhotoAnalysisResult parseAnalysisResponse(java.lang.String jsonText) {
        return null;
    }
    
    public final boolean isAiAnalysisAvailable() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getAiConfigurationStatus() {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0003\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0007"}, d2 = {"Lcom/pevplugshare/ai/PhotoAnalyzer$Companion;", "", "()V", "ANALYSIS_PROMPT", "", "API_URL", "TAG", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}