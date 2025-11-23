# OAuth Callback Service - API Documentation

## Overview

This is a generic OAuth 2.0 callback handler service designed for mobile applications. It solves the problem of OAuth providers requiring publicly accessible callback URLs by providing a temporary session-based redirect service.

**Version**: 1.0.0
**Base URL**: `https://your-domain.com`

## Architecture

```
Mobile App → POST /oauth/register → Get callback_url
                ↓
        OAuth Provider Authorization
                ↓
   Provider redirects to callback_url
                ↓
    GET /oauth/callback/{session_id}
                ↓
        Deep link to mobile app
```

## Authentication

All requests to `/oauth/register` must include an HMAC-SHA256 signature for security.

### Generating Signatures

**Algorithm**: HMAC-SHA256
**Message Format**: `{provider}|{deep_link_url}|{timestamp}`
**Secret**: Shared secret key (from `.env`)

**Example (Python)**:
```python
import hmac
import hashlib
import time

def generate_signature(provider: str, deep_link_url: str, timestamp: int, secret: str) -> str:
    message = f"{provider.lower()}|{deep_link_url}|{timestamp}"
    return hmac.new(
        secret.encode('utf-8'),
        message.encode('utf-8'),
        hashlib.sha256
    ).hexdigest()

# Usage
secret = "your-shared-secret"
provider = "openrouter"
deep_link_url = "myapp://oauth/callback"
timestamp = int(time.time())
signature = generate_signature(provider, deep_link_url, timestamp, secret)
```

**Example (Kotlin/Android)**:
```kotlin
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.security.MessageDigest

fun generateSignature(provider: String, deepLinkUrl: String, timestamp: Long, secret: String): String {
    val message = "${provider.lowercase()}|$deepLinkUrl|$timestamp"
    val mac = Mac.getInstance("HmacSHA256")
    val secretKey = SecretKeySpec(secret.toByteArray(), "HmacSHA256")
    mac.init(secretKey)
    val bytes = mac.doFinal(message.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

// Usage
val secret = "your-shared-secret"
val provider = "openrouter"
val deepLinkUrl = "myapp://oauth/callback"
val timestamp = System.currentTimeMillis() / 1000
val signature = generateSignature(provider, deepLinkUrl, timestamp, secret)
```

## Endpoints

### 1. Register OAuth Session

Creates a temporary session and returns a callback URL.

**Endpoint**: `POST /oauth/register`
**Content-Type**: `application/json`

**Request Body**:
```json
{
  "provider": "openrouter",
  "deep_link_url": "myapp://oauth/callback",
  "timestamp": 1700000000,
  "signature": "abc123def456..."
}
```

**Request Fields**:
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `provider` | string | Yes | OAuth provider name (e.g., "openrouter", "github") |
| `deep_link_url` | string | Yes | Deep link URL to redirect back to your app (e.g., "myapp://oauth/callback") |
| `timestamp` | integer | Yes | Unix timestamp (seconds). Must be within ±5 minutes of server time |
| `signature` | string | Yes | HMAC-SHA256 signature of `{provider}\|{deep_link_url}\|{timestamp}` |

**Success Response** (200 OK):
```json
{
  "session_id": "550e8400-e29b-41d4-a716-446655440000",
  "callback_url": "https://your-domain.com/oauth/callback/550e8400-e29b-41d4-a716-446655440000",
  "expires_in": 600,
  "provider": "openrouter"
}
```

**Response Fields**:
| Field | Type | Description |
|-------|------|-------------|
| `session_id` | string | Unique session identifier (UUID) |
| `callback_url` | string | Full callback URL to use in OAuth flow |
| `expires_in` | integer | Session TTL in seconds (default: 600) |
| `provider` | string | OAuth provider name |

**Error Responses**:

**400 Bad Request** - Invalid provider:
```json
{
  "error": "invalid_provider",
  "message": "Provider 'xyz' is not supported",
  "details": {
    "allowed_providers": ["openrouter", "github", "google"]
  }
}
```

**400 Bad Request** - Invalid signature:
```json
{
  "error": "invalid_signature",
  "message": "Timestamp too old or in future (diff: 305s, max: 300s)",
  "details": {
    "timestamp": 1700000000
  }
}
```

**429 Too Many Requests** - Rate limit exceeded:
```json
{
  "error": "rate_limit_exceeded",
  "message": "Rate limit exceeded: 10 requests per minute",
  "details": {
    "minute": 10,
    "hour": 45,
    "day": 230,
    "limits": {
      "minute": 10,
      "hour": 100,
      "day": 1000
    }
  }
}
```

