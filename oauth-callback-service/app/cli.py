"""
oauthctl — host CLI for the OAuth connector.

Run inside the container (shares the same DB file and dialect):

    sudo podman exec -it oauth oauthctl --help
    python -m app.cli --help

Every subcommand prints human-readable output by default; add --json for
scripting. Secrets are printed exactly once, to stdout.
"""
from __future__ import annotations

import json
import sys
from typing import Optional

import typer

from app.config import settings
from app.db.repositories import (
    Duplicate,
    Invalid,
    NotFound,
    Repositories,
)
from app.db.session import configure, get_session_factory, init_db
from app.api.deps import ALL_SCOPES

cli = typer.Typer(
    name="oauthctl",
    help="Manage the OAuth connector: applications, users, API clients, tokens.",
    no_args_is_help=True,
)
app_cli = typer.Typer(help="Registered client applications ('sites')", no_args_is_help=True)
user_cli = typer.Typer(help="Manager logins for the admin UI", no_args_is_help=True)
client_cli = typer.Typer(help="client_credentials API clients", no_args_is_help=True)
token_cli = typer.Typer(help="Access tokens issued to API clients", no_args_is_help=True)
audit_cli = typer.Typer(help="Audit log", no_args_is_help=True)
db_cli = typer.Typer(help="Database schema", no_args_is_help=True)

cli.add_typer(app_cli, name="app")
cli.add_typer(user_cli, name="user")
cli.add_typer(client_cli, name="client")
cli.add_typer(token_cli, name="token")
cli.add_typer(audit_cli, name="audit")
cli.add_typer(db_cli, name="db")

_state = {"json": False}


@cli.callback()
def _root(
    json_output: bool = typer.Option(False, "--json", help="Machine-readable JSON output"),
):
    """Manage the OAuth connector."""
    _state["json"] = json_output


def _out(data, human: str = None):
    """Emit --json or the human string."""
    if _state["json"]:
        typer.echo(json.dumps(data, indent=2, default=str))
    elif human is not None:
        typer.echo(human)
    else:
        typer.echo(json.dumps(data, indent=2, default=str))


def _repos() -> Repositories:
    configure()
    init_db()  # idempotent; keeps CLI usable on a fresh DB
    return Repositories(get_session_factory()())


def _fail(message: str, code: int = 1):
    typer.echo(f"error: {message}", err=True)
    raise typer.Exit(code)


def _handle_repo_errors(fn, *args, **kwargs):
    try:
        return fn(*args, **kwargs)
    except Duplicate as exc:
        _fail(str(exc), 2)
    except NotFound as exc:
        _fail(str(exc), 3)
    except Invalid as exc:
        _fail(str(exc), 4)


# ------------------------------------------------------------------ app


@app_cli.command("add")
def app_add(
    app_id: str = typer.Argument(...),
    name: str = typer.Argument(...),
    domains: str = typer.Argument(..., help="Comma-separated allowed domains"),
    deep_link: str = typer.Argument(..., help="e.g. myapp://oauth/callback"),
    secret: Optional[str] = typer.Option(None, help="Shared secret (generated if omitted)"),
):
    """Register a new application (site)."""
    repos = _repos()
    try:
        app, raw_secret = _handle_repo_errors(
            repos.applications.create,
            app_id=app_id, name=name, allowed_domains=domains,
            deep_link_scheme=deep_link, shared_secret=secret,
        )
        repos.audit.record(actor="cli", action="app.create", target=app_id)
        _out(
            {"app_id": app.app_id, "shared_secret": raw_secret},
            f"created application '{app.app_id}'\nshared secret: {raw_secret}",
        )
    finally:
        repos.close()


@app_cli.command("list")
def app_list():
    """List registered applications."""
    repos = _repos()
    try:
        apps = repos.applications.list()
        _out(
            [
                {
                    "app_id": a.app_id, "name": a.name, "is_active": a.is_active,
                    "allowed_domains": a.allowed_domains, "deep_link_scheme": a.deep_link_scheme,
                }
                for a in apps
            ],
            "\n".join(
                f"{a.app_id:<24} {'active' if a.is_active else 'DISABLED':<8} {a.name}"
                for a in apps
            ) or "(no applications)",
        )
    finally:
        repos.close()


