import type { Knex } from 'knex';
import { v4 as uuidv4 } from 'uuid';
import type {
  FriendGroup,
  GroupMember,
  GroupMemberRole,
  GroupInvitation,
  GroupInvitationStatus,
  PaginatedResult,
} from '../../models';
import { clampLimit } from '../../models';
import type { GroupRepository } from '../interfaces';

interface FriendGroupRow {
  id: string;
  owner_id: string;
  name: string;
  description: string | null;
  created_at: Date | string;
  updated_at: Date | string;
}

interface GroupMemberRow {
  group_id: string;
  user_id: string;
  role: string;
  added_at: Date | string;
}

interface GroupInvitationRow {
  id: string;
  group_id: string;
  invited_by: string;
  invitee_id: string | null;
  invitee_email: string | null;
  token: string;
  status: string;
  created_at: Date | string;
  expires_at: Date | string | null;
  responded_at: Date | string | null;
  responded_by: string | null;
}

function mapGroupRow(row: FriendGroupRow): FriendGroup {
  return {
    id: row.id,
    ownerId: row.owner_id,
    name: row.name,
    description: row.description,
    isPublic: false,
    createdAt: new Date(row.created_at),
    updatedAt: new Date(row.updated_at),
  };
}

function mapMemberRow(row: GroupMemberRow): GroupMember {
  return {
    id: `${row.group_id}:${row.user_id}`,
    groupId: row.group_id,
    userId: row.user_id,
    role: row.role as GroupMemberRole,
    joinedAt: new Date(row.added_at),
  };
}

function mapInvitationRow(row: GroupInvitationRow): GroupInvitation {
  return {
    id: row.id,
    groupId: row.group_id,
    inviterId: row.invited_by,
    inviteeId: row.invitee_id ?? '',
    status: row.status as GroupInvitationStatus,
    createdAt: new Date(row.created_at),
    updatedAt: row.responded_at ? new Date(row.responded_at) : new Date(row.created_at),
  };
}

export class MysqlGroupRepo implements GroupRepository {
  constructor(private readonly knex: Knex) {}

  async create(ownerId: string, name: string, description?: string): Promise<FriendGroup> {
    const id = uuidv4();
    const now = new Date();
    await this.knex('friend_groups').insert({
      id,
      owner_id: ownerId,
      name,
      description: description ?? null,
      created_at: now,
      updated_at: now,
    });
    const row = await this.knex('friend_groups').where({ id }).first();
    return mapGroupRow(row as FriendGroupRow);
  }

  async getById(id: string): Promise<FriendGroup | null> {
    const row = await this.knex('friend_groups').where({ id }).first();
    return row ? mapGroupRow(row as FriendGroupRow) : null;
  }

  async update(id: string, fields: { name?: string; description?: string }): Promise<FriendGroup> {
    const updates: Record<string, unknown> = { updated_at: new Date() };
    if (fields.name !== undefined) updates.name = fields.name;
    if (fields.description !== undefined) updates.description = fields.description;
    await this.knex('friend_groups').where({ id }).update(updates);
    const row = await this.knex('friend_groups').where({ id }).first();
    return mapGroupRow(row as FriendGroupRow);
  }

  async delete(id: string): Promise<boolean> {
    const count = await this.knex('friend_groups').where({ id }).delete();
    return count > 0;
  }

  async getByUser(userId: string, cursor?: string, limit?: number): Promise<PaginatedResult<FriendGroup>> {
    const effectiveLimit = clampLimit(limit);

    // Groups where user is owner OR member
    let qb = this.knex('friend_groups')
      .where('friend_groups.owner_id', userId)
      .orWhereIn('friend_groups.id', (sub) => {
        sub.select('group_id').from('friend_group_members').where('user_id', userId);
      })
      .orderBy('friend_groups.id', 'asc')
      .limit(effectiveLimit + 1);

    if (cursor) {
      qb = qb.where('friend_groups.id', '>', cursor);
    }

    const rows = await qb;
    const hasMore = rows.length > effectiveLimit;
    const data = rows.slice(0, effectiveLimit).map((r) => mapGroupRow(r as FriendGroupRow));
    const nextCursor = hasMore ? data[data.length - 1].id : null;

    return {
      data,
      pagination: { cursor: nextCursor, hasMore },
    };
  }

