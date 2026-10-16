"""client_credentials token endpoint + /api/v1 management API tests."""
import base64


def _auth(token):
    return {"Authorization": f"Bearer {token}"}


def test_token_wrong_secret(api_client, db_path):
    from app.db.session import get_session_factory
    from app.db.repositories import Repositories

    r = Repositories(get_session_factory()())
    client, _secret = r.api_clients.create("bot", "apps:read")
    r.close()

    res = api_client.post(
        "/oauth/token",
        data={
            "grant_type": "client_credentials",
            "client_id": client.client_id,
            "client_secret": "wrong",
        },
    )
    assert res.status_code == 401
    assert res.json()["error"] == "invalid_client"


def test_token_form_and_basic(api_client, db_path):
    from app.db.session import get_session_factory
    from app.db.repositories import Repositories

    r = Repositories(get_session_factory()())
    client, secret = r.api_clients.create("bot", "apps:read,apps:write")
    r.close()

    res = api_client.post(
        "/oauth/token",
        data={
            "grant_type": "client_credentials",
            "client_id": client.client_id,
            "client_secret": secret,
        },
    )
    assert res.status_code == 200
    body = res.json()
    assert body["token_type"] == "Bearer"
    assert "apps:read" in body["scope"]

    basic = base64.b64encode(f"{client.client_id}:{secret}".encode()).decode()
    res = api_client.post(
        "/oauth/token",
        headers={"Authorization": f"Basic {basic}"},
        data={"grant_type": "client_credentials"},
    )
    assert res.status_code == 200


def test_token_unsupported_grant(api_client, db_path):
    res = api_client.post(
        "/oauth/token",
        data={"grant_type": "authorization_code", "client_id": "x", "client_secret": "y"},
    )
    assert res.status_code == 400
    assert res.json()["error"] == "unsupported_grant_type"


def test_token_invalid_scope(api_client, db_path):
    from app.db.session import get_session_factory
    from app.db.repositories import Repositories

    r = Repositories(get_session_factory()())
    client, secret = r.api_clients.create("bot", "apps:read")
    r.close()

    res = api_client.post(
        "/oauth/token",
        data={
            "grant_type": "client_credentials",
            "client_id": client.client_id,
            "client_secret": secret,
            "scope": "apps:write",
        },
    )
    assert res.status_code == 400
    assert res.json()["error"] == "invalid_scope"


def test_api_requires_bearer(api_client):
    assert api_client.get("/api/v1/applications").status_code == 401


def test_whoami_and_applications_crud(api_client, issued_token):
    _cid, _secret, raw = issued_token
    h = _auth(raw)

    res = api_client.get("/api/v1/whoami", headers=h)
    assert res.status_code == 200
    assert res.json()["client_id"] == "testclient"

    res = api_client.post(
        "/api/v1/applications",
        headers=h,
        json={
            "app_id": "demo",
            "name": "Demo",
            "allowed_domains": "demo.example.com",
            "deep_link_scheme": "demo://oauth/callback",
        },
    )
    assert res.status_code == 201
    assert "shared_secret" in res.json()

    res = api_client.get("/api/v1/applications/demo", headers=h)
    assert res.status_code == 200
    assert res.json()["name"] == "Demo"

    res = api_client.post("/api/v1/applications/demo/rotate-secret", headers=h)
    assert res.status_code == 200
    assert res.json()["shared_secret"]

    res = api_client.patch(
        "/api/v1/applications/demo", headers=h, json={"name": "Demo Renamed"}
    )
    assert res.status_code == 200
    assert res.json()["name"] == "Demo Renamed"

    res = api_client.delete("/api/v1/applications/demo", headers=h)
    assert res.status_code == 200
    assert api_client.get("/api/v1/applications/demo", headers=h).status_code == 404


def test_scope_enforcement(api_client, db_path):
    from app.db.session import get_session_factory
    from app.db.repositories import Repositories

    r = Repositories(get_session_factory()())
    client, _s = r.api_clients.create("readonly", "apps:read")
    _row, raw = r.tokens.issue(client.client_id, "apps:read", 3600)
    r.close()

    h = _auth(raw)
    assert api_client.get("/api/v1/applications", headers=h).status_code == 200
    res = api_client.post(
        "/api/v1/applications",
        headers=h,
        json={
            "app_id": "x",
            "name": "x",
            "allowed_domains": "x",
            "deep_link_scheme": "x://y",
        },
    )
    assert res.status_code == 403
    assert res.json()["error"] == "insufficient_scope"


def test_revoked_token_rejected(api_client, issued_token):
    _cid, _secret, raw = issued_token
    assert api_client.get("/api/v1/whoami", headers=_auth(raw)).status_code == 200

    from app.db.session import get_session_factory
    from app.db.repositories import Repositories

    r = Repositories(get_session_factory()())
    r.tokens.revoke(raw)
    r.close()
    assert api_client.get("/api/v1/whoami", headers=_auth(raw)).status_code == 401


def test_disabled_client_token_rejected(api_client, issued_token):
    cid, _secret, raw = issued_token
    from app.db.session import get_session_factory
    from app.db.repositories import Repositories

    r = Repositories(get_session_factory()())
    r.api_clients.set_active(cid, False)
    r.close()
    assert api_client.get("/api/v1/whoami", headers=_auth(raw)).status_code == 401


def test_users_and_clients_via_api(api_client, issued_token):
    _cid, _secret, raw = issued_token
    h = _auth(raw)

    res = api_client.post(
        "/api/v1/users",
        headers=h,
        json={"username": "manager", "password": "hunter2secret", "role": "admin"},
    )
    assert res.status_code == 201

    res = api_client.get("/api/v1/users", headers=h)
    assert "manager" in [u["username"] for u in res.json()]

    res = api_client.post(
        "/api/v1/clients",
        headers=h,
        json={"name": "second", "scopes": "apps:read"},
    )
    assert res.status_code == 201
    assert res.json()["client_secret"]
    second_id = res.json()["client_id"]

    res = api_client.post(
        f"/api/v1/clients/{second_id}/rotate-secret", headers=h
    )
    assert res.status_code == 200
    assert res.json()["client_secret"]


def test_audit_trail(api_client, issued_token):
    _cid, _secret, raw = issued_token
    h = _auth(raw)
    api_client.post(
        "/api/v1/applications",
        headers=h,
        json={
            "app_id": "audited",
            "name": "Audited",
            "allowed_domains": "a.com",
            "deep_link_scheme": "a://b",
        },
    )
    res = api_client.get("/api/v1/audit", headers=h)
    assert res.status_code == 200
    actions = [r["action"] for r in res.json()]
    assert "app.create" in actions
