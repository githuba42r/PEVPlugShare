# OAuth2 Redirector - Traefik/Portainer Quick Update Guide

## Quick Fix for 404 on /admin Route

If you're getting 404 errors on `https://oauth.a42r.com/admin/`, the issue is likely an incorrect Authelia middleware name.

### Step 1: Verify Your Authelia Middleware Name

```bash
ssh your-server
docker inspect authelia --format '{{json .Config.Labels}}' | grep middleware
```

You should see something like:
```
"traefik.http.middlewares.authelia-forwardauth.forwardauth.address": "http://authelia:9091/..."
```

The middleware name is: **`authelia-forwardauth`**

### Step 2: Update in Portainer

1. **Login to Portainer**: `http://your-server:9000` or `https://portainer.yourdomain.com`

2. **Navigate to your OAuth stack**:
   - Stacks → `oauth-callback-service` (or whatever you named it)

3. **Edit the stack**:
   - Click "Editor" tab
   - Find the line with `oauth-webui.middlewares`
   - Change: `authelia@docker` → `authelia-forwardauth@docker`

4. **Full middleware label should be**:
   ```yaml
   - traefik.http.routers.oauth-webui.middlewares=authelia-forwardauth@docker
   ```

5. **Update the stack**:
   - Click "Update the stack"
   - Check "Re-pull image and redeploy" (optional)
   - Click "Update"

### Step 3: Verify It Works

```bash
# Should return 401 or redirect to Authelia (not 404!)
curl -I https://oauth.a42r.com/admin/

# OAuth API should still work (public)
curl https://oauth.a42r.com/health
```

**In browser**:
- Visit `https://oauth.a42r.com/admin/`
- Should redirect to Authelia login (not 404)
- After login, should see the Admin UI

---

## Complete Docker Compose for Portainer

Copy this entire file into Portainer's stack editor:

```yaml
version: '3.8'

services:
  oauth-callback:
    image: ghcr.io/githuba42r/oauth2-redirector:latest
    container_name: oauth-callback-service
    environment:
      - HOST=0.0.0.0
      - PORT=8000
      - WEBUI_PORT=8001
      - DATABASE_URL=sqlite:////app/data/oauth_sessions.db
      - SESSION_TTL_MINUTES=10
      - CLEANUP_INTERVAL_MINUTES=5
      - LOG_LEVEL=INFO

    labels:
      - traefik.enable=true
      - traefik.docker.network=traefik

      # OAuth API Routes (NO authentication)
      - traefik.http.routers.oauth-health.rule=Host(`oauth.a42r.com`) && Path(`/health`)
      - traefik.http.routers.oauth-health.entrypoints=websecure
      - traefik.http.routers.oauth-health.tls=true
      - traefik.http.routers.oauth-health.tls.certresolver=letsencrypt
      - traefik.http.routers.oauth-health.service=oauth-api-svc
      - traefik.http.routers.oauth-health.priority=100
      
      - traefik.http.routers.oauth-callback.rule=Host(`oauth.a42r.com`) && PathPrefix(`/oauth`)
      - traefik.http.routers.oauth-callback.entrypoints=websecure
      - traefik.http.routers.oauth-callback.tls=true
      - traefik.http.routers.oauth-callback.tls.certresolver=letsencrypt
      - traefik.http.routers.oauth-callback.service=oauth-api-svc
      - traefik.http.routers.oauth-callback.priority=90
      
      - traefik.http.routers.oauth-root.rule=Host(`oauth.a42r.com`) && Path(`/`)
      - traefik.http.routers.oauth-root.entrypoints=websecure
      - traefik.http.routers.oauth-root.tls=true
      - traefik.http.routers.oauth-root.tls.certresolver=letsencrypt
      - traefik.http.routers.oauth-root.service=oauth-api-svc
      - traefik.http.routers.oauth-root.priority=80

      - traefik.http.services.oauth-api-svc.loadbalancer.server.port=8000
      - traefik.http.services.oauth-api-svc.loadbalancer.server.scheme=http

      # Web UI Routes (REQUIRES Authelia)
      # IMPORTANT: Change middleware name to match YOUR Authelia setup!
      - traefik.http.routers.oauth-webui.rule=Host(`oauth.a42r.com`) && PathPrefix(`/admin`)
      - traefik.http.routers.oauth-webui.entrypoints=websecure
      - traefik.http.routers.oauth-webui.tls=true
      - traefik.http.routers.oauth-webui.tls.certresolver=letsencrypt
      - traefik.http.routers.oauth-webui.service=oauth-webui-svc
      - traefik.http.routers.oauth-webui.middlewares=authelia-forwardauth@docker
      - traefik.http.routers.oauth-webui.priority=110

      - traefik.http.services.oauth-webui-svc.loadbalancer.server.port=8001
      - traefik.http.services.oauth-webui-svc.loadbalancer.server.scheme=http

      # HTTP to HTTPS redirect
      - traefik.http.routers.oauth-http.rule=Host(`oauth.a42r.com`)
      - traefik.http.routers.oauth-http.entrypoints=web
      - traefik.http.routers.oauth-http.middlewares=https-redirect@file

    volumes:
      - /storage/docker/oauth-redirect-service:/app/data
    networks:
      - traefik
    restart: always
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8000/health"]
      interval: 30s
      timeout: 3s
      retries: 3
      start_period: 5s

networks:
  traefik:
    name: traefik
    external: true
```

