#!/bin/bash

# OAuth Callback Service - Docker Image Builder
# This script builds the Docker image and optionally pushes it to ghcr.io

set -e  # Exit on error

# Configuration
GITHUB_USER="githuba42r"
IMAGE_NAME="oauth2-redirector"
REGISTRY="ghcr.io"
FULL_IMAGE_NAME="${REGISTRY}/${GITHUB_USER}/${IMAGE_NAME}"

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Default values
VERSION="latest"
PUSH=false
FORCE=false

# Show usage
show_usage() {
    echo "Usage: $0 [OPTIONS] [VERSION]"
    echo ""
    echo "Build Docker image for OAuth2 Redirector Service"
    echo ""
    echo "Options:"
    echo "  --push          Push image to GitHub Container Registry after building"
    echo "  --force         Force rebuild without using cache (ignores cached layers)"
    echo "  -h, --help      Show this help message"
    echo ""
    echo "Arguments:"
    echo "  VERSION         Version tag (default: latest)"
    echo ""
    echo "Examples:"
    echo "  $0                      # Build only, tag as 'latest'"
    echo "  $0 --push               # Build and push 'latest'"
    echo "  $0 --force              # Force complete rebuild without cache"
    echo "  $0 --push --force       # Force rebuild and push"
    echo "  $0 v1.2.0               # Build only, tag as 'v1.2.0' and 'latest'"
    echo "  $0 --push v1.2.0        # Build and push 'v1.2.0' and 'latest'"
    echo "  $0 --force --push v1.2.0  # Force rebuild and push 'v1.2.0'"
    echo ""
}

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --push)
            PUSH=true
            shift
            ;;
        --force)
            FORCE=true
            shift
            ;;
        -h|--help)
            show_usage
            exit 0
            ;;
        -*)
            echo -e "${RED}Error: Unknown option: $1${NC}"
            show_usage
            exit 1
            ;;
        *)
            VERSION="$1"
            shift
            ;;
    esac
done

echo -e "${BLUE}================================${NC}"
echo -e "${BLUE}OAuth Callback Service Builder${NC}"
echo -e "${BLUE}================================${NC}"
echo ""

echo -e "${GREEN}Building version: ${VERSION}${NC}"
if [ "$PUSH" = true ]; then
    echo -e "${GREEN}Push to registry: enabled${NC}"
else
    echo -e "${YELLOW}Push to registry: disabled (use --push to enable)${NC}"
fi
if [ "$FORCE" = true ]; then
    echo -e "${GREEN}Force rebuild: enabled (no cache)${NC}"
else
    echo -e "${BLUE}Using cache: yes (use --force to rebuild from scratch)${NC}"
fi
echo ""

# Full image tags
IMAGE_TAG_VERSIONED="${FULL_IMAGE_NAME}:${VERSION}"
IMAGE_TAG_LATEST="${FULL_IMAGE_NAME}:latest"

echo -e "${BLUE}Image names:${NC}"
echo -e "  - ${IMAGE_TAG_VERSIONED}"
if [ "$VERSION" != "latest" ]; then
    echo -e "  - ${IMAGE_TAG_LATEST} (also tagged)"
fi
echo ""

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}Error: Docker is not running${NC}"
    exit 1
fi

# Check authentication only if --push is specified
if [ "$PUSH" = true ]; then
    echo -e "${BLUE}Checking GitHub Container Registry authentication...${NC}"
    if ! echo "$CR_PAT" | docker login ghcr.io -u USERNAME --password-stdin > /dev/null 2>&1; then
        echo -e "${YELLOW}Not logged in to GitHub Container Registry${NC}"
        echo ""
        echo -e "${BLUE}To authenticate, you need a GitHub Personal Access Token with 'write:packages' scope.${NC}"
        echo ""
        echo -e "${BLUE}Steps to authenticate:${NC}"
        echo -e "  1. Create a Personal Access Token at: https://github.com/settings/tokens"
        echo -e "     - Click 'Generate new token (classic)'"
        echo -e "     - Select scopes: write:packages, read:packages, delete:packages"
        echo -e "  2. Run: export CR_PAT=YOUR_TOKEN"
        echo -e "  3. Run: echo \$CR_PAT | docker login ghcr.io -u YOUR_GITHUB_USERNAME --password-stdin"
        echo ""
        read -p "Press Enter to continue after authenticating, or Ctrl+C to cancel..."
    fi
fi

# Build the Docker image
echo -e "${BLUE}Building Docker image...${NC}"
if [ "$FORCE" = true ]; then
    echo -e "${YELLOW}Force rebuild: Ignoring all cached layers${NC}"
    docker build --no-cache -t "${IMAGE_TAG_VERSIONED}" .
else
    docker build -t "${IMAGE_TAG_VERSIONED}" .
fi

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Build successful${NC}"
else
    echo -e "${RED}✗ Build failed${NC}"
    exit 1
fi

# Tag as latest if not already
if [ "$VERSION" != "latest" ]; then
    echo -e "${BLUE}Tagging as latest...${NC}"
    docker tag "${IMAGE_TAG_VERSIONED}" "${IMAGE_TAG_LATEST}"
fi

echo ""
echo -e "${GREEN}================================${NC}"
echo -e "${GREEN}Build Complete!${NC}"
echo -e "${GREEN}================================${NC}"
echo ""
echo -e "${BLUE}Local image built:${NC}"
echo -e "  ${IMAGE_TAG_VERSIONED}"
if [ "$VERSION" != "latest" ]; then
    echo -e "  ${IMAGE_TAG_LATEST}"
fi
echo ""

# Push to registry only if --push flag is set
if [ "$PUSH" = true ]; then
    echo -e "${BLUE}Pushing to GitHub Container Registry...${NC}"
    docker push "${IMAGE_TAG_VERSIONED}"
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓ Pushed ${IMAGE_TAG_VERSIONED}${NC}"
    else
        echo -e "${RED}✗ Push failed for ${IMAGE_TAG_VERSIONED}${NC}"
        exit 1
    fi
    
    if [ "$VERSION" != "latest" ]; then
        docker push "${IMAGE_TAG_LATEST}"
        if [ $? -eq 0 ]; then
            echo -e "${GREEN}✓ Pushed ${IMAGE_TAG_LATEST}${NC}"
        else
            echo -e "${RED}✗ Push failed for ${IMAGE_TAG_LATEST}${NC}"
            exit 1
        fi
    fi
    
    echo ""
    echo -e "${GREEN}================================${NC}"
    echo -e "${GREEN}Push Complete!${NC}"
    echo -e "${GREEN}================================${NC}"
    echo ""
    echo -e "${BLUE}Image published to:${NC}"
    echo -e "  ${IMAGE_TAG_VERSIONED}"
    if [ "$VERSION" != "latest" ]; then
        echo -e "  ${IMAGE_TAG_LATEST}"
    fi
    echo ""
    echo -e "${BLUE}View on GitHub:${NC}"
    echo -e "  https://github.com/${GITHUB_USER}/packages/container/${IMAGE_NAME}"
    echo ""
else
    echo -e "${YELLOW}Image built locally but not pushed to registry${NC}"
    echo -e "${YELLOW}Run with --push flag to push to GitHub Container Registry${NC}"
    echo ""
fi

echo -e "${BLUE}To use this image locally:${NC}"
echo -e "  docker run -p 8000:8000 -p 8001:8001 ${IMAGE_TAG_VERSIONED}"
echo ""
echo -e "${BLUE}Or in docker-compose.yml:${NC}"
echo -e "  image: ${IMAGE_TAG_VERSIONED}"
echo ""
