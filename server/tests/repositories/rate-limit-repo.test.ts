import { setupTestDb, teardownTestDb, getTestDb } from '../setup';
import { PostgresRateLimitRepo } from '../../src/repositories/postgres/rate-limit-repo';
import type { Knex } from 'knex';

describe('PostgresRateLimitRepo', () => {
  let db: Knex;
  let repo: PostgresRateLimitRepo;

  beforeAll(async () => {
    db = await setupTestDb();
  });

  afterAll(async () => {
    await teardownTestDb();
  });

  beforeEach(async () => {
    repo = new PostgresRateLimitRepo(getTestDb());
    await db('rate_limits').delete();
  });

  const globalInput = {
    scope: 'GLOBAL' as const,
    scopeId: null,
    endpoint: '/api/test',
    maxRequests: 100,
    windowSeconds: 3600,
  };

  it('creates a rate limit and returns it', async () => {
    const rl = await repo.create(globalInput);
    expect(rl.id).toBeTruthy();
    expect(rl.scope).toBe('GLOBAL');
    expect(rl.endpoint).toBe('/api/test');
    expect(rl.maxRequests).toBe(100);
    expect(rl.windowSeconds).toBe(3600);
    expect(rl.createdAt).toBeInstanceOf(Date);
    expect(rl.updatedAt).toBeInstanceOf(Date);
  });

  it('gets rate limit by id', async () => {
    const rl = await repo.create(globalInput);
    const found = await repo.getById(rl.id);
    expect(found).not.toBeNull();
    expect(found!.id).toBe(rl.id);
  });

  it('returns null for unknown id', async () => {
    const found = await repo.getById('00000000-0000-0000-0000-000000000000');
    expect(found).toBeNull();
  });

  it('updates a rate limit', async () => {
    const rl = await repo.create(globalInput);
    const updated = await repo.update(rl.id, { maxRequests: 50, windowSeconds: 1800 });
    expect(updated.maxRequests).toBe(50);
    expect(updated.windowSeconds).toBe(1800);
  });

  it('deletes a rate limit', async () => {
    const rl = await repo.create(globalInput);
    const deleted = await repo.delete(rl.id);
    expect(deleted).toBe(true);
    const found = await repo.getById(rl.id);
    expect(found).toBeNull();
  });

  it('returns false when deleting nonexistent record', async () => {
    const deleted = await repo.delete('00000000-0000-0000-0000-000000000000');
    expect(deleted).toBe(false);
  });

  it('gets all rate limits', async () => {
    await repo.create(globalInput);
    await repo.create({ ...globalInput, endpoint: '/api/other' });
    const all = await repo.getAll();
    expect(all.length).toBe(2);
  });

  it('gets by scope', async () => {
    await repo.create(globalInput);
    await repo.create({ scope: 'USER', scopeId: 'user-1', endpoint: '/api/test', maxRequests: 200, windowSeconds: 3600 });

    const global = await repo.getByScope('GLOBAL');
    expect(global.length).toBe(1);
    expect(global[0].scope).toBe('GLOBAL');

    const user = await repo.getByScope('USER', 'user-1');
    expect(user.length).toBe(1);
    expect(user[0].scopeId).toBe('user-1');
  });

  describe('resolve() tiered logic', () => {
    it('returns null when no rate limits match', async () => {
      const result = await repo.resolve('user-1', '/api/test', []);
      expect(result).toBeNull();
    });

    it('returns GLOBAL rate limit as fallback', async () => {
      await repo.create({ scope: 'GLOBAL', scopeId: null, endpoint: '*', maxRequests: 1000, windowSeconds: 3600 });
      const result = await repo.resolve('user-1', '/api/test', []);
      expect(result).not.toBeNull();
      expect(result!.scope).toBe('GLOBAL');
      expect(result!.maxRequests).toBe(1000);
    });

    it('GROUP overrides GLOBAL', async () => {
      await repo.create({ scope: 'GLOBAL', scopeId: null, endpoint: '*', maxRequests: 1000, windowSeconds: 3600 });
      await repo.create({ scope: 'GROUP', scopeId: 'group-1', endpoint: '*', maxRequests: 500, windowSeconds: 3600 });

      const result = await repo.resolve('user-1', '/api/test', ['group-1']);
      expect(result!.scope).toBe('GROUP');
      expect(result!.maxRequests).toBe(500);
    });

    it('USER overrides GROUP and GLOBAL', async () => {
      await repo.create({ scope: 'GLOBAL', scopeId: null, endpoint: '*', maxRequests: 1000, windowSeconds: 3600 });
      await repo.create({ scope: 'GROUP', scopeId: 'group-1', endpoint: '*', maxRequests: 500, windowSeconds: 3600 });
      await repo.create({ scope: 'USER', scopeId: 'user-1', endpoint: '*', maxRequests: 200, windowSeconds: 3600 });

      const result = await repo.resolve('user-1', '/api/test', ['group-1']);
      expect(result!.scope).toBe('USER');
      expect(result!.maxRequests).toBe(200);
    });

    it('selects most restrictive GROUP limit', async () => {
      await repo.create({ scope: 'GROUP', scopeId: 'group-1', endpoint: '*', maxRequests: 500, windowSeconds: 3600 });
      await repo.create({ scope: 'GROUP', scopeId: 'group-2', endpoint: '*', maxRequests: 100, windowSeconds: 3600 });

      const result = await repo.resolve('user-1', '/api/test', ['group-1', 'group-2']);
      expect(result!.scope).toBe('GROUP');
      expect(result!.maxRequests).toBe(100);
    });

    it('exact endpoint match preferred over wildcard for USER', async () => {
      await repo.create({ scope: 'USER', scopeId: 'user-1', endpoint: '*', maxRequests: 1000, windowSeconds: 3600 });
      await repo.create({ scope: 'USER', scopeId: 'user-1', endpoint: '/api/test', maxRequests: 50, windowSeconds: 3600 });

      const result = await repo.resolve('user-1', '/api/test', []);
      expect(result!.endpoint).toBe('/api/test');
      expect(result!.maxRequests).toBe(50);
    });

    it('returns GLOBAL wildcard when no specific endpoint match', async () => {
      await repo.create({ scope: 'GLOBAL', scopeId: null, endpoint: '*', maxRequests: 1000, windowSeconds: 3600 });

      const result = await repo.resolve('user-1', '/api/nonexistent', []);
      expect(result!.scope).toBe('GLOBAL');
      expect(result!.maxRequests).toBe(1000);
    });
  });
});
