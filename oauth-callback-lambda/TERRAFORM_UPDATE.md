# Terraform Configuration Update - Multi-Application Support

## What Changed

The Terraform infrastructure has been updated to support **multiple applications with multiple domains and deep_link_schemes**, matching the capabilities of the Docker version.

### Before (Single Application)
```hcl
domain_name = "https://your-api-gateway-url.execute-api.us-east-1.amazonaws.com/prod"
shared_secret = "my-secret"
deep_link_scheme = "pevplugshare://oauth/callback"
```

### After (Multiple Applications)
```hcl
applications = [
  {
    app_id           = "pevplugshare"
    name             = "PEV PlugShare"
    allowed_domains  = ["pevplugshare.a42r.com", "localhost:8000"]
    deep_link_scheme = "pevplugshare://oauth/callback"
    shared_secret    = "secret-1"
  },
  {
    app_id           = "another-app"
    name             = "Another App"
    allowed_domains  = ["app.example.com", "www.app.example.com"]
    deep_link_scheme = "anotherapp://oauth/callback"
    shared_secret    = "secret-2"
  }
]
```

## Changes Made

### 1. Updated Variable Definitions
- **Removed**: `domain_name`, `shared_secret`, `deep_link_scheme` (single values)
- **Added**: `applications` (list of application objects)
- **Added**: Validation to ensure applications are properly configured

### 2. Environment Variables
Lambda functions now receive:
- `APPLICATIONS`: JSON array of application configurations
- `DYNAMODB_TABLE`: DynamoDB table name
- `SESSION_TTL_MINUTES`: Session expiration time
- `OAUTH_PROVIDERS`: Allowed OAuth providers

### 3. Updated Configuration File
`terraform.tfvars.example` now shows:
- Multiple application configurations
- Comments showing how to add additional apps
- All per-application settings in one place

## How to Configure

### Step 1: Create terraform.tfvars

```bash
cd infrastructure
cp terraform.tfvars.example terraform.tfvars
```

### Step 2: Edit terraform.tfvars

For single application:
```hcl
applications = [
  {
    app_id           = "pevplugshare"
    name             = "PEV PlugShare"
    allowed_domains  = ["pevplugshare.a42r.com"]
    deep_link_scheme = "pevplugshare://oauth/callback"
    shared_secret    = "your-generated-secret"
  }
]
```

For multiple applications:
```hcl
applications = [
  {
    app_id           = "app1"
    name             = "App One"
    allowed_domains  = ["app1.example.com", "www.app1.example.com"]
    deep_link_scheme = "app1://oauth/callback"
    shared_secret    = "secret-for-app1"
  },
  {
    app_id           = "app2"
    name             = "App Two"
    allowed_domains  = ["app2.example.com"]
    deep_link_scheme = "app2://oauth/callback"
    shared_secret    = "secret-for-app2"
  }
]
```

### Step 3: Deploy

```bash
cd ..
bash scripts/deploy.sh
```

## Application Object Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `app_id` | string | Yes | Unique application identifier |
| `name` | string | Yes | Human-readable application name |
| `allowed_domains` | list(string) | Yes | List of domains where OAuth requests originate |
| `deep_link_scheme` | string | Yes | Deep link scheme for OAuth callbacks (e.g., `app://oauth/callback`) |
| `shared_secret` | string | Yes | HMAC secret for signature verification (32+ characters) |

## Examples

### Example 1: Single Application
```hcl
applications = [
  {
    app_id           = "pevplugshare"
    name             = "PEV PlugShare"
    allowed_domains  = ["pevplugshare.a42r.com"]
    deep_link_scheme = "pevplugshare://oauth/callback"
    shared_secret    = "abc123XYZ789..."
  }
]
```

### Example 2: Development + Production
```hcl
applications = [
  {
    app_id           = "pevplugshare-dev"
    name             = "PEV PlugShare (Development)"
    allowed_domains  = ["localhost:8000", "dev.pevplugshare.a42r.com"]
    deep_link_scheme = "pevplugshare://oauth/callback"
    shared_secret    = "dev-secret-123"
  },
  {
    app_id           = "pevplugshare-prod"
    name             = "PEV PlugShare (Production)"
    allowed_domains  = ["pevplugshare.a42r.com", "www.pevplugshare.a42r.com"]
    deep_link_scheme = "pevplugshare://oauth/callback"
    shared_secret    = "prod-secret-456"
  }
]
```