@app_cli.command("show")
def app_show(app_id: str = typer.Argument(...)):
    """Show one application, including its shared secret."""
    repos = _repos()
    try:
        a = _handle_repo_errors(repos.applications.require, app_id)
        _out(
            {
                "app_id": a.app_id, "name": a.name, "allowed_domains": a.allowed_domains,
                "deep_link_scheme": a.deep_link_scheme, "shared_secret": a.shared_secret,
                "is_active": a.is_active,
            },
            f"app_id:          {a.app_id}\n"
            f"name:            {a.name}\n"
            f"allowed_domains: {a.allowed_domains}\n"
            f"deep_link:       {a.deep_link_scheme}\n"
            f"shared_secret:   {a.shared_secret}\n"
            f"is_active:       {a.is_active}",
        )
    finally:
        repos.close()


@app_cli.command("update-domains")
def app_update_domains(app_id: str, domains: str):
    """Replace the allowed-domains list."""
    repos = _repos()
    try:
        a = _handle_repo_errors(repos.applications.update, app_id, allowed_domains=domains)
        repos.audit.record(actor="cli", action="app.update_domains", target=app_id)
        _out({"app_id": a.app_id, "allowed_domains": a.allowed_domains},
             f"updated {a.app_id}: {a.allowed_domains}")
    finally:
        repos.close()


@app_cli.command("rotate-secret")
def app_rotate_secret(app_id: str):
    """Generate a new shared secret (printed once)."""
    repos = _repos()
    try:
        secret = _handle_repo_errors(repos.applications.rotate_secret, app_id)
        repos.audit.record(actor="cli", action="app.rotate_secret", target=app_id)
        _out({"app_id": app_id, "shared_secret": secret},
             f"new shared secret for {app_id}:\n{secret}")
    finally:
        repos.close()


@app_cli.command("enable")
def app_enable(app_id: str):
    repos = _repos()
    try:
        _handle_repo_errors(repos.applications.set_active, app_id, True)
        repos.audit.record(actor="cli", action="app.enable", target=app_id)
        _out({"app_id": app_id, "is_active": True}, f"enabled {app_id}")
    finally:
        repos.close()


@app_cli.command("disable")
def app_disable(app_id: str):
    repos = _repos()
    try:
        _handle_repo_errors(repos.applications.set_active, app_id, False)
        repos.audit.record(actor="cli", action="app.disable", target=app_id)
        _out({"app_id": app_id, "is_active": False}, f"disabled {app_id}")
    finally:
        repos.close()


@app_cli.command("delete")
def app_delete(
    app_id: str,
    force: bool = typer.Option(False, "--force", "-f", help="Skip confirmation"),
):
    """Delete an application."""
    if not force and not typer.confirm(f"Delete application '{app_id}'?"):
        raise typer.Exit(0)
    repos = _repos()
    try:
        _handle_repo_errors(repos.applications.delete, app_id)
        repos.audit.record(actor="cli", action="app.delete", target=app_id)
        _out({"deleted": app_id}, f"deleted {app_id}")
    finally:
        repos.close()


# ------------------------------------------------------------------ user


@user_cli.command("add")
def user_add(
    username: str = typer.Argument(...),
    role: str = typer.Option("operator", help="admin | operator"),
    password: Optional[str] = typer.Option(
        None, help="Password (prompted if omitted)"
    ),
):
    """Create a manager login."""
    if password is None:
        password = typer.prompt("Password", hide_input=True, confirmation_prompt=True)
    repos = _repos()
    try:
        user = _handle_repo_errors(repos.users.create, username, password, role)
        repos.audit.record(actor="cli", action="user.create", target=username, detail=f"role={role}")
        _out({"username": user.username, "role": user.role},
             f"created user '{user.username}' (role: {user.role})")
    finally:
        repos.close()


@user_cli.command("list")
def user_list():
    repos = _repos()
    try:
        users = repos.users.list()
        _out(
            [
                {
                    "username": u.username, "role": u.role, "is_active": u.is_active,
                    "last_login": u.last_login.isoformat() if u.last_login else None,
                }
                for u in users
            ],
            "\n".join(
                f"{u.username:<24} {'admin' if u.role == 'admin' else 'operator':<9} "
                f"{'active' if u.is_active else 'DISABLED':<8} last_login={u.last_login or 'never'}"
                for u in users
            ) or "(no users)",
        )
    finally:
        repos.close()


@user_cli.command("passwd")
def user_passwd(
    username: str,
    password: Optional[str] = typer.Option(None, help="New password (prompted if omitted)"),
):
    """Change a user's password."""
    if password is None:
        password = typer.prompt("New password", hide_input=True, confirmation_prompt=True)
    repos = _repos()
    try:
        _handle_repo_errors(repos.users.set_password, username, password)
        repos.audit.record(actor="cli", action="user.passwd", target=username)
        _out({"username": username}, f"password updated for '{username}'")
    finally:
        repos.close()


