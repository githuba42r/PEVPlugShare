#!/bin/bash
# Build Lambda deployment packages

set -e

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}Building Lambda deployment packages...${NC}"

# Check Python 3 is available
if ! command -v python3 &> /dev/null; then
    echo -e "${RED}✗ Python 3 not found. Please install Python 3.8 or later.${NC}"
    exit 1
fi

PYTHON_VERSION=$(python3 --version | awk '{print $2}' | cut -d. -f1,2)
echo -e "${GREEN}Using Python ${PYTHON_VERSION}${NC}"

# Get the absolute path to build directory
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
BUILD_DIR="${SCRIPT_DIR}/../build"
mkdir -p "$BUILD_DIR"

# Clean previous builds
rm -f "${BUILD_DIR}"/*.zip

# Function to build Lambda package
build_lambda_function() {
    local FUNC_NAME=$1
    local SRC_DIR=$2
    
    echo -e "${GREEN}Building ${FUNC_NAME} function...${NC}"
    cd "$SRC_DIR"
    
    # Create temporary directory for package
    TMP_DIR=$(mktemp -d)
    trap "rm -rf $TMP_DIR" EXIT
    
    # Copy function code
    cp handler.py "$TMP_DIR/"
    
    # Copy shared utilities
    if [ -d "../shared" ]; then
        cp -r ../shared "$TMP_DIR/"
    fi
    
    # Install dependencies if requirements.txt exists
    if [ -f requirements.txt ]; then
        echo -e "${YELLOW}  Installing dependencies...${NC}"
        
        # Filter out boto3 (it's built into Lambda runtime)
        # Create a temporary requirements file without boto3
        if grep -q "boto3" requirements.txt; then
            grep -v "boto3" requirements.txt > "$TMP_DIR/requirements_filtered.txt" 2>/dev/null || touch "$TMP_DIR/requirements_filtered.txt"
        else
            cp requirements.txt "$TMP_DIR/requirements_filtered.txt"
        fi
        
        if [ -s "$TMP_DIR/requirements_filtered.txt" ]; then
            python3 -m pip install -r "$TMP_DIR/requirements_filtered.txt" -t "$TMP_DIR" --quiet 2>/dev/null || true
            rm "$TMP_DIR/requirements_filtered.txt"
        else
            echo -e "${YELLOW}  (boto3 will be provided by Lambda runtime)${NC}"
        fi
    fi
    
    # Create zip file
    cd "$TMP_DIR"
    zip -r "${BUILD_DIR}/${FUNC_NAME}.zip" . -q
    
    if [ -f "${BUILD_DIR}/${FUNC_NAME}.zip" ]; then
        echo -e "${GREEN}✓ ${FUNC_NAME}.zip created successfully${NC}"
    else
        echo -e "${RED}✗ Failed to create ${FUNC_NAME}.zip${NC}"
        exit 1
    fi
}

# Build both functions
build_lambda_function "register" "${SCRIPT_DIR}/../src/register"
build_lambda_function "callback" "${SCRIPT_DIR}/../src/callback"

echo -e ""
echo -e "${GREEN}✓ Build complete!${NC}"
echo -e "  register.zip: $(ls -lh ${BUILD_DIR}/register.zip | awk '{print $5}')"
echo -e "  callback.zip: $(ls -lh ${BUILD_DIR}/callback.zip | awk '{print $5}')"
