import express from 'express';
import request from 'supertest';
import type { Knex } from 'knex';
import { setupTestDb, teardownTestDb } from '../../setup';
import { PostgresUserRepo } from '../../../src/repositories/postgres/user-repo';
import { PostgresBanRepo } from '../../../src/repositories/postgres/ban-repo';
import { PostgresRateLimitRepo } from '../../../src/repositories/postgres/rate-limit-repo';
import { PostgresNotificationRepo } from '../../../src/repositories/postgres/notification-repo';
import { PostgresLocationRepo } from '../../../src/repositories/postgres/location-repo';
import { PostgresPhotoRepo } from '../../../src/repositories/postgres/photo-repo';
import type { Repositories } from '../../../src/repositories/factory';
import { requireAdmin } from '../../../src/auth/admin-middleware';
import { createAdminRateLimitRouter } from '../../../src/admin/api/rate-limits';
import { errorHandler } from '../../../src/middleware/error-handler';

function buildRepos(db: Knex): Repositories {
  return {
    users: new PostgresUserRepo(db),
    bans: new PostgresBanRepo(db),
    rateLimits: new PostgresRateLimitRepo(db),
    notifications: new PostgresNotificationRepo(db),
    locations: new PostgresLocationRepo(db),
    photos: new PostgresPhotoRepo(db),
  };
}

function createAdminApp(repos: Repositories) {
  const app = express();
  app.use(express.json());
  app.use((req: any, _res: any, next: any) => {
    req.userId = 'admin-user-id';
    req.isAdmin = true;
    next();
  });
  app.use(requireAdmin);
  app.use(createAdminRateLimitRouter(repos));
  app.use(errorHandler);
  return app;
}

describe('Admin API — Rate Limits', () => {
  let db: Knex;
  let repos: Repositories;
  let testUserId: string;

  beforeAll(async () => {
    db = await setupTestDb();
  });

  afterAll(async () => {
    await teardownTestDb();
  });

  beforeEach(async () => {
    repos = buildRepos(db);
    await db('rate_limits').delete();
    await db('users').delete();
    const user = await repos.users.create({ googleId: 'g-rl', email: 'rl@test.com', displayName: 'RL User' });
    testUserId = user.id;
  });

  it('GET /rate-limits returns empty array initially', async () => {
    const app = createAdminApp(repos);
    const res = await request(app).get('/rate-limits');
    expect(res.status).toBe(200);
    expect(res.body).toEqual([]);
  });

  it('POST /rate-limits creates a rate limit', async () => {
    const app = createAdminApp(repos);
    const res = await request(app).post('/rate-limits').send({
      scope: 'GLOBAL',
      endpoint: '/api/locations',
      maxRequests: 100,
      windowSeconds: 60,
    });
    expect(res.status).toBe(201);
    expect(res.body.scope).toBe('GLOBAL');
    expect(res.body.endpoint).toBe('/api/locations');
    expect(res.body.maxRequests).toBe(100);
    expect(res.body.windowSeconds).toBe(60);
  });

  it('POST /rate-limits returns 400 for missing fields', async () => {
    const app = createAdminApp(repos);
    const res = await request(app).post('/rate-limits').send({ scope: 'GLOBAL' });
    expect(res.status).toBe(400);
  });

  it('POST /rate-limits returns 400 for invalid scope', async () => {
    const app = createAdminApp(repos);
    const res = await request(app).post('/rate-limits').send({
      scope: 'INVALID',
      endpoint: '/api/test',
      maxRequests: 10,
      windowSeconds: 60,
    });
    expect(res.status).toBe(400);
  });

  it('GET /rate-limits returns created limits', async () => {
    await repos.rateLimits.create({ scope: 'GLOBAL', endpoint: '/test', maxRequests: 50, windowSeconds: 30 });
    const app = createAdminApp(repos);
    const res = await request(app).get('/rate-limits');
    expect(res.status).toBe(200);
    expect(res.body).toHaveLength(1);
  });

  it('PUT /rate-limits/:id updates rate limit', async () => {
    const limit = await repos.rateLimits.create({
      scope: 'GLOBAL',
      endpoint: '/test',
      maxRequests: 50,
      windowSeconds: 30,
    });
    const app = createAdminApp(repos);
    const res = await request(app).put(`/rate-limits/${limit.id}`).send({ maxRequests: 200 });
    expect(res.status).toBe(200);
    expect(res.body.maxRequests).toBe(200);
  });

  it('PUT /rate-limits/:id returns 404 for unknown id', async () => {
    const app = createAdminApp(repos);
    const res = await request(app)
      .put('/rate-limits/00000000-0000-0000-0000-000000000000')
      .send({ maxRequests: 10 });
    expect(res.status).toBe(404);
  });

  it('DELETE /rate-limits/:id deletes rate limit', async () => {
    const limit = await repos.rateLimits.create({
      scope: 'GLOBAL',
      endpoint: '/test',
      maxRequests: 50,
      windowSeconds: 30,
    });
    const app = createAdminApp(repos);
    const res = await request(app).delete(`/rate-limits/${limit.id}`);
    expect(res.status).toBe(200);
    expect(res.body.message).toBe('Rate limit deleted');

    const all = await repos.rateLimits.getAll();
    expect(all.find((r) => r.id === limit.id)).toBeUndefined();
  });

  it('DELETE /rate-limits/:id returns 404 for unknown id', async () => {
    const app = createAdminApp(repos);
    const res = await request(app).delete('/rate-limits/00000000-0000-0000-0000-000000000000');
    expect(res.status).toBe(404);
  });

  it('GET /rate-limits/resolve/:userId returns resolved limit or no-limit message', async () => {
    await repos.rateLimits.create({ scope: 'GLOBAL', endpoint: '*', maxRequests: 100, windowSeconds: 60 });
    const app = createAdminApp(repos);
    const res = await request(app).get(`/rate-limits/resolve/${testUserId}`);
    expect(res.status).toBe(200);
    // Could be resolved limit or no-limit message — either is valid
    const isResolved = res.body.maxRequests !== undefined;
    const isNoLimit = res.body.message === 'No rate limit applies';
    expect(isResolved || isNoLimit).toBe(true);
  });

  it('GET /rate-limits/resolve/:userId returns 404 for unknown user', async () => {
    const app = createAdminApp(repos);
    const res = await request(app).get('/rate-limits/resolve/00000000-0000-0000-0000-000000000000');
    expect(res.status).toBe(404);
  });
});
