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
import { createNotificationRouter } from '../../src/routes/notifications';
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

describe('Notification routes', () => {
  let db: Knex;
  let testUser: User;
  let token: string;

  beforeAll(async () => {
    db = await setupTestDb();
    const repos = buildRepos(db);

    testUser = await repos.users.create({
      googleId: 'g-notif-routes',
      email: 'notifroutes@example.com',
      displayName: 'Notif Route User',
    });
    token = signToken(
      { userId: testUser.id, email: testUser.email, isAdmin: false, hasSharingBan: false },
      TEST_JWT_SECRET,
    );
  });

  afterAll(async () => {
    await teardownTestDb();
  });

  beforeEach(async () => {
    await db('notifications').delete();
  });

  function buildApp() {
    const app = express();
    app.use(express.json());
    app.use(createAuthMiddleware(TEST_JWT_SECRET));
    const repos = buildRepos(getTestDb());
    app.use(createNotificationRouter(repos));
    app.use(errorHandler);
    return app;
  }

  describe('GET /api/notifications', () => {
    it('returns unread notifications', async () => {
      const repos = buildRepos(db);
      await repos.notifications.create(testUser.id, 'SYSTEM_MESSAGE', { text: 'Hello' });
      await repos.notifications.create(testUser.id, 'SYSTEM_MESSAGE', { text: 'World' });

      const app = buildApp();
      const res = await request(app)
        .get('/api/notifications')
        .set('Authorization', `Bearer ${token}`);

      expect(res.status).toBe(200);
      expect(res.body.data.length).toBe(2);
      expect(res.body.pagination).toHaveProperty('cursor');
      expect(res.body.pagination).toHaveProperty('hasMore');
    });

    it('returns empty when no unread notifications', async () => {
      const app = buildApp();
      const res = await request(app)
        .get('/api/notifications')
        .set('Authorization', `Bearer ${token}`);

      expect(res.status).toBe(200);
      expect(res.body.data.length).toBe(0);
    });
  });

  describe('PUT /api/notifications/:id/read', () => {
    it('marks notification as read', async () => {
      const repos = buildRepos(db);
      const notif = await repos.notifications.create(testUser.id, 'SYSTEM_MESSAGE', { text: 'Read me' });

      const app = buildApp();
      const res = await request(app)
        .put(`/api/notifications/${notif.id}/read`)
        .set('Authorization', `Bearer ${token}`);

      expect(res.status).toBe(200);
      expect(res.body.message).toBe('Notification marked as read');

      // Verify no longer in unread list
      const unread = await repos.notifications.getUnread(testUser.id);
      expect(unread.data.length).toBe(0);
    });
  });

  describe('PUT /api/notifications/read-all', () => {
    it('marks all notifications as read', async () => {
      const repos = buildRepos(db);
      await repos.notifications.create(testUser.id, 'SYSTEM_MESSAGE', { text: 'One' });
      await repos.notifications.create(testUser.id, 'SYSTEM_MESSAGE', { text: 'Two' });
      await repos.notifications.create(testUser.id, 'SYSTEM_MESSAGE', { text: 'Three' });

      const app = buildApp();
      const res = await request(app)
        .put('/api/notifications/read-all')
        .set('Authorization', `Bearer ${token}`);

      expect(res.status).toBe(200);
      expect(res.body.message).toBe('All notifications marked as read');

      // Verify all read
      const unread = await repos.notifications.getUnread(testUser.id);
      expect(unread.data.length).toBe(0);
    });
  });
});
