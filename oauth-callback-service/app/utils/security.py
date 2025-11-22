"""
HMAC signature utilities for request verification
"""
import hmac
import hashlib
import time
from typing import Optional
from app.config import settings


def generate_signature(provider: str, deep_link_url: str, timestamp: int, secret: Optional[str] = None) -> str:
    """
    Generate HMAC-SHA256 signature for a request

    Args:
        provider: OAuth provider name
        deep_link_url: Deep link URL for callback
        timestamp: Unix timestamp
        secret: Shared secret (defaults to settings.shared_secret)

    Returns:
        Hex-encoded HMAC signature
    """
    if secret is None:
        secret = settings.shared_secret

    # Create message to sign: provider|deep_link_url|timestamp
    message = f"{provider.lower()}|{deep_link_url}|{timestamp}"

    # Generate HMAC-SHA256
    signature = hmac.new(
        secret.encode('utf-8'),
        message.encode('utf-8'),
        hashlib.sha256
    ).hexdigest()

    return signature


def verify_signature(provider: str, deep_link_url: str, timestamp: int, signature: str, max_age_seconds: int = 300) -> tuple[bool, Optional[str]]:
    """
    Verify HMAC signature and timestamp freshness

    Args:
        provider: OAuth provider name
        deep_link_url: Deep link URL for callback
        timestamp: Unix timestamp from request
        signature: HMAC signature to verify
        max_age_seconds: Maximum age of timestamp (default 5 minutes)

    Returns:
        Tuple of (is_valid, error_message)
    """
    # Check timestamp freshness
    current_time = int(time.time())
    time_diff = abs(current_time - timestamp)

    if time_diff > max_age_seconds:
        return False, f"Timestamp too old or in future (diff: {time_diff}s, max: {max_age_seconds}s)"

    # Generate expected signature
    expected_signature = generate_signature(provider, deep_link_url, timestamp)

    # Constant-time comparison to prevent timing attacks
    is_valid = hmac.compare_digest(signature, expected_signature)

    if not is_valid:
        return False, "Signature verification failed"

    return True, None


def get_current_timestamp() -> int:
    """Get current Unix timestamp"""
    return int(time.time())
