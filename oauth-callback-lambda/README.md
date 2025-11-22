# OAuth Callback Service - AWS Lambda Implementation

Production-ready OAuth 2.0 callback handler deployed on AWS Lambda with automated infrastructure provisioning.

## Features

- ✅ **Serverless Architecture**: AWS Lambda + API Gateway + DynamoDB
- 🔒 **HMAC-SHA256 Security**: Request signature verification
- 💰 **Cost-Effective**: Pay only for what you use (~$1/month for typical usage)
- 🚀 **Auto-Scaling**: Handles traffic spikes automatically
- 📊 **Monitoring**: CloudWatch logs and metrics
- 🛠️ **Infrastructure as Code**: Terraform for reproducible deployments
- 🐳 **Local Development**: Docker Compose with LocalStack
- 📦 **Automated Deployment**: One-command deployment script

## Architecture

```
Client App → API Gateway → Lambda Functions → DynamoDB
                ↓                                ↓
         CloudWatch Logs              TTL Auto-Cleanup
```

### Components

- **API Gateway**: RESTful API endpoints
- **Lambda Functions**:
  - `register`: Creates OAuth sessions
  - `callback`: Handles OAuth provider redirects
- **DynamoDB**: Session storage with TTL
- **CloudWatch**: Logging and monitoring
- **IAM**: Least-privilege security roles

## Quick Start

### Prerequisites

1. **AWS Account** with appropriate permissions
2. **AWS CLI** configured: `aws configure`
3. **Terraform** >= 1.0: https://www.terraform.io/downloads
4. **Python 3.12**
5. **Bash** shell

### Deployment

**1. Clone and navigate**:
```bash
cd oauth-callback-lambda
```

**2. Configure**:
```bash
cp infrastructure/terraform.tfvars.example infrastructure/terraform.tfvars
```

Edit `infrastructure/terraform.tfvars`:
```hcl
aws_region = "us-east-1"
project_name = "oauth-callback"

# REQUIRED
domain_name = "https://your-api-gateway-url.execute-api.us-east-1.amazonaws.com/prod"
shared_secret = "GENERATE_WITH: python -c 'import secrets; print(secrets.token_urlsafe(32))'"

# Optional
session_ttl_minutes = 10
oauth_providers = "openrouter,github,google"
deep_link_scheme = "pevplugshare://oauth/callback"
```

**3. Deploy**:
```bash
./scripts/deploy.sh
```

The script will:
- ✅ Check prerequisites (AWS CLI, Terraform, Python)
- ✅ Build Lambda deployment packages
- ✅ Initialize Terraform
- ✅ Plan infrastructure changes
- ✅ Deploy to AWS
- ✅ Output API endpoint URL

**Expected output**:
```
✓ Deployment complete!

API Endpoint:
  https://abc123xyz.execute-api.us-east-1.amazonaws.com/prod

DynamoDB Table:
  oauth-callback-sessions

Lambda Functions:
  Register: arn:aws:lambda:us-east-1:123456789:function:oauth-callback-register
  Callback: arn:aws:lambda:us-east-1:123456789:function:oauth-callback-callback
```

**4. Update configuration**:

Update `infrastructure/terraform.tfvars` with the actual API Gateway URL:
```hcl
domain_name = "https://abc123xyz.execute-api.us-east-1.amazonaws.com/prod"
```

Then redeploy:
```bash
cd infrastructure
terraform apply
```

### Testing Deployment

```bash
# Generate signature
SECRET="your-shared-secret"
PROVIDER="openrouter"
TIMESTAMP=$(date +%s)
MESSAGE="${PROVIDER}|${TIMESTAMP}"
SIGNATURE=$(echo -n "$MESSAGE" | openssl dgst -sha256 -hmac "$SECRET" | awk '{print $2}')

# Test registration
curl -X POST https://your-api-url/prod/oauth/register \
  -H "Content-Type: application/json" \
  -d "{
    \"provider\": \"$PROVIDER\",
    \"timestamp\": $TIMESTAMP,
    \"signature\": \"$SIGNATURE\"
  }"
```

## Local Development with Docker

### Quick Start

**1. Navigate to Docker directory**:
```bash
cd docker
```

**2. Configure environment**:
```bash
cp .env.example .env
# Edit .env if needed
```

**3. Start services**:
```bash
docker-compose up -d
```

This starts:
- **LocalStack**: Local AWS services (DynamoDB, Lambda)
- **Lambda functions**: Register and Callback handlers
- **API Gateway**: Nginx proxy at http://localhost:3000
- **DynamoDB Admin**: Web UI at http://localhost:8001

