import { setupTestDb, teardownTestDb, getTestDb } from '../setup';
import { PostgresUserRepo } from '../../src/repositories/postgres/user-repo';
import { PostgresLocationRepo } from '../../src/repositories/postgres/location-repo';
import { PostgresDeepLinkRepo } from '../../src/repositories/postgres/deep-link-repo';
import type { Knex } from 'knex';

describe('PostgresDeepLinkRepo', () => {
  let db: Knex;
  let repo: PostgresDeepLinkRepo;
  let userId: string;
  let locationId: string;

  beforeAll(async () => {
    db = await setupTestDb();
    const userRepo = new PostgresUserRepo(db);
    const locationRepo = new PostgresLocationRepo(db);

    const user = await userRepo.create({
      googleId: 'deeplink-user-google',
      email: 'deeplink-user@example.com',
      displayName: 'Deep Link User',
    });
    userId = user.id;

    const loc = await locationRepo.create({
      ownerId: userId,
      name: 'Deep Link Location',
      latitude: -33.8688,
      longitude: 151.2093,
    });
    locationId = loc.id;
  });

  afterAll(async () => {
    await teardownTestDb();
  });

  beforeEach(async () => {
    repo = new PostgresDeepLinkRepo(getTestDb());
    await db('direct_share_tokens').delete();
  });

  let tokenCounter = 0;
  function uniqueToken(): string {
    return `test-token-${Date.now()}-${++tokenCounter}`;
  }

  // --- createToken ---
  it('creates a token with defaults', async () => {
    const token = uniqueToken();
    const result = await repo.createToken(locationId, userId, token);
    expect(result.id).toBeTruthy();
    expect(result.token).toBe(token);
    expect(result.locationId).toBe(locationId);
    expect(result.createdByUserId).toBe(userId);
    expect(result.maxUses).toBeNull();
    expect(result.useCount).toBe(0);
    expect(result.expiresAt).toBeNull();
    expect(result.createdAt).toBeInstanceOf(Date);
  });

  it('creates a token with maxUses and expiresAt', async () => {
    const token = uniqueToken();
    const expiresAt = new Date(Date.now() + 86400000); // +1 day
    const result = await repo.createToken(locationId, userId, token, true, 5, expiresAt);
    expect(result.maxUses).toBe(5);
    expect(result.expiresAt).toBeInstanceOf(Date);
  });

  // --- getByToken ---
  it('getByToken returns the token', async () => {
    const token = uniqueToken();
    await repo.createToken(locationId, userId, token);

    const found = await repo.getByToken(token);
    expect(found).not.toBeNull();
    expect(found!.token).toBe(token);
  });

  it('getByToken returns null for unknown token', async () => {
    const found = await repo.getByToken('nonexistent-token');
    expect(found).toBeNull();
  });

  it('getByToken returns null for expired token', async () => {
    const token = uniqueToken();
    const expiredAt = new Date(Date.now() - 1000); // already expired
    await repo.createToken(locationId, userId, token, false, undefined, expiredAt);

    const found = await repo.getByToken(token);
    expect(found).toBeNull();
  });

  it('getByToken returns null when use count reaches max_uses', async () => {
    const token = uniqueToken();
    const created = await repo.createToken(locationId, userId, token, false, 1);

    // Manually set use_count to max
    await db('direct_share_tokens').where({ id: created.id }).update({ use_count: 1 });

    const found = await repo.getByToken(token);
    expect(found).toBeNull();
  });

  it('getByToken returns token when use count is below max_uses', async () => {
    const token = uniqueToken();
    await repo.createToken(locationId, userId, token, false, 5);

    const found = await repo.getByToken(token);
    expect(found).not.toBeNull();
  });

  it('getByToken returns token with null max_uses regardless of use count', async () => {
    const token = uniqueToken();
    const created = await repo.createToken(locationId, userId, token);

    await db('direct_share_tokens').where({ id: created.id }).update({ use_count: 1000 });

    const found = await repo.getByToken(token);
    expect(found).not.toBeNull();
  });

  // --- incrementUseCount ---
  it('incrementUseCount increments the use_count', async () => {
    const token = uniqueToken();
    const created = await repo.createToken(locationId, userId, token);
    expect(created.useCount).toBe(0);

    await repo.incrementUseCount(created.id);
    await repo.incrementUseCount(created.id);

    const row = await db('direct_share_tokens').where({ id: created.id }).first();
    expect(row.use_count).toBe(2);
  });

  // --- delete ---
  it('delete removes the token and returns true', async () => {
    const token = uniqueToken();
    const created = await repo.createToken(locationId, userId, token);

    const result = await repo.delete(created.id);
    expect(result).toBe(true);

    const found = await repo.getByToken(token);
    expect(found).toBeNull();
  });

  it('delete returns false for unknown id', async () => {
    const result = await repo.delete('00000000-0000-0000-0000-000000000000');
    expect(result).toBe(false);
  });
});
