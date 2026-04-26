import type { Knex } from 'knex';
import { v4 as uuidv4 } from 'uuid';
import type { Notification, NotificationType, PaginatedResult } from '../../models';
import { clampLimit } from '../../models';
import type { NotificationRepository } from '../interfaces';

interface NotificationRow {
  id: string;
  user_id: string;
  type: string;
  payload_json: string;
  is_read: boolean | number;
  created_at: Date | string;
}

interface NotificationPayload {
  title?: string;
  body?: string;
  data?: Record<string, unknown>;
  [key: string]: unknown;
}

function mapRow(row: NotificationRow): Notification {
  let payload: NotificationPayload = {};
  try {
    payload = JSON.parse(row.payload_json) as NotificationPayload;
  } catch {
    payload = {};
  }

  return {
    id: row.id,
    userId: row.user_id,
    type: row.type as NotificationType,
    title: (payload.title as string) ?? '',
    body: (payload.body as string) ?? '',
    data: (payload.data as Record<string, unknown>) ?? null,
    isRead: Boolean(row.is_read),
    createdAt: new Date(row.created_at),
    readAt: null,
  };
}

export class PostgresNotificationRepo implements NotificationRepository {
  constructor(private readonly knex: Knex) {}

  async create(userId: string, type: NotificationType, payload: object): Promise<Notification> {
    const id = uuidv4();
    const now = new Date();
    await this.knex('notifications').insert({
      id,
      user_id: userId,
      type,
      payload_json: JSON.stringify(payload),
      is_read: false,
      created_at: now,
    });
    const row = await this.knex('notifications').where({ id }).first();
    return mapRow(row as NotificationRow);
  }

  async getUnread(
    userId: string,
    cursor?: string,
    limit?: number
  ): Promise<PaginatedResult<Notification>> {
    const effectiveLimit = clampLimit(limit);

    let qb = this.knex('notifications')
      .where({ user_id: userId, is_read: false })
      .orderBy('created_at', 'desc')
      .limit(effectiveLimit + 1);

    if (cursor) {
      // cursor is an id; we use created_at-based cursor via id comparison
      // For simplicity, use id as cursor (works for DESC ordering by created_at if we store id order)
      // Better: use created_at of the cursor row as the boundary
      const cursorRow = await this.knex('notifications').where({ id: cursor }).first();
      if (cursorRow) {
        qb = this.knex('notifications')
          .where({ user_id: userId, is_read: false })
          .where('created_at', '<', (cursorRow as NotificationRow).created_at)
          .orderBy('created_at', 'desc')
          .limit(effectiveLimit + 1);
      }
    }

    const rows = await qb;
    const hasMore = rows.length > effectiveLimit;
    const data = rows.slice(0, effectiveLimit).map((r) => mapRow(r as NotificationRow));
    const nextCursor = hasMore ? data[data.length - 1].id : null;

    return {
      data,
      pagination: { cursor: nextCursor, hasMore },
    };
  }

  async markRead(id: string): Promise<void> {
    await this.knex('notifications').where({ id }).update({ is_read: true });
  }

  async markAllRead(userId: string): Promise<void> {
    await this.knex('notifications').where({ user_id: userId }).update({ is_read: true });
  }

  async count(userId: string): Promise<number> {
    const result = await this.knex('notifications')
      .where({ user_id: userId, is_read: false })
      .count('* as count')
      .first();
    return parseInt(String((result as { count: string | number }).count), 10);
  }
}
