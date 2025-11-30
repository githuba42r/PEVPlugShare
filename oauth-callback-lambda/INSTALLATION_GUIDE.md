# AWS Lambda OAuth Redirector - Complete Installation Guide

## Prerequisites

Before installing the Lambda OAuth redirector, ensure you have:

### 1. **AWS Account**
- Active AWS account with appropriate permissions
- IAM user with access to Lambda, API Gateway, DynamoDB, IAM, and CloudWatch

### 2. **Local Tools**
```bash
# AWS CLI (v2)
aws --version  # Should be 2.x or later

# Terraform
terraform --version  # Should be 1.0+

# Python 3
python3 --version  # Should be 3.8+

# Git (optional, for cloning)
git --version
```

**Installation:**
```bash
# macOS
brew install awscli terraform python3

# Ubuntu/Debian
sudo apt-get install awscli terraform python3

# Windows
# Download from official sites or use chocolatey
choco install awscli terraform python
```

### 3. **AWS Credentials**
```bash
aws configure
# Enter your AWS Access Key ID
# Enter your AWS Secret Access Key
# Set default region (e.g., us-east-1)
# Set default output format (e.g., json)

# Verify credentials
aws sts get-caller-identity
```

## Step 1: Generate Secrets

Generate strong shared secrets for each application (for HMAC signature verification):

```bash
# Generate a 32-character URL-safe secret for each application
python3 -c "import secrets; print(secrets.token_urlsafe(32))"

# If deploying multiple applications, run this command for each one
python3 -c "import secrets; print(secrets.token_urlsafe(32))"
python3 -c "import secrets; print(secrets.token_urlsafe(32))"
```

**Save these secrets securely** - you'll need them for both Lambda configuration and the Android app(s).

## Step 2: Configure Terraform Variables

1. Navigate to the infrastructure directory:
```bash
cd oauth-callback-lambda/infrastructure
```

2. Create `terraform.tfvars` from the example:
```bash
cp terraform.tfvars.example terraform.tfvars
```

3. Edit `terraform.tfvars` with your application configuration(s):

**For single application:**
```hcl
aws_region = "us-east-1"
project_name = "oauth-callback"

applications = [
  {
    app_id           = "pevplugshare"
    name             = "PEV PlugShare"
    allowed_domains  = ["pevplugshare.a42r.com", "localhost:8000"]
    deep_link_scheme = "pevplugshare://oauth/callback"
    shared_secret    = "YOUR_GENERATED_SECRET"
  }
]

session_ttl_minutes = 10
oauth_providers = "openrouter,github,google"
```

**For multiple applications:**
```hcl
aws_region = "us-east-1"
project_name = "oauth-callback"

applications = [
  {
    app_id           = "pevplugshare-dev"
    name             = "PEV PlugShare (Dev)"
    allowed_domains  = ["localhost:8000", "dev.pevplugshare.a42r.com"]
    deep_link_scheme = "pevplugshare://oauth/callback"
    shared_secret    = "SECRET_1"
  },
  {
    app_id           = "pevplugshare-prod"
    name             = "PEV PlugShare (Prod)"
    allowed_domains  = ["pevplugshare.a42r.com"]
    deep_link_scheme = "pevplugshare://oauth/callback"
    shared_secret    = "SECRET_2"
  }
]

session_ttl_minutes = 10
oauth_providers = "openrouter,github,google"
```

For more examples, see [Terraform Configuration Guide](TERRAFORM_UPDATE.md).

## Step 3: Review Terraform Configuration

The `main.tf` file creates:
- **Lambda Functions**: OAuth register and callback handlers
- **API Gateway**: REST API for OAuth endpoints
- **DynamoDB Table**: Session storage with TTL
- **IAM Roles**: Proper permissions for Lambda access
- **CloudWatch Logs**: Automatic logging

Review before deploying:
```bash
terraform plan
```

## Step 4: Deploy the Infrastructure

### Option A: Using the Deployment Script (Recommended)

```bash
cd /path/to/PEVPlugShare/oauth-callback-lambda
bash scripts/deploy.sh
```

