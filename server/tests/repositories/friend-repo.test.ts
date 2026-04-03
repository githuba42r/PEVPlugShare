import { setupTestDb, teardownTestDb, getTestDb } from '../setup';
import { PostgresFriendRepo } from '../../src/repositories/postgres/friend-repo';
import { PostgresUserRepo } from '../../src/repositories/postgres/user-repo';
import type { Knex } from 'knex';
import type { User } from '../../src/models';

describe('PostgresFriendRepo', () => {
  let db: Knex;
  let repo: PostgresFriendRepo;
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
    repo = new PostgresFriendRepo(getTestDb());
    await db('friendships').delete();
  });

  it('sends a friend request and returns a PENDING friendship', async () => {
    const friendship = await repo.sendRequest(user1.id, user2.id);
    expect(friendship.id).toBeTruthy();
    expect(friendship.requesterId).toBe(user1.id);
    expect(friendship.addresseeId).toBe(user2.id);
    expect(friendship.status).toBe('PENDING');
    expect(friendship.createdAt).toBeInstanceOf(Date);
  });

  it('throws when a duplicate request is sent in the same direction', async () => {
    await repo.sendRequest(user1.id, user2.id);
    await expect(repo.sendRequest(user1.id, user2.id)).rejects.toThrow();
  });

  it('throws when a reverse-direction request is sent while one already exists', async () => {
    await repo.sendRequest(user1.id, user2.id);
    await expect(repo.sendRequest(user2.id, user1.id)).rejects.toThrow();
  });

  it('gets a request by id', async () => {
    const created = await repo.sendRequest(user1.id, user2.id);
    const found = await repo.getRequestById(created.id);
    expect(found).not.toBeNull();
    expect(found!.id).toBe(created.id);
    expect(found!.status).toBe('PENDING');
  });

  it('returns null for unknown request id', async () => {
    const found = await repo.getRequestById('00000000-0000-0000-0000-000000000000');
    expect(found).toBeNull();
  });

  it('accepts a pending request', async () => {
    const request = await repo.sendRequest(user1.id, user2.id);
    const accepted = await repo.acceptRequest(request.id);
    expect(accepted.status).toBe('ACCEPTED');
    expect(accepted.updatedAt).toBeInstanceOf(Date);
  });

  it('throws when accepting a non-existent request', async () => {
    await expect(repo.acceptRequest('00000000-0000-0000-0000-000000000000')).rejects.toThrow();
  });

  it('throws when accepting an already-accepted request', async () => {
    const request = await repo.sendRequest(user1.id, user2.id);
    await repo.acceptRequest(request.id);
    await expect(repo.acceptRequest(request.id)).rejects.toThrow();
  });

  it('declines a pending request', async () => {
    const request = await repo.sendRequest(user1.id, user2.id);
    const declined = await repo.declineRequest(request.id);
    expect(declined.status).toBe('DECLINED');
    expect(declined.updatedAt).toBeInstanceOf(Date);
  });

  it('throws when declining a non-existent request', async () => {
    await expect(repo.declineRequest('00000000-0000-0000-0000-000000000000')).rejects.toThrow();
  });

  it('returns incoming requests for the addressee', async () => {
    await repo.sendRequest(user1.id, user2.id);
    await repo.sendRequest(user3.id, user2.id);

    const result = await repo.getIncomingRequests(user2.id);
    expect(result.data.length).toBe(2);
    expect(result.data.every((f) => f.addresseeId === user2.id)).toBe(true);
    expect(result.data.every((f) => f.status === 'PENDING')).toBe(true);
  });

  it('returns outgoing requests for the requester', async () => {
    await repo.sendRequest(user1.id, user2.id);
    await repo.sendRequest(user1.id, user3.id);

    const result = await repo.getOutgoingRequests(user1.id);
    expect(result.data.length).toBe(2);
    expect(result.data.every((f) => f.requesterId === user1.id)).toBe(true);
  });

  it('does not include non-PENDING requests in incoming/outgoing', async () => {
    const req = await repo.sendRequest(user1.id, user2.id);
    await repo.acceptRequest(req.id);

    const incoming = await repo.getIncomingRequests(user2.id);
    const outgoing = await repo.getOutgoingRequests(user1.id);
    expect(incoming.data.length).toBe(0);
    expect(outgoing.data.length).toBe(0);
  });

  it('paginates incoming requests', async () => {
    // Create 4 users to send requests to user2
    const userRepo = new PostgresUserRepo(db);
    const extras: User[] = [];
    for (let i = 0; i < 4; i++) {
      extras.push(await userRepo.create({ googleId: `px${i}`, email: `px${i}@example.com`, displayName: `PX ${i}` }));
    }
    for (const u of extras) {
      await repo.sendRequest(u.id, user2.id);
    }

    const page1 = await repo.getIncomingRequests(user2.id, undefined, 2);
    expect(page1.data.length).toBe(2);
    expect(page1.pagination.hasMore).toBe(true);

    const page2 = await repo.getIncomingRequests(user2.id, page1.pagination.cursor!, 2);
    expect(page2.data.length).toBe(2);
    expect(page2.pagination.hasMore).toBe(false);
    expect(page2.pagination.cursor).toBeNull();
  });

  it('returns friends list for accepted friendships', async () => {
    const req = await repo.sendRequest(user1.id, user2.id);
    await repo.acceptRequest(req.id);

    const result = await repo.getFriends(user1.id);
    expect(result.data.length).toBe(1);
    expect(result.data[0].id).toBe(user2.id);
  });

  it('returns friends from both requester and addressee perspectives', async () => {
    const req1 = await repo.sendRequest(user1.id, user2.id);
    await repo.acceptRequest(req1.id);

    const req2 = await repo.sendRequest(user3.id, user1.id);
    await repo.acceptRequest(req2.id);

    const friends = await repo.getFriends(user1.id);
    expect(friends.data.length).toBe(2);
    const ids = friends.data.map((u) => u.id);
    expect(ids).toContain(user2.id);
    expect(ids).toContain(user3.id);
  });

  it('paginates friends list', async () => {
    const userRepo = new PostgresUserRepo(db);
    const extras: User[] = [];
    for (let i = 0; i < 4; i++) {
      extras.push(await userRepo.create({ googleId: `pf${i}`, email: `pf${i}@example.com`, displayName: `PF ${i}` }));
    }
    for (const u of extras) {
      const req = await repo.sendRequest(user1.id, u.id);
      await repo.acceptRequest(req.id);
    }

    const page1 = await repo.getFriends(user1.id, undefined, 2);
    expect(page1.data.length).toBe(2);
    expect(page1.pagination.hasMore).toBe(true);

    const page2 = await repo.getFriends(user1.id, page1.pagination.cursor!, 2);
    expect(page2.data.length).toBe(2);
    expect(page2.pagination.hasMore).toBe(false);
    expect(page2.pagination.cursor).toBeNull();
  });

  it('removes a friend and returns true', async () => {
    const req = await repo.sendRequest(user1.id, user2.id);
    await repo.acceptRequest(req.id);

    const removed = await repo.removeFriend(user1.id, user2.id);
    expect(removed).toBe(true);

    const friends = await repo.getFriends(user1.id);
    expect(friends.data.length).toBe(0);
  });

  it('returns false when removing a non-existent friendship', async () => {
    const removed = await repo.removeFriend(user1.id, user2.id);
    expect(removed).toBe(false);
  });

  it('returns false when removing a PENDING request (not ACCEPTED)', async () => {
    await repo.sendRequest(user1.id, user2.id);
    const removed = await repo.removeFriend(user1.id, user2.id);
    expect(removed).toBe(false);
  });

  it('areFriends returns true for accepted friendship', async () => {
    const req = await repo.sendRequest(user1.id, user2.id);
    await repo.acceptRequest(req.id);

    expect(await repo.areFriends(user1.id, user2.id)).toBe(true);
    expect(await repo.areFriends(user2.id, user1.id)).toBe(true);
  });

  it('areFriends returns false for pending request', async () => {
    await repo.sendRequest(user1.id, user2.id);
    expect(await repo.areFriends(user1.id, user2.id)).toBe(false);
  });

  it('areFriends returns false for no relationship', async () => {
    expect(await repo.areFriends(user1.id, user3.id)).toBe(false);
  });
});
