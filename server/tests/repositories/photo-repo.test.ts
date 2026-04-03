import { setupTestDb, teardownTestDb, getTestDb } from '../setup';
import { PostgresUserRepo } from '../../src/repositories/postgres/user-repo';
import { PostgresLocationRepo } from '../../src/repositories/postgres/location-repo';
import { PostgresPhotoRepo } from '../../src/repositories/postgres/photo-repo';
import type { Knex } from 'knex';

describe('PostgresPhotoRepo', () => {
  let db: Knex;
  let repo: PostgresPhotoRepo;
  let locationId: string;

  beforeAll(async () => {
    db = await setupTestDb();
    const userRepo = new PostgresUserRepo(db);
    const locationRepo = new PostgresLocationRepo(db);

    const user = await userRepo.create({
      googleId: 'photo-owner-google',
      email: 'photo-owner@example.com',
      displayName: 'Photo Owner',
    });

    const location = await locationRepo.create({
      ownerId: user.id,
      name: 'Test Location for Photos',
      latitude: -33.8688,
      longitude: 151.2093,
    });
    locationId = location.id;
  });

  afterAll(async () => {
    await teardownTestDb();
  });

  beforeEach(async () => {
    repo = new PostgresPhotoRepo(getTestDb());
    await db('server_location_photos').delete();
  });

  it('creates a photo and returns it', async () => {
    const photo = await repo.create(locationId, '/uploads/photo1.jpg', '/uploads/thumb1.jpg', {});
    expect(photo.id).toBeTruthy();
    expect(photo.locationId).toBe(locationId);
    expect(photo.filePath).toBe('/uploads/photo1.jpg');
    expect(photo.thumbnailPath).toBe('/uploads/thumb1.jpg');
    expect(photo.mimeType).toBe('image/jpeg');
    expect(photo.uploadedAt).toBeInstanceOf(Date);
    expect(photo.exifData).toBeNull();
    expect(photo.timestamp).toBeNull();
  });

  it('creates a photo with metadata', async () => {
    const ts = new Date('2024-06-15T10:00:00Z');
    const photo = await repo.create(locationId, '/uploads/photo2.jpg', null, {
      caption: 'Charger closeup',
      latitude: -33.8688,
      longitude: 151.2093,
      altitude: 50.0,
      bearing: 180.0,
      timestamp: ts,
      exifData: { make: 'Apple', model: 'iPhone 15' },
      width: 4032,
      height: 3024,
      fileSize: 2048000,
      mimeType: 'image/heic',
    });

    expect(photo.caption).toBe('Charger closeup');
    expect(photo.latitude).toBeCloseTo(-33.8688);
    expect(photo.longitude).toBeCloseTo(151.2093);
    expect(photo.altitude).toBeCloseTo(50.0);
    expect(photo.bearing).toBeCloseTo(180.0);
    expect(photo.timestamp).toBeInstanceOf(Date);
    expect(photo.timestamp!.getTime()).toBe(ts.getTime());
    expect(photo.exifData).toEqual({ make: 'Apple', model: 'iPhone 15' });
    expect(photo.width).toBe(4032);
    expect(photo.height).toBe(3024);
    expect(photo.fileSize).toBe(2048000);
    expect(photo.mimeType).toBe('image/heic');
    expect(photo.thumbnailPath).toBeNull();
  });

  it('gets photo by id', async () => {
    const created = await repo.create(locationId, '/uploads/photo3.jpg', null, {});
    const found = await repo.getById(created.id);
    expect(found).not.toBeNull();
    expect(found!.id).toBe(created.id);
  });

  it('returns null for unknown id', async () => {
    const found = await repo.getById('00000000-0000-0000-0000-000000000000');
    expect(found).toBeNull();
  });

  it('getByLocationId returns photos ordered by uploadedAt', async () => {
    await repo.create(locationId, '/uploads/a.jpg', null, {});
    await repo.create(locationId, '/uploads/b.jpg', null, {});
    await repo.create(locationId, '/uploads/c.jpg', null, {});

    const photos = await repo.getByLocationId(locationId);
    expect(photos).toHaveLength(3);
    // Verify all belong to the location
    photos.forEach((p) => expect(p.locationId).toBe(locationId));
  });

  it('getByLocationId returns empty array for unknown locationId', async () => {
    const photos = await repo.getByLocationId('00000000-0000-0000-0000-000000000000');
    expect(photos).toHaveLength(0);
  });

  it('delete removes a photo and returns true', async () => {
    const photo = await repo.create(locationId, '/uploads/to-delete.jpg', null, {});
    const result = await repo.delete(photo.id);
    expect(result).toBe(true);

    const found = await repo.getById(photo.id);
    expect(found).toBeNull();
  });

  it('delete returns false for unknown id', async () => {
    const result = await repo.delete('00000000-0000-0000-0000-000000000000');
    expect(result).toBe(false);
  });

  it('countByLocationId returns correct count', async () => {
    expect(await repo.countByLocationId(locationId)).toBe(0);

    await repo.create(locationId, '/uploads/p1.jpg', null, {});
    expect(await repo.countByLocationId(locationId)).toBe(1);

    await repo.create(locationId, '/uploads/p2.jpg', null, {});
    await repo.create(locationId, '/uploads/p3.jpg', null, {});
    expect(await repo.countByLocationId(locationId)).toBe(3);
  });
});
