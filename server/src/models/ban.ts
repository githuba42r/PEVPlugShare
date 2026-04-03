export type BanIdentifierType = 'EMAIL' | 'GOOGLE_ID';

export interface BannedIdentifier {
  id: string;
  identifierType: BanIdentifierType;
  identifierValue: string;
  reason: string | null;
  bannedByUserId: string;
  createdAt: Date;
  expiresAt: Date | null;
}

export interface SharingBan {
  id: string;
  bannedByUserId: string;
  bannedUserId: string;
  reason: string | null;
  createdAt: Date;
}
