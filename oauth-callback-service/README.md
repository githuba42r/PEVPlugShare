# OAuth Callback Service

A generic OAuth 2.0 callback handler service for mobile applications. Solves the problem of OAuth providers requiring publicly accessible callback URLs by providing a session-based redirect service.

## Features

- ✅ **Generic OAuth 2.0 Support**: Works with any OAuth provider (OpenRouter, GitHub, Google, etc.)
- 🔒 **HMAC-SHA256 Signature Verification**: Secure request authentication
- ⚡ **Rate Limiting**: Configurable per-minute/hour/day limits
- 💾 **SQLite Database**: Persistent session storage with auto-cleanup
- 🔄 **Automatic Session Expiration**: TTL-based cleanup (default 10 minutes)
- 📊 **Health Monitoring**: Built-in health check endpoint
- 🎨 **Beautiful Error Pages**: User-friendly HTML responses
- 📖 **Interactive API Docs**: Swagger UI and ReDoc included
- 🌐 **CORS Support**: Configurable allowed origins
- 🖥️ **Web Management UI**: Browser-based application management interface
- 🔐 **Authelia Integration**: Secure web UI access with single sign-on

## Quick Start

### 1. Install Dependencies

```bash
cd oauth-callback-service
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
pip install -r requirements.txt
```

### 2. Configure Environment

Copy the example environment file and edit it:

```bash
cp .env.example .env
```

Edit `.env` and set your configuration:

```env
# Database configuration
DATABASE_URL=sqlite:///./data/oauth_sessions.db

# Optional settings (see Configuration Reference)
SESSION_TTL_MINUTES=10
LOG_LEVEL=INFO
```

