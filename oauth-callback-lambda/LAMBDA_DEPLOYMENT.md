# AWS Lambda OAuth Callback - Multi-Application Setup

## Overview

The Lambda version of the OAuth callback service now supports multiple applications via environment variable configuration. Each application has its own allowed domains, deep link scheme, and shared secret.

## Environment Variables

### Required Configuration

#### `APPLICATIONS` (JSON array)
Multi-application configuration. Each application must specify:
- `app_id`: Unique identifier
- `name`: Human-readable name
- `allowed_domains`: List of allowed request hostnames
- `deep_link_scheme`: Deep link URL scheme for callbacks
- `shared_secret`: HMAC secret for signature verification

**Example:**
```json
[
  {
    "app_id": "pevplugshare",
    "name": "PEV PlugShare",
    "allowed_domains": ["pevplugshare.a42r.com", "pevplugshare-prod.a42r.com"],
    "deep_link_scheme": "pevplugshare://oauth/callback",
    "shared_secret": "your-production-secret-here"
  },
  {
    "app_id": "myotherapp",
    "name": "My Other App",
    "allowed_domains": ["myapp.example.com"],
    "deep_link_scheme": "myapp://oauth/callback",
    "shared_secret": "another-secret-here"
  }
]
```

#### `DYNAMODB_TABLE`
DynamoDB table name for session storage (default: `oauth-sessions`)

#### `SESSION_TTL_MINUTES`
Session expiration time in minutes (default: `10`)

#### `OAUTH_PROVIDERS`
Comma-separated list of allowed OAuth providers (default: `openrouter,github,google`)

### Removed Variables

- ~~`DOMAIN_NAME`~~ - Callback URLs now use request hostname
- ~~`SHARED_SECRET`~~ - Secrets now per-application in APPLICATIONS
- ~~`DEEP_LINK_SCHEME`~~ - Deep link schemes now per-application

## DynamoDB Schema Changes

### Sessions Table

The `oauth-sessions` table now includes additional fields:

**New Fields:**
- `app_id` (String): Application identifier
- `deep_link_url` (String): Full deep link callback URL

**Updated Schema:**
```
session_id (String, Primary Key)
app_id (String)
provider (String)
deep_link_url (String)
created_at (Number)
expires_at (Number, TTL attribute)
used (Boolean)
client_ip (String)
user_agent (String)
```

**TTL Configuration:**
Ensure the `expires_at` attribute is configured as the TTL attribute for automatic cleanup.

## Deployment

### 1. Generate Secrets

```bash
# Generate a strong secret for each application
python -c "import secrets; print(secrets.token_urlsafe(32))"
```

### 2. Set Environment Variables

**Option A: AWS Console**
1. Navigate to Lambda function
2. Configuration → Environment variables
3. Add/update variables

**Option B: AWS CLI**
```bash
aws lambda update-function-configuration \
  --function-name oauth-register \
  --environment "Variables={
    APPLICATIONS='[{\"app_id\":\"pevplugshare\",\"name\":\"PEV PlugShare\",\"allowed_domains\":[\"pevplugshare.a42r.com\"],\"deep_link_scheme\":\"pevplugshare://oauth/callback\",\"shared_secret\":\"YOUR_SECRET_HERE\"}]',
    DYNAMODB_TABLE='oauth-sessions',
    SESSION_TTL_MINUTES='10',
    OAUTH_PROVIDERS='openrouter,github,google'
  }"
```

**Option C: Terraform**
```hcl
resource "aws_lambda_function" "oauth_register" {
  # ... other configuration ...

  environment {
    variables = {
      APPLICATIONS = jsonencode([
        {
          app_id           = "pevplugshare"
          name             = "PEV PlugShare"
          allowed_domains  = ["pevplugshare.a42r.com", "pevplugshare-prod.a42r.com"]
          deep_link_scheme = "pevplugshare://oauth/callback"
          shared_secret    = var.oauth_secret  # Use variables for secrets!
        }
      ])
      DYNAMODB_TABLE       = aws_dynamodb_table.oauth_sessions.name
      SESSION_TTL_MINUTES  = "10"
      OAUTH_PROVIDERS      = "openrouter,github,google"
    }
  }
}
```

### 3. Deploy Lambda Functions

```bash
# Package dependencies
cd oauth-callback-lambda
pip install -r requirements.txt -t package/
cd package && zip -r ../deployment.zip . && cd ..
zip -r deployment.zip src/

# Deploy register function
aws lambda update-function-code \
  --function-name oauth-register \
  --zip-file fileb://deployment.zip

# Deploy callback function
aws lambda update-function-code \
  --function-name oauth-callback \
  --zip-file fileb://deployment.zip
```

### 4. Update API Gateway (if needed)

Ensure your API Gateway is configured to:
- Pass the `Host` header to Lambda
- Support path parameters for `/oauth/callback/{session_id}`

## Testing

### 1. Generate Test Signature

```python
import hmac
import hashlib
import time

provider = "openrouter"
deep_link_url = "pevplugshare://oauth/callback"
timestamp = int(time.time())
secret = "your-secret-here"

message = f"{provider}|{deep_link_url}|{timestamp}"
signature = hmac.new(
    secret.encode(),
    message.encode(),
    hashlib.sha256
).hexdigest()

print(f"Timestamp: {timestamp}")
print(f"Signature: {signature}")
```

