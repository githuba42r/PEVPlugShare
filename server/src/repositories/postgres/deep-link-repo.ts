import type { Knex } from 'knex';
import { v4 as uuidv4 } from 'uuid';
import type { DirectShareToken } from '../../models';
import type { DeepLinkRepository } from '../interfaces';

interface DirectShareTokenRow {
  id: string;
  location_id: string;
  created_by: string;
  token: string;
  is_single_use: boolean | number;
  use_count: number;
  max_uses: number | null;
  expires_at: Date | string | null;
  created_at: Date | string;
}

function mapRow(row: DirectShareTokenRow): DirectShareToken {
  return {
    id: row.id,
    token: row.token,
    locationId: row.location_id,
    createdByUserId: row.created_by,
    maxUses: row.max_uses,
    useCount: row.use_count,
    createdAt: new Date(row.created_at),
    expiresAt: row.expires_at ? new Date(row.expires_at) : null,
  };
}

export class PostgresDeepLinkRepo implements DeepLinkRepository {
  constructor(private readonly knex: Knex) {}

  async createToken(
    locationId: string,
    createdBy: string,
    token: string,
    isSingleUse?: boolean,
    maxUses?: number,
    expiresAt?: Date,
  ): Promise<DirectShareToken> {
    const id = uuidv4();
    const now = new Date();
    await this.knex('direct_share_tokens').insert({
      id,
      location_id: locationId,
      created_by: createdBy,
      token,
      is_single_use: isSingleUse ?? false,
      use_count: 0,
      max_uses: maxUses ?? null,
      expires_at: expiresAt ?? null,
      created_at: now,
    });
    const row = await this.knex('direct_share_tokens').where({ id }).first();
    return mapRow(row as DirectShareTokenRow);
  }

  async getByToken(token: string): Promise<DirectShareToken | null> {
    const now = new Date();
    const row = await this.knex('direct_share_tokens')
      .where({ token })
      .where((builder) => {
        builder.whereNull('expires_at').orWhere('expires_at', '>', now);
      })
      .where((builder) => {
        builder.whereNull('max_uses').orWhereRaw('use_count < max_uses');
      })
      .first();
    return row ? mapRow(row as DirectShareTokenRow) : null;
  }

  async incrementUseCount(id: string): Promise<void> {
    await this.knex('direct_share_tokens').where({ id }).update({
      use_count: this.knex.raw('use_count + 1'),
    });
  }

  async delete(id: string): Promise<boolean> {
    const count = await this.knex('direct_share_tokens').where({ id }).delete();
    return count > 0;
  }
}