**Note**: Domain names and secrets are now configured **per-application in the database**, not via environment variables. See [Application Management](#application-management) below.

### 3. Run the Service

**Development mode** (with auto-reload):
```bash
python -m app.main
```

Or with uvicorn directly:
```bash
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

**Production mode**:
```bash
uvicorn app.main:app --host 0.0.0.0 --port 8000 --workers 4
```

### 4. Verify Installation

Open your browser and check:
- Service info: http://localhost:8000/
- Health check: http://localhost:8000/health
- API docs: http://localhost:8000/docs
- ReDoc: http://localhost:8000/redoc

## Docker Deployment

### Quick Start with Docker

**1. Build the Docker image**:
```bash
# Build locally
./build.sh

# Force rebuild without cache
./build.sh --force

# Or build and push to registry (requires authentication)
./build.sh --push

# Build with version tag
./build.sh --push v1.1.0

# Force rebuild and push (e.g., after dependency updates)
./build.sh --force --push
```

**Alternative: Build with docker command directly**:
```bash
docker build -t oauth-callback-service .

# Force rebuild without cache
docker build --no-cache -t oauth-callback-service .
```

**2. Run the container**:
```bash
docker run -d \
  --name oauth-callback \
  -p 8000:8000 \
  -v oauth-data:/app/data \
  oauth-callback-service
```

**Note**: The service will start with a sample application. You must configure your applications in the database - see [Application Management](#application-management).

**3. Configure your application** (first time setup):

**Method 1: Using Python script** (recommended for Docker):
```bash
# Access the container
docker exec -it oauth-callback bash

# List all applications
python manage_db.py list

# Update the sample application
python manage_db.py update-domains pevplugshare "your-domain.com,your-other-domain.com"

# Generate and update secret
python manage_db.py update-secret pevplugshare

# Or specify your own secret
python manage_db.py update-secret pevplugshare "your-generated-secret-here"
```

**Method 2: Using sqlite3** (if available):
```bash
# Access the container
docker exec -it oauth-callback bash

# Update the sample application with your settings
sqlite3 /app/data/oauth_sessions.db
```

```sql
UPDATE applications
SET
  allowed_domains = 'your-domain.com,your-other-domain.com',
  shared_secret = 'your-generated-secret-here'
WHERE app_id = 'pevplugshare';
```

**3. Check logs**:
```bash
docker logs -f oauth-callback
```

**4. Stop/start**:
```bash
docker stop oauth-callback
docker start oauth-callback
```

### Using Docker Compose (Recommended)

**1. Create `.env` file**:
```bash
cp .env.example .env
# Edit .env if needed (most settings have sensible defaults)
```

**2. Start the service**:
```bash
docker-compose up -d
```

**3. Configure your application** (first time):

**Method 1: Using Python script** (recommended):
```bash
# Update the database
docker exec oauth-callback-service python manage_db.py update-domains pevplugshare "your-domain.com"

# Update secret (generates new one automatically)
docker exec oauth-callback-service python manage_db.py update-secret pevplugshare
```

**Method 2: Using sqlite3**:
```bash
# Generate a secure secret
python -c "import secrets; print(secrets.token_urlsafe(32))"

# Update the database
docker exec -it oauth-callback-service sqlite3 /app/data/oauth_sessions.db
```

```sql
UPDATE applications
SET
  allowed_domains = 'your-domain.com',
  shared_secret = 'your-generated-secret-here'
WHERE app_id = 'pevplugshare';
```

**3. View logs**:
```bash
docker-compose logs -f
```

**4. Stop the service**:
```bash
docker-compose down
```

**5. Stop and remove data**:
```bash
docker-compose down -v  # Removes volumes (database)
```

## Cloudflare Tunnel Integration

The service includes **optional Cloudflare Tunnel support** for automatic HTTPS public access without port forwarding or complex networking.

### Why Use Cloudflare Tunnel?

- ✅ **No Port Forwarding**: No need to expose ports on your router
- ✅ **Automatic HTTPS**: Free SSL/TLS certificates from Cloudflare
- ✅ **DDoS Protection**: Built-in Cloudflare security
- ✅ **Global CDN**: Fast access from anywhere in the world
- ✅ **Zero Configuration**: Works behind NAT, firewalls, CGNAT
- ✅ **Free Tier Available**: Perfect for personal projects

### Setting Up Cloudflare Tunnel

**1. Create a Cloudflare Account**:
- Go to https://dash.cloudflare.com and sign up
- Add your domain (or use a free Cloudflare subdomain)

**2. Create a Connector**:
- Go to **Zero Trust** → **Networks** → **Connectors**
- Click **Create a connector**
- Choose **Cloudflared** (not WARP Connector)
  - **Cloudflared**: For hosting services (HTTP/HTTPS) - ✅ Use this
  - **WARP Connector**: For accessing private networks - ❌ Not for this use case
- Click **Begin setup**
- Name your connector: `oauth-callback`
- Click **Save connector**
- **Copy the tunnel token** from the installation command (starts with `eyJ...`)

**3. Configure Public Hostname**:
- In your connector settings, go to the **Public Hostnames** tab
- Click **Add a public hostname**
- Configure the hostname:
  - **Subdomain**: `oauth` (or your choice)
  - **Domain**: Select your domain from the dropdown
  - **Service Type**: HTTP
  - **Service URL**: `localhost:8000`
- Click **Save hostname**

**4. Enable Tunnel in Docker**:

Edit your `.env` file:
```env
# Enable Cloudflare Tunnel
CLOUDFLARE_TUNNEL_ENABLED=true
CLOUDFLARE_TUNNEL_TOKEN=eyJhIjoiYmM0ZTM5ZjQwNjNmNDM4YWI5OTI3ZGE5ZjMyYzk3OGEiLCJ0IjoiOTZmYzA5NDAtYjNmYy00MTljLWI3YTMtNjk2ZGQwMjQxNDQ5IiwicyI6Ik9XVmxOelE1Wm1FdE56SmhZaTAwTkRjeExXSTFNV1V0TnpFek1UQTVNR1JpT1RCaiJ9
```

Then update your application's allowed domains in the database:
```bash
docker exec -it oauth-callback-service sqlite3 /app/data/oauth_sessions.db
```

```sql
UPDATE applications
SET allowed_domains = 'oauth.yourdomain.com,oauth-dev.yourdomain.com'
WHERE app_id = 'pevplugshare';
```

**5. Start the Service**:
```bash
docker-compose up -d
```

**6. Verify Tunnel Connection**:
```bash
# Check logs for cloudflared connection
docker-compose logs -f

# You should see:
# ☁️  Cloudflare Tunnel is ENABLED
# ✓ Cloudflare Tunnel configuration validated
# 🔗 Connecting to Cloudflare Tunnel...
# Connection registered
# 🌱 Sample application 'pevplugshare' initialized
```

**7. Test Public Access**:
```bash
# Test from outside your network
curl https://oauth.yourdomain.com/health
```

### Cloudflare Tunnel Architecture

The Docker container runs **two processes** managed by `supervisord`:
1. **FastAPI** (port 8000) - The OAuth callback service
2. **cloudflared** - Cloudflare Tunnel connector

```
Internet → Cloudflare CDN → Cloudflare Tunnel → Docker Container (FastAPI)
```

When enabled:
- FastAPI starts first and listens on localhost:8000
- cloudflared waits for FastAPI to be ready
- cloudflared connects to Cloudflare's network
- Your public hostname routes to the container

### Process Management with Supervisord

The container uses `supervisord` to manage both processes:

```ini
[program:fastapi]
command=uvicorn app.main:app --host 0.0.0.0 --port 8000
autostart=true
priority=100

[program:cloudflared]
command=/app/start-cloudflared.sh
autostart=false  # Only starts if CLOUDFLARE_TUNNEL_ENABLED=true
priority=200
```

View process status:
```bash
# Access container
docker exec -it oauth-callback-service bash

# Check supervisor status
supervisorctl status

# Output:
# fastapi                          RUNNING   pid 8, uptime 0:05:23
# cloudflared                      RUNNING   pid 15, uptime 0:05:20
```

### Troubleshooting Cloudflare Tunnel

**Tunnel not connecting**:
```bash
# Check cloudflared logs
docker-compose logs -f oauth-callback | grep cloudflared

# Common issues:
# - Invalid token: Check CLOUDFLARE_TUNNEL_TOKEN is complete
# - Token expired: Regenerate token in Cloudflare dashboard
# - Network blocked: Ensure container can reach Cloudflare's servers
```

**FastAPI not accessible via tunnel**:
```bash
# Verify FastAPI is running
docker exec oauth-callback-service curl http://localhost:8000/health

# Check tunnel configuration in Cloudflare dashboard:
# - Service should be: http://localhost:8000
# - NOT http://127.0.0.1:8000
```

**Disable tunnel temporarily**:
```env
# In .env file
CLOUDFLARE_TUNNEL_ENABLED=false
```
```bash
docker-compose restart
```

### Docker Image Details

- **Base**: Python 3.12 slim
- **Additional Tools**: cloudflared, supervisor, curl
- **Size**: ~200MB (with cloudflared)
- **Port**: 8000
- **Health Check**: Built-in HTTP health check every 30s
- **Database**: SQLite stored in `/app/data` volume
- **Logs**: stdout/stderr (view with `docker logs`)
- **Process Manager**: supervisord

### Production Docker Deployment

For production, use docker-compose with proper configuration:

```yaml
# docker-compose.prod.yml
version: '3.8'

services:
  oauth-callback:
    build: .
    container_name: oauth-callback-service
    restart: always
    ports:
      - "127.0.0.1:8000:8000"  # Only expose to localhost
    environment:
      - LOG_LEVEL=WARNING
      - ALLOWED_ORIGINS=https://openrouter.ai
    volumes:
      - oauth-data:/app/data
    healthcheck:
      test: ["CMD", "python", "-c", "import requests; requests.get('http://localhost:8000/health', timeout=2)"]
      interval: 30s
      timeout: 3s
      retries: 3

volumes:
  oauth-data:
```

**Note**: Configure applications in the database, not environment variables. See [Application Management](#application-management).

Run with:
```bash
docker-compose -f docker-compose.prod.yml up -d
```

### Docker + Cloudflare Tunnel

**1. Run OAuth service in Docker**:
```bash
docker-compose up -d
```

**2. Configure application domains**:
```bash
# Update allowed domains to include your tunnel URL
docker exec -it oauth-callback-service sqlite3 /app/data/oauth_sessions.db
```

```sql
UPDATE applications
SET allowed_domains = 'your-tunnel-url.trycloudflare.com,localhost:8000'
WHERE app_id = 'pevplugshare';
```

## Cloudflare Tunnel Setup

### Option 1: Quick Testing with cloudflared

```bash
# Install cloudflared (if not already installed)
# See: https://developers.cloudflare.com/cloudflare-one/connections/connect-apps/install-and-setup/installation/

# Run tunnel (creates temporary public URL)
cloudflared tunnel --url http://localhost:8000
```

This will output a temporary URL like `https://random-name.trycloudflare.com`.

Update your application's allowed domains in the database:
```bash
sqlite3 oauth_sessions.db
```

```sql
UPDATE applications
SET allowed_domains = 'random-name.trycloudflare.com,localhost:8000'
WHERE app_id = 'pevplugshare';
```

### Option 2: Persistent Cloudflare Tunnel

If you already have a Cloudflare Tunnel configured:

1. Add a public hostname in your tunnel config:
   ```yaml
   # config.yml
   ingress:
     - hostname: oauth.your-domain.com
       service: http://localhost:8000
     - service: http_status:404
   ```

2. Update your application's allowed domains:
   ```bash
   sqlite3 oauth_sessions.db
   ```

   ```sql
   UPDATE applications
   SET allowed_domains = 'oauth.your-domain.com'
   WHERE app_id = 'pevplugshare';
   ```

3. Restart your tunnel:
   ```bash
   cloudflared tunnel run your-tunnel-name
   ```

## API Usage

See [API_DOCUMENTATION.md](API_DOCUMENTATION.md) for complete API reference.

### Quick Example

**1. Register a session (Android)**:
```kotlin
val deepLinkUrl = "myapp://oauth/callback"  // Your app's deep link scheme
val timestamp = System.currentTimeMillis() / 1000
val signature = OAuthSignature.generateSignature("openrouter", deepLinkUrl, timestamp, secret)

val request = JSONObject().apply {
    put("provider", "openrouter")
    put("deep_link_url", deepLinkUrl)
    put("timestamp", timestamp)
    put("signature", signature)
}

val response = registerSession(request)
val callbackUrl = response.getString("callback_url")
// Use callbackUrl in OAuth flow
```

**2. Use callback URL in OAuth authorization**:
```kotlin
val authUrl = Uri.parse("https://openrouter.ai/auth").buildUpon()
    .appendQueryParameter("callback_url", callbackUrl)
    .appendQueryParameter("code_challenge", codeChallenge)
    .appendQueryParameter("code_challenge_method", "S256")
    .build()

CustomTabsIntent.Builder().build().launchUrl(context, authUrl)
```

**3. Handle deep link callback**:
```kotlin
override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)
    intent?.data?.let { uri ->
        val code = uri.getQueryParameter("code")
        val provider = uri.getQueryParameter("provider")
        // Exchange code for API key
    }
}
```

## Application Management

The service supports multiple applications through database configuration. Each application has its own allowed domains, deep link scheme, and shared secret.

### Web Management UI

The service includes a **web-based management interface** for easy application configuration without requiring command-line or SQL knowledge.

**Access the Web UI**:
- Local: http://localhost:8001/
- Production: https://oauth.yourdomain.com/admin/ (requires Authelia authentication)

**Features**:
- 📋 **List Applications**: View all configured OAuth applications
- ➕ **Add Application**: Create new application configurations with auto-generated secrets
- ✏️ **Edit Application**: Update domains, schemes, and settings
- 🔄 **Toggle Status**: Enable/disable applications without deletion
- 🔑 **Secret Management**: View and regenerate shared secrets
- 🗑️ **Delete Application**: Remove applications (with confirmation)

**Using the Web UI**:

**1. List Applications**:
- View all configured applications
- See status badges (Active/Inactive)
- Quick overview of allowed domains
- Click "View Details" to see full configuration

**2. Add New Application**:
- Click "Add Application" button
- Fill in required fields:
  - **App ID**: Unique identifier (e.g., `myapp`, `mobile-client`)
  - **Display Name**: Human-readable name (e.g., "My Application")
  - **Allowed Domains**: Comma-separated list (e.g., `myapp.com,localhost:8000`)
  - **Deep Link Scheme**: Your app's URL scheme (e.g., `myapp://oauth/callback`)
- Secret is auto-generated (cryptographically secure)
- Click "Add Application" to save

**3. Edit Application**:
- Click "Edit" on any application
- Update domains, scheme, or name
- Changes take effect immediately
- Click "Update Application" to save

**4. View/Copy Secrets**:
- Secrets are hidden by default (shown as `••••••••`)
- Click "View Secret" to reveal
- Click "Copy" to copy to clipboard
- **Important**: Store secrets securely, only visible in Web UI

**5. Regenerate Secret**:
- Click "Regenerate Secret" on any application
- Generates new cryptographically secure secret
- **Warning**: Old secret stops working immediately
- Update your mobile app before regenerating!

**6. Enable/Disable Application**:
- Toggle "Active/Inactive" button
- Disabled apps reject all OAuth requests
- Useful for maintenance or security incidents
- No data is deleted, easily re-enable later

**7. Delete Application**:
- Click "Delete" button
- Confirmation dialog prevents accidents
- **Permanent**: Cannot be undone
- Active sessions continue until expiration

**8. View Sessions** (if implemented):
- See recent OAuth sessions
- Filter by application
- Monitor usage patterns
- Identify potential issues

The Web UI provides:
- Clean, modern interface with responsive design
- No database knowledge required
- One-click secret generation and copying
- Real-time status updates
- Application statistics and session monitoring
- Responsive design (works on mobile/tablet)

**Note**: In production deployments with Traefik + Authelia, the Web UI is protected by authentication while the OAuth API endpoints remain publicly accessible.

### Traefik + Authelia Integration

The OAuth2 redirector service is designed to work seamlessly with **Traefik** reverse proxy and **Authelia** authentication middleware, providing a secure architecture where:

- **OAuth API endpoints** remain publicly accessible (required for OAuth flows)
- **Web Management UI** is protected by authentication

#### Architecture Overview

```
Internet → Traefik Reverse Proxy → Routes split by path:
                                  ├─ /health          → FastAPI (port 8000) - Public
                                  ├─ /oauth/*         → FastAPI (port 8000) - Public  
                                  ├─ /                → FastAPI (port 8000) - Public
                                  └─ /admin/*         → Authelia → Flask (port 8001) - Protected
```

**Why This Design?**
- OAuth callbacks MUST be publicly accessible (OAuth providers need to reach them)
- Administrative functions should be protected with authentication
- Single container runs both services on different ports
- Traefik routes based on URL path and forwards to appropriate service

#### Deployment with Docker Compose

Use the provided `docker-compose.production.yml` for production deployments with Traefik:

```bash
# Deploy with Traefik labels
docker-compose -f docker-compose.production.yml up -d
```

**Key Traefik labels configured**:

```yaml
# OAuth API - Public endpoints (port 8000)
- traefik.http.routers.oauth-api.rule=Host(`oauth.yourdomain.com`) && (Path(`/health`) || PathPrefix(`/oauth`) || Path(`/`))
- traefik.http.routers.oauth-api.priority=100
- traefik.http.services.oauth-api-svc.loadbalancer.server.port=8000

# Web UI - Protected by Authelia (port 8001)
- traefik.http.routers.oauth-webui.rule=Host(`oauth.yourdomain.com`) && PathPrefix(`/admin`)
- traefik.http.routers.oauth-webui.priority=110  # Higher priority to catch /admin before /
- traefik.http.routers.oauth-webui.middlewares=authelia@docker,oauth-webui-stripprefix
- traefik.http.middlewares.oauth-webui-stripprefix.stripprefix.prefixes=/admin
- traefik.http.services.oauth-webui-svc.loadbalancer.server.port=8001
```

**How it works**:
1. **Priority routing**: `/admin/*` routes have priority 110 (caught first)
2. **Authentication**: `authelia@docker` middleware protects `/admin/*` routes
3. **Path rewriting**: `stripprefix` removes `/admin` before forwarding to Flask (e.g., `/admin/` → `/`)
4. **Port mapping**: Traefik forwards to port 8001 for Web UI, port 8000 for OAuth API

#### Authelia Configuration

Add this configuration to your Authelia `configuration.yml`:

```yaml
access_control:
  default_policy: deny
  rules:
    # OAuth API - Public (bypass authentication)
    - domain: oauth.yourdomain.com
      policy: bypass
      resources:
        - "^/health$"           # Health check endpoint
        - "^/oauth/.*$"         # All OAuth callback endpoints
        - "^/$"                 # Root API info endpoint
    
    # Web Management UI - Require authentication
    - domain: oauth.yourdomain.com
      policy: one_factor       # or two_factor for higher security
      resources:
        - "^/admin.*$"          # All admin UI paths
```

**Security levels**:
- `bypass` - No authentication required (OAuth API must be public)
- `one_factor` - Username + password required (recommended for Web UI)
- `two_factor` - Username + password + 2FA required (for high-security environments)

**Important notes**:
- OAuth endpoints MUST use `bypass` policy (OAuth providers need public access)
- Regex patterns use `^` and `$` anchors for exact matching
- Test carefully to ensure OAuth flow isn't blocked

#### Verifying the Setup

**Test public OAuth API** (should work without authentication):
```bash
# Health check
curl https://oauth.yourdomain.com/health

# Root endpoint
curl https://oauth.yourdomain.com/

# These should return JSON, not redirect to login
```

**Test protected Web UI** (should require Authelia login):
```bash
# Try accessing admin without auth
curl https://oauth.yourdomain.com/admin/

# Should either:
# - Redirect to Authelia login page (302)
# - Return 401/403 Unauthorized
```

**Test OAuth flow** (end-to-end test):
1. Use your mobile app to initiate OAuth
2. Verify the OAuth provider can reach the callback URL
3. Confirm callback redirects properly to your app

#### Troubleshooting

**OAuth callbacks are being blocked**:
```bash
# Check Authelia logs
docker logs authelia

# Verify bypass rules are configured correctly
# Ensure Traefik is forwarding to correct port (8000 for OAuth API)

# Test direct access (bypass Traefik)
docker exec oauth-callback-service curl http://localhost:8000/health
```

**Web UI accessible without authentication**:
```bash
# Check Traefik labels
docker inspect oauth-callback-service | grep -A 5 "oauth-webui"

# Verify authelia@docker middleware exists
docker exec traefik cat /etc/traefik/traefik.yml

# Check middleware is applied
curl -I https://oauth.yourdomain.com/admin/
# Should see: X-Forwarded-User header or redirect to Authelia
```

**404 errors on /admin paths**:
```bash
# Verify both services are running
docker exec oauth-callback-service supervisorctl status
# Should show: fastapi RUNNING, webui RUNNING

# Check Web UI is listening
docker exec oauth-callback-service curl http://localhost:8001/

# Verify stripprefix middleware is working
# Flask expects / but receives /admin/ - stripprefix removes /admin
```

**Port conflicts**:
```bash
# Verify both ports are exposed
docker port oauth-callback-service
# Should show: 8000/tcp -> ..., 8001/tcp -> ...

# Check if ports are in use
netstat -tlnp | grep -E '8000|8001'
```

#### Security Considerations

**1. Never protect OAuth endpoints with authentication**:
```yaml
# ❌ WRONG - This will break OAuth flows
- domain: oauth.yourdomain.com
  policy: one_factor  # Don't do this!
  resources:
    - "^/oauth/.*$"

# ✅ CORRECT - OAuth endpoints must be public
- domain: oauth.yourdomain.com
  policy: bypass
  resources:
    - "^/oauth/.*$"
```

**2. Always protect admin endpoints**:
```yaml
# ✅ CORRECT - Admin UI requires authentication
- domain: oauth.yourdomain.com
  policy: one_factor
  resources:
    - "^/admin.*$"
```

**3. Use HTTPS in production**:
- Traefik should handle TLS termination
- Configure Let's Encrypt for automatic certificates
- Never expose OAuth service directly without HTTPS

**4. Rate limiting**:
- OAuth API has built-in rate limits (configurable)
- Consider adding Traefik rate limiting for additional protection
- Monitor logs for abuse patterns

**5. Monitoring**:
```bash
# Watch for authentication failures
docker logs authelia | grep "authentication"

# Monitor OAuth API usage
docker logs oauth-callback-service | grep "POST /oauth"

# Check for suspicious activity
docker logs oauth-callback-service | grep "429\|403\|401"
```

#### Alternative Deployment Without Authelia

If you don't use Authelia, consider:

**Option 1: Network isolation**
```yaml
# docker-compose.yml
services:
  oauth-callback:
    networks:
      - public    # OAuth API accessible
      - internal  # Web UI only on internal network
```

**Option 2: Firewall rules**
```bash
# Allow OAuth API (port 8000) from internet
iptables -A INPUT -p tcp --dport 8000 -j ACCEPT

# Allow Web UI (port 8001) only from specific IPs
iptables -A INPUT -p tcp --dport 8001 -s 192.168.1.0/24 -j ACCEPT
iptables -A INPUT -p tcp --dport 8001 -j DROP
```

**Option 3: VPN access for Web UI**
- Expose port 8000 publicly for OAuth
- Keep port 8001 on localhost only
- Access Web UI via VPN or SSH tunnel

```bash
# SSH tunnel to access Web UI
ssh -L 8001:localhost:8001 user@oauth-server

# Then access locally at http://localhost:8001/
```

### Database Management Methods

Choose the method that works best for your workflow:

**Method 1: Web UI** (Easiest - Recommended for most users)

**Method 1: Python Script** (Recommended for Docker)

A Python-based database management tool is included that doesn't require sqlite3:

```bash
# In Docker
docker exec oauth-callback-service python manage_db.py <command>

# Locally
python manage_db.py <command>
```

**Method 2: Direct SQL**

Use sqlite3 command-line tool for direct database access:

```bash
# In Docker
docker exec -it oauth-callback-service sqlite3 /app/data/oauth_sessions.db

# Locally
sqlite3 oauth_sessions.db
```

### View All Applications

**Using Python script**:
```bash
docker exec oauth-callback-service python manage_db.py list
```

**Using SQL**:
**Local**:
```bash
sqlite3 oauth_sessions.db "SELECT app_id, name, allowed_domains, is_active FROM applications;"
```

**Docker**:
```bash
docker exec oauth-callback-service sqlite3 /app/data/oauth_sessions.db \
  "SELECT app_id, name, allowed_domains, is_active FROM applications;"
```

### Add a New Application

**Using Python script**:
```bash
# Generates secret automatically
docker exec oauth-callback-service python manage_db.py add \
  myapp \
  "My Application" \
  "myapp.example.com,myapp-dev.example.com" \
  "myapp://oauth/callback"

# Or specify your own secret
docker exec oauth-callback-service python manage_db.py add \
  myapp \
  "My Application" \
  "myapp.example.com" \
  "myapp://oauth/callback" \
  "your-secret-here"
```

**Using SQL**:

**1. Generate a secure secret**:
```bash
python -c "import secrets; print(secrets.token_urlsafe(32))"
```

**2. Add to database**:

**Local**:
```bash
sqlite3 oauth_sessions.db
```

**Docker**:
```bash
docker exec -it oauth-callback-service sqlite3 /app/data/oauth_sessions.db
```

**SQL**:
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
  'myapp',                                    -- Unique app ID
  'My Application',                           -- Display name
  'myapp.example.com,myapp-dev.example.com', -- Comma-separated domains
  'myapp://oauth/callback',                  -- Deep link scheme
  'YOUR_GENERATED_SECRET_HERE',              -- Secret from step 1
  1,                                         -- Active (1) or inactive (0)
  datetime('now'),
  datetime('now')
);
```

**3. Verify**:
```sql
SELECT * FROM applications WHERE app_id = 'myapp';
```

**Quick SQL Example** (copy-paste ready):
```sql
-- Generate secret first with: python -c "import secrets; print(secrets.token_urlsafe(32))"

