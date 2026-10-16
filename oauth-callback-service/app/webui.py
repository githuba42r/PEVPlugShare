"""
Web interface for OAuth application management (in-app auth).

Flask app mounted at /admin (behind nginx). Managers log in with a username
and password (Argon2id-hashed, stored in the users table); the session is an
opaque token in an HttpOnly cookie, validated against web_sessions and
protected with a per-session CSRF token on every mutating call.
"""
from __future__ import annotations

import json
import logging
import os
import secrets
import time
from collections import defaultdict
from datetime import datetime
from typing import Optional

from flask import (
    Flask,
    Blueprint,
    g,
    jsonify,
    make_response,
    redirect,
    render_template,
    request,
    url_for,
)
from werkzeug.middleware.proxy_fix import ProxyFix

from app.bootstrap import bootstrap_admin
from app.config import settings
from app.db.repositories import (
    Duplicate,
    Invalid,
    NotFound,
    Repositories,
)
from app.db.session import get_session_factory

logger = logging.getLogger(__name__)

app = Flask(__name__, static_url_path='/admin/static')
# Trust exactly one proxy hop (nginx) for scheme/host/client-ip
app.wsgi_app = ProxyFix(app.wsgi_app, x_for=1, x_proto=1, x_host=1, x_prefix=1)

admin_bp = Blueprint('admin', __name__, url_prefix='/admin')

SESSION_COOKIE = "oauth_admin_session"
LOGIN_MAX_FAILURES = 5
LOGIN_WINDOW_SECONDS = 300

# In-memory failed-login tracking (per process; good enough for a single
# container running one webui worker)
_login_failures = defaultdict(list)  # key -> [failure timestamps]


def _client_ip() -> str:
    return request.headers.get("X-Real-IP", request.remote_addr or "unknown")


def _is_https() -> bool:
    return request.headers.get("X-Forwarded-Proto", request.scheme) == "https"


def _repos() -> Repositories:
    return Repositories(get_session_factory()())


def _session_cookie(response, raw_token: str, max_age: int):
    response.set_cookie(
        SESSION_COOKIE,
        raw_token,
        max_age=max_age,
        httponly=True,
        samesite="Lax",
        secure=_is_https(),
        path="/admin",
    )
    return response


def _login_blocked(key: str) -> bool:
    now = time.time()
    window = [t for t in _login_failures[key] if now - t < LOGIN_WINDOW_SECONDS]
    _login_failures[key] = window
    return len(window) >= LOGIN_MAX_FAILURES


def _record_login_failure(key: str) -> None:
    _login_failures[key].append(time.time())


# ---------------------------------------------------------------- auth guard


@app.before_request
def _require_auth():
    if request.endpoint == "static":
        return None
    path = request.path
    if path in ("/admin/login", "/admin/logout"):
        return None

    raw_token = request.cookies.get(SESSION_COOKIE)
    g.user = None
    g.csrf_token = None
    if raw_token:
        repos = _repos()
        try:
            row = repos.web_sessions.get_valid(raw_token)
            if row is not None:
                user = repos.users.get(row.username)
                if user is not None and user.is_active:
                    g.user = user
                    g.csrf_token = row.csrf_token
                    g.raw_session_token = raw_token
        finally:
            repos.close()

    if g.user is None:
        if path.startswith("/admin/api/"):
            return jsonify({"error": "unauthorized", "message": "login required"}), 401
        return redirect(url_for("login", next=path))

    # CSRF: every state-changing call must echo the session CSRF token
    if request.method in ("POST", "PUT", "PATCH", "DELETE"):
        presented = request.headers.get("X-CSRF-Token", "")
        if not presented or not secrets.compare_digest(presented, g.csrf_token or ""):
            if path.startswith("/admin/api/"):
                return jsonify({"error": "csrf", "message": "missing or invalid CSRF token"}), 403
            return jsonify({"error": "csrf", "message": "missing or invalid CSRF token"}), 403
    return None


