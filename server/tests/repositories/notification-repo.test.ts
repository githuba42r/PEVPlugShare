import { setupTestDb, teardownTestDb, getTestDb } from '../setup';
import { PostgresNotificationRepo } from '../../src/repositories/postgres/notification-repo';
import { PostgresUserRepo } from '../../src/repositories/postgres/user-repo';
import type { Knex } from 'knex';

describe('PostgresNotificationRepo', () => {
  let db: Knex;
  let repo: PostgresNotificationRepo;
  let userId: string;

  beforeAll(async () => {
    db = await setupTestDb();
    const userRepo = new PostgresUserRepo(db);
    const user = await userRepo.create({ googleId: 'g1', email: 'notif@example.com', displayName: 'Notif User' });
    userId = user.id;
  });

  afterAll(async () => {
    await teardownTestDb();
  });

  beforeEach(async () => {
    repo = new PostgresNotificationRepo(getTestDb());
    await db('notifications').delete();
  });

  const testPayload = { title: 'Hello', body: 'World', data: { key: 'value' } };

  it('creates a notification', async () => {
    const notif = await repo.create(userId, 'SYSTEM_MESSAGE', testPayload);
    expect(notif.id).toBeTruthy();
    expect(notif.userId).toBe(userId);
    expect(notif.type).toBe('SYSTEM_MESSAGE');
    expect(notif.title).toBe('Hello');
    expect(notif.body).toBe('World');
    expect(notif.data).toEqual({ key: 'value' });
    expect(notif.isRead).toBe(false);
    expect(notif.createdAt).toBeInstanceOf(Date);
    expect(notif.readAt).toBeNull();
  });

  it('counts unread notifications', async () => {
    expect(await repo.count(userId)).toBe(0);
    await repo.create(userId, 'SYSTEM_MESSAGE', testPayload);
    await repo.create(userId, 'FRIEND_REQUEST', testPayload);
    expect(await repo.count(userId)).toBe(2);
  });

  it('marks a single notification as read', async () => {
    const notif = await repo.create(userId, 'SYSTEM_MESSAGE', testPayload);
    await repo.markRead(notif.id);
    expect(await repo.count(userId)).toBe(0);
  });

  it('marks all notifications as read', async () => {
    await repo.create(userId, 'SYSTEM_MESSAGE', testPayload);
    await repo.create(userId, 'FRIEND_REQUEST', testPayload);
    expect(await repo.count(userId)).toBe(2);
    await repo.markAllRead(userId);
    expect(await repo.count(userId)).toBe(0);
  });

  it('gets unread notifications with pagination', async () => {
    for (let i = 0; i < 5; i++) {
      await repo.create(userId, 'SYSTEM_MESSAGE', { title: `Notif ${i}`, body: 'body' });
      // small delay to ensure distinct created_at ordering
      await new Promise((r) => setTimeout(r, 5));
    }

    const page1 = await repo.getUnread(userId, undefined, 3);
    expect(page1.data.length).toBe(3);
    expect(page1.pagination.hasMore).toBe(true);
    expect(page1.pagination.cursor).not.toBeNull();

    const page2 = await repo.getUnread(userId, page1.pagination.cursor!, 3);
    expect(page2.data.length).toBe(2);
    expect(page2.pagination.hasMore).toBe(false);
  });

  it('getUnread returns empty for no notifications', async () => {
    const result = await repo.getUnread(userId);
    expect(result.data).toHaveLength(0);
    expect(result.pagination.hasMore).toBe(false);
    expect(result.pagination.cursor).toBeNull();
  });

  it('getUnread only returns unread', async () => {
    const n1 = await repo.create(userId, 'SYSTEM_MESSAGE', testPayload);
    await repo.create(userId, 'FRIEND_REQUEST', testPayload);
    await repo.markRead(n1.id);

    const result = await repo.getUnread(userId);
    expect(result.data.length).toBe(1);
    expect(result.data[0].type).toBe('FRIEND_REQUEST');
  });

  it('count only counts unread for the specified user', async () => {
    // Create a second user
    const userRepo = new PostgresUserRepo(db);
    const otherUser = await userRepo.create({ googleId: 'g-other', email: 'other@example.com', displayName: 'Other' });

    await repo.create(userId, 'SYSTEM_MESSAGE', testPayload);
    await repo.create(otherUser.id, 'SYSTEM_MESSAGE', testPayload);

    expect(await repo.count(userId)).toBe(1);
    expect(await repo.count(otherUser.id)).toBe(1);
  });
});
