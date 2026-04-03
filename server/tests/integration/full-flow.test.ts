import request from 'supertest';
import * as os from 'os';
import * as path from 'path';
import * as fs from 'fs/promises';
import type { Knex } from 'knex';
import knex from 'knex';
import { createApp } from '../../src/app';
import { createRepositories } from '../../src/repositories/factory';
import { LocalStorage } from '../../src/storage/local-storage';
import { MemoryCache } from '../../src/cache/memory-cache';

// Mock Google OAuth so tests don't require real tokens
jest.mock('../../src/auth/google-oauth');
import { verifyGoogleToken } from '../../src/auth/google-oauth';
const mockVerifyGoogleToken = verifyGoogleToken as jest.MockedFunction<typeof verifyGoogleToken>;

// Mock config for stable values
jest.mock('../../src/config', () => ({
  config: {
    port: 8080,
    dbBackend: 'postgres',
    databaseUrl: '',
    redisUrl: undefined,
    googleClientId: 'test-client-id',
    jwtSecret: 'integration-test-secret',
    storagePath: '/tmp/test-storage',
    serverBaseUrl: 'http://localhost:8080',
    logLevel: 'silent',
    maxUploadSizeMb: 5,
    adminSetupEmail: undefined,
    sessionSecret: 'test-session-secret',
  },
}));

describe('Full flow integration', () => {
  let db: Knex;
  let tmpDir: string;
  let token: string;
  let locationId: string;

  beforeAll(async () => {
    // Create SQLite in-memory DB, run migrations
    db = knex({
      client: 'better-sqlite3',
      connection: { filename: ':memory:' },
      useNullAsDefault: true,
      migrations: {
        directory: './migrations',
        extension: 'ts',
        loadExtensions: ['.ts'],
      },
    });
    await db.migrate.latest();

    tmpDir = await fs.mkdtemp(path.join(os.tmpdir(), 'pevplugshare-test-'));
  });

  afterAll(async () => {
    await db.destroy();
    await fs.rm(tmpDir, { recursive: true, force: true });
  });

  function buildApp() {
    const repos = createRepositories(db);
    const storage = new LocalStorage(tmpDir);
    const cache = new MemoryCache();
    return createApp({ repos, storage, cache });
  }

  it('POST /auth/google — returns JWT and user profile', async () => {
    mockVerifyGoogleToken.mockResolvedValue({
      googleId: 'g-integration-test',
      email: 'integration@example.com',
      displayName: 'Integration User',
      avatarUrl: null,
    });

    const app = buildApp();
    const res = await request(app)
      .post('/auth/google')
      .send({ idToken: 'valid-google-token' });

    expect(res.status).toBe(200);
    expect(res.body.token).toBeTruthy();
    expect(res.body.user.email).toBe('integration@example.com');
    expect(res.body.user.displayName).toBe('Integration User');

    token = res.body.token;
  });

  it('GET /api/users/me — returns user profile with valid JWT', async () => {
    const app = buildApp();
    const res = await request(app)
      .get('/api/users/me')
      .set('Authorization', `Bearer ${token}`);

    expect(res.status).toBe(200);
    expect(res.body.email).toBe('integration@example.com');
    expect(res.body.displayName).toBe('Integration User');
    expect(res.body.id).toBeTruthy();
  });

  it('GET /api/users/me — returns 401 without JWT', async () => {
    const app = buildApp();
    const res = await request(app).get('/api/users/me');
    expect(res.status).toBe(401);
  });

  it('POST /api/locations — creates location (201)', async () => {
    const app = buildApp();
    const res = await request(app)
      .post('/api/locations')
      .set('Authorization', `Bearer ${token}`)
      .send({
        name: 'Test Charging Spot',
        latitude: -33.8688,
        longitude: 151.2093,
      });

    expect(res.status).toBe(201);
    expect(res.body.name).toBe('Test Charging Spot');
    expect(res.body.latitude).toBeCloseTo(-33.8688, 4);
    expect(res.body.longitude).toBeCloseTo(151.2093, 4);
    expect(res.body.id).toBeTruthy();
    expect(res.body.version).toBe(1);

    locationId = res.body.id;
  });

  it('GET /api/locations — list includes created location', async () => {
    const app = buildApp();
    const res = await request(app)
      .get('/api/locations')
      .set('Authorization', `Bearer ${token}`);

    expect(res.status).toBe(200);
    expect(res.body.data).toBeInstanceOf(Array);
    expect(res.body.data.length).toBeGreaterThanOrEqual(1);
    expect(res.body.data.some((l: any) => l.id === locationId)).toBe(true);
  });

  it('GET /api/locations/:id — returns location detail', async () => {
    const app = buildApp();
    const res = await request(app)
      .get(`/api/locations/${locationId}`)
      .set('Authorization', `Bearer ${token}`);

    expect(res.status).toBe(200);
    expect(res.body.id).toBe(locationId);
    expect(res.body.name).toBe('Test Charging Spot');
  });

  it('PUT /api/locations/:id — updates and increments version', async () => {
    const app = buildApp();
    const res = await request(app)
      .put(`/api/locations/${locationId}`)
      .set('Authorization', `Bearer ${token}`)
      .send({ name: 'Updated Charging Spot' });

    expect(res.status).toBe(200);
    expect(res.body.name).toBe('Updated Charging Spot');
    expect(res.body.version).toBe(2);
  });

  it('DELETE /api/locations/:id — soft deletes location', async () => {
    const app = buildApp();
    const res = await request(app)
      .delete(`/api/locations/${locationId}`)
      .set('Authorization', `Bearer ${token}`)
      .send({ reason: 'test cleanup' });

    expect(res.status).toBe(200);
    expect(res.body.message).toMatch(/deleted/i);
  });

  it('GET /api/locations/:id — returns 404 after soft delete', async () => {
    const app = buildApp();
    const res = await request(app)
      .get(`/api/locations/${locationId}`)
      .set('Authorization', `Bearer ${token}`);

    expect(res.status).toBe(404);
  });

  it('GET /api/notifications — returns empty list when no notifications', async () => {
    const app = buildApp();
    const res = await request(app)
      .get('/api/notifications')
      .set('Authorization', `Bearer ${token}`);

    expect(res.status).toBe(200);
    expect(res.body.data).toBeInstanceOf(Array);
    expect(res.body.data.length).toBe(0);
  });

  it('GET /health — works without auth', async () => {
    const app = buildApp();
    const res = await request(app).get('/health');

    expect(res.status).toBe(200);
    expect(res.body.status).toBe('ok');
  });
});
