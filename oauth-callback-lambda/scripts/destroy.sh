#!/bin/bash
# Destroy OAuth callback service infrastructure

set -e

# Colors
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${RED}╔══════════════════════════════════════════════════════════╗${NC}"
echo -e "${RED}║   OAuth Callback Service - Destroy Infrastructure       ║${NC}"
echo -e "${RED}╚══════════════════════════════════════════════════════════╝${NC}"
echo ""

INFRA_DIR="$(dirname "$0")/../infrastructure"

if [ ! -d "$INFRA_DIR/.terraform" ]; then
    echo -e "${RED}✗ Terraform not initialized${NC}"
    echo -e "Run ./scripts/deploy.sh first"
    exit 1
fi

cd "$INFRA_DIR"

# Show what will be destroyed
echo -e "${BLUE}Resources to be destroyed:${NC}"
terraform plan -destroy
echo ""

# Confirm
echo -e "${YELLOW}⚠ This will destroy ALL resources. Are you sure? (type 'destroy' to confirm)${NC}"
read -r CONFIRM

if [ "$CONFIRM" != "destroy" ]; then
    echo -e "${BLUE}Cancelled${NC}"
    exit 0
fi

# Destroy
terraform destroy -auto-approve

echo -e "${BLUE}✓ All resources destroyed${NC}"
