package com.pevplugshare.ai;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000>\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0005\n\u0002\u0010\u000b\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\b\u0007\u0018\u0000 \u001a2\u00020\u0001:\u0001\u001aB\u000f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0018\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\nH\u0002J\u0010\u0010\f\u001a\u00020\n2\b\u0010\u000b\u001a\u0004\u0018\u00010\nJ\u0010\u0010\r\u001a\u00020\n2\u0006\u0010\u000e\u001a\u00020\nH\u0002J\u001a\u0010\u000f\u001a\u00020\u00102\b\u0010\u0011\u001a\u0004\u0018\u00010\n2\b\u0010\u0012\u001a\u0004\u0018\u00010\nJ\u0010\u0010\u0013\u001a\u00020\u00142\u0006\u0010\u0015\u001a\u00020\nH\u0002J$\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\u00140\u00172\u0006\u0010\u000b\u001a\u00020\nH\u0086@\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\u0004\b\u0018\u0010\u0019R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u0082\u0002\u000b\n\u0002\b!\n\u0005\b\u00a1\u001e0\u0001\u00a8\u0006\u001b"}, d2 = {"Lcom/pevplugshare/ai/LocationDescriptionParser;", "", "tokenManager", "Lcom/pevplugshare/data/remote/TokenManager;", "(Lcom/pevplugshare/data/remote/TokenManager;)V", "okHttpClient", "Lokhttp3/OkHttpClient;", "buildRequestBody", "Lokhttp3/RequestBody;", "modelId", "", "description", "calculateDescriptionHash", "extractJson", "content", "hasDescriptionChanged", "", "oldHash", "newDescription", "parseAiResponse", "Lcom/pevplugshare/ai/ParsedLocationData;", "responseBody", "parseDescription", "Lkotlin/Result;", "parseDescription-gIAlu-s", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "Companion", "app_release"})
public final class LocationDescriptionParser {
    @org.jetbrains.annotations.NotNull()
    private final com.pevplugshare.data.remote.TokenManager tokenManager = null;
    @org.jetbrains.annotations.NotNull()
    private final okhttp3.OkHttpClient okHttpClient = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = "LocationDescriptionParser";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String SYSTEM_PROMPT = "You are a specialized AI assistant that extracts structured information from charging location descriptions for electric vehicles and personal electric vehicles (PEVs).\n\nExtract the following information from the description:\n1. Opening Hours - time/availability information\n2. Number of Charging Points/Sockets - count of available power outlets\n3. Capacity - power rating (Amps, Watts, or kW)\n4. Cost - whether free or paid\n5. Access Type - public or private/restricted\n\nCRITICAL PARSING RULES:\n\nSOCKET COUNTING:\n- Look for EXACT numbers: \"2x10A\", \"3 GPO\", \"dual GPO\" (=2), \"multiple\" (extract range if given)\n- Patterns like \"2x10A GPO\" means 2 sockets at 10A each\n- \"6-12 in total\" means uncertain count, use the LOW number (6)\n- \"dual\" = 2, \"triple\" = 3, \"multiple\" without number = null\n- DO NOT confuse capacity (10A) with socket count!\n- Examples:\n  * \"2x10A GPO\" \u2192 chargingPoints: 2, capacity: \"10A\"\n  * \"3 GPO 15A each\" \u2192 chargingPoints: 3, capacity: \"15A\"\n  * \"dual GPO\" \u2192 chargingPoints: 2\n  * \"6-12 in total\" \u2192 chargingPoints: 6\n  * \"multiple GPO\" with no number \u2192 chargingPoints: null\n\nOPENING HOURS - BE CONSERVATIVE:\n- ONLY extract if EXPLICITLY stated\n- If description mentions \"not suitable for night\" or \"turned off\" \u2192 LIMITED HOURS, not 24/7\n- \"not suitable for night\" \u2192 \"Daylight Hours\" or \"9AM-5PM (Weekdays)\"\n- \"may be turned off\" \u2192 indicates unreliable, still extract best guess\n- \"Tree lights get plugged in\" \u2192 indicates night usage restricted\n- If NO hours mentioned AND no restrictions \u2192 return null (let system default to 24/7)\n- If restrictions mentioned \u2192 estimate reasonable hours\n- Examples:\n  * \"not suitable for night\" \u2192 \"9AM-5PM (Weekdays)\"\n  * \"may be turned off during day\" \u2192 \"9AM-5PM (Weekdays)\" (conservative)\n  * \"24/7\" \u2192 \"24/7\"\n  * No mention at all \u2192 null\n\nACCESS TYPE:\n- \"food court\", \"public area\" \u2192 PUBLIC\n- \"customers only\", \"members only\", \"patrons only\" \u2192 PRIVATE (false)\n- In shopping centers, food courts, public spaces \u2192 PUBLIC\n- Behind buildings, restricted areas \u2192 PRIVATE\n- If unclear \u2192 null\n\nCOST:\n- Assume FREE unless price/fee explicitly mentioned\n- \"$\", \"paid\", \"charge\", \"fee\" \u2192 NOT FREE (isFree: false)\n- \"free\", \"no charge\", \"complimentary\" \u2192 FREE (isFree: true)\n- No mention \u2192 FREE (isFree: true)\n\nCAPACITY EXTRACTION:\n- Extract power rating with units: \"10A\", \"15A\", \"3.6kW\", \"2.4kW\"\n- Look for: \"10A\", \"15A each\", \"3.6kW\", etc.\n- If multiple capacities, list them: \"10A/15A\"\n\nReturn ONLY valid JSON:\n{\n  \"openingHours\": \"string or null\",\n  \"chargingPoints\": number or null,\n  \"capacity\": \"string or null\",\n  \"isFree\": boolean,\n  \"isPublic\": boolean or null\n}\n\nEXAMPLES:\n\nInput: \"In food court multiple dual GPO 6-12 in total multiple circuits, if some are not working try others\"\nResponse: {\"openingHours\": null, \"chargingPoints\": 6, \"capacity\": null, \"isFree\": true, \"isPublic\": true}\n\nInput: \"2x10A GPO on post next to first tree on river bank. Tree lights get plugged into it so not suitable for night. may be turned off during the day. Other power points in the area\"\nResponse: {\"openingHours\": \"9AM-5PM (Weekdays)\", \"chargingPoints\": 2, \"capacity\": \"10A\", \"isFree\": true, \"isPublic\": true}\n\nInput: \"Single 15A outlet behind building for customer use only. \\$5 per hour.\"\nResponse: {\"openingHours\": null, \"chargingPoints\": 1, \"capacity\": \"15A\", \"isFree\": false, \"isPublic\": false}\n\nInput: \"3 GPO available 24/7 in parking area\"\nResponse: {\"openingHours\": \"24/7\", \"chargingPoints\": 3, \"capacity\": null, \"isFree\": true, \"isPublic\": true}";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String USER_PROMPT_TEMPLATE = "Extract charging location information from this description:\n\n\"%s\"\n\nReturn only the JSON response, no additional text.";
    @org.jetbrains.annotations.NotNull()
    public static final com.pevplugshare.ai.LocationDescriptionParser.Companion Companion = null;
    
    @javax.inject.Inject()
    public LocationDescriptionParser(@org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.remote.TokenManager tokenManager) {
        super();
    }
    
    private final okhttp3.RequestBody buildRequestBody(java.lang.String modelId, java.lang.String description) {
        return null;
    }
    
    private final com.pevplugshare.ai.ParsedLocationData parseAiResponse(java.lang.String responseBody) {
        return null;
    }
    
    private final java.lang.String extractJson(java.lang.String content) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String calculateDescriptionHash(@org.jetbrains.annotations.Nullable()
    java.lang.String description) {
        return null;
    }
    
    public final boolean hasDescriptionChanged(@org.jetbrains.annotations.Nullable()
    java.lang.String oldHash, @org.jetbrains.annotations.Nullable()
    java.lang.String newDescription) {
        return false;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0003\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0007"}, d2 = {"Lcom/pevplugshare/ai/LocationDescriptionParser$Companion;", "", "()V", "SYSTEM_PROMPT", "", "TAG", "USER_PROMPT_TEMPLATE", "app_release"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}