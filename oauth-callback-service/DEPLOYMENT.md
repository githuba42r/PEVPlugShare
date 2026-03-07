# OAuth2 Redirector - Deployment Instructions

## Overview

This guide covers deploying the OAuth2 Redirector Service with Web Management UI to your production server with Traefik + Authelia integration.

## Prerequisites

### On Your Local Machine
- Docker installed and running
- GitHub account with access to `githuba42r` organization
- GitHub Personal Access Token with `write:packages` scope

### On Remote Server (home-server)
- Docker and Docker Compose installed
- Traefik reverse proxy configured and running
- Authelia authentication service configured
- Network: `traefik` (external Docker network for Traefik)
- Volume mount: `/storage/docker/oauth-redirect-service/` (for database persistence)

## Deployment Steps

### Step 1: Build and Push Docker Image to Registry

**On your local machine:**

```bash
cd /home/philg/src/AndroidStudioProjects/PEVPlugShare/oauth-callback-service

# Authenticate to GitHub Container Registry (one-time setup)
# Create token at: https://github.com/settings/tokens
# Required scopes: write:packages, read:packages
export CR_PAT=YOUR_GITHUB_TOKEN
echo $CR_PAT | docker login ghcr.io -u YOUR_GITHUB_USERNAME --password-stdin

# Build and push the image
./build.sh --push

# Or with a specific version tag
./build.sh --push v1.1.0
```

**Build options:**
```bash
# Build locally without pushing (for testing)
./build.sh

# Build with version tag locally
./build.sh v1.1.0

# Force rebuild without cache (ignores cached layers)
./build.sh --force

# Build and push to registry
./build.sh --push

# Build specific version and push
./build.sh --push v1.1.0

# Force rebuild and push (useful after dependency updates)
./build.sh --force --push

# Show help
./build.sh --help
```

**When to use --force:**
- After updating `requirements.txt` with new package versions
- When troubleshooting build issues related to caching
- After major changes to the Dockerfile
- When you want to ensure a completely fresh build

### Step 2: Prepare Production Environment

**On remote server (home-server):**

```bash
# Create directory structure
mkdir -p /storage/docker/oauth-redirect-service

# Ensure Traefik network exists
docker network create traefik || echo "Network already exists"

# Verify Authelia is running
docker ps | grep authelia
```

### Step 3: Deploy Updated Service

**Copy production compose file to server:**

```bash
# From local machine
scp oauth-callback-service/docker-compose.production.yml \
    home-server:/tmp/docker-compose.oauth.yml
```

**On remote server:**

```bash
# Pull latest image
docker pull ghcr.io/githuba42r/oauth2-redirector:latest

# Stop existing container if running
cd /path/to/oauth-service  # or wherever you have the old compose file
docker-compose down

# Deploy new version
cd /storage/docker/oauth-redirect-service
cp /tmp/docker-compose.oauth.yml docker-compose.yml

# Start service
docker-compose up -d

# Watch logs
docker logs -f oauth-callback-service
```

**Expected log output:**
```
[FASTAPI] INFO:     Started server process
[FASTAPI] INFO:     Waiting for application startup.
[FASTAPI] INFO:     Application startup complete.
[WEBUI] * Running on http://0.0.0.0:8001
[WEBUI] * Debug mode: off
```

### Step 4: Verify Both Services Are Running

```bash
# Check supervisor status
docker exec oauth-callback-service supervisorctl status

# Expected output:
# fastapi                          RUNNING   pid 8, uptime 0:00:10
# webui                            RUNNING   pid 15, uptime 0:00:08
```

### Step 5: Configure Authelia

**Add to Authelia configuration (`configuration.yml`):**

```yaml
access_control:
  default_policy: deny
  rules:
    # OAuth API - Public (NO authentication required)
    - domain: oauth.a42r.com
      policy: bypass
      resources:
        - "^/health$"           # Health check
        - "^/oauth/.*$"         # OAuth callbacks
        - "^/$"                 # Root API endpoint
    
    # Web Management UI - Requires authentication
    - domain: oauth.a42r.com
      policy: one_factor        # Change to two_factor for 2FA
      resources:
        - "^/admin.*$"          # All admin routes
```

**Restart Authelia:**
```bash
docker restart authelia
```

### Step 6: Test Deployment

