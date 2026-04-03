import { DuplicateDetectionService } from '../../src/services/duplicate-detection-service';
import type { LocationRepository } from '../../src/repositories/interfaces';
import type { Location } from '../../src/models';

function makeLocation(id: string, lat: number, lon: number): Location {
  return {
    id,
    ownerId: 'owner-1',
    name: `Location ${id}`,
    latitude: lat,
    longitude: lon,
    description: null,
    additionalInfo: null,
    openingHours: null,
    socketTypes: [],
    gpoCount: null,
    hasFee: false,
    chargingCosts: null,
    publicPrivate: 'PUBLIC',
    providerName: null,
    providerWebsite: null,
    businessName: null,
    streetAddress: null,
    suburb: null,
    state: null,
    postcode: null,
    color: null,
    icon: null,
    isDeleted: false,
    deletedReason: null,
    createdAt: new Date(),
    updatedAt: new Date(),
    version: 1,
  };
}

function makeRepo(locations: Location[]): LocationRepository {
  return {
    create: jest.fn(),
    getById: jest.fn(),
    getByOwner: jest.fn(),
    update: jest.fn(),
    softDelete: jest.fn(),
    getSharedWithUser: jest.fn(),
    getNearby: jest.fn(),
    findDuplicates: jest.fn().mockResolvedValue(locations),
  };
}

describe('DuplicateDetectionService', () => {
  const lat = -33.8688;
  const lon = 151.2093;

  it('findDuplicates returns results from repo', async () => {
    const locs = [makeLocation('loc-1', lat, lon), makeLocation('loc-2', lat, lon)];
    const repo = makeRepo(locs);
    const service = new DuplicateDetectionService(repo);

    const result = await service.findDuplicates(lat, lon);
    expect(result).toHaveLength(2);
    expect(result[0].id).toBe('loc-1');
    expect(result[1].id).toBe('loc-2');
    expect(repo.findDuplicates).toHaveBeenCalledWith(lat, lon, 50);
  });

  it('findDuplicates excludes specified ID', async () => {
    const locs = [makeLocation('loc-1', lat, lon), makeLocation('loc-2', lat, lon)];
    const repo = makeRepo(locs);
    const service = new DuplicateDetectionService(repo);

    const result = await service.findDuplicates(lat, lon, 'loc-1');
    expect(result).toHaveLength(1);
    expect(result[0].id).toBe('loc-2');
  });

  it('findDuplicates returns all when excludeId does not match', async () => {
    const locs = [makeLocation('loc-1', lat, lon)];
    const repo = makeRepo(locs);
    const service = new DuplicateDetectionService(repo);

    const result = await service.findDuplicates(lat, lon, 'nonexistent-id');
    expect(result).toHaveLength(1);
  });

  it('hasDuplicates returns true when duplicates exist', async () => {
    const locs = [makeLocation('loc-1', lat, lon)];
    const repo = makeRepo(locs);
    const service = new DuplicateDetectionService(repo);

    const result = await service.hasDuplicates(lat, lon);
    expect(result).toBe(true);
  });

  it('hasDuplicates returns false when no duplicates', async () => {
    const repo = makeRepo([]);
    const service = new DuplicateDetectionService(repo);

    const result = await service.hasDuplicates(lat, lon);
    expect(result).toBe(false);
  });

  it('hasDuplicates returns false when only excluded ID matches', async () => {
    const locs = [makeLocation('loc-1', lat, lon)];
    const repo = makeRepo(locs);
    const service = new DuplicateDetectionService(repo);

    const result = await service.hasDuplicates(lat, lon, 'loc-1');
    expect(result).toBe(false);
  });

  it('findDuplicates returns empty when repo returns no results', async () => {
    const repo = makeRepo([]);
    const service = new DuplicateDetectionService(repo);

    const result = await service.findDuplicates(lat, lon);
    expect(result).toHaveLength(0);
  });
});