### 2. Handle OAuth Callback

Receives callback from OAuth provider and redirects to mobile app.

**Endpoint**: `GET /oauth/callback/{session_id}`
**Response**: HTML page with automatic redirect

**Path Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| `session_id` | string | Session ID from registration |

**Query Parameters**:
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `code` | string | Yes* | Authorization code from OAuth provider |
| `error` | string | No | Error code if authorization failed |
| `error_description` | string | No | Human-readable error description |

*Either `code` or `error` will be present

**Success Response** (200 OK):
Returns HTML page that:
1. Displays success message
2. Shows loading spinner
3. Auto-redirects to deep link after 2 seconds
4. Provides manual fallback link

**Deep Link Format** (Success):
```
pevplugshare://oauth/callback?code={authorization_code}&provider={provider_name}
```

**Deep Link Format** (Error):
```
pevplugshare://oauth/callback?error={error_code}&error_description={description}
```

**Error Pages**:

The service returns user-friendly HTML error pages for:
- Session not found
- Session expired (>10 minutes old)
- Session already used
- Missing authorization code

### 3. Health Check

Check service status and active sessions.

**Endpoint**: `GET /health`

**Success Response** (200 OK):
```json
{
  "status": "healthy",
  "version": "1.0.0",
  "active_sessions": 5
}
```

### 4. Service Info

Get service information and supported providers.

**Endpoint**: `GET /`

**Success Response** (200 OK):
```json
{
  "service": "OAuth Callback Service",
  "version": "1.0.0",
  "description": "Generic OAuth 2.0 callback handler for mobile apps",
  "endpoints": {
    "health": "/health",
    "register": "POST /oauth/register",
    "callback": "GET /oauth/callback/{session_id}"
  },
  "supported_providers": ["openrouter", "github", "google"],
  "documentation": "/docs"
}
```

## Rate Limiting

Rate limits are applied per IP address:

| Window | Default Limit | Header |
|--------|---------------|--------|
| Minute | 10 requests | - |
| Hour | 100 requests | - |
| Day | 1000 requests | - |

Limits are configurable via environment variables.

## Security Features

1. **HMAC Signature Verification**: All registration requests must be signed
2. **Timestamp Validation**: Prevents replay attacks (±5 minute window)
3. **Single-Use Sessions**: Sessions marked as used after first callback
4. **Automatic Expiration**: Sessions expire after 10 minutes (configurable)
5. **Rate Limiting**: Prevents abuse and DDoS attacks
6. **HTTPS Only**: All connections must use TLS
7. **CORS Protection**: Configurable allowed origins

## Android Integration Example

### 1. Add Constants

```kotlin
// SettingsActivity.kt
private const val OAUTH_SERVICE_URL = "https://your-cloudflare-tunnel.example.com"
private const val OAUTH_SHARED_SECRET = "your-shared-secret"  // From native-secrets.cpp
private const val DEEP_LINK_SCHEME = "pevplugshare://oauth/callback"
```

### 2. Create Signature Helper

```kotlin
// util/OAuthSignature.kt
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object OAuthSignature {
    fun generateSignature(provider: String, deepLinkUrl: String, timestamp: Long, secret: String): String {
        val message = "${provider.lowercase()}|$deepLinkUrl|$timestamp"
        val mac = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(secret.toByteArray(), "HmacSHA256")
        mac.init(secretKey)
        val bytes = mac.doFinal(message.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun getCurrentTimestamp(): Long = System.currentTimeMillis() / 1000
}
```

### 3. Register Session

```kotlin
// SettingsActivity.kt
private suspend fun registerOAuthSession(provider: String): JSONObject {
    val deepLinkUrl = DEEP_LINK_SCHEME
    val timestamp = OAuthSignature.getCurrentTimestamp()
    val signature = OAuthSignature.generateSignature(provider, deepLinkUrl, timestamp, OAUTH_SHARED_SECRET)

    val requestBody = JSONObject().apply {
        put("provider", provider)
        put("deep_link_url", deepLinkUrl)
        put("timestamp", timestamp)
        put("signature", signature)
    }

    val client = OkHttpClient()
    val request = Request.Builder()
        .url("$OAUTH_SERVICE_URL/oauth/register")
        .post(RequestBody.create(
            "application/json".toMediaType(),
            requestBody.toString()
        ))
        .build()

    val response = client.newCall(request).execute()
    if (!response.isSuccessful) {
        throw IOException("Session registration failed: ${response.code}")
    }

    return JSONObject(response.body?.string() ?: "{}")
}
```