This script will:
1. ✓ Check all prerequisites
2. ✓ Build Lambda deployment packages
3. ✓ Initialize Terraform
4. ✓ Plan the deployment
5. ✓ Ask for confirmation
6. ✓ Deploy the infrastructure
7. ✓ Output API endpoint and Lambda ARNs

### Option B: Manual Deployment

```bash
cd oauth-callback-lambda/infrastructure

# Initialize Terraform
terraform init

# Plan deployment
terraform plan -out=tfplan

# Review the plan, then apply
terraform apply tfplan
```

## Step 5: Capture Deployment Outputs

After deployment, capture these important values:

```bash
cd infrastructure

# Get API endpoint
terraform output -raw api_endpoint
# Example: https://abc123.execute-api.us-east-1.amazonaws.com/prod

# Get DynamoDB table name
terraform output -raw dynamodb_table
# Example: oauth-sessions

# Get Lambda ARNs
terraform output -raw register_lambda_arn
terraform output -raw callback_lambda_arn
```

**Save these outputs** - you'll need them in your Android app configuration.

## Step 6: Configure the Android App

Update the app's `secrets.properties` file with values from your terraform.tfvars configuration:

```properties
# OAuth Configuration
OAUTH_API_ENDPOINT=https://abc123.execute-api.us-east-1.amazonaws.com/prod
OAUTH_REGISTER_ENDPOINT=${OAUTH_API_ENDPOINT}/oauth/register
OAUTH_CALLBACK_ENDPOINT=${OAUTH_API_ENDPOINT}/oauth/callback

# Shared Secret (must match the shared_secret in your terraform.tfvars for this application)
OAUTH_SHARED_SECRET=YOUR_SHARED_SECRET_FROM_TERRAFORM

# Deep Link Scheme (must match the deep_link_scheme in your terraform.tfvars for this application)
OAUTH_DEEP_LINK_SCHEME=pevplugshare://oauth/callback

# OAuth Providers
OAUTH_PROVIDERS=openrouter,github,google
```

**Important:** Ensure the `OAUTH_SHARED_SECRET` and `OAUTH_DEEP_LINK_SCHEME` match the values configured in `terraform.tfvars` for your application.

If deploying multiple applications with different shared secrets, configure each app with its corresponding secret.

Rebuild and deploy the Android app.

## Step 7: Test the Lambda Deployment

### Test 1: Register Endpoint

Generate a test signature:

```python
import hmac
import hashlib
import time

provider = "openrouter"
deep_link_url = "pevplugshare://oauth/callback"
timestamp = int(time.time())
secret = "YOUR-GENERATED-SECRET-HERE"

message = f"{provider}|{deep_link_url}|{timestamp}"
signature = hmac.new(
    secret.encode(),
    message.encode(),
    hashlib.sha256
).hexdigest()

print(f"Timestamp: {timestamp}")
print(f"Signature: {signature}")
```

Test the register endpoint:

```bash
curl -X POST https://your-api-endpoint/oauth/register \
  -H "Content-Type: application/json" \
  -H "Host: your-domain.com" \
  -d '{
    "provider": "openrouter",
    "deep_link_url": "pevplugshare://oauth/callback",
    "timestamp": <timestamp>,
    "signature": "<signature>"
  }'
```

Expected response:
```json
{
  "session_id": "550e8400-e29b-41d4-a716-446655440000",
  "callback_url": "https://your-domain.com/oauth/callback/550e8400-e29b-41d4-a716-446655440000",
  "expires_in": 600,
  "provider": "openrouter"
}
```

### Test 2: Check CloudWatch Logs

```bash
# View register function logs
aws logs tail /aws/lambda/oauth-register --follow

# View callback function logs  
aws logs tail /aws/lambda/oauth-callback --follow

# Filter for errors
aws logs filter-log-events \
  --log-group-name /aws/lambda/oauth-register \
  --filter-pattern "ERROR"
```

## Troubleshooting

### "AWS credentials not configured"
```bash
aws configure
# Enter your AWS credentials
```

### "Terraform not found"
```bash
# Install Terraform from https://www.terraform.io/downloads
# Then verify
terraform --version
```