**Important Configuration Notes**:

1. **Change `oauth.a42r.com`** to your domain
2. **Change middleware name** if your Authelia uses a different name:
   - Check: `docker inspect authelia | grep middleware`
   - Common names: `authelia-forwardauth@docker`, `authelia@docker`
3. **Change volume path** if you store data elsewhere:
   - Current: `/storage/docker/oauth-redirect-service:/app/data`
4. **Network name must match**: Usually `traefik` (external)

---

## Troubleshooting

### Container is healthy but /admin returns 404

**Cause**: Traefik can't find the Authelia middleware or middleware returns 404

**Fix**:
```bash
# 1. Check middleware exists
docker inspect authelia --format '{{json .Config.Labels}}' | grep middleware

# 2. Update the label in Portainer to match
- traefik.http.routers.oauth-webui.middlewares=YOUR-MIDDLEWARE-NAME@docker

# 3. Redeploy stack in Portainer
```

### Direct container access works but Traefik returns 404

```bash
# Works (bypasses Traefik)
curl http://server-ip:18001/admin/

# Fails (through Traefik)
curl https://oauth.a42r.com/admin/
```

**Cause**: Router not being registered or middleware issue

**Check**:
1. Container is on `traefik` network
2. Middleware name is correct
3. Traefik has reloaded (restart if needed)

```bash
# Verify network
docker inspect oauth-callback-service | grep NetworkMode

# Restart Traefik to reload
docker restart traefik
```

### Authelia redirects infinitely

**Cause**: Authelia configuration missing or wrong

**Fix**: Add to Authelia `configuration.yml`:
```yaml
access_control:
  default_policy: deny
  rules:
    - domain: oauth.a42r.com
      policy: bypass
      resources:
        - "^/health$"
        - "^/oauth/.*$"
        - "^/$"
    
    - domain: oauth.a42r.com
      policy: one_factor
      resources:
        - "^/admin.*$"
```

Then restart:
```bash
docker restart authelia
```

---

## Testing Checklist

- [ ] `curl https://oauth.a42r.com/health` → 200 OK (JSON)
- [ ] `curl -I https://oauth.a42r.com/admin/` → 401 or 302 redirect to Authelia (NOT 404)
- [ ] Browser: `https://oauth.a42r.com/admin/` → Authelia login page
- [ ] After Authelia login → OAuth Admin UI loads
- [ ] OAuth API accessible without auth: `curl https://oauth.a42r.com/`

---

## Quick Commands Reference

```bash
# Check container status
docker ps --filter name=oauth

# View logs
docker logs oauth-callback-service --tail 50

# Test direct access
curl http://localhost:8001/admin/  # From server
curl http://server-ip:18001/admin/ # From your machine (if ports exposed)

# Check Traefik labels
docker inspect oauth-callback-service | grep traefik.http.routers

# Verify Authelia middleware name
docker inspect authelia | grep middleware

# Restart services
docker restart oauth-callback-service
docker restart traefik
docker restart authelia
```

---

## Support

For detailed documentation:
- **Full deployment guide**: `DEPLOYMENT.md`
- **README**: `README.md`
- **API documentation**: `API_DOCUMENTATION.md`

**Common issues**:
1. ❌ 404 on /admin → Wrong middleware name
2. ❌ Infinite redirect → Missing Authelia config
3. ❌ 502 Bad Gateway → Container not running or wrong port
4. ❌ OAuth blocked → Authelia blocking OAuth endpoints (should be `bypass`)
