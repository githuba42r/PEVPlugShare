# Cloudflare Tunnel Quick Reference

## Setup Steps

### 1. Get Cloudflare Tunnel Token

1. Go to https://one.dash.cloudflare.com
2. Navigate to: **Zero Trust** → **Networks** → **Connectors**
3. Click **Create a connector**
4. Choose **Cloudflared** (not WARP Connector)
   - **Cloudflared**: For hosting services (HTTP/HTTPS) - ✅ Use this
   - **WARP Connector**: For accessing private networks - ❌ Not for this use case
5. Click **Begin setup**
6. Name your connector: `oauth-callback`
7. Click **Save connector**
8. **Copy the tunnel token** from the installation command
   - Look for the long string after `--token` (starts with `eyJ...`)
   - Example: `eyJhIjoiYmM0ZTM5ZjQwNjNmNDM4YWI5OTI3ZGE5ZjMyYzk3OGEi...`

### 2. Configure Public Hostname

After creating the connector, configure a public hostname:

1. In your connector settings, go to the **Public Hostnames** tab
2. Click **Add a public hostname**
3. Configure the hostname:
   - **Subdomain**: `oauth` (your choice)
   - **Domain**: Select your domain from the dropdown
   - **Path**: Leave empty (or `/` for all paths)
   - **Service**:
     - **Type**: HTTP
     - **URL**: `localhost:8000`
4. Click **Save hostname**

Your public URL will be: `https://oauth.yourdomain.com`

### 3. Configure Docker

Edit `.env` file:
```env
CLOUDFLARE_TUNNEL_ENABLED=true
CLOUDFLARE_TUNNEL_TOKEN=eyJhIjoiYmM0ZTM5ZjQwNjNmNDM4YWI5OTI3ZGE5ZjMyYzk3OGEiLCJ0IjoiOTZmYzA5NDAtYjNmYy00MTljLWI3YTMtNjk2ZGQwMjQxNDQ5IiwicyI6Ik9XVmxOelE1Wm1FdE56SmhZaTAwTkRjeExXSTFNV1V0TnpFek1UQTVNR1JpT1RCaiJ9
```

### 4. Configure Application

Update your application's allowed domains to include the tunnel URL:

```bash
# Generate a secure secret if you haven't already
python -c "import secrets; print(secrets.token_urlsafe(32))"

# Start the service
docker-compose up -d

# Configure the application
docker exec -it oauth-callback-service sqlite3 /app/data/oauth_sessions.db
```

```sql
UPDATE applications
SET
  allowed_domains = 'oauth.yourdomain.com,localhost:8000',
  shared_secret = 'your-generated-secret-here'
WHERE app_id = 'pevplugshare';
```

### 5. Start Service

```bash
docker-compose up -d
```

### 6. Test

```bash
# Check logs
docker-compose logs -f

# Test locally - health endpoint (always available)
curl http://localhost:8000/health

# Test publicly
curl https://oauth.yourdomain.com/health

# Optional: Enable diagnostic endpoints for testing
# Edit .env: ENABLE_DIAGNOSTIC_ENDPOINTS=true
# Restart: docker-compose restart
# Test ping: curl http://localhost:8000/ping
# Test version: curl http://localhost:8000/version
```

## Common Commands

```bash
# View all logs
docker-compose logs -f

# View only cloudflared logs
docker-compose logs -f | grep cloudflared

# View only FastAPI logs
docker-compose logs -f | grep fastapi

# Check process status inside container
docker exec oauth-callback-service supervisorctl status

# Restart service
docker-compose restart

# Rebuild and restart
docker-compose up -d --build

# Stop and remove everything
docker-compose down -v
```

## Environment Variables

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `CLOUDFLARE_TUNNEL_ENABLED` | No | `false` | Enable Cloudflare Tunnel |
| `CLOUDFLARE_TUNNEL_TOKEN` | Yes* | - | Tunnel token from Cloudflare dashboard |
| `DATABASE_URL` | No | `sqlite:///./data/oauth_sessions.db` | Database connection string |
| `SESSION_TTL_MINUTES` | No | `10` | Session expiration time |
| `LOG_LEVEL` | No | `INFO` | Logging level |

