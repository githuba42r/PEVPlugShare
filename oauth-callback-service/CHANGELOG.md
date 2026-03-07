# Changelog

All notable changes to the OAuth2 Redirector Service.

## [Unreleased] - 2026-03-07

### Fixed
- **Logging Configuration Error**: Fixed `KeyError: 'levelprefix'` by using proper uvicorn formatter classes
  - Changed from generic `format` to `()` class specification in logging_config.json
  - Now uses `uvicorn.logging.DefaultFormatter` and `uvicorn.logging.AccessFormatter`
  - Health check filtering works correctly without errors

### Changed
- **Startup Message**: Enhanced docker-entrypoint.sh to display Web Admin URL
  - Shows both OAuth API port and Web UI port
  - Lists all important endpoints (OAuth API, Web Admin, Health Check, API Docs)
  - Makes it clear where to access the Web Management interface

### Added - Web Management UI

#### New Features
- **Flask-based Web Management UI** on port 8001
  - Modern, responsive interface for managing OAuth applications
  - No SQL knowledge required
  - Real-time secret generation and management
  
#### Web UI Features
- List all OAuth applications with status indicators
- Add new applications with auto-generated secrets
- Edit existing applications (domains, schemes, names)
- Enable/disable applications without deletion
- View and copy shared secrets securely
- Regenerate secrets with one click
- Delete applications with confirmation
- View recent OAuth sessions (API endpoint ready)

#### API Endpoints (Web UI Backend)
- `GET /api/applications` - List all applications
- `GET /api/applications/<app_id>` - Get application details (includes secret)
- `POST /api/applications` - Create new application
- `PUT /api/applications/<app_id>` - Update application
- `DELETE /api/applications/<app_id>` - Delete application
- `POST /api/applications/<app_id>/toggle` - Enable/disable application
- `POST /api/applications/<app_id>/regenerate-secret` - Generate new secret
- `GET /api/sessions` - List recent OAuth sessions

#### Traefik + Authelia Integration
- **Dual-port architecture**: OAuth API (8000) public, Web UI (8001) protected
- Traefik routing configuration with path-based rules:
  - `/health`, `/oauth/*`, `/` → Public (no authentication)
  - `/admin/*` → Protected by Authelia authentication
- Production docker-compose with full Traefik labels
- URL rewriting: `/admin/` → `/` before forwarding to Flask
- Priority-based routing to prevent conflicts

#### Documentation
- Complete Web UI usage guide in README.md
- Comprehensive Traefik + Authelia integration section
- Security best practices for dual-service architecture
- Troubleshooting guide for common deployment issues
- Step-by-step deployment instructions in DEPLOYMENT.md

### Changed

#### Build System
- Renamed `build-and-push.sh` to `build.sh`
- Added `--push` flag requirement to push images to registry
- Build script now builds locally by default (safer)
- Improved argument parsing with `--help` flag
- Better output formatting and status messages

#### Health Check
- Changed from Python `requests` module to `curl`
- Fixes "unhealthy" container status issue
- More reliable health check execution

#### Logging
- Added `logging_config.json` for uvicorn
- Implemented `HealthCheckFilter` to suppress `/health` log spam
- Cleaner logs without health check noise every 30 seconds
- Added `[FASTAPI]` and `[WEBUI]` prefixes for log clarity

#### Docker Configuration
- Exposed port 8001 in addition to 8000
- Added `WEBUI_PORT` environment variable
- Updated supervisord to manage both fastapi and webui processes
- Added `start-webui.sh` startup script
- Fixed health check command (curl instead of Python)
- Added network configuration to production compose file

#### Configuration
- Added `webui_port` to config.py (default: 8001)
- Flask dependency added to requirements.txt

### Files Added
```
app/webui.py                           # Flask application
app/static/css/style.css               # Web UI styles
app/static/js/app.js                   # Web UI JavaScript
app/templates/index.html               # Web UI HTML template
app/utils/logging_filters.py           # Health check log filter
docker-compose.production.yml          # Production deployment config
logging_config.json                    # Uvicorn logging config
start-webui.sh                         # Flask startup script
setup-plugshare.sh                     # PEV PlugShare setup script
DEPLOYMENT.md                          # Deployment guide
CHANGELOG.md                           # This file
```

### Files Modified
```
Dockerfile                             # Added port 8001, webui scripts, logging config
docker-compose.yml                     # Added port 8001, WEBUI_PORT env var
README.md                              # Added Web UI and Authelia sections
app/config.py                          # Added webui_port configuration
app/main.py                            # Renamed middleware (cosmetic)
start-fastapi.sh                       # Added --log-config flag
supervisord.conf                       # Added [program:webui] section
requirements.txt                       # Added flask==3.0.0
```

### Security Notes

**Important**: This release changes the deployment architecture:
- OAuth API endpoints remain **publicly accessible** (required for OAuth flows)
- Web Management UI requires **Authelia authentication**
- Ensure Authelia is properly configured before deploying

**Authelia Configuration Required**:
```yaml
access_control:
  rules:
    # OAuth API - Public
    - domain: oauth.yourdomain.com
      policy: bypass
      resources:
        - "^/health$"
        - "^/oauth/.*$"
        - "^/$"
    
    # Web UI - Protected
    - domain: oauth.yourdomain.com
      policy: one_factor
      resources:
        - "^/admin.*$"
```

### Deployment Checklist

Before deploying this version:
- [ ] Review README.md Web UI and Authelia sections
- [ ] Read DEPLOYMENT.md completely
- [ ] Configure Authelia with proper access control rules
- [ ] Verify Traefik network exists on remote server
- [ ] Test authentication flow before going live
- [ ] Back up existing database
- [ ] Update docker-compose to use production.yml
- [ ] Test OAuth flow end-to-end after deployment

### Known Issues
None at this time.

### Breaking Changes
None. This is a backward-compatible addition.

### Migration Notes

**From Previous Versions**:
1. No database schema changes required
2. Existing applications continue to work
3. OAuth API endpoints unchanged
4. Add Web UI as new optional feature
5. Update docker-compose to expose port 8001

**Database**:
- No migrations required
- Existing data fully compatible

**Configuration**:
- Add `WEBUI_PORT=8001` to .env (optional, has default)
- Update Traefik labels if using production deployment
- Configure Authelia access control rules

## [1.0.0] - Previous Release

Initial OAuth2 Redirector Service release with:
- OAuth callback handling
- HMAC signature verification
- Rate limiting
- SQLite database
- Session management
- Multiple application support
- Cloudflare Tunnel integration
- Docker deployment
