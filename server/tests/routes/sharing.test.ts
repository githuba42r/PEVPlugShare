import express from 'express';
import request from 'supertest';
import type { Knex } from 'knex';
import { setupTestDb, teardownTestDb, getTestDb } from '../setup';
import { PostgresUserRepo } from '../../src/repositories/postgres/user-repo';
import { PostgresBanRepo } from '../../src/repositories/postgres/ban-repo';
import { PostgresLocationRepo } from '../../src/repositories/postgres/location-repo';
import { PostgresNotificationRepo } from '../../src/repositories/postgres/notification-repo';
import { PostgresGroupRepo } from '../../src/repositories/postgres/group-repo';
import { PostgresShareRepo } from '../../src/repositories/postgres/share-repo';
import { PostgresDeepLinkRepo } from '../../src/repositories/postgres/deep-link-repo';
import { SharingService } from '../../src/services/sharing-service';
import { createSharingRouter } from '../../src/routes/sharing';
import { createAuthMiddleware } from '../../src/auth/middleware';
import { signToken } from '../../src/auth/jwt';
import { errorHandler } from '../../src/middleware/error-handler';
import type { User, Location } from '../../src/models';

const TEST_JWT_SECRET = 'test-secret';

// Mock config so serverBaseUrl is stable
jest.mock('../../src/config', () => ({
  config: {
    jwtSecret: 'test-secret',
    googleClientId: 'test-client-id',
    adminSetupEmail: undefined,
    maxUploadSizeMb: 5,
    serverBaseUrl: 'http://localhost:8080',
  },
}));

function buildDeps(db: Knex) {
  const users = new PostgresUserRepo(db);
  const bans = new PostgresBanRepo(db);
  const locations = new PostgresLocationRepo(db);
  const notifications = new PostgresNotificationRepo(db);
  const groups = new PostgresGroupRepo(db);
  const shares = new PostgresShareRepo(db);
  const deepLinks = new PostgresDeepLinkRepo(db);
  return { users, bans, locations, notifications, groups, shares, deepLinks };
}