INSERT INTO applications (
  app_id, name, allowed_domains, deep_link_scheme, shared_secret,
  is_active, created_at, updated_at
) VALUES (
  'myapp',
  'My Application',
  'myapp.example.com,localhost:8000',
  'myapp://oauth/callback',
  'REPLACE_WITH_GENERATED_SECRET',
  1,
  datetime('now'),
  datetime('now')
);
```

### Update an Existing Application

**Using Python script**:

**Update allowed domains**:
```bash
docker exec oauth-callback-service python manage_db.py update-domains \
  pevplugshare \
  "new-domain.com,other-domain.com"
```

**Update shared secret** (generates automatically):
```bash
docker exec oauth-callback-service python manage_db.py update-secret pevplugshare
```

**Using SQL**:

**Update allowed domains**:
```sql
UPDATE applications
SET
  allowed_domains = 'new-domain.com,other-domain.com',
  updated_at = datetime('now')
WHERE app_id = 'pevplugshare';
```

**Update shared secret** (secret rotation):
```sql
UPDATE applications
SET
  shared_secret = 'NEW_SECURE_SECRET_HERE',
  updated_at = datetime('now')
WHERE app_id = 'pevplugshare';
```

**Important**: After updating the secret, you must rebuild and redeploy your mobile app with the new secret.

**Update deep link scheme**:
```sql
UPDATE applications
SET
  deep_link_scheme = 'newscheme://oauth/callback',
  updated_at = datetime('now')
