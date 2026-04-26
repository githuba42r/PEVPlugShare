import express from 'express';
import request from 'supertest';
import type { Knex } from 'knex';
import { setupTestDb, teardownTestDb, getTestDb } from '../setup';
import { PostgresUserRepo } from '../../src/repositories/postgres/user-repo';
import { PostgresNotificationRepo } from '../../src/repositories/postgres/notification-repo';
import { PostgresGroupRepo } from '../../src/repositories/postgres/group-repo';
import { createGroupRouter } from '../../src/routes/groups';
import { GroupService } from '../../src/services/group-service';
import { createAuthMiddleware } from '../../src/auth/middleware';
import { signToken } from '../../src/auth/jwt';
import { errorHandler } from '../../src/middleware/error-handler';
import type { User } from '../../src/models';

const TEST_JWT_SECRET = 'test-secret';
const TEST_BASE_URL = 'http://localhost:8080';

function buildDeps(db: Knex) {
  const users = new PostgresUserRepo(db);
  const notifications = new PostgresNotificationRepo(db);
  const groups = new PostgresGroupRepo(db);
  return { users, notifications, groups };
}

describe('Group routes', () => {
  let db: Knex;
  let userA: User;
  let userB: User;
  let userC: User;
  let tokenA: string;
  let tokenB: string;
  let tokenC: string;

  beforeAll(async () => {
    db = await setupTestDb();
    const { users } = buildDeps(db);

    userA = await users.create({
      googleId: 'g-group-a',
      email: 'groupa@example.com',
      displayName: 'Group User A',
    });
    userB = await users.create({
      googleId: 'g-group-b',
      email: 'groupb@example.com',
      displayName: 'Group User B',
    });
    userC = await users.create({
      googleId: 'g-group-c',
      email: 'groupc@example.com',
      displayName: 'Group User C',
    });

    tokenA = signToken(
      { userId: userA.id, email: userA.email, isAdmin: false, hasSharingBan: false },
      TEST_JWT_SECRET,
    );
    tokenB = signToken(
      { userId: userB.id, email: userB.email, isAdmin: false, hasSharingBan: false },
      TEST_JWT_SECRET,
    );
    tokenC = signToken(
      { userId: userC.id, email: userC.email, isAdmin: false, hasSharingBan: false },
      TEST_JWT_SECRET,
    );
  });

  afterAll(async () => {
    await teardownTestDb();
  });

  beforeEach(async () => {
    await db('group_invitations').delete();
    await db('friend_group_members').delete();
    await db('friend_groups').delete();
    await db('notifications').delete();
  });

  function buildApp() {
    const testDb = getTestDb();
    const deps = buildDeps(testDb);
    const groupService = new GroupService(deps.groups, deps.notifications, TEST_BASE_URL);
    const app = express();
    app.use(express.json());
    app.use(createAuthMiddleware(TEST_JWT_SECRET));
    app.use(createGroupRouter(groupService, deps.groups));
    app.use(errorHandler);
    return app;
  }

  describe('POST /api/groups', () => {
    it('creates a group → 201', async () => {
      const app = buildApp();
      const res = await request(app)
        .post('/api/groups')
        .set('Authorization', `Bearer ${tokenA}`)
        .send({ name: 'Test Group', description: 'A test group' });

      expect(res.status).toBe(201);
      expect(res.body.name).toBe('Test Group');
      expect(res.body.ownerId).toBe(userA.id);
    });

    it('returns 400 when name is missing', async () => {
      const app = buildApp();
      const res = await request(app)
        .post('/api/groups')
        .set('Authorization', `Bearer ${tokenA}`)
        .send({});

      expect(res.status).toBe(400);
    });
  });

  describe('GET /api/groups', () => {
    it('lists groups for user', async () => {
      const deps = buildDeps(db);
      await deps.groups.create(userA.id, 'My Group');

      const app = buildApp();
      const res = await request(app)
        .get('/api/groups')
        .set('Authorization', `Bearer ${tokenA}`);

      expect(res.status).toBe(200);
      expect(res.body.data.length).toBe(1);
      expect(res.body.data[0].name).toBe('My Group');
    });

    it('returns empty for user with no groups', async () => {
      const app = buildApp();
      const res = await request(app)
        .get('/api/groups')
        .set('Authorization', `Bearer ${tokenB}`);

      expect(res.status).toBe(200);
      expect(res.body.data.length).toBe(0);
    });
  });

  describe('GET /api/groups/:id', () => {
    it('returns group by ID', async () => {
      const deps = buildDeps(db);
      const group = await deps.groups.create(userA.id, 'Fetch Group');

      const app = buildApp();
      const res = await request(app)
        .get(`/api/groups/${group.id}`)
        .set('Authorization', `Bearer ${tokenA}`);

      expect(res.status).toBe(200);
      expect(res.body.id).toBe(group.id);
      expect(res.body.name).toBe('Fetch Group');
    });

    it('returns 404 for unknown group', async () => {
      const app = buildApp();
      const res = await request(app)
        .get('/api/groups/non-existent-id')
        .set('Authorization', `Bearer ${tokenA}`);

      expect(res.status).toBe(404);
    });
  });

  describe('PUT /api/groups/:id', () => {
    it('updates group as owner → 200', async () => {
      const deps = buildDeps(db);
      const group = await deps.groups.create(userA.id, 'Old Name');

      const app = buildApp();
      const res = await request(app)
        .put(`/api/groups/${group.id}`)
        .set('Authorization', `Bearer ${tokenA}`)
        .send({ name: 'New Name' });

      expect(res.status).toBe(200);
      expect(res.body.name).toBe('New Name');
    });

    it('returns 403 for non-admin', async () => {
      const deps = buildDeps(db);
      const group = await deps.groups.create(userA.id, 'Protected Group');

      const app = buildApp();
      const res = await request(app)
        .put(`/api/groups/${group.id}`)
        .set('Authorization', `Bearer ${tokenB}`)
        .send({ name: 'Hacked Name' });

      expect(res.status).toBe(403);
    });
  });

  describe('DELETE /api/groups/:id', () => {
    it('deletes group as owner → 200', async () => {
      const deps = buildDeps(db);
      const group = await deps.groups.create(userA.id, 'Delete Me');

      const app = buildApp();
      const res = await request(app)
        .delete(`/api/groups/${group.id}`)
        .set('Authorization', `Bearer ${tokenA}`);

      expect(res.status).toBe(200);
      expect(res.body.deleted).toBe(true);
    });

    it('returns 403 for non-admin', async () => {
      const deps = buildDeps(db);
      const group = await deps.groups.create(userA.id, 'No Delete');

      const app = buildApp();
      const res = await request(app)
        .delete(`/api/groups/${group.id}`)
        .set('Authorization', `Bearer ${tokenB}`);

      expect(res.status).toBe(403);
    });
  });

  describe('POST /api/groups/:id/members', () => {
    it('adds members as owner → 201', async () => {
      const deps = buildDeps(db);
      const group = await deps.groups.create(userA.id, 'Member Group');

      const app = buildApp();
      const res = await request(app)
        .post(`/api/groups/${group.id}/members`)
        .set('Authorization', `Bearer ${tokenA}`)
        .send({ userIds: [userB.id] });

      expect(res.status).toBe(201);
      expect(res.body.added).toBe(1);
    });

    it('returns 403 for non-admin', async () => {
      const deps = buildDeps(db);
      const group = await deps.groups.create(userA.id, 'Protected Members');

      const app = buildApp();
      const res = await request(app)
        .post(`/api/groups/${group.id}/members`)
        .set('Authorization', `Bearer ${tokenB}`)
        .send({ userIds: [userC.id] });

      expect(res.status).toBe(403);
    });
  });

  describe('GET /api/groups/:id/members', () => {
    it('lists members for group member → 200', async () => {
      const deps = buildDeps(db);
      const group = await deps.groups.create(userA.id, 'List Members');
      await deps.groups.addMember(group.id, userB.id, 'MEMBER');

      const app = buildApp();
      const res = await request(app)
        .get(`/api/groups/${group.id}/members`)
        .set('Authorization', `Bearer ${tokenA}`);

      expect(res.status).toBe(200);
      expect(res.body.length).toBe(1);
      expect(res.body[0].userId).toBe(userB.id);
    });

    it('returns 403 for non-member', async () => {
      const deps = buildDeps(db);
      const group = await deps.groups.create(userA.id, 'Private Group');

      const app = buildApp();
      const res = await request(app)
        .get(`/api/groups/${group.id}/members`)
        .set('Authorization', `Bearer ${tokenC}`);

      expect(res.status).toBe(403);
    });
  });

  describe('PUT /api/groups/:id/members/:userId/role', () => {
    it('changes member role as owner → 200', async () => {
      const deps = buildDeps(db);
      const group = await deps.groups.create(userA.id, 'Role Group');
      await deps.groups.addMember(group.id, userB.id, 'MEMBER');

      const app = buildApp();
      const res = await request(app)
        .put(`/api/groups/${group.id}/members/${userB.id}/role`)
        .set('Authorization', `Bearer ${tokenA}`)
        .send({ role: 'ADMIN' });

      expect(res.status).toBe(200);
      expect(res.body.role).toBe('ADMIN');
    });
  });

  describe('DELETE /api/groups/:id/members/:userId', () => {
    it('removes a member as owner → 200', async () => {
      const deps = buildDeps(db);
      const group = await deps.groups.create(userA.id, 'Remove Member');
      await deps.groups.addMember(group.id, userB.id, 'MEMBER');

      const app = buildApp();
      const res = await request(app)
        .delete(`/api/groups/${group.id}/members/${userB.id}`)
        .set('Authorization', `Bearer ${tokenA}`);

      expect(res.status).toBe(200);
      expect(res.body.removed).toBe(true);
    });
  });

  describe('POST /api/groups/:id/invite', () => {
    it('generates an invite token as member → 201', async () => {
      const deps = buildDeps(db);
      const group = await deps.groups.create(userA.id, 'Invite Group');

      const app = buildApp();
      const res = await request(app)
        .post(`/api/groups/${group.id}/invite`)
        .set('Authorization', `Bearer ${tokenA}`);

      expect(res.status).toBe(201);
      expect(res.body.token).toBeDefined();
      expect(res.body.url).toContain('/api/groups/invite/');
    });

    it('returns 403 for non-member', async () => {
      const deps = buildDeps(db);
      const group = await deps.groups.create(userA.id, 'Private Invite');

      const app = buildApp();
      const res = await request(app)
        .post(`/api/groups/${group.id}/invite`)
        .set('Authorization', `Bearer ${tokenB}`);

      expect(res.status).toBe(403);
    });
  });

  describe('GET /api/groups/invite/:token', () => {
    it('resolves an invite token → 200', async () => {
      const deps = buildDeps(db);
      const group = await deps.groups.create(userA.id, 'Token Group');
      const invitation = await deps.groups.createInvitation(group.id, userA.id, userB.id);

      const app = buildApp();
      const res = await request(app)
        .get(`/api/groups/invite/${invitation.id}`)
        .set('Authorization', `Bearer ${tokenB}`);

      expect(res.status).toBe(200);
      expect(res.body.id).toBe(invitation.id);
    });

    it('returns 404 for unknown token', async () => {
      const app = buildApp();
      const res = await request(app)
        .get('/api/groups/invite/non-existent-token')
        .set('Authorization', `Bearer ${tokenA}`);

      expect(res.status).toBe(404);
    });
  });

  describe('GET /api/groups/:id/invitations', () => {
    it('lists pending invitations as admin → 200', async () => {
      const deps = buildDeps(db);
      const group = await deps.groups.create(userA.id, 'Invitation Group');
      await deps.groups.createInvitation(group.id, userA.id, userB.id);

      const app = buildApp();
      const res = await request(app)
        .get(`/api/groups/${group.id}/invitations`)
        .set('Authorization', `Bearer ${tokenA}`);

      expect(res.status).toBe(200);
      expect(res.body.length).toBe(1);
    });

    it('returns 403 for non-admin', async () => {
      const deps = buildDeps(db);
      const group = await deps.groups.create(userA.id, 'Secret Invitations');

      const app = buildApp();
      const res = await request(app)
        .get(`/api/groups/${group.id}/invitations`)
        .set('Authorization', `Bearer ${tokenC}`);

      expect(res.status).toBe(403);
    });
  });

  describe('PUT /api/groups/:id/invitations/:inviteId/approve', () => {
    it('approves invitation as admin → 200', async () => {
      const deps = buildDeps(db);
      const group = await deps.groups.create(userA.id, 'Approve Group');
      const invitation = await deps.groups.createInvitation(group.id, userA.id, userB.id);

      const app = buildApp();
      const res = await request(app)
        .put(`/api/groups/${group.id}/invitations/${invitation.id}/approve`)
        .set('Authorization', `Bearer ${tokenA}`);

      expect(res.status).toBe(200);
      expect(res.body.approved).toBe(true);

      // Verify user is now a member
      const isMember = await deps.groups.isMember(group.id, userB.id);
      expect(isMember).toBe(true);
    });
  });

  describe('PUT /api/groups/:id/invitations/:inviteId/decline', () => {
    it('declines invitation as admin → 200', async () => {
      const deps = buildDeps(db);
      const group = await deps.groups.create(userA.id, 'Decline Group');
      const invitation = await deps.groups.createInvitation(group.id, userA.id, userC.id);

      const app = buildApp();
      const res = await request(app)
        .put(`/api/groups/${group.id}/invitations/${invitation.id}/decline`)
        .set('Authorization', `Bearer ${tokenA}`);

      expect(res.status).toBe(200);
      expect(res.body.declined).toBe(true);
    });
  });
});
