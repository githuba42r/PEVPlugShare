export type SuggestionStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'WITHDRAWN';

export interface ChangeSuggestion {
  id: string;
  locationId: string;
  suggestedByUserId: string;
  status: SuggestionStatus;
  fieldName: string;
  currentValue: string | null;
  suggestedValue: string;
  reason: string | null;
  reviewedByUserId: string | null;
  reviewedAt: Date | null;
  reviewNote: string | null;
  createdAt: Date;
  updatedAt: Date;
}
