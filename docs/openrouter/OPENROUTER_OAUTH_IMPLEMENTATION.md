# OpenRouter OAuth2 Implementation

Complete OAuth2 + PKCE implementation for OpenRouter authentication using the generic OAuth callback service.

## Architecture

```
Android App → OAuth Callback Service → OpenRouter
     ↓              ↓                        ↓
  Register      Get callback URL      Authorize
     ↓              ↓                        ↓
  Generate      Return session        Return code
   PKCE            info                      ↓
     ↓              ↓                   Deep link
  Open          Redirect to           back to app
  Browser       OpenRouter                  ↓
                                      Exchange code
                                      for API key
```

## Configuration

### Build Configuration

Settings are stored in `app/build.gradle.kts`:

**Development** (debug build):
```kotlin
buildConfigField("String", "OAUTH_CALLBACK_URL", "\"https://pevplugshare.a42r.com\"")
buildConfigField("String", "OAUTH_SHARED_SECRET", "\"9CsxVnJmHiI888wnUpOrhrFsYaLzq09dbcvmiNl8RP8\"")
buildConfigField("String", "DEEP_LINK_SCHEME", "\"pevplugshare://oauth/callback\"")
```

**Production** (release build):
```kotlin
buildConfigField("String", "OAUTH_CALLBACK_URL", "\"https://pevplugshare-prod.a42r.com\"")
buildConfigField("String", "OAUTH_SHARED_SECRET", "\"CHANGE_THIS_IN_PRODUCTION\"")
buildConfigField("String", "DEEP_LINK_SCHEME", "\"pevplugshare://oauth/callback\"")
```

### Deep Link Configuration

The app registers the deep link scheme in `AndroidManifest.xml`:

```xml
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data
        android:scheme="pevplugshare"
        android:host="oauth"
        android:path="/callback" />
</intent-filter>
```

## Implementation Components

### 1. OAuthSignature.kt

HMAC-SHA256 signature utility for secure callback service authentication.

**Message Format**: `{provider}|{deep_link_url}|{timestamp}`

```kotlin
val signature = OAuthSignature.generateSignature(
    provider = "openrouter",
    deepLinkUrl = "pevplugshare://oauth/callback",
    timestamp = 1700000000,
    secret = BuildConfig.OAUTH_SHARED_SECRET
)
```

### 2. OpenRouterOAuthService.kt

Service for managing the OAuth2 + PKCE flow:

- **registerOAuthSession()**: Register session with callback service
- **buildAuthorizationUrl()**: Build OpenRouter auth URL with PKCE
- **exchangeCodeForKey()**: Exchange authorization code for API key

**PKCE Implementation**:
```kotlin
// Code verifier: 64 random characters
val codeVerifier = generateRandomString(64)

// Code challenge: SHA-256(verifier), base64url encoded
val codeChallenge = Base64.encodeToString(
    MessageDigest.getInstance("SHA-256").digest(codeVerifier.toByteArray()),
    Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
)
```

### 3. OpenRouterOAuthViewModel.kt

ViewModel managing OAuth flow state:

**States**:
- `Idle` - Ready to start
- `Registering` - Registering with callback service
- `ReadyToAuthorize` - Authorization URL ready
- `ExchangingCode` - Exchanging code for API key
- `Success` - API key obtained
- `Error` - Flow failed

### 4. OAuthActivity.kt

Demo activity showing complete OAuth flow:

```kotlin
// Start OAuth flow
viewModel.startOAuthFlow()

// Observe state changes
viewModel.oauthState.observe(this) { state ->
    when (state) {
        is ReadyToAuthorize -> openBrowser(state.authUrl)
        is Success -> handleApiKey(state.apiKey)
        is Error -> showError(state.message)
    }
}

// Handle deep link callback
handleDeepLink(intent)
```

## OAuth Flow Sequence

### 1. Session Registration

```kotlin
POST https://pevplugshare.a42r.com/oauth/register
Content-Type: application/json

{
  "provider": "openrouter",
  "deep_link_url": "pevplugshare://oauth/callback",
  "timestamp": 1700000000,
  "signature": "abc123..."  // HMAC-SHA256
}

Response:
{
  "session_id": "uuid",
  "callback_url": "https://pevplugshare.a42r.com/oauth/callback/uuid",
  "expires_in": 600,
  "provider": "openrouter"
}
```

### 2. Authorization Request

```kotlin
GET https://openrouter.ai/auth?
    callback_url=https://pevplugshare.a42r.com/oauth/callback/uuid&
    code_challenge=BASE64URL(SHA256(code_verifier))&
    code_challenge_method=S256
```

User authorizes in browser → OpenRouter redirects to callback URL

### 3. Callback Service Redirect

```kotlin
Callback service receives:
GET https://pevplugshare.a42r.com/oauth/callback/uuid?code=AUTH_CODE

Callback service redirects to deep link:
pevplugshare://oauth/callback?code=AUTH_CODE&provider=openrouter
```

