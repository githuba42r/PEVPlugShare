import { setupTestDb, teardownTestDb, getTestDb } from '../setup';
import { PostgresUserRepo } from '../../src/repositories/postgres/user-repo';
import type { Knex } from 'knex';

describe('PostgresUserRepo', () => {
  let db: Knex;
  let repo: PostgresUserRepo;
  let adminUserId: string;

  beforeAll(async () => {
    db = await setupTestDb();
    // Create a persistent admin user for FK references in ban tests
    const adminRepo = new PostgresUserRepo(db);
    const admin = await adminRepo.create({ googleId: 'admin-google', email: 'admin@example.com', displayName: 'Admin' });
    adminUserId = admin.id;
  });

  afterAll(async () => {
    await teardownTestDb();
  });

  beforeEach(async () => {
    repo = new PostgresUserRepo(getTestDb());
    // Preserve the admin user for FK references; delete test users only
    await db('users').whereNot({ id: adminUserId }).delete();
  });

  const baseInput = {
    googleId: 'google-123',
    email: 'test@example.com',
    displayName: 'Test User',
    avatarUrl: null,
  };

  it('creates a user and returns it', async () => {
    const user = await repo.create(baseInput);
    expect(user.id).toBeTruthy();
    expect(user.googleId).toBe('google-123');
    expect(user.email).toBe('test@example.com');
    expect(user.displayName).toBe('Test User');
    expect(user.avatarUrl).toBeNull();
    expect(user.isAdmin).toBe(false);
    expect(user.isBanned).toBe(false);
    expect(user.createdAt).toBeInstanceOf(Date);
    expect(user.lastSeenAt).toBeInstanceOf(Date);
  });

  it('gets user by id', async () => {
    const created = await repo.create(baseInput);
    const found = await repo.getById(created.id);
    expect(found).not.toBeNull();
    expect(found!.id).toBe(created.id);
  });

  it('returns null for unknown id', async () => {
    const found = await repo.getById('00000000-0000-0000-0000-000000000000');
    expect(found).toBeNull();
  });

  it('gets user by googleId', async () => {
    const created = await repo.create(baseInput);
    const found = await repo.getByGoogleId('google-123');
    expect(found).not.toBeNull();
    expect(found!.id).toBe(created.id);
  });

  it('returns null for unknown googleId', async () => {
    const found = await repo.getByGoogleId('nonexistent');
    expect(found).toBeNull();
  });

  it('gets user by email', async () => {
    const created = await repo.create(baseInput);
    const found = await repo.getByEmail('test@example.com');
    expect(found).not.toBeNull();
    expect(found!.id).toBe(created.id);
  });

  it('returns null for unknown email', async () => {
    const found = await repo.getByEmail('nobody@example.com');
    expect(found).toBeNull();
  });

  it('updates last seen', async () => {
    const user = await repo.create(baseInput);
    const originalTime = user.lastSeenAt.getTime();
    await new Promise((r) => setTimeout(r, 10));
    await repo.updateLastSeen(user.id);
    const updated = await repo.getById(user.id);
    expect(updated!.lastSeenAt.getTime()).toBeGreaterThanOrEqual(originalTime);
  });

  it('sets admin status', async () => {
    const user = await repo.create(baseInput);
    await repo.setAdmin(user.id, true);
    const updated = await repo.getById(user.id);
    expect(updated!.isAdmin).toBe(true);
    await repo.setAdmin(user.id, false);
    const reset = await repo.getById(user.id);
    expect(reset!.isAdmin).toBe(false);
  });

  it('bans and unbans a user', async () => {
    const user = await repo.create(baseInput);
    await repo.setBanned(user.id, true, 'bad behavior', adminUserId);
    const banned = await repo.getById(user.id);
    expect(banned!.isBanned).toBe(true);
    expect(banned!.banReason).toBe('bad behavior');
    expect(banned!.bannedBy).toBe(adminUserId);
    expect(banned!.bannedAt).toBeInstanceOf(Date);

    await repo.setBanned(user.id, false);
    const unbanned = await repo.getById(user.id);
    expect(unbanned!.isBanned).toBe(false);
    expect(unbanned!.banReason).toBeNull();
    expect(unbanned!.bannedAt).toBeNull();
  });

  it('bans without optional fields', async () => {
    const user = await repo.create(baseInput);
    await repo.setBanned(user.id, true);
    const banned = await repo.getById(user.id);
    expect(banned!.isBanned).toBe(true);
    expect(banned!.banReason).toBeNull();
    expect(banned!.bannedBy).toBeNull();
  });

  it('counts users', async () => {
    // admin user already in db
    expect(await repo.count()).toBe(1);
    await repo.create(baseInput);
    expect(await repo.count()).toBe(2);
    await repo.create({ ...baseInput, googleId: 'g2', email: 'two@example.com' });
    expect(await repo.count()).toBe(3);
  });

  it('searches users by email', async () => {
    await repo.create(baseInput);
    await repo.create({ googleId: 'g2', email: 'other@example.com', displayName: 'Other' });

    const result = await repo.search('test@');
    expect(result.data.length).toBe(1);
    expect(result.data[0].email).toBe('test@example.com');
    expect(result.pagination.hasMore).toBe(false);
    expect(result.pagination.cursor).toBeNull();
  });

  it('searches users by displayName', async () => {
    await repo.create(baseInput);
    await repo.create({ googleId: 'g2', email: 'other@example.com', displayName: 'Jane Doe' });

    const result = await repo.search('Jane');
    expect(result.data.length).toBe(1);
    expect(result.data[0].displayName).toBe('Jane Doe');
  });

  it('paginates search results', async () => {
    for (let i = 0; i < 5; i++) {
      await repo.create({
        googleId: `g${i}`,
        email: `user${i}@example.com`,
        displayName: `User ${i}`,
      });
    }

    const page1 = await repo.search('user', undefined, 3);
    expect(page1.data.length).toBe(3);
    expect(page1.pagination.hasMore).toBe(true);
    expect(page1.pagination.cursor).not.toBeNull();

    const page2 = await repo.search('user', page1.pagination.cursor!, 3);
    expect(page2.data.length).toBe(2);
    expect(page2.pagination.hasMore).toBe(false);
    expect(page2.pagination.cursor).toBeNull();
  });

  it('search returns empty for no matches', async () => {
    await repo.create(baseInput);
    const result = await repo.search('zzznomatch');
    expect(result.data).toHaveLength(0);
    expect(result.pagination.hasMore).toBe(false);
  });
});
