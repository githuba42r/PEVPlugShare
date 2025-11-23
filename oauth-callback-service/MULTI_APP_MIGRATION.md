# OAuth Callback Service - Multi-Application Support Update

## Summary of Changes

The OAuth callback service has been updated to support multiple applications through a database-driven configuration system. This removes the need for environment variables containing secrets and domains.

## What Changed

### FastAPI Service (oauth-callback-service/)

#### 1. Database Schema
**NEW: `applications` table**
- `app_id` (PK): Unique application identifier
- `name`: Human-readable application name
- `allowed_domains`: Comma-separated list of domains (e.g., "example.com,example.org,localhost:8000")
- `deep_link_scheme`: Deep link URL scheme (e.g., "myapp://oauth/callback")
- `shared_secret`: HMAC secret for request signing
- `created_at`, `updated_at`: Timestamps
- `is_active`: Boolean flag

**UPDATED: `oauth_sessions` table**
- Added `app_id` column to link sessions to applications

#### 2. Environment Variables
**REMOVED:**
- `DOMAIN_NAME` - Callback URLs now use request hostname
- `SHARED_SECRET` - Secrets now stored per-application in database

**KEPT:**
- `DATABASE_URL`
- `SESSION_TTL_MINUTES`
- `CLEANUP_INTERVAL_MINUTES`
- `RATE_LIMIT_*`
- `ALLOWED_ORIGINS`
- `OAUTH_PROVIDERS`
- `LOG_LEVEL`
- `ENABLE_DIAGNOSTIC_ENDPOINTS`

#### 3. Registration Endpoint (`/oauth/register`)
**New Validation Flow:**
1. Extract request hostname from HTTP request
2. Query `applications` table for matching domain
3. Validate deep link scheme matches application's configured scheme
4. Verify HMAC signature using application's shared secret
5. Create session linked to application
6. Return callback URL using request hostname

**Error Responses:**
- `unknown_application`: No application configured for request domain
- `invalid_deep_link`: Deep link doesn't match registered scheme
- `invalid_signature`: HMAC signature verification failed

#### 4. Sample Data Seeding
On first startup, a sample application is automatically created:
```
App ID: pevplugshare
Allowed Domains: pevplugshare.a42r.com,pevplugshare-prod.a42r.com,localhost:8000
Deep Link: pevplugshare://oauth/callback
Secret: CHANGE_THIS_SECRET (MUST be changed!)
```

### AWS Lambda Version (oauth-callback-lambda/)

**⚠️ NOT YET UPDATED**

The Lambda version requires additional changes to work with the new multi-application system:

#### Required Changes:

1. **DynamoDB Schema Updates**
   - Create new `applications` table
   - Add GSI on `allowed_domains` for efficient lookup
   - Add `app_id` to `oauth_sessions` table
   - Update CloudFormation/Terraform templates

2. **Application Lookup**
   - Extract request hostname from API Gateway event
   - Query DynamoDB applications table
   - Cache application configs (optional, for performance)

3. **Signature Verification**
   - Update `security.py` to accept deep_link_url parameter
   - Pass application's secret to verification function
   - Update message format: `{provider}|{deep_link_url}|{timestamp}`

4. **Environment Variables**
   - Remove `SHARED_SECRET`
   - Remove `DOMAIN_NAME`
   - Add `APPLICATIONS_TABLE_NAME`

5. **IAM Permissions**
   - Grant Lambda read access to applications table
   - Update IAM policies in infrastructure code

#### Lambda-Specific Considerations:

- **Cold Start**: Consider caching application configs in global scope
- **DynamoDB Costs**: Evaluate using on-demand vs provisioned capacity
- **Multi-Region**: If using multiple regions, decide on table replication strategy
- **Secrets Management**: Consider AWS Secrets Manager for production secrets

## Migration Guide

### For Existing Deployments

#### FastAPI Service

1. **Backup Database**
   ```bash
   cp oauth_sessions.db oauth_sessions.db.backup
   ```

2. **Update Code**
   ```bash
   git pull
   # Code now includes application table migration
   ```

3. **Update Environment**
   - Remove `DOMAIN_NAME` from `.env`
   - Remove `SHARED_SECRET` from `.env`
   - Keep all other variables

4. **Restart Service**
   ```bash
   docker-compose down
   docker-compose up -d
   ```
   - Database migration runs automatically
   - Sample application created on first start

5. **Configure Your Application**
   ```bash
   sqlite3 oauth_sessions.db
   ```
   ```sql
   -- Update sample app with your actual secret
   UPDATE applications
   SET shared_secret = 'your-strong-secret-here'
   WHERE app_id = 'pevplugshare';

   -- Or add a new application
   INSERT INTO applications (
     app_id, name, allowed_domains,
     deep_link_scheme, shared_secret, is_active
   ) VALUES (
     'myapp',
     'My Application',
     'myapp.example.com,myapp-dev.example.com',
     'myapp://oauth/callback',
     'generate-strong-secret-here',
     1
   );
   ```

6. **Update Mobile App Secrets**
   - Update `secrets.properties` with new shared secret
   - Rebuild mobile app
   - Test OAuth flow

#### AWS Lambda (When Implemented)

1. **Create DynamoDB Table**
   ```bash
   aws dynamodb create-table \
     --table-name oauth-applications \
     --attribute-definitions \
       AttributeName=app_id,AttributeType=S \
     --key-schema AttributeName=app_id,KeyType=HASH \
     --billing-mode PAY_PER_REQUEST
   ```

