# OAuth Multi-Application Support - Implementation Complete

## Summary

Both the FastAPI callback service and AWS Lambda version have been updated to support multiple applications through database/configuration-driven architecture. This removes hardcoded secrets and domains, allowing a single deployment to serve multiple applications.

## What Changed

### Core Architecture Changes

1. **Application Configuration**
   - **FastAPI**: SQLite database table
   - **Lambda**: Environment variable JSON configuration
   - Each application has: ID, name, allowed domains, deep link scheme, and shared secret

2. **Request Routing**
   - HTTP `Host` header identifies the application
   - Applications validated against allowed domains
   - Each application uses its own secret for signature verification

3. **Signature Format**
   - **Old**: `{provider}|{timestamp}`
   - **New**: `{provider}|{deep_link_url}|{timestamp}`
   - Includes deep link URL for better security

4. **Callback URLs**
   - **Old**: Hardcoded from `DOMAIN_NAME` environment variable
   - **New**: Built from request hostname (supports multiple domains per app)

5. **Session Storage**
   - Added `app_id` field to link sessions to applications
   - Added `deep_link_url` field for callback redirection

## Files Modified

### OAuth Callback Service (FastAPI)

✅ **app/models/database.py**
- Added `Application` model with app_id, allowed_domains, deep_link_scheme, shared_secret
- Added `seed_sample_application()` for initial data
- Updated `OAuthSession` with app_id foreign key

✅ **app/routers/oauth.py**
- Extract request hostname from `request.url.hostname`
- Query Application table by allowed_domains
- Validate deep link URL matches application scheme
- Verify signature with application's shared_secret
- Build callback URL from request hostname

✅ **app/config.py**
- Removed `shared_secret` requirement
- Removed `domain_name` requirement

✅ **app/utils/security.py**
- Updated `verify_signature()` to accept `secret` parameter

✅ **app/main.py**
- Call `seed_sample_application()` on startup

✅ **MULTI_APP_MIGRATION.md**
- Comprehensive migration guide
- Testing procedures
- Security best practices

### OAuth Callback Lambda (AWS)

✅ **src/shared/security.py**
- Updated `generate_signature()` signature: `(provider, deep_link_url, timestamp, secret)`
- Updated `verify_signature()` signature: `(provider, deep_link_url, timestamp, signature, secret)`
- Changed message format to include deep_link_url
- Removed default secret fallback to environment variable

✅ **src/shared/applications.py** (NEW)
- `Application` class for configuration management
- `load_applications()` from environment variable JSON
- `get_application(domain)` for domain-based lookup
- Module-level caching for performance

✅ **src/register/handler.py**
- Extract request host from event headers
- Lookup application by domain
- Validate deep link scheme
- Verify signature with app's secret
- Store app_id and deep_link_url in session
- Build callback URL from request hostname

✅ **src/callback/handler.py**
- Retrieve deep_link_url from session (not environment)
- Use session's deep_link_url for redirects
- Removed DEEP_LINK_SCHEME environment variable

✅ **LAMBDA_DEPLOYMENT.md**
- Deployment guide for Lambda
- Environment variable configuration
- Security best practices
- Troubleshooting guide

## Configuration Examples

### FastAPI (Database)

```sql
INSERT INTO applications (
  app_id, name, allowed_domains,
  deep_link_scheme, shared_secret, is_active
) VALUES (
  'pevplugshare',
  'PEV PlugShare',
  'pevplugshare.a42r.com,pevplugshare-prod.a42r.com,localhost:8000',
  'pevplugshare://oauth/callback',
  'generate-with: python -c "import secrets; print(secrets.token_urlsafe(32))"',
  1
);
```

### Lambda (Environment Variable)

```json
{
  "APPLICATIONS": "[{\"app_id\":\"pevplugshare\",\"name\":\"PEV PlugShare\",\"allowed_domains\":[\"pevplugshare.a42r.com\",\"pevplugshare-prod.a42r.com\"],\"deep_link_scheme\":\"pevplugshare://oauth/callback\",\"shared_secret\":\"your-secret-here\"}]"
}
```

