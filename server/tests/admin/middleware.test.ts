import express from 'express';
import request from 'supertest';
import { requireAdmin, requireAdminSession } from '../../src/auth/admin-middleware';

// ---------------------------------------------------------------------------
// requireAdmin tests
// ---------------------------------------------------------------------------

function buildRequireAdminApp(opts: { sessionIsAdmin?: boolean; jwtIsAdmin?: boolean } = {}) {
  const app = express();

  // Simulate session state by injecting it before the middleware
  app.use((req: any, _res, next) => {
    req.session = opts.sessionIsAdmin ? { isAdmin: true, userId: 'user-1' } : {};
    if (opts.jwtIsAdmin) {
      req.isAdmin = true;
    }
    next();
  });

  app.get('/admin/api/test', requireAdmin, (_req, res) => {
    res.status(200).json({ ok: true });
  });

  return app;
}

describe('requireAdmin middleware', () => {
  it('returns 403 when not admin (no session, no JWT)', async () => {
    const app = buildRequireAdminApp();
    const res = await request(app).get('/admin/api/test');
    expect(res.status).toBe(403);
    expect(res.body).toEqual({ error: 'Admin access required' });
  });

  it('passes when req.isAdmin is true (JWT claim)', async () => {
    const app = buildRequireAdminApp({ jwtIsAdmin: true });
    const res = await request(app).get('/admin/api/test');
    expect(res.status).toBe(200);
    expect(res.body).toEqual({ ok: true });
  });

  it('passes when session isAdmin is true (browser session)', async () => {
    const app = buildRequireAdminApp({ sessionIsAdmin: true });
    const res = await request(app).get('/admin/api/test');
    expect(res.status).toBe(200);
    expect(res.body).toEqual({ ok: true });
  });
});

// ---------------------------------------------------------------------------
// requireAdminSession tests
// ---------------------------------------------------------------------------

function buildRequireAdminSessionApp(opts: { hasValidSession?: boolean } = {}) {
  const app = express();

  app.use((req: any, _res, next) => {
    if (opts.hasValidSession) {
      req.session = { userId: 'user-1', isAdmin: true };
    } else {
      req.session = {};
    }
    next();
  });

  app.get('/admin/dashboard', requireAdminSession, (_req, res) => {
    res.status(200).send('Admin dashboard');
  });

  return app;
}

describe('requireAdminSession middleware', () => {
  it('redirects to /admin/login when no session', async () => {
    const app = buildRequireAdminSessionApp();
    const res = await request(app).get('/admin/dashboard');
    expect(res.status).toBe(302);
    expect(res.headers['location']).toBe('/admin/login');
  });

  it('passes when session has userId and isAdmin', async () => {
    const app = buildRequireAdminSessionApp({ hasValidSession: true });
    const res = await request(app).get('/admin/dashboard');
    expect(res.status).toBe(200);
    expect(res.text).toBe('Admin dashboard');
  });
});