def _audit(action: str, target: Optional[str] = None, detail: Optional[str] = None):
    repos = _repos()
    try:
        repos.audit.record(
            actor=g.user.username if g.user else "anonymous",
            action=action,
            target=target,
            ip=_client_ip(),
            detail=detail,
        )
    finally:
        repos.close()


def _require_admin():
    if g.user is None or g.user.role != "admin":
        return jsonify({"error": "forbidden", "message": "admin role required"}), 403
    return None


# ---------------------------------------------------------------- login


@app.route('/admin/login', methods=['GET', 'POST'])
def login():
    error = None
    if request.method == 'POST':
        username = (request.form.get("username") or "").strip()
        password = request.form.get("password") or ""
        key = f"{_client_ip()}|{username}"

        if _login_blocked(key):
            error = "Too many failed attempts. Try again later."
            return render_template("login.html", error=error, username=username), 429

        repos = _repos()
        try:
            user = repos.users.authenticate(username, password)
            if user is None:
                _record_login_failure(key)
                logger.warning("Failed login for '%s' from %s", username, _client_ip())
                repos.audit.record(
                    actor=username or "anonymous",
                    action="login.failed",
                    ip=_client_ip(),
                )
                error = "Invalid username or password."
            else:
                _login_failures.pop(key, None)
                row, raw_token = repos.web_sessions.create(
                    username=user.username,
                    ttl_minutes=settings.admin_session_ttl_minutes,
                    ip=_client_ip(),
                    user_agent=request.headers.get("User-Agent", "")[:500],
                )
                repos.users.touch_login(user.username)
                repos.audit.record(
                    actor=user.username, action="login.success", ip=_client_ip()
                )
                response = make_response(
                    redirect(url_for("admin.index"))
                )
                return _session_cookie(
                    response, raw_token, settings.admin_session_ttl_minutes * 60
                )
        finally:
            repos.close()

    status = 429 if error and "Too many" in error else 200
    return render_template("login.html", error=error, username=""), status


@app.route('/admin/logout', methods=['POST'])
def logout():
    raw_token = request.cookies.get(SESSION_COOKIE)
    if raw_token:
        repos = _repos()
        try:
            repos.web_sessions.delete(raw_token)
            user = getattr(g, "user", None)
            if user:
                repos.audit.record(
                    actor=user.username, action="logout", ip=_client_ip()
                )
        finally:
            repos.close()
    response = make_response(redirect(url_for("login")))
    response.delete_cookie(SESSION_COOKIE, path="/admin")
    return response


@app.route('/')
def root():
    return redirect('/admin/')


# ---------------------------------------------------------------- pages


@admin_bp.route('/')
def index():
    return render_template(
        'index.html',
        username=g.user.username,
        role=g.user.role,
    )


# ---------------------------------------------------------------- session info


@admin_bp.route('/api/me')
def me():
    return jsonify({
        'username': g.user.username,
        'role': g.user.role,
        'csrf_token': g.csrf_token,
        'last_login': g.user.last_login.isoformat() if g.user.last_login else None,
    })


# ---------------------------------------------------------------- applications


def _app_json(app_obj, include_secret=False):
    data = {
        'app_id': app_obj.app_id,
        'name': app_obj.name,
        'allowed_domains': app_obj.allowed_domains,
        'deep_link_scheme': app_obj.deep_link_scheme,
        'is_active': app_obj.is_active,
        'created_at': app_obj.created_at.isoformat() if app_obj.created_at else None,
        'updated_at': app_obj.updated_at.isoformat() if app_obj.updated_at else None,
    }
    if include_secret:
        data['shared_secret'] = app_obj.shared_secret
    else:
        data['secret_preview'] = app_obj.shared_secret[:20] + '...' if app_obj.shared_secret else ''
    return data


@admin_bp.route('/api/applications', methods=['GET'])
def get_applications():
    repos = _repos()
    try:
        return jsonify([_app_json(a) for a in repos.applications.list()])
    finally:
        repos.close()


