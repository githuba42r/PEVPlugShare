"""
Lambda function: Register OAuth Session
"""
import json
import uuid
import time
import os
import boto3
from datetime import datetime, timedelta

# Import shared utilities
from shared.security import verify_signature
from shared.responses import success_response, error_response
from shared.applications import get_application


# Initialize DynamoDB
dynamodb = boto3.resource('dynamodb')
table_name = os.environ.get('DYNAMODB_TABLE', 'oauth-sessions')
table = dynamodb.Table(table_name)

# Configuration from environment
SESSION_TTL_MINUTES = int(os.environ.get('SESSION_TTL_MINUTES', '10'))
ALLOWED_PROVIDERS = os.environ.get('OAUTH_PROVIDERS', 'openrouter,github,google').split(',')


def lambda_handler(event, context):
    """
    Handle POST /oauth/register

    Registers a new OAuth session and returns callback URL
    """
    try:
        # Debug: Log environment
        import os
        print(f"DEBUG: APPLICATIONS env var: {os.environ.get('APPLICATIONS', 'NOT SET')}")
        
        # Parse request body
        body = json.loads(event.get('body', '{}'))
        provider = body.get('provider', '').lower()
        deep_link_url = body.get('deep_link_url', '')
        timestamp = body.get('timestamp')
        signature = body.get('signature')

        # Validate required fields
        if not all([provider, deep_link_url, timestamp, signature]):
            return error_response(
                400,
                'missing_fields',
                'Missing required fields: provider, deep_link_url, timestamp, signature'
            )

        # Extract request hostname (prefer X-Forwarded-Host for custom domains through proxies)
        headers = event.get('headers', {})
        request_host = (
            headers.get('X-Forwarded-Host') or 
            headers.get('x-forwarded-host') or
            headers.get('Host') or 
            headers.get('host') or 
            'default-app'
        )
        print(f"DEBUG: request_host={request_host}, headers={headers}")

        # Lookup application by domain
        application = get_application(request_host)
        print(f"DEBUG: get_application('{request_host}') returned: {application}")
        
        # For testing/development, allow unknown domains to use the first application
        if not application and request_host == 'default-app':
            # Use the first configured application as default
            from shared.applications import _APPLICATIONS
            if _APPLICATIONS:
                # Get the first application from the domain map
                application = next(iter(_APPLICATIONS.values())) if _APPLICATIONS else None
                print(f"DEBUG: Using default application: {application}")
        
        if not application:
            return error_response(
                400,
                'unknown_application',
                f"No application configured for domain: {request_host}",
                {'request_host': request_host}
            )

        # Validate deep link scheme
        if not deep_link_url.startswith(application.deep_link_scheme):
            return error_response(
                400,
                'invalid_deep_link',
                f"Deep link URL must start with: {application.deep_link_scheme}",
                {
                    'expected_scheme': application.deep_link_scheme,
                    'received_url': deep_link_url
                }
            )

        # Validate provider
        if provider not in ALLOWED_PROVIDERS:
            return error_response(
                400,
                'invalid_provider',
                f"Provider '{provider}' is not supported",
                {'allowed_providers': ALLOWED_PROVIDERS}
            )

        # Verify signature with application's secret
        is_valid, error_msg = verify_signature(
            provider,
            deep_link_url,
            timestamp,
            signature,
            application.shared_secret
        )
        if not is_valid:
            return error_response(
                400,
                'invalid_signature',
                error_msg,
                {'timestamp': timestamp}
            )

        # Generate session ID
        session_id = str(uuid.uuid4())

        # Calculate expiration
        current_time = int(time.time())
        expires_at = current_time + (SESSION_TTL_MINUTES * 60)

        # Get client info
        request_context = event.get('requestContext', {})
        identity = request_context.get('identity', {})
        client_ip = identity.get('sourceIp', 'unknown')
        user_agent = headers.get('User-Agent', 'unknown')

        # Store session in DynamoDB
        table.put_item(Item={
            'session_id': session_id,
            'app_id': application.app_id,
            'provider': provider,
            'deep_link_url': deep_link_url,
            'created_at': current_time,
            'expires_at': expires_at,  # DynamoDB TTL attribute
            'used': False,
            'client_ip': client_ip,
            'user_agent': user_agent
        })

        # Build callback URL using the same domain the request came from
        # Include the stage name (prod) in the callback URL
        callback_url = f"https://{request_host}/prod/oauth/callback/{session_id}"

        # Return response
        return success_response(200, {
            'session_id': session_id,
            'callback_url': callback_url,
            'expires_in': SESSION_TTL_MINUTES * 60,
            'provider': provider
        })

    except json.JSONDecodeError as e:
        print(f"JSON decode error: {str(e)}")
        return error_response(
            400,
            'invalid_json',
            'Request body must be valid JSON'
        )

    except Exception as e:
        import traceback
        print(f"Error in register function: {str(e)}")
        print(f"Traceback: {traceback.format_exc()}")
        return error_response(
            500,
            'internal_error',
            'An unexpected error occurred'
        )
