"""
ORM models for all tables.

``Application`` and ``OAuthSession`` keep the exact schema the service has
shipped with (existing production SQLite files must stay valid); everything
else is new in the auth/management phase.
"""
from datetime import datetime

from sqlalchemy import (
    Boolean,
    Column,
    DateTime,
    Integer,
    String,
    Text,
)
from sqlalchemy.orm import declarative_base

Base = declarative_base()


class Application(Base):
    """Registered client application (a "site")."""
    __tablename__ = "applications"

    app_id = Column(String(50), primary_key=True, index=True)
    name = Column(String(200), nullable=False)
    allowed_domains = Column(Text, nullable=False)  # Comma-separated list
    deep_link_scheme = Column(String(200), nullable=False)
    shared_secret = Column(String(500), nullable=False)
    created_at = Column(DateTime, default=datetime.utcnow, nullable=False)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow, nullable=False)
    is_active = Column(Boolean, default=True, nullable=False)

    def get_allowed_domains_list(self):
        return [domain.strip() for domain in self.allowed_domains.split(",")]

    def is_domain_allowed(self, domain: str) -> bool:
        return domain in self.get_allowed_domains_list()


class OAuthSession(Base):
    """Callback state for an in-flight OAuth flow."""
    __tablename__ = "oauth_sessions"

    session_id = Column(String(36), primary_key=True, index=True)
    app_id = Column(String(50), nullable=False, index=True)
    provider = Column(String(50), nullable=False, index=True)
    deep_link_url = Column(String(500), nullable=False)
    created_at = Column(DateTime, default=datetime.utcnow, nullable=False)
    expires_at = Column(DateTime, nullable=False, index=True)
    used = Column(Boolean, default=False, nullable=False)
    client_ip = Column(String(45), nullable=True)
    user_agent = Column(String(500), nullable=True)

    def is_expired(self) -> bool:
        return datetime.utcnow() > self.expires_at

    def is_valid(self) -> bool:
        return not self.used and not self.is_expired()


class User(Base):
    """Admin UI manager login."""
    __tablename__ = "users"

    username = Column(String(100), primary_key=True)
    password_hash = Column(String(500), nullable=False)
    role = Column(String(20), nullable=False, default="operator")  # admin | operator
    is_active = Column(Boolean, default=True, nullable=False)
    created_at = Column(DateTime, default=datetime.utcnow, nullable=False)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow, nullable=False)
    last_login = Column(DateTime, nullable=True)


class WebSession(Base):
    """Admin UI login session (opaque token, stored hashed)."""
    __tablename__ = "web_sessions"

    token_hash = Column(String(64), primary_key=True)
    username = Column(String(100), nullable=False, index=True)
    csrf_token = Column(String(64), nullable=False)
    created_at = Column(DateTime, default=datetime.utcnow, nullable=False)
    expires_at = Column(DateTime, nullable=False, index=True)
    ip = Column(String(45), nullable=True)
    user_agent = Column(String(500), nullable=True)

    def is_expired(self) -> bool:
        return datetime.utcnow() > self.expires_at


class ApiClient(Base):
    """OAuth2 client for the client_credentials management API."""
    __tablename__ = "api_clients"

    client_id = Column(String(100), primary_key=True)
    name = Column(String(200), nullable=False)
    secret_hash = Column(String(128), nullable=False)  # sha256 hex
    scopes = Column(Text, nullable=False, default="")  # Comma-separated
    is_active = Column(Boolean, default=True, nullable=False)
    created_at = Column(DateTime, default=datetime.utcnow, nullable=False)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow, nullable=False)

    def get_scopes_list(self):
        return [s.strip() for s in self.scopes.split(",") if s.strip()]


class ApiAccessToken(Base):
    """Opaque client_credentials access token, stored hashed."""
    __tablename__ = "api_access_tokens"

    token_hash = Column(String(64), primary_key=True)
    client_id = Column(String(100), nullable=False, index=True)
    scopes = Column(String(500), nullable=False, default="")
    created_at = Column(DateTime, default=datetime.utcnow, nullable=False)
    expires_at = Column(DateTime, nullable=False, index=True)
    revoked_at = Column(DateTime, nullable=True)

    def get_scopes_list(self):
        return [s.strip() for s in self.scopes.split(",") if s.strip()]

    def is_valid(self) -> bool:
        return self.revoked_at is None and datetime.utcnow() < self.expires_at


class AuditLog(Base):
    """Audit trail of management actions."""
    __tablename__ = "audit_log"

    id = Column(Integer, primary_key=True, autoincrement=True)
    ts = Column(DateTime, default=datetime.utcnow, nullable=False, index=True)
    actor = Column(String(120), nullable=False)  # username, client_id, or "system"
    action = Column(String(100), nullable=False)
    target = Column(String(200), nullable=True)
    ip = Column(String(45), nullable=True)
    detail = Column(Text, nullable=True)