@admin_bp.route('/api/applications/<app_id>', methods=['GET'])
def get_application(app_id):
    repos = _repos()
    try:
        app_obj = repos.applications.get(app_id)
        if not app_obj:
            return jsonify({'error': 'Application not found'}), 404
        return jsonify(_app_json(app_obj, include_secret=True))
    finally:
        repos.close()


@admin_bp.route('/api/applications', methods=['POST'])
def create_application():
    data = request.json or {}
    for field in ('app_id', 'name', 'allowed_domains', 'deep_link_scheme'):
        if not data.get(field):
            return jsonify({'error': f'Missing required field: {field}'}), 400
    repos = _repos()
    try:
        app_obj, secret = repos.applications.create(
            app_id=data['app_id'],
            name=data['name'],
            allowed_domains=data['allowed_domains'],
            deep_link_scheme=data['deep_link_scheme'],
            shared_secret=data.get('shared_secret'),
            is_active=data.get('is_active', True),
        )
        _audit('app.create', app_obj.app_id)
        payload = _app_json(app_obj, include_secret=True)
        payload['shared_secret'] = secret
        return jsonify(payload), 201
    except Duplicate as exc:
        return jsonify({'error': str(exc)}), 409
    except Exception as exc:
        logger.error("Error creating application: %s", exc)
        return jsonify({'error': str(exc)}), 500
    finally:
        repos.close()


@admin_bp.route('/api/applications/<app_id>', methods=['PUT'])
def update_application(app_id):
    data = request.json or {}
    repos = _repos()
    try:
        app_obj = repos.applications.update(app_id, **{
            k: data[k] for k in ('name', 'allowed_domains', 'deep_link_scheme', 'shared_secret', 'is_active')
            if k in data
        })
        _audit('app.update', app_id)
        return jsonify(_app_json(app_obj, include_secret=True))
    except NotFound as exc:
        return jsonify({'error': str(exc)}), 404
    except Exception as exc:
        logger.error("Error updating application: %s", exc)
        return jsonify({'error': str(exc)}), 500
    finally:
        repos.close()


@admin_bp.route('/api/applications/<app_id>', methods=['DELETE'])
def delete_application(app_id):
    repos = _repos()
    try:
        repos.applications.delete(app_id)
        _audit('app.delete', app_id)
        return jsonify({'success': True})
    except NotFound as exc:
        return jsonify({'error': str(exc)}), 404
    except Exception as exc:
        logger.error("Error deleting application: %s", exc)
        return jsonify({'error': str(exc)}), 500
    finally:
        repos.close()


@admin_bp.route('/api/applications/<app_id>/toggle', methods=['POST'])
def toggle_application(app_id):
    repos = _repos()
    try:
        app_obj = repos.applications.require(app_id)
        app_obj = repos.applications.set_active(app_id, not app_obj.is_active)
        _audit('app.toggle', app_id, detail=f"is_active={app_obj.is_active}")
        return jsonify({'app_id': app_id, 'is_active': app_obj.is_active})
    except NotFound as exc:
        return jsonify({'error': str(exc)}), 404
    finally:
        repos.close()


@admin_bp.route('/api/applications/<app_id>/regenerate-secret', methods=['POST'])
def regenerate_secret(app_id):
    repos = _repos()
    try:
        secret = repos.applications.rotate_secret(app_id)
        _audit('app.rotate_secret', app_id)
        return jsonify({'app_id': app_id, 'shared_secret': secret})
    except NotFound as exc:
        return jsonify({'error': str(exc)}), 404
    finally:
        repos.close()


# ---------------------------------------------------------------- users


def _user_json(user):
    return {
        'username': user.username,
        'role': user.role,
        'is_active': user.is_active,
        'created_at': user.created_at.isoformat() if user.created_at else None,
        'last_login': user.last_login.isoformat() if user.last_login else None,
    }


@admin_bp.route('/api/users', methods=['GET'])
def list_users():
    repos = _repos()
    try:
        return jsonify([_user_json(u) for u in repos.users.list()])
    finally:
        repos.close()


