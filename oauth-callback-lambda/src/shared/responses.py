"""
Shared response helpers for Lambda functions
"""
import json
from typing import Dict, Any, Optional


def success_response(
    status_code: int,
    body: Dict[str, Any],
    headers: Optional[Dict[str, str]] = None
) -> Dict[str, Any]:
    """Create successful API Gateway response"""
    default_headers = {
        'Content-Type': 'application/json',
        'Access-Control-Allow-Origin': '*',
        'Access-Control-Allow-Headers': 'Content-Type',
        'Access-Control-Allow-Methods': 'GET,POST,OPTIONS'
    }

    if headers:
        default_headers.update(headers)

    return {
        'statusCode': status_code,
        'headers': default_headers,
        'body': json.dumps(body)
    }


def error_response(
    status_code: int,
    error_type: str,
    message: str,
    details: Optional[Dict[str, Any]] = None
) -> Dict[str, Any]:
    """Create error API Gateway response"""
    body = {
        'error': error_type,
        'message': message
    }

    if details:
        body['details'] = details

    return success_response(status_code, body)


def html_response(
    status_code: int,
    html_content: str
) -> Dict[str, Any]:
    """Create HTML API Gateway response"""
    return {
        'statusCode': status_code,
        'headers': {
            'Content-Type': 'text/html; charset=utf-8',
            'Cache-Control': 'no-store'
        },
        'body': html_content
    }