## API Changes

### Request Format (Updated)

```json
{
  "provider": "openrouter",
  "deep_link_url": "pevplugshare://oauth/callback",
  "timestamp": 1700000000,
  "signature": "abc123..."
}
```

**New Required Field:**
- `deep_link_url`: Full deep link callback URL (used in signature)

### Response Format (Changed)

**Before:**
```json
{
  "callback_url": "https://configured-domain.com/oauth/callback/{id}"
}
```

**After:**
```json
{
  "callback_url": "https://request-hostname/oauth/callback/{id}"
}
```

The callback URL now reflects the actual request hostname.

### Error Responses (New)

**unknown_application**
```json
{
  "error": "unknown_application",
  "message": "No application configured for domain: example.com",
  "details": {"request_host": "example.com"}
}
```

**invalid_deep_link**
```json
{
  "error": "invalid_deep_link",
  "message": "Deep link URL must start with: pevplugshare://oauth/callback",
  "details": {
    "expected_scheme": "pevplugshare://oauth/callback",
    "received_url": "wrongapp://oauth/callback"
  }
}
```

## Mobile App Updates Required

### 1. Update Signature Generation

**Before:**
```kotlin
val message = "$provider|$timestamp"
```

**After:**
```kotlin
val deepLinkUrl = "pevplugshare://oauth/callback"
val message = "$provider|$deepLinkUrl|$timestamp"
```

### 2. Update Request Body

**Before:**
```kotlin
val requestBody = mapOf(
    "provider" to provider,
    "timestamp" to timestamp,
    "signature" to signature
)
```

**After:**
```kotlin
val requestBody = mapOf(
    "provider" to provider,
    "deep_link_url" to deepLinkUrl,
    "timestamp" to timestamp,
    "signature" to signature
)
```

### 3. Example Implementation

```kotlin
// In OAuthCallbackService or similar
private fun generateSignature(
    provider: String,
    deepLinkUrl: String,
    timestamp: Long,
    secret: String
): String {
    val message = "${provider.lowercase()}|$deepLinkUrl|$timestamp"
    val mac = Mac.getInstance("HmacSHA256")
    val secretKey = SecretKeySpec(secret.toByteArray(), "HmacSHA256")
    mac.init(secretKey)
    val bytes = mac.doFinal(message.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

suspend fun registerOAuthSession(provider: String): String {
    val deepLinkUrl = getDeepLinkUrl() // e.g., "pevplugshare://oauth/callback"
    val timestamp = System.currentTimeMillis() / 1000
    val secret = getSharedSecret() // From native-secrets.cpp

    val signature = generateSignature(provider, deepLinkUrl, timestamp, secret)

    val response = apiClient.post("oauth/register") {
        contentType(ContentType.Application.Json)
        setBody(mapOf(
            "provider" to provider,
            "deep_link_url" to deepLinkUrl,
            "timestamp" to timestamp,
            "signature" to signature
        ))
    }

    return response.body<OAuthRegisterResponse>().callback_url
}
```

## Testing Multi-Application Setup

### 1. Add Test Application

**FastAPI:**
```sql
INSERT INTO applications VALUES (
  'test-app', 'Test App', 'localhost:8000',
  'testapp://callback', 'test-secret-123',
  datetime('now'), datetime('now'), 1
);
```

**Lambda:**
```bash
export APPLICATIONS='[
  {
    "app_id": "test-app",
    "name": "Test App",
    "allowed_domains": ["localhost"],
    "deep_link_scheme": "testapp://callback",
    "shared_secret": "test-secret-123"
  }
]'
```

### 2. Generate Test Signature

```python
import hmac, hashlib, time

provider = "openrouter"
deep_link = "testapp://callback"
timestamp = int(time.time())
secret = "test-secret-123"

message = f"{provider}|{deep_link}|{timestamp}"
signature = hmac.new(
    secret.encode(),
    message.encode(),
    hashlib.sha256
).hexdigest()

print(f"Timestamp: {timestamp}")
print(f"Signature: {signature}")
```