### 4. Start OAuth Flow

```kotlin
private fun startOAuthFlow() {
    lifecycleScope.launch(Dispatchers.IO) {
        try {
            // Step 1: Register session with callback service
            val sessionResponse = registerOAuthSession("openrouter")
            val callbackUrl = sessionResponse.getString("callback_url")

            // Step 2: Generate PKCE parameters
            val codeVerifier = PkceUtils.generateCodeVerifier()
            val codeChallenge = PkceUtils.generateCodeChallenge(codeVerifier)
            tokenManager.savePkceCodeVerifier(codeVerifier)

            // Step 3: Build OpenRouter auth URL with callback service URL
            val authUrl = Uri.parse("https://openrouter.ai/auth").buildUpon()
                .appendQueryParameter("callback_url", callbackUrl)
                .appendQueryParameter("code_challenge", codeChallenge)
                .appendQueryParameter("code_challenge_method", "S256")
                .build()

            // Step 4: Launch browser
            withContext(Dispatchers.Main) {
                val customTabsIntent = CustomTabsIntent.Builder()
                    .setShowTitle(true)
                    .build()
                customTabsIntent.launchUrl(this@SettingsActivity, authUrl)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@SettingsActivity,
                    "Failed to start OAuth: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
```

### 5. Update AndroidManifest

```xml
<!-- AndroidManifest.xml -->
<activity android:name=".ui.settings.SettingsActivity">
    <!-- Existing intent filters... -->

    <!-- Deep link for OAuth callback -->
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data
            android:scheme="pevplugshare"
            android:host="oauth"
            android:path="/callback" />
    </intent-filter>
</activity>
```

### 6. Handle Deep Link Callback

```kotlin
// SettingsActivity.kt
override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)
    intent?.data?.let { uri ->
        if (uri.scheme == "pevplugshare" && uri.host == "oauth") {
            handleOAuthCallback(uri)
        }
    }
}

private fun handleOAuthCallback(uri: Uri) {
    val code = uri.getQueryParameter("code")
    val error = uri.getQueryParameter("error")
    val provider = uri.getQueryParameter("provider")

    if (error != null) {
        val errorDesc = uri.getQueryParameter("error_description") ?: "Unknown error"
        Toast.makeText(this, "OAuth failed: $errorDesc", Toast.LENGTH_LONG).show()
        return
    }

    if (code != null && provider != null) {
        // Exchange code for API key
        lifecycleScope.launch {
            exchangeCodeForApiKey(code)
        }
    }
}
```

## Complete OAuth Flow Example

### OpenRouter Integration

