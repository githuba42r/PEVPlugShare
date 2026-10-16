"""
OAuth2 token endpoint (client_credentials grant only).

This service is a management plane, not an end-user IdP: the only supported
grant is ``client_credentials`` for API clients created via ``oauthctl`` or
the admin UI.
"""
from __future__ import annotations

import base64
import binascii
import logging
from typing import Optional

from fastapi import APIRouter, Depends, Form, HTTPException, Request
from fastapi.responses import JSONResponse
from sqlalchemy.orm import Session

from app.api.deps import ALL_SCOPES
from app.config import settings
from app.db.repositories import Repositories
from app.db.session import get_db

logger = logging.getLogger(__name__)

router = APIRouter()


def _invalid_client(message: str) -> HTTPException:
    return HTTPException(
        status_code=401,
        detail={"error": "invalid_client", "message": message},
        headers={"WWW-Authenticate": "Basic"},
    )


def _extract_client_credentials(
    request: Request,
    client_id: Optional[str],
    client_secret: Optional[str],
) -> tuple:
    """RFC 6749 §2.3.1: HTTP Basic or form-body credentials."""
    auth = request.headers.get("Authorization", "")
    if auth.lower().startswith("basic "):
        try:
            decoded = base64.b64decode(auth[6:].strip()).decode("utf-8")
            cid, _, secret = decoded.partition(":")
        except (binascii.Error, UnicodeDecodeError):
            raise _invalid_client("malformed Basic authorization header")
        if not cid:
            raise _invalid_client("missing client_id in Basic authorization")
        return cid, secret

    if client_id and client_secret:
        return client_id, client_secret

    raise _invalid_client("client credentials required (Basic auth or form body)")


def _filter_scopes(requested: Optional[str], client_scopes: list) -> list:
    """Token scopes = intersection of requested and client's registered scopes."""
    if not requested:
        return client_scopes
    wanted = [s.strip() for s in requested.replace(",", " ").split() if s.strip()]
    unknown = [s for s in wanted if s not in ALL_SCOPES]
    if unknown:
        raise HTTPException(
            status_code=400,
            detail={
                "error": "invalid_scope",
                "message": f"unknown scope(s): {', '.join(unknown)}",
            },
        )
    granted = [s for s in wanted if s in client_scopes]
    if not granted:
        raise HTTPException(
            status_code=400,
            detail={
                "error": "invalid_scope",
                "message": "requested scope(s) not granted to this client",
            },
        )
    return granted


@router.post("/oauth/token")
async def token(
    request: Request,
    grant_type: str = Form(...),
    client_id: Optional[str] = Form(None),
    client_secret: Optional[str] = Form(None),
    scope: Optional[str] = Form(None),
    db: Session = Depends(get_db),
):
    if grant_type != "client_credentials":
        raise HTTPException(
            status_code=400,
            detail={
                "error": "unsupported_grant_type",
                "message": "only 'client_credentials' is supported",
            },
        )

    cid, secret = _extract_client_credentials(request, client_id, client_secret)
    repos = Repositories(db)
    client = repos.api_clients.authenticate(cid, secret)
    if client is None:
        logger.warning("token request failed for client_id=%s", cid)
        repos.audit.record(
            actor=cid, action="token.denied", ip=request.client.host if request.client else None
        )
        raise _invalid_client("invalid client credentials")

    granted = _filter_scopes(scope, client.get_scopes_list())
    row, raw_token = repos.tokens.issue(
        client_id=client.client_id,
        scopes=",".join(granted),
        ttl_seconds=settings.access_token_ttl_seconds,
    )
    repos.audit.record(
        actor=client.client_id,
        action="token.issued",
        target=",".join(granted),
        ip=request.client.host if request.client else None,
    )

    return JSONResponse(
        content={
            "access_token": raw_token,
            "token_type": "Bearer",
            "expires_in": int(
                (row.expires_at - row.created_at).total_seconds()
            ),
            "scope": " ".join(granted),
        }
    )
