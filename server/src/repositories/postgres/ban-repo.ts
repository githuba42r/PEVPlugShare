import type { Knex } from 'knex';
import { v4 as uuidv4 } from 'uuid';
import type { BannedIdentifier, BanIdentifierType, SharingBan } from '../../models';
import type { BanRepository } from '../interfaces';

interface BannedIdentifierRow {
  id: string;
  identifier_type: string;
  identifier: string;
  reason: string | null;
  banned_by: string | null;
  created_at: Date | string;
}

interface SharingBanRow {
  id: string;
  user_id: string;
  reason: string | null;
  banned_by: string;
  expires_at: Date | string | null;
  created_at: Date | string;
}

function mapBannedIdentifierRow(row: BannedIdentifierRow): BannedIdentifier {
  return {
    id: row.id,
    identifierType: row.identifier_type as BanIdentifierType,
    identifierValue: row.identifier,
    reason: row.reason,
    bannedByUserId: row.banned_by ?? '',
    createdAt: new Date(row.created_at),
    expiresAt: null,
  };
}

function mapSharingBanRow(row: SharingBanRow): SharingBan {
  return {
    id: row.id,
    bannedByUserId: row.banned_by,
    bannedUserId: row.user_id,
    reason: row.reason,
    createdAt: new Date(row.created_at),
  };
}

export class PostgresBanRepo implements BanRepository {
  constructor(private readonly knex: Knex) {}

  async addBannedIdentifier(
    type: BanIdentifierType,
    identifier: string,
    reason?: string,
    bannedBy?: string
  ): Promise<BannedIdentifier> {
    const id = uuidv4();
    const now = new Date();
    await this.knex('banned_identifiers').insert({
      id,
      identifier_type: type,
      identifier,
      reason: reason ?? null,
      banned_by: bannedBy ?? null,
      created_at: now,
    });
    const row = await this.knex('banned_identifiers').where({ id }).first();
    return mapBannedIdentifierRow(row as BannedIdentifierRow);
  }

  async removeBannedIdentifier(id: string): Promise<boolean> {
    const count = await this.knex('banned_identifiers').where({ id }).delete();
    return count > 0;
  }

  async isBanned(type: BanIdentifierType, identifier: string): Promise<boolean> {
    const row = await this.knex('banned_identifiers')
      .where({ identifier_type: type, identifier })
      .first();
    return row !== undefined;
  }

  async listBannedIdentifiers(): Promise<BannedIdentifier[]> {
    const rows = await this.knex('banned_identifiers').select('*');
    return rows.map((r) => mapBannedIdentifierRow(r as BannedIdentifierRow));
  }

  async addSharingBan(
    userId: string,
    reason: string,
    bannedBy: string,
    expiresAt?: Date
  ): Promise<SharingBan> {
    const id = uuidv4();
    const now = new Date();
    await this.knex('sharing_bans').insert({
      id,
      user_id: userId,
      reason,
      banned_by: bannedBy,
      expires_at: expiresAt ?? null,
      created_at: now,
    });
    const row = await this.knex('sharing_bans').where({ id }).first();
    return mapSharingBanRow(row as SharingBanRow);
  }

  async removeSharingBan(id: string): Promise<boolean> {
    const count = await this.knex('sharing_bans').where({ id }).delete();
    return count > 0;
  }

  async hasActiveSharingBan(userId: string): Promise<boolean> {
    const now = new Date();
    const row = await this.knex('sharing_bans')
      .where({ user_id: userId })
      .where((builder) => {
        builder.whereNull('expires_at').orWhere('expires_at', '>', now);
      })
      .first();
    return row !== undefined;
  }

  async listSharingBans(): Promise<SharingBan[]> {
    const rows = await this.knex('sharing_bans').select('*');
    return rows.map((r) => mapSharingBanRow(r as SharingBanRow));
  }
}
