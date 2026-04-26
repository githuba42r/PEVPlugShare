export interface DirectShareToken {
  id: string;
  token: string;
  locationId: string;
  createdByUserId: string;
  maxUses: number | null;
  useCount: number;
  createdAt: Date;
  expiresAt: Date | null;
}