WHERE app_id = 'pevplugshare';
```

**Update display name**:
```sql
UPDATE applications
SET
  name = 'New Application Name',
  updated_at = datetime('now')
WHERE app_id = 'pevplugshare';
```

### Temporarily Disable an Application

**Using Python script**:

**Disable** (reject all requests):
```bash
docker exec oauth-callback-service python manage_db.py disable myapp
```

**Re-enable**:
```bash
docker exec oauth-callback-service python manage_db.py enable myapp
```

**Using SQL**:

**Disable** (reject all requests):
```sql
UPDATE applications
SET
  is_active = 0,
  updated_at = datetime('now')
WHERE app_id = 'myapp';
```

**Re-enable**:
```sql
UPDATE applications
SET
  is_active = 1,
  updated_at = datetime('now')
WHERE app_id = 'myapp';
```

When disabled, requests will receive:
```json
{
  "error": "unknown_application",
  "message": "No application configured for domain: myapp.example.com"
}
```

### Remove an Application

**Using Python script**:
```bash
# Will prompt for confirmation
docker exec -it oauth-callback-service python manage_db.py delete myapp
```

**Using SQL**:

**Caution**: This permanently deletes the application configuration.

```sql
-- First, check for active sessions
SELECT COUNT(*) FROM oauth_sessions WHERE app_id = 'myapp';

