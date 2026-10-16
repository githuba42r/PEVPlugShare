/* OAuth Application Manager — admin SPA (vanilla JS) */
(function () {
    'use strict';

    let CSRF_TOKEN = '';
    let CURRENT_USER = { username: null, role: null };
    let editingAppId = null;
    let passwordTargetUser = null;

    // ---------------------------------------------------------------- helpers

    async function api(path, options = {}) {
        const opts = Object.assign({ headers: {} }, options);
        opts.headers['X-CSRF-Token'] = CSRF_TOKEN;
        if (opts.body && !opts.headers['Content-Type']) {
            opts.headers['Content-Type'] = 'application/json';
        }
        const res = await fetch(path, opts);
        if (res.status === 401) {
            window.location.href = '/admin/login';
            throw new Error('unauthorized');
        }
        let data = null;
        try { data = await res.json(); } catch (e) { /* no body */ }
        if (!res.ok) {
            const msg = (data && (data.message || data.error)) || res.statusText;
            throw new Error(typeof msg === 'string' ? msg : JSON.stringify(msg));
        }
        return data;
    }

    function el(tag, attrs = {}, ...children) {
        const node = document.createElement(tag);
        Object.entries(attrs).forEach(([k, v]) => {
            if (k === 'class') node.className = v;
            else if (k.startsWith('on')) node.addEventListener(k.slice(2), v);
            else if (v !== null && v !== undefined) node.setAttribute(k, v);
        });
        children.flat().forEach(c => {
            if (c === null || c === undefined) return;
            node.appendChild(typeof c === 'string' ? document.createTextNode(c) : c);
        });
        return node;
    }

    function showModal(id) { document.getElementById(id).style.display = 'block'; }
    function hideModal(id) { document.getElementById(id).style.display = 'none'; }

    function showSecret(title, label, subject, secret) {
        document.getElementById('secretTitle').textContent = title;
        document.getElementById('secretLabel').textContent = label;
        document.getElementById('secretSubject').textContent = subject;
        document.getElementById('secretValue').textContent = secret;
        showModal('secretModal');
    }

    // ---------------------------------------------------------------- tabs

    document.querySelectorAll('.tab').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('.tab').forEach(b => b.classList.remove('active'));
            document.querySelectorAll('.tab-panel').forEach(p => p.classList.remove('active'));
            btn.classList.add('active');
            document.getElementById('panel-' + btn.dataset.tab).classList.add('active');
            loadTab(btn.dataset.tab);
        });
    });

    function loadTab(tab) {
        if (tab === 'apps') loadApps();
        else if (tab === 'users') loadUsers();
        else if (tab === 'clients') loadClients();
        else if (tab === 'sessions') loadSessions();
        else if (tab === 'audit') loadAudit();
    }

    // ---------------------------------------------------------------- applications

    async function loadApps() {
        const list = document.getElementById('appList');
        list.innerHTML = '<div class="loading">Loading applications...</div>';
        try {
            const apps = await api('/admin/api/applications');
            list.innerHTML = '';
            if (!apps.length) {
                list.appendChild(el('div', { class: 'empty-state' }, 'No applications yet.'));
                return;
            }
            apps.forEach(app => list.appendChild(appCard(app)));
        } catch (e) {
            list.innerHTML = '<div class="error-message">' + e.message + '</div>';
        }
    }

    function appCard(app) {
        return el('div', { class: 'app-card' },
            el('div', { class: 'app-header' },
                el('div', { class: 'app-title' },
                    el('h3', {}, app.name),
                    el('span', {
                        class: 'status-badge ' + (app.is_active ? 'status-active' : 'status-inactive')
                    }, app.is_active ? 'Active' : 'Disabled')
                ),
                el('div', { class: 'app-actions' },
                    el('button', { class: 'btn btn-secondary btn-small', onclick: () => openEditApp(app) }, 'Edit'),
                    el('button', { class: 'btn btn-secondary btn-small', onclick: () => viewSecret(app.app_id) }, 'Secret'),
                    el('button', { class: 'btn btn-secondary btn-small', onclick: () => toggleApp(app.app_id) },
                        app.is_active ? 'Disable' : 'Enable'),
                    el('button', { class: 'btn btn-danger btn-small', onclick: () => deleteApp(app.app_id, app.name) }, 'Delete')
                )
            ),
            el('div', { class: 'app-details' },
                detailRow('App ID', app.app_id),
                detailRow('Domains', app.allowed_domains),
                detailRow('Deep Link', app.deep_link_scheme),
                detailRow('Secret', app.secret_preview || ''),
                detailRow('Updated', app.updated_at || '')
            )
        );
    }

    function detailRow(label, value) {
        return el('div', { class: 'detail-row' },
            el('span', { class: 'detail-label' }, label),
            el('span', { class: 'detail-value' }, value)
        );
    }

    function openEditApp(app) {
        editingAppId = app.app_id;
        document.getElementById('modalTitle').textContent = 'Edit Application';
        document.getElementById('appId').value = app.app_id;
        document.getElementById('appId').readOnly = true;
        document.getElementById('appName').value = app.name;
        document.getElementById('allowedDomains').value = app.allowed_domains;
        document.getElementById('deepLinkScheme').value = app.deep_link_scheme;
        document.getElementById('sharedSecret').value = '';
        showModal('appModal');
    }

    document.getElementById('addAppBtn').addEventListener('click', () => {
        editingAppId = null;
        document.getElementById('modalTitle').textContent = 'Add New Application';
        document.getElementById('appForm').reset();
        document.getElementById('appId').readOnly = false;
        showModal('appModal');
    });

    document.getElementById('generateSecretBtn').addEventListener('click', () => {
        const bytes = new Uint8Array(32);
        crypto.getRandomValues(bytes);
        document.getElementById('sharedSecret').value =
            btoa(String.fromCharCode(...bytes)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
    });

    document.getElementById('appForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const body = {
            name: document.getElementById('appName').value,
            allowed_domains: document.getElementById('allowedDomains').value,
            deep_link_scheme: document.getElementById('deepLinkScheme').value,
        };
        const secret = document.getElementById('sharedSecret').value;
        if (secret) body.shared_secret = secret;
        try {
            if (editingAppId) {
                await api('/admin/api/applications/' + encodeURIComponent(editingAppId), {
                    method: 'PUT', body: JSON.stringify(body),
                });
            } else {
                body.app_id = document.getElementById('appId').value;
                const created = await api('/admin/api/applications', {
                    method: 'POST', body: JSON.stringify(body),
                });
                hideModal('appModal');
                showSecret('Application Created', 'Shared secret:', created.app_id, created.shared_secret);
                loadApps();
                return;
            }
            hideModal('appModal');
            loadApps();
        } catch (err) { alert(err.message); }
    });

    async function viewSecret(appId) {
        try {
            const app = await api('/admin/api/applications/' + encodeURIComponent(appId));
            showSecret('Shared Secret', 'App:', app.app_id, app.shared_secret);
        } catch (e) { alert(e.message); }
    }

    async function toggleApp(appId) {
        try {
            await api('/admin/api/applications/' + encodeURIComponent(appId) + '/toggle', { method: 'POST' });
            loadApps();
        } catch (e) { alert(e.message); }
    }

    async function deleteApp(appId, name) {
        if (!confirm('Delete application "' + name + '"? This cannot be undone.')) return;
        try {
            await api('/admin/api/applications/' + encodeURIComponent(appId), { method: 'DELETE' });
            loadApps();
        } catch (e) { alert(e.message); }
    }

    // ---------------------------------------------------------------- users

    async function loadUsers() {
        const list = document.getElementById('userList');
        list.innerHTML = '<div class="loading">Loading users...</div>';
        try {
            const users = await api('/admin/api/users');
            list.innerHTML = '';
            if (!users.length) {
                list.appendChild(el('div', { class: 'empty-state' }, 'No users. Create one with oauthctl or the button above.'));
                return;
            }
            users.forEach(u => list.appendChild(userCard(u)));
        } catch (e) {
            list.innerHTML = '<div class="error-message">' + e.message + '</div>';
        }
    }

    function userCard(u) {
        const isAdmin = CURRENT_USER.role === 'admin';
        const actions = [];
        if (isAdmin) {
            actions.push(el('button', { class: 'btn btn-secondary btn-small', onclick: () => openPasswordModal(u.username) }, 'Password'));
            actions.push(el('button', {
                class: 'btn btn-secondary btn-small',
                onclick: async () => {
                    try {
                        await api('/admin/api/users/' + encodeURIComponent(u.username), {
                            method: 'PATCH', body: JSON.stringify({ is_active: !u.is_active }),
                        });
                        loadUsers();
                    } catch (e) { alert(e.message); }
                }
            }, u.is_active ? 'Disable' : 'Enable'));
            if (u.username !== CURRENT_USER.username) {
                actions.push(el('button', {
                    class: 'btn btn-danger btn-small',
                    onclick: async () => {
                        if (!confirm('Delete user "' + u.username + '"?')) return;
                        try {
                            await api('/admin/api/users/' + encodeURIComponent(u.username), { method: 'DELETE' });
                            loadUsers();
                        } catch (e) { alert(e.message); }
                    }
                }, 'Delete'));
            }
        }
        return el('div', { class: 'app-card' },
            el('div', { class: 'app-header' },
                el('div', { class: 'app-title' },
                    el('h3', {}, u.username),
                    el('span', { class: 'status-badge ' + (u.is_active ? 'status-active' : 'status-inactive') },
                        u.is_active ? 'Active' : 'Disabled'),
                    el('span', { class: 'status-badge status-role' }, u.role)
                ),
                el('div', { class: 'app-actions' }, actions)
            ),
            el('div', { class: 'app-details' },
                detailRow('Last login', u.last_login || 'never'),
                detailRow('Created', u.created_at || '')
            )
        );
    }

    document.getElementById('addUserBtn').addEventListener('click', () => {
        document.getElementById('userForm').reset();
        showModal('userModal');
    });

    document.getElementById('userForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        try {
            await api('/admin/api/users', {
                method: 'POST',
                body: JSON.stringify({
                    username: document.getElementById('newUsername').value,
                    password: document.getElementById('newPassword').value,
                    role: document.getElementById('newRole').value,
                }),
            });
            hideModal('userModal');
            loadUsers();
        } catch (err) { alert(err.message); }
    });

    function openPasswordModal(username) {
        passwordTargetUser = username;
        document.getElementById('pwUsername').textContent = username;
        document.getElementById('passwordForm').reset();
        showModal('passwordModal');
    }

    document.getElementById('passwordForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        try {
            await api('/admin/api/users/' + encodeURIComponent(passwordTargetUser), {
                method: 'PATCH',
                body: JSON.stringify({ password: document.getElementById('pwNew').value }),
            });
            hideModal('passwordModal');
        } catch (err) { alert(err.message); }
    });

    // ---------------------------------------------------------------- clients

    async function loadClients() {
        const list = document.getElementById('clientList');
        list.innerHTML = '<div class="loading">Loading API clients...</div>';
        try {
            const clients = await api('/admin/api/clients');
            list.innerHTML = '';
            if (!clients.length) {
                list.appendChild(el('div', { class: 'empty-state' }, 'No API clients yet.'));
                return;
            }
            clients.forEach(c => list.appendChild(clientCard(c)));
        } catch (e) {
            list.innerHTML = '<div class="error-message">' + e.message + '</div>';
        }
    }

    function clientCard(c) {
        return el('div', { class: 'app-card' },
            el('div', { class: 'app-header' },
                el('div', { class: 'app-title' },
                    el('h3', {}, c.name),
                    el('span', { class: 'status-badge ' + (c.is_active ? 'status-active' : 'status-inactive') },
                        c.is_active ? 'Active' : 'Disabled')
                ),
                el('div', { class: 'app-actions' },
                    el('button', {
                        class: 'btn btn-secondary btn-small',
                        onclick: async () => {
                            if (!confirm('Rotate secret for "' + c.name + '"? All existing tokens will be revoked.')) return;
                            try {
                                const res = await api('/admin/api/clients/' + encodeURIComponent(c.client_id) + '/rotate-secret', { method: 'POST' });
                                showSecret('Client Secret Rotated', 'Client:', res.client_id, res.client_secret);
                            } catch (e) { alert(e.message); }
                        }
                    }, 'Rotate Secret'),
                    el('button', {
                        class: 'btn btn-secondary btn-small',
                        onclick: async () => {
                            try {
                                await api('/admin/api/clients/' + encodeURIComponent(c.client_id) + '/revoke-tokens', { method: 'POST' });
                                alert('Tokens revoked.');
                            } catch (e) { alert(e.message); }
                        }
                    }, 'Revoke Tokens'),
                    el('button', {
                        class: 'btn btn-danger btn-small',
                        onclick: async () => {
                            if (!confirm('Delete API client "' + c.name + '"?')) return;
                            try {
                                await api('/admin/api/clients/' + encodeURIComponent(c.client_id), { method: 'DELETE' });
                                loadClients();
                            } catch (e) { alert(e.message); }
                        }
                    }, 'Delete')
                )
            ),
            el('div', { class: 'app-details' },
                detailRow('Client ID', c.client_id),
                detailRow('Scopes', c.scopes),
                detailRow('Updated', c.updated_at || '')
            )
        );
    }

    document.getElementById('addClientBtn').addEventListener('click', () => {
        document.getElementById('clientForm').reset();
        showModal('clientModal');
    });

    document.getElementById('clientForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const scopes = Array.from(document.querySelectorAll('#clientForm .scope-grid input:checked'))
            .map(cb => cb.value);
        if (!scopes.length) { alert('Select at least one scope.'); return; }
        const clientId = document.getElementById('clientIdInput').value.trim();
        const body = {
            name: document.getElementById('clientName').value,
            scopes: scopes.join(','),
        };
        if (clientId) body.client_id = clientId;
        try {
            const created = await api('/admin/api/clients', { method: 'POST', body: JSON.stringify(body) });
            hideModal('clientModal');
            showSecret('API Client Created', 'Client secret:', created.client_id, created.client_secret);
            loadClients();
        } catch (err) { alert(err.message); }
    });

    // ---------------------------------------------------------------- sessions

    async function loadSessions() {
        const list = document.getElementById('sessionList');
        list.innerHTML = '<div class="loading">Loading sessions...</div>';
        try {
            const sessions = await api('/admin/api/sessions?limit=100');
            list.innerHTML = '';
            if (!sessions.length) {
                list.appendChild(el('div', { class: 'empty-state' }, 'No sessions.'));
                return;
            }
            const table = el('table', { class: 'data-table' },
                el('thead', {}, el('tr', {},
                    el('th', {}, 'Session'), el('th', {}, 'App'), el('th', {}, 'Provider'),
                    el('th', {}, 'Created'), el('th', {}, 'Expires'), el('th', {}, 'Used'), el('th', {}, 'IP')
                ))
            );
            const tbody = el('tbody');
            sessions.forEach(s => {
                tbody.appendChild(el('tr', {},
                    el('td', {}, s.session_id.slice(0, 8) + '…'),
                    el('td', {}, s.app_id),
                    el('td', {}, s.provider),
                    el('td', {}, (s.created_at || '').replace('T', ' ').slice(0, 19)),
                    el('td', {}, (s.expires_at || '').replace('T', ' ').slice(0, 19)),
                    el('td', {}, s.used ? 'yes' : 'no'),
                    el('td', {}, s.client_ip || '')
                ));
            });
            table.appendChild(tbody);
            list.appendChild(table);
        } catch (e) {
            list.innerHTML = '<div class="error-message">' + e.message + '</div>';
        }
    }

    document.getElementById('purgeSessionsBtn').addEventListener('click', async () => {
        if (!confirm('Purge ALL callback sessions?')) return;
        try {
            const res = await api('/admin/api/sessions', { method: 'DELETE' });
            alert('Purged ' + res.purged + ' sessions.');
            loadSessions();
        } catch (e) { alert(e.message); }
    });

    // ---------------------------------------------------------------- audit

    async function loadAudit() {
        const list = document.getElementById('auditList');
        list.innerHTML = '<div class="loading">Loading audit log...</div>';
        try {
            const rows = await api('/admin/api/audit?limit=200');
            list.innerHTML = '';
            if (!rows.length) {
                list.appendChild(el('div', { class: 'empty-state' }, 'Audit log is empty.'));
                return;
            }
            const table = el('table', { class: 'data-table' },
                el('thead', {}, el('tr', {},
                    el('th', {}, 'Time'), el('th', {}, 'Actor'), el('th', {}, 'Action'),
                    el('th', {}, 'Target'), el('th', {}, 'IP'), el('th', {}, 'Detail')
                ))
            );
            const tbody = el('tbody');
            rows.forEach(r => {
                tbody.appendChild(el('tr', {},
                    el('td', {}, (r.ts || '').replace('T', ' ').slice(0, 19)),
                    el('td', {}, r.actor),
                    el('td', {}, r.action),
                    el('td', {}, r.target || ''),
                    el('td', {}, r.ip || ''),
                    el('td', {}, r.detail || '')
                ));
            });
            table.appendChild(tbody);
            list.appendChild(table);
        } catch (e) {
            list.innerHTML = '<div class="error-message">' + e.message + '</div>';
        }
    }

    // ---------------------------------------------------------------- modal wiring

    document.querySelectorAll('.modal .close, .modal .cancel-btn').forEach(node => {
        node.addEventListener('click', () => node.closest('.modal').style.display = 'none');
    });
    window.addEventListener('click', (e) => {
        if (e.target.classList.contains('modal')) e.target.style.display = 'none';
    });

    document.getElementById('copySecretBtn').addEventListener('click', async () => {
        const value = document.getElementById('secretValue').textContent;
        try {
            await navigator.clipboard.writeText(value);
            document.getElementById('copySecretBtn').textContent = 'Copied';
            setTimeout(() => { document.getElementById('copySecretBtn').textContent = 'Copy'; }, 1500);
        } catch (e) { /* clipboard unavailable */ }
    });

    document.getElementById('refreshAppsBtn').addEventListener('click', loadApps);
    document.getElementById('refreshUsersBtn').addEventListener('click', loadUsers);
    document.getElementById('refreshClientsBtn').addEventListener('click', loadClients);
    document.getElementById('refreshSessionsBtn').addEventListener('click', loadSessions);
    document.getElementById('refreshAuditBtn').addEventListener('click', loadAudit);

    // ---------------------------------------------------------------- boot

    (async function init() {
        try {
            const me = await api('/admin/api/me');
            CSRF_TOKEN = me.csrf_token;
            CURRENT_USER = { username: me.username, role: me.role };
            document.getElementById('userDisplay').textContent = me.username + ' (' + me.role + ')';
            // inject CSRF into the logout form too
            const logoutCsrf = document.querySelector('.logout-form input[name=csrf]');
            if (logoutCsrf) logoutCsrf.value = CSRF_TOKEN;
        } catch (e) { return; }

        try {
            const version = await api('/admin/api/version');
            document.getElementById('versionDisplay').textContent =
                'v' + version.version + (version.app_identification_method ? ' · ' + version.app_identification_method : '');
        } catch (e) { /* non-fatal */ }

        loadApps();
    })();
})();
