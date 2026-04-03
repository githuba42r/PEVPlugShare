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

// Placeholder interfaces for future phases
export interface LocationRepository {}
export interface PhotoRepository {}
export interface FriendRepository {}
export interface GroupRepository {}
export interface ShareRepository {}
export interface SuggestionRepository {}
export interface DeepLinkRepository {}