-- Delete the application
DELETE FROM applications WHERE app_id = 'myapp';
```

**Note**: Existing sessions will continue to work until they expire, but no new sessions can be created.

### Application Configuration Examples

**Development + Production Domains**:
```sql
UPDATE applications
SET allowed_domains = 'myapp.com,dev.myapp.com,localhost:8000'
WHERE app_id = 'myapp';
```

**Multiple Domains** (e.g., different regional domains):
```sql
UPDATE applications
SET allowed_domains = 'myapp.com,myapp.co.uk,myapp.eu,myapp.asia'
WHERE app_id = 'myapp';
```

**With Cloudflare Tunnel**:
```sql
UPDATE applications
SET allowed_domains = 'random-name.trycloudflare.com,myapp.com'
WHERE app_id = 'myapp';
```

### Python Management Script Reference

The `manage_db.py` script provides a user-friendly interface for database management:

**List all applications**:
```bash
docker exec oauth-callback-service python manage_db.py list
```

**Show detailed application info**:
```bash
docker exec oauth-callback-service python manage_db.py show pevplugshare
```

**View recent sessions**:
```bash
# All sessions
docker exec oauth-callback-service python manage_db.py sessions

# Sessions for specific app
docker exec oauth-callback-service python manage_db.py sessions pevplugshare
```

**Complete command reference**:
```bash
docker exec oauth-callback-service python manage_db.py
```

This will display all available commands and their usage.

### Security Best Practices

**1. Secret Generation**:
```bash
# Always use cryptographically secure secrets
python -c "import secrets; print(secrets.token_urlsafe(32))"

