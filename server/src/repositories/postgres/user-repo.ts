import type { Knex } from 'knex';
import { v4 as uuidv4 } from 'uuid';
import type { User, CreateUserInput, PaginatedResult } from '../../models';
import { clampLimit } from '../../models';
import type { UserRepository } from '../interfaces';

interface UserRow {
  id: string;
  google_id: string;
  email: string;
  display_name: string;
  avatar_url: string | null;
  is_admin: boolean | number;
  is_banned: boolean | number;
  ban_reason: string | null;
  banned_at: Date | string | null;
  banned_by: string | null;
  created_at: Date | string;
  last_seen_at: Date | string;
}

function mapRow(row: UserRow): User {
  return {
    id: row.id,
    googleId: row.google_id,
    email: row.email,
    displayName: row.display_name,
    avatarUrl: row.avatar_url,
    isAdmin: Boolean(row.is_admin),
    isBanned: Boolean(row.is_banned),
    banReason: row.ban_reason,
    bannedAt: row.banned_at ? new Date(row.banned_at) : null,
    bannedBy: row.banned_by,
    createdAt: new Date(row.created_at),
    lastSeenAt: new Date(row.last_seen_at),
  };
}

export class PostgresUserRepo implements UserRepository {
  constructor(private readonly knex: Knex) {}

  async create(input: CreateUserInput): Promise<User> {
    const id = uuidv4();
    const now = new Date();
    await this.knex('users').insert({
      id,
      google_id: input.googleId,
      email: input.email,
      display_name: input.displayName,
      avatar_url: input.avatarUrl ?? null,
      is_admin: false,
      is_banned: false,
      ban_reason: null,
      banned_at: null,
      banned_by: null,
      created_at: now,
      last_seen_at: now,
    });
    const row = await this.knex('users').where({ id }).first();
    return mapRow(row as UserRow);
  }

  async getById(id: string): Promise<User | null> {
    const row = await this.knex('users').where({ id }).first();
    return row ? mapRow(row as UserRow) : null;
  }

  async getByGoogleId(googleId: string): Promise<User | null> {
    const row = await this.knex('users').where({ google_id: googleId }).first();
    return row ? mapRow(row as UserRow) : null;
  }

  async getByEmail(email: string): Promise<User | null> {
    const row = await this.knex('users').where({ email }).first();
    return row ? mapRow(row as UserRow) : null;
  }

  async updateLastSeen(id: string): Promise<void> {
    await this.knex('users').where({ id }).update({ last_seen_at: new Date() });
  }

  async setAdmin(id: string, isAdmin: boolean): Promise<void> {
    await this.knex('users').where({ id }).update({ is_admin: isAdmin });
  }

  async setBanned(id: string, isBanned: boolean, reason?: string, bannedBy?: string): Promise<void> {
    await this.knex('users').where({ id }).update({
      is_banned: isBanned,
      ban_reason: isBanned ? (reason ?? null) : null,
      banned_at: isBanned ? new Date() : null,
      banned_by: isBanned ? (bannedBy ?? null) : null,
    });
  }

  async search(query: string, cursor?: string, limit?: number): Promise<PaginatedResult<User>> {
    const effectiveLimit = clampLimit(limit);
    const like = `%${query}%`;

    let qb = this.knex('users')
      .where((builder) => {
        builder.where('email', 'like', like).orWhere('display_name', 'like', like);
      })
      .orderBy('id', 'asc')
      .limit(effectiveLimit + 1);

    if (cursor) {
      qb = qb.where('id', '>', cursor);
    }

    const rows = await qb;
    const hasMore = rows.length > effectiveLimit;
    const data = rows.slice(0, effectiveLimit).map((r) => mapRow(r as UserRow));
    const nextCursor = hasMore ? data[data.length - 1].id : null;

    return {
      data,
      pagination: { cursor: nextCursor, hasMore },
    };
  }

  async count(): Promise<number> {
    const result = await this.knex('users').count('* as count').first();
    return parseInt(String((result as { count: string | number }).count), 10);
  }
}
