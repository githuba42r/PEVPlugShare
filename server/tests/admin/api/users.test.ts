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
import { createAdminUserRouter } from '../../../src/admin/api/users';
import { createAdminBanRouter } from '../../../src/admin/api/bans';
import { createAdminSharingBanRouter } from '../../../src/admin/api/sharing-bans';
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

function createAdminApp(repos: Repositories, adminId: string) {
  const app = express();
  app.use(express.json());
  app.use((req: any, _res: any, next: any) => {
    req.userId = adminId;
    req.isAdmin = true;
    next();
  });
  app.use(requireAdmin);
  app.use(createAdminUserRouter(repos));
  app.use(createAdminBanRouter(repos));
  app.use(createAdminSharingBanRouter(repos));
  app.use(errorHandler);
  return app;
}

function createNonAdminApp(repos: Repositories) {
  const app = express();
  app.use(express.json());
  app.use(requireAdmin);
  app.use(createAdminUserRouter(repos));
  app.use(errorHandler);
  return app;
}

describe('Admin API — Users, Bans, and Sharing Bans', () => {
  let db: Knex;
  let repos: Repositories;
  let adminId: string;

  beforeAll(async () => {
    db = await setupTestDb();
    // Create a persistent admin user for FK references
    const userRepo = new PostgresUserRepo(db);
    const admin = await userRepo.create({
      googleId: 'admin-google',
      email: 'admin@test.com',
      displayName: 'Admin',
    });
    adminId = admin.id;
  });

  afterAll(async () => {
    await teardownTestDb();
  });

  beforeEach(async () => {
    repos = buildRepos(db);
    await db('sharing_bans').delete();
    await db('banned_identifiers').delete();
    // Delete non-admin users
    await db('users').whereNot({ id: adminId }).delete();
  });

  // --- Auth ---

  it('returns 403 for non-admin', async () => {
    const app = createNonAdminApp(repos);
    const res = await request(app).get('/users');
    expect(res.status).toBe(403);
  });

  // --- Users ---

  it('GET /users returns paginated list', async () => {
    await repos.users.create({ googleId: 'g1', email: 'a@test.com', displayName: 'User A' });
    await repos.users.create({ googleId: 'g2', email: 'b@test.com', displayName: 'User B' });

    const app = createAdminApp(repos, adminId);
    const res = await request(app).get('/users');
    expect(res.status).toBe(200);
    // 2 test users + admin
    expect(res.body.data.length).toBeGreaterThanOrEqual(2);
    expect(res.body.pagination).toBeDefined();
  });

  it('GET /users?q= searches users', async () => {
    await repos.users.create({ googleId: 'g1', email: 'a@test.com', displayName: 'User A' });
    await repos.users.create({ googleId: 'g2', email: 'b@test.com', displayName: 'User B' });

    const app = createAdminApp(repos, adminId);
    const res = await request(app).get('/users?q=b@test');
    expect(res.status).toBe(200);
    expect(res.body.data).toHaveLength(1);
    expect(res.body.data[0].email).toBe('b@test.com');
  });

  it('GET /users/:id returns user', async () => {
    const user = await repos.users.create({ googleId: 'g1', email: 'a@test.com', displayName: 'User A' });
    const app = createAdminApp(repos, adminId);
    const res = await request(app).get(`/users/${user.id}`);
    expect(res.status).toBe(200);
    expect(res.body.id).toBe(user.id);
    expect(res.body.email).toBe('a@test.com');
  });

  it('GET /users/:id returns 404 for unknown user', async () => {
    const app = createAdminApp(repos, adminId);
    const res = await request(app).get('/users/00000000-0000-0000-0000-000000000000');
    expect(res.status).toBe(404);
  });

  it('PUT /users/:id/admin grants admin', async () => {
    const user = await repos.users.create({ googleId: 'g1', email: 'a@test.com', displayName: 'User A' });
    const app = createAdminApp(repos, adminId);
    const res = await request(app).put(`/users/${user.id}/admin`).send({ isAdmin: true });
    expect(res.status).toBe(200);
    expect(res.body.message).toBe('Admin status updated');

    const updated = await repos.users.getById(user.id);
    expect(updated!.isAdmin).toBe(true);
  });

  it('PUT /users/:id/admin returns 400 if isAdmin not boolean', async () => {
    const user = await repos.users.create({ googleId: 'g1', email: 'a@test.com', displayName: 'User A' });
    const app = createAdminApp(repos, adminId);
    const res = await request(app).put(`/users/${user.id}/admin`).send({ isAdmin: 'yes' });
    expect(res.status).toBe(400);
  });

  it('PUT /users/:id/ban bans user and adds banned identifiers', async () => {
    const user = await repos.users.create({ googleId: 'goog-123', email: 'bad@test.com', displayName: 'Bad User' });
    const app = createAdminApp(repos, adminId);
    const res = await request(app)
      .put(`/users/${user.id}/ban`)
      .send({ reason: 'Violation of terms' });
    expect(res.status).toBe(200);
    expect(res.body.message).toBe('User banned');

    const updated = await repos.users.getById(user.id);
    expect(updated!.isBanned).toBe(true);

    const bans = await repos.bans.listBannedIdentifiers();
    const emailBan = bans.find((b) => b.identifierType === 'EMAIL' && b.identifierValue === 'bad@test.com');
    const googleBan = bans.find((b) => b.identifierType === 'GOOGLE_ID' && b.identifierValue === 'goog-123');
    expect(emailBan).toBeDefined();
    expect(googleBan).toBeDefined();
  });

  it('PUT /users/:id/ban returns 404 for unknown user', async () => {
    const app = createAdminApp(repos, adminId);
    const res = await request(app)
      .put('/users/00000000-0000-0000-0000-000000000000/ban')
      .send({ reason: 'spam' });
    expect(res.status).toBe(404);
  });

  it('PUT /users/:id/unban unbans user', async () => {
    const user = await repos.users.create({ googleId: 'g1', email: 'a@test.com', displayName: 'User A' });
    await repos.users.setBanned(user.id, true, 'test ban', adminId);

    const app = createAdminApp(repos, adminId);
    const res = await request(app).put(`/users/${user.id}/unban`);
    expect(res.status).toBe(200);
    expect(res.body.message).toBe('User unbanned');

    const updated = await repos.users.getById(user.id);
    expect(updated!.isBanned).toBe(false);
  });

  // --- Bans ---

  it('GET /bans returns empty array initially', async () => {
    const app = createAdminApp(repos, adminId);
    const res = await request(app).get('/bans');
    expect(res.status).toBe(200);
    expect(res.body).toEqual([]);
  });

  it('POST /bans creates a ban', async () => {
    const app = createAdminApp(repos, adminId);
    const res = await request(app)
      .post('/bans')
      .send({ type: 'EMAIL', identifier: 'spammer@evil.com', reason: 'spam' });
    expect(res.status).toBe(201);
    expect(res.body.identifierType).toBe('EMAIL');
    expect(res.body.identifierValue).toBe('spammer@evil.com');
  });

  it('POST /bans returns 400 if type missing', async () => {
    const app = createAdminApp(repos, adminId);
    const res = await request(app).post('/bans').send({ identifier: 'test@test.com' });
    expect(res.status).toBe(400);
  });

  it('POST /bans returns 400 for invalid type', async () => {
    const app = createAdminApp(repos, adminId);
    const res = await request(app).post('/bans').send({ type: 'PHONE', identifier: '0400000000' });
    expect(res.status).toBe(400);
  });

  it('GET /bans returns created bans', async () => {
    await repos.bans.addBannedIdentifier('EMAIL', 'a@b.com', 'test', adminId);
    await repos.bans.addBannedIdentifier('GOOGLE_ID', 'goog-abc', 'test', adminId);

    const app = createAdminApp(repos, adminId);
    const res = await request(app).get('/bans');
    expect(res.status).toBe(200);
    expect(res.body).toHaveLength(2);
  });

  it('DELETE /bans/:id removes ban', async () => {
    const ban = await repos.bans.addBannedIdentifier('EMAIL', 'remove@me.com', undefined, adminId);
    const app = createAdminApp(repos, adminId);
    const res = await request(app).delete(`/bans/${ban.id}`);
    expect(res.status).toBe(200);
    expect(res.body.message).toBe('Ban removed');

    const bans = await repos.bans.listBannedIdentifiers();
    expect(bans.find((b) => b.id === ban.id)).toBeUndefined();
  });

  it('DELETE /bans/:id returns 404 for unknown id', async () => {
    const app = createAdminApp(repos, adminId);
    const res = await request(app).delete('/bans/00000000-0000-0000-0000-000000000000');
    expect(res.status).toBe(404);
  });

  // --- Sharing Bans ---

  it('GET /sharing-bans returns empty array initially', async () => {
    const app = createAdminApp(repos, adminId);
    const res = await request(app).get('/sharing-bans');
    expect(res.status).toBe(200);
    expect(res.body).toEqual([]);
  });

  it('POST /users/:id/sharing-ban creates sharing ban', async () => {
    const user = await repos.users.create({ googleId: 'g-share', email: 'share@test.com', displayName: 'Share User' });
    const app = createAdminApp(repos, adminId);
    const res = await request(app)
      .post(`/users/${user.id}/sharing-ban`)
      .send({ reason: 'Abusing sharing' });
    expect(res.status).toBe(201);
    expect(res.body.bannedUserId).toBe(user.id);
    expect(res.body.reason).toBe('Abusing sharing');
  });

  it('POST /users/:id/sharing-ban returns 400 if reason missing', async () => {
    const user = await repos.users.create({ googleId: 'g-share2', email: 'share2@test.com', displayName: 'Share User 2' });
    const app = createAdminApp(repos, adminId);
    const res = await request(app).post(`/users/${user.id}/sharing-ban`).send({});
    expect(res.status).toBe(400);
  });

  it('GET /sharing-bans returns sharing bans', async () => {
    const user = await repos.users.create({ googleId: 'g-share3', email: 'share3@test.com', displayName: 'Share User 3' });
    await repos.bans.addSharingBan(user.id, 'spam', adminId);
    const app = createAdminApp(repos, adminId);
    const res = await request(app).get('/sharing-bans');
    expect(res.status).toBe(200);
    expect(res.body).toHaveLength(1);
    expect(res.body[0].bannedUserId).toBe(user.id);
  });

  it('DELETE /users/:id/sharing-ban removes active sharing ban', async () => {
    const user = await repos.users.create({ googleId: 'g-share4', email: 'share4@test.com', displayName: 'Share User 4' });
    await repos.bans.addSharingBan(user.id, 'spam', adminId);
    const app = createAdminApp(repos, adminId);
    const res = await request(app).delete(`/users/${user.id}/sharing-ban`);
    expect(res.status).toBe(200);
    expect(res.body.message).toBe('Sharing ban removed');

    const hasActive = await repos.bans.hasActiveSharingBan(user.id);
    expect(hasActive).toBe(false);
  });

  it('DELETE /users/:id/sharing-ban returns 404 if no active ban', async () => {
    const user = await repos.users.create({ googleId: 'g-share5', email: 'share5@test.com', displayName: 'Share User 5' });
    const app = createAdminApp(repos, adminId);
    const res = await request(app).delete(`/users/${user.id}/sharing-ban`);
    expect(res.status).toBe(404);
  });
});