# NOT secure (don't use):
# - Simple passwords
# - Dictionary words
# - Predictable patterns
```

**2. Domain Validation**:
- Only add domains you control
- Use exact domain matches (no wildcards)
- Include port numbers if not using 80/443
- Test with `localhost:8000` for development

**3. Secret Rotation**:
```sql
-- Rotate secrets periodically (e.g., every 90 days)
UPDATE applications
SET shared_secret = 'NEW_SECRET', updated_at = datetime('now')
WHERE app_id = 'myapp';
```

After rotation:
1. Update mobile app with new secret
2. Test thoroughly
3. Deploy updated mobile app
4. Old sessions continue to work until expiration

**4. Monitoring**:
```sql
-- Check recent sessions by application
SELECT app_id, COUNT(*) as session_count, MAX(created_at) as last_session
FROM oauth_sessions
GROUP BY app_id;

-- Find applications with no recent activity
SELECT app_id, name, updated_at
FROM applications
WHERE app_id NOT IN (
  SELECT DISTINCT app_id FROM oauth_sessions
  WHERE created_at > datetime('now', '-7 days')
);
```

### Database Schema

```sql
-- Applications table
CREATE TABLE applications (
  app_id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  allowed_domains TEXT NOT NULL,     -- Comma-separated list
  deep_link_scheme TEXT NOT NULL,
  shared_secret TEXT NOT NULL,
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL,
  is_active INTEGER NOT NULL DEFAULT 1
);

-- Sessions table (references applications)
CREATE TABLE oauth_sessions (
  session_id TEXT PRIMARY KEY,
  app_id TEXT NOT NULL,              -- Foreign key to applications
  provider TEXT NOT NULL,
  deep_link_url TEXT NOT NULL,
  created_at INTEGER NOT NULL,
  expires_at INTEGER NOT NULL,
  used INTEGER NOT NULL DEFAULT 0,
  client_ip TEXT,
  user_agent TEXT,
  FOREIGN KEY (app_id) REFERENCES applications(app_id)
);
```

### Backup and Restore

**Backup**:
```bash
# Local
cp oauth_sessions.db oauth_sessions.backup-$(date +%Y%m%d).db

# Docker
docker cp oauth-callback-service:/app/data/oauth_sessions.db \
  ./oauth_sessions.backup-$(date +%Y%m%d).db
```

**Restore**:
```bash
# Local
cp oauth_sessions.backup-20241123.db oauth_sessions.db

# Docker
docker cp ./oauth_sessions.backup-20241123.db \
  oauth-callback-service:/app/data/oauth_sessions.db