```kotlin
// Complete OpenRouter OAuth flow with callback service

private suspend fun startOpenRouterOAuth() {
    try {
        // 1. Register callback session
        val deepLinkUrl = DEEP_LINK_SCHEME
        val timestamp = System.currentTimeMillis() / 1000
        val signature = OAuthSignature.generateSignature("openrouter", deepLinkUrl, timestamp, OAUTH_SHARED_SECRET)

        val registerRequest = JSONObject().apply {
            put("provider", "openrouter")
            put("deep_link_url", deepLinkUrl)
            put("timestamp", timestamp)
            put("signature", signature)
        }

        val sessionResponse = registerSession(registerRequest)
        val callbackUrl = sessionResponse.getString("callback_url")

        // 2. Generate PKCE
        val codeVerifier = PkceUtils.generateCodeVerifier()
        val codeChallenge = PkceUtils.generateCodeChallenge(codeVerifier)
        tokenManager.savePkceCodeVerifier(codeVerifier)

        // 3. Build auth URL
        val authUrl = Uri.parse("https://openrouter.ai/auth").buildUpon()
            .appendQueryParameter("callback_url", callbackUrl)
            .appendQueryParameter("code_challenge", codeChallenge)
            .appendQueryParameter("code_challenge_method", "S256")
            .build()

        // 4. Launch browser
        withContext(Dispatchers.Main) {
            CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
                .launchUrl(this@SettingsActivity, authUrl)
        }

    } catch (e: Exception) {
        Log.e(TAG, "OAuth flow failed", e)
        withContext(Dispatchers.Main) {
            Toast.makeText(this@SettingsActivity,
                "Failed to start OAuth: ${e.message}",
                Toast.LENGTH_LONG).show()
        }
    }
}

private suspend fun registerSession(request: JSONObject): JSONObject {
    val client = OkHttpClient()
    val requestBody = RequestBody.create(
        "application/json".toMediaType(),
        request.toString()
    )

    val httpRequest = Request.Builder()
        .url("$OAUTH_SERVICE_URL/oauth/register")
        .post(requestBody)
        .build()

    val response = client.newCall(httpRequest).execute()
    val body = response.body?.string() ?: "{}"

    if (!response.isSuccessful) {
        val error = JSONObject(body)
        throw IOException(error.optString("message", "Registration failed"))
    }

    return JSONObject(body)
}

private suspend fun exchangeCodeForApiKey(code: String) {
    val codeVerifier = tokenManager.getPkceCodeVerifier() ?: run {
        Log.e(TAG, "No code verifier found")
        return
    }

    // Exchange code for API key using OpenRouter token endpoint
    val client = OkHttpClient()
    val requestBody = FormBody.Builder()
        .add("code", code)
        .add("code_verifier", codeVerifier)
        .add("code_challenge_method", "S256")
        .build()

    val request = Request.Builder()
        .url("https://openrouter.ai/api/v1/auth/keys")
        .post(requestBody)
        .build()

    try {
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            val json = JSONObject(responseBody)
            val apiKey = json.getString("key")

            // Save API key
            tokenManager.saveOpenRouterApiKey(apiKey)
            tokenManager.clearPkceCodeVerifier()

            withContext(Dispatchers.Main) {
                Toast.makeText(this@SettingsActivity,
                    "Successfully connected OpenRouter account!",
                    Toast.LENGTH_SHORT).show()
            }
        } else {
            throw IOException("Token exchange failed: ${response.code}")
        }
    } catch (e: Exception) {
        Log.e(TAG, "Token exchange failed", e)
        withContext(Dispatchers.Main) {
            Toast.makeText(this@SettingsActivity,
                "Failed to complete authorization: ${e.message}",
                Toast.LENGTH_LONG).show()
        }
    }
}
```

## Testing

### Manual Testing with cURL

**1. Register a session**:
```bash
# Generate signature
SECRET="your-shared-secret"
PROVIDER="openrouter"
TIMESTAMP=$(date +%s)
MESSAGE="${PROVIDER}|${TIMESTAMP}"
SIGNATURE=$(echo -n "$MESSAGE" | openssl dgst -sha256 -hmac "$SECRET" | awk '{print $2}')

# Make request
curl -X POST https://your-domain.com/oauth/register \
  -H "Content-Type: application/json" \
  -d "{
    \"provider\": \"$PROVIDER\",
    \"timestamp\": $TIMESTAMP,
    \"signature\": \"$SIGNATURE\"
  }"
```

**2. Test callback** (simulate OAuth provider redirect):
```bash
SESSION_ID="your-session-id-from-step-1"
AUTH_CODE="test_authorization_code"

curl "https://your-domain.com/oauth/callback/${SESSION_ID}?code=${AUTH_CODE}"
```

## Error Handling

All errors follow a consistent format:

```json
{
  "error": "error_type",
  "message": "Human-readable message",
  "details": {
    "additional": "context"
  }
}
```

**Common Error Types**:
- `invalid_provider`: Unsupported OAuth provider
- `invalid_signature`: HMAC signature verification failed
- `rate_limit_exceeded`: Too many requests from IP
- `session_not_found`: Invalid or expired session ID
- `session_expired`: Session TTL exceeded
- `session_used`: Session already consumed
- `internal_server_error`: Unexpected server error

## Configuration

All settings are configured via environment variables in `.env`:

```env
# Database
DATABASE_URL=sqlite:///./oauth_sessions.db

# Session
SESSION_TTL_MINUTES=10
CLEANUP_INTERVAL_MINUTES=5

# Rate Limiting
RATE_LIMIT_PER_MINUTE=10
RATE_LIMIT_PER_HOUR=100
RATE_LIMIT_PER_DAY=1000

# Providers
OAUTH_PROVIDERS=openrouter,github,google

# Logging
LOG_LEVEL=INFO
```

**Note**: Domain names, deep link schemes, and secrets are configured **per-application in the database**, not via environment variables. See the main README for application management instructions.

## Support

For issues or questions:
- Check logs: Service logs all requests and errors
- Health endpoint: `GET /health` for service status
- Interactive docs: `/docs` for Swagger UI
- ReDoc: `/redoc` for alternative documentation
