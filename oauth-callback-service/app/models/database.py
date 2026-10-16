"""
Back-compat shim.

The ORM models live in ``app.db.models`` now; this module re-exports them so
existing imports (``app.models``, ``app.models.database``) keep working.
"""
from datetime import datetime, timedelta

from app.db.models import Application, OAuthSession, Base
from app.db.session import get_db, get_session_factory, init_db
from app.utils.passwords import new_token


class _SessionLocalShim:
    """Lazy accessor so importing this module never forces engine creation."""

    def __call__(self):
        return get_session_factory()()


SessionLocal = _SessionLocalShim()


def init_db_tables() -> None:
    init_db()


def seed_sample_application() -> bool:
    """Seed a sample application for development/testing"""
    from app.db.repositories import Repositories

    repos = Repositories(get_session_factory()())
    try:
        if repos.applications.get("pevplugshare") is not None:
            return False
        repos.applications.create(
            app_id="pevplugshare",
            name="PEV PlugShare",
            allowed_domains="pevplugshare.a42r.com,pevplugshare-prod.a42r.com,localhost:8000",
            deep_link_scheme="pevplugshare://oauth/callback",
            shared_secret="CHANGE_THIS_SECRET",
        )
        return True
    finally:
        repos.close()


def cleanup_expired_sessions() -> int:
    """Remove expired OAuth callback sessions from the database"""
    repos = Repositories(get_session_factory()())
    try:
        return repos.oauth_sessions.delete_expired()
    finally:
        repos.close()


def cleanup_expired_web_and_api() -> dict:
    """Housekeeping for the newer tables (called by the same background task)"""
    repos = Repositories(get_session_factory()())
    try:
        return {
            "web_sessions": repos.web_sessions.delete_expired(),
            "api_tokens": repos.tokens.delete_expired(),
        }
    finally:
        repos.close()
