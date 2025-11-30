# AWS Lambda OAuth Callback Service

Serverless OAuth callback handler for PEV PlugShare and other applications, deployed on AWS Lambda with API Gateway, DynamoDB, and complete infrastructure-as-code using Terraform.

## Features

- 🚀 **Serverless Architecture**: Run OAuth callbacks without managing servers
- 🔐 **HMAC Signature Verification**: Secure request validation using shared secrets
- 🌐 **Multi-Application Support**: Handle multiple apps with different configurations
- 💾 **DynamoDB Sessions**: Persistent session storage with automatic TTL cleanup
- 📊 **CloudWatch Logging**: Complete request/response logging for debugging
- 🏗️ **Infrastructure as Code**: Terraform configuration for reproducible deployments
- 💰 **Low Cost**: ~$0.05/month for 10,000 OAuth flows
- ⚡ **Fast**: API Gateway + Lambda for minimal latency

## Quick Links

- **[Quick Start (5 minutes)](QUICKSTART.md)** - Deploy in 5 minutes (single or multiple apps)
- **[Full Installation Guide](INSTALLATION_GUIDE.md)** - Detailed setup instructions
- **[Terraform Configuration Guide](TERRAFORM_UPDATE.md)** - Multi-application configuration examples
- **[Deployment Documentation](LAMBDA_DEPLOYMENT.md)** - Configuration and best practices

## Architecture

```
┌─────────────────────────────────────────────────────┐
│  Mobile App                                         │
│  (OAuth Login)                                      │
└────────────────────┬────────────────────────────────┘
                     │
                     │ HTTP Request
                     ▼
┌─────────────────────────────────────────────────────┐
│  AWS API Gateway                                    │
│  - Route: POST /oauth/register                      │
│  - Route: GET  /oauth/callback/{session_id}        │
└────────────────────┬────────────────────────────────┘
                     │
        ┌────────────┴────────────┐
        │                         │
        ▼                         ▼
   ┌────────────┐          ┌──────────────┐
   │  Lambda    │          │   Lambda     │
   │  Register  │          │   Callback   │
   │  Function  │          │   Function   │
   └────────────┘          └──────────────┘
        │                         │
        └────────────┬────────────┘
                     │
                     ▼
        ┌─────────────────────────┐
        │   DynamoDB Table        │
        │   oauth-sessions        │
        │   (TTL Cleanup)         │
        └─────────────────────────┘
```

## Project Structure

```
oauth-callback-lambda/
├── README.md                      # This file
├── QUICKSTART.md                 # 5-minute quick start
├── INSTALLATION_GUIDE.md         # Detailed installation
├── TERRAFORM_UPDATE.md           # Multi-app Terraform guide
├── LAMBDA_DEPLOYMENT.md          # Configuration reference
├── docker/                        # Docker build files
│   └── Dockerfile               # Lambda container image
├── infrastructure/               # Terraform configuration
│   ├── main.tf                  # Main infrastructure
│   ├── terraform.tfvars.example # Configuration template (UPDATED for multi-app)
│   └── terraform.tfvars         # (create this file)
├── scripts/                       # Deployment scripts
│   ├── deploy.sh                # Main deployment script
│   ├── build.sh                 # Build Lambda packages
│   └── destroy.sh               # Cleanup script
└── src/                           # Lambda function code
    ├── register.py              # OAuth register handler
    ├── callback.py              # OAuth callback handler
    ├── applications.py          # App configuration (supports multiple apps)
    ├── dynamo.py                # DynamoDB operations
    ├── security.py              # Signature verification
    └── requirements.txt         # Python dependencies
```

## Installation

### Prerequisites

```bash
# Check each tool is installed
aws --version          # AWS CLI v2
terraform --version    # Terraform 1.0+
python3 --version      # Python 3.8+
```

See [Full Installation Guide](INSTALLATION_GUIDE.md) for detailed setup.

### 5-Minute Quick Start

