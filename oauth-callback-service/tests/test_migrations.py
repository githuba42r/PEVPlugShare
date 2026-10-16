"""Migration runner tests: fresh DB, idempotency, and legacy-DB stamping."""
from sqlalchemy import create_engine, inspect, text


def test_fresh_db_full_upgrade(db_path):
    from app.db.session import configure, init_db, get_session_factory
    from app.db.models import User, Application

    engine = configure(db_path)
    init_db(engine)

    names = set(inspect(engine).get_table_names())
    for expected in (
        "applications", "oauth_sessions", "users", "web_sessions",
        "api_clients", "api_access_tokens", "audit_log", "schema_migrations",
    ):
        assert expected in names

    # idempotent
    init_db(engine)

    with engine.connect() as conn:
        version = conn.execute(text("SELECT MAX(version) FROM schema_migrations")).scalar()
    assert version == 2


def test_legacy_db_is_stamped(tmp_path):
    """A DB created by the old create_all() flow (no schema_migrations) must be
    recognised: existing tables untouched, new tables added."""
    url = f"sqlite:///{tmp_path / 'legacy.db'}"
    engine = create_engine(url)
    with engine.begin() as conn:
        conn.execute(text(
            "CREATE TABLE applications ("
            "app_id VARCHAR(50) PRIMARY KEY, name VARCHAR(200) NOT NULL, "
            "allowed_domains TEXT NOT NULL, deep_link_scheme VARCHAR(200) NOT NULL, "
            "shared_secret VARCHAR(500) NOT NULL, "
            "created_at DATETIME NOT NULL, updated_at DATETIME NOT NULL, "
            "is_active BOOLEAN NOT NULL)"
        ))
        conn.execute(text(
            "INSERT INTO applications VALUES "
            "('legacy', 'Legacy App', 'legacy.example.com', 'legacy://cb', "
            "'secret', '2026-01-01 00:00:00', '2026-01-01 00:00:00', 1)"
        ))
        conn.execute(text(
            "CREATE TABLE oauth_sessions ("
            "session_id VARCHAR(36) PRIMARY KEY, app_id VARCHAR(50) NOT NULL, "
            "provider VARCHAR(50) NOT NULL, deep_link_url VARCHAR(500) NOT NULL, "
            "created_at DATETIME NOT NULL, expires_at DATETIME NOT NULL, "
            "used BOOLEAN NOT NULL, client_ip VARCHAR(45), user_agent VARCHAR(500))"
        ))

    from app.db.session import configure, init_db

    engine = configure(url)
    init_db(engine)

    names = set(inspect(engine).get_table_names())
    assert "users" in names
    assert "api_clients" in names

    # pre-existing row preserved
    from app.db.session import get_session_factory
    from app.db.repositories import Repositories

    r = Repositories(get_session_factory()())
    app = r.applications.require("legacy")
    assert app.name == "Legacy App"
    r.close()
