"""
/api/v1 — remote management API, authenticated with client_credentials
Bearer tokens (see routes_token.py for issuance).
"""
from __future__ import annotations

from typing import Optional

from fastapi import APIRouter, Depends, HTTPException, Request
from pydantic import BaseModel, Field
from sqlalchemy.orm import Session

from app.api.deps import AuthContext, require_scopes
from app.db.repositories import Duplicate, Invalid, NotFound, Repositories
from app.db.session import get_db

router = APIRouter(prefix="/api/v1")


def _http(status: int, error: str, message: str) -> HTTPException:
    return HTTPException(status_code=status, detail={"error": error, "message": message})


def _client_ip(request: Request) -> Optional[str]:
    return request.client.host if request.client else None


# ---------------------------------------------------------------- schemas


class ApplicationIn(BaseModel):
    app_id: str = Field(..., pattern=r"^[a-z0-9_-]{1,50}$")
    name: str
    allowed_domains: str
    deep_link_scheme: str
    shared_secret: Optional[str] = None
    is_active: bool = True


class ApplicationPatch(BaseModel):
    name: Optional[str] = None
    allowed_domains: Optional[str] = None
    deep_link_scheme: Optional[str] = None
    is_active: Optional[bool] = None


class ClientIn(BaseModel):
    name: str
    scopes: str = Field(..., description="Comma-separated scopes")
    client_id: Optional[str] = Field(None, pattern=r"^[a-z0-9_-]{1,100}$")


class ClientPatch(BaseModel):
    name: Optional[str] = None
    scopes: Optional[str] = None
    is_active: Optional[bool] = None


class UserIn(BaseModel):
    username: str = Field(..., pattern=r"^[a-zA-Z0-9_.-]{1,100}$")
    password: str = Field(..., min_length=8)
    role: str = Field("operator", pattern=r"^(admin|operator)$")


class UserPatch(BaseModel):
    password: Optional[str] = Field(None, min_length=8)
    role: Optional[str] = Field(None, pattern=r"^(admin|operator)$")
    is_active: Optional[bool] = None


# ---------------------------------------------------------------- whoami


@router.get("/whoami")
def whoami(ctx: AuthContext = Depends(require_scopes("apps:read"))):
    return {"client_id": ctx.client_id, "scopes": ctx.scopes}


# ---------------------------------------------------------------- applications


def _app_json(app, include_secret: bool = False) -> dict:
    data = {
        "app_id": app.app_id,
        "name": app.name,
        "allowed_domains": app.allowed_domains,
        "deep_link_scheme": app.deep_link_scheme,
        "is_active": app.is_active,
        "created_at": app.created_at.isoformat() if app.created_at else None,
        "updated_at": app.updated_at.isoformat() if app.updated_at else None,
    }
    if include_secret:
        data["shared_secret"] = app.shared_secret
    else:
        data["secret_preview"] = (
            app.shared_secret[:20] + "..." if app.shared_secret else ""
        )
    return data


@router.get("/applications")
def list_applications(
    db: Session = Depends(get_db),
    ctx: AuthContext = Depends(require_scopes("apps:read")),
):
    repos = Repositories(db)
    return [_app_json(a) for a in repos.applications.list()]


@router.get("/applications/{app_id}")
def get_application(
    app_id: str,
    db: Session = Depends(get_db),
    ctx: AuthContext = Depends(require_scopes("apps:read")),
):
    repos = Repositories(db)
    app = repos.applications.get(app_id)
    if app is None:
        raise _http(404, "not_found", f"application '{app_id}' not found")
    return _app_json(app, include_secret=True)


