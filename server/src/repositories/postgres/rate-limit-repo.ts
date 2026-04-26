import type { Knex } from 'knex';
import { v4 as uuidv4 } from 'uuid';
import type { RateLimit, CreateRateLimitInput, ResolvedRateLimit } from '../../models';
import type { RateLimitRepository } from '../interfaces';

interface RateLimitRow {
  id: string;
  scope: string;
  scope_id: string | null;
  endpoint: string;
  max_requests: number;
  window_seconds: number;
  created_at: Date | string;
  updated_at: Date | string;
}

function mapRow(row: RateLimitRow): RateLimit {
  return {
    id: row.id,
    scope: row.scope as RateLimit['scope'],
    scopeId: row.scope_id,
    endpoint: row.endpoint,
    maxRequests: row.max_requests,
    windowSeconds: row.window_seconds,
    createdAt: new Date(row.created_at),
    updatedAt: new Date(row.updated_at),
  };
}

export class PostgresRateLimitRepo implements RateLimitRepository {
  constructor(private readonly knex: Knex) {}

  async create(input: CreateRateLimitInput): Promise<RateLimit> {
    const id = uuidv4();
    const now = new Date();
    await this.knex('rate_limits').insert({
      id,
      scope: input.scope,
      scope_id: input.scopeId ?? null,
      endpoint: input.endpoint,
      max_requests: input.maxRequests,
      window_seconds: input.windowSeconds,
      created_at: now,
      updated_at: now,
    });
    const row = await this.knex('rate_limits').where({ id }).first();
    return mapRow(row as RateLimitRow);
  }

  async getById(id: string): Promise<RateLimit | null> {
    const row = await this.knex('rate_limits').where({ id }).first();
    return row ? mapRow(row as RateLimitRow) : null;
  }

  async update(id: string, fields: Partial<CreateRateLimitInput>): Promise<RateLimit> {
    const updateData: Record<string, unknown> = { updated_at: new Date() };
    if (fields.scope !== undefined) updateData.scope = fields.scope;
    if (fields.scopeId !== undefined) updateData.scope_id = fields.scopeId;
    if (fields.endpoint !== undefined) updateData.endpoint = fields.endpoint;
    if (fields.maxRequests !== undefined) updateData.max_requests = fields.maxRequests;
    if (fields.windowSeconds !== undefined) updateData.window_seconds = fields.windowSeconds;

    await this.knex('rate_limits').where({ id }).update(updateData);
    const row = await this.knex('rate_limits').where({ id }).first();
    return mapRow(row as RateLimitRow);
  }

  async delete(id: string): Promise<boolean> {
    const count = await this.knex('rate_limits').where({ id }).delete();
    return count > 0;
  }

  async getAll(): Promise<RateLimit[]> {
    const rows = await this.knex('rate_limits').select('*');
    return rows.map((r) => mapRow(r as RateLimitRow));
  }

  async getByScope(scope: string, scopeId?: string): Promise<RateLimit[]> {
    let qb = this.knex('rate_limits').where({ scope });
    if (scopeId !== undefined) {
      qb = qb.where({ scope_id: scopeId });
    }
    const rows = await qb;
    return rows.map((r) => mapRow(r as RateLimitRow));
  }

  async resolve(
    userId: string,
    endpoint: string,
    userGroupIds: string[]
  ): Promise<ResolvedRateLimit | null> {
    // 1. Check USER scope
    const userRows = await this.knex('rate_limits')
      .where({ scope: 'USER', scope_id: userId })
      .whereIn('endpoint', [endpoint, '*']);
    if (userRows.length > 0) {
      // prefer exact endpoint match over wildcard
      const exact = userRows.find((r: RateLimitRow) => r.endpoint === endpoint);
      const row = (exact ?? userRows[0]) as RateLimitRow;
      return {
        endpoint: row.endpoint,
        maxRequests: row.max_requests,
        windowSeconds: row.window_seconds,
        scope: 'USER',
        scopeId: row.scope_id,
      };
    }

    // 2. Check GROUP scope
    if (userGroupIds.length > 0) {
      const groupRows = await this.knex('rate_limits')
        .where({ scope: 'GROUP' })
        .whereIn('scope_id', userGroupIds)
        .whereIn('endpoint', [endpoint, '*']);
      if (groupRows.length > 0) {
        // most restrictive = lowest maxRequests
        const sorted = [...groupRows].sort(
          (a: RateLimitRow, b: RateLimitRow) => a.max_requests - b.max_requests
        );
        const row = sorted[0] as RateLimitRow;
        return {
          endpoint: row.endpoint,
          maxRequests: row.max_requests,
          windowSeconds: row.window_seconds,
          scope: 'GROUP',
          scopeId: row.scope_id,
        };
      }
    }

    // 3. Check GLOBAL scope
    const globalRows = await this.knex('rate_limits')
      .where({ scope: 'GLOBAL' })
      .whereIn('endpoint', [endpoint, '*']);
    if (globalRows.length > 0) {
      const exact = globalRows.find((r: RateLimitRow) => r.endpoint === endpoint);
      const row = (exact ?? globalRows[0]) as RateLimitRow;
      return {
        endpoint: row.endpoint,
        maxRequests: row.max_requests,
        windowSeconds: row.window_seconds,
        scope: 'GLOBAL',
        scopeId: row.scope_id,
      };
    }

    return null;
  }
}