```bash
# 1. Generate secret(s) for your application(s)
python3 -c "import secrets; print(secrets.token_urlsafe(32))"

# 2. Configure Terraform with your application(s)
cd infrastructure
cp terraform.tfvars.example terraform.tfvars
# Edit terraform.tfvars with your application configuration
# Supports: single app, multiple domains per app, or multiple apps

# 3. Deploy
cd ..
bash scripts/deploy.sh

# 4. Get API endpoint
cd infrastructure
terraform output -raw api_endpoint
```

**Supports:**
- ✅ Single application with single domain
- ✅ Single application with multiple domains (dev/staging/prod)
- ✅ Multiple different applications in one deployment

See [Quick Start Guide](QUICKSTART.md) for more details and examples.

## Configuration

### Multi-Application Support

Terraform now supports configuring multiple applications with different domains and deep link schemes:

**Single Application:**
```hcl
applications = [
  {
    app_id           = "pevplugshare"
    name             = "PEV PlugShare"
    allowed_domains  = ["pevplugshare.a42r.com"]
    deep_link_scheme = "pevplugshare://oauth/callback"
    shared_secret    = "your-secret"
  }
]
```

**Multiple Applications:**
```hcl
applications = [
  {
    app_id           = "app1"
    name             = "App One"
    allowed_domains  = ["app1.example.com"]
    deep_link_scheme = "app1://oauth/callback"
    shared_secret    = "secret-1"
  },
  {
    app_id           = "app2"
    name             = "App Two"
    allowed_domains  = ["app2.example.com"]
    deep_link_scheme = "app2://oauth/callback"
    shared_secret    = "secret-2"
  }
]
```

For complete configuration examples, see [Terraform Configuration Guide](TERRAFORM_UPDATE.md).

### Environment Variables

The Lambda functions read configuration from environment variables:

| Variable | Default | Description |
|----------|---------|-------------|
| `APPLICATIONS` | Required | JSON array of app configurations |
| `DYNAMODB_TABLE` | `oauth-sessions` | DynamoDB table name |
| `SESSION_TTL_MINUTES` | `10` | Session expiration time |
| `OAUTH_PROVIDERS` | `openrouter,github,google` | Allowed OAuth providers |

### Application Configuration

Each application in the `APPLICATIONS` array must specify:

```json
{
  "app_id": "pevplugshare",
  "name": "PEV PlugShare",
  "allowed_domains": ["pevplugshare.a42r.com"],
  "deep_link_scheme": "pevplugshare://oauth/callback",
  "shared_secret": "your-secret-here"
}
```

## Usage

### Register OAuth Session

```bash
curl -X POST https://your-api-endpoint/oauth/register \
  -H "Content-Type: application/json" \
  -H "Host: your-domain.com" \
  -d '{
    "provider": "openrouter",
    "deep_link_url": "pevplugshare://oauth/callback",
    "timestamp": 1234567890,
    "signature": "abc123..."
  }'
```

**Response:**
```json
{
  "session_id": "550e8400-e29b-41d4-a716-446655440000",
  "callback_url": "https://your-domain.com/oauth/callback/550e8400-e29b-41d4-a716-446655440000",
  "expires_in": 600,
  "provider": "openrouter"
}
```

### Handle Callback

The OAuth provider redirects to:
```
https://your-domain.com/oauth/callback/550e8400-e29b-41d4-a716-446655440000?code=...&state=...
```

Lambda processes and redirects back to:
```
pevplugshare://oauth/callback?session_id=550e8400-e29b-41d4-a716-446655440000&code=...&state=...
```

## Security

### HMAC Signature Verification

All requests are validated using HMAC-SHA256:

```python
message = f"{provider}|{deep_link_url}|{timestamp}"
signature = hmac.new(
    secret.encode(),
    message.encode(),
    hashlib.sha256
).hexdigest()
```

### Best Practices