docker-compose restart
```

**Export applications only** (for migration):
```bash
sqlite3 oauth_sessions.db <<EOF
.mode insert applications
SELECT * FROM applications;
EOF
```

## Project Structure

```
oauth-callback-service/
├── app/
│   ├── __init__.py
│   ├── main.py              # FastAPI application
│   ├── config.py            # Configuration management
│   ├── models/
│   │   ├── __init__.py
│   │   ├── database.py      # SQLAlchemy models
│   │   └── schemas.py       # Pydantic schemas
│   ├── routers/
│   │   ├── __init__.py
│   │   ├── oauth.py         # OAuth endpoints
│   │   └── health.py        # Health check endpoints
│   └── utils/
│       ├── __init__.py
│       ├── security.py      # HMAC signature utilities
│       └── rate_limit.py    # Rate limiting logic
├── .env.example             # Example environment file
├── .env.docker              # Docker-specific environment example
├── .gitignore
├── requirements.txt         # Python dependencies
├── Dockerfile               # Docker image definition
├── docker-compose.yml       # Docker Compose configuration
├── run.sh                   # Quick start script
├── test_service.py          # Test suite
├── API_DOCUMENTATION.md     # Complete API reference
└── README.md               # This file
```

## Configuration Reference

### Environment Variables

| Variable | Type | Default | Description |
|----------|------|---------|-------------|
| `HOST` | string | `0.0.0.0` | Server bind address |
| `PORT` | integer | `8000` | Server port |
| `DATABASE_URL` | string | `sqlite:///./oauth_sessions.db` | Database connection string |
| `SESSION_TTL_MINUTES` | integer | `10` | Session expiration time |
| `CLEANUP_INTERVAL_MINUTES` | integer | `5` | Database cleanup interval |
| `RATE_LIMIT_PER_MINUTE` | integer | `10` | Requests per minute per IP |
| `RATE_LIMIT_PER_HOUR` | integer | `100` | Requests per hour per IP |
| `RATE_LIMIT_PER_DAY` | integer | `1000` | Requests per day per IP |
| `ALLOWED_ORIGINS` | string | `*` | CORS allowed origins (comma-separated) |
| `OAUTH_PROVIDERS` | string | `openrouter,github,google` | Supported providers (comma-separated) |
| `LOG_LEVEL` | string | `INFO` | Logging level (DEBUG, INFO, WARNING, ERROR) |
| `CLOUDFLARE_TUNNEL_ENABLED` | boolean | `false` | Enable Cloudflare Tunnel integration |
| `CLOUDFLARE_TUNNEL_TOKEN` | string | - | Cloudflare Tunnel token (required if tunnel enabled) |
| `ENABLE_DIAGNOSTIC_ENDPOINTS` | boolean | `false` | Enable /ping and /version endpoints (testing only) |

### Removed Environment Variables

The following variables are **no longer used**. Configuration is now per-application in the database:

- ~~`DOMAIN_NAME`~~ - Now configured per-application in `allowed_domains`
- ~~`SHARED_SECRET`~~ - Now configured per-application in `shared_secret`
- ~~`DEEP_LINK_SCHEME`~~ - Now configured per-application in `deep_link_scheme`

