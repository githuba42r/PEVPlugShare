import express from 'express';
import request from 'supertest';
import type { Knex } from 'knex';
import { setupTestDb, teardownTestDb, getTestDb } from '../setup';
import { PostgresUserRepo } from '../../src/repositories/postgres/user-repo';
import { PostgresBanRepo } from '../../src/repositories/postgres/ban-repo';
import { PostgresRateLimitRepo } from '../../src/repositories/postgres/rate-limit-repo';
import { PostgresNotificationRepo } from '../../src/repositories/postgres/notification-repo';
import { PostgresLocationRepo } from '../../src/repositories/postgres/location-repo';
import { PostgresPhotoRepo } from '../../src/repositories/postgres/photo-repo';
import type { Repositories } from '../../src/repositories/factory';
import { createUserRouter } from '../../src/routes/users';
import { createAuthMiddleware } from '../../src/auth/middleware';
import { signToken } from '../../src/auth/jwt';
import { errorHandler } from '../../src/middleware/error-handler';
import type { User } from '../../src/models';

const TEST_JWT_SECRET = 'test-secret';

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

describe('User routes', () => {
  let db: Knex;
  let testUser: User;
  let token: string;

  beforeAll(async () => {
    db = await setupTestDb();
    const repos = buildRepos(db);
    testUser = await repos.users.create({
      googleId: 'g-user-routes',
      email: 'userroutes@example.com',
      displayName: 'Route Test User',
    });
    token = signToken(
      { userId: testUser.id, email: testUser.email, isAdmin: false, hasSharingBan: false },
      TEST_JWT_SECRET,
    );
  });

  afterAll(async () => {
    await teardownTestDb();
  });

  function buildApp() {
    const app = express();
    app.use(express.json());
    app.use(createAuthMiddleware(TEST_JWT_SECRET));
    const repos = buildRepos(getTestDb());
    app.use(createUserRouter(repos));
    app.use(errorHandler);
    return app;
  }

  describe('GET /api/users/me', () => {
    it('returns user profile with valid token', async () => {
      const app = buildApp();
      const res = await request(app)
        .get('/api/users/me')
        .set('Authorization', `Bearer ${token}`);

      expect(res.status).toBe(200);
      expect(res.body.id).toBe(testUser.id);
      expect(res.body.email).toBe(testUser.email);
      expect(res.body.displayName).toBe(testUser.displayName);
      expect(res.body).toHaveProperty('isAdmin');
      expect(res.body).toHaveProperty('createdAt');
    });

    it('returns 401 without token', async () => {
      const app = buildApp();
      const res = await request(app).get('/api/users/me');
      expect(res.status).toBe(401);
    });
  });

  describe('GET /api/users/search', () => {
    it('returns results for valid query', async () => {
      const app = buildApp();
      const res = await request(app)
        .get('/api/users/search?q=Route')
        .set('Authorization', `Bearer ${token}`);

      expect(res.status).toBe(200);
      expect(res.body).toHaveProperty('data');
      expect(Array.isArray(res.body.data)).toBe(true);
      expect(res.body.data.length).toBeGreaterThan(0);
      expect(res.body.data[0].displayName).toBe('Route Test User');
    });

    it('returns 400 for query shorter than 2 chars', async () => {
      const app = buildApp();
      const res = await request(app)
        .get('/api/users/search?q=a')
        .set('Authorization', `Bearer ${token}`);

      expect(res.status).toBe(400);
    });

    it('returns 400 for empty query', async () => {
      const app = buildApp();
      const res = await request(app)
        .get('/api/users/search?q=')
        .set('Authorization', `Bearer ${token}`);

      expect(res.status).toBe(400);
    });
  });
});