**4. Initialize DynamoDB**:
```bash
./init-localstack.sh
```

**5. Test locally**:
```bash
python3 test-local.py
```

**6. Access services**:
- API: http://localhost:3000
- DynamoDB Admin: http://localhost:8001
- LocalStack: http://localhost:4566

**7. Stop services**:
```bash
docker-compose down
```

### Development Workflow

1. **Edit Lambda code**: `src/register/` or `src/callback/`
2. **Rebuild containers**:
   ```bash
   docker-compose up -d --build
   ```
3. **Test changes**:
   ```bash
   python3 test-local.py
   ```
4. **View logs**:
   ```bash
   docker-compose logs -f lambda-register
   docker-compose logs -f lambda-callback
   ```

## Project Structure

```
oauth-callback-lambda/
├── src/
│   ├── shared/              # Shared utilities
│   │   ├── security.py      # HMAC signature functions
│   │   ├── responses.py     # API Gateway response helpers
│   │   └── templates.py     # HTML page templates
│   ├── register/
│   │   ├── handler.py       # Register Lambda function
│   │   └── requirements.txt
│   └── callback/
│       ├── handler.py       # Callback Lambda function
│       └── requirements.txt
├── infrastructure/
│   ├── main.tf              # Terraform configuration
│   └── terraform.tfvars.example
├── scripts/
│   ├── build.sh             # Build Lambda packages
│   ├── deploy.sh            # Deploy to AWS
│   └── destroy.sh           # Destroy infrastructure
├── docker/
│   ├── docker-compose.yml   # Local development setup
│   ├── Dockerfile.register  # Register Lambda image
│   ├── Dockerfile.callback  # Callback Lambda image
│   ├── nginx.conf           # API Gateway proxy
│   ├── init-localstack.sh   # Initialize DynamoDB
│   └── test-local.py        # Local testing script
├── build/                   # Generated deployment packages
└── README.md
```

## API Endpoints

### POST /oauth/register

Register a new OAuth session.

**Request**:
```json
{
  "provider": "openrouter",
  "timestamp": 1700000000,
  "signature": "hmac-sha256-hex"
}
```

**Response** (200):
```json
{
  "session_id": "uuid",
  "callback_url": "https://api-url/oauth/callback/uuid",
  "expires_in": 600,
  "provider": "openrouter"
}
```

### GET /oauth/callback/{session_id}

Handle OAuth provider callback.

**Query Parameters**:
- `code`: Authorization code (from OAuth provider)
- `error`: Error code (if authorization failed)

**Response**: HTML redirect page to mobile app deep link

## Cost Estimation

### Monthly Costs (1,000 OAuth flows/month)

| Service | Usage | Cost |
|---------|-------|------|
| Lambda (Register) | 1,000 invocations × 100ms | $0.00 (free tier) |
| Lambda (Callback) | 1,000 invocations × 100ms | $0.00 (free tier) |
| API Gateway | 2,000 requests | $0.007 |
| DynamoDB | 2,000 read/write units | $0.05 |
| CloudWatch Logs | 1 GB | $0.50 |
| **Total** | | **~$0.56/month** |

### Cost Optimization

- DynamoDB TTL automatically deletes expired sessions (free)
- Pay-per-request billing (no reserved capacity)
- Lambda free tier: 1M requests/month
- API Gateway free tier: 1M requests/month (first 12 months)

## Monitoring

### CloudWatch Logs

**View Register function logs**:
```bash
aws logs tail /aws/lambda/oauth-callback-register --follow
```

**View Callback function logs**:
```bash
aws logs tail /aws/lambda/oauth-callback-callback --follow
```

### CloudWatch Metrics

**Lambda invocations**:
```bash
aws cloudwatch get-metric-statistics \
  --namespace AWS/Lambda \
  --metric-name Invocations \
  --dimensions Name=FunctionName,Value=oauth-callback-register \
  --start-time 2024-11-01T00:00:00Z \
  --end-time 2024-11-22T23:59:59Z \
  --period 3600 \
  --statistics Sum
```

### DynamoDB

**View active sessions**:
```bash
aws dynamodb scan \
  --table-name oauth-callback-sessions \
  --filter-expression "used = :false AND expires_at > :now" \
  --expression-attribute-values '{":false": {"BOOL": false}, ":now": {"N": "'$(date +%s)'"}}'
```

**Session count**:
```bash
aws dynamodb describe-table \
  --table-name oauth-callback-sessions \
  --query 'Table.ItemCount'
```

## Security Best Practices

1. **Rotate secrets regularly**:
   ```bash
   # Generate new secret
   python -c "import secrets; print(secrets.token_urlsafe(32))"
   
   # Update Terraform
   # Update Android app
   ```

