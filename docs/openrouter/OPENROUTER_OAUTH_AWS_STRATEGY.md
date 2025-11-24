# OpenRouter OAuth AWS Lambda Strategy

## Problem Statement
OpenRouter's OAuth flow requires a publicly accessible callback URL. The current localhost approach (`http://localhost:48621/oauth/callback`) fails with "failed to create or update app" error because OpenRouter cannot reach localhost for validation.

## Solution Architecture

### High-Level Flow
```
Android App → OpenRouter Auth → API Gateway → Lambda → DynamoDB → Deep Link → Android App
```

### Components

#### 1. API Gateway REST API
**Base URL**: `https://api.pevplugshare.com` (or similar)

**Endpoints**:
- `POST /oauth/register`
  - Registers a new OAuth session
  - Returns temporary callback URL
  - No authentication required (rate-limited)

- `GET /oauth/callback/{session_id}`
  - Handles OpenRouter redirect
  - Validates session and extracts auth code
  - Redirects to Android deep link

#### 2. Lambda Functions

**Function 1: RegisterDevice**
- **Runtime**: Python 3.12 or Node.js 20
- **Memory**: 128MB
- **Timeout**: 10 seconds
- **Trigger**: API Gateway POST /oauth/register

**Logic**:
```python
import uuid
import time
import boto3
from datetime import datetime, timedelta

def handler(event, context):
    # Generate cryptographically secure session ID
    session_id = str(uuid.uuid4())

    # Calculate expiration (10 minutes)
    expires_at = int(time.time()) + 600

    # Store in DynamoDB
    dynamodb = boto3.resource('dynamodb')
    table = dynamodb.Table('oauth-sessions')
    table.put_item(Item={
        'session_id': session_id,
        'created_at': int(time.time()),
        'expires_at': expires_at,  # TTL attribute
        'used': False,
        'device_info': event.get('headers', {}).get('User-Agent', 'unknown')
    })

    # Return callback URL
    callback_url = f"https://api.pevplugshare.com/oauth/callback/{session_id}"

    return {
        'statusCode': 200,
        'headers': {
            'Content-Type': 'application/json',
            'Access-Control-Allow-Origin': '*'
        },
        'body': json.dumps({
            'session_id': session_id,
            'callback_url': callback_url,
            'expires_in': 600
        })
    }
```

**Function 2: HandleCallback**
- **Runtime**: Python 3.12 or Node.js 20
- **Memory**: 128MB
- **Timeout**: 10 seconds
- **Trigger**: API Gateway GET /oauth/callback/{session_id}

**Logic**:
```python
import boto3
import time
from urllib.parse import urlencode

def handler(event, context):
    session_id = event['pathParameters']['session_id']
    query_params = event.get('queryStringParameters', {})
    auth_code = query_params.get('code')

    if not auth_code:
        return {
            'statusCode': 400,
            'body': 'Missing authorization code'
        }

    # Validate session in DynamoDB
    dynamodb = boto3.resource('dynamodb')
    table = dynamodb.Table('oauth-sessions')

    try:
        response = table.get_item(Key={'session_id': session_id})
        item = response.get('Item')

        # Validate session
        if not item:
            return {'statusCode': 404, 'body': 'Session not found'}

        if item.get('used'):
            return {'statusCode': 400, 'body': 'Session already used'}

        if int(time.time()) > item['expires_at']:
            return {'statusCode': 400, 'body': 'Session expired'}

        # Mark session as used
        table.update_item(
            Key={'session_id': session_id},
            UpdateExpression='SET used = :val',
            ExpressionAttributeValues={':val': True}
        )

        # Build deep link to Android app
        deep_link = f"pevplugshare://oauth/callback?code={auth_code}"

        # Return HTML redirect page
        html = f"""
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <title>Authorization Complete</title>
            <meta http-equiv="refresh" content="0;url={deep_link}">
        </head>
        <body>
            <h1>Authorization successful!</h1>
            <p>Redirecting back to PEV PlugShare...</p>
            <p>If you are not redirected, <a href="{deep_link}">click here</a>.</p>
        </body>
        </html>
        """

        return {
            'statusCode': 200,
            'headers': {
                'Content-Type': 'text/html',
                'Cache-Control': 'no-store'
            },
            'body': html
        }

    except Exception as e:
        return {
            'statusCode': 500,
            'body': f'Internal error: {str(e)}'
        }
```

