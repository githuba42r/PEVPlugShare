# Building and Pushing Docker Image to GitHub Container Registry

This guide explains how to build the OAuth Callback Service Docker image and push it to the GitHub Container Registry (ghcr.io).

## Quick Start

```bash
cd oauth-callback-service

# Build and push with version tag
./build-and-push.sh v1.0.0

# Or build and push as latest
./build-and-push.sh
```

## Prerequisites

### 1. Docker Installed and Running

```bash
docker --version
# Should output: Docker version 20.x or higher
```

### 2. GitHub Personal Access Token (PAT)

You need a GitHub Personal Access Token with package permissions:

**Create a token:**
1. Go to https://github.com/settings/tokens
2. Click **"Generate new token (classic)"**
3. Give it a name: `Docker Registry Access`
4. Select scopes:
   - ✅ `write:packages` - Upload packages
   - ✅ `read:packages` - Download packages
   - ✅ `delete:packages` - Delete package versions (optional)
5. Click **"Generate token"**
6. **Copy the token** (you won't see it again!)

### 3. Authenticate with GitHub Container Registry

```bash
# Set your token as an environment variable
export CR_PAT=YOUR_TOKEN_HERE

# Login to GitHub Container Registry
echo $CR_PAT | docker login ghcr.io -u YOUR_GITHUB_USERNAME --password-stdin
```

**Example:**
```bash
export CR_PAT=ghp_1234567890abcdefghijklmnopqrstuvwxyz
echo $CR_PAT | docker login ghcr.io -u githuba42r --password-stdin
```

**Expected output:**
```
Login Succeeded
```

## Building and Pushing

### Option 1: Using the Build Script (Recommended)

The `build-and-push.sh` script automates the entire process:

```bash
cd oauth-callback-service

# Build and push with a specific version
./build-and-push.sh v1.0.0

# This will create and push:
# - ghcr.io/githuba42r/oauth2-redirector:v1.0.0
# - ghcr.io/githuba42r/oauth2-redirector:latest
```

**Build as latest only:**
```bash
./build-and-push.sh latest
# Or just:
./build-and-push.sh
```

### Option 2: Manual Build and Push

If you prefer manual control:

```bash
cd oauth-callback-service

# Set your version
VERSION=v1.0.0

# Build the image
docker build -t ghcr.io/githuba42r/oauth2-redirector:${VERSION} .

# Tag as latest
docker tag \
  ghcr.io/githuba42r/oauth2-redirector:${VERSION} \
  ghcr.io/githuba42r/oauth2-redirector:latest

# Push both tags
docker push ghcr.io/githuba42r/oauth2-redirector:${VERSION}
docker push ghcr.io/githuba42r/oauth2-redirector:latest
```

## Using the Published Image

### Pull the Image

```bash
# Pull latest
docker pull ghcr.io/githuba42r/oauth2-redirector:latest

# Pull specific version
docker pull ghcr.io/githuba42r/oauth2-redirector:v1.0.0
```

### Update docker-compose.yml

Edit `docker-compose.yml` to use the published image instead of building locally:

```yaml
services:
  oauth-callback:
    # Replace this line:
    # build: .
    
    # With this:
    image: ghcr.io/githuba42r/oauth2-redirector:latest
    
    container_name: oauth-callback-service
    ports:
      - "8000:8000"
    # ... rest of config
```

Then run:
```bash
docker-compose pull  # Pull latest image
docker-compose up -d  # Start service
```

## Versioning Strategy

### Semantic Versioning

Use semantic versioning for releases:

- **Major version** (v2.0.0): Breaking changes
- **Minor version** (v1.1.0): New features, backward compatible
- **Patch version** (v1.0.1): Bug fixes

**Examples:**
```bash
# Initial release
./build-and-push.sh v1.0.0

# Bug fix
./build-and-push.sh v1.0.1

# New feature
./build-and-push.sh v1.1.0

# Breaking change
./build-and-push.sh v2.0.0
```

### Development Builds

For development/testing:

```bash
# Development build
./build-and-push.sh dev

# Feature branch build
./build-and-push.sh feature-oauth-improvements

# Commit-based build
./build-and-push.sh $(git rev-parse --short HEAD)
```

## Making the Package Public

By default, GitHub Container Registry packages are private. To make your image public:

1. Go to https://github.com/users/githuba42r/packages/container/oauth2-redirector/settings
2. Click **"Package settings"**
3. Scroll to **"Danger Zone"**
4. Click **"Change visibility"**
5. Select **"Public"**
6. Confirm the action

Now anyone can pull your image without authentication:
```bash
docker pull ghcr.io/githuba42r/oauth2-redirector:latest
```

## Viewing Published Images

### On GitHub

Visit: https://github.com/githuba42r/packages/container/oauth2-redirector

You can see:
- All published versions
- Download statistics
- Package size
- Vulnerability scanning results

### Using Docker CLI

```bash
# List local images
docker images ghcr.io/githuba42r/oauth2-redirector

# Inspect an image
docker inspect ghcr.io/githuba42r/oauth2-redirector:latest

# View image history
docker history ghcr.io/githuba42r/oauth2-redirector:latest
```

## Troubleshooting

### "denied: permission_denied"

**Problem:** You don't have permission to push to the registry.

**Solution:**
```bash
# Verify you're logged in
docker login ghcr.io

# Check your token has write:packages scope
# Recreate token if needed: https://github.com/settings/tokens
```

### "unauthorized: authentication required"

**Problem:** Not logged in to GitHub Container Registry.

**Solution:**
```bash
export CR_PAT=your_token_here
echo $CR_PAT | docker login ghcr.io -u YOUR_USERNAME --password-stdin
```

### Build fails with "no space left on device"

**Problem:** Docker is running out of disk space.

**Solution:**
```bash
# Clean up unused Docker resources
docker system prune -a

# Remove old images
docker image prune -a

# Check disk space
df -h
```

### "manifest unknown: manifest unknown"

**Problem:** Trying to pull an image that doesn't exist.

**Solution:**
- Check the image name and tag are correct
- Verify the package exists on GitHub
- Make sure the package is public (if pulling without auth)

## GitHub Actions (Optional)

You can automate builds using GitHub Actions. Create `.github/workflows/docker-publish.yml`:

```yaml
name: Build and Push Docker Image

on:
  push:
    branches: [ main ]
    tags: [ 'v*' ]
  pull_request:
    branches: [ main ]

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: oauth2-redirector

jobs:
  build-and-push:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Log in to GitHub Container Registry
        uses: docker/login-action@v3
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Extract metadata
        id: meta
        uses: docker/metadata-action@v5
        with:
          images: ${{ env.REGISTRY }}/${{ github.repository_owner }}/${{ env.IMAGE_NAME }}
          tags: |
            type=ref,event=branch
            type=ref,event=pr
            type=semver,pattern={{version}}
            type=semver,pattern={{major}}.{{minor}}
            type=sha

      - name: Build and push Docker image
        uses: docker/build-push-action@v5
        with:
          context: ./oauth-callback-service
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}
```

This will automatically build and push images:
- On every push to `main` branch
- On every tag starting with `v` (e.g., v1.0.0)
- On pull requests (for testing)

## Best Practices

1. **Tag releases**: Always tag production releases with version numbers
2. **Use latest for development**: Use `latest` tag for development/testing
3. **Document changes**: Update README when pushing new versions
4. **Test locally first**: Always test images locally before pushing
5. **Scan for vulnerabilities**: GitHub automatically scans images
6. **Keep tokens secure**: Never commit tokens to git
7. **Rotate tokens regularly**: Create new tokens every 90 days

## Next Steps

1. ✅ Build and push your first image
2. ✅ Update docker-compose.yml to use the published image
3. ✅ Make the package public (if desired)
4. ⏳ Set up GitHub Actions for automated builds
5. ⏳ Document version releases in CHANGELOG.md
