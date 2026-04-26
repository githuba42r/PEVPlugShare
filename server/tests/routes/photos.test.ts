import express from 'express';
import request from 'supertest';
import * as os from 'os';
import * as path from 'path';
import * as fs from 'fs/promises';
import type { Knex } from 'knex';
import { setupTestDb, teardownTestDb, getTestDb } from '../setup';
import { PostgresUserRepo } from '../../src/repositories/postgres/user-repo';
import { PostgresBanRepo } from '../../src/repositories/postgres/ban-repo';
import { PostgresRateLimitRepo } from '../../src/repositories/postgres/rate-limit-repo';
import { PostgresNotificationRepo } from '../../src/repositories/postgres/notification-repo';
import { PostgresLocationRepo } from '../../src/repositories/postgres/location-repo';
import { PostgresPhotoRepo } from '../../src/repositories/postgres/photo-repo';
import { PostgresFriendRepo } from '../../src/repositories/postgres/friend-repo';
import { PostgresGroupRepo } from '../../src/repositories/postgres/group-repo';
import { PostgresShareRepo } from '../../src/repositories/postgres/share-repo';
import { PostgresSuggestionRepo } from '../../src/repositories/postgres/suggestion-repo';
import { PostgresDeepLinkRepo } from '../../src/repositories/postgres/deep-link-repo';
import type { Repositories } from '../../src/repositories/factory';
import { createPhotoRouter } from '../../src/routes/photos';
import { createAuthMiddleware } from '../../src/auth/middleware';
import { signToken } from '../../src/auth/jwt';
import { errorHandler } from '../../src/middleware/error-handler';
import { LocalStorage } from '../../src/storage/local-storage';
import type { User, Location } from '../../src/models';

const TEST_JWT_SECRET = 'test-secret';

// Mock config
jest.mock('../../src/config', () => ({
  config: {
    jwtSecret: 'test-secret',
    googleClientId: 'test-client-id',
    adminSetupEmail: undefined,
    maxUploadSizeMb: 5,
  },
}));

function buildRepos(db: Knex): Repositories {
  return {
    users: new PostgresUserRepo(db),
    bans: new PostgresBanRepo(db),
    rateLimits: new PostgresRateLimitRepo(db),
    notifications: new PostgresNotificationRepo(db),
    locations: new PostgresLocationRepo(db),
    photos: new PostgresPhotoRepo(db),
    friends: new PostgresFriendRepo(db),
    groups: new PostgresGroupRepo(db),
    shares: new PostgresShareRepo(db),
    suggestions: new PostgresSuggestionRepo(db),
    deepLinks: new PostgresDeepLinkRepo(db),
  };
}

