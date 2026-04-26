export type GroupMemberRole = 'ADMIN' | 'MEMBER';

export type GroupInvitationStatus = 'PENDING' | 'ACCEPTED' | 'DECLINED';

export interface FriendGroup {
  id: string;
  ownerId: string;
  name: string;
  description: string | null;
  isPublic: boolean;
  createdAt: Date;
  updatedAt: Date;
}

export interface GroupMember {
  id: string;
  groupId: string;
  userId: string;
  role: GroupMemberRole;
  joinedAt: Date;
}

export interface GroupInvitation {
  id: string;
  groupId: string;
  inviterId: string;
  inviteeId: string;
  status: GroupInvitationStatus;
  createdAt: Date;
  updatedAt: Date;
}
