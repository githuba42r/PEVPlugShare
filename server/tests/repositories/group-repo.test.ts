import { setupTestDb, teardownTestDb, getTestDb } from '../setup';
import { PostgresGroupRepo } from '../../src/repositories/postgres/group-repo';
import { PostgresUserRepo } from '../../src/repositories/postgres/user-repo';
import type { Knex } from 'knex';
import type { User } from '../../src/models';

describe('PostgresGroupRepo', () => {
  let db: Knex;
  let repo: PostgresGroupRepo;
  let user1: User;
  let user2: User;
  let user3: User;

  beforeAll(async () => {
    db = await setupTestDb();
    const userRepo = new PostgresUserRepo(db);
    user1 = await userRepo.create({ googleId: 'g1', email: 'user1@example.com', displayName: 'User One' });
    user2 = await userRepo.create({ googleId: 'g2', email: 'user2@example.com', displayName: 'User Two' });
    user3 = await userRepo.create({ googleId: 'g3', email: 'user3@example.com', displayName: 'User Three' });
  });

  afterAll(async () => {
    await teardownTestDb();
  });

  beforeEach(async () => {
    repo = new PostgresGroupRepo(getTestDb());
    await db('group_invitations').delete();
    await db('friend_group_members').delete();
    await db('friend_groups').delete();
  });

  // ─── Group CRUD ────────────────────────────────────────────────────────────

  it('creates a group and returns it', async () => {
    const group = await repo.create(user1.id, 'My Group', 'A test group');
    expect(group.id).toBeTruthy();
    expect(group.ownerId).toBe(user1.id);
    expect(group.name).toBe('My Group');
    expect(group.description).toBe('A test group');
    expect(group.createdAt).toBeInstanceOf(Date);
    expect(group.updatedAt).toBeInstanceOf(Date);
  });

  it('creates a group without description', async () => {
    const group = await repo.create(user1.id, 'No Desc Group');
    expect(group.description).toBeNull();
  });

  it('gets a group by id', async () => {
    const created = await repo.create(user1.id, 'Test Group');
    const found = await repo.getById(created.id);
    expect(found).not.toBeNull();
    expect(found!.id).toBe(created.id);
    expect(found!.name).toBe('Test Group');
  });

  it('returns null for unknown group id', async () => {
    const found = await repo.getById('00000000-0000-0000-0000-000000000000');
    expect(found).toBeNull();
  });

  it('updates group name and description', async () => {
    const group = await repo.create(user1.id, 'Original Name', 'Old desc');
    const updated = await repo.update(group.id, { name: 'New Name', description: 'New desc' });
    expect(updated.name).toBe('New Name');
    expect(updated.description).toBe('New desc');
  });

  it('updates only group name', async () => {
    const group = await repo.create(user1.id, 'Old Name', 'Keep this desc');
    const updated = await repo.update(group.id, { name: 'Updated Name' });
    expect(updated.name).toBe('Updated Name');
    expect(updated.description).toBe('Keep this desc');
  });

  it('deletes a group and returns true', async () => {
    const group = await repo.create(user1.id, 'To Delete');
    const deleted = await repo.delete(group.id);
    expect(deleted).toBe(true);
    const found = await repo.getById(group.id);
    expect(found).toBeNull();
  });

  it('returns false when deleting non-existent group', async () => {
    const deleted = await repo.delete('00000000-0000-0000-0000-000000000000');
    expect(deleted).toBe(false);
  });

  // ─── getByUser ──────────────────────────────────────────────────────────────

  it('returns groups where user is owner', async () => {
    await repo.create(user1.id, 'Owned Group');
    const result = await repo.getByUser(user1.id);
    expect(result.data.length).toBe(1);
    expect(result.data[0].ownerId).toBe(user1.id);
  });

  it('returns groups where user is a member (not owner)', async () => {
    const group = await repo.create(user1.id, 'Owner Group');
    await repo.addMember(group.id, user2.id, 'MEMBER');

    const result = await repo.getByUser(user2.id);
    expect(result.data.length).toBe(1);
    expect(result.data[0].id).toBe(group.id);
  });

  it('returns both owned and member groups without duplicates', async () => {
    const ownedGroup = await repo.create(user1.id, 'User1 Owned');
    const otherGroup = await repo.create(user2.id, 'User2 Owned');
    await repo.addMember(otherGroup.id, user1.id, 'MEMBER');

    const result = await repo.getByUser(user1.id);
    expect(result.data.length).toBe(2);
    const ids = result.data.map((g) => g.id);
    expect(ids).toContain(ownedGroup.id);
    expect(ids).toContain(otherGroup.id);
  });

  // ─── listAll ────────────────────────────────────────────────────────────────

  it('lists all groups with pagination', async () => {
    for (let i = 0; i < 5; i++) {
      await repo.create(user1.id, `Group ${i}`);
    }

    const page1 = await repo.listAll(undefined, 3);
    expect(page1.data.length).toBe(3);
    expect(page1.pagination.hasMore).toBe(true);
    expect(page1.pagination.cursor).not.toBeNull();

    const page2 = await repo.listAll(page1.pagination.cursor!, 3);
    expect(page2.data.length).toBe(2);
    expect(page2.pagination.hasMore).toBe(false);
    expect(page2.pagination.cursor).toBeNull();
  });

  // ─── Members ────────────────────────────────────────────────────────────────

  it('adds a member and returns it', async () => {
    const group = await repo.create(user1.id, 'Test Group');
    const member = await repo.addMember(group.id, user2.id, 'MEMBER');
    expect(member.groupId).toBe(group.id);
    expect(member.userId).toBe(user2.id);
    expect(member.role).toBe('MEMBER');
    expect(member.joinedAt).toBeInstanceOf(Date);
  });

  it('adds an admin member', async () => {
    const group = await repo.create(user1.id, 'Test Group');
    const member = await repo.addMember(group.id, user2.id, 'ADMIN');
    expect(member.role).toBe('ADMIN');
  });

  it('gets all members of a group', async () => {
    const group = await repo.create(user1.id, 'Test Group');
    await repo.addMember(group.id, user2.id, 'MEMBER');
    await repo.addMember(group.id, user3.id, 'ADMIN');

    const members = await repo.getMembers(group.id);
    expect(members.length).toBe(2);
    const userIds = members.map((m) => m.userId);
    expect(userIds).toContain(user2.id);
    expect(userIds).toContain(user3.id);
  });

  it('removes a member and returns true', async () => {
    const group = await repo.create(user1.id, 'Test Group');
    await repo.addMember(group.id, user2.id, 'MEMBER');

    const removed = await repo.removeMember(group.id, user2.id);
    expect(removed).toBe(true);

    const members = await repo.getMembers(group.id);
    expect(members.length).toBe(0);
  });

  it('returns false when removing a non-existent member', async () => {
    const group = await repo.create(user1.id, 'Test Group');
    const removed = await repo.removeMember(group.id, user2.id);
    expect(removed).toBe(false);
  });

  it('sets member role', async () => {
    const group = await repo.create(user1.id, 'Test Group');
    await repo.addMember(group.id, user2.id, 'MEMBER');
    const updated = await repo.setMemberRole(group.id, user2.id, 'ADMIN');
    expect(updated.role).toBe('ADMIN');
  });

  // ─── isMember / isAdmin ─────────────────────────────────────────────────────

  it('isMember returns true for owner', async () => {
    const group = await repo.create(user1.id, 'Test Group');
    expect(await repo.isMember(group.id, user1.id)).toBe(true);
  });

  it('isMember returns true for member', async () => {
    const group = await repo.create(user1.id, 'Test Group');
    await repo.addMember(group.id, user2.id, 'MEMBER');
    expect(await repo.isMember(group.id, user2.id)).toBe(true);
  });

  it('isMember returns false for non-member', async () => {
    const group = await repo.create(user1.id, 'Test Group');
    expect(await repo.isMember(group.id, user3.id)).toBe(false);
  });

  it('isAdmin returns true for owner', async () => {
    const group = await repo.create(user1.id, 'Test Group');
    expect(await repo.isAdmin(group.id, user1.id)).toBe(true);
  });

  it('isAdmin returns true for member with ADMIN role', async () => {
    const group = await repo.create(user1.id, 'Test Group');
    await repo.addMember(group.id, user2.id, 'ADMIN');
    expect(await repo.isAdmin(group.id, user2.id)).toBe(true);
  });

  it('isAdmin returns false for regular member', async () => {
    const group = await repo.create(user1.id, 'Test Group');
    await repo.addMember(group.id, user2.id, 'MEMBER');
    expect(await repo.isAdmin(group.id, user2.id)).toBe(false);
  });

  it('isAdmin returns false for non-member', async () => {
    const group = await repo.create(user1.id, 'Test Group');
    expect(await repo.isAdmin(group.id, user3.id)).toBe(false);
  });

  // ─── getUserGroupIds ─────────────────────────────────────────────────────────

  it('returns all group ids for a user (owned + member)', async () => {
    const ownedGroup = await repo.create(user1.id, 'Owned');
    const memberGroup = await repo.create(user2.id, 'Other');
    await repo.addMember(memberGroup.id, user1.id, 'MEMBER');

    const ids = await repo.getUserGroupIds(user1.id);
    expect(ids).toContain(ownedGroup.id);
    expect(ids).toContain(memberGroup.id);
    expect(ids.length).toBe(2);
  });

  it('returns empty array when user has no groups', async () => {
    const ids = await repo.getUserGroupIds(user3.id);
    expect(ids).toEqual([]);
  });

  // ─── Invitations ─────────────────────────────────────────────────────────────

  it('creates an invitation and returns it', async () => {
    const group = await repo.create(user1.id, 'Test Group');
    const inv = await repo.createInvitation(group.id, user1.id, user2.id);
    expect(inv.id).toBeTruthy();
    expect(inv.groupId).toBe(group.id);
    expect(inv.inviterId).toBe(user1.id);
    expect(inv.inviteeId).toBe(user2.id);
    expect(inv.status).toBe('PENDING');
    expect(inv.createdAt).toBeInstanceOf(Date);
  });

  it('gets an invitation by id', async () => {
    const group = await repo.create(user1.id, 'Test Group');
    const created = await repo.createInvitation(group.id, user1.id, user2.id);
    const found = await repo.getInvitationById(created.id);
    expect(found).not.toBeNull();
    expect(found!.id).toBe(created.id);
  });

  it('returns null for unknown invitation id', async () => {
    const found = await repo.getInvitationById('00000000-0000-0000-0000-000000000000');
    expect(found).toBeNull();
  });

  it('gets pending invitations for a group', async () => {
    const group = await repo.create(user1.id, 'Test Group');
    await repo.createInvitation(group.id, user1.id, user2.id);
    await repo.createInvitation(group.id, user1.id, user3.id);

    const pending = await repo.getPendingInvitations(group.id);
    expect(pending.length).toBe(2);
    expect(pending.every((i) => i.status === 'PENDING')).toBe(true);
  });

  it('does not return non-PENDING invitations from getPendingInvitations', async () => {
    const group = await repo.create(user1.id, 'Test Group');
    const inv = await repo.createInvitation(group.id, user1.id, user2.id);
    await repo.updateInvitationStatus(inv.id, 'ACCEPTED', user2.id);

    const pending = await repo.getPendingInvitations(group.id);
    expect(pending.length).toBe(0);
  });

  it('updates invitation status to ACCEPTED', async () => {
    const group = await repo.create(user1.id, 'Test Group');
    const inv = await repo.createInvitation(group.id, user1.id, user2.id);
    const updated = await repo.updateInvitationStatus(inv.id, 'ACCEPTED', user2.id);
    expect(updated.status).toBe('ACCEPTED');
    expect(updated.updatedAt).toBeInstanceOf(Date);
  });

  it('updates invitation status to DECLINED', async () => {
    const group = await repo.create(user1.id, 'Test Group');
    const inv = await repo.createInvitation(group.id, user1.id, user2.id);
    const updated = await repo.updateInvitationStatus(inv.id, 'DECLINED', user2.id);
    expect(updated.status).toBe('DECLINED');
  });
});