1. **Store secrets securely** in AWS Secrets Manager
2. **Rotate secrets regularly** using rotation lambda
3. **Enable CloudWatch alarms** for suspicious activity
4. **Use HTTPS only** for all API calls
5. **Validate timestamps** to prevent replay attacks
6. **Enable DynamoDB encryption** at rest

See [Deployment Documentation](LAMBDA_DEPLOYMENT.md) for detailed security guidelines.

## Monitoring

### CloudWatch Logs

View logs for debugging:

```bash
# Register function logs
aws logs tail /aws/lambda/oauth-register --follow

# Callback function logs
aws logs tail /aws/lambda/oauth-callback --follow

# Filter for errors
aws logs filter-log-events \
  --log-group-name /aws/lambda/oauth-register \
  --filter-pattern "ERROR"
```

### CloudWatch Metrics

Monitor Lambda performance:

```bash
# View invocations
aws cloudwatch get-metric-statistics \
  --namespace AWS/Lambda \
  --metric-name Invocations \
  --statistics Sum \
  --start-time 2025-11-29T00:00:00Z \
  --end-time 2025-11-30T00:00:00Z \
  --period 300
```

## Troubleshooting

| Issue | Solution |
|-------|----------|
| "unknown_application" | Verify Host header matches allowed_domains |
| "invalid_signature" | Check shared secret and message format |
| Lambda timeout | Increase timeout in Lambda configuration |
| DynamoDB errors | Check table exists and has correct IAM permissions |
| Cold starts slow | Lambda functions optimize automatically |

See [Installation Guide - Troubleshooting](INSTALLATION_GUIDE.md#troubleshooting) for more.

## Cost Estimation

For 10,000 monthly OAuth flows:

| Service | Cost |
|---------|------|
| Lambda (20,000 invocations) | $0.004 |
| Lambda Compute | < $0.01 |
| DynamoDB (on-demand) | $0.015 |
| API Gateway (20,000 requests) | $0.02 |
| **Total Monthly** | **~$0.05** |

Costs scale linearly with volume. Use on-demand DynamoDB billing for predictable costs.

## Deployment Management

### Update Lambda Code

```bash
# Rebuild and redeploy
bash scripts/build.sh
cd infrastructure
terraform apply
```

### Destroy Infrastructure

```bash
cd infrastructure
terraform destroy
```

**Warning:** This deletes all resources including Lambda functions and DynamoDB data.

## Development

### Local Testing

```bash
# Set environment variables
export APPLICATIONS='[...]'
export DYNAMODB_TABLE=oauth-sessions

# Run locally (requires DynamoDB local)
python3 src/register.py
```

### Building Docker Image

```bash
cd docker
docker build -t oauth-callback:latest .
docker run -e APPLICATIONS='[...]' oauth-callback:latest
```

## API Reference

### POST /oauth/register

Register a new OAuth session.

**Request:**
```json
{
  "provider": "openrouter",
  "deep_link_url": "pevplugshare://oauth/callback",
  "timestamp": 1234567890,
  "signature": "abc123..."
}
```

**Response (200 OK):**
```json
{
  "session_id": "...",
  "callback_url": "https://...",
  "expires_in": 600,
  "provider": "openrouter"
}
```

**Errors:**
- `400` - Invalid request
- `401` - Invalid signature
- `403` - Unknown application
- `500` - Server error

### GET /oauth/callback/{session_id}

Handle OAuth provider callback.

**Query Parameters:**
- `code` - Authorization code from provider
- `state` - State parameter from provider
- `error` - Error from provider (if present)

**Response:**
Redirects to deep link URL with parameters.

## Support

- **Issues**: Check [Troubleshooting Guide](INSTALLATION_GUIDE.md#troubleshooting)
- **Questions**: See [Full Documentation](LAMBDA_DEPLOYMENT.md)
- **AWS Support**: https://console.aws.amazon.com/support/

## License

Same as PEV PlugShare project

## Contributing

Contributions welcome! Please ensure:
1. Code follows existing style
2. All tests pass
3. CloudWatch logs are helpful
4. Documentation is updated