  async listAll(cursor?: string, limit?: number): Promise<PaginatedResult<FriendGroup>> {
    const effectiveLimit = clampLimit(limit);

    let qb = this.knex('friend_groups').orderBy('id', 'asc').limit(effectiveLimit + 1);

    if (cursor) {
      qb = qb.where('id', '>', cursor);
    }

    const rows = await qb;
    const hasMore = rows.length > effectiveLimit;
    const data = rows.slice(0, effectiveLimit).map((r) => mapGroupRow(r as FriendGroupRow));
    const nextCursor = hasMore ? data[data.length - 1].id : null;

    return {
      data,
      pagination: { cursor: nextCursor, hasMore },
    };
  }

  async addMember(groupId: string, userId: string, role: GroupMemberRole): Promise<GroupMember> {
    const now = new Date();
    await this.knex('friend_group_members').insert({
      group_id: groupId,
      user_id: userId,
      role,
      added_at: now,
    });
    const row = await this.knex('friend_group_members').where({ group_id: groupId, user_id: userId }).first();
    return mapMemberRow(row as GroupMemberRow);
  }

  async removeMember(groupId: string, userId: string): Promise<boolean> {
    const count = await this.knex('friend_group_members')
      .where({ group_id: groupId, user_id: userId })
      .delete();
    return count > 0;
  }

  async setMemberRole(groupId: string, userId: string, role: GroupMemberRole): Promise<GroupMember> {
    await this.knex('friend_group_members')
      .where({ group_id: groupId, user_id: userId })
      .update({ role });
    const row = await this.knex('friend_group_members').where({ group_id: groupId, user_id: userId }).first();
    return mapMemberRow(row as GroupMemberRow);
  }

  async getMembers(groupId: string): Promise<GroupMember[]> {
    const rows = await this.knex('friend_group_members')
      .where({ group_id: groupId })
      .orderBy('added_at', 'asc');
    return rows.map((r) => mapMemberRow(r as GroupMemberRow));
  }

  async isMember(groupId: string, userId: string): Promise<boolean> {
    // Owner counts as member
    const group = await this.knex('friend_groups').where({ id: groupId }).first();
    if (group && group.owner_id === userId) return true;

    const row = await this.knex('friend_group_members')
      .where({ group_id: groupId, user_id: userId })
      .first();
    return row !== undefined;
  }

  async isAdmin(groupId: string, userId: string): Promise<boolean> {
    // Owner is always admin
    const group = await this.knex('friend_groups').where({ id: groupId }).first();
    if (group && group.owner_id === userId) return true;

    const row = await this.knex('friend_group_members')
      .where({ group_id: groupId, user_id: userId, role: 'ADMIN' })
      .first();
    return row !== undefined;
  }

  async getUserGroupIds(userId: string): Promise<string[]> {
    const owned = await this.knex('friend_groups')
      .where({ owner_id: userId })
      .select('id');
    const membered = await this.knex('friend_group_members')
      .where({ user_id: userId })
      .select('group_id as id');

    const all = [...owned, ...membered];
    const unique = [...new Set(all.map((r: { id: string }) => r.id))];
    return unique;
  }

  async createInvitation(groupId: string, invitedBy: string, inviteeId: string): Promise<GroupInvitation> {
    const id = uuidv4();
    const token = uuidv4();
    const now = new Date();
    await this.knex('group_invitations').insert({
      id,
      group_id: groupId,
      invited_by: invitedBy,
      invitee_id: inviteeId,
      invitee_email: null,
      token,
      status: 'PENDING',
      created_at: now,
      expires_at: null,
      responded_at: null,
      responded_by: null,
    });
    const row = await this.knex('group_invitations').where({ id }).first();
    return mapInvitationRow(row as GroupInvitationRow);
  }

  async getInvitationById(id: string): Promise<GroupInvitation | null> {
    const row = await this.knex('group_invitations').where({ id }).first();
    return row ? mapInvitationRow(row as GroupInvitationRow) : null;
  }

  async getPendingInvitations(groupId: string): Promise<GroupInvitation[]> {
    const rows = await this.knex('group_invitations')
      .where({ group_id: groupId, status: 'PENDING' })
      .orderBy('created_at', 'asc');
    return rows.map((r) => mapInvitationRow(r as GroupInvitationRow));
  }

  async updateInvitationStatus(
    id: string,
    status: GroupInvitationStatus,
    respondedBy?: string,
  ): Promise<GroupInvitation> {
    const now = new Date();
    await this.knex('group_invitations').where({ id }).update({
      status,
      responded_at: now,
      responded_by: respondedBy ?? null,
    });
    const row = await this.knex('group_invitations').where({ id }).first();
    return mapInvitationRow(row as GroupInvitationRow);
  }
}
