import type { LocationRepository } from '../repositories/interfaces';
import type { Location } from '../models';

const DUPLICATE_RADIUS_METRES = 50;

export class DuplicateDetectionService {
  constructor(private readonly locationRepo: LocationRepository) {}

  async findDuplicates(lat: number, lon: number, excludeId?: string): Promise<Location[]> {
    const duplicates = await this.locationRepo.findDuplicates(lat, lon, DUPLICATE_RADIUS_METRES);
    return excludeId ? duplicates.filter((loc) => loc.id !== excludeId) : duplicates;
  }

  async hasDuplicates(lat: number, lon: number, excludeId?: string): Promise<boolean> {
    return (await this.findDuplicates(lat, lon, excludeId)).length > 0;
  }
}
