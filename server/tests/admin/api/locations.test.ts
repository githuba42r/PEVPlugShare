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
import { PostgresFriendRepo } from '../../../src/repositories/postgres/friend-repo';
import { PostgresGroupRepo } from '../../../src/repositories/postgres/group-repo';
import { PostgresShareRepo } from '../../../src/repositories/postgres/share-repo';
import { PostgresSuggestionRepo } from '../../../src/repositories/postgres/suggestion-repo';
import { PostgresDeepLinkRepo } from '../../../src/repositories/postgres/deep-link-repo';
import type { Repositories } from '../../../src/repositories/factory';
import { requireAdmin } from '../../../src/auth/admin-middleware';
import { createAdminLocationRouter } from '../../../src/admin/api/locations';
import { createAdminGroupRouter } from '../../../src/admin/api/groups';
import { errorHandler } from '../../../src/middleware/error-handler';

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

function createAdminApp(repos: Repositories) {
  const app = express();
  app.use(express.json());
  app.use((req: any, _res: any, next: any) => {
    req.userId = 'admin-user-id';
    req.isAdmin = true;
    next();
  });
  app.use(requireAdmin);
  app.use(createAdminLocationRouter(repos));
  app.use(createAdminGroupRouter(repos));
  app.use(errorHandler);
  return app;
}

describe('Admin API — Locations and Groups', () => {
  let db: Knex;
  let repos: Repositories;
  let ownerId: string;

  beforeAll(async () => {
    db = await setupTestDb();
    const userRepo = new PostgresUserRepo(db);
    const owner = await userRepo.create({ googleId: 'g-owner', email: 'owner@test.com', displayName: 'Owner' });
    ownerId = owner.id;
  });

  afterAll(async () => {
    await teardownTestDb();
  });

  beforeEach(async () => {
    repos = buildRepos(db);
    await db('server_location_photos').delete();
    await db('server_locations').delete();
  });

  it('GET /locations returns empty list initially', async () => {
    const app = createAdminApp(repos);
    const res = await request(app).get('/locations');
    expect(res.status).toBe(200);
    expect(res.body.data).toHaveLength(0);
    expect(res.body.pagination).toBeDefined();
  });

  it('GET /locations returns all locations including deleted', async () => {
    await repos.locations.create({
      ownerId,
      name: 'Location A',
      latitude: -33.8,
      longitude: 151.2,
      socketTypes: [],
      publicPrivate: 'PUBLIC',
    });
    const loc2 = await repos.locations.create({
      ownerId,
      name: 'Location B',
      latitude: -34.0,
      longitude: 151.0,
      socketTypes: [],
      publicPrivate: 'PUBLIC',
    });
    await repos.locations.softDelete(loc2.id, 'test');

    const app = createAdminApp(repos);
    const res = await request(app).get('/locations');
    expect(res.status).toBe(200);
    expect(res.body.data).toHaveLength(2);
  });

  it('GET /locations/:id returns location with photos', async () => {
    const loc = await repos.locations.create({
      ownerId,
      name: 'Test Location',
      latitude: -33.8,
      longitude: 151.2,
      socketTypes: [],
      publicPrivate: 'PUBLIC',
    });

    const app = createAdminApp(repos);
    const res = await request(app).get(`/locations/${loc.id}`);
    expect(res.status).toBe(200);
    expect(res.body.id).toBe(loc.id);
    expect(res.body.name).toBe('Test Location');
    expect(res.body.photos).toBeDefined();
    expect(Array.isArray(res.body.photos)).toBe(true);
  });

  it('GET /locations/:id returns 404 for unknown location', async () => {
    const app = createAdminApp(repos);
    const res = await request(app).get('/locations/00000000-0000-0000-0000-000000000000');
    expect(res.status).toBe(404);
  });

  it('DELETE /locations/:id soft-deletes location', async () => {
    const loc = await repos.locations.create({
      ownerId,
      name: 'Delete Me',
      latitude: -33.8,
      longitude: 151.2,
      socketTypes: [],
      publicPrivate: 'PUBLIC',
    });

    const app = createAdminApp(repos);
    const res = await request(app).delete(`/locations/${loc.id}`).send({ reason: 'Policy violation' });
    expect(res.status).toBe(200);
    expect(res.body.message).toBe('Location deleted');

    const updated = await repos.locations.getById(loc.id);
    expect(updated!.isDeleted).toBe(true);
    expect(updated!.deletedReason).toBe('Policy violation');
  });

  it('DELETE /locations/:id uses default reason when none provided', async () => {
    const loc = await repos.locations.create({
      ownerId,
      name: 'Delete Me Default',
      latitude: -33.8,
      longitude: 151.2,
      socketTypes: [],
      publicPrivate: 'PUBLIC',
    });

    const app = createAdminApp(repos);
    const res = await request(app).delete(`/locations/${loc.id}`);
    expect(res.status).toBe(200);
    const updated = await repos.locations.getById(loc.id);
    expect(updated!.deletedReason).toBe('Deleted by admin');
  });

  it('PUT /locations/:id/block-sharing marks location as deleted', async () => {
    const loc = await repos.locations.create({
      ownerId,
      name: 'Block Me',
      latitude: -33.8,
      longitude: 151.2,
      socketTypes: [],
      publicPrivate: 'PUBLIC',
    });

    const app = createAdminApp(repos);
    const res = await request(app).put(`/locations/${loc.id}/block-sharing`);
    expect(res.status).toBe(200);
    expect(res.body.message).toBe('Sharing blocked');

    const updated = await repos.locations.getById(loc.id);
    expect(updated!.isDeleted).toBe(true);
    expect(updated!.deletedReason).toBe('Sharing blocked by admin');
  });

  // --- Groups ---

  it('GET /groups returns empty list when no groups exist', async () => {
    const app = createAdminApp(repos);
    const res = await request(app).get('/groups');
    expect(res.status).toBe(200);
    expect(res.body.data).toEqual([]);
    expect(res.body.pagination.hasMore).toBe(false);
  });

  it('GET /groups/:id returns 404 for non-existent group', async () => {
    const app = createAdminApp(repos);
    const res = await request(app).get('/groups/some-id');
    expect(res.status).toBe(404);
  });

  it('DELETE /groups/:id returns 404 for non-existent group', async () => {
    const app = createAdminApp(repos);
    const res = await request(app).delete('/groups/some-id');
    expect(res.status).toBe(404);
  });
});