### 4. Code Exchange

```kotlin
POST https://openrouter.ai/api/v1/auth/keys
Content-Type: application/json

{
  "code": "AUTH_CODE",
  "code_verifier": "ORIGINAL_CODE_VERIFIER"
}

Response:
{
  "key": "sk-or-v1-..."
}
```

## Security Features

### HMAC Signature
- Every callback service request is HMAC signed
- Signature includes: provider, deep_link_url, timestamp
- Prevents unauthorized session registration
- Timestamp validation (±5 minutes) prevents replay attacks

### PKCE (Proof Key for Code Exchange)
- Code verifier: 64 random characters
- Code challenge: SHA-256 hash, base64url encoded
- Prevents authorization code interception attacks
- Code verifier never leaves the app

### Deep Link Security
- Custom URL scheme: `pevplugshare://`
- Only this app can handle the deep link
- Callback data validated before processing

### Build Configuration Security
- Secrets stored in BuildConfig (not version control)
- Different secrets for debug/release builds
- Production secret must be changed before release

## Testing

### Manual Testing

1. **Build and run** the app
2. **Launch OAuthActivity**
3. **Tap "Connect OpenRouter"**
4. **Observe flow**:
   - Status: "Registering OAuth session..."
   - Browser opens OpenRouter authorization page
   - User authorizes
   - App returns via deep link
   - Status: "Exchanging authorization code..."
   - Status: "✅ Authentication successful!"
   - API key displayed

### Testing Deep Links

```bash
# Test deep link (with adb)
adb shell am start -W -a android.intent.action.VIEW \
  -d "pevplugshare://oauth/callback?code=test_code&provider=openrouter"
```

### Testing Callback Service

```bash
# Test registration (requires running callback service)
curl -X POST https://pevplugshare.a42r.com/oauth/register \
  -H "Content-Type: application/json" \
  -d '{
    "provider": "openrouter",
    "deep_link_url": "pevplugshare://oauth/callback",
    "timestamp": '$(date +%s)',
    "signature": "..."
  }'
```

## Error Handling

### Network Errors
```kotlin
try {
    val pkceState = oauthService.registerOAuthSession()
} catch (e: Exception) {
    // Handle: Connection timeout, DNS failure, etc.
    showError("Registration failed: ${e.message}")
}
```

### OAuth Errors
```kotlin
// Callback with error parameter
pevplugshare://oauth/callback?error=access_denied&error_description=User%20cancelled

viewModel.handleError("access_denied", "User cancelled")
```

### Validation Errors
```kotlin
// Invalid callback
if (code == null || provider == null) {
    viewModel.handleError("invalid_callback", "Missing parameters")
}

// Wrong provider
if (provider != "openrouter") {
    viewModel.handleError("invalid_provider", "Unexpected provider")
}
```

## Production Deployment Checklist

- [ ] Generate new production `OAUTH_SHARED_SECRET`
- [ ] Update production `OAUTH_CALLBACK_URL`
- [ ] Deploy production callback service
- [ ] Configure production Cloudflare Tunnel
- [ ] Test complete OAuth flow in release build
- [ ] Verify deep links work in production
- [ ] Monitor callback service logs
- [ ] Document production secrets securely

## Callback Service Requirements

The OAuth callback service must be:
- Publicly accessible via HTTPS
- Running with correct `SHARED_SECRET`
- Configured with matching deep link URL
- Database initialized (SQLite)

See `oauth-callback-service/README.md` for deployment instructions.

## Troubleshooting

### "Invalid signature" error
- Check `OAUTH_SHARED_SECRET` matches callback service
- Verify timestamp is current (within ±5 minutes)
- Ensure signature message format is correct

### Deep link not working
- Check AndroidManifest deep link configuration
- Verify `DEEP_LINK_SCHEME` matches manifest
- Test with `adb shell am start -d "..."`

### Browser not opening
- Check Chrome Custom Tabs dependency
- Verify URL is valid HTTPS
- Check device has browser installed

### Code exchange fails
- Verify `code_verifier` matches original
- Check OpenRouter API key exchange endpoint
- Ensure code hasn't expired (10 minutes)

## Future Enhancements

- [ ] Store API key securely (EncryptedSharedPreferences)
- [ ] Refresh token support
- [ ] Multiple OAuth providers (GitHub, Google)
- [ ] OAuth state parameter for CSRF protection
- [ ] Better error messages and recovery
- [ ] Offline queue for failed exchanges

## References

- OAuth 2.0 RFC: https://tools.ietf.org/html/rfc6749
- PKCE RFC: https://tools.ietf.org/html/rfc7636
- OpenRouter API: https://openrouter.ai/docs
- Android Custom Tabs: https://developer.chrome.com/docs/android/custom-tabs/
