"""
Repository layer — the pluggability seam.

Every persistence operation used by the FastAPI service, the Flask admin UI
and the ``oauthctl`` CLI goes through these classes. The SQL implementation
below is the only one that ships today; a future DynamoDB/Firebase backend is
a new class implementing the same method surface, selected by config.

All methods commit (or roll back) on the injected Session themselves, so a
caller never has to know about unit-of-work details.
"""
from __future__ import annotations

import secrets
from datetime import datetime, timedelta
from typing import List, Optional, Tuple

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.db.models import (
    ApiAccessToken,
    ApiClient,
    Application,
    AuditLog,
    OAuthSession,
    User,
    WebSession,
)
from app.utils.passwords import (
    hash_password,
    hash_token,
    new_token,
    verify_password,
)


class RepoError(Exception):
    """Base repository error."""


class NotFound(RepoError):
    pass


class Duplicate(RepoError):
    pass


class Invalid(RepoError):
    pass


class ApplicationRepository:
    def __init__(self, session: Session):
        self.session = session

    def get(self, app_id: str) -> Optional[Application]:
        return self.session.get(Application, app_id)

    def require(self, app_id: str) -> Application:
        app = self.get(app_id)
        if app is None:
            raise NotFound(f"application '{app_id}' not found")
        return app

    def list(self) -> List[Application]:
        return list(
            self.session.scalars(select(Application).order_by(Application.app_id))
        )

    def list_active(self) -> List[Application]:
        return list(
            self.session.scalars(
                select(Application).where(Application.is_active.is_(True))
            )
        )

    def create(
        self,
        app_id: str,
        name: str,
        allowed_domains: str,
        deep_link_scheme: str,
        shared_secret: Optional[str] = None,
        is_active: bool = True,
    ) -> Tuple[Application, str]:
        if self.get(app_id) is not None:
            raise Duplicate(f"application '{app_id}' already exists")
        secret = shared_secret or new_token()
        app = Application(
            app_id=app_id,
            name=name,
            allowed_domains=allowed_domains,
            deep_link_scheme=deep_link_scheme,
            shared_secret=secret,
            is_active=is_active,
        )
        self.session.add(app)
        self.session.commit()
        self.session.refresh(app)
        return app, secret

    def update(self, app_id: str, **fields) -> Application:
        app = self.require(app_id)
        allowed = {
            "name",
            "allowed_domains",
            "deep_link_scheme",
            "shared_secret",
            "is_active",
        }
        for key, value in fields.items():
            if key in allowed and value is not None:
                setattr(app, key, value)
        app.updated_at = datetime.utcnow()
        self.session.commit()
        self.session.refresh(app)
        return app

    def rotate_secret(self, app_id: str, secret: Optional[str] = None) -> str:
        new_secret = secret or new_token()
        self.update(app_id, shared_secret=new_secret)
        return new_secret

    def set_active(self, app_id: str, active: bool) -> Application:
        return self.update(app_id, is_active=active)

    def delete(self, app_id: str) -> None:
        app = self.require(app_id)
        self.session.delete(app)
        self.session.commit()


class UserRepository:
    def __init__(self, session: Session):
        self.session = session

    def get(self, username: str) -> Optional[User]:
        return self.session.get(User, username)

    def require(self, username: str) -> User:
        user = self.get(username)
        if user is None:
            raise NotFound(f"user '{username}' not found")
        return user

    def list(self) -> List[User]:
        return list(self.session.scalars(select(User).order_by(User.username)))

    def count(self) -> int:
        return len(self.list())

    def create(self, username: str, password: str, role: str = "operator") -> User:
        if role not in ("admin", "operator"):
            raise Invalid(f"invalid role '{role}' (admin|operator)")
        if self.get(username) is not None:
            raise Duplicate(f"user '{username}' already exists")
        user = User(
            username=username,
            password_hash=hash_password(password),
            role=role,
        )
        self.session.add(user)
        self.session.commit()
        self.session.refresh(user)
        return user

    def set_password(self, username: str, password: str) -> User:
        user = self.require(username)
        user.password_hash = hash_password(password)
        user.updated_at = datetime.utcnow()
        self.session.commit()
        return user

    def set_role(self, username: str, role: str) -> User:
        if role not in ("admin", "operator"):
            raise Invalid(f"invalid role '{role}' (admin|operator)")
        user = self.require(username)
        user.role = role
        user.updated_at = datetime.utcnow()
        self.session.commit()
        return user

    def set_active(self, username: str, active: bool) -> User:
        user = self.require(username)
        user.is_active = active
        user.updated_at = datetime.utcnow()
        self.session.commit()
        return user

    def delete(self, username: str) -> None:
        user = self.require(username)
        self.session.delete(user)
        self.session.execute(
            WebSession.__table__.delete().where(WebSession.username == username)
        )
        self.session.commit()

    def touch_login(self, username: str) -> None:
        user = self.get(username)
        if user is not None:
            user.last_login = datetime.utcnow()
            self.session.commit()

    def authenticate(self, username: str, password: str) -> Optional[User]:
        user = self.get(username)
        if user is None or not user.is_active:
            return None
        if not verify_password(user.password_hash, password):
            return None
        return user


