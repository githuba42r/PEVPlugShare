import type { Knex } from 'knex';
import { v4 as uuidv4 } from 'uuid';
import type { Location, CreateLocationInput, PaginatedResult } from '../../models';
import { clampLimit } from '../../models';
import type { LocationRepository } from '../interfaces';

interface LocationRow {
  id: string;
  owner_id: string;
  name: string;
  latitude: number;
  longitude: number;
  description: string | null;
  additional_info: string | null;
  opening_hours: string | null;
  socket_types: string | null;
  gpo_count: number | null;
  has_fee: boolean | number | null;
  charging_costs: string | null;
  public_private: string | null;
  provider_name: string | null;
  provider_website: string | null;
  business_name: string | null;
  street_address: string | null;
  suburb: string | null;
  state: string | null;
  postcode: string | null;
  color: string | null;
  icon: string | null;
  is_deleted: boolean | number;
  deleted_reason: string | null;
  created_at: Date | string;
  updated_at: Date | string;
  version: number;
}

function mapRow(row: LocationRow): Location {
  return {
    id: row.id,
    ownerId: row.owner_id,
    name: row.name,
    latitude: row.latitude,
    longitude: row.longitude,
    description: row.description,
    additionalInfo: row.additional_info,
    openingHours: row.opening_hours,
    socketTypes: row.socket_types ? row.socket_types.split(',').filter(Boolean) : [],
    gpoCount: row.gpo_count,
    hasFee: Boolean(row.has_fee),
    chargingCosts: row.charging_costs,
    publicPrivate: (row.public_private as Location['publicPrivate']) ?? 'PUBLIC',
    providerName: row.provider_name,
    providerWebsite: row.provider_website,
    businessName: row.business_name,
    streetAddress: row.street_address,
    suburb: row.suburb,
    state: row.state,
    postcode: row.postcode,
    color: row.color,
    icon: row.icon,
    isDeleted: Boolean(row.is_deleted),
    deletedReason: row.deleted_reason,
    createdAt: new Date(row.created_at),
    updatedAt: new Date(row.updated_at),
    version: row.version,
  };
}