#### 3. DynamoDB Table

**Table Name**: `oauth-sessions`

**Schema**:
- **Partition Key**: `session_id` (String)
- **Attributes**:
  - `created_at` (Number) - Unix timestamp
  - `expires_at` (Number) - Unix timestamp, TTL attribute
  - `used` (Boolean) - Single-use flag
  - `device_info` (String) - User-Agent for debugging

**Settings**:
- Billing mode: On-demand (pay per request)
- TTL: Enabled on `expires_at` attribute
- Point-in-time recovery: Disabled (not needed for ephemeral data)

**Indexes**: None needed (simple key-value lookups)

#### 4. Rate Limiting Configuration

**API Gateway Usage Plan**:
```yaml
Throttle:
  RateLimit: 10      # requests per second per IP
  BurstLimit: 20     # burst capacity

Quota:
  Limit: 1000        # requests per day per API key
  Period: DAY

# Alternative: WAF rate limiting
WAF_RateLimit:
  Limit: 100         # requests per 5 minutes per IP
  Scope: REGIONAL
```

**CloudWatch Alarms**:
- Alert if requests > 500/hour (potential abuse)
- Alert if 4XX errors > 50/hour (possible attack)
- Alert if Lambda errors > 10/hour (code issue)

### Cost Analysis

**Monthly Estimates** (assuming 1,000 OAuth flows/month):

| Service | Usage | Cost |
|---------|-------|------|
| API Gateway | 2,000 requests | $0.007 |
| Lambda (RegisterDevice) | 1,000 invocations | $0.00 (free tier) |
| Lambda (HandleCallback) | 1,000 invocations | $0.00 (free tier) |
| DynamoDB | 2,000 read/write | $0.05 |
| CloudWatch Logs | 1 GB/month | $0.50 |
| Route53 (optional) | 1 hosted zone | $0.50 |
| **Total** | | **~$1.12/month** |

**Free Tier Coverage**:
- Lambda: 1M requests/month free
- API Gateway: 1M requests/month free (first 12 months)
- DynamoDB: 25 GB storage + 25 WCU/RCU free

**Actual cost for typical usage**: <$1/month after free tier expires

### Security Considerations

1. **HTTPS Only**: API Gateway enforces TLS 1.2+
2. **CORS Headers**: Restrict to openrouter.ai origin
3. **Session Validation**:
   - Cryptographically random UUIDs
   - 10-minute expiration
   - Single-use tokens
   - Automatic cleanup via TTL
4. **Rate Limiting**: Prevent DDoS and abuse
5. **No Sensitive Data**: Sessions contain no user info
6. **CloudWatch Logging**: Audit trail for debugging

### Android App Changes

#### Update Constants
```kotlin
private const val OAUTH_REGISTER_URL = "https://api.pevplugshare.com/oauth/register"
private const val DEEP_LINK_SCHEME = "pevplugshare://oauth/callback"
```

#### Modified OAuth Flow
```kotlin
private fun startOAuthFlow() {
    lifecycleScope.launch(Dispatchers.IO) {
        try {
            // Step 1: Register session with AWS
            val sessionResponse = registerOAuthSession()
            val callbackUrl = sessionResponse.getString("callback_url")

            // Step 2: Generate PKCE parameters
            val codeVerifier = PkceUtils.generateCodeVerifier()
            val codeChallenge = PkceUtils.generateCodeChallenge(codeVerifier)
            tokenManager.savePkceCodeVerifier(codeVerifier)

            // Step 3: Build OpenRouter auth URL
            val authUrl = Uri.parse(OPENROUTER_AUTH_URL).buildUpon()
                .appendQueryParameter("callback_url", callbackUrl)
                .appendQueryParameter("code_challenge", codeChallenge)
                .appendQueryParameter("code_challenge_method", "S256")
                .build()

            // Step 4: Launch browser
            withContext(Dispatchers.Main) {
                val customTabsIntent = CustomTabsIntent.Builder()
                    .setShowTitle(true)
                    .build()
                customTabsIntent.launchUrl(this@SettingsActivity, authUrl)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@SettingsActivity,
                    "Failed to start OAuth: ${e.message}",
                    Toast.LENGTH_LONG).show()
            }
        }
    }
}

private suspend fun registerOAuthSession(): JSONObject {
    val client = OkHttpClient()
    val request = Request.Builder()
        .url(OAUTH_REGISTER_URL)
        .post(RequestBody.create(null, ""))
        .build()

    val response = client.newCall(request).execute()
    if (!response.isSuccessful) {
        throw IOException("Session registration failed: ${response.code}")
    }

    return JSONObject(response.body?.string() ?: "{}")
}
```