describe('Sharing routes', () => {
  let db: Knex;
  let user1: User;
  let user2: User;
  let token1: string;
  let token2: string;
  let location: Location;

  beforeAll(async () => {
    db = await setupTestDb();
    const deps = buildDeps(db);

    user1 = await deps.users.create({
      googleId: 'g-share-owner',
      email: 'shareowner@example.com',
      displayName: 'Share Owner',
    });
    user2 = await deps.users.create({
      googleId: 'g-share-recipient',
      email: 'sharerecipient@example.com',
      displayName: 'Share Recipient',
    });

    token1 = signToken(
      { userId: user1.id, email: user1.email, isAdmin: false, hasSharingBan: false },
      TEST_JWT_SECRET,
    );
    token2 = signToken(
      { userId: user2.id, email: user2.email, isAdmin: false, hasSharingBan: false },
      TEST_JWT_SECRET,
    );

    location = await deps.locations.create({
      ownerId: user1.id,
      name: 'My Shared Spot',
      latitude: -33.8688,
      longitude: 151.2093,
    });
  });

  afterAll(async () => {
    await teardownTestDb();
  });

  beforeEach(async () => {
    const testDb = getTestDb();
    await testDb('hidden_shares').delete();
    await testDb('location_shares').delete();
    await testDb('direct_share_tokens').delete();
    await testDb('notifications').delete();
  });

  function buildApp() {
    const testDb = getTestDb();
    const deps = buildDeps(testDb);
    const sharingService = new SharingService(
      deps.shares,
      deps.bans,
      deps.deepLinks,
      deps.locations,
      deps.notifications,
      deps.groups,
      'http://localhost:8080',
    );
    const app = express();
    app.use(express.json());
    app.use(createAuthMiddleware(TEST_JWT_SECRET));
    app.use(createSharingRouter(sharingService, deps.shares));
    app.use(errorHandler);
    return app;
  }

  describe('POST /api/locations/:id/share', () => {
    it('shares location with user → 201', async () => {
      const app = buildApp();
      const res = await request(app)
        .post(`/api/locations/${location.id}/share`)
        .set('Authorization', `Bearer ${token1}`)
        .send({ userId: user2.id });

      expect(res.status).toBe(201);
      expect(res.body.locationId).toBe(location.id);
      expect(res.body.sharedWithUserId).toBe(user2.id);
    });

    it('returns 400 when neither userId nor groupId provided', async () => {
      const app = buildApp();
      const res = await request(app)
        .post(`/api/locations/${location.id}/share`)
        .set('Authorization', `Bearer ${token1}`)
        .send({});

      expect(res.status).toBe(400);
    });

    it('returns 403 when non-owner tries to share', async () => {
      const app = buildApp();
      const res = await request(app)
        .post(`/api/locations/${location.id}/share`)
        .set('Authorization', `Bearer ${token2}`)
        .send({ userId: user1.id });

      expect(res.status).toBe(403);
    });

    it('returns 403 when user has sharing ban', async () => {
      const testDb = getTestDb();
      const deps = buildDeps(testDb);
      // Add sharing ban for user1
      await deps.bans.addSharingBan(user1.id, 'Test ban', user2.id);

      const app = buildApp();
      const res = await request(app)
        .post(`/api/locations/${location.id}/share`)
        .set('Authorization', `Bearer ${token1}`)
        .send({ userId: user2.id });

      expect(res.status).toBe(403);

      // Remove sharing ban to avoid affecting other tests
      const bans = await deps.bans.listSharingBans();
      for (const ban of bans) {
        if (ban.bannedUserId === user1.id) {
          await deps.bans.removeSharingBan(ban.id);
        }
      }
    });
  });

  describe('GET /api/locations/:id/shares', () => {
    it('lists shares for location', async () => {
      const testDb = getTestDb();
      const deps = buildDeps(testDb);
      await deps.shares.shareWithUser(location.id, user1.id, user2.id);

      const app = buildApp();
      const res = await request(app)
        .get(`/api/locations/${location.id}/shares`)
        .set('Authorization', `Bearer ${token1}`);

      expect(res.status).toBe(200);
      expect(res.body.data.length).toBe(1);
      expect(res.body.data[0].sharedWithUserId).toBe(user2.id);
    });
  });

  describe('POST /api/locations/:id/share/direct', () => {
    it('generates a direct link token and URL → 201', async () => {
      const app = buildApp();
      const res = await request(app)
        .post(`/api/locations/${location.id}/share/direct`)
        .set('Authorization', `Bearer ${token1}`)
        .send({});

      expect(res.status).toBe(201);
      expect(res.body.token).toBeTruthy();
      expect(res.body.url).toContain('/share/');
      expect(res.body.url).toContain(res.body.token);
    });

    it('returns 403 when non-owner tries to generate link', async () => {
      const app = buildApp();
      const res = await request(app)
        .post(`/api/locations/${location.id}/share/direct`)
        .set('Authorization', `Bearer ${token2}`)
        .send({});

      expect(res.status).toBe(403);
    });
  });

  describe('GET /api/share/:token', () => {
    it('resolves a direct link token → returns location', async () => {
      // First generate a token
      const app = buildApp();
      const genRes = await request(app)
        .post(`/api/locations/${location.id}/share/direct`)
        .set('Authorization', `Bearer ${token1}`)
        .send({});

      expect(genRes.status).toBe(201);
      const { token } = genRes.body;

      const resolveRes = await request(app)
        .get(`/api/share/${token}`)
        .set('Authorization', `Bearer ${token2}`);

      expect(resolveRes.status).toBe(200);
      expect(resolveRes.body.id).toBe(location.id);
      expect(resolveRes.body.name).toBe(location.name);
    });

    it('returns 404 for unknown token', async () => {
      const app = buildApp();
      const res = await request(app)
        .get('/api/share/nonexistent-token')
        .set('Authorization', `Bearer ${token1}`);

      expect(res.status).toBe(404);
    });
  });

  describe('DELETE /api/locations/:id/share/:shareId', () => {
    it('revokes a share → returns revoked: true', async () => {
      const testDb = getTestDb();
      const deps = buildDeps(testDb);
      const share = await deps.shares.shareWithUser(location.id, user1.id, user2.id);

      const app = buildApp();
      const res = await request(app)
        .delete(`/api/locations/${location.id}/share/${share.id}`)
        .set('Authorization', `Bearer ${token1}`);

      expect(res.status).toBe(200);
      expect(res.body.revoked).toBe(true);
    });
  });

  describe('POST /api/locations/:shareId/hide', () => {
    it('hides a share → 201', async () => {
      const testDb = getTestDb();
      const deps = buildDeps(testDb);
      const share = await deps.shares.shareWithUser(location.id, user1.id, user2.id);

      const app = buildApp();
      const res = await request(app)
        .post(`/api/locations/${share.id}/hide`)
        .set('Authorization', `Bearer ${token2}`)
        .send({ locationId: location.id });

      expect(res.status).toBe(201);
      expect(res.body.hiddenByUserId).toBe(user2.id);
    });
  });

  describe('DELETE /api/locations/:shareId/hide', () => {
    it('unhides a share → returns unhidden: true', async () => {
      const testDb = getTestDb();
      const deps = buildDeps(testDb);
      const share = await deps.shares.shareWithUser(location.id, user1.id, user2.id);
      await deps.shares.hideShare(user2.id, location.id, share.id);

      const app = buildApp();
      const res = await request(app)
        .delete(`/api/locations/${share.id}/hide`)
        .set('Authorization', `Bearer ${token2}`);

      expect(res.status).toBe(200);
      expect(res.body.unhidden).toBe(true);
    });
  });
});
