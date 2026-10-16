"""Admin web UI in-app auth tests (login, CSRF, guard, management)."""


def _login(client, username="admin", password="hunter2secret"):
    return client.post(
        "/admin/login", data={"username": username, "password": password}
    )


def _csrf(client):
    return client.get("/admin/api/me").get_json()["csrf_token"]


def test_unauthenticated_redirects_to_login(web_client):
    res = web_client.get("/admin/")
    assert res.status_code == 302
    assert "/admin/login" in res.headers["Location"]


def test_api_requires_login(web_client):
    assert web_client.get("/admin/api/applications").status_code == 401
    assert web_client.get("/admin/api/me").status_code == 401


def test_bad_login(web_client):
    res = _login(web_client, "admin", "wrong-password")
    assert res.status_code == 200
    assert "Invalid" in res.get_data(as_text=True)
    # cookie must not be set
    assert web_client.get("/admin/api/me").status_code == 401


def test_login_rate_limit(web_client):
    for _ in range(5):
        _login(web_client, "admin", "wrong-password")
    res = _login(web_client, "admin", "hunter2secret")
    assert res.status_code == 429


def test_login_success_and_me(web_client):
    res = _login(web_client)
    assert res.status_code == 302
    me = web_client.get("/admin/api/me").get_json()
    assert me["username"] == "admin"
    assert me["role"] == "admin"
    assert me["csrf_token"]


def test_csrf_required_for_mutations(web_client):
    _login(web_client)
    res = web_client.post("/admin/api/applications/pevplugshare/toggle")
    assert res.status_code == 403

    res = web_client.post(
        "/admin/api/applications/pevplugshare/toggle",
        headers={"X-CSRF-Token": _csrf(web_client)},
    )
    assert res.status_code == 200
    assert res.get_json()["is_active"] is False


def test_applications_crud(web_client):
    _login(web_client)
    csrf = _csrf(web_client)
    h = {"X-CSRF-Token": csrf}

    res = web_client.post(
        "/admin/api/applications",
        headers=h,
        json={
            "app_id": "newapp",
            "name": "New App",
            "allowed_domains": "new.example.com",
            "deep_link_scheme": "new://cb",
        },
    )
    assert res.status_code == 201
    assert res.get_json()["shared_secret"]

    apps = web_client.get("/admin/api/applications").get_json()
    assert "newapp" in [a["app_id"] for a in apps]

    res = web_client.post("/admin/api/applications/newapp/regenerate-secret", headers=h)
    assert res.status_code == 200

    res = web_client.delete("/admin/api/applications/newapp", headers=h)
    assert res.status_code == 200


def test_user_management_admin_only(web_client):
    _login(web_client)
    csrf = _csrf(web_client)
    h = {"X-CSRF-Token": csrf}

    # create an operator
    res = web_client.post(
        "/admin/api/users",
        headers=h,
        json={"username": "ops", "password": "hunter2secret", "role": "operator"},
    )
    assert res.status_code == 201

    # operator cannot create users
    web_client.post("/admin/logout")
    _login(web_client, "ops", "hunter2secret")
    res = web_client.post(
        "/admin/api/users",
        headers={"X-CSRF-Token": _csrf(web_client)},
        json={"username": "ops2", "password": "hunter2secret"},
    )
    assert res.status_code == 403

    # operator cannot delete self-protected things via admin endpoints either
    res = web_client.get("/admin/api/users")
    assert res.status_code == 200


def test_logout_invalidates_session(web_client):
    _login(web_client)
    assert web_client.get("/admin/api/me").status_code == 200
    web_client.post("/admin/logout")
    assert web_client.get("/admin/api/me").status_code == 401


def test_clients_and_sessions_endpoints(web_client):
    _login(web_client)
    csrf = _csrf(web_client)
    h = {"X-CSRF-Token": csrf}

    res = web_client.post(
        "/admin/api/clients",
        headers=h,
        json={"name": "ci", "scopes": "apps:read,apps:write"},
    )
    assert res.status_code == 201
    assert res.get_json()["client_secret"]

    clients = web_client.get("/admin/api/clients").get_json()
    assert clients

    res = web_client.post(
        "/admin/api/clients/" + clients[0]["client_id"] + "/revoke-tokens", headers=h
    )
    assert res.status_code == 200

    assert web_client.get("/admin/api/sessions").status_code == 200
    assert web_client.get("/admin/api/audit").status_code == 200