#### Update AndroidManifest
```xml
<!-- Deep link for OAuth callback -->
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="pevplugshare"
          android:host="oauth"
          android:path="/callback" />
</intent-filter>
```

### Infrastructure as Code (Terraform)

```hcl
# DynamoDB Table
resource "aws_dynamodb_table" "oauth_sessions" {
  name           = "oauth-sessions"
  billing_mode   = "PAY_PER_REQUEST"
  hash_key       = "session_id"

  attribute {
    name = "session_id"
    type = "S"
  }

  ttl {
    attribute_name = "expires_at"
    enabled        = true
  }

  tags = {
    Application = "PEVPlugShare"
    Purpose     = "OAuth"
  }
}

# Lambda IAM Role
resource "aws_iam_role" "lambda_role" {
  name = "pevplugshare-oauth-lambda-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = {
        Service = "lambda.amazonaws.com"
      }
    }]
  })
}

# Lambda DynamoDB Policy
resource "aws_iam_role_policy" "lambda_dynamodb" {
  name = "dynamodb-access"
  role = aws_iam_role.lambda_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Action = [
        "dynamodb:PutItem",
        "dynamodb:GetItem",
        "dynamodb:UpdateItem"
      ]
      Resource = aws_dynamodb_table.oauth_sessions.arn
    }]
  })
}

# API Gateway
resource "aws_api_gateway_rest_api" "oauth_api" {
  name        = "pevplugshare-oauth"
  description = "OAuth callback handler for PEV PlugShare"
}

# Usage Plan with Rate Limiting
resource "aws_api_gateway_usage_plan" "oauth_plan" {
  name = "oauth-rate-limit"

  api_stages {
    api_id = aws_api_gateway_rest_api.oauth_api.id
    stage  = aws_api_gateway_stage.prod.stage_name
  }

  throttle_settings {
    rate_limit  = 10
    burst_limit = 20
  }

  quota_settings {
    limit  = 1000
    period = "DAY"
  }
}
```

### Deployment Checklist

- [ ] Create DynamoDB table with TTL enabled
- [ ] Deploy Lambda functions (RegisterDevice, HandleCallback)
- [ ] Configure IAM roles and policies
- [ ] Create API Gateway endpoints
- [ ] Set up rate limiting and usage plans
- [ ] Configure custom domain (optional)
- [ ] Update Android app with new endpoints
- [ ] Update AndroidManifest with deep link scheme
- [ ] Test complete OAuth flow end-to-end
- [ ] Set up CloudWatch alarms
- [ ] Document API endpoints for future reference

### Testing Strategy

1. **Unit Tests**: Test Lambda functions locally with SAM CLI
2. **Integration Tests**: Test API Gateway → Lambda → DynamoDB flow
3. **E2E Tests**: Complete Android app OAuth flow
4. **Load Tests**: Verify rate limiting works as expected
5. **Security Tests**: Attempt replay attacks, expired sessions

### Alternative Approaches Considered

1. **GitHub Pages with JavaScript redirect**: Free but unprofessional, no session validation
2. **Localhost callback**: Zero cost but OpenRouter rejects it
3. **Heroku free tier**: No longer available
4. **Cloudflare Workers**: Similar cost, less familiar ecosystem

### Future Enhancements

- [ ] Add device fingerprinting for abuse detection
- [ ] Implement webhook for real-time monitoring
- [ ] Add analytics dashboard for OAuth metrics
- [ ] Support multiple OAuth providers (not just OpenRouter)
- [ ] Implement OAuth token refresh logic