function haversineKm(lat1: number, lon1: number, lat2: number, lon2: number): number {
  const R = 6371;
  const dLat = ((lat2 - lat1) * Math.PI) / 180;
  const dLon = ((lon2 - lon1) * Math.PI) / 180;
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos((lat1 * Math.PI) / 180) *
      Math.cos((lat2 * Math.PI) / 180) *
      Math.sin(dLon / 2) *
      Math.sin(dLon / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c;
}

export class PostgresLocationRepo implements LocationRepository {
  constructor(private readonly knex: Knex) {}

  async create(input: CreateLocationInput): Promise<Location> {
    const id = uuidv4();
    const now = new Date();
    await this.knex('server_locations').insert({
      id,
      owner_id: input.ownerId,
      name: input.name,
      latitude: input.latitude,
      longitude: input.longitude,
      description: input.description ?? null,
      additional_info: input.additionalInfo ?? null,
      opening_hours: input.openingHours ?? null,
      socket_types: input.socketTypes ? input.socketTypes.join(',') : null,
      gpo_count: input.gpoCount ?? null,
      has_fee: input.hasFee ?? false,
      charging_costs: input.chargingCosts ?? null,
      public_private: input.publicPrivate ?? 'PUBLIC',
      provider_name: input.providerName ?? null,
      provider_website: input.providerWebsite ?? null,
      business_name: input.businessName ?? null,
      street_address: input.streetAddress ?? null,
      suburb: input.suburb ?? null,
      state: input.state ?? null,
      postcode: input.postcode ?? null,
      color: input.color ?? null,
      icon: input.icon ?? null,
      is_deleted: false,
      deleted_reason: null,
      created_at: now,
      updated_at: now,
      version: 1,
    });
    const row = await this.knex('server_locations').where({ id }).first();
    return mapRow(row as LocationRow);
  }

  async getById(id: string): Promise<Location | null> {
    const row = await this.knex('server_locations').where({ id }).first();
    return row ? mapRow(row as LocationRow) : null;
  }

  async getByOwner(ownerId: string, cursor?: string, limit?: number): Promise<PaginatedResult<Location>> {
    const effectiveLimit = clampLimit(limit);

    let qb = this.knex('server_locations')
      .where({ owner_id: ownerId, is_deleted: false })
      .orderBy('id', 'asc')
      .limit(effectiveLimit + 1);

    if (cursor) {
      qb = qb.where('id', '>', cursor);
    }

    const rows = await qb;
    const hasMore = rows.length > effectiveLimit;
    const data = rows.slice(0, effectiveLimit).map((r) => mapRow(r as LocationRow));
    const nextCursor = hasMore ? data[data.length - 1].id : null;

    return {
      data,
      pagination: { cursor: nextCursor, hasMore },
    };
  }

  async update(id: string, fields: Partial<CreateLocationInput>): Promise<Location> {
    const updateData: Record<string, unknown> = { updated_at: new Date() };

    if (fields.name !== undefined) updateData.name = fields.name;
    if (fields.latitude !== undefined) updateData.latitude = fields.latitude;
    if (fields.longitude !== undefined) updateData.longitude = fields.longitude;
    if (fields.description !== undefined) updateData.description = fields.description;
    if (fields.additionalInfo !== undefined) updateData.additional_info = fields.additionalInfo;
    if (fields.openingHours !== undefined) updateData.opening_hours = fields.openingHours;
    if (fields.socketTypes !== undefined) updateData.socket_types = fields.socketTypes.join(',');
    if (fields.gpoCount !== undefined) updateData.gpo_count = fields.gpoCount;
    if (fields.hasFee !== undefined) updateData.has_fee = fields.hasFee;
    if (fields.chargingCosts !== undefined) updateData.charging_costs = fields.chargingCosts;
    if (fields.publicPrivate !== undefined) updateData.public_private = fields.publicPrivate;
    if (fields.providerName !== undefined) updateData.provider_name = fields.providerName;
    if (fields.providerWebsite !== undefined) updateData.provider_website = fields.providerWebsite;
    if (fields.businessName !== undefined) updateData.business_name = fields.businessName;
    if (fields.streetAddress !== undefined) updateData.street_address = fields.streetAddress;
    if (fields.suburb !== undefined) updateData.suburb = fields.suburb;
    if (fields.state !== undefined) updateData.state = fields.state;
    if (fields.postcode !== undefined) updateData.postcode = fields.postcode;
    if (fields.color !== undefined) updateData.color = fields.color;
    if (fields.icon !== undefined) updateData.icon = fields.icon;

    await this.knex('server_locations').where({ id }).update({
      ...updateData,
      version: this.knex.raw('version + 1'),
    });

    const updated = await this.getById(id);
    if (!updated) throw new Error(`Location ${id} not found after update`);
    return updated;
  }

  async softDelete(id: string, reason?: string): Promise<boolean> {
    const count = await this.knex('server_locations').where({ id }).update({
      is_deleted: true,
      deleted_reason: reason ?? null,
      updated_at: new Date(),
    });
    return count > 0;
  }

  async getSharedWithUser(_userId: string, _cursor?: string, _limit?: number): Promise<PaginatedResult<Location>> {
    // Sharing not implemented until Phase 6
    return { data: [], pagination: { cursor: null, hasMore: false } };
  }

  async getNearby(lat: number, lon: number, radiusKm: number, cursor?: string, limit?: number): Promise<PaginatedResult<Location>> {
    const effectiveLimit = clampLimit(limit);
    const latDelta = radiusKm / 111.0;
    const lonDelta = radiusKm / (111.0 * Math.cos((lat * Math.PI) / 180));

    let qb = this.knex('server_locations')
      .where('is_deleted', false)
      .whereBetween('latitude', [lat - latDelta, lat + latDelta])
      .whereBetween('longitude', [lon - lonDelta, lon + lonDelta])
      .orderBy('id', 'asc');

    if (cursor) {
      qb = qb.where('id', '>', cursor);
    }

    const rows = await qb;

    // Post-filter with Haversine for accurate radius
    const filtered = rows
      .map((r) => mapRow(r as LocationRow))
      .filter((loc) => haversineKm(lat, lon, loc.latitude, loc.longitude) <= radiusKm);

    const hasMore = filtered.length > effectiveLimit;
    const data = filtered.slice(0, effectiveLimit);
    const nextCursor = hasMore ? data[data.length - 1].id : null;

    return {
      data,
      pagination: { cursor: nextCursor, hasMore },
    };
  }

  async findDuplicates(lat: number, lon: number, radiusMetres: number): Promise<Location[]> {
    const radiusKm = radiusMetres / 1000;
    const latDelta = radiusKm / 111.0;
    const lonDelta = radiusKm / (111.0 * Math.cos((lat * Math.PI) / 180));

    const rows = await this.knex('server_locations')
      .where('is_deleted', false)
      .whereBetween('latitude', [lat - latDelta, lat + latDelta])
      .whereBetween('longitude', [lon - lonDelta, lon + lonDelta]);

    return rows
      .map((r) => mapRow(r as LocationRow))
      .filter((loc) => haversineKm(lat, lon, loc.latitude, loc.longitude) <= radiusKm);
  }
}
