import type {
  User,
  CreateUserInput,
  RateLimit,
  CreateRateLimitInput,
  ResolvedRateLimit,
  BannedIdentifier,
  BanIdentifierType,
  SharingBan,
  Notification,
  NotificationType,
  PaginatedResult,
  Location,
  CreateLocationInput,
  Photo,
  Friendship,
  FriendGroup,
  GroupMember,
  GroupMemberRole,
  GroupInvitation,
  GroupInvitationStatus,
  LocationShare,
  HiddenShare,
  ChangeSuggestion,
  DirectShareToken,
} from '../models';

export interface UserRepository {
  create(input: CreateUserInput): Promise<User>;
  getById(id: string): Promise<User | null>;
  getByGoogleId(googleId: string): Promise<User | null>;
  getByEmail(email: string): Promise<User | null>;
  updateLastSeen(id: string): Promise<void>;
  setAdmin(id: string, isAdmin: boolean): Promise<void>;
  setBanned(id: string, isBanned: boolean, reason?: string, bannedBy?: string): Promise<void>;
  search(query: string, cursor?: string, limit?: number): Promise<PaginatedResult<User>>;
  count(): Promise<number>;
}

export interface RateLimitRepository {
  create(input: CreateRateLimitInput): Promise<RateLimit>;
  getById(id: string): Promise<RateLimit | null>;
  update(id: string, fields: Partial<CreateRateLimitInput>): Promise<RateLimit>;
  delete(id: string): Promise<boolean>;
  getAll(): Promise<RateLimit[]>;
  getByScope(scope: string, scopeId?: string): Promise<RateLimit[]>;
  resolve(userId: string, endpoint: string, userGroupIds: string[]): Promise<ResolvedRateLimit | null>;
}

export interface BanRepository {
  addBannedIdentifier(type: BanIdentifierType, identifier: string, reason?: string, bannedBy?: string): Promise<BannedIdentifier>;
  removeBannedIdentifier(id: string): Promise<boolean>;
  isBanned(type: BanIdentifierType, identifier: string): Promise<boolean>;
  listBannedIdentifiers(): Promise<BannedIdentifier[]>;
  addSharingBan(userId: string, reason: string, bannedBy: string, expiresAt?: Date): Promise<SharingBan>;
  removeSharingBan(id: string): Promise<boolean>;
  hasActiveSharingBan(userId: string): Promise<boolean>;
  listSharingBans(): Promise<SharingBan[]>;
}

export interface NotificationRepository {
  create(userId: string, type: NotificationType, payload: object): Promise<Notification>;
  getUnread(userId: string, cursor?: string, limit?: number): Promise<PaginatedResult<Notification>>;
  markRead(id: string): Promise<void>;
  markAllRead(userId: string): Promise<void>;
  count(userId: string): Promise<number>;
}

export interface LocationRepository {
  create(input: CreateLocationInput): Promise<Location>;
  getById(id: string): Promise<Location | null>;
  getByOwner(ownerId: string, cursor?: string, limit?: number): Promise<PaginatedResult<Location>>;
  update(id: string, fields: Partial<CreateLocationInput>): Promise<Location>;
  softDelete(id: string, reason?: string): Promise<boolean>;
  getSharedWithUser(userId: string, cursor?: string, limit?: number): Promise<PaginatedResult<Location>>;
  getNearby(lat: number, lon: number, radiusKm: number, cursor?: string, limit?: number): Promise<PaginatedResult<Location>>;
  findDuplicates(lat: number, lon: number, radiusMetres: number): Promise<Location[]>;
  listAll(cursor?: string, limit?: number): Promise<PaginatedResult<Location>>;
  count(): Promise<number>;
}