@router.post("/applications", status_code=201)
def create_application(
    body: ApplicationIn,
    request: Request,
    db: Session = Depends(get_db),
    ctx: AuthContext = Depends(require_scopes("apps:write")),
):
    repos = Repositories(db)
    try:
        app, secret = repos.applications.create(
            app_id=body.app_id,
            name=body.name,
            allowed_domains=body.allowed_domains,
            deep_link_scheme=body.deep_link_scheme,
            shared_secret=body.shared_secret,
            is_active=body.is_active,
        )
    except Duplicate as exc:
        raise _http(409, "duplicate", str(exc))
    repos.audit.record(
        actor=ctx.client_id,
        action="app.create",
        target=body.app_id,
        ip=_client_ip(request),
    )
    data = _app_json(app, include_secret=True)
    data["shared_secret"] = secret
    return data


@router.patch("/applications/{app_id}")
def patch_application(
    app_id: str,
    body: ApplicationPatch,
    request: Request,
    db: Session = Depends(get_db),
    ctx: AuthContext = Depends(require_scopes("apps:write")),
):
    repos = Repositories(db)
    try:
        app = repos.applications.update(app_id, **body.model_dump(exclude_unset=True))
    except NotFound as exc:
        raise _http(404, "not_found", str(exc))
    repos.audit.record(
        actor=ctx.client_id,
        action="app.update",
        target=app_id,
        ip=_client_ip(request),
        detail=body.model_dump_json(exclude_unset=True),
    )
    return _app_json(app)


@router.post("/applications/{app_id}/rotate-secret")
def rotate_application_secret(
    app_id: str,
    request: Request,
    db: Session = Depends(get_db),
    ctx: AuthContext = Depends(require_scopes("apps:write")),
):
    repos = Repositories(db)
    try:
        secret = repos.applications.rotate_secret(app_id)
    except NotFound as exc:
        raise _http(404, "not_found", str(exc))
    repos.audit.record(
        actor=ctx.client_id, action="app.rotate_secret", target=app_id, ip=_client_ip(request)
    )
    return {"app_id": app_id, "shared_secret": secret}


@router.delete("/applications/{app_id}")
def delete_application(
    app_id: str,
    request: Request,
    db: Session = Depends(get_db),
    ctx: AuthContext = Depends(require_scopes("apps:write")),
):
    repos = Repositories(db)
    try:
        repos.applications.delete(app_id)
    except NotFound as exc:
        raise _http(404, "not_found", str(exc))
    repos.audit.record(
        actor=ctx.client_id, action="app.delete", target=app_id, ip=_client_ip(request)
    )
    return {"success": True}


# ---------------------------------------------------------------- api clients


def _client_json(client) -> dict:
    return {
        "client_id": client.client_id,
        "name": client.name,
        "scopes": client.scopes,
        "is_active": client.is_active,
        "created_at": client.created_at.isoformat() if client.created_at else None,
        "updated_at": client.updated_at.isoformat() if client.updated_at else None,
    }


@router.get("/clients")
def list_clients(
    db: Session = Depends(get_db),
    ctx: AuthContext = Depends(require_scopes("clients:manage")),
):
    repos = Repositories(db)
    return [_client_json(c) for c in repos.api_clients.list()]


@router.get("/clients/{client_id}")
def get_client(
    client_id: str,
    db: Session = Depends(get_db),
    ctx: AuthContext = Depends(require_scopes("clients:manage")),
):
    repos = Repositories(db)
    client = repos.api_clients.get(client_id)
    if client is None:
        raise _http(404, "not_found", f"api client '{client_id}' not found")
    tokens = [
        {
            "token_hash": t.token_hash[:16] + "...",
            "scopes": t.scopes,
            "created_at": t.created_at.isoformat() if t.created_at else None,
            "expires_at": t.expires_at.isoformat() if t.expires_at else None,
            "revoked_at": t.revoked_at.isoformat() if t.revoked_at else None,
        }
        for t in repos.tokens.list_for_client(client_id)
    ]
    data = _client_json(client)
    data["tokens"] = tokens
    return data


