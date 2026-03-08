# Multi-App Identification Method Support

## Summary

The OAuth callback service has been updated to support two application identification methods, allowing it to be hosted on a single domain (e.g., `oauth.a42r.com`) for multiple applications.

## Changes Made

### 1. Configuration (`app/config.py`)
- Added `app_identification_method` setting (default: `"domain"`)
- Options: `"domain"` or `"appid"`

### 2. Request Schema (`app/models/schemas.py`)
- Added optional `app_id` field to `RegisterRequest`
- Required when `APP_IDENTIFICATION_METHOD=appid`

### 3. Registration Endpoint (`app/routers/oauth.py`)
- Added dual identification logic:
  - **App ID method**: Lookup by `app_id` from request body
  - **Domain method**: Lookup by request hostname (backward compatible)
- All existing validation remains (deep link scheme, signature, timestamp)

### 4. Documentation
- Updated `API_DOCUMENTATION.md` with identification method details
- Updated `.env.example` with configuration examples
- Created `register_app_example.sql` with SQL examples

## Identification Methods

### Domain-Based (Default - Backward Compatible)
```env
APP_IDENTIFICATION_METHOD=domain
```

**When to use:**
- Each app has its own subdomain
- Example: `pevplugshare.a42r.com`, `osmand.a42r.com`
- Apps identified by request hostname
- No `app_id` needed in requests

**Database setup:**
```sql
INSERT INTO applications VALUES (
    'pevplugshare',
    'PEV PlugShare',
    'pevplugshare.a42r.com,localhost:8000',  -- Unique domains
    'pevplugshare://oauth/callback',
    'secret1',
    1
);
```

### App ID-Based (New Feature)
```env
APP_IDENTIFICATION_METHOD=appid
```

**When to use:**
- Single domain for all apps
- Example: All apps use `oauth.a42r.com`
- Apps identified by `app_id` in request
- Requires `app_id` in all requests

**Database setup:**
```sql
-- Multiple apps sharing oauth.a42r.com
INSERT INTO applications VALUES (
    'pevplugshare',
    'PEV PlugShare',
    'oauth.a42r.com',  -- Shared domain
    'pevplugshare://oauth/callback',
    'secret1',
    1
);

INSERT INTO applications VALUES (
    'com.a42r.eucosmandplugin',
    'OSMAnd Range Estimation',
    'oauth.a42r.com',  -- Same domain
    'osmeucworld://oauth/callback',
    'secret2',
    1
);
```

**Request example:**
```json
{
  "provider": "openrouter",
  "deep_link_url": "osmeucworld://oauth/callback",
  "timestamp": 1700000000,
  "signature": "abc123...",
  "app_id": "com.a42r.eucosmandplugin"
}
```

## Registering the OSMAnd App

### Step 1: Set identification method
Edit `.env` on the server:
```env
APP_IDENTIFICATION_METHOD=appid
```

### Step 2: Register the app in database
```bash
sqlite3 oauth-callback-service/data/oauth_sessions.db
```

```sql
INSERT INTO applications (
    app_id,
    name,
    allowed_domains,
    deep_link_scheme,
    shared_secret,
    is_active,
    created_at,
    updated_at
) VALUES (
    'com.a42r.eucosmandplugin',
    'OSMAnd Range Estimation',
    'oauth.a42r.com',
    'osmeucworld://oauth/callback',
    'ibEVy3hSw-T1XEMMbjpC...',  -- Use actual secret
    1,
    datetime('now'),
    datetime('now')
);
```

### Step 3: Update existing pevplugshare app
```sql
UPDATE applications 
SET allowed_domains = 'oauth.a42r.com'
WHERE app_id = 'pevplugshare';
```

### Step 4: Restart the service
```bash
cd oauth-callback-service
docker-compose restart
# OR
./run.sh
```

### Step 5: Update client apps
Both apps must now include `app_id` in their registration requests to `oauth.a42r.com`.

## Security Validation

Both methods maintain full security:
- ✅ HMAC-SHA256 signature verification with app-specific secret
- ✅ Timestamp validation (±5 minutes)
- ✅ Deep link scheme validation against registered scheme
- ✅ Per-app secret isolation
- ✅ Rate limiting per IP

## Backward Compatibility

The default `domain` method maintains 100% backward compatibility. Existing deployments continue working without changes.

## Testing

### Test domain-based (existing behavior):
```bash
curl -X POST https://pevplugshare.a42r.com/oauth/register \
  -H "Content-Type: application/json" \
  -d '{
    "provider": "openrouter",
    "deep_link_url": "pevplugshare://oauth/callback",
    "timestamp": 1700000000,
    "signature": "..."
  }'
```

### Test app ID-based (new feature):
```bash
curl -X POST https://oauth.a42r.com/oauth/register \
  -H "Content-Type: application/json" \
  -d '{
    "provider": "openrouter",
    "deep_link_url": "osmeucworld://oauth/callback",
    "timestamp": 1700000000,
    "signature": "...",
    "app_id": "com.a42r.eucosmandplugin"
  }'
```

## Files Modified

1. `app/config.py` - Added `app_identification_method` setting
2. `app/models/schemas.py` - Added optional `app_id` field
3. `app/routers/oauth.py` - Added dual identification logic
4. `API_DOCUMENTATION.md` - Documented new feature
5. `.env.example` - Added configuration examples
6. `register_app_example.sql` - Created (new file with SQL examples)
7. `APPID_IDENTIFICATION.md` - Created (this file)

## Migration Path

**Current setup (domain-based):**
- No changes required
- Service continues working as-is

**Migrating to app ID-based:**
1. Add `APP_IDENTIFICATION_METHOD=appid` to `.env`
2. Update all apps in database to share same domain
3. Update client code to include `app_id` in requests
4. Restart service
5. Test all apps

You can also run both methods simultaneously by deploying two instances of the service with different configurations.