class WebSessionRepository:
    def __init__(self, session: Session):
        self.session = session

    def create(
        self,
        username: str,
        ttl_minutes: int,
        ip: Optional[str] = None,
        user_agent: Optional[str] = None,
    ) -> Tuple[WebSession, str]:
        raw_token = new_token()
        row = WebSession(
            token_hash=hash_token(raw_token),
            username=username,
            csrf_token=new_token(24),
            expires_at=datetime.utcnow() + timedelta(minutes=ttl_minutes),
            ip=ip,
            user_agent=user_agent,
        )
        self.session.add(row)
        self.session.commit()
        return row, raw_token

    def get_valid(self, raw_token: str) -> Optional[WebSession]:
        row = self.session.get(WebSession, hash_token(raw_token))
        if row is None or row.is_expired():
            return None
        return row

    def delete(self, raw_token: str) -> bool:
        row = self.session.get(WebSession, hash_token(raw_token))
        if row is None:
            return False
        self.session.delete(row)
        self.session.commit()
        return True

    def delete_for_user(self, username: str) -> int:
        rows = list(
            self.session.scalars(
                select(WebSession).where(WebSession.username == username)
            )
        )
        for row in rows:
            self.session.delete(row)
        self.session.commit()
        return len(rows)

    def list(self, limit: int = 100) -> List[WebSession]:
        return list(
            self.session.scalars(
                select(WebSession)
                .order_by(WebSession.created_at.desc())
                .limit(limit)
            )
        )

    def delete_expired(self) -> int:
        rows = list(
            self.session.scalars(
                select(WebSession).where(
                    WebSession.expires_at < datetime.utcnow()
                )
            )
        )
        for row in rows:
            self.session.delete(row)
        self.session.commit()
        return len(rows)


class ApiClientRepository:
    def __init__(self, session: Session):
        self.session = session

    def get(self, client_id: str) -> Optional[ApiClient]:
        return self.session.get(ApiClient, client_id)

    def require(self, client_id: str) -> ApiClient:
        client = self.get(client_id)
        if client is None:
            raise NotFound(f"api client '{client_id}' not found")
        return client

    def list(self) -> List[ApiClient]:
        return list(
            self.session.scalars(
                select(ApiClient).order_by(ApiClient.client_id)
            )
        )

    def create(
        self,
        name: str,
        scopes: str,
        client_id: Optional[str] = None,
    ) -> Tuple[ApiClient, str]:
        cid = client_id or f"cli_{secrets.token_hex(8)}"
        if self.get(cid) is not None:
            raise Duplicate(f"api client '{cid}' already exists")
        raw_secret = new_token()
        client = ApiClient(
            client_id=cid,
            name=name,
            secret_hash=hash_token(raw_secret),
            scopes=scopes,
        )
        self.session.add(client)
        self.session.commit()
        self.session.refresh(client)
        return client, raw_secret

    def rotate_secret(self, client_id: str) -> str:
        client = self.require(client_id)
        raw_secret = new_token()
        client.secret_hash = hash_token(raw_secret)
        client.updated_at = datetime.utcnow()
        self.session.commit()
        return raw_secret

    def update(self, client_id: str, **fields) -> ApiClient:
        client = self.require(client_id)
        for key in ("name", "scopes", "is_active"):
            if key in fields and fields[key] is not None:
                setattr(client, key, fields[key])
        client.updated_at = datetime.utcnow()
        self.session.commit()
        self.session.refresh(client)
        return client

    def set_active(self, client_id: str, active: bool) -> ApiClient:
        return self.update(client_id, is_active=active)

    def delete(self, client_id: str) -> None:
        client = self.require(client_id)
        self.session.delete(client)
        self.session.execute(
            ApiAccessToken.__table__.delete().where(
                ApiAccessToken.client_id == client_id
            )
        )
        self.session.commit()

    def authenticate(self, client_id: str, secret: str) -> Optional[ApiClient]:
        from app.utils.passwords import token_matches

        client = self.get(client_id)
        if client is None or not client.is_active:
            return None
        if not token_matches(client.secret_hash, secret):
            return None
        return client