**Test OAuth API (public endpoints):**
```bash
# Health check - should return JSON
curl https://oauth.a42r.com/health
# Expected: {"status":"healthy","version":"1.0.0",...}

# Root endpoint - should return service info
curl https://oauth.a42r.com/
# Expected: {"service":"OAuth Callback Service",...}
```

**Test Web UI (protected endpoint):**
```bash
# Should redirect to Authelia or return 401/403
curl -I https://oauth.a42r.com/admin/
# Expected: HTTP 302 or 401 (not authenticated)

# After authenticating via browser, should return 200
curl -H "Cookie: authelia_session=..." https://oauth.a42r.com/admin/
# Expected: HTTP 200 with HTML
```

**Test in browser:**
1. Open https://oauth.a42r.com/admin/
2. Should redirect to Authelia login
3. Log in with your Authelia credentials
4. Should see Web Management UI
5. Test adding/editing applications

### Step 7: Configure PEV PlugShare Application

**Option A: Use Web UI (Recommended):**

1. Navigate to https://oauth.a42r.com/admin/
2. Click "Add Application"
3. Fill in:
   - **App ID**: `pevplugshare`
   - **Name**: `PEV PlugShare`
   - **Allowed Domains**: `pevplugshare.a42r.com,pevplugshare-prod.a42r.com,localhost:8000,localhost:8082`
   - **Deep Link Scheme**: `pevplugshare://oauth/callback`
