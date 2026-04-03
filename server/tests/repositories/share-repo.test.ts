import { setupTestDb, teardownTestDb, getTestDb } from '../setup';
import { PostgresUserRepo } from '../../src/repositories/postgres/user-repo';
import { PostgresLocationRepo } from '../../src/repositories/postgres/location-repo';
import { PostgresGroupRepo } from '../../src/repositories/postgres/group-repo';
import { PostgresShareRepo } from '../../src/repositories/postgres/share-repo';
import type { Knex } from 'knex';

describe('PostgresShareRepo', () => {
  let db: Knex;
  let repo: PostgresShareRepo;
  let userId1: string;
  let userId2: string;
  let locationId: string;
  let groupId: string;

  beforeAll(async () => {
    db = await setupTestDb();
    const userRepo = new PostgresUserRepo(db);
    const locationRepo = new PostgresLocationRepo(db);
    const groupRepo = new PostgresGroupRepo(db);

    const user1 = await userRepo.create({
      googleId: 'share-user1-google',
      email: 'share-user1@example.com',
      displayName: 'Share User 1',
    });
    userId1 = user1.id;

    const user2 = await userRepo.create({
      googleId: 'share-user2-google',
      email: 'share-user2@example.com',
      displayName: 'Share User 2',
    });
    userId2 = user2.id;

    const loc = await locationRepo.create({
      ownerId: userId1,
      name: 'Shared Location',
      latitude: -33.8688,
      longitude: 151.2093,
    });
    locationId = loc.id;

    const group = await groupRepo.create(userId1, 'Test Group');
    groupId = group.id;
  });

  afterAll(async () => {
    await teardownTestDb();
  });

  beforeEach(async () => {
    repo = new PostgresShareRepo(getTestDb());
    await db('hidden_shares').delete();
    await db('location_shares').delete();
  });

  // --- shareWithUser ---
  it('shareWithUser creates a share record', async () => {
    const share = await repo.shareWithUser(locationId, userId1, userId2);
    expect(share.id).toBeTruthy();
    expect(share.locationId).toBe(locationId);
    expect(share.sharedByUserId).toBe(userId1);
    expect(share.sharedWithUserId).toBe(userId2);
    expect(share.sharedWithGroupId).toBeNull();
    expect(share.canEdit).toBe(false);
    expect(share.createdAt).toBeInstanceOf(Date);
    expect(share.expiresAt).toBeNull();
  });

  // --- shareWithGroup ---
  it('shareWithGroup creates a share record', async () => {
    const share = await repo.shareWithGroup(locationId, userId1, groupId);
    expect(share.id).toBeTruthy();
    expect(share.sharedWithGroupId).toBe(groupId);
    expect(share.sharedWithUserId).toBeNull();
  });

  // --- revokeShare ---
  it('revokeShare deletes the share and returns true', async () => {
    const share = await repo.shareWithUser(locationId, userId1, userId2);
    const result = await repo.revokeShare(share.id);
    expect(result).toBe(true);

    const fetched = await db('location_shares').where({ id: share.id }).first();
    expect(fetched).toBeUndefined();
  });

  it('revokeShare returns false for unknown id', async () => {
    const result = await repo.revokeShare('00000000-0000-0000-0000-000000000000');
    expect(result).toBe(false);
  });

  // --- getSharesForLocation ---
  it('getSharesForLocation returns shares for a location', async () => {
    await repo.shareWithUser(locationId, userId1, userId2);
    await repo.shareWithGroup(locationId, userId1, groupId);

    const result = await repo.getSharesForLocation(locationId);
    expect(result.data).toHaveLength(2);
    expect(result.pagination.hasMore).toBe(false);
  });

  it('getSharesForLocation paginates', async () => {
    for (let i = 0; i < 4; i++) {
      await repo.shareWithUser(locationId, userId1, userId2);
    }

    const page1 = await repo.getSharesForLocation(locationId, undefined, 3);
    expect(page1.data).toHaveLength(3);
    expect(page1.pagination.hasMore).toBe(true);
    expect(page1.pagination.cursor).not.toBeNull();

    const page2 = await repo.getSharesForLocation(locationId, page1.pagination.cursor!, 3);
    expect(page2.data).toHaveLength(1);
    expect(page2.pagination.hasMore).toBe(false);
  });

  // --- getLocationsSharedWithUser ---
  it('getLocationsSharedWithUser returns locations shared directly with user', async () => {
    await repo.shareWithUser(locationId, userId1, userId2);

    const result = await repo.getLocationsSharedWithUser(userId2, []);
    expect(result.data).toHaveLength(1);
    expect(result.data[0].id).toBe(locationId);
  });

  it('getLocationsSharedWithUser returns locations shared via group', async () => {
    await repo.shareWithGroup(locationId, userId1, groupId);

    const result = await repo.getLocationsSharedWithUser(userId2, [groupId]);
    expect(result.data).toHaveLength(1);
    expect(result.data[0].id).toBe(locationId);
  });

  it('getLocationsSharedWithUser excludes hidden shares', async () => {
    const share = await repo.shareWithUser(locationId, userId1, userId2);
    await repo.hideShare(userId2, locationId, share.id);

    const result = await repo.getLocationsSharedWithUser(userId2, []);
    expect(result.data).toHaveLength(0);
  });

  it('getLocationsSharedWithUser shows unhidden shares again', async () => {
    const share = await repo.shareWithUser(locationId, userId1, userId2);
    await repo.hideShare(userId2, locationId, share.id);
    await repo.unhideShare(userId2, share.id);

    const result = await repo.getLocationsSharedWithUser(userId2, []);
    expect(result.data).toHaveLength(1);
  });

  it('getLocationsSharedWithUser excludes inactive shares', async () => {
    const share = await repo.shareWithUser(locationId, userId1, userId2);
    await repo.deactivateSharesForLocation(locationId);

    const result = await repo.getLocationsSharedWithUser(userId2, []);
    expect(result.data).toHaveLength(0);
  });

  it('getLocationsSharedWithUser returns empty when no groups provided and no direct share', async () => {
    await repo.shareWithGroup(locationId, userId1, groupId);

    const result = await repo.getLocationsSharedWithUser(userId2, []);
    expect(result.data).toHaveLength(0);
  });

  // --- deactivateSharesForLocation ---
  it('deactivateSharesForLocation sets is_active false for all shares', async () => {
    await repo.shareWithUser(locationId, userId1, userId2);
    await repo.shareWithGroup(locationId, userId1, groupId);

    await repo.deactivateSharesForLocation(locationId);

    const rows = await db('location_shares').where({ location_id: locationId });
    expect(rows.every((r: any) => !r.is_active)).toBe(true);
  });

  // --- isSharedWithUser ---
  it('isSharedWithUser returns true for direct share', async () => {
    await repo.shareWithUser(locationId, userId1, userId2);
    const result = await repo.isSharedWithUser(locationId, userId2, []);
    expect(result).toBe(true);
  });

  it('isSharedWithUser returns true for group share', async () => {
    await repo.shareWithGroup(locationId, userId1, groupId);
    const result = await repo.isSharedWithUser(locationId, userId2, [groupId]);
    expect(result).toBe(true);
  });

  it('isSharedWithUser returns false when not shared', async () => {
    const result = await repo.isSharedWithUser(locationId, userId2, []);
    expect(result).toBe(false);
  });

  it('isSharedWithUser returns false when share is inactive', async () => {
    await repo.shareWithUser(locationId, userId1, userId2);
    await repo.deactivateSharesForLocation(locationId);
    const result = await repo.isSharedWithUser(locationId, userId2, []);
    expect(result).toBe(false);
  });

  // --- hideShare / unhideShare / getHiddenShareIds ---
  it('hideShare creates a hidden_shares record', async () => {
    const share = await repo.shareWithUser(locationId, userId1, userId2);
    const hidden = await repo.hideShare(userId2, locationId, share.id);
    expect(hidden.id).toBeTruthy();
    expect(hidden.locationId).toBe(locationId);
    expect(hidden.hiddenByUserId).toBe(userId2);
    expect(hidden.createdAt).toBeInstanceOf(Date);
  });

  it('unhideShare removes the hidden record and returns true', async () => {
    const share = await repo.shareWithUser(locationId, userId1, userId2);
    await repo.hideShare(userId2, locationId, share.id);

    const result = await repo.unhideShare(userId2, share.id);
    expect(result).toBe(true);

    const ids = await repo.getHiddenShareIds(userId2);
    expect(ids).not.toContain(share.id);
  });

  it('unhideShare returns false for unknown record', async () => {
    const result = await repo.unhideShare(userId2, '00000000-0000-0000-0000-000000000000');
    expect(result).toBe(false);
  });

  it('getHiddenShareIds returns share ids for user', async () => {
    const share1 = await repo.shareWithUser(locationId, userId1, userId2);
    const share2 = await repo.shareWithUser(locationId, userId1, userId2);
    await repo.hideShare(userId2, locationId, share1.id);
    await repo.hideShare(userId2, locationId, share2.id);

    const ids = await repo.getHiddenShareIds(userId2);
    expect(ids).toContain(share1.id);
    expect(ids).toContain(share2.id);
  });

  // --- unhideByLocationVersion ---
  it('unhideByLocationVersion removes all hidden shares for a location', async () => {
    const share = await repo.shareWithUser(locationId, userId1, userId2);
    await repo.hideShare(userId2, locationId, share.id);

    await repo.unhideByLocationVersion(locationId);

    const ids = await repo.getHiddenShareIds(userId2);
    expect(ids).toHaveLength(0);
  });
});
