"""
Bearer-token authentication for the management API.

Opaque tokens only: presented token -> sha256 -> row lookup. A token is valid
when it exists, is not revoked, has not expired, and its client is still
active. Scopes are checked as a subset of the token's granted scopes.
"""
from __future__ import annotations

from dataclasses import dataclass, field
from typing import List, Optional

from fastapi import Depends, HTTPException, Request
from sqlalchemy.orm import Session

from app.db.repositories import Repositories
from app.db.session import get_db

ALL_SCOPES = [
    "apps:read",
    "apps:write",
    "clients:manage",
    "users:manage",
    "sessions:read",
    "sessions:write",
    "audit:read",
]


@dataclass
class AuthContext:
    client_id: str
    scopes: List[str] = field(default_factory=list)
    token_hash: str = ""

    def has_scope(self, scope: str) -> bool:
        return scope in self.scopes


def _extract_bearer(request: Request) -> Optional[str]:
    auth = request.headers.get("Authorization", "")
    if auth.lower().startswith("bearer "):
        return auth[7:].strip()
    return None


def _unauthorized(detail: str) -> HTTPException:
    return HTTPException(
        status_code=401,
        detail={"error": "unauthorized", "message": detail},
        headers={"WWW-Authenticate": "Bearer"},
    )


def get_auth_context(
    request: Request, db: Session = Depends(get_db)
) -> AuthContext:
    token = _extract_bearer(request)
    if not token:
        raise _unauthorized("missing bearer token")

    repos = Repositories(db)
    row = repos.tokens.get_valid(token)
    if row is None:
        raise _unauthorized("invalid, revoked, or expired token")

    client = repos.api_clients.get(row.client_id)
    if client is None or not client.is_active:
        raise _unauthorized("client is disabled or deleted")

    return AuthContext(
        client_id=client.client_id,
        scopes=row.get_scopes_list(),
        token_hash=row.token_hash,
    )


def require_scopes(*needed: str):
    """Dependency factory: the token must carry every listed scope."""

    def _dependency(ctx: AuthContext = Depends(get_auth_context)) -> AuthContext:
        missing = [s for s in needed if not ctx.has_scope(s)]
        if missing:
            raise HTTPException(
                status_code=403,
                detail={
                    "error": "insufficient_scope",
                    "message": f"token is missing scope(s): {', '.join(missing)}",
                },
            )
        return ctx

    return _dependency