@user_cli.command("role")
def user_role(username: str, role: str = typer.Argument(..., help="admin | operator")):
    repos = _repos()
    try:
        _handle_repo_errors(repos.users.set_role, username, role)
        repos.audit.record(actor="cli", action="user.role", target=username, detail=role)
        _out({"username": username, "role": role}, f"'{username}' is now {role}")
    finally:
        repos.close()


@user_cli.command("enable")
def user_enable(username: str):
    repos = _repos()
    try:
        _handle_repo_errors(repos.users.set_active, username, True)
        _out({"username": username, "is_active": True}, f"enabled '{username}'")
    finally:
        repos.close()


@user_cli.command("disable")
def user_disable(username: str):
    repos = _repos()
    try:
        _handle_repo_errors(repos.users.set_active, username, False)
        repos.web_sessions.delete_for_user(username)
        _out({"username": username, "is_active": False}, f"disabled '{username}' (sessions revoked)")
    finally:
        repos.close()


@user_cli.command("rm")
def user_rm(
    username: str,
    force: bool = typer.Option(False, "--force", "-f"),
):
    if not force and not typer.confirm(f"Delete user '{username}'?"):
        raise typer.Exit(0)
    repos = _repos()
    try:
        _handle_repo_errors(repos.users.delete, username)
        repos.audit.record(actor="cli", action="user.delete", target=username)
        _out({"deleted": username}, f"deleted user '{username}'")
    finally:
        repos.close()


# ------------------------------------------------------------------ client


@client_cli.command("create")
def client_create(
    name: str = typer.Argument(...),
    scopes: str = typer.Argument(..., help=f"Comma-separated. One or more of: {','.join(ALL_SCOPES)}"),
    client_id: Optional[str] = typer.Option(None, help="Client ID (auto-generated if omitted)"),
):
    """Create a client_credentials API client (secret printed once)."""
    requested = [s.strip() for s in scopes.split(",") if s.strip()]
    unknown = [s for s in requested if s not in ALL_SCOPES]
    if unknown:
        _fail(f"unknown scope(s): {', '.join(unknown)}; valid: {', '.join(ALL_SCOPES)}")
    repos = _repos()
    try:
        client, secret = _handle_repo_errors(
            repos.api_clients.create, name=name, scopes=",".join(requested), client_id=client_id
        )
        repos.audit.record(actor="cli", action="client.create", target=client.client_id)
        _out(
            {"client_id": client.client_id, "client_secret": secret, "scopes": client.scopes},
            f"created api client '{client.client_id}'\nclient secret: {secret}",
        )
    finally:
        repos.close()


@client_cli.command("list")
def client_list():
    repos = _repos()
    try:
        clients = repos.api_clients.list()
        _out(
            [
                {"client_id": c.client_id, "name": c.name, "scopes": c.scopes, "is_active": c.is_active}
                for c in clients
            ],
            "\n".join(
                f"{c.client_id:<28} {'active' if c.is_active else 'DISABLED':<8} {c.scopes:<40} {c.name}"
                for c in clients
            ) or "(no api clients)",
        )
    finally:
        repos.close()


@client_cli.command("rotate-secret")
def client_rotate(client_id: str):
    """Rotate a client's secret and revoke all its tokens (new secret printed once)."""
    repos = _repos()
    try:
        secret = _handle_repo_errors(repos.api_clients.rotate_secret, client_id)
        revoked = repos.tokens.revoke_all_for_client(client_id)
        repos.audit.record(actor="cli", action="client.rotate_secret", target=client_id)
        _out(
            {"client_id": client_id, "client_secret": secret, "revoked_tokens": revoked},
            f"new secret for '{client_id}' (revoked {revoked} tokens):\n{secret}",
        )
    finally:
        repos.close()


@client_cli.command("disable")
def client_disable(client_id: str):
    repos = _repos()
    try:
        _handle_repo_errors(repos.api_clients.set_active, client_id, False)
        repos.audit.record(actor="cli", action="client.disable", target=client_id)
        _out({"client_id": client_id, "is_active": False}, f"disabled '{client_id}'")
    finally:
        repos.close()


@client_cli.command("enable")
def client_enable(client_id: str):
    repos = _repos()
    try:
        _handle_repo_errors(repos.api_clients.set_active, client_id, True)
        _out({"client_id": client_id, "is_active": True}, f"enabled '{client_id}'")
    finally:
        repos.close()


@client_cli.command("revoke")
def client_revoke(client_id: str):
    """Revoke every outstanding token for a client."""
    repos = _repos()
    try:
        _handle_repo_errors(repos.api_clients.require, client_id)
        count = repos.tokens.revoke_all_for_client(client_id)
        repos.audit.record(actor="cli", action="client.revoke_tokens", target=client_id)
        _out({"client_id": client_id, "revoked": count}, f"revoked {count} token(s) for '{client_id}'")
    finally:
        repos.close()


