import express from 'express';
import request from 'supertest';
import type { Knex } from 'knex';
import { setupTestDb, teardownTestDb, getTestDb } from '../setup';
import { PostgresUserRepo } from '../../src/repositories/postgres/user-repo';
import { PostgresBanRepo } from '../../src/repositories/postgres/ban-repo';
import { PostgresRateLimitRepo } from '../../src/repositories/postgres/rate-limit-repo';
import { PostgresNotificationRepo } from '../../src/repositories/postgres/notification-repo';
import { PostgresLocationRepo } from '../../src/repositories/postgres/location-repo';
import { PostgresPhotoRepo } from '../../src/repositories/postgres/photo-repo';
import { PostgresFriendRepo } from '../../src/repositories/postgres/friend-repo';
import { createFriendRouter } from '../../src/routes/friends';
import { FriendService } from '../../src/services/friend-service';
import { createAuthMiddleware } from '../../src/auth/middleware';
import { signToken } from '../../src/auth/jwt';
import { errorHandler } from '../../src/middleware/error-handler';
import type { User } from '../../src/models';

const TEST_JWT_SECRET = 'test-secret';

function buildDeps(db: Knex) {
  const users = new PostgresUserRepo(db);
  const notifications = new PostgresNotificationRepo(db);
  const friends = new PostgresFriendRepo(db);
  return { users, notifications, friends };
}

