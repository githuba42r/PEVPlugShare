#!/bin/bash

# Setup script for PEV PlugShare OAuth configuration
# This script configures the OAuth redirect service for the PlugShare app

set -e  # Exit on error

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}================================${NC}"
echo -e "${BLUE}PEV PlugShare OAuth Setup${NC}"
echo -e "${BLUE}================================${NC}"
echo ""

# Configuration
APP_ID="pevplugshare"
APP_NAME="PEV PlugShare"
ALLOWED_DOMAINS="pevplugshare.a42r.com,pevplugshare-prod.a42r.com,localhost:8000,localhost:8082"
DEEP_LINK_SCHEME="pevplugshare://oauth/callback"
SHARED_SECRET="eSYpX2jtO_7QKoiOKTRq7Bw4It4prX05eqBXzbty8NA"

echo -e "${BLUE}Configuration:${NC}"
echo -e "  App ID:           ${APP_ID}"
echo -e "  App Name:         ${APP_NAME}"
echo -e "  Allowed Domains:  ${ALLOWED_DOMAINS}"
echo -e "  Deep Link Scheme: ${DEEP_LINK_SCHEME}"
echo -e "  Shared Secret:    ${SHARED_SECRET:0:20}... (truncated)"
echo ""

# Check if running inside container or on host
if [ -f "/app/manage_db.py" ]; then
    # Running inside container
    echo -e "${BLUE}Running inside container...${NC}"
    MANAGE_CMD="python /app/manage_db.py"
else
    # Running on host - use docker exec
    echo -e "${BLUE}Running on host, checking for container...${NC}"
    
    # Check if container exists
    if ! docker ps --filter "name=oauth-callback-service" --format "{{.Names}}" | grep -q "oauth-callback-service"; then
        echo -e "${RED}Error: oauth-callback-service container not running${NC}"
        echo -e "${YELLOW}Please start the container first with: docker-compose up -d${NC}"
        exit 1
    fi
    
    MANAGE_CMD="docker exec oauth-callback-service python manage_db.py"
fi

# Check if app already exists
echo -e "${BLUE}Checking if application already exists...${NC}"
if $MANAGE_CMD show "$APP_ID" > /dev/null 2>&1; then
    echo -e "${YELLOW}Application '${APP_ID}' already exists. Updating configuration...${NC}"
    
    # Update domains
    echo -e "${BLUE}Updating allowed domains...${NC}"
    $MANAGE_CMD update-domains "$APP_ID" "$ALLOWED_DOMAINS"
    
    # Update secret
    echo -e "${BLUE}Updating shared secret...${NC}"
    $MANAGE_CMD update-secret "$APP_ID" "$SHARED_SECRET"
    
    # Enable the app (in case it was disabled)
    echo -e "${BLUE}Enabling application...${NC}"
    $MANAGE_CMD enable "$APP_ID"
    
else
    # Add new application
    echo -e "${BLUE}Adding new application...${NC}"
    $MANAGE_CMD add "$APP_ID" "$APP_NAME" "$ALLOWED_DOMAINS" "$DEEP_LINK_SCHEME" "$SHARED_SECRET"
fi

echo ""
echo -e "${GREEN}================================${NC}"
echo -e "${GREEN}Setup Complete!${NC}"
echo -e "${GREEN}================================${NC}"
echo ""

# Show the application details
echo -e "${BLUE}Application details:${NC}"
$MANAGE_CMD show "$APP_ID"

echo ""
echo -e "${YELLOW}⚠️  Important:${NC}"
echo -e "  1. Copy the shared secret to your Android app configuration"
echo -e "  2. The secret is: ${SHARED_SECRET}"
echo -e "  3. Use app_id '${APP_ID}' when making OAuth requests"
echo ""
echo -e "${BLUE}OAuth endpoints:${NC}"
echo -e "  Register: POST https://oauth.a42r.com/oauth/register"
echo -e "  Callback: GET  https://oauth.a42r.com/oauth/callback/{session_id}"
echo ""