### 2. Test Register Endpoint

```bash
curl -X POST https://your-api-gateway-url/oauth/register \
  -H "Content-Type: application/json" \
  -H "Host: pevplugshare.a42r.com" \
  -d '{
    "provider": "openrouter",
    "deep_link_url": "pevplugshare://oauth/callback",
    "timestamp": <timestamp>,
    "signature": "<signature>"
  }'
```

### 3. Verify Response

Expected success response:
```json
{
  "session_id": "550e8400-e29b-41d4-a716-446655440000",
  "callback_url": "https://pevplugshare.a42r.com/oauth/callback/550e8400-e29b-41d4-a716-446655440000",
  "expires_in": 600,
  "provider": "openrouter"
}
```

## Security Best Practices

### 1. Use AWS Secrets Manager

Instead of storing secrets in environment variables:

```python
import boto3
import json

def get_applications_from_secrets():
    """Load application config from Secrets Manager"""
    secrets_client = boto3.client('secretsmanager')

    response = secrets_client.get_secret_value(
        SecretId='oauth-applications'
    )

    return json.loads(response['SecretString'])
```

Update `applications.py`:
```python
import os
import json
from typing import Optional, Dict

# Try Secrets Manager first, fall back to env var
try:
    apps_config = get_applications_from_secrets()
except:
    apps_config = json.loads(os.environ.get('APPLICATIONS', '[]'))
```

### 2. Enable CloudWatch Logging

Log all signature verification failures:

```python
import logging
logger = logging.getLogger()
logger.setLevel(logging.INFO)

# In verify_signature
if not is_valid:
    logger.warning(f"Signature verification failed for {provider} from {request_host}")
```

### 3. Set Up Alarms

Create CloudWatch alarms for:
- High rate of signature failures
- Unknown application requests
- Session table errors

### 4. Rotate Secrets Regularly

```bash
# Generate new secret
NEW_SECRET=$(python -c "import secrets; print(secrets.token_urlsafe(32))")

# Update environment variable
aws lambda update-function-configuration \
  --function-name oauth-register \
  --environment "Variables={APPLICATIONS='[...]'}"  # Update with new secret

# Update mobile app's secrets.properties
# Rebuild and deploy mobile app
```

## Troubleshooting

### "unknown_application" Error

**Cause:** Request hostname doesn't match any `allowed_domains`

**Fix:**
1. Check the `Host` header in the request
2. Verify domain is in `allowed_domains` for the application
3. Ensure API Gateway is passing the Host header

### "invalid_signature" Error

**Cause:** HMAC signature doesn't match

**Fix:**
1. Verify message format: `{provider}|{deep_link_url}|{timestamp}`
2. Check secret matches between mobile app and Lambda config
3. Ensure timestamp is fresh (within 5 minutes)
4. Verify all parameters are lowercase and exact

### "invalid_deep_link" Error

**Cause:** Deep link URL doesn't match configured scheme

**Fix:**
1. Check `deep_link_url` starts with `deep_link_scheme`
2. Verify scheme is exact (case-sensitive)
3. Example: `pevplugshare://oauth/callback` must start with `pevplugshare://oauth/callback`

### CloudWatch Logs

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

## Migration from Single Application

### Before (Single App)
```bash
DOMAIN_NAME=https://pevplugshare.a42r.com
SHARED_SECRET=my-secret
DEEP_LINK_SCHEME=pevplugshare://oauth/callback
```

### After (Multi-App)
```bash
APPLICATIONS='[{
  "app_id": "pevplugshare",
  "name": "PEV PlugShare",
  "allowed_domains": ["pevplugshare.a42r.com"],
  "deep_link_scheme": "pevplugshare://oauth/callback",
  "shared_secret": "my-secret"
}]'
```

### DynamoDB Migration

No migration needed for existing sessions - they will expire naturally. New sessions will include `app_id` and `deep_link_url` fields.

## Performance Considerations

### Cold Start Optimization

Applications are loaded once per Lambda container:

```python
# In applications.py
_APPLICATIONS = load_applications()  # Cached at module level

def get_application(domain: str) -> Optional[Application]:
    return _APPLICATIONS.get(domain)  # Fast dictionary lookup
```

### API Gateway Caching

Consider enabling API Gateway caching for the register endpoint with:
- Cache key: `Host` header + request body
- TTL: Short (e.g., 60 seconds)

### DynamoDB Capacity

For high-volume applications:
- Use on-demand billing mode, or
- Monitor consumed capacity and adjust provisioned throughput
- Enable auto-scaling for provisioned capacity

## Cost Estimates

Assuming 10,000 OAuth flows per month:

- **Lambda Invocations**: ~20,000 (register + callback) = $0.004
- **Lambda Compute**: Negligible (< 1 GB-second)
- **DynamoDB**:
  - Writes: 10,000 = $0.01
  - Reads: 10,000 = $0.002
  - Storage: Minimal (TTL cleanup)
- **API Gateway**: 20,000 requests = $0.02

**Total: ~$0.04/month** (excluding data transfer)

## Next Steps

1. ✅ Updated Lambda handlers for multi-app support
2. ⏳ Update infrastructure code (CloudFormation/Terraform)
3. ⏳ Migrate secrets to AWS Secrets Manager
4. ⏳ Add CloudWatch dashboards and alarms
5. ⏳ Create deployment pipeline
6. ⏳ Add integration tests
