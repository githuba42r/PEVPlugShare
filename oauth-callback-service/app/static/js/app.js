// OAuth Application Manager - Frontend JavaScript

let currentEditingApp = null;

// Initialize
document.addEventListener('DOMContentLoaded', () => {
    loadApplications();
    setupEventListeners();
});

function setupEventListeners() {
    // Add app button
    document.getElementById('addAppBtn').addEventListener('click', () => {
        openAppModal();
    });

    // Refresh button
    document.getElementById('refreshBtn').addEventListener('click', () => {
        loadApplications();
    });

    // Modal close buttons
    document.querySelectorAll('.close, .cancel-btn').forEach(btn => {
        btn.addEventListener('click', closeModals);
    });

    // Click outside modal to close
    window.addEventListener('click', (e) => {
        if (e.target.classList.contains('modal')) {
            closeModals();
        }
    });

    // Form submit
    document.getElementById('appForm').addEventListener('submit', handleFormSubmit);

    // Generate secret button
    document.getElementById('generateSecretBtn').addEventListener('click', generateSecret);

    // Copy secret button
    document.getElementById('copySecretBtn').addEventListener('click', copySecret);
}

async function loadApplications() {
    const appList = document.getElementById('appList');
    appList.innerHTML = '<div class="loading">Loading applications...</div>';

    try {
        const response = await fetch('/api/applications');
        if (!response.ok) throw new Error('Failed to load applications');
        
        const apps = await response.json();
        
        if (apps.length === 0) {
            appList.innerHTML = '<div class="empty-state">No applications configured. Click "Add New Application" to get started.</div>';
            return;
        }

        appList.innerHTML = apps.map(app => createAppCard(app)).join('');
        
        // Setup action button listeners
        setupAppActionListeners();
    } catch (error) {
        console.error('Error loading applications:', error);
        appList.innerHTML = '<div class="error-message">Failed to load applications. Please try again.</div>';
    }
}

function createAppCard(app) {
    const statusClass = app.is_active ? 'status-active' : 'status-inactive';
    const statusText = app.is_active ? 'Active' : 'Inactive';
    const toggleText = app.is_active ? 'Disable' : 'Enable';

    return `
        <div class="app-card" data-app-id="${app.app_id}">
            <div class="app-header">
                <div class="app-title">
                    <h3>${escapeHtml(app.name)}</h3>
                    <span class="status-badge ${statusClass}">${statusText}</span>
                </div>
                <div class="app-actions">
                    <button class="btn btn-small btn-secondary view-secret-btn" data-app-id="${app.app_id}">View Secret</button>
                    <button class="btn btn-small btn-secondary edit-btn" data-app-id="${app.app_id}">Edit</button>
                    <button class="btn btn-small btn-secondary toggle-btn" data-app-id="${app.app_id}">${toggleText}</button>
                    <button class="btn btn-small btn-danger delete-btn" data-app-id="${app.app_id}">Delete</button>
                </div>
            </div>
            <div class="app-details">
                <div class="detail-row">
                    <span class="detail-label">App ID:</span>
                    <span class="detail-value">${escapeHtml(app.app_id)}</span>
                </div>
                <div class="detail-row">
                    <span class="detail-label">Allowed Domains:</span>
                    <span class="detail-value">${escapeHtml(app.allowed_domains)}</span>
                </div>
                <div class="detail-row">
                    <span class="detail-label">Deep Link Scheme:</span>
                    <span class="detail-value">${escapeHtml(app.deep_link_scheme)}</span>
                </div>
                <div class="detail-row">
                    <span class="detail-label">Secret:</span>
                    <span class="detail-value">${escapeHtml(app.secret_preview)}</span>
                </div>
                <div class="detail-row">
                    <span class="detail-label">Last Updated:</span>
                    <span class="detail-value">${formatDate(app.updated_at)}</span>
                </div>
            </div>
        </div>
    `;
}

function setupAppActionListeners() {
    // View secret buttons
    document.querySelectorAll('.view-secret-btn').forEach(btn => {
        btn.addEventListener('click', () => viewSecret(btn.dataset.appId));
    });

    // Edit buttons
    document.querySelectorAll('.edit-btn').forEach(btn => {
        btn.addEventListener('click', () => editApplication(btn.dataset.appId));
    });

    // Toggle buttons
    document.querySelectorAll('.toggle-btn').forEach(btn => {
        btn.addEventListener('click', () => toggleApplication(btn.dataset.appId));
    });

    // Delete buttons
    document.querySelectorAll('.delete-btn').forEach(btn => {
        btn.addEventListener('click', () => deleteApplication(btn.dataset.appId));
    });
}

