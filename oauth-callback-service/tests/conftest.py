"""
Shared fixtures: each test module gets a fresh temp SQLite database wired
through the same configure()/init_db() path the service uses in production.
"""
import os
import tempfile

import pytest


@pytest.fixture(autouse=True)
def _reset_rate_limiters():
    """Rate limiters are module-global; clear them so tests don't trip 429."""
    from app.main import rate_limiter

    def _clear():
        with rate_limiter.lock:
            rate_limiter.requests.clear()

    _clear()
    yield
    _clear()


@pytest.fixture()
def db_path(tmp_path):
    return f"sqlite:///{tmp_path / 'test.db'}"


@pytest.fixture()
def repos(db_path):
    from app.db.session import configure, init_db, get_session_factory
    from app.db.repositories import Repositories

    configure(db_path)
    init_db()
    r = Repositories(get_session_factory()())
    yield r
    r.close()


@pytest.fixture()
def api_client(db_path):
    from fastapi.testclient import TestClient
    from app.main import app
    from app.db.session import configure, init_db, get_session_factory
    from app.db.repositories import Repositories

    configure(db_path)
    init_db()
    with TestClient(app) as client:
        yield client


@pytest.fixture()
def issued_token(api_client, db_path):
    """Create an API client with broad scopes and return (client_id, raw_token)."""
    from app.db.session import get_session_factory
    from app.db.repositories import Repositories

    r = Repositories(get_session_factory()())
    client, secret = r.api_clients.create(
        "testclient",
        "apps:read,apps:write,clients:manage,users:manage,sessions:read,sessions:write,audit:read",
        client_id="testclient",
    )
    _row, raw = r.tokens.issue(client.client_id, client.scopes, 3600)
    client_id = client.client_id
    r.close()
    return client_id, secret, raw


@pytest.fixture()
def web_client(db_path):
    from app.webui import app as webapp
    from app.db.session import configure, init_db, get_session_factory
    from app.db.repositories import Repositories

    configure(db_path)
    init_db()
    r = Repositories(get_session_factory()())
    r.users.create("admin", "hunter2secret", role="admin")
    r.applications.create(
        "pevplugshare", "PEV PlugShare", "pevplugshare.a42r.com", "pevplugshare://oauth/callback"
    )
    r.close()

    # Rate-limit state is module-global; clear it between tests.
    from app.webui import _login_failures

    _login_failures.clear()

    webapp.config["TESTING"] = True
    with webapp.test_client() as client:
        yield client
