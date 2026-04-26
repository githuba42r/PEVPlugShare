import type { Knex } from 'knex';
import { v4 as uuidv4 } from 'uuid';
import type { Friendship, FriendshipStatus, User, PaginatedResult } from '../../models';
import { clampLimit } from '../../models';
import type { FriendRepository } from '../interfaces';

interface FriendshipRow {
  id: string;
  requester_id: string;
  addressee_id: string;
  status: string;
  requested_at: Date | string;
  responded_at: Date | string | null;
}

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

function mapFriendshipRow(row: FriendshipRow): Friendship {
  return {
    id: row.id,
    requesterId: row.requester_id,
    addresseeId: row.addressee_id,
    status: row.status as FriendshipStatus,
    createdAt: new Date(row.requested_at),
    updatedAt: row.responded_at ? new Date(row.responded_at) : new Date(row.requested_at),
  };
}

function mapUserRow(row: UserRow): User {
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

export class MysqlFriendRepo implements FriendRepository {
  constructor(private readonly knex: Knex) {}

  async sendRequest(requesterId: string, addresseeId: string): Promise<Friendship> {
    // Check no existing friendship in either direction
    const existing = await this.knex('friendships')
      .where((b) => {
        b.where({ requester_id: requesterId, addressee_id: addresseeId }).orWhere({
          requester_id: addresseeId,
          addressee_id: requesterId,
        });
      })
      .first();

    if (existing) {
      throw new Error('A friendship or request already exists between these users');
    }

    const id = uuidv4();
    const now = new Date();
    await this.knex('friendships').insert({
      id,
      requester_id: requesterId,
      addressee_id: addresseeId,
      status: 'PENDING',
      requested_at: now,
      responded_at: null,
    });

    const row = await this.knex('friendships').where({ id }).first();
    return mapFriendshipRow(row as FriendshipRow);
  }

  async getRequestById(id: string): Promise<Friendship | null> {
    const row = await this.knex('friendships').where({ id }).first();
    return row ? mapFriendshipRow(row as FriendshipRow) : null;
  }

  async acceptRequest(id: string): Promise<Friendship> {
    const now = new Date();
    const count = await this.knex('friendships')
      .where({ id, status: 'PENDING' })
      .update({ status: 'ACCEPTED', responded_at: now });

    if (count === 0) {
      throw new Error('Friendship request not found or not in PENDING state');
    }

    const row = await this.knex('friendships').where({ id }).first();
    return mapFriendshipRow(row as FriendshipRow);
  }

  async declineRequest(id: string): Promise<Friendship> {
    const now = new Date();
    const count = await this.knex('friendships')
      .where({ id, status: 'PENDING' })
      .update({ status: 'DECLINED', responded_at: now });

    if (count === 0) {
      throw new Error('Friendship request not found or not in PENDING state');
    }

    const row = await this.knex('friendships').where({ id }).first();
    return mapFriendshipRow(row as FriendshipRow);
  }

  async removeFriend(userId: string, friendId: string): Promise<boolean> {
    const count = await this.knex('friendships')
      .where((b) => {
        b.where({ requester_id: userId, addressee_id: friendId }).orWhere({
          requester_id: friendId,
          addressee_id: userId,
        });
      })
      .andWhere('status', 'ACCEPTED')
      .delete();

    return count > 0;
  }

  async getFriends(userId: string, cursor?: string, limit?: number): Promise<PaginatedResult<User>> {
    const effectiveLimit = clampLimit(limit);

    // Get accepted friendships and join with the other party's user record
    // using a raw join condition to select the friend (not the caller)
    let qb = this.knex
      .select('users.*', 'friendships.id as friendship_id')
      .from('friendships')
      .join('users', this.knex.raw(
        `users.id = CASE WHEN friendships.requester_id = ? THEN friendships.addressee_id ELSE friendships.requester_id END`,
        [userId],
      ))
      .where((b) => {
        b.where('friendships.requester_id', userId).orWhere('friendships.addressee_id', userId);
      })
      .andWhere('friendships.status', 'ACCEPTED')
      .orderBy('friendships.id', 'asc')
      .limit(effectiveLimit + 1);

    if (cursor) {
      qb = qb.where('friendships.id', '>', cursor);
    }

    const rows = await qb;
    const hasMore = rows.length > effectiveLimit;
    const data = rows.slice(0, effectiveLimit).map((r) => mapUserRow(r as unknown as UserRow));
    const nextCursor = hasMore ? (rows[effectiveLimit - 1] as { friendship_id: string }).friendship_id : null;

    return {
      data,
      pagination: { cursor: nextCursor, hasMore },
    };
  }

  async getIncomingRequests(
    userId: string,
    cursor?: string,
    limit?: number,
  ): Promise<PaginatedResult<Friendship>> {
    const effectiveLimit = clampLimit(limit);

    let qb = this.knex('friendships')
      .where({ addressee_id: userId, status: 'PENDING' })
      .orderBy('id', 'asc')
      .limit(effectiveLimit + 1);

    if (cursor) {
      qb = qb.where('id', '>', cursor);
    }

    const rows = await qb;
    const hasMore = rows.length > effectiveLimit;
    const data = rows.slice(0, effectiveLimit).map((r) => mapFriendshipRow(r as FriendshipRow));
    const nextCursor = hasMore ? data[data.length - 1].id : null;

    return {
      data,
      pagination: { cursor: nextCursor, hasMore },
    };
  }

  async getOutgoingRequests(
    userId: string,
    cursor?: string,
    limit?: number,
  ): Promise<PaginatedResult<Friendship>> {
    const effectiveLimit = clampLimit(limit);

    let qb = this.knex('friendships')
      .where({ requester_id: userId, status: 'PENDING' })
      .orderBy('id', 'asc')
      .limit(effectiveLimit + 1);

    if (cursor) {
      qb = qb.where('id', '>', cursor);
    }

    const rows = await qb;
    const hasMore = rows.length > effectiveLimit;
    const data = rows.slice(0, effectiveLimit).map((r) => mapFriendshipRow(r as FriendshipRow));
    const nextCursor = hasMore ? data[data.length - 1].id : null;

    return {
      data,
      pagination: { cursor: nextCursor, hasMore },
    };
  }

  async areFriends(userId1: string, userId2: string): Promise<boolean> {
    const row = await this.knex('friendships')
      .where((b) => {
        b.where({ requester_id: userId1, addressee_id: userId2 }).orWhere({
          requester_id: userId2,
          addressee_id: userId1,
        });
      })
      .andWhere('status', 'ACCEPTED')
      .first();

    return row !== undefined;
  }
}