2. **Enable AWS CloudTrail** for audit logging

3. **Use AWS Secrets Manager** for production:
   ```hcl
   # In main.tf
   data "aws_secretsmanager_secret_version" "shared_secret" {
     secret_id = "oauth-callback-shared-secret"
   }
   ```

4. **Restrict API Gateway access** with WAF:
   ```hcl
   resource "aws_wafv2_web_acl" "oauth_api" {
     # Rate limiting rules
   }
   ```

5. **Enable API Gateway logging**:
   ```hcl
   resource "aws_api_gateway_stage" "prod" {
     access_log_settings {
       destination_arn = aws_cloudwatch_log_group.api_gateway.arn
       format          = "$requestId"
     }
   }
   ```

## Troubleshooting

### Deployment Issues

**"AWS credentials not configured"**:
```bash
aws configure
# Enter your Access Key ID, Secret Access Key, and region
```

**"Terraform not found"**:
```bash
# macOS
brew install terraform

# Linux
wget https://releases.hashicorp.com/terraform/1.6.0/terraform_1.6.0_linux_amd64.zip
unzip terraform_1.6.0_linux_amd64.zip
sudo mv terraform /usr/local/bin/
```

**"Function package too large"**:
```bash
# Reduce dependencies in requirements.txt
# Use Lambda layers for common dependencies
```

### Runtime Issues

**"Invalid signature" errors**:
- Verify `shared_secret` matches between Terraform and Android app
- Check timestamp is within ±5 minutes
- Ensure message format: `{provider}|{timestamp}`

**"Session not found"**:
- Sessions expire after 10 minutes
- Check DynamoDB table exists
- Verify Lambda has DynamoDB permissions

**"No authorization code"**:
- Check OAuth provider configuration
- Verify callback URL matches API Gateway endpoint

### Local Development Issues

**LocalStack not starting**:
```bash
# Check Docker is running
docker ps

# View LocalStack logs
docker-compose logs localstack

# Restart services
docker-compose down
docker-compose up -d
```

**DynamoDB table not created**:
```bash
# Run init script manually
./init-localstack.sh

# Or create table via AWS CLI
aws dynamodb create-table ... --endpoint-url http://localhost:4566
```

## Updating the Service

### Code Changes

1. Edit Lambda function code in `src/`
2. Rebuild packages:
   ```bash
   ./scripts/build.sh
   ```
3. Deploy changes:
   ```bash
   cd infrastructure
   terraform apply
   ```

### Configuration Changes

1. Edit `infrastructure/terraform.tfvars`
2. Apply changes:
   ```bash
   cd infrastructure
   terraform apply
   ```

## Destroying Infrastructure

**⚠️ Warning**: This will delete all resources and data.

```bash
./scripts/destroy.sh
```

Or manually:
```bash
cd infrastructure
terraform destroy
```

## Migration from FastAPI Service

If migrating from the FastAPI implementation:

1. **Deploy Lambda version** alongside FastAPI
2. **Update Android app** with new API endpoint
3. **Test thoroughly** in production
4. **Monitor CloudWatch** for errors
5. **Decommission FastAPI** once stable
6. **Update Cloudflare Tunnel** to point to API Gateway (optional)

## Advanced Configuration

### Custom Domain

Add custom domain to API Gateway:

```hcl
# In main.tf
resource "aws_api_gateway_domain_name" "oauth" {
  domain_name = "oauth.yourdomain.com"
  certificate_arn = aws_acm_certificate.oauth.arn
}

resource "aws_api_gateway_base_path_mapping" "oauth" {
  api_id      = aws_api_gateway_rest_api.oauth_api.id
  stage_name  = aws_api_gateway_stage.prod.stage_name
  domain_name = aws_api_gateway_domain_name.oauth.domain_name
}
```

### Multi-Region Deployment

Deploy to multiple regions for low latency:

```bash
# Deploy to us-east-1
terraform workspace new us-east-1
terraform apply -var="aws_region=us-east-1"

# Deploy to eu-west-1
terraform workspace new eu-west-1
terraform apply -var="aws_region=eu-west-1"
```

### VPC Integration

Deploy Lambdas in VPC for enhanced security:

```hcl
resource "aws_lambda_function" "register" {
  vpc_config {
    subnet_ids         = var.subnet_ids
    security_group_ids = var.security_group_ids
  }
}
```

## Support

For issues or questions:
- Check CloudWatch Logs for Lambda errors
- Review DynamoDB table for session data
- Test locally with Docker first
- Verify AWS IAM permissions
- Check API Gateway execution logs

## License

Part of PEV PlugShare Android application.
