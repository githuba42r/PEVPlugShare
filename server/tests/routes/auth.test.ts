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
import { PostgresFriendRepo } from '../../src/repositories/postgres/friend-repo';
import { PostgresGroupRepo } from '../../src/repositories/postgres/group-repo';
import { PostgresShareRepo } from '../../src/repositories/postgres/share-repo';
import { PostgresSuggestionRepo } from '../../src/repositories/postgres/suggestion-repo';
import { PostgresDeepLinkRepo } from '../../src/repositories/postgres/deep-link-repo';
import type { Repositories } from '../../src/repositories/factory';
import { createAuthRouter } from '../../src/routes/auth';
import { errorHandler } from '../../src/middleware/error-handler';

// Mock Google OAuth so tests don't require real tokens
jest.mock('../../src/auth/google-oauth');
import { verifyGoogleToken } from '../../src/auth/google-oauth';
const mockVerifyGoogleToken = verifyGoogleToken as jest.MockedFunction<typeof verifyGoogleToken>;

// Mock config to provide stable jwtSecret and googleClientId
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

describe('POST /auth/google', () => {
  let db: Knex;

  beforeAll(async () => {
    db = await setupTestDb();
  });

  afterAll(async () => {
    await teardownTestDb();
  });

  beforeEach(async () => {
    // Clean up between tests
    await db('server_location_photos').delete();
    await db('server_locations').delete();
    await db('notifications').delete();
    await db('sharing_bans').delete();
    await db('banned_identifiers').delete();
    await db('users').delete();
    mockVerifyGoogleToken.mockReset();
  });

  function buildApp() {
    const app = express();
    app.use(express.json());
    const repos = buildRepos(getTestDb());
    app.use(createAuthRouter(repos));
    app.use(errorHandler);
    return app;
  }

  it('returns 400 when idToken is missing', async () => {
    const app = buildApp();
    const res = await request(app).post('/auth/google').send({});
    expect(res.status).toBe(400);
    expect(res.body.error).toMatch(/idToken/i);
  });

  it('returns 401 when Google token is invalid', async () => {
    mockVerifyGoogleToken.mockResolvedValue(null);
    const app = buildApp();
    const res = await request(app).post('/auth/google').send({ idToken: 'bad-token' });
    expect(res.status).toBe(401);
  });

  it('creates a new user on first login and returns JWT', async () => {
    mockVerifyGoogleToken.mockResolvedValue({
      googleId: 'g-newuser',
      email: 'newuser@example.com',
      displayName: 'New User',
      avatarUrl: null,
    });

    const app = buildApp();
    const res = await request(app).post('/auth/google').send({ idToken: 'valid-token' });

    expect(res.status).toBe(200);
    expect(res.body.token).toBeTruthy();
    expect(res.body.user.email).toBe('newuser@example.com');
    expect(res.body.user.displayName).toBe('New User');
    expect(res.body.user.id).toBeTruthy();

    // Verify user was actually created in DB
    const repos = buildRepos(db);
    const user = await repos.users.getByGoogleId('g-newuser');
    expect(user).not.toBeNull();
  });

  it('returns existing user on second login without creating duplicate', async () => {
    mockVerifyGoogleToken.mockResolvedValue({
      googleId: 'g-existing',
      email: 'existing@example.com',
      displayName: 'Existing User',
      avatarUrl: null,
    });

    const app = buildApp();

    // First login
    const res1 = await request(app).post('/auth/google').send({ idToken: 'valid-token' });
    expect(res1.status).toBe(200);
    const userId1 = res1.body.user.id;

    // Second login
    const res2 = await request(app).post('/auth/google').send({ idToken: 'valid-token' });
    expect(res2.status).toBe(200);
    expect(res2.body.user.id).toBe(userId1);

    // Only one user in DB
    const count = await db('users').count('* as c').first();
    expect(Number((count as any).c)).toBe(1);
  });

  it('returns 403 for banned email', async () => {
    const repos = buildRepos(db);
    await repos.bans.addBannedIdentifier('EMAIL', 'banned@example.com', 'test ban');

    mockVerifyGoogleToken.mockResolvedValue({
      googleId: 'g-banned',
      email: 'banned@example.com',
      displayName: 'Banned User',
      avatarUrl: null,
    });

    const app = buildApp();
    const res = await request(app).post('/auth/google').send({ idToken: 'valid-token' });
    expect(res.status).toBe(403);
  });
});