*Required when `CLOUDFLARE_TUNNEL_ENABLED=true`

**Note**: Domain names and secrets are configured per-application in the database, not via environment variables. See the main README for application management.

## Architecture

```
┌─────────────────────────────────────────────┐
│        Docker Container                      │
│                                              │
│  ┌────────────┐      ┌─────────────────┐   │
│  │ Supervisord│      │                  │   │
│  └─────┬──────┘      │   FastAPI :8000  │   │
│        │             │   (OAuth Service)│   │
│        ├─────────────►                  │   │
│        │   starts    └────────▲─────────┘   │
│        │                      │              │
│        │                      │              │
│        │             ┌────────┴─────────┐   │
│        └─────────────►                  │   │
│            starts     │   cloudflared    │   │
│         (if enabled)  │   (CF Tunnel)    │   │
│                       └────────┬─────────┘   │
└────────────────────────────────┼─────────────┘
                                 │
                                 │ HTTPS tunnel
                                 ▼
                        ┌────────────────┐
                        │  Cloudflare    │
                        │    Network     │
                        └────────┬───────┘
                                 │
                                 ▼
                           🌍 Internet
                    https://oauth.yourdomain.com
```

## Troubleshooting

### Issue: "CLOUDFLARE_TUNNEL_TOKEN is required"

**Solution**: Set the token in `.env`:
```env
CLOUDFLARE_TUNNEL_TOKEN=your-token-here
```

### Issue: "Tunnel not connecting"

**Check**:
1. Token is valid and complete
2. Container can reach internet
3. Cloudflare tunnel status in dashboard

**Fix**:
```bash
# View cloudflared logs
docker-compose logs cloudflared

# Regenerate token if expired
# Update .env with new token
docker-compose restart
```

### Issue: "Public URL returns 502 Bad Gateway"

**Causes**:
- FastAPI not running
- Cloudflare tunnel configuration wrong

**Check**:
```bash
# Verify FastAPI is running
docker exec oauth-callback-service curl http://localhost:8000/health

# Check tunnel service URL in Cloudflare dashboard
# Should be: localhost:8000 (NOT 127.0.0.1:8000)
```

### Issue: Disable tunnel but keep service running

**Solution**:
```env
# In .env
CLOUDFLARE_TUNNEL_ENABLED=false
```
```bash
docker-compose restart
```

## Security Notes

1. **Never commit** `.env` file to git (already in `.gitignore`)
2. **Rotate secrets regularly**: Update application's `shared_secret` in database every 90 days
3. **Use strong secrets**: `python -c "import secrets; print(secrets.token_urlsafe(32))"`
4. **Monitor** Cloudflare dashboard for unusual traffic
5. **Rate limiting** is enabled by default (10/min, 100/hour, 1000/day)
6. **Domain validation**: Only add domains you control to `allowed_domains`
7. **Disable unused apps**: Set `is_active = 0` for applications no longer in use

## Benefits vs Alternatives

| Feature | Cloudflare Tunnel | Port Forwarding | AWS Lambda |
|---------|------------------|-----------------|------------|
| No router config | ✅ | ❌ | ✅ |
| Free HTTPS | ✅ | ❌ | ✅ |
| DDoS protection | ✅ | ❌ | ✅ |
| Works behind NAT | ✅ | ❌ | ✅ |
| Self-hosted | ✅ | ✅ | ❌ |
| Monthly cost | Free | Free | ~$0.56 |
| Maintenance | Low | High | Low |

## References

- Cloudflare Tunnel Docs: https://developers.cloudflare.com/cloudflare-one/connections/connect-apps/
- Zero Trust Dashboard: https://one.dash.cloudflare.com/
- Supervisord Docs: http://supervisord.org/
