package com.pevplugshare.ai;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000N\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010 \n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0006\b\u0007\u0018\u0000 \u001f2\u00020\u0001:\u0001\u001fB\u000f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004Jv\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\n2\u0006\u0010\f\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\u000f2\u0006\u0010\u0010\u001a\u00020\u00112\f\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\n0\u00132\b\u0010\u0014\u001a\u0004\u0018\u00010\n2\b\u0010\u0015\u001a\u0004\u0018\u00010\n2\b\u0010\u0016\u001a\u0004\u0018\u00010\n2\u0018\u0010\u0017\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\n\u0012\u0004\u0012\u00020\n0\u00180\u0013H\u0002J\u0084\u0001\u0010\u0019\u001a\b\u0012\u0004\u0012\u00020\n0\u001a2\u0006\u0010\u000b\u001a\u00020\n2\u0006\u0010\f\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\u000f2\u0006\u0010\u0010\u001a\u00020\u00112\f\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\n0\u00132\b\u0010\u0014\u001a\u0004\u0018\u00010\n2\b\u0010\u0015\u001a\u0004\u0018\u00010\n2\b\u0010\u0016\u001a\u0004\u0018\u00010\n2\u001a\b\u0002\u0010\u0017\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\n\u0012\u0004\u0012\u00020\n0\u00180\u0013H\u0086@\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\u0004\b\u001b\u0010\u001cJ\u0010\u0010\u001d\u001a\u00020\n2\u0006\u0010\u001e\u001a\u00020\nH\u0002R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u0082\u0002\u000b\n\u0002\b!\n\u0005\b\u00a1\u001e0\u0001\u00a8\u0006 "}, d2 = {"Lcom/pevplugshare/ai/LocationDescriptionGenerator;", "", "tokenManager", "Lcom/pevplugshare/data/remote/TokenManager;", "(Lcom/pevplugshare/data/remote/TokenManager;)V", "okHttpClient", "Lokhttp3/OkHttpClient;", "buildRequestBody", "Lokhttp3/RequestBody;", "modelId", "", "locationName", "accessType", "Lcom/pevplugshare/data/model/LocationAccessType;", "hasFee", "", "chargingPoints", "", "socketTypes", "", "openingHours", "capacity", "additionalInfo", "conversationHistory", "Lkotlin/Pair;", "generateDescription", "Lkotlin/Result;", "generateDescription-LiYkppA", "(Ljava/lang/String;Lcom/pevplugshare/data/model/LocationAccessType;ZILjava/util/List;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/util/List;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "parseAiResponse", "responseBody", "Companion", "app_debug"})
public final class LocationDescriptionGenerator {
    @org.jetbrains.annotations.NotNull()
    private final com.pevplugshare.data.remote.TokenManager tokenManager = null;
    @org.jetbrains.annotations.NotNull()
    private final okhttp3.OkHttpClient okHttpClient = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = "LocationDescriptionGenerator";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String SYSTEM_PROMPT = "You are a specialized AI assistant that generates standardized, informative descriptions for PEV (Personal Electric Vehicle) charging locations.\n\nYour task is to create a clear, readable description that includes all available charging details in a consistent format.\n\nThe description MUST follow this EXACT format:\n\n[Brief contextual description of the location - 1-2 sentences]\n\nDetails:\n* Public Access: [allowed/restricted]\n* Sockets: [number and type of sockets, OR \"at least 1 charging point is available\" if number is 0 or unknown]\n* Available: [opening hours]\n* Cost: [Free/Paid]\n* Type: PEV Charging Location\n\nIMPORTANT RULES:\n1. Start with a brief, contextual description (1-2 sentences) describing the physical location or key features\n2. Follow with \"Details:\" on a new line with a blank line before it\n3. Use bullet points (asterisk + space) for each detail\n4. If the number of charging points is 0 or unknown, use \"at least 1 [socket type] is available\"\n5. Keep it informative but concise\n6. Include any additional context in the opening description\n\nExample output:\n\"Single GPO on Light Pole on corner of grassed area near The Wheel of Brisbane.\n\nDetails:\n* Public Access: allowed\n* Sockets: at least 1 AU GPO\n* Available: 24x7\n* Cost: Free\n* Type: PEV Charging Location\"\n\nAnother example:\n\"Multiple charging points in visitor parking area. Undercover parking available with good lighting for evening use.\n\nDetails:\n* Public Access: allowed\n* Sockets: 4 Type 2 EV chargers (7kW each)\n* Available: 9am-6pm Mon-Fri\n* Cost: Paid (app required)\n* Type: PEV Charging Location\"\n\nGenerate a description following this EXACT format.";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String USER_PROMPT_TEMPLATE = "Generate a standardized charging location description with the following details:\n\nLocation Name: %s\nAccess Type: %s\nCost: %s\nNumber of Charging Points: %d\nSocket Types: %s\nOpening Hours: %s\nPower Capacity: %s\nAdditional Information: %s\n\nFollow the EXACT format from the system prompt with:\n1. Brief contextual description (1-2 sentences)\n2. Blank line\n3. \"Details:\" header\n4. Bullet points for all charging details\n5. If charging points is 0, use \"at least 1 [socket type] is available\"\n\nReturn ONLY the formatted description, no additional commentary.";
    @org.jetbrains.annotations.NotNull()
    public static final com.pevplugshare.ai.LocationDescriptionGenerator.Companion Companion = null;
    
    @javax.inject.Inject()
    public LocationDescriptionGenerator(@org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.remote.TokenManager tokenManager) {
        super();
    }
    
    private final okhttp3.RequestBody buildRequestBody(java.lang.String modelId, java.lang.String locationName, com.pevplugshare.data.model.LocationAccessType accessType, boolean hasFee, int chargingPoints, java.util.List<java.lang.String> socketTypes, java.lang.String openingHours, java.lang.String capacity, java.lang.String additionalInfo, java.util.List<kotlin.Pair<java.lang.String, java.lang.String>> conversationHistory) {
        return null;
    }
    
    private final java.lang.String parseAiResponse(java.lang.String responseBody) {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0003\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0007"}, d2 = {"Lcom/pevplugshare/ai/LocationDescriptionGenerator$Companion;", "", "()V", "SYSTEM_PROMPT", "", "TAG", "USER_PROMPT_TEMPLATE", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}