2. **Seed Application Data**
   ```bash
   aws dynamodb put-item \
     --table-name oauth-applications \
     --item '{
       "app_id": {"S": "pevplugshare"},
       "name": {"S": "PEV PlugShare"},
       "allowed_domains": {"S": "pevplugshare.a42r.com,pevplugshare-prod.a42r.com"},
       "deep_link_scheme": {"S": "pevplugshare://oauth/callback"},
       "shared_secret": {"S": "your-secret-here"},
       "is_active": {"BOOL": true}
     }'
   ```

3. **Update Lambda Code** (pending implementation)

4. **Deploy**
   ```bash
   ./scripts/deploy.sh
   ```

## Adding New Applications

### Via SQLite (FastAPI)

```sql
INSERT INTO applications (
  app_id,
  name,
  allowed_domains,
  deep_link_scheme,
  shared_secret,
  is_active,
  created_at,
  updated_at
) VALUES (
  'app-unique-id',
  'Application Display Name',
  'domain1.com,domain2.com,localhost:8000',
  'yourapp://oauth/callback',
  'generate-strong-secret: python -c "import secrets; print(secrets.token_urlsafe(32))"',
  1,
  datetime('now'),
  datetime('now')
);
```

### Via DynamoDB (Lambda - Future)

```bash
aws dynamodb put-item \
  --table-name oauth-applications \
  --item file://application.json
```

```json
{
  "app_id": {"S": "app-unique-id"},
  "name": {"S": "Application Display Name"},
  "allowed_domains": {"S": "domain1.com,domain2.com"},
  "deep_link_scheme": {"S": "yourapp://oauth/callback"},
  "shared_secret": {"S": "strong-random-secret"},
  "is_active": {"BOOL": true}
}
```

## Security Best Practices

1. **Strong Secrets**: Generate cryptographically secure secrets
   ```bash
   python -c "import secrets; print(secrets.token_urlsafe(32))"
   ```

2. **Domain Validation**: Only add trusted domains to `allowed_domains`

3. **HTTPS Only**: Ensure all domains use HTTPS in production

4. **Secret Rotation**: Periodically rotate shared secrets
   ```sql
   UPDATE applications
   SET shared_secret = 'new-secret', updated_at = datetime('now')
   WHERE app_id = 'your-app';
   ```

5. **Monitoring**: Log failed signature verifications

6. **Inactive Apps**: Disable unused applications
   ```sql
   UPDATE applications SET is_active = 0 WHERE app_id = 'old-app';
   ```

## Testing

### Test Multi-Application Setup

1. **Create Test Application**
   ```sql
   INSERT INTO applications VALUES (
     'test-app', 'Test App', 'localhost:8000',
     'testapp://callback', 'test-secret-123',
     datetime('now'), datetime('now'), 1
   );
   ```

2. **Generate Test Signature**
   ```python
   import hmac, hashlib, time

   provider = "openrouter"
   deep_link = "testapp://callback"
   timestamp = int(time.time())
   secret = "test-secret-123"

   message = f"{provider}|{deep_link}|{timestamp}"
   signature = hmac.new(
       secret.encode(),
       message.encode(),
       hashlib.sha256
   ).hexdigest()

   print(f"Timestamp: {timestamp}")
   print(f"Signature: {signature}")
   ```

3. **Test Request**
   ```bash
   curl -X POST http://localhost:8000/oauth/register \
     -H "Content-Type: application/json" \
     -H "Host: localhost:8000" \
     -d '{
       "provider": "openrouter",
       "deep_link_url": "testapp://callback",
       "timestamp": <timestamp>,
       "signature": "<signature>"
     }'
   ```

## Troubleshooting

### "unknown_application" Error
- Check request hostname matches `allowed_domains`
- Verify application exists and is active
- Check domain includes port if not 80/443

### "invalid_signature" Error
- Verify shared secret matches between app and database
- Check message format: `{provider}|{deep_link_url}|{timestamp}`
- Ensure timestamp is fresh (within 5 minutes)

### "invalid_deep_link" Error
- Deep link URL must start with configured scheme
- Check for typos in scheme
- Scheme is case-sensitive

## API Changes

### Request Format (Unchanged)
```json
{
  "provider": "openrouter",
  "deep_link_url": "myapp://oauth/callback",
  "timestamp": 1700000000,
  "signature": "abc123..."
}
```

### Response Format (Changed)
**Before:**
```json
{
  "callback_url": "https://configured-domain.com/oauth/callback/{id}"
}
```

**After:**
```json
{
  "callback_url": "https://request-hostname/oauth/callback/{id}"
}
```

The callback URL now uses the request's hostname instead of a configured domain. This allows multiple domains per application.

## Next Steps

1. ✅ FastAPI service updated
2. ⏳ Update AWS Lambda handler
3. ⏳ Create DynamoDB migration scripts
4. ⏳ Update infrastructure as code (CloudFormation/Terraform)
5. ⏳ Add management API for applications (create, update, delete)
6. ⏳ Add admin dashboard for managing applications
7. ⏳ Implement application usage analytics

## Questions?

- Check logs: `docker logs oauth-callback-service`
- Database: `sqlite3 oauth_sessions.db "SELECT * FROM applications;"`
- Verify table schema: `sqlite3 oauth_sessions.db ".schema applications"`
