"""
Shared utilities for OAuth Lambda functions
"""
import hmac
import hashlib
import time
import os
from typing import Tuple, Optional


def generate_signature(provider: str, deep_link_url: str, timestamp: int, secret: str) -> str:
    """
    Generate HMAC-SHA256 signature for request validation

    Args:
        provider: OAuth provider name
        deep_link_url: Deep link callback URL
        timestamp: Unix timestamp
        secret: Shared secret for the application

    Returns:
        Hex-encoded HMAC signature
    """
    message = f"{provider.lower()}|{deep_link_url}|{timestamp}"
    signature = hmac.new(
        secret.encode('utf-8'),
        message.encode('utf-8'),
        hashlib.sha256
    ).hexdigest()

    return signature


def verify_signature(
    provider: str,
    deep_link_url: str,
    timestamp: int,
    signature: str,
    secret: str,
    max_age_seconds: int = 300
) -> Tuple[bool, Optional[str]]:
    """
    Verify HMAC signature and timestamp freshness

    Args:
        provider: OAuth provider name
        deep_link_url: Deep link callback URL
        timestamp: Unix timestamp from request
        signature: HMAC signature to verify
        secret: Shared secret for the application
        max_age_seconds: Maximum age of timestamp (default 5 minutes)

    Returns:
        Tuple of (is_valid, error_message)
    """
    # Check timestamp freshness
    current_time = int(time.time())
    timestamp_int = int(timestamp) if isinstance(timestamp, str) else timestamp
    time_diff = abs(current_time - timestamp_int)

    if time_diff > max_age_seconds:
        return False, f"Timestamp too old or in future (diff: {time_diff}s)"

    # Generate expected signature
    expected_signature = generate_signature(provider, deep_link_url, timestamp, secret)

    # Constant-time comparison
    is_valid = hmac.compare_digest(signature, expected_signature)

    if not is_valid:
        return False, "Signature verification failed"

    return True, None


def get_current_timestamp() -> int:
    """Get current Unix timestamp"""
    return int(time.time())
