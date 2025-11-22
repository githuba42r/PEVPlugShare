#!/bin/bash
# Build Lambda deployment packages

set -e

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}Building Lambda deployment packages...${NC}"

# Create build directory
BUILD_DIR="$(dirname "$0")/../build"
mkdir -p "$BUILD_DIR"

# Clean previous builds
rm -rf "$BUILD_DIR"/*.zip

echo -e "${GREEN}Building register function...${NC}"
cd "$(dirname "$0")/../src/register"

# Create temporary directory for package
TMP_DIR=$(mktemp -d)
trap "rm -rf $TMP_DIR" EXIT

# Copy function code
cp handler.py "$TMP_DIR/"

# Copy shared utilities
cp -r ../shared "$TMP_DIR/"

# Install dependencies if requirements.txt exists
if [ -f requirements.txt ]; then
    echo "Installing dependencies..."
    pip install -r requirements.txt -t "$TMP_DIR" --quiet
fi

# Create zip file
cd "$TMP_DIR"
zip -r "$BUILD_DIR/register.zip" . -q

echo -e "${GREEN}Building callback function...${NC}"
cd "$(dirname "$0")/../src/callback"

# Create temporary directory for package
TMP_DIR=$(mktemp -d)
trap "rm -rf $TMP_DIR" EXIT

# Copy function code
cp handler.py "$TMP_DIR/"

# Copy shared utilities
cp -r ../shared "$TMP_DIR/"

# Install dependencies if requirements.txt exists
if [ -f requirements.txt ]; then
    echo "Installing dependencies..."
    pip install -r requirements.txt -t "$TMP_DIR" --quiet
fi

# Create zip file
cd "$TMP_DIR"
zip -r "$BUILD_DIR/callback.zip" . -q

echo -e "${GREEN}✓ Build complete!${NC}"
echo -e "  register.zip: $(ls -lh $BUILD_DIR/register.zip | awk '{print $5}')"
echo -e "  callback.zip: $(ls -lh $BUILD_DIR/callback.zip | awk '{print $5}')"
