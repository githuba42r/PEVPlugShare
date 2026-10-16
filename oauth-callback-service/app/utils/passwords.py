"""
Password hashing and opaque-token helpers.

Argon2id for passwords (memory-hard, current OWASP recommendation); SHA-256
for high-entropy opaque tokens (web sessions, API tokens, client secrets) —
fast hashing is correct there because the inputs are 256-bit random values,
not human passwords.
"""
from __future__ import annotations

import hashlib
import secrets

from argon2 import PasswordHasher
from argon2.exceptions import VerifyMismatchError, VerificationError, InvalidHashError

# Tuned interactive-login params (roughly 50-100ms on modern hardware)
_hasher = PasswordHasher(
    time_cost=2,
    memory_cost=64 * 1024,
    parallelism=2,
)


def hash_password(password: str) -> str:
    return _hasher.hash(password)


def verify_password(password_hash: str, password: str) -> bool:
    try:
        return _hasher.verify(password_hash, password)
    except (VerifyMismatchError, VerificationError, InvalidHashError):
        return False


def needs_rehash(password_hash: str) -> bool:
    try:
        return _hasher.check_needs_rehash(password_hash)
    except InvalidHashError:
        return True


def new_token(nbytes: int = 32) -> str:
    """URL-safe opaque token (returned to the caller exactly once)."""
    return secrets.token_urlsafe(nbytes)


def hash_token(token: str) -> str:
    return hashlib.sha256(token.encode("utf-8")).hexdigest()


def token_matches(stored_hash: str, presented_token: str) -> bool:
    """Constant-time compare of a presented token against a stored hash."""
    import hmac

    return hmac.compare_digest(stored_hash, hash_token(presented_token))
