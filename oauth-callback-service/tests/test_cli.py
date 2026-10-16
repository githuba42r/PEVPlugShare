"""oauthctl CLI tests via typer's CliRunner (same DB path the service uses)."""
import json

import pytest
from typer.testing import CliRunner

from app.cli import cli


@pytest.fixture()
def runner(db_path, monkeypatch):
    # Point settings at the temp DB before any command runs configure()
    from app.config import settings

    monkeypatch.setattr(settings, "database_url", db_path)
    return CliRunner()


def test_app_lifecycle(runner):
    res = runner.invoke(
        cli,
        [
            "app", "add", "demo", "Demo App", "demo.example.com,localhost:8000",
            "demo://oauth/callback",
        ],
    )
    assert res.exit_code == 0, res.output
    assert "shared secret:" in res.output

    res = runner.invoke(cli, ["app", "list"])
    assert res.exit_code == 0
    assert "demo" in res.output

    res = runner.invoke(cli, ["app", "show", "demo"])
    assert res.exit_code == 0
    assert "demo.example.com" in res.output

    res = runner.invoke(cli, ["app", "rotate-secret", "demo"])
    assert res.exit_code == 0
    assert "shared secret" in res.output.lower()

    res = runner.invoke(cli, ["app", "disable", "demo"])
    assert res.exit_code == 0

    res = runner.invoke(cli, ["app", "enable", "demo"])
    assert res.exit_code == 0

    res = runner.invoke(cli, ["app", "delete", "--force", "demo"])
    assert res.exit_code == 0

    res = runner.invoke(cli, ["app", "show", "demo"])
    assert res.exit_code == 3  # not found


def test_user_lifecycle(runner):
    res = runner.invoke(
        cli, ["user", "add", "alice", "--role", "admin", "--password", "hunter2secret"]
    )
    assert res.exit_code == 0, res.output

    res = runner.invoke(cli, ["user", "list"])
    assert "alice" in res.output
    assert "admin" in res.output

    res = runner.invoke(cli, ["user", "passwd", "alice", "--password", "newpassword1"])
    assert res.exit_code == 0

    res = runner.invoke(cli, ["user", "role", "alice", "operator"])
    assert res.exit_code == 0

    res = runner.invoke(cli, ["user", "disable", "alice"])
    assert res.exit_code == 0

    res = runner.invoke(cli, ["user", "enable", "alice"])
    assert res.exit_code == 0

    res = runner.invoke(cli, ["user", "rm", "--force", "alice"])
    assert res.exit_code == 0


def test_client_and_token(runner):
    res = runner.invoke(
        cli, ["client", "create", "deploy-bot", "apps:read,apps:write"]
    )
    assert res.exit_code == 0, res.output
    assert "client secret:" in res.output

    # unknown scope rejected
    res = runner.invoke(cli, ["client", "create", "bad", "not:a:scope"])
    assert res.exit_code != 0

    res = runner.invoke(cli, ["client", "list"])
    assert "deploy-bot" in res.output

    # extract client_id from JSON output
    res = runner.invoke(cli, ["--json", "client", "list"])
    clients = json.loads(res.output)
    cid = clients[0]["client_id"]

    res = runner.invoke(cli, ["token", "mint", cid])
    assert res.exit_code == 0, res.output
    assert "access token" in res.output.lower()

    res = runner.invoke(cli, ["token", "list", "--client-id", cid])
    assert res.exit_code == 0

    res = runner.invoke(cli, ["client", "revoke", cid])
    assert res.exit_code == 0

    res = runner.invoke(cli, ["client", "rm", "--force", cid])
    assert res.exit_code == 0


def test_json_output(runner):
    runner.invoke(cli, ["app", "add", "j", "J", "j.com", "j://k"])
    res = runner.invoke(cli, ["--json", "app", "list"])
    data = json.loads(res.output)
    assert data[0]["app_id"] == "j"


def test_audit_tail(runner):
    runner.invoke(cli, ["app", "add", "a", "A", "a.com", "a://b"])
    res = runner.invoke(cli, ["audit", "tail"])
    assert res.exit_code == 0
    assert "app.create" in res.output


def test_db_upgrade(runner):
    res = runner.invoke(cli, ["db", "upgrade"])
    assert res.exit_code == 0
    assert "schema up to date" in res.output