4. Click "Add Application"
5. Copy the generated secret (you'll need this for the Android app)

**Option B: Use setup script:**
```bash
# Copy script to server
scp setup-plugshare.sh home-server:/tmp/

# Run on server
ssh home-server
chmod +x /tmp/setup-plugshare.sh
docker cp /tmp/setup-plugshare.sh oauth-callback-service:/tmp/
docker exec oauth-callback-service /tmp/setup-plugshare.sh

# Secret will be displayed - copy it!
```

**Option C: Use manage_db.py:**
```bash
docker exec oauth-callback-service python manage_db.py add \
  pevplugshare \
  "PEV PlugShare" \
  "pevplugshare.a42r.com,pevplugshare-prod.a42r.com,localhost:8000,localhost:8082" \
  "pevplugshare://oauth/callback" \
  "eSYpX2jtO_7QKoiOKTRq7Bw4It4prX05eqBXzbty8NA"
```

### Step 8: Update Android App

**Store the shared secret in your Android app:**

The secret needs to be added to the PEV PlugShare Android app configuration:
- App ID: `pevplugshare`
- Secret: `[from step 7]`

Location TBD - need to identify where OAuth configuration is stored in the app.

## Verification Checklist

- [ ] Docker image built and pushed to ghcr.io
- [ ] Container running on home-server
- [ ] Both processes running (fastapi + webui)
- [ ] Health check passing: `curl https://oauth.a42r.com/health`
- [ ] OAuth API accessible without auth
- [ ] Web UI requires Authelia authentication
- [ ] Authelia rules configured correctly
- [ ] PEV PlugShare application configured
- [ ] Shared secret copied to Android app
- [ ] End-to-end OAuth flow tested with Android app

## Architecture Summary

```
┌─────────────────────────────────────────────────────────────┐
│                        Internet                              │
└────────────────────────┬────────────────────────────────────┘
                         │
                    [Traefik]
                         │
        ┌────────────────┴────────────────┐
        │                                  │
   [Public Routes]              [Protected Routes]
   No Authentication            Authelia Required
        │                                  │
   ┌────┴─────────────────┐          ┌────┴────────┐
   │ /health              │          │ /admin/*    │
   │ /oauth/*             │          │             │
   │ /                    │          │             │
   └────┬─────────────────┘          └────┬────────┘
        │                                  │
        ↓                                  ↓
   [FastAPI - Port 8000]        [Flask Web UI - Port 8001]
        │                                  │
        └──────────────┬───────────────────┘
                       │
            [OAuth Callback Container]
            - supervisord manages both
            - SQLite database shared
            - Logs to stdout/stderr
```

## Troubleshooting

### Container fails to start

```bash
# Check logs
docker logs oauth-callback-service

# Check supervisor status
docker exec oauth-callback-service supervisorctl status

# Restart processes
docker exec oauth-callback-service supervisorctl restart all
```

### Web UI returns 404

```bash
# Verify webui process is running
docker exec oauth-callback-service supervisorctl status webui

# Check if port 8001 is listening
docker exec oauth-callback-service netstat -tlnp | grep 8001

# Test direct access
docker exec oauth-callback-service curl http://localhost:8001/
```

### OAuth endpoints blocked by Authelia

```bash
# Check Authelia logs
docker logs authelia | tail -50

# Verify bypass rules in configuration.yml
# Ensure OAuth routes use policy: bypass

# Test direct container access (bypass Traefik)
docker exec oauth-callback-service curl http://localhost:8000/health
```

### Database issues

```bash
# Check database file exists
docker exec oauth-callback-service ls -la /app/data/

# Verify database is writable
docker exec oauth-callback-service sqlite3 /app/data/oauth_sessions.db "SELECT COUNT(*) FROM applications;"

# Backup database
docker cp oauth-callback-service:/app/data/oauth_sessions.db ./backup-$(date +%Y%m%d).db
```

### Health check failing

```bash
# Check if FastAPI is responding
docker exec oauth-callback-service curl http://localhost:8000/health

# Check supervisor logs
docker exec oauth-callback-service supervisorctl tail fastapi

# Restart FastAPI
docker exec oauth-callback-service supervisorctl restart fastapi
```

## Rollback Procedure

If something goes wrong:

```bash
# Stop new version
docker-compose down

# Pull previous version
docker pull ghcr.io/githuba42r/oauth2-redirector:previous-tag

# Update docker-compose.yml to use previous tag
# image: ghcr.io/githuba42r/oauth2-redirector:previous-tag

# Start previous version
docker-compose up -d
```

## Post-Deployment Tasks

1. **Monitor logs** for the first 24 hours:
   ```bash
   docker logs -f --since 10m oauth-callback-service
   ```

2. **Test OAuth flow** with PEV PlugShare app:
   - Initiate OAuth from mobile app
   - Verify callback works
   - Confirm token exchange completes

3. **Backup database**:
   ```bash
   docker cp oauth-callback-service:/app/data/oauth_sessions.db \
     /backups/oauth-$(date +%Y%m%d).db
   ```

4. **Set up monitoring** (optional):
   - Add health check to monitoring system
   - Configure alerts for container down
   - Monitor disk usage for database

5. **Document the secret** (securely):
   - Store shared secret in password manager
   - Document which version of Android app uses which secret
   - Plan secret rotation schedule (every 90 days recommended)

## Maintenance

### Viewing Sessions

```bash
# Via Web UI
https://oauth.a42r.com/admin/
# Click "Sessions" tab

# Via command line
docker exec oauth-callback-service python manage_db.py sessions

# Via SQL
docker exec oauth-callback-service sqlite3 /app/data/oauth_sessions.db \
  "SELECT * FROM oauth_sessions ORDER BY created_at DESC LIMIT 10;"
```

### Updating Configuration

```bash
# Edit application settings via Web UI
https://oauth.a42r.com/admin/

# Or via command line
docker exec oauth-callback-service python manage_db.py update-domains \
  pevplugshare "new-domain.com,other-domain.com"
```

### Log Rotation

Logs are written to stdout/stderr and handled by Docker:

```bash
# Configure log rotation in docker-compose.yml
services:
  oauth-callback:
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"
```

### Database Maintenance

```bash
# Vacuum database (compact and optimize)
docker exec oauth-callback-service sqlite3 /app/data/oauth_sessions.db "VACUUM;"

# Clean up old sessions (automatic, but can run manually)
docker exec oauth-callback-service sqlite3 /app/data/oauth_sessions.db \
  "DELETE FROM oauth_sessions WHERE expires_at < $(date +%s);"
```

## Security Checklist

- [ ] Shared secrets are cryptographically random (32+ bytes)
- [ ] Secrets are not committed to Git
- [ ] Authelia is configured with strong password policy
- [ ] Authelia is using one_factor or two_factor for /admin routes
- [ ] OAuth endpoints use bypass policy (no auth required)
- [ ] HTTPS is enforced (Traefik handles TLS)
- [ ] Rate limiting is enabled (built-in)
- [ ] Log monitoring is configured
- [ ] Database backups are scheduled
- [ ] Only necessary ports are exposed
- [ ] Traefik network is properly isolated

## Support

For issues or questions:
- Check logs: `docker logs oauth-callback-service`
- Review health: `curl https://oauth.a42r.com/health`
- Test endpoints: https://oauth.a42r.com/docs
- Check this deployment guide
- Review main README.md

## Version History

- **v1.1.0** (2026-03-07): Added Web Management UI with Authelia integration
- **v1.0.0**: Initial OAuth callback service release