describe('Photo routes', () => {
  let db: Knex;
  let testUser: User;
  let testLocation: Location;
  let token: string;
  let tempDir: string;
  let storage: LocalStorage;

  beforeAll(async () => {
    db = await setupTestDb();
    tempDir = await fs.mkdtemp(path.join(os.tmpdir(), 'photos-test-'));
    storage = new LocalStorage(tempDir);

    const repos = buildRepos(db);
    testUser = await repos.users.create({
      googleId: 'g-photo-owner',
      email: 'photoowner@example.com',
      displayName: 'Photo Owner',
    });
    token = signToken(
      { userId: testUser.id, email: testUser.email, isAdmin: false, hasSharingBan: false },
      TEST_JWT_SECRET,
    );

    testLocation = await repos.locations.create({
      ownerId: testUser.id,
      name: 'Photo Location',
      latitude: -33.8688,
      longitude: 151.2093,
    });
  });

  afterAll(async () => {
    await teardownTestDb();
    await fs.rm(tempDir, { recursive: true, force: true });
  });

  beforeEach(async () => {
    await db('server_location_photos').delete();
    // Don't delete the testLocation since we reuse it across tests
  });

  function buildApp() {
    const app = express();
    app.use(express.json());
    app.use(createAuthMiddleware(TEST_JWT_SECRET));
    const repos = buildRepos(getTestDb());
    app.use(createPhotoRouter(repos, storage));
    app.use(errorHandler);
    return app;
  }

  describe('POST /api/locations/:id/photos', () => {
    it('uploads photo and returns 201', async () => {
      const app = buildApp();
      const res = await request(app)
        .post(`/api/locations/${testLocation.id}/photos`)
        .set('Authorization', `Bearer ${token}`)
        .attach('photo', Buffer.from('fake-image-data'), 'test.jpg');

      expect(res.status).toBe(201);
      expect(res.body.id).toBeTruthy();
      expect(res.body.locationId).toBe(testLocation.id);
      expect(res.body.filePath).toBeTruthy();
    });

    it('returns 400 when no file is attached', async () => {
      const app = buildApp();
      const res = await request(app)
        .post(`/api/locations/${testLocation.id}/photos`)
        .set('Authorization', `Bearer ${token}`);

      expect(res.status).toBe(400);
    });

    it('returns 404 for non-existent location', async () => {
      const app = buildApp();
      const res = await request(app)
        .post('/api/locations/00000000-0000-0000-0000-000000000000/photos')
        .set('Authorization', `Bearer ${token}`)
        .attach('photo', Buffer.from('fake'), 'test.jpg');

      expect(res.status).toBe(404);
    });
  });

  describe('GET /api/photos/:photoId/image', () => {
    it('returns the image file', async () => {
      // Create a photo first
      const uploadApp = buildApp();
      const uploadRes = await request(uploadApp)
        .post(`/api/locations/${testLocation.id}/photos`)
        .set('Authorization', `Bearer ${token}`)
        .attach('photo', Buffer.from('fake-image-content'), 'test.jpg');

      expect(uploadRes.status).toBe(201);
      const photoId = uploadRes.body.id;

      const app = buildApp();
      const res = await request(app)
        .get(`/api/photos/${photoId}/image`)
        .set('Authorization', `Bearer ${token}`);

      expect(res.status).toBe(200);
      expect(res.body).toBeTruthy();
    });

    it('returns 304 when ETag matches', async () => {
      // Create a photo first
      const uploadApp = buildApp();
      const uploadRes = await request(uploadApp)
        .post(`/api/locations/${testLocation.id}/photos`)
        .set('Authorization', `Bearer ${token}`)
        .attach('photo', Buffer.from('fake-image-etag'), 'test.jpg');

      expect(uploadRes.status).toBe(201);
      const photoId = uploadRes.body.id;

      // Get image to retrieve ETag
      const app = buildApp();
      const firstRes = await request(app)
        .get(`/api/photos/${photoId}/image`)
        .set('Authorization', `Bearer ${token}`);

      const etag = firstRes.headers['etag'];
      expect(etag).toBeTruthy();

      // Request again with If-None-Match
      const secondRes = await request(app)
        .get(`/api/photos/${photoId}/image`)
        .set('Authorization', `Bearer ${token}`)
        .set('If-None-Match', etag);

      expect(secondRes.status).toBe(304);
    });
  });

  describe('DELETE /api/locations/:id/photos/:photoId', () => {
    it('deletes photo successfully', async () => {
      // Upload first
      const uploadApp = buildApp();
      const uploadRes = await request(uploadApp)
        .post(`/api/locations/${testLocation.id}/photos`)
        .set('Authorization', `Bearer ${token}`)
        .attach('photo', Buffer.from('to-delete'), 'test.jpg');

      expect(uploadRes.status).toBe(201);
      const photoId = uploadRes.body.id;

      const app = buildApp();
      const res = await request(app)
        .delete(`/api/locations/${testLocation.id}/photos/${photoId}`)
        .set('Authorization', `Bearer ${token}`);

      expect(res.status).toBe(200);
      expect(res.body.message).toBe('Photo deleted');

      // Verify it's gone from DB
      const repos = buildRepos(db);
      const photo = await repos.photos.getById(photoId);
      expect(photo).toBeNull();
    });
  });
});
