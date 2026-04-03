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
import type { Repositories } from '../../../src/repositories/factory';
import { requireAdmin } from '../../../src/auth/admin-middleware';
import { createAdminStatsRouter } from '../../../src/admin/api/stats';
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
  app.use(createAdminStatsRouter(repos));
  app.use(errorHandler);
  return app;
}

describe('Admin API — Stats', () => {
  let db: Knex;
  let repos: Repositories;

  beforeAll(async () => {
    db = await setupTestDb();
  });

  afterAll(async () => {
    await teardownTestDb();
  });

  beforeEach(async () => {
    repos = buildRepos(db);
    await db('rate_limits').delete();
    await db('server_location_photos').delete();
    await db('server_locations').delete();
    await db('sharing_bans').delete();
    await db('banned_identifiers').delete();
    await db('users').delete();
  });

  it('GET /stats returns zero counts on empty db', async () => {
    const app = createAdminApp(repos);
    const res = await request(app).get('/stats');
    expect(res.status).toBe(200);
    expect(res.body.users).toBe(0);
    expect(res.body.locations).toBe(0);
    expect(res.body.rateLimits).toBe(0);
  });

  it('GET /stats returns correct counts', async () => {
    const user1 = await repos.users.create({ googleId: 'g1', email: 'u1@test.com', displayName: 'User 1' });
    const user2 = await repos.users.create({ googleId: 'g2', email: 'u2@test.com', displayName: 'User 2' });

    await repos.locations.create({
      ownerId: user1.id,
      name: 'Loc A',
      latitude: -33.8,
      longitude: 151.2,
      socketTypes: [],
      publicPrivate: 'PUBLIC',
    });
    await repos.locations.create({
      ownerId: user2.id,
      name: 'Loc B',
      latitude: -34.0,
      longitude: 151.0,
      socketTypes: [],
      publicPrivate: 'PUBLIC',
    });

    await repos.rateLimits.create({ scope: 'GLOBAL', endpoint: '/test', maxRequests: 100, windowSeconds: 60 });

    const app = createAdminApp(repos);
    const res = await request(app).get('/stats');
    expect(res.status).toBe(200);
    expect(res.body.users).toBe(2);
    expect(res.body.locations).toBe(2);
    expect(res.body.rateLimits).toBe(1);
  });
});
