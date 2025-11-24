# OAuth Logging and UI Improvements

**Date:** November 23, 2025

## Issues Fixed

### 1. OAuth Callback Service Logging
**Problem:** The OAuth callback service was not logging registration requests or callbacks, making debugging impossible.

**Root Causes:**
- FastAPI logs were being written to separate log files (`/var/log/supervisor/fastapi.*.log`) instead of console
- The `Application` model uses `name` attribute but code was accessing `app_name`, causing AttributeError
- No service prefixes in logs, making it difficult to distinguish between FastAPI and Cloudflared logs
- Buffered output from `sed` preventing real-time log visibility

**Solutions:**
1. **Updated supervisord.conf** to redirect logs to stdout/stderr:
   - Changed `stderr_logfile` and `stdout_logfile` to `/dev/stderr` and `/dev/stdout`
   - Set `*_logfile_maxbytes=0` to disable file rotation

2. **Created wrapper scripts** for service-prefixed logging:
   - `start-fastapi.sh`: Prefixes all FastAPI logs with `[FASTAPI]`
   - Updated `start-cloudflared.sh`: Prefixes all Cloudflared logs with `[CLOUDFLARED]`
   - Used `stdbuf -oL -eL` to disable buffering for real-time output

3. **Fixed AttributeError in oauth.py** (3 occurrences):
   ```python
   # Before
   "app_name": matching_app.app_name

   # After
   "app_name": matching_app.name
   ```

4. **Updated Dockerfile** to copy `start-fastapi.sh` and make it executable

### 2. Database Credentials Mismatch
**Problem:** OAuth registration was failing with 500 errors because the database had a placeholder secret.

**Solution:**
- Used `manage_db.py` to update the shared secret from `CHANGE_THIS_SECRET` to the actual Android app secret: `bzmtwlZTBpFyR9CfkjD6_AFUCv7Sh1ckZm0LVn6QEi0`
- Command: `docker exec pevplugshare-oauth-callback python /app/manage_db.py update-secret pevplugshare <secret>`

### 3. OAuth Activity UI Enhancements
**Problem:** After successful OAuth, the app redirected to Settings instead of OAuth page, and there was no model selection UI.

**Solutions:**

#### Added Model Selection Card to activity_oauth.xml:
```xml
<com.google.android.material.card.MaterialCardView
    android:id="@+id/cardModelSelection"
    android:visibility="gone" <!-- Shown only when authenticated -->
    ...>
    <Spinner android:id="@+id/spinnerOpenRouterModel" ... />
</com.google.android.material.card.MaterialCardView>
```

#### Updated OAuthActivity.kt:
- Added `TokenManager` injection for accessing model preferences
- Created `setupModelSelection()` method with 12 popular AI models:
  - Claude 3.5 Sonnet, Opus, Sonnet, Haiku
  - GPT-4o, GPT-4 Turbo, GPT-4, GPT-3.5 Turbo
  - Gemini Pro 1.5, Gemini Pro
  - Llama 3.1 70B/8B Instruct
- Connected spinner to `tokenManager.saveSelectedAiModel()` and `getSelectedAiModel()`
- Updated `updateAuthenticationUI()` to show/hide model selection card based on auth status

## Files Modified

### OAuth Callback Service
1. `oauth-callback-service/supervisord.conf` - Redirect logs to console
2. `oauth-callback-service/start-fastapi.sh` - NEW: FastAPI wrapper with [FASTAPI] prefix
3. `oauth-callback-service/start-cloudflared.sh` - Added [CLOUDFLARED] prefix
4. `oauth-callback-service/Dockerfile` - Copy and make start-fastapi.sh executable
5. `oauth-callback-service/app/routers/oauth.py` - Fixed `app_name` → `name` (3 places)

### Android App
1. `app/src/main/res/layout/activity_oauth.xml` - Added model selection card
2. `app/src/main/java/com/a42r/pevplugshare/ui/oauth/OAuthActivity.kt` - Added model selection logic

## Testing

### Docker Logs
You can now monitor specific service logs:
```bash
# All logs
docker logs -f pevplugshare-oauth-callback

# FastAPI only
docker logs -f pevplugshare-oauth-callback | grep FASTAPI

# Cloudflared only
docker logs -f pevplugshare-oauth-callback | grep CLOUDFLARED

# OAuth events
docker logs -f pevplugshare-oauth-callback | grep -E "registration|callback"
```

### OAuth Flow
1. Open app → Menu → "OpenRouter Authentication"
2. Click "Connect to OpenRouter"
3. App registers session with callback service (logs show `[FASTAPI] OAuth registration attempt`)
4. Browser opens for OAuth authorization
5. After authorization, app receives callback via deep link
6. App exchanges code for API key (logs show callback processing)
7. Success message displayed
8. Model selection card appears
9. User selects preferred AI model from dropdown
10. Selection is saved automatically

## Database Status

**Application:** pevplugshare
- App ID: `pevplugshare`
- Allowed Domains: `pevplugshare.a42r.com,pevplugshare-prod.a42r.com,localhost:8000`
- Deep Link Scheme: `pevplugshare://oauth/callback`
- Shared Secret: ✅ Updated (matches Android app)
- Status: Active ✅

## Next Steps

1. **Rebuild OAuth callback container** (if not already done):
   ```bash
   cd oauth-callback-service
   docker build -t oauth-callback-service .
   docker stop pevplugshare-oauth-callback
   docker rm pevplugshare-oauth-callback
   docker run -d --name pevplugshare-oauth-callback --env-file .env -v /tmp/pevplugshare-oauth-callback:/app/data oauth-callback-service:latest
   ```

2. **Test OAuth flow** with proper logging now active

3. **Monitor logs** to verify registration and callback events are being logged

4. **Verify model selection** persists across app restarts
