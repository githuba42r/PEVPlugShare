"""
Lambda function: Handle OAuth Callback
"""
import json
import os
import boto3
import time
from urllib.parse import urlencode

# Import shared utilities
from shared.responses import html_response
from shared.templates import create_redirect_page, create_error_page


# Initialize DynamoDB
dynamodb = boto3.resource('dynamodb')
table_name = os.environ.get('DYNAMODB_TABLE', 'oauth-sessions')
table = dynamodb.Table(table_name)


def lambda_handler(event, context):
    """
    Handle GET /oauth/callback/{session_id}

    Receives OAuth provider callback and redirects to mobile app deep link
    """
    try:
        # Extract session ID from path
        path_parameters = event.get('pathParameters', {})
        session_id = path_parameters.get('session_id')

        if not session_id:
            return html_response(
                400,
                create_error_page(
                    "Invalid Request",
                    "Missing session ID in callback URL.",
                    "Please start the OAuth flow again from the app."
                )
            )

        # Extract query parameters
        query_params = event.get('queryStringParameters') or {}
        auth_code = query_params.get('code')
        error = query_params.get('error')
        error_description = query_params.get('error_description')

        # Lookup session in DynamoDB
        try:
            response = table.get_item(Key={'session_id': session_id})
            item = response.get('Item')
        except Exception as e:
            print(f"DynamoDB error: {str(e)}")
            return html_response(
                500,
                create_error_page(
                    "Database Error",
                    "Unable to retrieve session information.",
                    "Please try again or contact support if the issue persists."
                )
            )

        # Validate session exists
        if not item:
            return html_response(
                404,
                create_error_page(
                    "Session Not Found",
                    "The OAuth session has expired or does not exist.",
                    "Please try connecting your account again from the app."
                )
            )

        # Check if session already used
        if item.get('used', False):
            return html_response(
                400,
                create_error_page(
                    "Session Already Used",
                    "This OAuth session has already been completed.",
                    "Please start a new connection from the app."
                )
            )

        # Check if session expired
        current_time = int(time.time())
        expires_at = item.get('expires_at', 0)

        if current_time > expires_at:
            return html_response(
                400,
                create_error_page(
                    "Session Expired",
                    f"This OAuth session expired. Sessions are valid for 10 minutes.",
                    "Please try again from the app."
                )
            )

        # Mark session as used
        try:
            table.update_item(
                Key={'session_id': session_id},
                UpdateExpression='SET used = :val',
                ExpressionAttributeValues={':val': True}
            )
        except Exception as e:
            print(f"Error marking session as used: {str(e)}")
            # Continue anyway - better to allow duplicate use than fail

        # Get deep link URL from session
        deep_link_url = item.get('deep_link_url', 'pevplugshare://oauth/callback')

        # Handle OAuth error from provider
        if error:
            deep_link = f"{deep_link_url}?error={error}"
            if error_description:
                deep_link += f"&error_description={error_description}"

            return html_response(
                200,
                create_redirect_page(
                    "Authorization Failed",
                    f"OAuth authorization failed: {error}",
                    deep_link,
                    is_error=True
                )
            )

        # Handle missing authorization code
        if not auth_code:
            return html_response(
                400,
                create_error_page(
                    "Missing Authorization Code",
                    "No authorization code was provided by the OAuth provider.",
                    "This may indicate a problem with the OAuth flow."
                )
            )

        # Build deep link with authorization code
        provider = item.get('provider', 'unknown')
        deep_link = f"{deep_link_url}?code={auth_code}&provider={provider}"

        # Return success redirect page
        return html_response(
            200,
            create_redirect_page(
                "Authorization Successful",
                f"You've successfully authorized with {provider.title()}!",
                deep_link,
                is_error=False
            )
        )

    except Exception as e:
        print(f"Error in callback function: {str(e)}")
        return html_response(
            500,
            create_error_page(
                "Internal Error",
                "An unexpected error occurred while processing your request.",
                "Please try again or contact support if the issue persists."
            )
        )