describe('Friend routes', () => {
  let db: Knex;
  let userA: User;
  let userB: User;
  let tokenA: string;
  let tokenB: string;

  beforeAll(async () => {
    db = await setupTestDb();
    const { users } = buildDeps(db);

    userA = await users.create({
      googleId: 'g-friend-a',
      email: 'frienda@example.com',
      displayName: 'Friend A',
    });
    userB = await users.create({
      googleId: 'g-friend-b',
      email: 'friendb@example.com',
      displayName: 'Friend B',
    });

    tokenA = signToken(
      { userId: userA.id, email: userA.email, isAdmin: false, hasSharingBan: false },
      TEST_JWT_SECRET,
    );
    tokenB = signToken(
      { userId: userB.id, email: userB.email, isAdmin: false, hasSharingBan: false },
      TEST_JWT_SECRET,
    );
  });

  afterAll(async () => {
    await teardownTestDb();
  });

  beforeEach(async () => {
    await db('friendships').delete();
    await db('notifications').delete();
  });

  function buildApp() {
    const testDb = getTestDb();
    const deps = buildDeps(testDb);
    const friendService = new FriendService(
      deps.friends,
      deps.users,
      deps.notifications,
    );
    const app = express();
    app.use(express.json());
    app.use(createAuthMiddleware(TEST_JWT_SECRET));
    app.use(createFriendRouter(friendService, deps.friends));
    app.use(errorHandler);
    return app;
  }

  describe('POST /api/friends/request', () => {
    it('sends a friend request by email → 201', async () => {
      const app = buildApp();
      const res = await request(app)
        .post('/api/friends/request')
        .set('Authorization', `Bearer ${tokenA}`)
        .send({ email: userB.email });

      expect(res.status).toBe(201);
      expect(res.body.requesterId).toBe(userA.id);
      expect(res.body.addresseeId).toBe(userB.id);
      expect(res.body.status).toBe('PENDING');
    });

    it('sends a friend request by userId → 201', async () => {
      const app = buildApp();
      const res = await request(app)
        .post('/api/friends/request')
        .set('Authorization', `Bearer ${tokenA}`)
        .send({ userId: userB.id });

      expect(res.status).toBe(201);
      expect(res.body.status).toBe('PENDING');
    });

    it('returns 404 for unknown email', async () => {
      const app = buildApp();
      const res = await request(app)
        .post('/api/friends/request')
        .set('Authorization', `Bearer ${tokenA}`)
        .send({ email: 'nobody@example.com' });

      expect(res.status).toBe(404);
    });

    it('returns 400 when sending request to self', async () => {
      const app = buildApp();
      const res = await request(app)
        .post('/api/friends/request')
        .set('Authorization', `Bearer ${tokenA}`)
        .send({ userId: userA.id });

      expect(res.status).toBe(400);
    });

    it('returns 400 when no email or userId provided', async () => {
      const app = buildApp();
      const res = await request(app)
        .post('/api/friends/request')
        .set('Authorization', `Bearer ${tokenA}`)
        .send({});

      expect(res.status).toBe(400);
    });
  });

  describe('GET /api/friends/requests/incoming', () => {
    it('lists incoming pending requests', async () => {
      const deps = buildDeps(db);
      await deps.friends.sendRequest(userA.id, userB.id);

      const app = buildApp();
      const res = await request(app)
        .get('/api/friends/requests/incoming')
        .set('Authorization', `Bearer ${tokenB}`);

      expect(res.status).toBe(200);
      expect(res.body.data.length).toBe(1);
      expect(res.body.data[0].requesterId).toBe(userA.id);
    });

    it('returns empty for user with no incoming requests', async () => {
      const app = buildApp();
      const res = await request(app)
        .get('/api/friends/requests/incoming')
        .set('Authorization', `Bearer ${tokenA}`);

      expect(res.status).toBe(200);
      expect(res.body.data.length).toBe(0);
    });
  });

  describe('GET /api/friends/requests/outgoing', () => {
    it('lists outgoing pending requests', async () => {
      const deps = buildDeps(db);
      await deps.friends.sendRequest(userA.id, userB.id);

      const app = buildApp();
      const res = await request(app)
        .get('/api/friends/requests/outgoing')
        .set('Authorization', `Bearer ${tokenA}`);

      expect(res.status).toBe(200);
      expect(res.body.data.length).toBe(1);
      expect(res.body.data[0].addresseeId).toBe(userB.id);
    });
  });

  describe('PUT /api/friends/requests/:id/accept', () => {
    it('accepts a friend request → 200', async () => {
      const deps = buildDeps(db);
      const friendship = await deps.friends.sendRequest(userA.id, userB.id);

      const app = buildApp();
      const res = await request(app)
        .put(`/api/friends/requests/${friendship.id}/accept`)
        .set('Authorization', `Bearer ${tokenB}`);

      expect(res.status).toBe(200);
      expect(res.body.status).toBe('ACCEPTED');
    });

    it('returns 403 if not the addressee', async () => {
      const deps = buildDeps(db);
      const friendship = await deps.friends.sendRequest(userA.id, userB.id);

      const app = buildApp();
      const res = await request(app)
        .put(`/api/friends/requests/${friendship.id}/accept`)
        .set('Authorization', `Bearer ${tokenA}`);

      expect(res.status).toBe(403);
    });

    it('returns 404 for unknown request ID', async () => {
      const app = buildApp();
      const res = await request(app)
        .put('/api/friends/requests/non-existent-id/accept')
        .set('Authorization', `Bearer ${tokenB}`);

      expect(res.status).toBe(404);
    });
  });

  describe('PUT /api/friends/requests/:id/decline', () => {
    it('declines a friend request → 200', async () => {
      const deps = buildDeps(db);
      const friendship = await deps.friends.sendRequest(userA.id, userB.id);

      const app = buildApp();
      const res = await request(app)
        .put(`/api/friends/requests/${friendship.id}/decline`)
        .set('Authorization', `Bearer ${tokenB}`);

      expect(res.status).toBe(200);
      expect(res.body.status).toBe('DECLINED');
    });
  });

  describe('GET /api/friends', () => {
    it('lists accepted friends', async () => {
      const deps = buildDeps(db);
      const friendship = await deps.friends.sendRequest(userA.id, userB.id);
      await deps.friends.acceptRequest(friendship.id);

      const app = buildApp();
      const res = await request(app)
        .get('/api/friends')
        .set('Authorization', `Bearer ${tokenA}`);

      expect(res.status).toBe(200);
      expect(res.body.data.length).toBe(1);
      expect(res.body.data[0].id).toBe(userB.id);
    });

    it('returns empty after friend is removed', async () => {
      const deps = buildDeps(db);
      const friendship = await deps.friends.sendRequest(userA.id, userB.id);
      await deps.friends.acceptRequest(friendship.id);
      await deps.friends.removeFriend(userA.id, userB.id);

      const app = buildApp();
      const res = await request(app)
        .get('/api/friends')
        .set('Authorization', `Bearer ${tokenA}`);

      expect(res.status).toBe(200);
      expect(res.body.data.length).toBe(0);
    });
  });

  describe('DELETE /api/friends/:userId', () => {
    it('removes a friend → 200', async () => {
      const deps = buildDeps(db);
      const friendship = await deps.friends.sendRequest(userA.id, userB.id);
      await deps.friends.acceptRequest(friendship.id);

      const app = buildApp();
      const res = await request(app)
        .delete(`/api/friends/${userB.id}`)
        .set('Authorization', `Bearer ${tokenA}`);

      expect(res.status).toBe(200);
      expect(res.body.removed).toBe(true);
    });

    it('returns false when not friends', async () => {
      const app = buildApp();
      const res = await request(app)
        .delete(`/api/friends/${userB.id}`)
        .set('Authorization', `Bearer ${tokenA}`);

      expect(res.status).toBe(200);
      expect(res.body.removed).toBe(false);
    });
  });
});