class TokenRepository:
    def __init__(self, session: Session):
        self.session = session

    def issue(
        self, client_id: str, scopes: str, ttl_seconds: int
    ) -> Tuple[ApiAccessToken, str]:
        raw_token = new_token()
        row = ApiAccessToken(
            token_hash=hash_token(raw_token),
            client_id=client_id,
            scopes=scopes,
            expires_at=datetime.utcnow() + timedelta(seconds=ttl_seconds),
        )
        self.session.add(row)
        self.session.commit()
        self.session.refresh(row)
        return row, raw_token

    def get_valid(self, raw_token: str) -> Optional[ApiAccessToken]:
        row = self.session.get(ApiAccessToken, hash_token(raw_token))
        if row is None or not row.is_valid():
            return None
        return row

    def revoke(self, raw_token: str) -> bool:
        row = self.session.get(ApiAccessToken, hash_token(raw_token))
        if row is None or row.revoked_at is not None:
            return False
        row.revoked_at = datetime.utcnow()
        self.session.commit()
        return True

    def revoke_all_for_client(self, client_id: str) -> int:
        rows = list(
            self.session.scalars(
                select(ApiAccessToken).where(
                    ApiAccessToken.client_id == client_id,
                    ApiAccessToken.revoked_at.is_(None),
                )
            )
        )
        for row in rows:
            row.revoked_at = datetime.utcnow()
        self.session.commit()
        return len(rows)

    def list_for_client(self, client_id: str) -> List[ApiAccessToken]:
        return list(
            self.session.scalars(
                select(ApiAccessToken)
                .where(ApiAccessToken.client_id == client_id)
                .order_by(ApiAccessToken.created_at.desc())
            )
        )

    def list_recent(self, limit: int = 100) -> List[ApiAccessToken]:
        return list(
            self.session.scalars(
                select(ApiAccessToken)
                .order_by(ApiAccessToken.created_at.desc())
                .limit(limit)
            )
        )

    def delete_expired(self) -> int:
        rows = list(
            self.session.scalars(
                select(ApiAccessToken).where(
                    ApiAccessToken.expires_at < datetime.utcnow()
                )
            )
        )
        for row in rows:
            self.session.delete(row)
        self.session.commit()
        return len(rows)


class OAuthSessionRepository:
    def __init__(self, session: Session):
        self.session = session

    def list(self, app_id: Optional[str] = None, limit: int = 50) -> List[OAuthSession]:
        query = select(OAuthSession).order_by(OAuthSession.created_at.desc())
        if app_id:
            query = query.where(OAuthSession.app_id == app_id)
        query = query.limit(limit)
        return list(self.session.scalars(query))

    def get(self, session_id: str) -> Optional[OAuthSession]:
        return self.session.get(OAuthSession, session_id)

    def delete_expired(self) -> int:
        rows = list(
            self.session.scalars(
                select(OAuthSession).where(
                    OAuthSession.expires_at < datetime.utcnow()
                )
            )
        )
        for row in rows:
            self.session.delete(row)
        self.session.commit()
        return len(rows)

    def purge(self, app_id: Optional[str] = None) -> int:
        query = select(OAuthSession)
        if app_id:
            query = query.where(OAuthSession.app_id == app_id)
        rows = list(self.session.scalars(query))
        for row in rows:
            self.session.delete(row)
        self.session.commit()
        return len(rows)


class AuditRepository:
    def __init__(self, session: Session):
        self.session = session

    def record(
        self,
        actor: str,
        action: str,
        target: Optional[str] = None,
        ip: Optional[str] = None,
        detail: Optional[str] = None,
    ) -> AuditLog:
        row = AuditLog(actor=actor, action=action, target=target, ip=ip, detail=detail)
        self.session.add(row)
        self.session.commit()
        return row

    def list(self, limit: int = 100, actor: Optional[str] = None) -> List[AuditLog]:
        query = select(AuditLog).order_by(AuditLog.id.desc())
        if actor:
            query = query.where(AuditLog.actor == actor)
        query = query.limit(limit)
        return list(self.session.scalars(query))


class Repositories:
    """Facade binding one Session to every repository."""

    def __init__(self, session: Session):
        self.session = session
        self.applications = ApplicationRepository(session)
        self.users = UserRepository(session)
        self.web_sessions = WebSessionRepository(session)
        self.api_clients = ApiClientRepository(session)
        self.tokens = TokenRepository(session)
        self.oauth_sessions = OAuthSessionRepository(session)
        self.audit = AuditRepository(session)

    def close(self) -> None:
        self.session.close()

    def __enter__(self) -> "Repositories":
        return self

    def __exit__(self, *exc) -> None:
        self.close()


def get_repos(session: Session) -> Repositories:
    return Repositories(session)