@admin_bp.route('/api/users', methods=['POST'])
def create_user():
    denied = _require_admin()
    if denied:
        return denied
    data = request.json or {}
    username = (data.get('username') or '').strip()
    password = data.get('password') or ''
    role = data.get('role') or 'operator'
    if not username or len(password) < 8:
        return jsonify({'error': 'username and password (min 8 chars) required'}), 400
    repos = _repos()
    try:
        user = repos.users.create(username, password, role)
        _audit('user.create', username, detail=f"role={role}")
        return jsonify(_user_json(user)), 201
    except Duplicate as exc:
        return jsonify({'error': str(exc)}), 409
    except Invalid as exc:
        return jsonify({'error': str(exc)}), 400
    finally:
        repos.close()


@admin_bp.route('/api/users/<username>', methods=['PATCH'])
def patch_user(username):
    denied = _require_admin()
    if denied:
        return denied
    data = request.json or {}
    repos = _repos()
    try:
        if 'password' in data:
            repos.users.set_password(username, data['password'])
        if 'role' in data:
            repos.users.set_role(username, data['role'])
        if 'is_active' in data:
            repos.users.set_active(username, bool(data['is_active']))
        user = repos.users.require(username)
        _audit('user.update', username,
               detail=json.dumps({k: v for k, v in data.items() if k != 'password'}))
        return jsonify(_user_json(user))
    except NotFound as exc:
        return jsonify({'error': str(exc)}), 404
    except Invalid as exc:
        return jsonify({'error': str(exc)}), 400
    finally:
        repos.close()


@admin_bp.route('/api/users/<username>', methods=['DELETE'])
def delete_user(username):
    denied = _require_admin()
    if denied:
        return denied
    if username == g.user.username:
        return jsonify({'error': 'cannot delete your own account'}), 400
    repos = _repos()
    try:
        repos.users.delete(username)
        _audit('user.delete', username)
        return jsonify({'success': True})
    except NotFound as exc:
        return jsonify({'error': str(exc)}), 404
    finally:
        repos.close()


# ---------------------------------------------------------------- api clients


def _client_json(client):
    return {
        'client_id': client.client_id,
        'name': client.name,
        'scopes': client.scopes,
        'is_active': client.is_active,
        'created_at': client.created_at.isoformat() if client.created_at else None,
        'updated_at': client.updated_at.isoformat() if client.updated_at else None,
    }


@admin_bp.route('/api/clients', methods=['GET'])
def list_clients():
    repos = _repos()
    try:
        return jsonify([_client_json(c) for c in repos.api_clients.list()])
    finally:
        repos.close()


@admin_bp.route('/api/clients', methods=['POST'])
def create_client():
    data = request.json or {}
    name = (data.get('name') or '').strip()
    scopes = data.get('scopes') or ''
    if not name or not scopes:
        return jsonify({'error': 'name and scopes required'}), 400
    repos = _repos()
    try:
        client, secret = repos.api_clients.create(
            name=name, scopes=scopes, client_id=data.get('client_id') or None
        )
        _audit('client.create', client.client_id, detail=f"scopes={scopes}")
        payload = _client_json(client)
        payload['client_secret'] = secret  # shown once
        return jsonify(payload), 201
    except Duplicate as exc:
        return jsonify({'error': str(exc)}), 409
    finally:
        repos.close()


@admin_bp.route('/api/clients/<client_id>', methods=['PATCH'])
def patch_client(client_id):
    data = request.json or {}
    repos = _repos()
    try:
        client = repos.api_clients.update(client_id, **{
            k: data[k] for k in ('name', 'scopes', 'is_active') if k in data
        })
        _audit('client.update', client_id)
        return jsonify(_client_json(client))
    except NotFound as exc:
        return jsonify({'error': str(exc)}), 404
    finally:
        repos.close()