@router.post("/clients", status_code=201)
def create_client(
    body: ClientIn,
    request: Request,
    db: Session = Depends(get_db),
    ctx: AuthContext = Depends(require_scopes("clients:manage")),
):
    from app.api.deps import ALL_SCOPES

    requested = [s.strip() for s in body.scopes.split(",") if s.strip()]
    unknown = [s for s in requested if s not in ALL_SCOPES]
    if unknown:
        raise _http(400, "invalid_scope", f"unknown scope(s): {', '.join(unknown)}")

    repos = Repositories(db)
    try:
        client, secret = repos.api_clients.create(
            name=body.name, scopes=",".join(requested), client_id=body.client_id
        )
    except Duplicate as exc:
        raise _http(409, "duplicate", str(exc))
    repos.audit.record(
        actor=ctx.client_id,
        action="client.create",
        target=client.client_id,
        ip=_client_ip(request),
    )
    data = _client_json(client)
    data["client_secret"] = secret  # shown once
    return data


@router.patch("/clients/{client_id}")
def patch_client(
    client_id: str,
    body: ClientPatch,
    request: Request,
    db: Session = Depends(get_db),
    ctx: AuthContext = Depends(require_scopes("clients:manage")),
):
    repos = Repositories(db)
    try:
        client = repos.api_clients.update(client_id, **body.model_dump(exclude_unset=True))
    except NotFound as exc:
        raise _http(404, "not_found", str(exc))
    repos.audit.record(
        actor=ctx.client_id,
        action="client.update",
        target=client_id,
        ip=_client_ip(request),
        detail=body.model_dump_json(exclude_unset=True),
    )
    return _client_json(client)


@router.post("/clients/{client_id}/rotate-secret")
def rotate_client_secret(
    client_id: str,
    request: Request,
    db: Session = Depends(get_db),
    ctx: AuthContext = Depends(require_scopes("clients:manage")),
):
    repos = Repositories(db)
    try:
        secret = repos.api_clients.rotate_secret(client_id)
        repos.tokens.revoke_all_for_client(client_id)
    except NotFound as exc:
        raise _http(404, "not_found", str(exc))
    repos.audit.record(
        actor=ctx.client_id,
        action="client.rotate_secret",
        target=client_id,
        ip=_client_ip(request),
    )
    return {"client_id": client_id, "client_secret": secret}


@router.delete("/clients/{client_id}")
def delete_client(
    client_id: str,
    request: Request,
    db: Session = Depends(get_db),
    ctx: AuthContext = Depends(require_scopes("clients:manage")),
):
    repos = Repositories(db)
    try:
        repos.api_clients.delete(client_id)
    except NotFound as exc:
        raise _http(404, "not_found", str(exc))
    repos.audit.record(
        actor=ctx.client_id, action="client.delete", target=client_id, ip=_client_ip(request)
    )
    return {"success": True}


@router.post("/clients/{client_id}/revoke-tokens")
def revoke_client_tokens(
    client_id: str,
    request: Request,
    db: Session = Depends(get_db),
    ctx: AuthContext = Depends(require_scopes("clients:manage")),
):
    repos = Repositories(db)
    if repos.api_clients.get(client_id) is None:
        raise _http(404, "not_found", f"api client '{client_id}' not found")
    count = repos.tokens.revoke_all_for_client(client_id)
    repos.audit.record(
        actor=ctx.client_id,
        action="client.revoke_tokens",
        target=client_id,
        ip=_client_ip(request),
        detail=f"revoked {count}",
    )
    return {"client_id": client_id, "revoked": count}


# ---------------------------------------------------------------- users


def _user_json(user) -> dict:
    return {
        "username": user.username,
        "role": user.role,
        "is_active": user.is_active,
        "created_at": user.created_at.isoformat() if user.created_at else None,
        "last_login": user.last_login.isoformat() if user.last_login else None,
    }


@router.get("/users")
def list_users(
    db: Session = Depends(get_db),
    ctx: AuthContext = Depends(require_scopes("users:manage")),
):
    repos = Repositories(db)
    return [_user_json(u) for u in repos.users.list()]