### "unknown_application" Error
- Verify the `Host` header matches a domain in your configuration
- Check API Gateway is passing the Host header correctly

### "invalid_signature" Error
- Ensure the shared secret in the Android app matches the Lambda configuration
- Verify timestamp is current (within 5 minutes)
- Check message format: `{provider}|{deep_link_url}|{timestamp}`

### Lambda Function Not Found
```bash
# List deployed Lambda functions
aws lambda list-functions --region us-east-1

# Check specific function
aws lambda get-function --function-name oauth-register
```

### DynamoDB Issues
```bash
# Check table exists and is active
aws dynamodb describe-table --table-name oauth-sessions

# View CloudWatch metrics
aws cloudwatch get-metric-statistics \
  --namespace AWS/DynamoDB \
  --metric-name ConsumedWriteCapacityUnits \
  --dimensions Name=TableName,Value=oauth-sessions \
  --start-time 2025-11-29T00:00:00Z \
  --end-time 2025-11-30T00:00:00Z \
  --period 300 \
  --statistics Sum
```

## Post-Deployment Configuration

### 1. Enable CloudWatch Alarms

Create alarms for monitoring:

```bash
# High error rate
aws cloudwatch put-metric-alarm \
  --alarm-name oauth-register-errors \
  --alarm-description "OAuth register function errors" \
  --metric-name Errors \
  --namespace AWS/Lambda \
  --statistic Sum \
  --period 300 \
  --threshold 10 \
  --comparison-operator GreaterThanThreshold

# Throttling
aws cloudwatch put-metric-alarm \
  --alarm-name oauth-dynamodb-throttling \
  --alarm-description "DynamoDB throttling" \
  --metric-name ConsumedWriteCapacityUnits \
  --namespace AWS/DynamoDB \
  --statistic Sum \
  --period 300 \
  --threshold 100
```

### 2. Configure AWS Secrets Manager (Recommended)

Instead of storing the shared secret in Terraform variables:

```bash
aws secretsmanager create-secret \
  --name oauth-applications \
  --secret-string '[{
    "app_id": "pevplugshare",
    "name": "PEV PlugShare",
    "allowed_domains": ["your-domain.com"],
    "deep_link_scheme": "pevplugshare://oauth/callback",
    "shared_secret": "YOUR-SECRET"
  }]'
```

### 3. Set Up API Gateway Custom Domain

For a custom domain (e.g., `oauth.yourdomain.com`):

```bash
# Create ACM certificate (if not already created)
aws acm request-certificate \
  --domain-name oauth.yourdomain.com \
  --validation-method DNS

# After validation, create custom domain in API Gateway
aws apigateway create-domain-name \
  --domain-name oauth.yourdomain.com \
  --certificate-arn arn:aws:acm:...
```

## Cleanup and Destruction

To remove all AWS resources:

```bash
cd oauth-callback-lambda/infrastructure

# View what will be destroyed
terraform plan -destroy

# Destroy infrastructure
terraform destroy
```

**Warning:** This will delete all Lambda functions, API Gateway, DynamoDB tables, and other created resources.

## Cost Estimation

Monthly costs for 10,000 OAuth flows:

| Service | Cost |
|---------|------|
| Lambda Invocations | $0.004 |
| Lambda Compute | < $0.01 |
| DynamoDB (on-demand) | $0.015 |
| API Gateway | $0.02 |
| **Total** | **~$0.05/month** |

## Support and Documentation

- **Lambda Documentation**: https://docs.aws.amazon.com/lambda/
- **Terraform AWS Provider**: https://registry.terraform.io/providers/hashicorp/aws/latest
- **DynamoDB Guide**: https://docs.aws.amazon.com/dynamodb/
- **API Gateway**: https://docs.aws.amazon.com/apigateway/

## Next Steps

1. ✅ Deploy Lambda infrastructure
2. ✅ Test OAuth endpoints
3. ✅ Configure Android app
4. ✅ Monitor CloudWatch logs
5. ⏳ Set up CloudWatch alarms
6. ⏳ Migrate to AWS Secrets Manager
7. ⏳ Configure custom domain
8. ⏳ Set up CI/CD pipeline for updates