See [Application Management](#application-management) for database configuration.

### Generating Secrets

Generate a cryptographically secure secret for your application's `shared_secret`:

```bash
python -c "import secrets; print(secrets.token_urlsafe(32))"
```

Example output: `xK7pQ9rN2mZ4vB8wC3dE5fG6hJ1kL0mN2oP4qR5sT7uV9wX`

Use this secret when adding or updating applications in the database. See [Application Management](#application-management).

## Security Best Practices

1. **Always use HTTPS**: Never run in production without TLS
2. **Keep secrets secure**: Don't commit `.env` to version control
3. **Use strong secrets**: Generate with `secrets.token_urlsafe(32)`
4. **Enable rate limiting**: Prevent abuse (already enabled by default)
5. **Monitor logs**: Check for suspicious activity
6. **Restrict CORS**: Set specific origins in production:
   ```env
   ALLOWED_ORIGINS=https://openrouter.ai,https://yourdomain.com
   ```

## Development

### Running Tests

```bash
# Install dev dependencies
pip install pytest pytest-asyncio httpx

# Run tests (if test suite exists)
pytest
```

### Debug Mode

Enable debug logging:

```env
LOG_LEVEL=DEBUG
```

This will show:
- All HTTP requests
- SQL queries
- Rate limiting decisions
- Signature verification details

### Database Management

**View sessions** (local):
```bash
sqlite3 oauth_sessions.db "SELECT * FROM oauth_sessions;"
```

**View sessions** (Docker):
```bash
docker exec oauth-callback sqlite3 /app/data/oauth_sessions.db "SELECT * FROM oauth_sessions;"
```

**Clear all sessions** (local):
```bash
sqlite3 oauth_sessions.db "DELETE FROM oauth_sessions;"
```

**Clear all sessions** (Docker):
```bash
docker exec oauth-callback sqlite3 /app/data/oauth_sessions.db "DELETE FROM oauth_sessions;"
```

**Reset database** (local):
```bash
rm oauth_sessions.db
# Database will be recreated on next startup
```

**Reset database** (Docker):
```bash
docker-compose down -v  # Removes volume with database
docker-compose up -d    # Starts fresh
```

**Backup database** (Docker):
```bash
docker cp oauth-callback:/app/data/oauth_sessions.db ./backup-$(date +%Y%m%d).db
```

**Restore database** (Docker):
```bash
docker cp ./backup-20241122.db oauth-callback:/app/data/oauth_sessions.db
docker-compose restart
```

## Deployment

### Docker Production Deployment

See the [Docker Deployment](#docker-deployment) section above for complete Docker instructions.

### Kubernetes Deployment

Example Kubernetes manifests:

**deployment.yaml**:
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: oauth-callback
spec:
  replicas: 2
  selector:
    matchLabels:
      app: oauth-callback
  template:
    metadata:
      labels:
        app: oauth-callback
    spec:
      containers:
      - name: oauth-callback
        image: oauth-callback-service:latest
        ports:
        - containerPort: 8000
        env:
        - name: LOG_LEVEL
          value: "WARNING"
        - name: ALLOWED_ORIGINS
          value: "https://openrouter.ai"
        volumeMounts:
        - name: data
          mountPath: /app/data
        livenessProbe:
          httpGet:
            path: /health
            port: 8000
          initialDelaySeconds: 10
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /health
            port: 8000
          initialDelaySeconds: 5
          periodSeconds: 10
      volumes:
      - name: data
        persistentVolumeClaim:
          claimName: oauth-data-pvc
```

**service.yaml**:
```yaml
apiVersion: v1
kind: Service
metadata:
  name: oauth-callback
spec:
  selector:
    app: oauth-callback
  ports:
  - port: 80
    targetPort: 8000
  type: ClusterIP
```

### systemd Service (Alternative to Docker)


Create `Dockerfile`:
```dockerfile
FROM python:3.12-slim

WORKDIR /app

COPY requirements.txt .
### systemd Service (Alternative to Docker)

Create `/etc/systemd/system/oauth-callback.service`:

```ini
[Unit]
Description=OAuth Callback Service
After=network.target

[Service]
Type=simple
User=www-data
WorkingDirectory=/path/to/oauth-callback-service
Environment="PATH=/path/to/oauth-callback-service/venv/bin"
ExecStart=/path/to/oauth-callback-service/venv/bin/uvicorn app.main:app --host 0.0.0.0 --port 8000
Restart=always

[Install]
WantedBy=multi-user.target
```

Enable and start:
```bash
sudo systemctl enable oauth-callback
sudo systemctl start oauth-callback
sudo systemctl status oauth-callback
```

## Monitoring

### Health Check

```bash
curl http://localhost:8000/health
```

Response:
```json
{
  "status": "healthy",
  "version": "1.0.0",
  "active_sessions": 5
}
```

### Diagnostic Endpoints (Testing/Debugging)

The service includes optional diagnostic endpoints for testing and troubleshooting. These are **disabled by default** for security.

**Enable diagnostic endpoints**:
```env
# In .env file
ENABLE_DIAGNOSTIC_ENDPOINTS=true
```

#### Ping Endpoint

Simple connectivity test:
```bash
curl http://localhost:8000/ping
```

Response:
```json
{
  "status": "pong",
  "timestamp": "2024-11-23T10:30:45.123456Z",
  "server_time": 1700737845
}
```

**Use cases**:
- Test network connectivity
- Verify service is responding
- Check server clock synchronization

#### Version Endpoint

Detailed version and system information:
```bash
curl http://localhost:8000/version
```

Response:
```json
{
  "service": "OAuth Callback Service",
  "version": "1.0.0",
  "python_version": "3.12.0",
  "platform": "Linux",
  "platform_release": "6.5.0",
  "uptime_seconds": 3600,
  "diagnostic_endpoints_enabled": true,
  "supported_providers": ["openrouter", "github", "google"],
  "rate_limits": {
    "oauth_endpoints": {
      "per_minute": 10,
      "per_hour": 100,
      "per_day": 1000
    },
    "diagnostic_endpoints": {
      "per_minute": 5
    }
  }
}
```

**Use cases**:
- Verify correct version is deployed
- Check Python version compatibility
- Monitor service uptime
- Review rate limit configuration
- Diagnose platform-specific issues

#### Rate Limiting for Diagnostic Endpoints

**Security measures**:
- **Strict rate limit**: 5 requests per minute per IP
- **Disabled by default**: Must explicitly enable
- **Returns 429** when rate limit exceeded
- **Returns 404** when disabled

**Example rate limit error**:
```json
{
  "detail": "Rate limit exceeded for diagnostic endpoints (5 per minute)"
}
```

**Best practices**:
- Only enable in development/testing environments
- Disable in production unless actively debugging
- Monitor access logs for unusual activity
- Use VPN or internal network for sensitive diagnostics

### Logs

Service logs all requests and errors:

```
2024-11-22 10:30:45 - app.main - INFO - Starting OAuth Callback Service...
2024-11-22 10:30:45 - app.main - INFO - Database initialized
2024-11-22 10:30:45 - app.main - INFO - Session cleanup task started (interval: 5 minutes)
2024-11-22 10:35:12 - app.main - INFO - Cleaned up 3 expired sessions
```

## Troubleshooting

### "Invalid signature" errors

1. Check timestamp freshness (must be within ±5 minutes)
2. Verify secret matches between client and server
3. Ensure message format: `{provider}|{timestamp}` (lowercase provider)
4. Check HMAC algorithm (must be HMAC-SHA256)

### Rate limiting triggers

- Check current limits: `GET /health` and review logs
- Adjust limits in `.env` if needed
- Different IPs aren't affected (rate limiting is per-IP)

### Sessions not found

- Sessions expire after 10 minutes (configurable)
- Sessions are single-use only
- Check database: `sqlite3 oauth_sessions.db "SELECT * FROM oauth_sessions;"`

### Cloudflare tunnel issues

1. Verify tunnel is running: `cloudflared tunnel info`
2. Check public hostname configuration
3. Test local service first: `curl http://localhost:8000/health`
4. Update application's `allowed_domains` in database to include tunnel URL

### "unknown_application" errors

- Check the request's `Host` header matches an application's `allowed_domains`
- Verify application exists: `SELECT * FROM applications WHERE is_active = 1;`
- Check domain includes port if using non-standard ports (e.g., `localhost:8000`)
- Ensure application is active (`is_active = 1`)

## Contributing

This is a development/testing service. For production use, consider:
- Migrating to PostgreSQL (change `DATABASE_URL`)
- Adding admin API for application management
- Implementing webhook notifications
- Adding metrics/monitoring (Prometheus)
- Setting up log aggregation

## License

This service is part of the PEV PlugShare Android application.

## Support

For issues or questions:
- Check logs for errors
- Review API documentation: [API_DOCUMENTATION.md](API_DOCUMENTATION.md)
- Test with interactive docs: http://localhost:8000/docs
- Verify health endpoint: http://localhost:8000/health
