"""
Startup bootstrapping: create the first admin user when the users table is
empty and BOOTSTRAP_ADMIN_PASSWORD is set. Existing users are never touched.
"""
from __future__ import annotations

import logging

from app.config import settings
from app.db.repositories import Repositories
from app.db.session import get_session_factory

logger = logging.getLogger(__name__)


def bootstrap_admin() -> bool:
    """Returns True if a bootstrap admin was created."""
    if not settings.bootstrap_admin_password:
        return False

    repos = Repositories(get_session_factory()())
    try:
        if repos.users.count() > 0:
            return False
        username = settings.bootstrap_admin_user or "admin"
        repos.users.create(username, settings.bootstrap_admin_password, role="admin")
        repos.audit.record(actor="system", action="user.bootstrap", target=username)
        logger.info("Bootstrapped admin user '%s'", username)
        return True
    finally:
        repos.close()
