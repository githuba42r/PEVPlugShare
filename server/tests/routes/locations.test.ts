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
import { createLocationRouter } from '../../src/routes/locations';
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

describe('Location routes', () => {
  let db: Knex;
  let testUser: User;
  let otherUser: User;
  let token: string;
  let otherToken: string;

  beforeAll(async () => {
    db = await setupTestDb();
    const repos = buildRepos(db);

    testUser = await repos.users.create({
      googleId: 'g-loc-owner',
      email: 'locowner@example.com',
      displayName: 'Location Owner',
    });
    token = signToken(
      { userId: testUser.id, email: testUser.email, isAdmin: false, hasSharingBan: false },
      TEST_JWT_SECRET,
    );

    otherUser = await repos.users.create({
      googleId: 'g-loc-other',
      email: 'locother@example.com',
      displayName: 'Other User',
    });
    otherToken = signToken(
      { userId: otherUser.id, email: otherUser.email, isAdmin: false, hasSharingBan: false },
      TEST_JWT_SECRET,
    );
  });

  afterAll(async () => {
    await teardownTestDb();
  });

  beforeEach(async () => {
    await db('server_location_photos').delete();
    await db('server_locations').delete();
  });

  function buildApp() {
    const app = express();
    app.use(express.json());
    app.use(createAuthMiddleware(TEST_JWT_SECRET));
    const repos = buildRepos(getTestDb());
    app.use(createLocationRouter(repos));
    app.use(errorHandler);
    return app;
  }

  describe('POST /api/locations', () => {
    it('creates a location and returns 201', async () => {
      const app = buildApp();
      const res = await request(app)
        .post('/api/locations')
        .set('Authorization', `Bearer ${token}`)
        .send({ name: 'Test Spot', latitude: -33.8688, longitude: 151.2093 });

      expect(res.status).toBe(201);
      expect(res.body.name).toBe('Test Spot');
      expect(res.body.ownerId).toBe(testUser.id);
      expect(res.body.id).toBeTruthy();
    });

    it('returns 400 when name is missing', async () => {
      const app = buildApp();
      const res = await request(app)
        .post('/api/locations')
        .set('Authorization', `Bearer ${token}`)
        .send({ latitude: -33.8688, longitude: 151.2093 });

      expect(res.status).toBe(400);
    });

    it('returns 400 when latitude is missing', async () => {
      const app = buildApp();
      const res = await request(app)
        .post('/api/locations')
        .set('Authorization', `Bearer ${token}`)
        .send({ name: 'No Lat', longitude: 151.2093 });

      expect(res.status).toBe(400);
    });
  });

  describe('GET /api/locations', () => {
    it('returns owned locations with pagination', async () => {
      const repos = buildRepos(db);
      await repos.locations.create({
        ownerId: testUser.id,
        name: 'My Place',
        latitude: -33.0,
        longitude: 151.0,
      });
      await repos.locations.create({
        ownerId: otherUser.id,
        name: 'Their Place',
        latitude: -34.0,
        longitude: 152.0,
      });

      const app = buildApp();
      const res = await request(app)
        .get('/api/locations')
        .set('Authorization', `Bearer ${token}`);

      expect(res.status).toBe(200);
      expect(res.body.data.length).toBe(1);
      expect(res.body.data[0].name).toBe('My Place');
      expect(res.body.pagination).toHaveProperty('cursor');
      expect(res.body.pagination).toHaveProperty('hasMore');
    });
  });

  describe('GET /api/locations/:id', () => {
    it('returns location for owner', async () => {
      const repos = buildRepos(db);
      const loc = await repos.locations.create({
        ownerId: testUser.id,
        name: 'Owned',
        latitude: -33.0,
        longitude: 151.0,
      });

      const app = buildApp();
      const res = await request(app)
        .get(`/api/locations/${loc.id}`)
        .set('Authorization', `Bearer ${token}`);

      expect(res.status).toBe(200);
      expect(res.body.id).toBe(loc.id);
    });

    it('returns 404 for non-existent location', async () => {
      const app = buildApp();
      const res = await request(app)
        .get('/api/locations/00000000-0000-0000-0000-000000000000')
        .set('Authorization', `Bearer ${token}`);

      expect(res.status).toBe(404);
    });

    it('returns 403 for non-owner', async () => {
      const repos = buildRepos(db);
      const loc = await repos.locations.create({
        ownerId: otherUser.id,
        name: 'Theirs',
        latitude: -34.0,
        longitude: 152.0,
      });

      const app = buildApp();
      const res = await request(app)
        .get(`/api/locations/${loc.id}`)
        .set('Authorization', `Bearer ${token}`);

      expect(res.status).toBe(403);
    });
  });

  describe('PUT /api/locations/:id', () => {
    it('updates location and increments version', async () => {
      const repos = buildRepos(db);
      const loc = await repos.locations.create({
        ownerId: testUser.id,
        name: 'Original',
        latitude: -33.0,
        longitude: 151.0,
      });

      const app = buildApp();
      const res = await request(app)
        .put(`/api/locations/${loc.id}`)
        .set('Authorization', `Bearer ${token}`)
        .send({ name: 'Updated' });

      expect(res.status).toBe(200);
      expect(res.body.name).toBe('Updated');
      expect(res.body.version).toBeGreaterThan(loc.version);
    });

    it('returns 403 for non-owner', async () => {
      const repos = buildRepos(db);
      const loc = await repos.locations.create({
        ownerId: otherUser.id,
        name: 'Theirs',
        latitude: -34.0,
        longitude: 152.0,
      });

      const app = buildApp();
      const res = await request(app)
        .put(`/api/locations/${loc.id}`)
        .set('Authorization', `Bearer ${token}`)
        .send({ name: 'Stolen' });

      expect(res.status).toBe(403);
    });
  });

  describe('DELETE /api/locations/:id', () => {
    it('soft-deletes location', async () => {
      const repos = buildRepos(db);
      const loc = await repos.locations.create({
        ownerId: testUser.id,
        name: 'To Delete',
        latitude: -33.0,
        longitude: 151.0,
      });

      const app = buildApp();
      const res = await request(app)
        .delete(`/api/locations/${loc.id}`)
        .set('Authorization', `Bearer ${token}`)
        .send({ reason: 'Gone' });

      expect(res.status).toBe(200);
      expect(res.body.message).toBe('Location deleted');

      // Verify soft-deleted
      const updated = await repos.locations.getById(loc.id);
      expect(updated?.isDeleted).toBe(true);
    });
  });

  describe('GET /api/locations/nearby', () => {
    it('returns locations within radius', async () => {
      const repos = buildRepos(db);
      // Create a location at Sydney CBD
      await repos.locations.create({
        ownerId: testUser.id,
        name: 'Near Sydney',
        latitude: -33.8688,
        longitude: 151.2093,
      });
      // Create a location far away (Melbourne)
      await repos.locations.create({
        ownerId: testUser.id,
        name: 'Melbourne',
        latitude: -37.8136,
        longitude: 144.9631,
      });

      const app = buildApp();
      // Search near Sydney with 10km radius
      const res = await request(app)
        .get('/api/locations/nearby?lat=-33.8688&lon=151.2093&radius=10')
        .set('Authorization', `Bearer ${token}`);

      expect(res.status).toBe(200);
      expect(res.body.data.length).toBeGreaterThanOrEqual(1);
      const names = res.body.data.map((l: any) => l.name);
      expect(names).toContain('Near Sydney');
      expect(names).not.toContain('Melbourne');
    });

    it('returns 400 when lat is missing', async () => {
      const app = buildApp();
      const res = await request(app)
        .get('/api/locations/nearby?lon=151.2093&radius=10')
        .set('Authorization', `Bearer ${token}`);

      expect(res.status).toBe(400);
    });
  });
});