### Example 3: Multiple Applications
```hcl
applications = [
  {
    app_id           = "pevplugshare"
    name             = "PEV PlugShare"
    allowed_domains  = ["pevplugshare.a42r.com"]
    deep_link_scheme = "pevplugshare://oauth/callback"
    shared_secret    = "secret-1"
  },
  {
    app_id           = "ev-finder"
    name             = "EV Finder"
    allowed_domains  = ["evfinder.a42r.com"]
    deep_link_scheme = "evfinder://oauth/callback"
    shared_secret    = "secret-2"
  },
  {
    app_id           = "chargepoint-mapper"
    name             = "ChargePoint Mapper"
    allowed_domains  = ["chargepoint-mapper.example.com"]
    deep_link_scheme = "chargepoint://oauth/callback"
    shared_secret    = "secret-3"
  }
]
```

## Security Best Practices

1. **Generate Strong Secrets**: Each application should have a unique secret
   ```bash
   python3 -c "import secrets; print(secrets.token_urlsafe(32))"
   ```

2. **Use AWS Secrets Manager**: For production, consider storing secrets there
   ```hcl
   # In Terraform
   applications = [
     {
       app_id           = "pevplugshare"
       name             = "PEV PlugShare"
       allowed_domains  = [...]
       deep_link_scheme = "..."
       shared_secret    = aws_secretsmanager_secret_version.oauth_secret.secret_string
     }
   ]
   ```

3. **Validate Domains**: Only add domains you control
   - Use HTTPS domains in production
   - Include all variations (with/without www, different subdomains)

4. **Rotate Secrets Regularly**
   - Regenerate secrets periodically
   - Update Android app with new secret
   - Coordinate with deployments

## Migration from Old Configuration

If you have an existing deployment with old single-app configuration:

### Old Format
```hcl
domain_name = "https://api-gateway.execute-api.us-east-1.amazonaws.com/prod"
shared_secret = "my-secret"
deep_link_scheme = "pevplugshare://oauth/callback"
```

### New Format
```hcl
applications = [
  {
    app_id           = "pevplugshare"
    name             = "PEV PlugShare"
    allowed_domains  = ["api-gateway.execute-api.us-east-1.amazonaws.com"]
    deep_link_scheme = "pevplugshare://oauth/callback"
    shared_secret    = "my-secret"
  }
]
```

Then redeploy:
```bash
cd infrastructure
terraform plan
terraform apply
```

## Troubleshooting

### Error: "At least one application must be configured"
- Ensure `applications` variable has at least one application object
- Check for syntax errors in the configuration

### Error: "All application fields are required"
- Verify all applications have: `app_id`, `name`, `allowed_domains`, `deep_link_scheme`, `shared_secret`
- Check for null or empty values

### Lambda not receiving configuration
- Verify `applications` is properly formatted JSON
- Check Lambda environment variables in AWS Console
- View CloudWatch logs for parsing errors

## Testing Multi-Application Setup

Test with the first application:
```bash
curl -X POST https://your-api.com/oauth/register \
  -H "Content-Type: application/json" \
  -H "Host: app1.example.com" \
  -d '{...}'
```

Test with the second application:
```bash
curl -X POST https://your-api.com/oauth/register \
  -H "Content-Type: application/json" \
  -H "Host: app2.example.com" \
  -d '{...}'
```

Each application will be validated against its own secret and deep link scheme.

## Backward Compatibility

- Old terraform.tfvars files will NOT work with the updated code
- You must regenerate terraform.tfvars from the new example
- This is a breaking change requiring manual configuration update

## Benefits

✅ Support multiple applications
✅ Multiple domains per application
✅ Different secrets per application
✅ Different deep link schemes
✅ Development + production in one deployment
✅ Easy to add/remove applications
✅ Matches Docker version capabilities

