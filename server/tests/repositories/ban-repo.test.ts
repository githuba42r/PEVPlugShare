import { setupTestDb, teardownTestDb, getTestDb } from '../setup';
import { PostgresBanRepo } from '../../src/repositories/postgres/ban-repo';
import { PostgresUserRepo } from '../../src/repositories/postgres/user-repo';
import type { Knex } from 'knex';

describe('PostgresBanRepo', () => {
  let db: Knex;
  let repo: PostgresBanRepo;
  let userId1: string;
  let userId2: string;

  beforeAll(async () => {
    db = await setupTestDb();
    // Create users for FK references
    const userRepo = new PostgresUserRepo(db);
    const u1 = await userRepo.create({ googleId: 'g1', email: 'u1@example.com', displayName: 'User 1' });
    const u2 = await userRepo.create({ googleId: 'g2', email: 'u2@example.com', displayName: 'User 2' });
    userId1 = u1.id;
    userId2 = u2.id;
  });

  afterAll(async () => {
    await teardownTestDb();
  });

  beforeEach(async () => {
    repo = new PostgresBanRepo(getTestDb());
    await db('sharing_bans').delete();
    await db('banned_identifiers').delete();
  });

  describe('BannedIdentifiers', () => {
    it('adds a banned identifier', async () => {
      const bi = await repo.addBannedIdentifier('EMAIL', 'spam@example.com', 'spammer', userId1);
      expect(bi.id).toBeTruthy();
      expect(bi.identifierType).toBe('EMAIL');
      expect(bi.identifierValue).toBe('spam@example.com');
      expect(bi.reason).toBe('spammer');
      expect(bi.bannedByUserId).toBe(userId1);
      expect(bi.createdAt).toBeInstanceOf(Date);
    });

    it('removes a banned identifier', async () => {
      const bi = await repo.addBannedIdentifier('EMAIL', 'spam@example.com');
      const removed = await repo.removeBannedIdentifier(bi.id);
      expect(removed).toBe(true);
      const found = await repo.isBanned('EMAIL', 'spam@example.com');
      expect(found).toBe(false);
    });

    it('returns false when removing nonexistent identifier', async () => {
      const removed = await repo.removeBannedIdentifier('00000000-0000-0000-0000-000000000000');
      expect(removed).toBe(false);
    });

    it('checks if email is banned', async () => {
      expect(await repo.isBanned('EMAIL', 'banned@example.com')).toBe(false);
      await repo.addBannedIdentifier('EMAIL', 'banned@example.com');
      expect(await repo.isBanned('EMAIL', 'banned@example.com')).toBe(true);
    });

    it('checks if google_id is banned', async () => {
      expect(await repo.isBanned('GOOGLE_ID', 'google-bad-123')).toBe(false);
      await repo.addBannedIdentifier('GOOGLE_ID', 'google-bad-123');
      expect(await repo.isBanned('GOOGLE_ID', 'google-bad-123')).toBe(true);
    });

    it('lists all banned identifiers', async () => {
      expect(await repo.listBannedIdentifiers()).toHaveLength(0);
      await repo.addBannedIdentifier('EMAIL', 'a@example.com');
      await repo.addBannedIdentifier('GOOGLE_ID', 'g-id-1');
      const list = await repo.listBannedIdentifiers();
      expect(list).toHaveLength(2);
    });

    it('adds banned identifier without optional fields', async () => {
      const bi = await repo.addBannedIdentifier('EMAIL', 'noinfo@example.com');
      expect(bi.reason).toBeNull();
      expect(bi.bannedByUserId).toBe('');
    });
  });

  describe('SharingBans', () => {
    it('adds a sharing ban', async () => {
      const ban = await repo.addSharingBan(userId1, 'spamming shares', userId2);
      expect(ban.id).toBeTruthy();
      expect(ban.bannedUserId).toBe(userId1);
      expect(ban.bannedByUserId).toBe(userId2);
      expect(ban.reason).toBe('spamming shares');
      expect(ban.createdAt).toBeInstanceOf(Date);
    });

    it('removes a sharing ban', async () => {
      const ban = await repo.addSharingBan(userId1, 'test', userId2);
      const removed = await repo.removeSharingBan(ban.id);
      expect(removed).toBe(true);
      const active = await repo.hasActiveSharingBan(userId1);
      expect(active).toBe(false);
    });

    it('returns false when removing nonexistent sharing ban', async () => {
      const removed = await repo.removeSharingBan('00000000-0000-0000-0000-000000000000');
      expect(removed).toBe(false);
    });

    it('detects active sharing ban without expiry', async () => {
      expect(await repo.hasActiveSharingBan(userId1)).toBe(false);
      await repo.addSharingBan(userId1, 'reason', userId2);
      expect(await repo.hasActiveSharingBan(userId1)).toBe(true);
    });

    it('detects active sharing ban with future expiry', async () => {
      const future = new Date(Date.now() + 3600 * 1000);
      await repo.addSharingBan(userId1, 'reason', userId2, future);
      expect(await repo.hasActiveSharingBan(userId1)).toBe(true);
    });

    it('does not detect expired sharing ban', async () => {
      const past = new Date(Date.now() - 3600 * 1000);
      await repo.addSharingBan(userId1, 'reason', userId2, past);
      expect(await repo.hasActiveSharingBan(userId1)).toBe(false);
    });

    it('lists all sharing bans', async () => {
      expect(await repo.listSharingBans()).toHaveLength(0);
      await repo.addSharingBan(userId1, 'reason 1', userId2);
      await repo.addSharingBan(userId2, 'reason 2', userId1);
      const list = await repo.listSharingBans();
      expect(list).toHaveLength(2);
    });
  });
});
