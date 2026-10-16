"""
Engine / session factory.

The backend is chosen by the ``database_url`` scheme so swapping SQLite for
MySQL or Postgres is a config change only:

    sqlite:////app/data/oauth.db
    postgresql+asyncpg://user:pass@host/db   (driver must be installed)
    mysql+pymysql://user:pass@host/db

SQLite runs in WAL mode with foreign keys on so the FastAPI process, the
Flask admin UI and an ``oauthctl`` exec can share the file safely.
"""
from __future__ import annotations

from typing import Iterator, Optional

from sqlalchemy import create_engine, event
from sqlalchemy.engine import Engine
from sqlalchemy.orm import Session, sessionmaker

from app.config import settings

_engine: Optional[Engine] = None
_session_factory: Optional[sessionmaker] = None


def _connect_args_for(url: str) -> dict:
    if url.startswith("sqlite"):
        return {"check_same_thread": False}
    return {}


def _apply_sqlite_pragmas(dbapi_conn, _record) -> None:
    # Only real sqlite3 connections (not, e.g., a mock in tests)
    if type(dbapi_conn).__module__.startswith("sqlite3"):
        cursor = dbapi_conn.cursor()
        cursor.execute("PRAGMA journal_mode=WAL")
        cursor.execute("PRAGMA foreign_keys=ON")
        cursor.close()


def make_engine(url: str, echo: bool = False) -> Engine:
    engine = create_engine(
        url,
        connect_args=_connect_args_for(url),
        echo=echo,
        future=True,
    )
    if url.startswith("sqlite"):
        event.listen(engine, "connect", _apply_sqlite_pragmas)
    return engine


def configure(url: Optional[str] = None, echo: Optional[bool] = None) -> Engine:
    """(Re)build the process-wide engine + session factory."""
    global _engine, _session_factory
    if _engine is not None:
        _engine.dispose()
    _engine = make_engine(
        url or settings.database_url,
        echo=settings.log_level == "DEBUG" if echo is None else echo,
    )
    _session_factory = sessionmaker(autocommit=False, autoflush=False, bind=_engine, future=True)
    return _engine


def get_engine() -> Engine:
    if _engine is None:
        configure()
    return _engine


def get_session_factory() -> sessionmaker:
    if _session_factory is None:
        configure()
    return _session_factory


def get_db() -> Iterator[Session]:
    """FastAPI dependency yielding a Session."""
    session: Session = get_session_factory()()
    try:
        yield session
    finally:
        session.close()


def run_migrations(engine: Optional[Engine] = None) -> None:
    from app.db.migrations import upgrade

    upgrade(engine or get_engine())


def init_db(engine: Optional[Engine] = None) -> None:
    """Bring the schema up to date (idempotent, safe on existing DBs)."""
    run_migrations(engine)