@client_cli.command("rm")
def client_rm(client_id: str, force: bool = typer.Option(False, "--force", "-f")):
    if not force and not typer.confirm(f"Delete api client '{client_id}'?"):
        raise typer.Exit(0)
    repos = _repos()
    try:
        _handle_repo_errors(repos.api_clients.delete, client_id)
        repos.audit.record(actor="cli", action="client.delete", target=client_id)
        _out({"deleted": client_id}, f"deleted api client '{client_id}'")
    finally:
        repos.close()


# ------------------------------------------------------------------ token


@token_cli.command("mint")
def token_mint(
    client_id: str = typer.Argument(...),
    scope: str = typer.Option("", help="Space/comma separated (default: all client scopes)"),
    ttl: int = typer.Option(None, help="TTL in seconds (default: settings.access_token_ttl_seconds)"),
):
    """Mint an access token for a client (printed once)."""
    repos = _repos()
    try:
        client = _handle_repo_errors(repos.api_clients.require, client_id)
        if not client.is_active:
            _fail(f"client '{client_id}' is disabled")
        granted = [s.strip() for s in scope.replace(",", " ").split() if s.strip()] or client.get_scopes_list()
        bad = [s for s in granted if s not in client.get_scopes_list()]
        if bad:
            _fail(f"client '{client_id}' does not hold scope(s): {', '.join(bad)}")
        row, raw = repos.tokens.issue(
            client_id=client_id,
            scopes=",".join(granted),
            ttl_seconds=ttl or settings.access_token_ttl_seconds,
        )
        repos.audit.record(actor="cli", action="token.mint", target=client_id, detail=",".join(granted))
        _out(
            {
                "client_id": client_id,
                "access_token": raw,
                "scopes": granted,
                "expires_at": row.expires_at.isoformat(),
            },
            f"access token for '{client_id}' (scopes: {', '.join(granted)}, expires {row.expires_at.isoformat()}):\n{raw}",
        )
    finally:
        repos.close()


@token_cli.command("list")
def token_list(client_id: Optional[str] = typer.Option(None, help="Filter by client")):
    repos = _repos()
    try:
        rows = (
            repos.tokens.list_for_client(client_id)
            if client_id
            else repos.tokens.list_recent(200)
        )
        _out(
            [
                {
                    "client_id": t.client_id,
                    "scopes": t.scopes,
                    "created_at": t.created_at.isoformat() if t.created_at else None,
                    "expires_at": t.expires_at.isoformat() if t.expires_at else None,
                    "revoked": t.revoked_at is not None,
                }
                for t in rows
            ],
            "\n".join(
                f"{t.client_id:<28} {'REVOKED' if t.revoked_at else 'active':<8} "
                f"expires={t.expires_at.isoformat()} scopes={t.scopes}"
                for t in rows
            ) or "(no tokens)",
        )
    finally:
        repos.close()


@token_cli.command("revoke")
def token_revoke(
    token: str = typer.Argument(..., help="The raw access token value"),
):
    repos = _repos()
    try:
        ok = repos.tokens.revoke(token)
        if not ok:
            _fail("token not found or already revoked", 3)
        repos.audit.record(actor="cli", action="token.revoke")
        _out({"revoked": True}, "token revoked")
    finally:
        repos.close()


# ------------------------------------------------------------------ audit


@audit_cli.command("tail")
def audit_tail(
    limit: int = typer.Option(30, help="Number of entries"),
    actor: Optional[str] = typer.Option(None, help="Filter by actor"),
):
    repos = _repos()
    try:
        rows = repos.audit.list(limit=limit, actor=actor)
        _out(
            [
                {
                    "ts": r.ts.isoformat() if r.ts else None, "actor": r.actor,
                    "action": r.action, "target": r.target, "ip": r.ip, "detail": r.detail,
                }
                for r in rows
            ],
            "\n".join(
                f"{(r.ts.isoformat() if r.ts else ''):<20} {r.actor:<20} {r.action:<22} {r.target or ''} {r.detail or ''}"
                for r in rows
            ) or "(audit log empty)",
        )
    finally:
        repos.close()


# ------------------------------------------------------------------ db


@db_cli.command("upgrade")
def db_upgrade():
    """Bring the schema up to date (same code path as service startup)."""
    configure()
    init_db()
    _out({"status": "ok"}, "schema up to date")


def main():
    cli()


if __name__ == "__main__":
    main()
