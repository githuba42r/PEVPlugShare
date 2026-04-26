import type { Knex } from 'knex';
import { v4 as uuidv4 } from 'uuid';
import type { LocationShare, HiddenShare, Location, PaginatedResult } from '../../models';
import { clampLimit } from '../../models';
import type { ShareRepository } from '../interfaces';

interface LocationShareRow {
  id: string;
  location_id: string;
  shared_by: string;
  shared_with_user: string | null;
  shared_with_group: string | null;
  is_active: boolean | number;
  shared_at: Date | string;
}

interface HiddenShareRow {
  id: string;
  user_id: string;
  location_id: string;
  share_id: string;
  hidden_at: Date | string;
}

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

function mapShareRow(row: LocationShareRow): LocationShare {
  return {
    id: row.id,
    locationId: row.location_id,
    sharedByUserId: row.shared_by,
    sharedWithUserId: row.shared_with_user,
    sharedWithGroupId: row.shared_with_group,
    canEdit: false,
    createdAt: new Date(row.shared_at),
    expiresAt: null,
  };
}

function mapHiddenShareRow(row: HiddenShareRow): HiddenShare {
  return {
    id: row.id,
    locationId: row.location_id,
    hiddenByUserId: row.user_id,
    createdAt: new Date(row.hidden_at),
  };
}

function mapLocationRow(row: LocationRow): Location {
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

export class PostgresShareRepo implements ShareRepository {
  constructor(private readonly knex: Knex) {}

  async shareWithUser(locationId: string, sharedBy: string, sharedWithUser: string): Promise<LocationShare> {
    const id = uuidv4();
    const now = new Date();
    await this.knex('location_shares').insert({
      id,
      location_id: locationId,
      shared_by: sharedBy,
      shared_with_user: sharedWithUser,
      shared_with_group: null,
      is_active: true,
      shared_at: now,
    });
    const row = await this.knex('location_shares').where({ id }).first();
    return mapShareRow(row as LocationShareRow);
  }

  async shareWithGroup(locationId: string, sharedBy: string, sharedWithGroup: string): Promise<LocationShare> {
    const id = uuidv4();
    const now = new Date();
    await this.knex('location_shares').insert({
      id,
      location_id: locationId,
      shared_by: sharedBy,
      shared_with_user: null,
      shared_with_group: sharedWithGroup,
      is_active: true,
      shared_at: now,
    });
    const row = await this.knex('location_shares').where({ id }).first();
    return mapShareRow(row as LocationShareRow);
  }

  async revokeShare(shareId: string): Promise<boolean> {
    const count = await this.knex('location_shares').where({ id: shareId }).delete();
    return count > 0;
  }

  async getSharesForLocation(locationId: string, cursor?: string, limit?: number): Promise<PaginatedResult<LocationShare>> {
    const effectiveLimit = clampLimit(limit);

    let qb = this.knex('location_shares')
      .where({ location_id: locationId })
      .orderBy('id', 'asc')
      .limit(effectiveLimit + 1);

    if (cursor) {
      qb = qb.where('id', '>', cursor);
    }

    const rows = await qb;
    const hasMore = rows.length > effectiveLimit;
    const data = rows.slice(0, effectiveLimit).map((r) => mapShareRow(r as LocationShareRow));
    const nextCursor = hasMore ? data[data.length - 1].id : null;

    return {
      data,
      pagination: { cursor: nextCursor, hasMore },
    };
  }

  async getLocationsSharedWithUser(
    userId: string,
    userGroupIds: string[],
    cursor?: string,
    limit?: number,
  ): Promise<PaginatedResult<Location>> {
    const effectiveLimit = clampLimit(limit);

    const hiddenSubquery = this.knex('hidden_shares').where({ user_id: userId }).select('share_id');

    let qb = this.knex('server_locations')
      .join('location_shares', 'server_locations.id', 'location_shares.location_id')
      .where('server_locations.is_deleted', false)
      .where('location_shares.is_active', true)
      .where((builder) => {
        builder.where('location_shares.shared_with_user', userId);
        if (userGroupIds.length > 0) {
          builder.orWhereIn('location_shares.shared_with_group', userGroupIds);
        }
      })
      .whereNotIn('location_shares.id', hiddenSubquery)
      .select('server_locations.*', 'location_shares.id as share_id_for_cursor')
      .orderBy('location_shares.id', 'asc')
      .limit(effectiveLimit + 1);

    if (cursor) {
      qb = qb.where('location_shares.id', '>', cursor);
    }

    const rows = await qb;
    const hasMore = rows.length > effectiveLimit;
    const data = rows.slice(0, effectiveLimit).map((r) => mapLocationRow(r as LocationRow));
    // cursor is based on the share id to allow stable pagination
    const nextCursor = hasMore ? (rows[effectiveLimit - 1] as any).share_id_for_cursor : null;

    return {
      data,
      pagination: { cursor: nextCursor, hasMore },
    };
  }

  async deactivateSharesForLocation(locationId: string): Promise<void> {
    await this.knex('location_shares').where({ location_id: locationId }).update({ is_active: false });
  }

  async isSharedWithUser(locationId: string, userId: string, userGroupIds: string[]): Promise<boolean> {
    let qb = this.knex('location_shares')
      .where({ location_id: locationId, is_active: true })
      .where((builder) => {
        builder.where('shared_with_user', userId);
        if (userGroupIds.length > 0) {
          builder.orWhereIn('shared_with_group', userGroupIds);
        }
      })
      .first();

    const row = await qb;
    return row !== undefined;
  }

  async hideShare(userId: string, locationId: string, shareId: string): Promise<HiddenShare> {
    const id = uuidv4();
    const now = new Date();
    await this.knex('hidden_shares').insert({
      id,
      user_id: userId,
      location_id: locationId,
      share_id: shareId,
      hidden_at: now,
    });
    const row = await this.knex('hidden_shares').where({ id }).first();
    return mapHiddenShareRow(row as HiddenShareRow);
  }

  async unhideShare(userId: string, shareId: string): Promise<boolean> {
    const count = await this.knex('hidden_shares').where({ user_id: userId, share_id: shareId }).delete();
    return count > 0;
  }

  async getHiddenShareIds(userId: string): Promise<string[]> {
    const rows = await this.knex('hidden_shares').where({ user_id: userId }).select('share_id');
    return rows.map((r: { share_id: string }) => r.share_id);
  }

  async unhideByLocationVersion(locationId: string): Promise<void> {
    await this.knex('hidden_shares').where({ location_id: locationId }).delete();
  }
}
