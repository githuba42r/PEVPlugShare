"""
Minimal, dependency-free schema migrations.

Alembic was considered; a ~100-line runner was chosen instead so the container
stays lean and the CLI (``oauthctl db upgrade``) can run the exact same code
path the service runs at startup. Each migration is an ``(version, name,
upgrade_fn)`` triple recorded in ``schema_migrations``; ``upgrade_fn`` receives
an open SQLAlchemy Connection.

Legacy databases created by older builds (``applications`` / ``oauth_sessions``
exist, ``schema_migrations`` does not) are detected and stamped so the new
tables are added without touching existing rows.
"""
from __future__ import annotations

import logging
from datetime import datetime
from typing import Callable, List, Tuple

from sqlalchemy import Connection, MetaData, Table, Column, Integer, String, DateTime, inspect, text

from app.db.models import Base

logger = logging.getLogger(__name__)


def _create_table_set(conn: Connection, names: List[str]) -> None:
    metadata = MetaData()
    for table_name in names:
        Base.metadata.tables[table_name].tometadata(metadata)
    metadata.create_all(bind=conn)


def _upgrade_v1(conn: Connection) -> None:
    _create_table_set(conn, ["applications", "oauth_sessions"])


def _upgrade_v2(conn: Connection) -> None:
    _create_table_set(
        conn,
        ["users", "web_sessions", "api_clients", "api_access_tokens", "audit_log"],
    )


MIGRATIONS: List[Tuple[int, str, Callable[[Connection], None]]] = [
    (1, "initial_applications_sessions", _upgrade_v1),
    (2, "auth_and_management", _upgrade_v2),
]

VERSION_TABLE = Table(
    "schema_migrations",
    MetaData(),
    Column("version", Integer, primary_key=True),
    Column("name", String(200), nullable=False),
    Column("applied_at", DateTime, nullable=False),
)


def _current_version(conn: Connection) -> int:
    VERSION_TABLE.create(bind=conn, checkfirst=True)
    row = conn.execute(text("SELECT MAX(version) FROM schema_migrations")).scalar()
    return int(row) if row else 0


def _has_legacy_tables(conn: Connection) -> bool:
    inspector_names = set(inspect(conn).get_table_names())
    return (
        "schema_migrations" not in inspector_names
        and "applications" in inspector_names
    )


def upgrade(engine) -> None:
    with engine.begin() as conn:
        version = _current_version(conn)
        if version == 0 and _has_legacy_tables(conn):
            # DB predates migrations: stamp everything that already exists
            for v, name, _fn in MIGRATIONS:
                conn.execute(
                    VERSION_TABLE.insert().values(
                        version=v, name=name, applied_at=datetime.utcnow()
                    )
                )
            version = MIGRATIONS[-1][0]
            logger.info("Legacy database detected; stamped at version %s", version)

        for v, name, fn in MIGRATIONS:
            if v <= version:
                continue
            logger.info("Applying migration %s (%s)", v, name)
            fn(conn)
            conn.execute(
                VERSION_TABLE.insert().values(
                    version=v, name=name, applied_at=datetime.utcnow()
                )
            )
        logger.info("Database schema at version %s", _current_version(conn))
