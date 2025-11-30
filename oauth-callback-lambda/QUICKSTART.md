# AWS Lambda OAuth Redirector - Quick Start (5 minutes)

## Prerequisites Checklist

- [ ] AWS Account with credentials configured (`aws configure`)
- [ ] Terraform installed (`terraform --version`)
- [ ] Python 3 installed (`python3 --version`)
- [ ] AWS CLI v2 installed (`aws --version`)

## Quick Start Steps

### 1. Generate Secrets (30 seconds)

Generate one secret per application:

```bash
python3 -c "import secrets; print(secrets.token_urlsafe(32))"
# Example output: abc123XYZ789...
```

**Save the output(s)** - you'll need them in the next step.

### 2. Configure Terraform (2 minutes)

```bash
cd oauth-callback-lambda/infrastructure
cp terraform.tfvars.example terraform.tfvars
```

Edit `terraform.tfvars` with your application configuration:

**Single Application:**
```hcl
aws_region = "us-east-1"

applications = [
  {
    app_id           = "pevplugshare"
    name             = "PEV PlugShare"
    allowed_domains  = ["pevplugshare.a42r.com", "localhost:8000"]
    deep_link_scheme = "pevplugshare://oauth/callback"
    shared_secret    = "PASTE_YOUR_SECRET_HERE"
  }
]

session_ttl_minutes = 10
oauth_providers = "openrouter,github,google"
```

**Multiple Applications:**
```hcl
aws_region = "us-east-1"

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

### 3. Deploy (3 minutes)

```bash
cd ..
bash scripts/deploy.sh
```

Follow the prompts to deploy. When complete, you'll get:
- **API Endpoint**: `https://xxxxx.execute-api.us-east-1.amazonaws.com/prod`
- **DynamoDB Table**: `oauth-sessions`
- **Lambda Functions**: deployed and active

### 4. Configure Android App (1 minute)

Get your API endpoint:
```bash
cd infrastructure
terraform output -raw api_endpoint
```

Update your Android app's `secrets.properties`:
```properties
OAUTH_API_ENDPOINT=https://xxxxx.execute-api.us-east-1.amazonaws.com/prod
OAUTH_REGISTER_ENDPOINT=${OAUTH_API_ENDPOINT}/oauth/register
OAUTH_CALLBACK_ENDPOINT=${OAUTH_API_ENDPOINT}/oauth/callback
OAUTH_SHARED_SECRET=YOUR_SECRET_HERE
OAUTH_DEEP_LINK_SCHEME=pevplugshare://oauth/callback
OAUTH_PROVIDERS=openrouter,github,google
```

## Verification

Test the deployment:

```bash
# Check Lambda functions exist
aws lambda list-functions --region us-east-1 | grep oauth

# Check DynamoDB table
aws dynamodb describe-table --table-name oauth-sessions --region us-east-1

# Check API Gateway
aws apigateway get-rest-apis --region us-east-1

# View Lambda environment variables
aws lambda get-function-configuration \
  --function-name oauth-callback-register \
  --query 'Environment.Variables.APPLICATIONS' \
  --output text | jq .
```

## Testing Multi-Application Setup

If you configured multiple applications:

```bash
# Test with first application's domain
curl -X POST https://your-api.com/oauth/register \
  -H "Host: dev.pevplugshare.a42r.com" \
  -H "Content-Type: application/json" \
  -d '{"provider":"openrouter","deep_link_url":"pevplugshare://oauth/callback","timestamp":1234567890,"signature":"..."}'

# Test with second application's domain
curl -X POST https://your-api.com/oauth/register \
  -H "Host: pevplugshare.a42r.com" \
  -H "Content-Type: application/json" \
  -d '{"provider":"openrouter","deep_link_url":"pevplugshare://oauth/callback","timestamp":1234567890,"signature":"..."}'
```

## Next Steps

1. Update Android app `secrets.properties` with API endpoint and secret
2. Rebuild and deploy Android app
3. Test OAuth login flow with each configured domain
4. Monitor CloudWatch logs: `aws logs tail /aws/lambda/oauth-register --follow`

## Troubleshooting

| Issue | Solution |
|-------|----------|
| "At least one application must be configured" | Ensure applications array is not empty in terraform.tfvars |
| "All application fields are required" | Check all apps have: app_id, name, allowed_domains, deep_link_scheme, shared_secret |
| AWS credentials error | Run `aws configure` |
| Terraform not found | Install from https://www.terraform.io/downloads |
| Deployment failed | Run `terraform plan` and check output for errors |
| Lambda not receiving config | Verify APPLICATIONS environment variable in Lambda console |
| Request domain not allowed | Verify Host header matches an allowed_domain |

## See Also

- [Full Installation Guide](INSTALLATION_GUIDE.md)
- [Terraform Configuration Guide](TERRAFORM_UPDATE.md)
- [Deployment Documentation](LAMBDA_DEPLOYMENT.md)
- [README](README.md)
