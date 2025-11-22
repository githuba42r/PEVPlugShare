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
# Required settings
DOMAIN_NAME=https://your-cloudflare-tunnel.example.com
SHARED_SECRET=your-secret-key-here

# Generate a secure secret:
# python -c "import secrets; print(secrets.token_urlsafe(32))"
```

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
docker build -t oauth-callback-service .
```

**2. Run the container**:
```bash
# Generate a secure secret first
SECRET=$(python -c "import secrets; print(secrets.token_urlsafe(32))")

# Run container with environment variables
docker run -d \
  --name oauth-callback \
  -p 8000:8000 \
  -e DOMAIN_NAME=https://your-cloudflare-tunnel.example.com \
  -e SHARED_SECRET=$SECRET \
  -v oauth-data:/app/data \
  oauth-callback-service
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
cp .env.docker .env
# Edit .env and set DOMAIN_NAME and SHARED_SECRET
```

**2. Start the service**:
```bash
docker-compose up -d
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

### Docker Image Details

- **Base**: Python 3.12 slim
- **Size**: ~150MB
- **Port**: 8000
- **Health Check**: Built-in HTTP health check every 30s
- **Database**: SQLite stored in `/app/data` volume
- **Logs**: stdout/stderr (view with `docker logs`)

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
      - DOMAIN_NAME=https://oauth.yourdomain.com
      - SHARED_SECRET=${SHARED_SECRET}
      - LOG_LEVEL=WARNING
      - ALLOWED_ORIGINS=https://openrouter.ai,https://yourdomain.com
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

Run with:
```bash
docker-compose -f docker-compose.prod.yml up -d
```

### Docker + Cloudflare Tunnel

**1. Run OAuth service in Docker**:
```bash
docker-compose up -d
```

**2. Run Cloudflare tunnel (separate container or host)**:
```bash
# Option A: Run cloudflared on host
cloudflared tunnel --url http://localhost:8000

# Option B: Add to docker-compose.yml
services:
  cloudflared:
    image: cloudflare/cloudflared:latest
    command: tunnel --url http://oauth-callback:8000
    depends_on:
      - oauth-callback
```

**3. Update `.env` with tunnel URL**:
```env
DOMAIN_NAME=https://your-tunnel-url.trycloudflare.com
```

**4. Restart service**:
```bash
docker-compose restart
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

Update your `.env`:
```env
DOMAIN_NAME=https://random-name.trycloudflare.com
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

2. Update `.env`:
   ```env
   DOMAIN_NAME=https://oauth.your-domain.com
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
val timestamp = System.currentTimeMillis() / 1000
val signature = OAuthSignature.generateSignature("openrouter", timestamp, secret)

val request = JSONObject().apply {
    put("provider", "openrouter")
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
| `DOMAIN_NAME` | string | **Required** | Public domain for callback URLs |
| `DEEP_LINK_SCHEME` | string | `pevplugshare://oauth/callback` | Mobile app deep link |
| `DATABASE_URL` | string | `sqlite:///./oauth_sessions.db` | Database connection string |
| `SESSION_TTL_MINUTES` | integer | `10` | Session expiration time |
| `CLEANUP_INTERVAL_MINUTES` | integer | `5` | Database cleanup interval |
| `SHARED_SECRET` | string | **Required** | HMAC signature secret |
| `RATE_LIMIT_PER_MINUTE` | integer | `10` | Requests per minute per IP |
| `RATE_LIMIT_PER_HOUR` | integer | `100` | Requests per hour per IP |
| `RATE_LIMIT_PER_DAY` | integer | `1000` | Requests per day per IP |
| `ALLOWED_ORIGINS` | string | `*` | CORS allowed origins (comma-separated) |
| `OAUTH_PROVIDERS` | string | `openrouter,github,google` | Supported providers (comma-separated) |
| `LOG_LEVEL` | string | `INFO` | Logging level (DEBUG, INFO, WARNING, ERROR) |

### Generating Secrets

Generate a cryptographically secure secret for `SHARED_SECRET`:

```bash
python -c "import secrets; print(secrets.token_urlsafe(32))"
```

Example output: `xK7pQ9rN2mZ4vB8wC3dE5fG6hJ1kL0mN2oP4qR5sT7uV9wX`

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
        - name: DOMAIN_NAME
          value: "https://oauth.yourdomain.com"
        - name: SHARED_SECRET
          valueFrom:
            secretKeyRef:
              name: oauth-secrets
              key: shared-secret
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
4. Update `DOMAIN_NAME` in `.env` to match tunnel URL

## Contributing

This is a development/testing service. For production use, consider:
- Migrating to PostgreSQL (change `DATABASE_URL`)
- Adding authentication for admin endpoints
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
