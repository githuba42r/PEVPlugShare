#!/bin/bash
# Deploy OAuth callback service to AWS

set -e

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}╔══════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║   OAuth Callback Service - AWS Lambda Deployment        ║${NC}"
echo -e "${BLUE}╚══════════════════════════════════════════════════════════╝${NC}"
echo ""

# Check prerequisites
echo -e "${BLUE}Checking prerequisites...${NC}"

# Check AWS CLI
if ! command -v aws &> /dev/null; then
    echo -e "${RED}✗ AWS CLI not found. Please install: https://aws.amazon.com/cli/${NC}"
    exit 1
fi
echo -e "${GREEN}✓ AWS CLI found${NC}"

# Check Terraform
if ! command -v terraform &> /dev/null; then
    echo -e "${RED}✗ Terraform not found. Please install: https://www.terraform.io/downloads${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Terraform found${NC}"

# Check Python
if ! command -v python3 &> /dev/null; then
    echo -e "${RED}✗ Python 3 not found${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Python 3 found${NC}"

# Check AWS credentials
if ! aws sts get-caller-identity &> /dev/null; then
    echo -e "${RED}✗ AWS credentials not configured${NC}"
    echo -e "${YELLOW}Run: aws configure${NC}"
    exit 1
fi
echo -e "${GREEN}✓ AWS credentials configured${NC}"

ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
echo -e "  Account ID: ${ACCOUNT_ID}"
echo ""

# Check terraform.tfvars
INFRA_DIR="$(dirname "$0")/../infrastructure"
if [ ! -f "$INFRA_DIR/terraform.tfvars" ]; then
    echo -e "${YELLOW}⚠ terraform.tfvars not found${NC}"
    echo -e "${YELLOW}Creating from example...${NC}"
    cp "$INFRA_DIR/terraform.tfvars.example" "$INFRA_DIR/terraform.tfvars"
    echo -e "${RED}Please edit infrastructure/terraform.tfvars with your configuration:${NC}"
    echo -e "  - domain_name"
    echo -e "  - shared_secret"
    echo ""
    echo -e "Then run this script again."
    exit 1
fi

# Step 1: Build Lambda packages
echo -e "${BLUE}Step 1: Building Lambda deployment packages...${NC}"
bash "$(dirname "$0")/build.sh"
echo ""

# Step 2: Initialize Terraform
echo -e "${BLUE}Step 2: Initializing Terraform...${NC}"
cd "$INFRA_DIR"
terraform init
echo ""

# Step 3: Plan deployment
echo -e "${BLUE}Step 3: Planning deployment...${NC}"
terraform plan -out=tfplan
echo ""

# Step 4: Confirm deployment
echo -e "${YELLOW}Ready to deploy. Continue? (yes/no)${NC}"
read -r CONFIRM

if [ "$CONFIRM" != "yes" ]; then
    echo -e "${RED}Deployment cancelled${NC}"
    exit 0
fi

# Step 5: Apply Terraform
echo -e "${BLUE}Step 4: Deploying infrastructure...${NC}"
terraform apply tfplan
echo ""

# Step 6: Get outputs
echo -e "${GREEN}✓ Deployment complete!${NC}"
echo ""
echo -e "${BLUE}API Endpoint:${NC}"
API_ENDPOINT=$(terraform output -raw api_endpoint)
echo -e "  ${API_ENDPOINT}"
echo ""

echo -e "${BLUE}DynamoDB Table:${NC}"
DYNAMODB_TABLE=$(terraform output -raw dynamodb_table)
echo -e "  ${DYNAMODB_TABLE}"
echo ""

echo -e "${BLUE}Lambda Functions:${NC}"
echo -e "  Register: $(terraform output -raw register_lambda_arn)"
echo -e "  Callback: $(terraform output -raw callback_lambda_arn)"
echo ""

echo -e "${GREEN}Next steps:${NC}"
echo -e "  1. Update your .env file with:"
echo -e "     DOMAIN_NAME=${API_ENDPOINT}"
echo -e "  2. Test the service:"
echo -e "     curl ${API_ENDPOINT}/oauth/register"
echo -e "  3. Configure your Android app with the API endpoint"
echo ""

echo -e "${BLUE}To destroy the infrastructure later:${NC}"
echo -e "  cd infrastructure && terraform destroy"
