-- Example SQL for registering applications in the OAuth callback service
-- 
-- Usage:
-- 1. Generate a secure random secret:
--    python -c "import secrets; print(secrets.token_urlsafe(32))"
-- 
-- 2. Execute this SQL:
--    sqlite3 oauth_sessions.db < register_app_example.sql
--    OR
--    sqlite3 oauth_sessions.db
--    sqlite> .read register_app_example.sql

-- ============================================================================
-- EXAMPLE 1: Register OSMAnd Range Estimation App (for appid identification)
-- ============================================================================
-- Use when APP_IDENTIFICATION_METHOD=appid
-- Multiple apps can share the same domain (oauth.a42r.com)

INSERT INTO applications (
    app_id,
    name,
    allowed_domains,
    deep_link_scheme,
    shared_secret,
    is_active,
    created_at,
    updated_at
) VALUES (
    'com.a42r.eucosmandplugin',
    'OSMAnd Range Estimation',
    'oauth.a42r.com',  -- Can be shared with other apps when using appid method
    'osmeucworld://oauth/callback',
    'ibEVy3hSw-T1XEMMbjpC...',  -- REPLACE with actual secret
    1,
    datetime('now'),
    datetime('now')
);

-- ============================================================================
-- EXAMPLE 2: Register PEV PlugShare App (existing app)
-- ============================================================================
-- Already exists, but here's how it would be registered:

INSERT OR IGNORE INTO applications (
    app_id,
    name,
    allowed_domains,
    deep_link_scheme,
    shared_secret,
    is_active,
    created_at,
    updated_at
) VALUES (
    'pevplugshare',
    'PEV PlugShare',
    'pevplugshare.a42r.com,pevplugshare-prod.a42r.com,localhost:8000',
    'pevplugshare://oauth/callback',
    'CHANGE_THIS_SECRET',  -- REPLACE with actual secret
    1,
    datetime('now'),
    datetime('now')
);

-- ============================================================================
-- EXAMPLE 3: Register new app with subdomain (for domain identification)
-- ============================================================================
-- Use when APP_IDENTIFICATION_METHOD=domain
-- Each app should have unique domain(s)

INSERT INTO applications (
    app_id,
    name,
    allowed_domains,
    deep_link_scheme,
    shared_secret,
    is_active,
    created_at,
    updated_at
) VALUES (
    'com.example.myapp',
    'My Example App',
    'myapp.example.com,myapp-dev.example.com',  -- Unique domain per app
    'myapp://oauth/callback',
    'GENERATE_SECURE_SECRET_HERE',  -- python -c "import secrets; print(secrets.token_urlsafe(32))"
    1,
    datetime('now'),
    datetime('now')
);

-- ============================================================================
-- UTILITY QUERIES
-- ============================================================================

-- List all registered applications:
-- SELECT app_id, name, allowed_domains, deep_link_scheme, is_active, updated_at FROM applications;

-- Update an application's secret:
-- UPDATE applications SET shared_secret = 'new-secret', updated_at = datetime('now') WHERE app_id = 'com.example.myapp';

-- Deactivate an application:
-- UPDATE applications SET is_active = 0, updated_at = datetime('now') WHERE app_id = 'com.example.myapp';

-- Reactivate an application:
-- UPDATE applications SET is_active = 1, updated_at = datetime('now') WHERE app_id = 'com.example.myapp';

-- Delete an application (and its sessions):
-- DELETE FROM oauth_sessions WHERE app_id = 'com.example.myapp';
-- DELETE FROM applications WHERE app_id = 'com.example.myapp';
