import type { Knex } from 'knex';
import { v4 as uuidv4 } from 'uuid';
import type { ChangeSuggestion, SuggestionStatus, PaginatedResult } from '../../models';
import { clampLimit } from '../../models';
import type { SuggestionRepository } from '../interfaces';

interface SuggestionRow {
  id: string;
  location_id: string;
  suggested_by: string;
  status: string;
  changes_json: string;
  message: string | null;
  response_msg: string | null;
  created_at: Date | string;
  responded_at: Date | string | null;
}

function mapRow(row: SuggestionRow): ChangeSuggestion {
  return {
    id: row.id,
    locationId: row.location_id,
    suggestedByUserId: row.suggested_by,
    status: row.status as SuggestionStatus,
    fieldName: row.changes_json,
    currentValue: null,
    suggestedValue: '',
    reason: row.message,
    reviewedByUserId: null,
    reviewedAt: row.responded_at ? new Date(row.responded_at) : null,
    reviewNote: row.response_msg,
    createdAt: new Date(row.created_at),
    updatedAt: new Date(row.created_at),
  };
}

export class PostgresSuggestionRepo implements SuggestionRepository {
  constructor(private readonly knex: Knex) {}

  async create(locationId: string, suggestedBy: string, changesJson: string, message?: string): Promise<ChangeSuggestion> {
    const id = uuidv4();
    const now = new Date();
    await this.knex('change_suggestions').insert({
      id,
      location_id: locationId,
      suggested_by: suggestedBy,
      status: 'PENDING',
      changes_json: changesJson,
      message: message ?? null,
      response_msg: null,
      created_at: now,
      responded_at: null,
    });
    const row = await this.knex('change_suggestions').where({ id }).first();
    return mapRow(row as SuggestionRow);
  }

  async getById(id: string): Promise<ChangeSuggestion | null> {
    const row = await this.knex('change_suggestions').where({ id }).first();
    return row ? mapRow(row as SuggestionRow) : null;
  }

  async getIncoming(ownerId: string, cursor?: string, limit?: number): Promise<PaginatedResult<ChangeSuggestion>> {
    const effectiveLimit = clampLimit(limit);

    let qb = this.knex('change_suggestions')
      .join('server_locations', 'change_suggestions.location_id', 'server_locations.id')
      .where('server_locations.owner_id', ownerId)
      .where('change_suggestions.status', 'PENDING')
      .select('change_suggestions.*')
      .orderBy('change_suggestions.id', 'asc')
      .limit(effectiveLimit + 1);

    if (cursor) {
      qb = qb.where('change_suggestions.id', '>', cursor);
    }

    const rows = await qb;
    const hasMore = rows.length > effectiveLimit;
    const data = rows.slice(0, effectiveLimit).map((r) => mapRow(r as SuggestionRow));
    const nextCursor = hasMore ? data[data.length - 1].id : null;

    return {
      data,
      pagination: { cursor: nextCursor, hasMore },
    };
  }

  async getOutgoing(userId: string, cursor?: string, limit?: number): Promise<PaginatedResult<ChangeSuggestion>> {
    const effectiveLimit = clampLimit(limit);

    let qb = this.knex('change_suggestions')
      .where({ suggested_by: userId })
      .orderBy('id', 'asc')
      .limit(effectiveLimit + 1);

    if (cursor) {
      qb = qb.where('id', '>', cursor);
    }

    const rows = await qb;
    const hasMore = rows.length > effectiveLimit;
    const data = rows.slice(0, effectiveLimit).map((r) => mapRow(r as SuggestionRow));
    const nextCursor = hasMore ? data[data.length - 1].id : null;

    return {
      data,
      pagination: { cursor: nextCursor, hasMore },
    };
  }

  async accept(id: string, responseMsg?: string): Promise<ChangeSuggestion> {
    const now = new Date();
    await this.knex('change_suggestions').where({ id }).update({
      status: 'ACCEPTED',
      response_msg: responseMsg ?? null,
      responded_at: now,
    });
    const row = await this.knex('change_suggestions').where({ id }).first();
    if (!row) throw new Error(`ChangeSuggestion ${id} not found`);
    return mapRow(row as SuggestionRow);
  }

  async reject(id: string, responseMsg?: string): Promise<ChangeSuggestion> {
    const now = new Date();
    await this.knex('change_suggestions').where({ id }).update({
      status: 'REJECTED',
      response_msg: responseMsg ?? null,
      responded_at: now,
    });
    const row = await this.knex('change_suggestions').where({ id }).first();
    if (!row) throw new Error(`ChangeSuggestion ${id} not found`);
    return mapRow(row as SuggestionRow);
  }

  async withdraw(id: string): Promise<ChangeSuggestion> {
    await this.knex('change_suggestions').where({ id, status: 'PENDING' }).update({
      status: 'WITHDRAWN',
    });
    const row = await this.knex('change_suggestions').where({ id }).first();
    if (!row) throw new Error(`ChangeSuggestion ${id} not found`);
    return mapRow(row as SuggestionRow);
  }
}
