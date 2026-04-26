export interface LocationShare {
  id: string;
  locationId: string;
  sharedByUserId: string;
  sharedWithUserId: string | null;
  sharedWithGroupId: string | null;
  canEdit: boolean;
  createdAt: Date;
  expiresAt: Date | null;
}

export interface HiddenShare {
  id: string;
  locationId: string;
  hiddenByUserId: string;
  createdAt: Date;
}
