export interface Location {
  id: string;
  ownerId: string;
  name: string;
  latitude: number;
  longitude: number;
  description: string | null;
  additionalInfo: string | null;
  openingHours: string | null;
  socketTypes: string[];
  gpoCount: number | null;
  hasFee: boolean;
  chargingCosts: string | null;
  publicPrivate: 'PUBLIC' | 'PRIVATE' | 'SEMI_PRIVATE';
  providerName: string | null;
  providerWebsite: string | null;
  businessName: string | null;
  streetAddress: string | null;
  suburb: string | null;
  state: string | null;
  postcode: string | null;
  color: string | null;
  icon: string | null;
  isDeleted: boolean;
  deletedReason: string | null;
  createdAt: Date;
  updatedAt: Date;
  version: number;
}

export interface CreateLocationInput {
  ownerId: string;
  name: string;
  latitude: number;
  longitude: number;
  description?: string | null;
  additionalInfo?: string | null;
  openingHours?: string | null;
  socketTypes?: string[];
  gpoCount?: number | null;
  hasFee?: boolean;
  chargingCosts?: string | null;
  publicPrivate?: 'PUBLIC' | 'PRIVATE' | 'SEMI_PRIVATE';
  providerName?: string | null;
  providerWebsite?: string | null;
  businessName?: string | null;
  streetAddress?: string | null;
  suburb?: string | null;
  state?: string | null;
  postcode?: string | null;
  color?: string | null;
  icon?: string | null;
}
