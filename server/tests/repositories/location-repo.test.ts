import { setupTestDb, teardownTestDb, getTestDb } from '../setup';
import { PostgresUserRepo } from '../../src/repositories/postgres/user-repo';
import { PostgresLocationRepo } from '../../src/repositories/postgres/location-repo';
import type { Knex } from 'knex';

describe('PostgresLocationRepo', () => {
  let db: Knex;
  let repo: PostgresLocationRepo;
  let ownerId: string;

  beforeAll(async () => {
    db = await setupTestDb();
    const userRepo = new PostgresUserRepo(db);
    const user = await userRepo.create({
      googleId: 'owner-google',
      email: 'owner@example.com',
      displayName: 'Owner User',
    });
    ownerId = user.id;
  });

  afterAll(async () => {
    await teardownTestDb();
  });

  beforeEach(async () => {
    repo = new PostgresLocationRepo(getTestDb());
    await db('server_locations').delete();
  });

  const baseInput = {
    ownerId: '', // set in each test via ownerId
    name: 'Test Charger',
    latitude: -33.8688,
    longitude: 151.2093,
  };

  it('creates a location and returns it', async () => {
    const loc = await repo.create({ ...baseInput, ownerId });
    expect(loc.id).toBeTruthy();
    expect(loc.ownerId).toBe(ownerId);
    expect(loc.name).toBe('Test Charger');
    expect(loc.latitude).toBeCloseTo(-33.8688);
    expect(loc.longitude).toBeCloseTo(151.2093);
    expect(loc.isDeleted).toBe(false);
    expect(loc.version).toBe(1);
    expect(loc.socketTypes).toEqual([]);
    expect(loc.hasFee).toBe(false);
    expect(loc.publicPrivate).toBe('PUBLIC');
    expect(loc.createdAt).toBeInstanceOf(Date);
    expect(loc.updatedAt).toBeInstanceOf(Date);
  });

  it('creates a location with all optional fields', async () => {
    const loc = await repo.create({
      ownerId,
      name: 'Full Charger',
      latitude: -37.8136,
      longitude: 144.9631,
      description: 'A great charger',
      additionalInfo: 'Bring your own cable',
      openingHours: '24/7',
      socketTypes: ['Type2', 'CCS'],
      gpoCount: 2,
      hasFee: true,
      chargingCosts: '$0.30/kWh',
      publicPrivate: 'SEMI_PRIVATE',
      providerName: 'ChargeCo',
      providerWebsite: 'https://chargeco.example.com',
      businessName: 'The Office',
      streetAddress: '123 Main St',
      suburb: 'Melbourne',
      state: 'VIC',
      postcode: '3000',
      color: '#FF0000',
      icon: 'bolt',
    });

    expect(loc.socketTypes).toEqual(['Type2', 'CCS']);
    expect(loc.hasFee).toBe(true);
    expect(loc.publicPrivate).toBe('SEMI_PRIVATE');
    expect(loc.gpoCount).toBe(2);
    expect(loc.description).toBe('A great charger');
    expect(loc.color).toBe('#FF0000');
  });

  it('gets location by id', async () => {
    const created = await repo.create({ ...baseInput, ownerId });
    const found = await repo.getById(created.id);
    expect(found).not.toBeNull();
    expect(found!.id).toBe(created.id);
  });

  it('returns null for unknown id', async () => {
    const found = await repo.getById('00000000-0000-0000-0000-000000000000');
    expect(found).toBeNull();
  });

  it('getByOwner returns owner locations excluding deleted', async () => {
    const loc1 = await repo.create({ ...baseInput, ownerId, name: 'Loc 1' });
    const loc2 = await repo.create({ ...baseInput, ownerId, name: 'Loc 2' });
    await repo.softDelete(loc2.id);

    const result = await repo.getByOwner(ownerId);
    expect(result.data).toHaveLength(1);
    expect(result.data[0].id).toBe(loc1.id);
    expect(result.pagination.hasMore).toBe(false);
  });

  it('getByOwner paginates results', async () => {
    for (let i = 0; i < 5; i++) {
      await repo.create({ ...baseInput, ownerId, name: `Loc ${i}` });
    }

    const page1 = await repo.getByOwner(ownerId, undefined, 3);
    expect(page1.data).toHaveLength(3);
    expect(page1.pagination.hasMore).toBe(true);
    expect(page1.pagination.cursor).not.toBeNull();

    const page2 = await repo.getByOwner(ownerId, page1.pagination.cursor!, 3);
    expect(page2.data).toHaveLength(2);
    expect(page2.pagination.hasMore).toBe(false);
    expect(page2.pagination.cursor).toBeNull();
  });

  it('update increments version and updates fields', async () => {
    const loc = await repo.create({ ...baseInput, ownerId });
    expect(loc.version).toBe(1);

    const updated = await repo.update(loc.id, { name: 'Updated Name', hasFee: true });
    expect(updated.version).toBe(2);
    expect(updated.name).toBe('Updated Name');
    expect(updated.hasFee).toBe(true);
  });

  it('update increments version on repeated updates', async () => {
    const loc = await repo.create({ ...baseInput, ownerId });
    await repo.update(loc.id, { name: 'v2' });
    const v3 = await repo.update(loc.id, { name: 'v3' });
    expect(v3.version).toBe(3);
  });

  it('softDelete sets is_deleted and reason', async () => {
    const loc = await repo.create({ ...baseInput, ownerId });
    const result = await repo.softDelete(loc.id, 'duplicate entry');
    expect(result).toBe(true);

    const found = await repo.getById(loc.id);
    expect(found!.isDeleted).toBe(true);
    expect(found!.deletedReason).toBe('duplicate entry');
  });

  it('softDelete returns false for unknown id', async () => {
    const result = await repo.softDelete('00000000-0000-0000-0000-000000000000');
    expect(result).toBe(false);
  });

  it('getSharedWithUser returns empty result (Phase 6)', async () => {
    const result = await repo.getSharedWithUser(ownerId);
    expect(result.data).toHaveLength(0);
    expect(result.pagination.hasMore).toBe(false);
    expect(result.pagination.cursor).toBeNull();
  });

  it('getNearby returns locations within radius', async () => {
    // Sydney CBD ~-33.8688, 151.2093
    await repo.create({ ownerId, name: 'Near', latitude: -33.8690, longitude: 151.2095 });
    // Far away - Melbourne ~-37.8136, 144.9631
    await repo.create({ ownerId, name: 'Far', latitude: -37.8136, longitude: 144.9631 });

    const result = await repo.getNearby(-33.8688, 151.2093, 1.0); // 1km radius
    expect(result.data).toHaveLength(1);
    expect(result.data[0].name).toBe('Near');
  });

  it('getNearby excludes deleted locations', async () => {
    const loc = await repo.create({ ownerId, name: 'Deleted Near', latitude: -33.8690, longitude: 151.2095 });
    await repo.softDelete(loc.id);

    const result = await repo.getNearby(-33.8688, 151.2093, 1.0);
    expect(result.data).toHaveLength(0);
  });

  it('findDuplicates returns locations within radius in metres', async () => {
    // ~20 metres away
    await repo.create({ ownerId, name: 'Close', latitude: -33.86882, longitude: 151.20932 });
    // ~500 metres away
    await repo.create({ ownerId, name: 'Distant', latitude: -33.8733, longitude: 151.2093 });

    const dups = await repo.findDuplicates(-33.8688, 151.2093, 50);
    expect(dups).toHaveLength(1);
    expect(dups[0].name).toBe('Close');
  });

  it('findDuplicates excludes deleted locations', async () => {
    const loc = await repo.create({ ownerId, name: 'Close Deleted', latitude: -33.86882, longitude: 151.20932 });
    await repo.softDelete(loc.id);

    const dups = await repo.findDuplicates(-33.8688, 151.2093, 50);
    expect(dups).toHaveLength(0);
  });
});
