"""
Pluggable database layer.

SQL backends (SQLite now, MySQL/Postgres via a DATABASE_URL scheme swap) are
handled by SQLAlchemy behind the repository classes in ``repositories.py``.
Non-SQL backends (DynamoDB, Firebase, ...) can be added later by implementing
the same repository interfaces — no call sites change.

Portability rules for anything added here:
  - no raw SQL outside ``migrations.py`` and repository internals
  - no SQLite-only column types or pragmas outside ``session.py``
  - timestamps are naive UTC (``datetime.utcnow``) everywhere
  - never call DB-side ``now()``
"""
from app.db.session import (
    configure,
    get_engine,
    get_session_factory,
    get_db,
    init_db,
    run_migrations,
)
from app.db.repositories import Repositories

__all__ = [
    "configure",
    "get_engine",
    "get_session_factory",
    "get_db",
    "init_db",
    "run_migrations",
    "Repositories",
]