export interface PhotoRepository {
  create(locationId: string, filePath: string, thumbnailPath: string | null, metadata: Partial<Photo>): Promise<Photo>;
  getById(id: string): Promise<Photo | null>;
  getByLocationId(locationId: string): Promise<Photo[]>;
  delete(id: string): Promise<boolean>;
  countByLocationId(locationId: string): Promise<number>;
}
export interface FriendRepository {
  sendRequest(requesterId: string, addresseeId: string): Promise<Friendship>;
  getRequestById(id: string): Promise<Friendship | null>;
  acceptRequest(id: string): Promise<Friendship>;
  declineRequest(id: string): Promise<Friendship>;
  removeFriend(userId: string, friendId: string): Promise<boolean>;
  getFriends(userId: string, cursor?: string, limit?: number): Promise<PaginatedResult<User>>;
  getIncomingRequests(userId: string, cursor?: string, limit?: number): Promise<PaginatedResult<Friendship>>;
  getOutgoingRequests(userId: string, cursor?: string, limit?: number): Promise<PaginatedResult<Friendship>>;
  areFriends(userId1: string, userId2: string): Promise<boolean>;
}

export interface GroupRepository {
  create(ownerId: string, name: string, description?: string): Promise<FriendGroup>;
  getById(id: string): Promise<FriendGroup | null>;
  update(id: string, fields: { name?: string; description?: string }): Promise<FriendGroup>;
  delete(id: string): Promise<boolean>;
  getByUser(userId: string, cursor?: string, limit?: number): Promise<PaginatedResult<FriendGroup>>;
  listAll(cursor?: string, limit?: number): Promise<PaginatedResult<FriendGroup>>;

  addMember(groupId: string, userId: string, role: GroupMemberRole): Promise<GroupMember>;
  removeMember(groupId: string, userId: string): Promise<boolean>;
  setMemberRole(groupId: string, userId: string, role: GroupMemberRole): Promise<GroupMember>;
  getMembers(groupId: string): Promise<GroupMember[]>;
  isMember(groupId: string, userId: string): Promise<boolean>;
  isAdmin(groupId: string, userId: string): Promise<boolean>;
  getUserGroupIds(userId: string): Promise<string[]>;

  createInvitation(groupId: string, invitedBy: string, inviteeId: string): Promise<GroupInvitation>;
  getInvitationById(id: string): Promise<GroupInvitation | null>;
  getPendingInvitations(groupId: string): Promise<GroupInvitation[]>;
  updateInvitationStatus(id: string, status: GroupInvitationStatus, respondedBy?: string): Promise<GroupInvitation>;
}
export interface ShareRepository {
  shareWithUser(locationId: string, sharedBy: string, sharedWithUser: string): Promise<LocationShare>;
  shareWithGroup(locationId: string, sharedBy: string, sharedWithGroup: string): Promise<LocationShare>;
  revokeShare(shareId: string): Promise<boolean>;
  getSharesForLocation(locationId: string, cursor?: string, limit?: number): Promise<PaginatedResult<LocationShare>>;
  getLocationsSharedWithUser(userId: string, userGroupIds: string[], cursor?: string, limit?: number): Promise<PaginatedResult<Location>>;
  deactivateSharesForLocation(locationId: string): Promise<void>;
  isSharedWithUser(locationId: string, userId: string, userGroupIds: string[]): Promise<boolean>;

  hideShare(userId: string, locationId: string, shareId: string): Promise<HiddenShare>;
  unhideShare(userId: string, shareId: string): Promise<boolean>;
  getHiddenShareIds(userId: string): Promise<string[]>;
  unhideByLocationVersion(locationId: string): Promise<void>;
}

export interface SuggestionRepository {
  create(locationId: string, suggestedBy: string, changesJson: string, message?: string): Promise<ChangeSuggestion>;
  getById(id: string): Promise<ChangeSuggestion | null>;
  getIncoming(ownerId: string, cursor?: string, limit?: number): Promise<PaginatedResult<ChangeSuggestion>>;
  getOutgoing(userId: string, cursor?: string, limit?: number): Promise<PaginatedResult<ChangeSuggestion>>;
  accept(id: string, responseMsg?: string): Promise<ChangeSuggestion>;
  reject(id: string, responseMsg?: string): Promise<ChangeSuggestion>;
  withdraw(id: string): Promise<ChangeSuggestion>;
}

export interface DeepLinkRepository {
  createToken(locationId: string, createdBy: string, token: string, isSingleUse?: boolean, maxUses?: number, expiresAt?: Date): Promise<DirectShareToken>;
  getByToken(token: string): Promise<DirectShareToken | null>;
  incrementUseCount(id: string): Promise<void>;
  delete(id: string): Promise<boolean>;
}