@router.post("/users", status_code=201)
def create_user(
    body: UserIn,
    request: Request,
    db: Session = Depends(get_db),
    ctx: AuthContext = Depends(require_scopes("users:manage")),
):
    repos = Repositories(db)
    try:
        user = repos.users.create(body.username, body.password, body.role)
    except (Duplicate, Invalid) as exc:
        raise _http(400 if isinstance(exc, Invalid) else 409, type(exc).__name__.lower(), str(exc))
    repos.audit.record(
        actor=ctx.client_id, action="user.create", target=body.username, ip=_client_ip(request)
    )
    return _user_json(user)


@router.patch("/users/{username}")
def patch_user(
    username: str,
    body: UserPatch,
    request: Request,
    db: Session = Depends(get_db),
    ctx: AuthContext = Depends(require_scopes("users:manage")),
):
    repos = Repositories(db)
    data = body.model_dump(exclude_unset=True)
    try:
        if "password" in data:
            repos.users.set_password(username, data.pop("password"))
        if "role" in data:
            repos.users.set_role(username, data.pop("role"))
        if "is_active" in data:
            repos.users.set_active(username, data.pop("is_active"))
        user = repos.users.require(username)
    except NotFound as exc:
        raise _http(404, "not_found", str(exc))
    except Invalid as exc:
        raise _http(400, "invalid", str(exc))
    repos.audit.record(
        actor=ctx.client_id,
        action="user.update",
        target=username,
        ip=_client_ip(request),
        detail=body.model_dump_json(exclude_unset=True, exclude={"password"}),
    )
    return _user_json(user)


@router.delete("/users/{username}")
def delete_user(
    username: str,
    request: Request,
    db: Session = Depends(get_db),
    ctx: AuthContext = Depends(require_scopes("users:manage")),
):
    repos = Repositories(db)
    if username == ctx.client_id:
        raise _http(400, "invalid", "cannot delete the acting principal")
    try:
        repos.users.delete(username)
    except NotFound as exc:
        raise _http(404, "not_found", str(exc))
    repos.audit.record(
        actor=ctx.client_id, action="user.delete", target=username, ip=_client_ip(request)
    )
    return {"success": True}


# ---------------------------------------------------------------- sessions


@router.get("/sessions")
def list_sessions(
    app_id: Optional[str] = None,
    limit: int = 50,
    db: Session = Depends(get_db),
    ctx: AuthContext = Depends(require_scopes("sessions:read")),
):
    repos = Repositories(db)
    return [
        {
            "session_id": s.session_id,
            "app_id": s.app_id,
            "provider": s.provider,
            "created_at": s.created_at.isoformat() if s.created_at else None,
            "expires_at": s.expires_at.isoformat() if s.expires_at else None,
            "used": s.used,
            "is_expired": s.is_expired(),
        }
        for s in repos.oauth_sessions.list(app_id=app_id, limit=limit)
    ]


@router.delete("/sessions")
def purge_sessions(
    request: Request,
    app_id: Optional[str] = None,
    db: Session = Depends(get_db),
    ctx: AuthContext = Depends(require_scopes("sessions:write")),
):
    repos = Repositories(db)
    count = repos.oauth_sessions.purge(app_id=app_id)
    repos.audit.record(
        actor=ctx.client_id,
        action="sessions.purge",
        target=app_id or "*",
        ip=_client_ip(request),
        detail=f"purged {count}",
    )
    return {"purged": count}


# ---------------------------------------------------------------- audit


@router.get("/audit")
def list_audit(
    limit: int = 100,
    actor: Optional[str] = None,
    db: Session = Depends(get_db),
    ctx: AuthContext = Depends(require_scopes("audit:read")),
):
    repos = Repositories(db)
    return [
        {
            "id": row.id,
            "ts": row.ts.isoformat() if row.ts else None,
            "actor": row.actor,
            "action": row.action,
            "target": row.target,
            "ip": row.ip,
            "detail": row.detail,
        }
        for row in repos.audit.list(limit=limit, actor=actor)
    ]