### 3. Test Request

```bash
curl -X POST http://localhost:8000/oauth/register \
  -H "Content-Type: application/json" \
  -H "Host: localhost:8000" \
  -d '{
    "provider": "openrouter",
    "deep_link_url": "testapp://callback",
    "timestamp": <timestamp>,
    "signature": "<signature>"
  }'
```

### 4. Verify Response

Expected:
```json
{
  "callback_url": "http://localhost:8000/oauth/callback/<session-id>",
  "expires_in": 600,
  "provider": "openrouter"
}
```

## Security Improvements

### ✅ Removed from Git
- OAuth secrets no longer in any git-tracked files
- Build-time injection via secrets.properties (gitignored)
- Native C++ with compile-time definitions

### ✅ Per-Application Security
- Each app has its own secret
- Domain validation per application
- Deep link scheme validation
- Prevents cross-application session hijacking

### ✅ Enhanced Signature
- Includes deep link URL in signature
- Prevents redirect attacks
- Timestamp validation (5-minute window)
- HMAC-SHA256 with constant-time comparison

### ✅ Request Validation
- Host header validation
- Domain whitelist per application
- Provider whitelist
- Session expiration and single-use enforcement

## Deployment Checklist

### FastAPI Service

- [ ] Pull latest code
- [ ] Remove `DOMAIN_NAME` from `.env`
- [ ] Remove `SHARED_SECRET` from `.env`
- [ ] Restart service (auto-migration runs)
- [ ] Update sample application secret in database
- [ ] Update mobile app `secrets.properties`
- [ ] Test OAuth flow
- [ ] Monitor logs for errors

### AWS Lambda

- [ ] Generate production secret
- [ ] Update `APPLICATIONS` environment variable
- [ ] Remove `DOMAIN_NAME` environment variable
- [ ] Remove `SHARED_SECRET` environment variable
- [ ] Remove `DEEP_LINK_SCHEME` environment variable
- [ ] Deploy updated Lambda functions
- [ ] Update mobile app `secrets.properties`
- [ ] Test OAuth flow
- [ ] Monitor CloudWatch logs

### Mobile App

- [ ] Update signature generation to include deep_link_url
- [ ] Update API request to include deep_link_url field
- [ ] Update secrets.properties with new secret
- [ ] Rebuild app with updated secrets
- [ ] Test OAuth flows
- [ ] Deploy updated app

## Documentation

- ✅ `MULTI_APP_MIGRATION.md` - FastAPI migration guide
- ✅ `LAMBDA_DEPLOYMENT.md` - Lambda deployment guide
- ✅ `OAUTH_SECRET_MANAGEMENT.md` - Android secret management (existing)
- ✅ `OAUTH_QUICK_START.md` - Quick start guide (existing)

## Next Steps

### Immediate
1. Update Android app signature generation
2. Test with development environment
3. Update production secrets
4. Deploy to production

### Future Enhancements
1. Admin API for managing applications
2. Web dashboard for application management
3. Application usage analytics
4. Automated secret rotation
5. AWS Secrets Manager integration for Lambda
6. DynamoDB table for Lambda applications (instead of env var)
7. Multi-region deployment support

## Support

For issues or questions:
1. Check troubleshooting sections in migration guides
2. Review CloudWatch/application logs
3. Verify signature generation matches specification
4. Test with development environment first

## Summary

✅ **FastAPI service** - Fully updated and documented
✅ **AWS Lambda** - Fully updated and documented
✅ **Multi-application support** - Complete
✅ **Security improvements** - Implemented
✅ **Documentation** - Comprehensive guides created
⏳ **Mobile app updates** - Ready for implementation
⏳ **Deployment** - Ready to deploy

The OAuth callback service now supports multiple applications with individual security configurations, removing all hardcoded secrets and enabling flexible multi-tenant deployment.
