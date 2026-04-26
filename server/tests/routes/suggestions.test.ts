import express from 'express';
import request from 'supertest';
import type { Knex } from 'knex';
import { setupTestDb, teardownTestDb, getTestDb } from '../setup';
import { PostgresUserRepo } from '../../src/repositories/postgres/user-repo';
import { PostgresLocationRepo } from '../../src/repositories/postgres/location-repo';
import { PostgresNotificationRepo } from '../../src/repositories/postgres/notification-repo';
import { PostgresShareRepo } from '../../src/repositories/postgres/share-repo';
import { PostgresSuggestionRepo } from '../../src/repositories/postgres/suggestion-repo';
import { SuggestionService } from '../../src/services/suggestion-service';
import { createSuggestionRouter } from '../../src/routes/suggestions';
import { createAuthMiddleware } from '../../src/auth/middleware';
import { signToken } from '../../src/auth/jwt';
import { errorHandler } from '../../src/middleware/error-handler';
import type { User, Location } from '../../src/models';

const TEST_JWT_SECRET = 'test-secret';

function buildDeps(db: Knex) {
  const users = new PostgresUserRepo(db);
  const locations = new PostgresLocationRepo(db);
  const notifications = new PostgresNotificationRepo(db);
  const shares = new PostgresShareRepo(db);
  const suggestions = new PostgresSuggestionRepo(db);
  return { users, locations, notifications, shares, suggestions };
}

