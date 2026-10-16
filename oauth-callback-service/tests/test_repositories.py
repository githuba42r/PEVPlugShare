"""Repository-level tests (SQL implementation of the pluggable layer)."""
import pytest

from app.db.repositories import Duplicate, Invalid, NotFound


def test_application_create_and_get(repos):
    app, secret = repos.applications.create(
        "demo", "Demo", "demo.example.com", "demo://oauth/callback"
    )
    assert app.app_id == "demo"
    assert secret and len(secret) > 20
    fetched = repos.applications.require("demo")
    assert fetched.allowed_domains == "demo.example.com"
    assert fetched.is_domain_allowed("demo.example.com")
    assert not fetched.is_domain_allowed("evil.com")


def test_application_duplicate(repos):
    repos.applications.create("demo", "Demo", "d.com", "d://cb")
    with pytest.raises(Duplicate):
        repos.applications.create("demo", "Demo2", "d2.com", "d2://cb")


def test_application_rotate_secret(repos):
    repos.applications.create("demo", "Demo", "d.com", "d://cb")
    old = repos.applications.require("demo").shared_secret
    new = repos.applications.rotate_secret("demo")
    assert new != old
    assert repos.applications.require("demo").shared_secret == new


def test_application_not_found(repos):
    with pytest.raises(NotFound):
        repos.applications.require("nope")
    with pytest.raises(NotFound):
        repos.applications.rotate_secret("nope")


def test_user_authenticate(repos):
    repos.users.create("alice", "correct-horse", role="admin")
    assert repos.users.authenticate("alice", "correct-horse") is not None
    assert repos.users.authenticate("alice", "wrong") is None
    assert repos.users.authenticate("nobody", "correct-horse") is None


def test_user_disable_blocks_auth(repos):
    repos.users.create("bob", "hunter2secret")
    repos.users.set_active("bob", False)
    assert repos.users.authenticate("bob", "hunter2secret") is None


def test_user_role_validation(repos):
    with pytest.raises(Invalid):
        repos.users.create("x", "hunter2secret", role="superuser")


def test_user_delete_removes_sessions(repos):
    repos.users.create("carol", "hunter2secret")
    _row, raw = repos.web_sessions.create("carol", 60)
    assert repos.web_sessions.get_valid(raw) is not None
    repos.users.delete("carol")
    assert repos.web_sessions.get_valid(raw) is None


def test_web_session_valid_and_expiry(repos):
    repos.users.create("dave", "hunter2secret")
    row, raw = repos.web_sessions.create("dave", ttl_minutes=60)
    assert repos.web_sessions.get_valid(raw) is not None
    # force-expire
    from datetime import datetime, timedelta

    row.expires_at = datetime.utcnow() - timedelta(minutes=1)
    repos.session.commit()
    assert repos.web_sessions.get_valid(raw) is None


def test_api_client_authenticate(repos):
    client, secret = repos.api_clients.create("bot", "apps:read")
    assert repos.api_clients.authenticate(client.client_id, secret) is not None
    assert repos.api_clients.authenticate(client.client_id, "wrong") is None


def test_token_lifecycle(repos):
    client, _secret = repos.api_clients.create("bot", "apps:read")
    row, raw = repos.tokens.issue(client.client_id, "apps:read", ttl_seconds=3600)
    assert repos.tokens.get_valid(raw) is not None
    assert repos.tokens.revoke(raw) is True
    assert repos.tokens.get_valid(raw) is None
    assert repos.tokens.revoke(raw) is False


def test_revoke_all_for_client(repos):
    client, _secret = repos.api_clients.create("bot", "apps:read")
    for _ in range(3):
        _row, raw = repos.tokens.issue(client.client_id, "apps:read", 3600)
        assert repos.tokens.get_valid(raw) is not None
    assert repos.tokens.revoke_all_for_client(client.client_id) == 3


def test_audit_record_and_list(repos):
    repos.audit.record(actor="tester", action="unit.test", target="something")
    rows = repos.audit.list(limit=10)
    assert any(r.action == "unit.test" for r in rows)
