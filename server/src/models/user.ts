export interface User {
  id: string;
  googleId: string;
  email: string;
  displayName: string;
  avatarUrl: string | null;
  isAdmin: boolean;
  isBanned: boolean;
  banReason: string | null;
  bannedAt: Date | null;
  bannedBy: string | null;
  createdAt: Date;
  lastSeenAt: Date;
}

export interface CreateUserInput {
  googleId: string;
  email: string;
  displayName: string;
  avatarUrl?: string | null;
}
