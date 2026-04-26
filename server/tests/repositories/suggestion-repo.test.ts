import { setupTestDb, teardownTestDb, getTestDb } from '../setup';
import { PostgresUserRepo } from '../../src/repositories/postgres/user-repo';
import { PostgresLocationRepo } from '../../src/repositories/postgres/location-repo';
import { PostgresSuggestionRepo } from '../../src/repositories/postgres/suggestion-repo';
import type { Knex } from 'knex';

describe('PostgresSuggestionRepo', () => {
  let db: Knex;
  let repo: PostgresSuggestionRepo;
  let ownerId: string;
  let suggestorId: string;
  let locationId: string;

  beforeAll(async () => {
    db = await setupTestDb();
    const userRepo = new PostgresUserRepo(db);
    const locationRepo = new PostgresLocationRepo(db);

    const owner = await userRepo.create({
      googleId: 'sugg-owner-google',
      email: 'sugg-owner@example.com',
      displayName: 'Suggestion Owner',
    });
    ownerId = owner.id;

    const suggestor = await userRepo.create({
      googleId: 'sugg-suggestor-google',
      email: 'sugg-suggestor@example.com',
      displayName: 'Suggestion Suggestor',
    });
    suggestorId = suggestor.id;

    const loc = await locationRepo.create({
      ownerId,
      name: 'Suggestion Location',
      latitude: -33.8688,
      longitude: 151.2093,
    });
    locationId = loc.id;
  });

  afterAll(async () => {
    await teardownTestDb();
  });

  beforeEach(async () => {
    repo = new PostgresSuggestionRepo(getTestDb());
    await db('change_suggestions').delete();
  });

  // --- create ---
  it('creates a suggestion with PENDING status', async () => {
    const suggestion = await repo.create(locationId, suggestorId, '{"name":"New Name"}');
    expect(suggestion.id).toBeTruthy();
    expect(suggestion.locationId).toBe(locationId);
    expect(suggestion.suggestedByUserId).toBe(suggestorId);
    expect(suggestion.status).toBe('PENDING');
    expect(suggestion.fieldName).toBe('{"name":"New Name"}');
    expect(suggestion.reason).toBeNull();
    expect(suggestion.reviewedAt).toBeNull();
    expect(suggestion.reviewNote).toBeNull();
    expect(suggestion.createdAt).toBeInstanceOf(Date);
  });

  it('creates a suggestion with optional message', async () => {
    const suggestion = await repo.create(locationId, suggestorId, '{"name":"New Name"}', 'Please update the name');
    expect(suggestion.reason).toBe('Please update the name');
  });

  // --- getById ---
  it('getById returns the suggestion', async () => {
    const created = await repo.create(locationId, suggestorId, '{"name":"Test"}');
    const found = await repo.getById(created.id);
    expect(found).not.toBeNull();
    expect(found!.id).toBe(created.id);
  });

  it('getById returns null for unknown id', async () => {
    const found = await repo.getById('00000000-0000-0000-0000-000000000000');
    expect(found).toBeNull();
  });

  // --- getIncoming ---
  it('getIncoming returns pending suggestions for the location owner', async () => {
    await repo.create(locationId, suggestorId, '{"name":"Name1"}');
    await repo.create(locationId, suggestorId, '{"name":"Name2"}');

    const result = await repo.getIncoming(ownerId);
    expect(result.data).toHaveLength(2);
    expect(result.pagination.hasMore).toBe(false);
  });

  it('getIncoming excludes non-pending suggestions', async () => {
    const s1 = await repo.create(locationId, suggestorId, '{"name":"Name1"}');
    await repo.create(locationId, suggestorId, '{"name":"Name2"}');
    await repo.accept(s1.id);

    const result = await repo.getIncoming(ownerId);
    expect(result.data).toHaveLength(1);
    expect(result.data[0].status).toBe('PENDING');
  });

  it('getIncoming returns empty for other owner', async () => {
    await repo.create(locationId, suggestorId, '{"name":"Name"}');

    const result = await repo.getIncoming(suggestorId); // not the owner
    expect(result.data).toHaveLength(0);
  });

  it('getIncoming paginates', async () => {
    for (let i = 0; i < 4; i++) {
      await repo.create(locationId, suggestorId, `{"idx":${i}}`);
    }

    const page1 = await repo.getIncoming(ownerId, undefined, 3);
    expect(page1.data).toHaveLength(3);
    expect(page1.pagination.hasMore).toBe(true);

    const page2 = await repo.getIncoming(ownerId, page1.pagination.cursor!, 3);
    expect(page2.data).toHaveLength(1);
    expect(page2.pagination.hasMore).toBe(false);
  });

  // --- getOutgoing ---
  it('getOutgoing returns suggestions made by user', async () => {
    await repo.create(locationId, suggestorId, '{"name":"A"}');
    await repo.create(locationId, suggestorId, '{"name":"B"}');

    const result = await repo.getOutgoing(suggestorId);
    expect(result.data).toHaveLength(2);
  });

  it('getOutgoing returns empty for user with no suggestions', async () => {
    await repo.create(locationId, suggestorId, '{"name":"A"}');

    const result = await repo.getOutgoing(ownerId);
    expect(result.data).toHaveLength(0);
  });

  it('getOutgoing paginates', async () => {
    for (let i = 0; i < 4; i++) {
      await repo.create(locationId, suggestorId, `{"idx":${i}}`);
    }

    const page1 = await repo.getOutgoing(suggestorId, undefined, 3);
    expect(page1.data).toHaveLength(3);
    expect(page1.pagination.hasMore).toBe(true);

    const page2 = await repo.getOutgoing(suggestorId, page1.pagination.cursor!, 3);
    expect(page2.data).toHaveLength(1);
    expect(page2.pagination.hasMore).toBe(false);
  });

  // --- accept ---
  it('accept sets status to ACCEPTED with response message', async () => {
    const created = await repo.create(locationId, suggestorId, '{"name":"New"}');
    const accepted = await repo.accept(created.id, 'Looks good');
    expect(accepted.status).toBe('ACCEPTED');
    expect(accepted.reviewNote).toBe('Looks good');
    expect(accepted.reviewedAt).toBeInstanceOf(Date);
  });

  it('accept sets status to ACCEPTED without response message', async () => {
    const created = await repo.create(locationId, suggestorId, '{"name":"New"}');
    const accepted = await repo.accept(created.id);
    expect(accepted.status).toBe('ACCEPTED');
    expect(accepted.reviewNote).toBeNull();
  });

  // --- reject ---
  it('reject sets status to REJECTED with response message', async () => {
    const created = await repo.create(locationId, suggestorId, '{"name":"Bad"}');
    const rejected = await repo.reject(created.id, 'Not valid');
    expect(rejected.status).toBe('REJECTED');
    expect(rejected.reviewNote).toBe('Not valid');
    expect(rejected.reviewedAt).toBeInstanceOf(Date);
  });

  // --- withdraw ---
  it('withdraw sets status to WITHDRAWN', async () => {
    const created = await repo.create(locationId, suggestorId, '{"name":"Oops"}');
    const withdrawn = await repo.withdraw(created.id);
    expect(withdrawn.status).toBe('WITHDRAWN');
  });

  it('withdraw on non-pending does not change status', async () => {
    const created = await repo.create(locationId, suggestorId, '{"name":"Test"}');
    await repo.accept(created.id);
    const result = await repo.withdraw(created.id);
    expect(result.status).toBe('ACCEPTED'); // was already accepted
  });
});
