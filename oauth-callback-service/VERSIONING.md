# OAuth Callback Service - Versioning Guide

## Overview

The OAuth Callback Service uses semantic versioning (SemVer) with automated version management through the `bump-version.sh` script. Version information is tracked in `version.json` and displayed in the admin UI, health endpoints, and Docker images.

## Version Format

The service follows [Semantic Versioning 2.0.0](https://semver.org/):

```
MAJOR.MINOR.PATCH[-PRERELEASE]
```

- **MAJOR**: Incompatible API changes
- **MINOR**: New functionality (backward compatible)
- **PATCH**: Bug fixes (backward compatible)
- **PRERELEASE**: Optional prerelease identifier (e.g., `alpha.0`, `beta.1`, `rc.2`)

## Version Tracking

### version.json

Version information is stored in `version.json`:

```json
{
  "version": "1.0.0",
  "build_date": "2026-03-08T12:34:56Z",
  "git_commit": "abc1234",
  "git_branch": "main"
}
```

**Fields:**
- `version`: Current semantic version
- `build_date`: ISO 8601 timestamp of last build
- `git_commit`: Short git commit hash
- `git_branch`: Git branch name

### Git Tags

Version bumps create git tags with the prefix `oauth-service/v`:

```
oauth-service/v1.0.0
oauth-service/v1.1.0
oauth-service/v2.0.0
```

This prefix distinguishes OAuth service versions from PEVPlugShare app versions in the same repository.

## Bumping Versions

### Using bump-version.sh

The `bump-version.sh` script automates version management:

```bash
cd oauth-callback-service
./bump-version.sh <bump-type> [OPTIONS]
```

**Bump Types:**
- `patch` - Bug fixes (1.0.0 → 1.0.1)
- `minor` - New features (1.0.1 → 1.1.0)
- `major` - Breaking changes (1.1.0 → 2.0.0)
- `prerelease` - Prerelease versions (1.0.0 → 1.0.1-alpha.0)

**Options:**
- `--no-commit` - Don't create git commit
- `--no-tag` - Don't create git tag
- `-h, --help` - Show help

### Examples

**Patch Release (Bug Fix):**
```bash
./bump-version.sh patch
# 1.0.0 → 1.0.1
# Creates commit & tag: oauth-service/v1.0.1
```

**Minor Release (New Feature):**
```bash
./bump-version.sh minor
# 1.0.1 → 1.1.0
# Creates commit & tag: oauth-service/v1.1.0
```

**Major Release (Breaking Change):**
```bash
./bump-version.sh major
# 1.1.0 → 2.0.0
# Creates commit & tag: oauth-service/v2.0.0
```

**Prerelease:**
```bash
./bump-version.sh prerelease
# 1.0.0 → 1.0.1-alpha.0
# Second call: 1.0.1-alpha.0 → 1.0.1-alpha.1
```

**Without Git Operations:**
```bash
./bump-version.sh patch --no-commit
# Updates version.json only
```

## Safety Checks

The bump script performs the following checks before bumping:

1. **Git Repository**: Verifies the directory is a git repository
2. **Uncommitted Changes**: Checks for uncommitted files in `oauth-callback-service/`
3. **Version File**: Creates `version.json` if it doesn't exist

**Example of safety check failure:**
```bash
$ ./bump-version.sh patch
Checking for uncommitted changes...
Error: There are uncommitted changes in oauth-callback-service/

Uncommitted changes:
 M app/config.py
 
Please commit or stash your changes before bumping version.
Or use --no-commit to skip git operations.
```

## Build Integration

### Automatic Version Updates

The `build.sh` script automatically updates build metadata in `version.json`:

```bash
./build.sh v1.2.0
```

This updates:
- `build_date`: Current UTC timestamp
- `git_commit`: Current git commit hash
- `git_branch`: Current git branch

### Docker Image Tags

Built images are tagged with the version number:

```bash
./build.sh --push v1.2.0
```

Creates:
- `ghcr.io/githuba42r/oauth2-redirector:v1.2.0`
- `ghcr.io/githuba42r/oauth2-redirector:latest`

## Version Display

### Admin UI

Version information is displayed in the admin web UI header:

- **Version badge**: Shows current version (e.g., `v1.2.0`)
- **Build date**: Shows when the service was last built
- **Tooltip**: Hover to see git commit hash

### Health Endpoint

The `/health` endpoint includes version information:

```bash
curl https://oauth.a42r.com/health
```

```json
{
  "status": "healthy",
  "version": "1.2.0",
  "active_sessions": 5
}
```

### Version Endpoint (Diagnostic)

When `ENABLE_DIAGNOSTIC_ENDPOINTS=true`, the `/version` endpoint provides detailed info:

```bash
curl https://oauth.a42r.com/version
```

```json
{
  "service": "OAuth Callback Service",
  "version": "1.2.0",
  "build_date": "2026-03-08T12:34:56Z",
  "git_commit": "abc1234",
  "python_version": "3.11.0",
  "platform": "Linux",
  "uptime_seconds": 3600,
  "supported_providers": ["openrouter", "github", "google"]
}
```

### Root Endpoint

The root API endpoint also shows version:

```bash
curl https://oauth.a42r.com/
```

```json
{
  "service": "OAuth Callback Service",
  "version": "1.2.0",
  "build_date": "2026-03-08T12:34:56Z",
  "description": "Generic OAuth 2.0 callback handler for mobile apps",
  "endpoints": {...},
  "documentation": "/docs"
}
```

## Workflow Examples

### Regular Release Workflow

```bash
# 1. Make your changes and test
cd oauth-callback-service
# ... make changes ...

# 2. Commit your changes
git add .
git commit -m "feat: add new feature"

# 3. Bump version
./bump-version.sh minor
# Output: Version: 1.0.0 → 1.1.0
# Creates commit & tag: oauth-service/v1.1.0

# 4. Build and push Docker image
./build.sh --push v1.1.0

# 5. Push to git
git push origin main
git push origin oauth-service/v1.1.0

# 6. Deploy the new version
# Update docker-compose.yml or k8s deployment to use v1.1.0
```

### Hotfix Workflow

```bash
# 1. Fix the bug
git checkout main
# ... fix bug ...
git add .
git commit -m "fix: critical bug in authentication"

# 2. Bump patch version
./bump-version.sh patch
# Output: Version: 1.1.0 → 1.1.1

# 3. Build and deploy immediately
./build.sh --push v1.1.1
git push origin main oauth-service/v1.1.1

# 4. Deploy hotfix
docker pull ghcr.io/githuba42r/oauth2-redirector:v1.1.1
docker-compose up -d
```

### Prerelease Workflow

```bash
# 1. Create feature branch
git checkout -b feature/new-oauth-provider

# 2. Make changes
# ... implement new feature ...

# 3. Create prerelease
./bump-version.sh prerelease
# Output: Version: 1.1.0 → 1.2.0-alpha.0

# 4. Build and test
./build.sh v1.2.0-alpha.0

# 5. After testing, create final release
git checkout main
git merge feature/new-oauth-provider
./bump-version.sh minor
# Output: Version: 1.1.0 → 1.2.0
```

## Best Practices

### When to Bump Versions

**MAJOR (Breaking Changes):**
- Changed API request/response format
- Removed endpoints or parameters
- Changed authentication mechanism
- Modified database schema (incompatible)

**MINOR (New Features):**
- Added new OAuth provider support
- Added new API endpoints
- Added optional parameters
- New admin UI features

**PATCH (Bug Fixes):**
- Fixed security vulnerability
- Fixed incorrect behavior
- Performance improvements
- Documentation updates

### Commit Message Convention

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
feat: add GitHub OAuth provider support
fix: resolve signature verification timeout
docs: update API documentation
chore: bump version to 1.2.0
```

### Version Tagging Strategy

- Always tag releases with `oauth-service/v` prefix
- Use annotated tags (created by bump-version.sh)
- Include release notes in tag annotations
- Push tags to remote after verification

### Docker Image Strategy

- Production: Use specific version tags (e.g., `v1.2.0`)
- Development: Use `latest` tag
- Testing: Use prerelease tags (e.g., `v1.2.0-alpha.0`)
- Never use `latest` in production

## Troubleshooting

### "There are uncommitted changes"

```bash
# Option 1: Commit your changes
git add .
git commit -m "your changes"
./bump-version.sh patch

# Option 2: Skip git operations
./bump-version.sh patch --no-commit
```

### "Version file not found"

The script automatically creates `version.json` with version `0.1.0` if it doesn't exist.

### "Failed to bump version"

Check Python is installed and version.json has valid JSON:

```bash
python3 --version
python3 -c "import json; print(json.load(open('version.json')))"
```

### Version not showing in UI

1. Check version.json exists and has valid data
2. Verify build.sh was run to update build_date
3. Check browser console for JavaScript errors
4. Clear browser cache and reload

## Migration from Old System

If you're migrating from an unversioned service:

```bash
# 1. Create initial version file
cd oauth-callback-service
./bump-version.sh patch --no-commit
# This creates version.json with version 0.1.0

# 2. Edit version manually if needed
nano version.json
# Change "version": "1.0.0" if you're already past 0.x

# 3. Commit the version file
git add version.json
git commit -m "chore: add version tracking"

# 4. Create initial tag
git tag -a oauth-service/v1.0.0 -m "Initial version"
git push origin main oauth-service/v1.0.0
```

## Summary

- **Bump versions**: `./bump-version.sh <type>`
- **Git tags**: `oauth-service/v1.2.3` format
- **Build updates**: Automatic during `./build.sh`
- **Display**: Admin UI, `/health`, `/version`, `/`
- **Safety**: Checks for uncommitted changes
- **Semantic**: MAJOR.MINOR.PATCH[-PRERELEASE]