@admin_bp.route('/api/clients/<client_id>/rotate-secret', methods=['POST'])
def rotate_client_secret(client_id):
    repos = _repos()
    try:
        secret = repos.api_clients.rotate_secret(client_id)
        revoked = repos.tokens.revoke_all_for_client(client_id)
        _audit('client.rotate_secret', client_id, detail=f"revoked {revoked} tokens")
        return jsonify({'client_id': client_id, 'client_secret': secret})
    except NotFound as exc:
        return jsonify({'error': str(exc)}), 404
    finally:
        repos.close()


@admin_bp.route('/api/clients/<client_id>/revoke-tokens', methods=['POST'])
def revoke_client_tokens(client_id):
    repos = _repos()
    try:
        if repos.api_clients.get(client_id) is None:
            return jsonify({'error': 'client not found'}), 404
        count = repos.tokens.revoke_all_for_client(client_id)
        _audit('client.revoke_tokens', client_id, detail=f"revoked {count}")
        return jsonify({'client_id': client_id, 'revoked': count})
    finally:
        repos.close()


@admin_bp.route('/api/clients/<client_id>', methods=['DELETE'])
def delete_client(client_id):
    repos = _repos()
    try:
        repos.api_clients.delete(client_id)
        _audit('client.delete', client_id)
        return jsonify({'success': True})
    except NotFound as exc:
        return jsonify({'error': str(exc)}), 404
    finally:
        repos.close()


# ---------------------------------------------------------------- sessions


@admin_bp.route('/api/sessions', methods=['GET'])
def get_sessions():
    limit = request.args.get('limit', 50, type=int)
    app_id = request.args.get('app_id')
    repos = _repos()
    try:
        return jsonify([{
            'session_id': s.session_id,
            'app_id': s.app_id,
            'provider': s.provider,
            'created_at': s.created_at.isoformat() if s.created_at else None,
            'expires_at': s.expires_at.isoformat() if s.expires_at else None,
            'used': s.used,
            'client_ip': s.client_ip,
            'is_expired': s.is_expired(),
        } for s in repos.oauth_sessions.list(app_id=app_id, limit=limit)])
    finally:
        repos.close()


@admin_bp.route('/api/sessions', methods=['DELETE'])
def purge_sessions():
    app_id = request.args.get('app_id')
    repos = _repos()
    try:
        count = repos.oauth_sessions.purge(app_id=app_id)
        _audit('sessions.purge', app_id or '*', detail=f"purged {count}")
        return jsonify({'purged': count})
    finally:
        repos.close()


# ---------------------------------------------------------------- audit


@admin_bp.route('/api/audit', methods=['GET'])
def get_audit():
    limit = request.args.get('limit', 100, type=int)
    repos = _repos()
    try:
        return jsonify([{
            'id': row.id,
            'ts': row.ts.isoformat() if row.ts else None,
            'actor': row.actor,
            'action': row.action,
            'target': row.target,
            'ip': row.ip,
            'detail': row.detail,
        } for row in repos.audit.list(limit=limit)])
    finally:
        repos.close()


# ---------------------------------------------------------------- version


@admin_bp.route('/api/version', methods=['GET'])
def get_version():
    version_file = os.path.join(os.path.dirname(os.path.dirname(__file__)), 'version.json')
    try:
        if os.path.exists(version_file):
            with open(version_file, 'r') as f:
                version_data = json.load(f)
        else:
            version_data = {'version': 'unknown', 'build_date': '', 'git_commit': '', 'git_branch': ''}
        version_data['app_identification_method'] = settings.app_identification_method
        return jsonify(version_data)
    except Exception as e:
        logger.error("Error reading version file: %s", e)
        return jsonify({
            'version': 'error', 'build_date': '', 'git_commit': '', 'git_branch': '',
            'app_identification_method': settings.app_identification_method, 'error': str(e),
        })


app.register_blueprint(admin_bp)

# Best-effort bootstrap at webui start (no-op when users already exist)
try:
    bootstrap_admin()
except Exception as exc:  # pragma: no cover - startup only
    logger.warning("bootstrap_admin during webui start failed: %s", exc)


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=8001, debug=True)