function openAppModal(app = null) {
    currentEditingApp = app;
    const modal = document.getElementById('appModal');
    const form = document.getElementById('appForm');
    const title = document.getElementById('modalTitle');

    form.reset();

    if (app) {
        title.textContent = 'Edit Application';
        document.getElementById('appId').value = app.app_id;
        document.getElementById('appId').readOnly = true;
        document.getElementById('appName').value = app.name;
        document.getElementById('allowedDomains').value = app.allowed_domains;
        document.getElementById('deepLinkScheme').value = app.deep_link_scheme;
        document.getElementById('sharedSecret').value = app.shared_secret;
    } else {
        title.textContent = 'Add New Application';
        document.getElementById('appId').readOnly = false;
        generateSecret();
    }

    modal.style.display = 'block';
}

function closeModals() {
    document.getElementById('appModal').style.display = 'none';
    document.getElementById('secretModal').style.display = 'none';
    currentEditingApp = null;
}

async function handleFormSubmit(e) {
    e.preventDefault();

    const formData = new FormData(e.target);
    const data = Object.fromEntries(formData.entries());

    try {
        let response;
        if (currentEditingApp) {
            // Update existing app
            response = await fetch(`/api/applications/${data.app_id}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(data)
            });
        } else {
            // Create new app
            response = await fetch('/api/applications', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(data)
            });
        }

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.error || 'Failed to save application');
        }

        closeModals();
        loadApplications();
    } catch (error) {
        console.error('Error saving application:', error);
        alert('Error: ' + error.message);
    }
}

async function editApplication(appId) {
    try {
        const response = await fetch(`/api/applications/${appId}`);
        if (!response.ok) throw new Error('Failed to load application');
        
        const app = await response.json();
        openAppModal(app);
    } catch (error) {
        console.error('Error loading application:', error);
        alert('Error: ' + error.message);
    }
}

async function toggleApplication(appId) {
    try {
        const response = await fetch(`/api/applications/${appId}/toggle`, {
            method: 'POST'
        });
        
        if (!response.ok) throw new Error('Failed to toggle application');
        
        loadApplications();
    } catch (error) {
        console.error('Error toggling application:', error);
        alert('Error: ' + error.message);
    }
}

async function deleteApplication(appId) {
    if (!confirm(`Are you sure you want to delete the application "${appId}"? This cannot be undone.`)) {
        return;
    }

    try {
        const response = await fetch(`/api/applications/${appId}`, {
            method: 'DELETE'
        });
        
        if (!response.ok) throw new Error('Failed to delete application');
        
        loadApplications();
    } catch (error) {
        console.error('Error deleting application:', error);
        alert('Error: ' + error.message);
    }
}

async function viewSecret(appId) {
    try {
        const response = await fetch(`/api/applications/${appId}`);
        if (!response.ok) throw new Error('Failed to load application');
        
        const app = await response.json();
        
        document.getElementById('secretAppName').textContent = app.name;
        document.getElementById('secretValue').textContent = app.shared_secret;
        document.getElementById('secretModal').style.display = 'block';
    } catch (error) {
        console.error('Error loading secret:', error);
        alert('Error: ' + error.message);
    }
}

function generateSecret() {
    // Generate a URL-safe random string (similar to Python's secrets.token_urlsafe)
    const array = new Uint8Array(32);
    crypto.getRandomValues(array);
    const secret = btoa(String.fromCharCode.apply(null, array))
        .replace(/\+/g, '-')
        .replace(/\//g, '_')
        .replace(/=/g, '');
    
    document.getElementById('sharedSecret').value = secret;
}

function copySecret() {
    const secretValue = document.getElementById('secretValue').textContent;
    navigator.clipboard.writeText(secretValue).then(() => {
        const btn = document.getElementById('copySecretBtn');
        const originalText = btn.textContent;
        btn.textContent = 'Copied!';
        setTimeout(() => {
            btn.textContent = originalText;
        }, 2000);
    });
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function formatDate(isoString) {
    if (!isoString) return 'N/A';
    const date = new Date(isoString);
    return date.toLocaleString();
}