describe('Suggestion routes', () => {
  let db: Knex;
  let user1: User;
  let user2: User;
  let user3: User;
  let token1: string;
  let token2: string;
  let token3: string;
  let location: Location;

  beforeAll(async () => {
    db = await setupTestDb();
    const deps = buildDeps(db);

    user1 = await deps.users.create({
      googleId: 'g-suggest-owner',
      email: 'suggestowner@example.com',
      displayName: 'Suggestion Owner',
    });
    user2 = await deps.users.create({
      googleId: 'g-suggest-user2',
      email: 'suggestuser2@example.com',
      displayName: 'Suggestion User 2',
    });
    user3 = await deps.users.create({
      googleId: 'g-suggest-user3',
      email: 'suggestuser3@example.com',
      displayName: 'Suggestion User 3 (no access)',
    });

    token1 = signToken(
      { userId: user1.id, email: user1.email, isAdmin: false, hasSharingBan: false },
      TEST_JWT_SECRET,
    );
    token2 = signToken(
      { userId: user2.id, email: user2.email, isAdmin: false, hasSharingBan: false },
      TEST_JWT_SECRET,
    );
    token3 = signToken(
      { userId: user3.id, email: user3.email, isAdmin: false, hasSharingBan: false },
      TEST_JWT_SECRET,
    );

    location = await deps.locations.create({
      ownerId: user1.id,
      name: 'Suggestion Test Spot',
      latitude: -33.8688,
      longitude: 151.2093,
    });

    // Share location with user2 (not user3)
    await deps.shares.shareWithUser(location.id, user1.id, user2.id);
  });

  afterAll(async () => {
    await teardownTestDb();
  });

  beforeEach(async () => {
    const testDb = getTestDb();
    await testDb('change_suggestions').delete();
    await testDb('notifications').delete();
  });

  function buildApp() {
    const testDb = getTestDb();
    const deps = buildDeps(testDb);
    const suggestionService = new SuggestionService(
      deps.suggestions,
      deps.locations,
      deps.shares,
      deps.notifications,
    );
    const app = express();
    app.use(express.json());
    app.use(createAuthMiddleware(TEST_JWT_SECRET));
    app.use(createSuggestionRouter(suggestionService, deps.suggestions));
    app.use(errorHandler);
    return app;
  }

  describe('POST /api/locations/:id/suggestions', () => {
    it('user2 (with access) submits suggestion → 201', async () => {
      const app = buildApp();
      const res = await request(app)
        .post(`/api/locations/${location.id}/suggestions`)
        .set('Authorization', `Bearer ${token2}`)
        .send({ changesJson: JSON.stringify({ name: 'Better Name' }), message: 'Please update' });

      expect(res.status).toBe(201);
      expect(res.body.locationId).toBe(location.id);
      expect(res.body.suggestedByUserId).toBe(user2.id);
    });

    it('returns 400 when changesJson is missing', async () => {
      const app = buildApp();
      const res = await request(app)
        .post(`/api/locations/${location.id}/suggestions`)
        .set('Authorization', `Bearer ${token2}`)
        .send({});

      expect(res.status).toBe(400);
    });

    it('user3 (no access) cannot submit → 403', async () => {
      const app = buildApp();
      const res = await request(app)
        .post(`/api/locations/${location.id}/suggestions`)
        .set('Authorization', `Bearer ${token3}`)
        .send({ changesJson: JSON.stringify({ name: 'Hacked Name' }) });

      expect(res.status).toBe(403);
    });

    it('returns 404 for unknown location', async () => {
      const app = buildApp();
      const res = await request(app)
        .post('/api/locations/00000000-0000-0000-0000-000000000000/suggestions')
        .set('Authorization', `Bearer ${token2}`)
        .send({ changesJson: JSON.stringify({ name: 'x' }) });

      expect(res.status).toBe(404);
    });
  });

  describe('GET /api/suggestions/incoming', () => {
    it('owner (user1) sees incoming suggestions', async () => {
      const testDb = getTestDb();
      const deps = buildDeps(testDb);
      await deps.suggestions.create(location.id, user2.id, JSON.stringify({ name: 'New Name' }), 'message');

      const app = buildApp();
      const res = await request(app)
        .get('/api/suggestions/incoming')
        .set('Authorization', `Bearer ${token1}`);

      expect(res.status).toBe(200);
      expect(res.body.data.length).toBeGreaterThanOrEqual(1);
      const ids = res.body.data.map((s: any) => s.locationId);
      expect(ids).toContain(location.id);
    });
  });

  describe('GET /api/suggestions/outgoing', () => {
    it('user2 sees their outgoing suggestions', async () => {
      const testDb = getTestDb();
      const deps = buildDeps(testDb);
      await deps.suggestions.create(location.id, user2.id, JSON.stringify({ name: 'New Name' }));

      const app = buildApp();
      const res = await request(app)
        .get('/api/suggestions/outgoing')
        .set('Authorization', `Bearer ${token2}`);

      expect(res.status).toBe(200);
      expect(res.body.data.length).toBeGreaterThanOrEqual(1);
      expect(res.body.data[0].suggestedByUserId).toBe(user2.id);
    });
  });

  describe('PUT /api/suggestions/:id/accept', () => {
    it('owner accepts suggestion → location updated, version incremented', async () => {
      const testDb = getTestDb();
      const deps = buildDeps(testDb);
      const suggestion = await deps.suggestions.create(
        location.id,
        user2.id,
        JSON.stringify({ name: 'Accepted Name' }),
      );

      const app = buildApp();
      const res = await request(app)
        .put(`/api/suggestions/${suggestion.id}/accept`)
        .set('Authorization', `Bearer ${token1}`)
        .send({ responseMsg: 'Great suggestion!' });

      expect(res.status).toBe(200);
      expect(res.body.status).toBe('ACCEPTED');
      expect(res.body.reviewNote).toBe('Great suggestion!');

      // Verify location was updated
      const updatedLoc = await deps.locations.getById(location.id);
      expect(updatedLoc?.name).toBe('Accepted Name');
      expect(updatedLoc?.version).toBeGreaterThan(location.version);
    });

    it('non-owner cannot accept → 403', async () => {
      const testDb = getTestDb();
      const deps = buildDeps(testDb);
      const suggestion = await deps.suggestions.create(
        location.id,
        user2.id,
        JSON.stringify({ name: 'x' }),
      );

      const app = buildApp();
      const res = await request(app)
        .put(`/api/suggestions/${suggestion.id}/accept`)
        .set('Authorization', `Bearer ${token2}`);

      expect(res.status).toBe(403);
    });

    it('returns 404 for unknown suggestion', async () => {
      const app = buildApp();
      const res = await request(app)
        .put('/api/suggestions/00000000-0000-0000-0000-000000000000/accept')
        .set('Authorization', `Bearer ${token1}`);

      expect(res.status).toBe(404);
    });
  });

  describe('PUT /api/suggestions/:id/reject', () => {
    it('owner rejects suggestion → status REJECTED', async () => {
      const testDb = getTestDb();
      const deps = buildDeps(testDb);
      const suggestion = await deps.suggestions.create(
        location.id,
        user2.id,
        JSON.stringify({ name: 'Bad Name' }),
      );

      const app = buildApp();
      const res = await request(app)
        .put(`/api/suggestions/${suggestion.id}/reject`)
        .set('Authorization', `Bearer ${token1}`)
        .send({ responseMsg: 'Not applicable' });

      expect(res.status).toBe(200);
      expect(res.body.status).toBe('REJECTED');
    });

    it('non-owner cannot reject → 403', async () => {
      const testDb = getTestDb();
      const deps = buildDeps(testDb);
      const suggestion = await deps.suggestions.create(
        location.id,
        user2.id,
        JSON.stringify({ name: 'x' }),
      );

      const app = buildApp();
      const res = await request(app)
        .put(`/api/suggestions/${suggestion.id}/reject`)
        .set('Authorization', `Bearer ${token2}`);

      expect(res.status).toBe(403);
    });
  });

  describe('PUT /api/suggestions/:id/withdraw', () => {
    it('suggester withdraws their suggestion → status WITHDRAWN', async () => {
      const testDb = getTestDb();
      const deps = buildDeps(testDb);
      const suggestion = await deps.suggestions.create(
        location.id,
        user2.id,
        JSON.stringify({ name: 'Withdraw Me' }),
      );

      const app = buildApp();
      const res = await request(app)
        .put(`/api/suggestions/${suggestion.id}/withdraw`)
        .set('Authorization', `Bearer ${token2}`);

      expect(res.status).toBe(200);
      expect(res.body.status).toBe('WITHDRAWN');
    });

    it('non-suggester cannot withdraw → 403', async () => {
      const testDb = getTestDb();
      const deps = buildDeps(testDb);
      const suggestion = await deps.suggestions.create(
        location.id,
        user2.id,
        JSON.stringify({ name: 'x' }),
      );

      const app = buildApp();
      const res = await request(app)
        .put(`/api/suggestions/${suggestion.id}/withdraw`)
        .set('Authorization', `Bearer ${token1}`);

      expect(res.status).toBe(403);
    });
  });
});
