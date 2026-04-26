import express from 'express';
import request from 'supertest';
import { createAuthMiddleware } from '../../src/auth/middleware';
import { signToken, JwtPayload } from '../../src/auth/jwt';

const TEST_SECRET = 'test-middleware-secret';

const testPayload: JwtPayload = {
  userId: 'user-abc',
  email: 'user@example.com',
  isAdmin: false,
  hasSharingBan: false,
};

function buildApp() {
  const app = express();
  app.use(createAuthMiddleware(TEST_SECRET));
  app.get('/test', (req: any, res) => {
    res.json({
      userId: req.userId,
      email: req.email,
      isAdmin: req.isAdmin,
      hasSharingBan: req.hasSharingBan,
      userGroupIds: req.userGroupIds,
    });
  });
  return app;
}

describe('Auth middleware', () => {
  it('returns 401 when no Authorization header is provided', async () => {
    const app = buildApp();
    const res = await request(app).get('/test');
    expect(res.status).toBe(401);
  });

  it('returns 401 when Authorization header does not start with Bearer', async () => {
    const app = buildApp();
    const res = await request(app).get('/test').set('Authorization', 'Basic sometoken');
    expect(res.status).toBe(401);
  });

  it('returns 401 for an invalid token', async () => {
    const app = buildApp();
    const res = await request(app).get('/test').set('Authorization', 'Bearer not.a.valid.token');
    expect(res.status).toBe(401);
  });

  it('returns 401 for a token signed with the wrong secret', async () => {
    const app = buildApp();
    const token = signToken(testPayload, 'wrong-secret');
    const res = await request(app).get('/test').set('Authorization', `Bearer ${token}`);
    expect(res.status).toBe(401);
  });

  it('sets userId, email, isAdmin, hasSharingBan, and userGroupIds on request for valid token', async () => {
    const app = buildApp();
    const token = signToken(testPayload, TEST_SECRET);
    const res = await request(app).get('/test').set('Authorization', `Bearer ${token}`);

    expect(res.status).toBe(200);
    expect(res.body.userId).toBe('user-abc');
    expect(res.body.email).toBe('user@example.com');
    expect(res.body.isAdmin).toBe(false);
    expect(res.body.hasSharingBan).toBe(false);
    expect(res.body.userGroupIds).toEqual([]);
  });

  it('sets isAdmin to true for admin token', async () => {
    const app = buildApp();
    const adminPayload: JwtPayload = { ...testPayload, isAdmin: true };
    const token = signToken(adminPayload, TEST_SECRET);
    const res = await request(app).get('/test').set('Authorization', `Bearer ${token}`);

    expect(res.status).toBe(200);
    expect(res.body.isAdmin).toBe(true);
  });

  it('sets hasSharingBan to true for banned user token', async () => {
    const app = buildApp();
    const bannedPayload: JwtPayload = { ...testPayload, hasSharingBan: true };
    const token = signToken(bannedPayload, TEST_SECRET);
    const res = await request(app).get('/test').set('Authorization', `Bearer ${token}`);

    expect(res.status).toBe(200);
    expect(res.body.hasSharingBan).toBe(true);
  });
});
